package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/**
 * Compact seller-only activation panel with matching native app design.
 * The admin secret is entered manually, never embedded or persisted by the APK.
 */
final class NativeOwnerLicenseDialog {
    private NativeOwnerLicenseDialog() {}

    private static RadioButton option(Activity activity, String label) {
        RadioButton button = new RadioButton(activity);
        button.setId(View.generateViewId());
        button.setText(label);
        button.setTextColor(NativeVisualTokens.HOME_CARD_TEXT);
        button.setTextSize(12.5f);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setButtonTintList(ColorStateList.valueOf(NativeVisualTokens.ACCENT));
        button.setPadding(NativeLicenseUi.dp(activity, 8), 0,
                NativeLicenseUi.dp(activity, 4), 0);
        button.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE_SOFT, NativeVisualTokens.LINE,
                NativeLicenseUi.dp(activity, 11), NativeLicenseUi.dp(activity, 1)));
        return button;
    }

    private static String displayExpiry(JSONObject data) {
        if (data == null) return "Покупатель не найден";
        String kind = data.optString("status", data.optString("kind", "expired"));
        String description;
        switch (kind) {
            case "trial": description = "Пробный период"; break;
            case "monthly": description = "Доступ на 30 дней"; break;
            case "lifetime": description = "Бессрочный доступ"; break;
            default: description = "Доступ истёк"; break;
        }
        long expiration = data.optLong("access_until", 0);
        if (expiration > 0 && !"lifetime".equals(kind)) {
            description += " · до " + DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT).format(new Date(expiration));
        }
        return description;
    }

    static void show(Activity activity, NativeLicenseClient client) {
        NativeLicenseUi.Sheet sheet = new NativeLicenseUi.Sheet(
                activity, "Режим владельца", "Выдача лицензий после оплаты");

        sheet.body.addView(NativeLicenseUi.notice(activity,
                "Сначала проверьте поступление перевода в банковском приложении."),
                NativeLicenseUi.spaced(activity, 11));

        sheet.body.addView(NativeLicenseUi.heading(activity, "Ключ владельца"),
                NativeLicenseUi.spaced(activity, 12));
        EditText key = NativeLicenseUi.field(activity, "Секретный ключ",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        sheet.body.addView(key, NativeLicenseUi.spaced(activity, 5));

        sheet.body.addView(NativeLicenseUi.heading(activity, "ID покупателя"),
                NativeLicenseUi.spaced(activity, 11));
        LinearLayout lookup = NativeLicenseUi.row(activity, 8);
        EditText buyer = NativeLicenseUi.field(activity, "Вставьте ID из WhatsApp",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        lookup.addView(buyer, NativeLicenseUi.weighted(activity, 0));
        Button check = NativeLicenseUi.button(activity, "Найти", false);
        lookup.addView(check, new LinearLayout.LayoutParams(
                NativeLicenseUi.dp(activity, 83), NativeLicenseUi.dp(activity, 43)));
        sheet.body.addView(lookup, NativeLicenseUi.spaced(activity, 5));

        TextView current = NativeLicenseUi.text(activity, "", 11.5f, false);
        sheet.body.addView(current, NativeLicenseUi.spaced(activity, 5));

        sheet.body.addView(NativeLicenseUi.heading(activity, "Тип лицензии"),
                NativeLicenseUi.spaced(activity, 12));

        RadioGroup plans = new RadioGroup(activity);
        plans.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton month = option(activity, "30 дней");
        RadioButton forever = option(activity, "Навсегда");
        RadioGroup.LayoutParams first = new RadioGroup.LayoutParams(
                0, NativeLicenseUi.dp(activity, 45), 1f);
        plans.addView(month, first);
        RadioGroup.LayoutParams second = new RadioGroup.LayoutParams(
                0, NativeLicenseUi.dp(activity, 45), 1f);
        second.leftMargin = NativeLicenseUi.dp(activity, 8);
        plans.addView(forever, second);
        plans.setOnCheckedChangeListener((group, checkedId) -> {
            month.setBackground(NativeVisualTokens.outlined(
                    checkedId == month.getId() ? NativeVisualTokens.ACCENT_SOFT
                            : NativeVisualTokens.SURFACE_SOFT,
                    checkedId == month.getId() ? NativeVisualTokens.ACCENT
                            : NativeVisualTokens.LINE,
                    NativeLicenseUi.dp(activity, 11), NativeLicenseUi.dp(activity, 1)));
            forever.setBackground(NativeVisualTokens.outlined(
                    checkedId == forever.getId() ? NativeVisualTokens.ACCENT_SOFT
                            : NativeVisualTokens.SURFACE_SOFT,
                    checkedId == forever.getId() ? NativeVisualTokens.ACCENT
                            : NativeVisualTokens.LINE,
                    NativeLicenseUi.dp(activity, 11), NativeLicenseUi.dp(activity, 1)));
        });
        plans.check(forever.getId());
        plans.check(month.getId());
        sheet.body.addView(plans, NativeLicenseUi.spaced(activity, 6));

        sheet.body.addView(NativeLicenseUi.heading(activity, "Номер перевода"),
                NativeLicenseUi.spaced(activity, 12));
        EditText transaction = NativeLicenseUi.field(activity,
                "Уникальный номер или метка от 6 символов",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        sheet.body.addView(transaction, NativeLicenseUi.spaced(activity, 5));

        Button activate = NativeLicenseUi.button(activity, "Открыть доступ", true);
        sheet.body.addView(activate, NativeLicenseUi.spaced(activity, 12));

        TextView result = NativeLicenseUi.text(activity, "", 11.5f, false);
        sheet.body.addView(result, NativeLicenseUi.spaced(activity, 7));

        if (!NativeLicenseClient.enabled()) {
            check.setEnabled(false);
            activate.setEnabled(false);
            result.setText("Тестовая версия: сервер лицензий пока не подключён.");
        }

        check.setOnClickListener(v -> {
            String id = buyer.getText().toString().trim();
            if (id.isEmpty()) {
                buyer.setError("Введите ID покупателя");
                return;
            }
            check.setEnabled(false);
            current.setText("Проверяем покупателя…");
            client.lookupCustomer(key.getText().toString(), id, (ok, message, data) -> {
                if (sheet.visible()) {
                    check.setEnabled(true);
                    current.setText(ok ? displayExpiry(data) : message);
                }
            });
        });

        activate.setOnClickListener(v -> {
            String id = buyer.getText().toString().trim();
            String reference = transaction.getText().toString().trim();
            if (!id.matches("[a-fA-F0-9-]{36}")) {
                buyer.setError("Проверьте ID покупателя");
                return;
            }
            if (!reference.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{5,122}")) {
                transaction.setError("Введите уникальную метку от 6 символов");
                return;
            }
            if (key.getText().toString().length() < 32) {
                key.setError("Требуется секретный ключ владельца");
                return;
            }
            final boolean monthly = plans.getCheckedRadioButtonId() == month.getId();
            NativeLicenseUi.Sheet confirm = new NativeLicenseUi.Sheet(
                    activity, "Подтвердить перевод", "Проверка перед выдачей доступа");
            confirm.body.addView(NativeLicenseUi.notice(activity,
                    "Деньги должны реально поступить на ваш счёт. Скриншота недостаточно."),
                    NativeLicenseUi.spaced(activity, 9));
            confirm.body.addView(NativeLicenseUi.text(activity,
                    "Лицензия: " + (monthly ? "30 дней" : "навсегда"),
                    13f, true), NativeLicenseUi.spaced(activity, 12));
            confirm.body.addView(NativeLicenseUi.text(activity,
                    "Номер перевода: " + reference, 12f, false),
                    NativeLicenseUi.spaced(activity, 5));
            LinearLayout buttons = NativeLicenseUi.row(activity, 8);
            Button cancel = NativeLicenseUi.button(activity, "Отмена", false);
            Button approve = NativeLicenseUi.button(activity, "Деньги получены", true);
            buttons.addView(cancel, NativeLicenseUi.weighted(activity, 0));
            buttons.addView(approve, NativeLicenseUi.weighted(activity, 8));
            confirm.body.addView(buttons, NativeLicenseUi.spaced(activity, 14));
            cancel.setOnClickListener(view -> confirm.dismiss());
            approve.setOnClickListener(view -> {
                confirm.dismiss();
                activate.setEnabled(false);
                result.setText("Активируем лицензию…");
                client.grantManually(key.getText().toString(), id,
                        monthly ? "monthly" : "lifetime", reference,
                        (ok, message, details) -> {
                            if (sheet.visible()) {
                                activate.setEnabled(true);
                                result.setText(message);
                                if (ok) {
                                    transaction.setText("");
                                    current.setText(displayExpiry(details));
                                }
                            }
                        });
            });
            confirm.show();
        });

        sheet.show();
    }
}
