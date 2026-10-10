package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Lightweight activation dialog; no payment card data enters the APK. */
final class NativeLicenseDialog {
    private NativeLicenseDialog() {}

    static void show(Activity activity, NativeLicenseClient client, Runnable onChanged) {
        int pad = Math.round(18 * activity.getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);

        TextView description = new TextView(activity);
        description.setText(client.status() + "\n\n7 дней бесплатно после подтверждения почты. " +
                "Доступ на месяц и навсегда выдаётся после подтверждения оплаты продавцом.");
        description.setTextSize(14);
        layout.addView(description);

        EditText email = new EditText(activity);
        email.setHint("Электронная почта");
        email.setSingleLine(true);
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setText(client.email());
        layout.addView(email);

        EditText code = new EditText(activity);
        code.setHint("Шестизначный код из письма");
        code.setSingleLine(true);
        code.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(code);

        TextView result = new TextView(activity);
        result.setGravity(Gravity.CENTER_VERTICAL);
        result.setTextSize(13);
        layout.addView(result);

        Button request = new Button(activity);
        request.setText("Отправить код");
        layout.addView(request);
        Button enter = new Button(activity);
        enter.setText("Войти и активировать");
        layout.addView(enter);
        Button refresh = new Button(activity);
        refresh.setText("Проверить лицензию");
        layout.addView(refresh);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Лицензия — Замер потолка")
                .setView(layout)
                .setNegativeButton("Закрыть", null)
                .create();

        if (!NativeLicenseClient.enabled()) {
            result.setText("Сервер ещё не подключён. Пока действует прежний бесплатный режим.");
            email.setEnabled(false);
            code.setEnabled(false);
            request.setEnabled(false);
            enter.setEnabled(false);
            refresh.setEnabled(false);
        }
        request.setOnClickListener(view -> {
            String address = email.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
                email.setError("Укажи правильный адрес почты");
                return;
            }
            request.setEnabled(false);
            result.setText("Отправляем код…");
            client.requestCode(address, (ok, message) -> {
                if (!activity.isFinishing() && dialog.isShowing()) {
                    request.setEnabled(true);
                    result.setText(message);
                }
            });
        });
        enter.setOnClickListener(view -> {
            String address = email.getText().toString().trim();
            String otp = code.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
                email.setError("Проверь адрес");
                return;
            }
            if (!otp.matches("[0-9]{6}")) {
                code.setError("Введите 6 цифр");
                return;
            }
            enter.setEnabled(false);
            result.setText("Проверяем код…");
            client.verifyCode(address, otp, (ok, message) -> {
                if (!activity.isFinishing() && dialog.isShowing()) {
                    enter.setEnabled(true);
                    description.setText(client.status());
                    result.setText(message);
                    onChanged.run();
                }
            });
        });
        refresh.setOnClickListener(view -> {
            refresh.setEnabled(false);
            client.refresh((ok, message) -> {
                if (!activity.isFinishing() && dialog.isShowing()) {
                    refresh.setEnabled(true);
                    description.setText(client.status());
                    result.setText(message);
                    onChanged.run();
                }
            });
        });
        dialog.show();
    }
}
