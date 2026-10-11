package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Compact license overview, recovery and purchase entry.
 * All security decisions remain in NativeLicenseClient and the license server.
 */
final class NativeLicenseDialog {
    private NativeLicenseDialog() {}

    private static void copy(Activity activity, String label, String value) {
        ClipboardManager clip = (ClipboardManager)
                activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clip != null && value != null && !value.isEmpty())
            clip.setPrimaryClip(ClipData.newPlainText(label, value));
    }

    static void show(Activity activity, NativeLicenseClient client, Runnable onChanged) {
        NativeLicenseUi.Sheet sheet = new NativeLicenseUi.Sheet(
                activity, "Лицензия", "Замер потолков · управление доступом");

        TextView status = NativeLicenseUi.notice(activity, client.status());
        sheet.body.addView(status, NativeLicenseUi.spaced(activity, 12));

        LinearLayout idPanel = NativeLicenseUi.panel(activity);
        LinearLayout idHead = NativeLicenseUi.row(activity, 0);
        idHead.addView(NativeLicenseUi.heading(activity, "ID покупателя"),
                NativeLicenseUi.weighted(activity, 0));
        TextView copyId = NativeLicenseUi.inlineLink(activity, "Копировать");
        idHead.addView(copyId);
        idPanel.addView(idHead, NativeLicenseUi.match());

        TextView userId = NativeLicenseUi.text(activity, "", 12f, false);
        userId.setSingleLine(true);
        userId.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        userId.setTextIsSelectable(true);
        idPanel.addView(userId, NativeLicenseUi.spaced(activity, 6));
        sheet.body.addView(idPanel, NativeLicenseUi.spaced(activity, 10));

        LinearLayout actions = NativeLicenseUi.row(activity, 8);
        Button begin = NativeLicenseUi.button(activity, "Начать 7 дней", true);
        Button refresh = NativeLicenseUi.button(activity, "Проверить доступ", false);
        actions.addView(begin, NativeLicenseUi.weighted(activity, 0));
        actions.addView(refresh, NativeLicenseUi.weighted(activity, 8));
        sheet.body.addView(actions, NativeLicenseUi.spaced(activity, 10));

        Button purchase = NativeLicenseUi.button(activity, "Тарифы и связь с продавцом", true);
        sheet.body.addView(purchase, NativeLicenseUi.spaced(activity, 8));
        TextView prices = NativeLicenseUi.text(activity,
                "30 дней — 299 ₽    ·    Навсегда — 1 900 ₽", 12f, false);
        prices.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sheet.body.addView(prices, NativeLicenseUi.spaced(activity, 6));

        LinearLayout restoreHead = NativeLicenseUi.row(activity, 0);
        restoreHead.addView(NativeLicenseUi.heading(activity, "Восстановление доступа"),
                NativeLicenseUi.weighted(activity, 0));
        TextView expand = NativeLicenseUi.inlineLink(activity, "Показать");
        restoreHead.addView(expand);
        sheet.body.addView(restoreHead, NativeLicenseUi.spaced(activity, 14));

        LinearLayout restoreArea = NativeLicenseUi.panel(activity);
        restoreArea.setVisibility(View.GONE);
        restoreArea.addView(NativeLicenseUi.text(activity,
                "Секретный код нужен при смене телефона. Не отправляйте его продавцу.",
                11.5f, false), NativeLicenseUi.match());

        TextView recovery = NativeLicenseUi.text(activity, "", 12f, true);
        recovery.setSingleLine(true);
        recovery.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        recovery.setTextIsSelectable(true);
        restoreArea.addView(recovery, NativeLicenseUi.spaced(activity, 8));

        Button copyRecovery = NativeLicenseUi.button(activity, "Скопировать секретный код", false);
        restoreArea.addView(copyRecovery, NativeLicenseUi.spaced(activity, 5));

        EditText restoreInput = NativeLicenseUi.field(activity, "Введите сохранённый код cm1_…",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        restoreArea.addView(restoreInput, NativeLicenseUi.spaced(activity, 12));

        Button restore = NativeLicenseUi.button(activity, "Восстановить лицензию", false);
        restoreArea.addView(restore, NativeLicenseUi.spaced(activity, 7));
        sheet.body.addView(restoreArea, NativeLicenseUi.spaced(activity, 6));

        TextView result = NativeLicenseUi.text(activity, "", 12f, false);
        sheet.body.addView(result, NativeLicenseUi.spaced(activity, 8));

        TextView owner = NativeLicenseUi.inlineLink(activity, "Для владельца · управление лицензиями ›");
        owner.setTextSize(11.5f);
        owner.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sheet.body.addView(owner, NativeLicenseUi.spaced(activity, 4));

        Runnable render = () -> {
            boolean connected = NativeLicenseClient.enabled();
            boolean registered = !client.recoveryCode().isEmpty();
            status.setText(client.status());
            userId.setText(registered ? client.userId() : "Появится после начала пробного периода");
            recovery.setText(registered ? client.recoveryCode() : "Код ещё не создан");
            copyId.setEnabled(registered);
            copyId.setAlpha(registered ? 1f : .42f);
            copyRecovery.setEnabled(registered);
            begin.setEnabled(connected && !registered);
            refresh.setEnabled(connected && registered);
            restore.setEnabled(connected);
            restoreInput.setEnabled(connected);
            // The tariff sheet remains previewable, but all payments are
            // disabled until a verified license server is connected.
            purchase.setEnabled(true);
            if (!connected) {
                result.setText("Тестовая версия: сервер не подключён, доступ остаётся бесплатным.");
            }
        };

        expand.setOnClickListener(v -> {
            boolean open = restoreArea.getVisibility() != View.VISIBLE;
            restoreArea.setVisibility(open ? View.VISIBLE : View.GONE);
            expand.setText(open ? "Скрыть" : "Показать");
        });

        copyId.setOnClickListener(v -> {
            copy(activity, "ID покупателя Замер потолков", client.userId());
            result.setText("ID скопирован. Передавайте продавцу только ID.");
        });

        copyRecovery.setOnClickListener(v -> {
            copy(activity, "Секретный код восстановления", client.recoveryCode());
            result.setText("Код скопирован. Храните его в надёжном месте.");
        });

        begin.setOnClickListener(v -> {
            begin.setEnabled(false);
            result.setText("Активируем пробный период…");
            client.startTrial((ok, message) -> {
                if (sheet.visible()) {
                    render.run();
                    result.setText(message);
                    onChanged.run();
                }
            });
        });

        refresh.setOnClickListener(v -> {
            refresh.setEnabled(false);
            result.setText("Проверяем доступ…");
            client.refresh((ok, message) -> {
                if (sheet.visible()) {
                    render.run();
                    result.setText(message);
                    onChanged.run();
                }
            });
        });

        restore.setOnClickListener(v -> {
            String code = restoreInput.getText().toString().trim();
            if (!code.matches("cm1_[A-Za-z0-9_-]{43}")) {
                restoreInput.setError("Проверьте полный код восстановления");
                return;
            }
            restore.setEnabled(false);
            result.setText("Восстанавливаем лицензию…");
            client.restore(code, (ok, message) -> {
                if (sheet.visible()) {
                    render.run();
                    result.setText(message);
                    onChanged.run();
                }
            });
        });

        purchase.setOnClickListener(v -> {
            sheet.dismiss();
            NativePurchaseDialog.show(activity, client, onChanged);
        });

        owner.setOnClickListener(v -> {
            sheet.dismiss();
            NativeOwnerLicenseDialog.show(activity, client);
        });

        render.run();
        sheet.show();
    }
}
