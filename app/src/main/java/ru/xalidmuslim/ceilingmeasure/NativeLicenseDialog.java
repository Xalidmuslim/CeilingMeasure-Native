package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Registration-free license screen. All purchases are external to this activity. */
final class NativeLicenseDialog {
    private NativeLicenseDialog() {}

    static void show(Activity activity, NativeLicenseClient client, Runnable onChanged) {
        int dp = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp, dp, dp, dp);

        TextView status = new TextView(activity);
        status.setTextSize(15);
        layout.addView(status);

        TextView buyerId = new TextView(activity);
        buyerId.setTextSize(13);
        layout.addView(buyerId);

        Button copyBuyerId = new Button(activity);
        copyBuyerId.setText("Копировать ID покупателя для продавца");
        layout.addView(copyBuyerId);

        TextView recoveryTitle = new TextView(activity);
        recoveryTitle.setTextSize(13);
        recoveryTitle.setText("Секретный код восстановления. Сохраните его. Никому не сообщайте — даже продавцу.");
        layout.addView(recoveryTitle);

        EditText codeDisplay = new EditText(activity);
        codeDisplay.setSingleLine(true);
        codeDisplay.setTextIsSelectable(true);
        codeDisplay.setInputType(InputType.TYPE_CLASS_TEXT);
        codeDisplay.setEnabled(false);
        layout.addView(codeDisplay);

        Button copy = new Button(activity);
        copy.setText("Копировать мой код");
        layout.addView(copy);

        Button begin = new Button(activity);
        begin.setText("Начать 7 дней бесплатно");
        layout.addView(begin);

        Button refresh = new Button(activity);
        refresh.setText("Обновить лицензию");
        layout.addView(refresh);

        Button purchase = new Button(activity);
        purchase.setText("Связаться с продавцом / купить доступ");
        layout.addView(purchase);

        Button owner = new Button(activity);
        owner.setText("Режим владельца");
        layout.addView(owner);

        TextView restoreTitle = new TextView(activity);
        restoreTitle.setText("Сменили телефон? Введите ранее сохранённый код:");
        restoreTitle.setTextSize(13);
        layout.addView(restoreTitle);

        EditText recoveryInput = new EditText(activity);
        recoveryInput.setSingleLine(true);
        recoveryInput.setHint("cm1_…");
        recoveryInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        layout.addView(recoveryInput);

        Button restore = new Button(activity);
        restore.setText("Восстановить доступ");
        layout.addView(restore);

        TextView result = new TextView(activity);
        result.setTextSize(13);
        layout.addView(result);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(layout);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Лицензия — Замер потолка")
                .setView(scroll)
                .setNegativeButton("Закрыть", null)
                .create();

        Runnable render = () -> {
            boolean ready = NativeLicenseClient.enabled();
            boolean hasCode = !client.recoveryCode().isEmpty();
            status.setText(client.status());
            buyerId.setText(hasCode ? "ID покупателя: " + client.userId() : "ID появится после активации");
            copyBuyerId.setEnabled(hasCode);
            codeDisplay.setText(hasCode ? client.recoveryCode() : "Код появится после начала пробного периода");
            begin.setEnabled(ready && !hasCode);
            refresh.setEnabled(ready && hasCode);
            purchase.setEnabled(ready && hasCode);
            copy.setEnabled(hasCode);
            restore.setEnabled(ready);
            recoveryInput.setEnabled(ready);
            if (!ready) result.setText("Сервер лицензий ещё не подключён. Действует бесплатный режим.");
        };

        begin.setOnClickListener(view -> {
            begin.setEnabled(false);
            result.setText("Создаём пробную лицензию…");
            client.startTrial((ok, message) -> {
                if (!activity.isFinishing() && dialog.isShowing()) {
                    render.run();
                    result.setText(message);
                    onChanged.run();
                }
            });
        });

        refresh.setOnClickListener(view -> {
            refresh.setEnabled(false);
            result.setText("Проверяем лицензию…");
            client.refresh((ok, message) -> {
                if (!activity.isFinishing() && dialog.isShowing()) {
                    render.run();
                    result.setText(message);
                    onChanged.run();
                }
            });
        });

        restore.setOnClickListener(view -> {
            String entered=recoveryInput.getText().toString().trim();
            if (!entered.matches("cm1_[A-Za-z0-9_-]{43}")) {
                recoveryInput.setError("Проверьте полный код восстановления");
                return;
            }
            restore.setEnabled(false);
            result.setText("Восстанавливаем покупку…");
            client.restore(entered,(ok,message) -> {
                if (!activity.isFinishing() && dialog.isShowing()) {
                    render.run();
                    result.setText(message);
                    onChanged.run();
                }
            });
        });

        purchase.setOnClickListener(view -> {
            dialog.dismiss();
            NativePurchaseDialog.show(activity, client, onChanged);
        });

        owner.setOnClickListener(view -> {
            dialog.dismiss();
            NativeOwnerLicenseDialog.show(activity, client);
        });

        copyBuyerId.setOnClickListener(view -> {
            String id=client.userId();
            if (id.isEmpty()) return;
            ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("ID покупателя Замера потолка",id));
                result.setText("Отправьте продавцу только ID покупателя, не секретный код.");
            }
        });

        copy.setOnClickListener(view -> {
            String key=client.recoveryCode();
            if (key.isEmpty()) return;
            ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Код восстановления Замера потолка",key));
                result.setText("Код скопирован. Храните его в надёжном месте.");
            }
        });
        render.run();
        dialog.show();
    }
}
