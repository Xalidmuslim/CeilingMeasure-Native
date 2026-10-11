package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Native direct-transfer checkout preview and WhatsApp contact sheet.
 * No bank details or card SDK. Seller verifies the transfer separately.
 */
final class NativePurchaseDialog {
    private NativePurchaseDialog() {}

    private static String checkedPhone(String value) {
        return value != null && value.matches("[1-9][0-9]{7,14}") ? value : "";
    }

    private static void openWhatsApp(Activity activity, String number, String message,
            TextView feedback) {
        String safe = checkedPhone(number);
        if (safe.isEmpty()) {
            feedback.setText("Номер продавца ещё не настроен.");
            return;
        }
        Uri uri = new Uri.Builder().scheme("https").authority("wa.me")
                .appendPath(safe).appendQueryParameter("text", message).build();
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            feedback.setText("WhatsApp не открылся. Свяжитесь с продавцом по телефону.");
        }
    }

    private static RadioButton plan(Activity activity, String label) {
        RadioButton option = new RadioButton(activity);
        option.setId(View.generateViewId());
        option.setText(label);
        option.setTextSize(13f);
        option.setTextColor(NativeVisualTokens.HOME_CARD_TEXT);
        option.setButtonTintList(ColorStateList.valueOf(NativeVisualTokens.ACCENT_STRONG));
        option.setGravity(Gravity.CENTER_VERTICAL);
        option.setPadding(NativeLicenseUi.dp(activity, 8),
                NativeLicenseUi.dp(activity, 7),
                NativeLicenseUi.dp(activity, 4),
                NativeLicenseUi.dp(activity, 7));
        option.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE_SOFT,
                NativeVisualTokens.LINE,
                NativeLicenseUi.dp(activity, 12), NativeLicenseUi.dp(activity, 1)));
        return option;
    }

    static void show(Activity activity, NativeLicenseClient client, Runnable onChanged) {
        NativeLicenseUi.Sheet sheet = new NativeLicenseUi.Sheet(
                activity, "Выбор лицензии", "Разовая покупка · без автоматических списаний");

        TextView description = NativeLicenseUi.notice(activity,
                "7 дней бесплатно. После перевода продавцу доступ активируется вручную.");
        sheet.body.addView(description, NativeLicenseUi.spaced(activity, 11));

        RadioGroup plans = new RadioGroup(activity);
        plans.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton monthly = plan(activity, "30 дней\n299 ₽");
        RadioButton lifetime = plan(activity, "Навсегда\n1 900 ₽");
        RadioGroup.LayoutParams monthlyLp = new RadioGroup.LayoutParams(
                0, NativeLicenseUi.dp(activity, 70), 1f);
        plans.addView(monthly, monthlyLp);
        RadioGroup.LayoutParams lifetimeLp = new RadioGroup.LayoutParams(
                0, NativeLicenseUi.dp(activity, 70), 1f);
        lifetimeLp.leftMargin = NativeLicenseUi.dp(activity, 8);
        plans.addView(lifetime, lifetimeLp);
        plans.check(monthly.getId());

        plans.setOnCheckedChangeListener((group, id) -> {
            monthly.setBackground(NativeVisualTokens.outlined(
                    id == monthly.getId() ? NativeVisualTokens.ACCENT_SOFT
                            : NativeVisualTokens.SURFACE_SOFT,
                    id == monthly.getId() ? NativeVisualTokens.ACCENT
                            : NativeVisualTokens.LINE,
                    NativeLicenseUi.dp(activity, 12), NativeLicenseUi.dp(activity, 1)));
            lifetime.setBackground(NativeVisualTokens.outlined(
                    id == lifetime.getId() ? NativeVisualTokens.ACCENT_SOFT
                            : NativeVisualTokens.SURFACE_SOFT,
                    id == lifetime.getId() ? NativeVisualTokens.ACCENT
                            : NativeVisualTokens.LINE,
                    NativeLicenseUi.dp(activity, 12), NativeLicenseUi.dp(activity, 1)));
        });
        plans.check(lifetime.getId());
        plans.check(monthly.getId());
        sheet.body.addView(plans, NativeLicenseUi.spaced(activity, 11));

        LinearLayout info = NativeLicenseUi.panel(activity);
        info.addView(NativeLicenseUi.heading(activity, "Связь с продавцом"));
        TextView number = NativeLicenseUi.text(activity,
                "+7 901 922-10-00", 15f, true);
        number.setTextColor(NativeVisualTokens.ACCENT_STRONG);
        info.addView(number, NativeLicenseUi.spaced(activity, 5));

        LinearLayout buyerRow = NativeLicenseUi.row(activity, 0);
        TextView idLabel = NativeLicenseUi.text(activity,
                client.userId().isEmpty() ? "ID появится после активации"
                        : "ID: " + client.userId(), 11f, false);
        idLabel.setSingleLine(true);
        idLabel.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        buyerRow.addView(idLabel, NativeLicenseUi.weighted(activity, 0));
        TextView copyId = NativeLicenseUi.inlineLink(activity, "Копировать ID");
        buyerRow.addView(copyId);
        info.addView(buyerRow, NativeLicenseUi.spaced(activity, 5));
        sheet.body.addView(info, NativeLicenseUi.spaced(activity, 11));

        LinearLayout buttons = NativeLicenseUi.row(activity, 8);
        Button whatsapp = NativeLicenseUi.button(activity, "WhatsApp", true);
        Button phone = NativeLicenseUi.button(activity, "Позвонить", false);
        buttons.addView(whatsapp, NativeLicenseUi.weighted(activity, 0));
        buttons.addView(phone, NativeLicenseUi.weighted(activity, 8));
        sheet.body.addView(buttons, NativeLicenseUi.spaced(activity, 10));

        Button refresh = NativeLicenseUi.button(activity, "Проверить активацию", false);
        sheet.body.addView(refresh, NativeLicenseUi.spaced(activity, 8));

        TextView result = NativeLicenseUi.text(activity, "", 12f, false);
        sheet.body.addView(result, NativeLicenseUi.spaced(activity, 7));

        sheet.body.addView(NativeLicenseUi.text(activity,
                "После поступления перевода продавец откроет доступ по ID. "
                        + "Секретный код восстановления ему не передавайте.",
                11f, false), NativeLicenseUi.spaced(activity, 4));

        copyId.setEnabled(!client.userId().isEmpty());
        copyId.setAlpha(client.userId().isEmpty() ? .42f : 1f);
        copyId.setOnClickListener(v -> {
            String id = client.userId();
            if (id.isEmpty()) return;
            ClipboardManager clipboard = (ClipboardManager)
                    activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("ID покупателя", id));
                result.setText("ID скопирован. Его можно отправить продавцу.");
            }
        });

        whatsapp.setEnabled(false);
        phone.setEnabled(false);
        refresh.setEnabled(NativeLicenseClient.enabled() && !client.recoveryCode().isEmpty());

        refresh.setOnClickListener(v -> {
            refresh.setEnabled(false);
            result.setText("Проверяем лицензию…");
            client.refresh((ok, message) -> {
                if (sheet.visible()) {
                    refresh.setEnabled(true);
                    result.setText(message);
                    onChanged.run();
                }
            });
        });

        if (!NativeLicenseClient.enabled()) {
            result.setText("Тестовая версия: подключение к продавцу и активация пока отключены.");
        } else if (client.userId().isEmpty()) {
            result.setText("Сначала запустите бесплатный пробный период.");
        } else {
            result.setText("Загружаем данные продавца…");
            client.loadCatalogue((catalogue, error) -> {
                if (!sheet.visible()) return;
                if (error != null || catalogue == null) {
                    result.setText("Не удалось загрузить контакты. Попробуйте позже.");
                    return;
                }
                JSONObject data = catalogue.optJSONObject("plans");
                JSONObject monthData = data == null ? null : data.optJSONObject("monthly");
                JSONObject lifetimeData = data == null ? null : data.optJSONObject("lifetime");
                final int monthPrice = monthData == null
                        ? 299 : monthData.optInt("price_rub", 299);
                final int foreverPrice = lifetimeData == null
                        ? 1900 : lifetimeData.optInt("price_rub", 1900);
                monthly.setText("30 дней\n" + monthPrice + " ₽");
                lifetime.setText("Навсегда\n"
                        + String.format(java.util.Locale.forLanguageTag("ru"),
                                "%,d ₽", foreverPrice).replace(',', ' '));
                final String wa = checkedPhone(catalogue.optString("whatsapp_number", ""));
                final String call = checkedPhone(catalogue.optString("call_number", ""));
                if (wa.isEmpty() && call.isEmpty()) {
                    result.setText("Контакты пока не настроены.");
                    return;
                }
                number.setText("+" + (!wa.isEmpty() ? wa : call));
                whatsapp.setEnabled(!wa.isEmpty());
                phone.setEnabled(!call.isEmpty());
                whatsapp.setOnClickListener(v -> {
                    boolean isMonth = plans.getCheckedRadioButtonId() == monthly.getId();
                    String period = isMonth ? "на 30 дней" : "навсегда";
                    String price = (isMonth ? monthPrice : foreverPrice) + " ₽";
                    String message = "Здравствуйте! Хочу купить «Замер потолков» "
                            + period + " за " + price + ". Мой ID: " + client.userId()
                            + ". Как оплатить переводом?";
                    openWhatsApp(activity, wa, message, result);
                });
                phone.setOnClickListener(v -> {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:+" + call)));
                    } catch (ActivityNotFoundException ignored) {
                        result.setText("Номер телефона: +" + call);
                    }
                });
                result.setText("Выберите вариант и свяжитесь с продавцом.");
            });
        }

        sheet.show();
    }
}
