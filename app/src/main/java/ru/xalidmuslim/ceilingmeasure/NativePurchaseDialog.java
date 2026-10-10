package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Manual direct-transfer purchase flow with no payment processor or card entry.
 * Buyer contacts seller, sends only public user ID, and waits for seller's
 * bank-receipt verification before license activation.
 */
final class NativePurchaseDialog {
    private NativePurchaseDialog() {}

    private static TextView text(Activity activity, String value, int sp) {
        TextView label = new TextView(activity);
        label.setText(value);
        label.setTextSize(sp);
        int p = Math.round(activity.getResources().getDisplayMetrics().density * 6);
        label.setPadding(0,p,0,p);
        return label;
    }

    private static String checkedPhone(String value) {
        return value != null && value.matches("[1-9][0-9]{7,14}") ? value : "";
    }

    private static void writeClipboard(Activity activity, String title, String value) {
        ClipboardManager manager=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if(manager!=null)manager.setPrimaryClip(ClipData.newPlainText(title,value));
    }

    private static void openWhatsApp(Activity activity, String number, String message, TextView status) {
        String safe = checkedPhone(number);
        if (safe.isEmpty()) {
            status.setText("Продавец ещё не указал номер WhatsApp.");
            return;
        }
        Uri uri=new Uri.Builder().scheme("https").authority("wa.me")
                .appendPath(safe).appendQueryParameter("text",message).build();
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,uri));
        } catch (ActivityNotFoundException ex) {
            status.setText("Не удалось открыть WhatsApp. Позвоните продавцу по указанному номеру.");
        }
    }

    static void show(Activity activity, NativeLicenseClient client, Runnable onChanged) {
        int inset=Math.round(16*activity.getResources().getDisplayMetrics().density);
        ScrollView scroll=new ScrollView(activity);
        LinearLayout column=new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(inset,inset,inset,inset);
        scroll.addView(column);

        column.addView(text(activity,
                "7 дней бесплатно. Затем свяжитесь с продавцом, переведите деньги по сообщённым им реквизитам — и он откроет доступ.",14));
        column.addView(text(activity,"Никаких автоматических списаний и оплаты внутри приложения.",13));
        TextView buyerId=text(activity,"Ваш ID покупателя: "+client.userId(),13);
        buyerId.setTextIsSelectable(true);
        column.addView(buyerId);

        Button copyId=new Button(activity);
        copyId.setText("Скопировать ID покупателя");
        column.addView(copyId);

        RadioGroup plans=new RadioGroup(activity);
        plans.setOrientation(LinearLayout.VERTICAL);
        RadioButton month=new RadioButton(activity);
        month.setId(View.generateViewId());
        month.setText("30 дней — 299 ₽");
        plans.addView(month);
        RadioButton lifetime=new RadioButton(activity);
        lifetime.setId(View.generateViewId());
        lifetime.setText("Навсегда — 2 990 ₽");
        plans.addView(lifetime);
        plans.check(month.getId());
        column.addView(plans);

        TextView contact=text(activity,"Загружаем номер продавца…",14);
        column.addView(contact);

        Button chat=new Button(activity);
        chat.setText("Написать в WhatsApp");
        chat.setEnabled(false);
        column.addView(chat);

        Button call=new Button(activity);
        call.setText("Позвонить продавцу");
        call.setEnabled(false);
        column.addView(call);

        Button refresh=new Button(activity);
        refresh.setText("Проверить, открыт ли доступ");
        column.addView(refresh);

        TextView result=text(activity,"",13);
        column.addView(result);
        column.addView(text(activity,
                "Важно: в России WhatsApp может не открываться. В этом случае позвоните продавцу. " +
                "Сообщайте ему только ID покупателя, но никогда секретный код восстановления.",12));

        AlertDialog dialog=new AlertDialog.Builder(activity)
                .setTitle("Приобрести лицензию")
                .setView(scroll)
                .setNegativeButton("Закрыть",null)
                .create();

        copyId.setOnClickListener(v -> {
            if (client.userId().isEmpty()) return;
            writeClipboard(activity,"ID Замера потолка",client.userId());
            result.setText("ID скопирован — отправьте его продавцу.");
        });

        refresh.setOnClickListener(v -> {
            refresh.setEnabled(false);
            result.setText("Проверяем лицензию на сервере…");
            client.refresh((ok,message) -> {
                if(!activity.isFinishing() && dialog.isShowing()) {
                    refresh.setEnabled(true);
                    result.setText(message);
                    onChanged.run();
                }
            });
        });

        if(!NativeLicenseClient.enabled()) {
            contact.setText("Сервер лицензий пока не подключён. Текущая бесплатная версия продолжает работать.");
            refresh.setEnabled(false);
        } else if(client.userId().isEmpty()) {
            contact.setText("Сначала начните бесплатный 7-дневный период в разделе «Лицензия».");
            refresh.setEnabled(false);
        } else {
            client.loadCatalogue((catalogue,error) -> {
                if (activity.isFinishing() || !dialog.isShowing()) return;
                if (error != null || catalogue == null) {
                    contact.setText("Не удалось загрузить номер. Проверьте интернет или попробуйте позже.");
                    return;
                }
                JSONObject plansJson=catalogue.optJSONObject("plans");
                JSONObject m=plansJson==null?null:plansJson.optJSONObject("monthly");
                JSONObject l=plansJson==null?null:plansJson.optJSONObject("lifetime");
                final int monthPrice=m==null?299:m.optInt("price_rub",299);
                final int foreverPrice=l==null?2990:l.optInt("price_rub",2990);
                month.setText("30 дней — "+monthPrice+" ₽");
                lifetime.setText("Навсегда — "+foreverPrice+" ₽");
                String whatsapp=checkedPhone(catalogue.optString("whatsapp_number",""));
                String telephone=checkedPhone(catalogue.optString("call_number",""));
                if (whatsapp.isEmpty() && telephone.isEmpty()) {
                    contact.setText("Продавец ещё не указал контакт. Покупка пока не открыта.");
                    return;
                }
                contact.setText("Связь с продавцом: +"+(!whatsapp.isEmpty()?whatsapp:telephone));
                chat.setEnabled(!whatsapp.isEmpty());
                call.setEnabled(!telephone.isEmpty());
                chat.setOnClickListener(v -> {
                    boolean monthly=plans.getCheckedRadioButtonId()==month.getId();
                    String name=monthly?"на 30 дней":"навсегда";
                    String price=monthly?monthPrice+" ₽":foreverPrice+" ₽";
                    String text="Здравствуйте! Хочу купить лицензию «Замер потолка» "+
                            name+" за "+price+". Мой ID покупателя: "+client.userId()+
                            ". Напишите, пожалуйста, как оплатить переводом.";
                    openWhatsApp(activity,whatsapp,text,result);
                });
                call.setOnClickListener(v -> {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:+"+telephone)));
                    } catch (ActivityNotFoundException ex) {
                        result.setText("Нет приложения для звонков. Номер: +"+telephone);
                    }
                });
            });
        }

        dialog.show();
    }
}
