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
 * Native v225: anonymous registration using a local recovery secret.
 * Never handles bank card details. Never interferes with stored ceiling drawings.
 */
final class NativeLicenseClient {
    interface Callback { void done(boolean success, String message); }

    private static final String PREFS = "native_license_access";
    private static final String DEVICE = "device";
    private static final String RECOVERY = "recovery";
    private static final String LICENSE = "license";
    private static final String USER_ID = "user_id";
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

    String userId() { return prefs.getString(USER_ID, ""); }
    String recoveryCode() { return prefs.getString(RECOVERY, ""); }
    private String deviceId() { return prefs.getString(DEVICE, ""); }

    private JSONObject signedPayload(String token) {
        if (!enabled() || token == null) return null;
        try {
            int separator = token.indexOf('.');
            if (separator < 1 || separator != token.lastIndexOf('.')) return null;
            String body = token.substring(0, separator);
            String encodedSignature = token.substring(separator + 1);
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.decode(BuildConfig.LICENSE_PUBLIC_KEY_B64, Base64.DEFAULT)));
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(body.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.decode(encodedSignature,
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING))) return null;
            JSONObject claims = new JSONObject(new String(
                    Base64.decode(body, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                    StandardCharsets.UTF_8));
            if (claims.optInt("schema") != 1
                    || !"ceilingmeasure-license".equals(claims.optString("issuer"))
                    || !"ru.xalidmuslim.ceilingmeasure".equals(claims.optString("audience"))
                    || !deviceId().equals(claims.optString("device_id"))) return null;
            return claims;
        } catch (Exception ignored) { return null; }
    }

    private JSONObject validPayload() {
        JSONObject claims = signedPayload(prefs.getString(LICENSE, ""));
        if (claims == null) return null;
        try {
            long now = System.currentTimeMillis();
            long lastClock = prefs.getLong(LAST_CLOCK, 0L);
            if (lastClock > now + FIVE_MIN) return null;
            if (now > lastClock) prefs.edit().putLong(LAST_CLOCK, now).apply();
            if ("expired".equals(claims.optString("kind"))) return null;
            long issued = claims.getLong("issued_at");
            long lease = claims.getLong("lease_until");
            if (issued > now + FIVE_MIN || lease <= now) return null;
            if (!claims.isNull("access_until") && claims.getLong("access_until") <= now)
                return null;
            return claims;
        } catch (Exception ignored) { return null; }
    }

    boolean canEdit() { return !enabled() || validPayload() != null; }

    String status() {
        if (!enabled()) return "Сервер пока не подключён — текущий доступ сохранён";
        JSONObject claims = validPayload();
        if (claims == null) return recoveryCode().isEmpty()
                ? "7 дней бесплатно · без регистрации и почты"
                : "Требуется проверка или продление лицензии";
        switch (claims.optString("kind")) {
            case "trial": return "Пробный доступ активен";
            case "monthly": return "Месячная лицензия активна";
            case "lifetime": return "Бессрочная лицензия активна";
            default: return "Лицензия неактивна";
        }
    }

    void startTrial(Callback callback) {
        if (!recoveryCode().isEmpty()) {
            callback.done(false, "Пробный период уже зарегистрирован на этом устройстве");
            return;
        }
        JSONObject input = new JSONObject();
        try { input.put("device_id", deviceId()); } catch (Exception ignored) {}
        call("POST", "/v1/auth/register", input, "", (result, error) -> {
            if (error != null) {callback.done(false, error);return;}
            String code = result.optString("recovery_code");
            if (!code.matches("cm1_[A-Za-z0-9_-]{43}")) {
                callback.done(false, "Сервер не вернул код восстановления");return;
            }
            if (!storeSignedLicense(result, code)) {
                callback.done(false, "Не удалось проверить подпись лицензии");return;
            }
            callback.done(true, "Доступ открыт на 7 дней. Сохраните код восстановления!");
        });
    }

    void restore(String recoveryCode, Callback callback) {
        if (!recoveryCode.matches("cm1_[A-Za-z0-9_-]{43}")) {
            callback.done(false, "Неверный формат кода восстановления");
            return;
        }
        JSONObject input = new JSONObject();
        try {
            input.put("device_id", deviceId());
            input.put("recovery_code", recoveryCode);
        } catch (Exception ignored) {}
        call("POST", "/v1/auth/recover", input, "", (result, error) -> {
            if (error != null) {callback.done(false, error);return;}
            if (!storeSignedLicense(result, recoveryCode)) {
                callback.done(false, "Подпись сервера не прошла проверку");return;
            }
            callback.done(true, status());
        });
    }

    void refresh(Callback callback) {
        if (!enabled()) {callback.done(true, status());return;}
        String recovery = recoveryCode();
        if (recovery.isEmpty()) {
            callback.done(false, "Начните пробный период или восстановите покупку");return;
        }
        JSONObject input = new JSONObject();
        try { input.put("device_id", deviceId()); } catch (Exception ignored) {}
        call("POST", "/v1/license", input, recovery, (result, error) -> {
            if (error != null) {callback.done(false, error);return;}
            if (!storeSignedLicense(result, recovery)) {
                callback.done(false, "Подпись сервера не прошла проверку");return;
            }
            callback.done(true, status());
        });
    }

    private boolean storeSignedLicense(JSONObject result, String recovery) {
        if (result == null) return false;
        String token = result.optString("license");
        JSONObject claims = signedPayload(token);
        String id = result.optString("user_id");
        if (claims == null || id.isEmpty() || !id.equals(claims.optString("user_id"))) return false;
        try {
            long issued = claims.getLong("issued_at");
            if (issued > System.currentTimeMillis() + FIVE_MIN) return false;
            prefs.edit().putString(RECOVERY, recovery).putString(USER_ID, id)
                    .putString(LICENSE, token).apply();
            return true;
        } catch (Exception ignored) {return false;}
    }

    private interface Result { void accept(JSONObject result, String error); }

    private void call(String method, String path, JSONObject data, String bearer, Result done) {
        if (!enabled()) {main.post(() -> done.accept(null, "Сервер не настроен"));return;}
        worker.execute(() -> {
            HttpURLConnection connection = null;
            JSONObject result = null;
            String problem = null;
            try {
                connection = (HttpURLConnection)new URL(BuildConfig.LICENSE_SERVER_URL + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/json");
                if (!bearer.isEmpty())
                    connection.setRequestProperty("Authorization", "Bearer " + bearer);
                if (data != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (OutputStream stream=connection.getOutputStream()) {
                        stream.write(data.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
                int status=connection.getResponseCode();
                InputStream responseStream=status>=200 && status<300
                        ? connection.getInputStream() : connection.getErrorStream();
                if (responseStream == null) throw new Exception("empty_response");
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && body.length() < 16000)
                        body.append(line);
                }
                result = new JSONObject(body.toString());
                if (status < 200 || status >= 300) {
                    String error = result.optString("error");
                    problem = status == 401 ? "Код восстановления не найден" :
                            status == 429 ? "Лимит пробных регистраций. Попробуйте завтра" :
                            "Сервер вернул ошибку: " + error;
                }
            } catch (Exception ignored) {
                problem = "Нет связи с сервером лицензий";
            } finally {
                if (connection != null) connection.disconnect();
            }
            final JSONObject value = result;
            final String message = problem;
            main.post(() -> done.accept(value, message));
        });
    }
}
