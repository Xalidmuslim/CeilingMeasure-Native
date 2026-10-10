package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional v225 licensing adapter.
 * All geometry and project persistence remain independent of this class.
 * Empty BuildConfig fields mean licensing has not been deployed yet (legacy access).
 * Client never trusts an unsigned HTTP status or a device-controlled trial start.
 */
final class NativeLicenseClient {
    interface Callback { void done(boolean success, String message); }

    private static final String PREFS = "native_license_access";
    private static final String DEVICE = "device";
    private static final String SESSION = "session";
    private static final String LICENSE = "license";
    private static final String EMAIL = "email";
    private static final String LAST_CLOCK = "last_clock";
    private static final long FIVE_MIN = 300000L;

    private final SharedPreferences prefs;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    NativeLicenseClient(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getString(DEVICE, "").isEmpty()) {
            prefs.edit().putString(DEVICE, UUID.randomUUID().toString().replace("-", "")).apply();
        }
    }

    static boolean enabled() {
        return BuildConfig.LICENSE_SERVER_URL.startsWith("https://")
                && !BuildConfig.LICENSE_PUBLIC_KEY_B64.isEmpty();
    }

    String email() { return prefs.getString(EMAIL, ""); }

    private String deviceId() { return prefs.getString(DEVICE, ""); }

    private JSONObject validPayload() {
        if (!enabled()) return null;
        try {
            String token = prefs.getString(LICENSE, "");
            String[] pieces = token.split("\\.");
            if (pieces.length != 2) return null;
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.decode(BuildConfig.LICENSE_PUBLIC_KEY_B64, Base64.DEFAULT)));
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(pieces[0].getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.decode(pieces[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING)))
                return null;
            JSONObject claims = new JSONObject(new String(
                    Base64.decode(pieces[0], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                    StandardCharsets.UTF_8));
            long now = System.currentTimeMillis();
            long seen = prefs.getLong(LAST_CLOCK, 0L);
            // Fail closed if local time has moved substantially backwards.
            if (seen > now + FIVE_MIN) return null;
            if (now > seen) prefs.edit().putLong(LAST_CLOCK, now).apply();
            if (claims.optInt("schema") != 1
                    || !"ceilingmeasure-license".equals(claims.optString("issuer"))
                    || !"ru.xalidmuslim.ceilingmeasure".equals(claims.optString("audience"))
                    || !deviceId().equals(claims.optString("device_id"))
                    || "expired".equals(claims.optString("kind"))) return null;
            long issued = claims.getLong("issued_at"), lease = claims.getLong("lease_until");
            if (issued > now + FIVE_MIN || lease <= now) return null;
            if (!claims.isNull("access_until") && claims.getLong("access_until") <= now) return null;
            return claims;
        } catch (Exception exception) { return null; }
    }

    boolean canEdit() { return !enabled() || validPayload() != null; }

    String status() {
        if (!enabled()) return "Лицензирование ещё не подключено";
        JSONObject claims = validPayload();
        if (claims == null) return "Нужна активация или обновление лицензии";
        switch (claims.optString("kind")) {
            case "trial": return "Пробный доступ — 7 дней с регистрации";
            case "monthly": return "Подписка активна";
            case "lifetime": return "Бессрочная лицензия активна";
            default: return "Нет активной лицензии";
        }
    }

    void requestCode(String email, Callback callback) {
        JSONObject json = new JSONObject();
        try { json.put("email", email); } catch (Exception ignored) {}
        runRequest("POST", "/v1/auth/request", json, "", (result, error) ->
                callback.done(error == null, error == null ? "Код отправлен на почту" : error));
    }

    void verifyCode(String email, String code, Callback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("code", code);
            json.put("device_id", deviceId());
        } catch (Exception ignored) {}
        runRequest("POST", "/v1/auth/verify", json, "", (result, error) -> {
            if (error != null) { callback.done(false, error); return; }
            String session = result.optString("session_token");
            if (session.isEmpty()) { callback.done(false, "Сервер не вернул сессию"); return; }
            prefs.edit().putString(SESSION, session).putString(EMAIL, email.trim()).remove(LICENSE).apply();
            refresh(callback);
        });
    }

    void refresh(Callback callback) {
        if (!enabled()) { callback.done(true, status()); return; }
        String session = prefs.getString(SESSION, "");
        if (session.isEmpty()) { callback.done(false, "Сначала войдите по электронной почте"); return; }
        runRequest("GET", "/v1/license", null, session, (result, error) -> {
            if (error != null) { callback.done(false, error); return; }
            String license = result.optString("license");
            if (license.isEmpty()) { callback.done(false, "Сервер не вернул лицензию"); return; }
            String previous = prefs.getString(LICENSE, "");
            prefs.edit().putString(LICENSE, license).apply();
            if (!canEdit() && !"expired".equals(result.optString("status"))) {
                prefs.edit().putString(LICENSE, previous).apply();
                callback.done(false, "Подпись сервера не прошла проверку");
                return;
            }
            callback.done(true, status());
        });
    }

    void signOut() {
        prefs.edit().remove(EMAIL).remove(SESSION).remove(LICENSE).apply();
    }

    private interface Result { void accept(JSONObject result, String error); }

    private void runRequest(String method, String path, JSONObject data, String session, Result done) {
        if (!enabled()) { main.post(() -> done.accept(null, "Сервер не настроен")); return; }
        worker.execute(() -> {
            HttpURLConnection conn = null;
            JSONObject response = null;
            String error = null;
            try {
                conn = (HttpURLConnection) new URL(BuildConfig.LICENSE_SERVER_URL + path).openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");
                if (!session.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + session);
                if (data != null) {
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (OutputStream out = conn.getOutputStream()) {
                        out.write(data.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
                int code = conn.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                if (stream == null) throw new Exception("Пустой ответ сервера");
                StringBuilder builder = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null && builder.length() < 16000) builder.append(line);
                }
                response = new JSONObject(builder.toString());
                if (code < 200 || code >= 300)
                    error = code == 401 ? "Войдите снова или проверьте код" :
                            code == 429 ? "Слишком много запросов. Попробуйте позже" :
                            "Ошибка сервера (" + code + ")";
            } catch (Exception e) { error = "Нет связи с сервером лицензий"; }
            finally { if (conn != null) conn.disconnect(); }
            final JSONObject result = response;
            final String message = error;
            main.post(() -> done.accept(result, message));
        });
    }
}
