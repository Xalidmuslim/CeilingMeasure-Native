package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * One-off sales only. Uses a real, seller-configured external checkout link.
 * The app never handles card details and never trusts a buyer-supplied receipt
 * as proof of payment; seller reviews purchases on the license server.
 */
final class NativePurchaseDialog {
    private NativePurchaseDialog() {}

    private static TextView label(Activity activity, String value) {
        TextView item = new TextView(activity);
        item.setText(value);
        item.setTextSize(14);
        item.setPadding(0, 9, 0, 9);
        return item;
    }

    private static String safeCheckoutUrl(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            Uri uri = Uri.parse(value);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null
                    || uri.getUserInfo() != null) return "";
            host = host.toLowerCase(java.util.Locale.ROOT);
            if (host.equals("lava.top") || host.endsWith(".lava.top")
                    || host.equals("boosty.to") || host.endsWith(".boosty.to"))
                return value;
            return "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void openExternalCheckout(Activity activity, String url, TextView status) {
        String accepted = safeCheckoutUrl(url);
        if (accepted.isEmpty()) {
            status.setText("Продавец ещё не подключил безопасную ссылку для оплаты.");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(accepted));
            activity.startActivity(intent);
            status.setText("После оплаты вернитесь сюда и отправьте номер заказа для проверки.");
        } catch (ActivityNotFoundException ignored) {
            status.setText("На устройстве не найден браузер для оплаты.");
        }
    }

    static void show(Activity activity, NativeLicenseClient client, Runnable onChanged) {
        final int inset = Math.round(16 * activity.getResources().getDisplayMetrics().density);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout column = new LinearLayout(activity);
        column.setPadding(inset, inset / 2, inset, inset);
        column.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(column);

        column.addView(label(activity, "Обычная оплата российской банковской картой или через СБП. Без автосписаний."));

        TextView buyer = label(activity, "ID покупателя: " + client.userId());
        buyer.setTextIsSelectable(true);
        column.addView(buyer);
        Button copyBuyer = new Button(activity);
        copyBuyer.setText("Скопировать ID для продавца");
        column.addView(copyBuyer);

        TextView catalogueStatus = label(activity, "Проверяем доступность ссылок оплаты…");
        column.addView(catalogueStatus);

        RadioGroup selection = new RadioGroup(activity);
        selection.setOrientation(LinearLayout.VERTICAL);
        RadioButton monthlyOption = new RadioButton(activity);
        monthlyOption.setText("30 дней — 299 ₽");
        monthlyOption.setId(View.generateViewId());
        selection.addView(monthlyOption);
        RadioButton foreverOption = new RadioButton(activity);
        foreverOption.setText("Навсегда — 2 990 ₽");
        foreverOption.setId(View.generateViewId());
        selection.addView(foreverOption);
        selection.check(monthlyOption.getId());
        column.addView(selection);

        Button month = new Button(activity);
        month.setText("Оплатить 30 дней");
        month.setEnabled(false);
        column.addView(month);

        Button forever = new Button(activity);
        forever.setText("Купить навсегда");
        forever.setEnabled(false);
        column.addView(forever);

        column.addView(label(activity,
                "После оплаты укажите номер заказа. Продавец проверит поступление денег и активирует лицензию. " +
                "Код восстановления никому отправлять не нужно."));

        EditText receipt = new EditText(activity);
        receipt.setHint("Номер заказа / платежа (не менее 6 символов)");
        receipt.setSingleLine(true);
        receipt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        column.addView(receipt);

        Button send = new Button(activity);
        send.setText("Отправить заявку на активацию");
        column.addView(send);

        TextView result = label(activity, "");
        column.addView(result);
        column.addView(label(activity,
                "На стороне lava.top для первой покупки может потребоваться email. " +
                "В самом приложении email, пароль и платёжные реквизиты не нужны."));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Покупка лицензии")
                .setView(scroll)
                .setNegativeButton("Закрыть", null)
                .create();

        copyBuyer.setOnClickListener(v -> {
            if (client.userId().isEmpty()) return;
            ClipboardManager clipboard = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("ID покупателя",client.userId()));
                result.setText("ID скопирован. Не передавайте секретный код восстановления.");
            }
        });
        send.setOnClickListener(v -> {
            boolean isMonthly = selection.getCheckedRadioButtonId() == monthlyOption.getId();
            send.setEnabled(false);
            result.setText("Отправляем номер заказа на проверку…");
            client.submitPaymentClaim(isMonthly ? "monthly" : "lifetime",
                    receipt.getText().toString().trim(), (success,message) -> {
                        if (!activity.isFinishing() && dialog.isShowing()) {
                            send.setEnabled(true);
                            result.setText(message);
                            onChanged.run();
                        }
                    });
        });

        if (!NativeLicenseClient.enabled()) {
            catalogueStatus.setText("Сервис лицензий ещё не подключён. Покупка недоступна.");
            send.setEnabled(false);
        } else if (client.userId().isEmpty()) {
            catalogueStatus.setText("Сначала активируйте бесплатные 7 дней.");
            send.setEnabled(false);
        } else {
            client.loadCatalogue((catalogue,error) -> {
                if (activity.isFinishing() || !dialog.isShowing()) return;
                if (error != null) {
                    catalogueStatus.setText("Не удалось получить ссылки оплаты: " + error);
                    return;
                }
                JSONObject plans = catalogue.optJSONObject("plans");
                JSONObject monthData = plans == null ? null : plans.optJSONObject("monthly");
                JSONObject lifeData = plans == null ? null : plans.optJSONObject("lifetime");
                if (monthData == null || lifeData == null) {
                    catalogueStatus.setText("Варианты оплаты ещё не настроены.");
                    return;
                }
                int monthPrice = monthData.optInt("price_rub",299);
                int foreverPrice = lifeData.optInt("price_rub",2990);
                monthlyOption.setText("30 дней — " + monthPrice + " ₽");
                foreverOption.setText("Навсегда — " + foreverPrice + " ₽");

                String monthUrl = safeCheckoutUrl(monthData.optString("checkout_url",""));
                String foreverUrl = safeCheckoutUrl(lifeData.optString("checkout_url",""));
                month.setEnabled(!monthUrl.isEmpty());
                forever.setEnabled(!foreverUrl.isEmpty());
                month.setOnClickListener(v -> {
                    selection.check(monthlyOption.getId());
                    openExternalCheckout(activity,monthUrl,result);
                });
                forever.setOnClickListener(v -> {
                    selection.check(foreverOption.getId());
                    openExternalCheckout(activity,foreverUrl,result);
                });
                catalogueStatus.setText(monthUrl.isEmpty() && foreverUrl.isEmpty()
                        ? "Продавец ещё не добавил ссылки на оплату."
                        : "Ссылки готовы. Выберите вариант и перейдите к оплате.");
            });
        }
        dialog.show();
    }
}
