package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/**
 * Secure manual licensing on the seller's own Android phone.
 * The admin API key is typed each time; never written to disk or embedded
 * in the APK. Bank transfer confirmation always happens outside the app.
 */
final class NativeOwnerLicenseDialog {
    private NativeOwnerLicenseDialog() {}

    private static TextView text(Activity activity, String value) {
        TextView label = new TextView(activity);
        label.setText(value);
        label.setTextSize(13);
        int pad=Math.round(5*activity.getResources().getDisplayMetrics().density);
        label.setPadding(0,pad,0,pad);
        return label;
    }

    private static String displayExpiry(JSONObject object) {
        if (object == null) return "Покупатель не найден";
        String kind = object.optString("status",object.optString("kind","unknown"));
        String label;
        switch (kind) {
            case "trial": label="Пробный период"; break;
            case "monthly": label="Оплачен доступ на 30 дней"; break;
            case "lifetime": label="Бессрочная лицензия"; break;
            default: label="Срок доступа истёк"; break;
        }
        if (!object.isNull("access_until") && object.has("access_until")) {
            long until=object.optLong("access_until",0L);
            if (until>0L) label+=". Срок до: "+DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(until));
        }
        return label;
    }

    static void show(Activity activity, NativeLicenseClient client) {
        int p=Math.round(16*activity.getResources().getDisplayMetrics().density);
        ScrollView scroll=new ScrollView(activity);
        LinearLayout body=new LinearLayout(activity);
        body.setPadding(p,p,p,p);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);

        body.addView(text(activity,
                "Только для владельца. Здесь можно открыть покупателю 30 дней или бессрочный доступ " +
                "после проверки реального поступления перевода в банковском приложении."));

        EditText key=new EditText(activity);
        key.setHint("Секретный ключ владельца");
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(key);

        EditText buyer=new EditText(activity);
        buyer.setHint("ID покупателя из WhatsApp");
        buyer.setSingleLine(true);
        buyer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        body.addView(buyer);

        Button check=new Button(activity);
        check.setText("Проверить покупателя");
        body.addView(check);

        TextView current=text(activity,"");
        body.addView(current);

        RadioGroup plans=new RadioGroup(activity);
        plans.setOrientation(LinearLayout.VERTICAL);
        RadioButton monthly=new RadioButton(activity);
        monthly.setId(android.view.View.generateViewId());
        monthly.setText("Открыть на 30 дней");
        plans.addView(monthly);
        RadioButton lifetime=new RadioButton(activity);
        lifetime.setId(android.view.View.generateViewId());
        lifetime.setText("Открыть навсегда");
        plans.addView(lifetime);
        plans.check(monthly.getId());
        body.addView(plans);

        EditText transaction=new EditText(activity);
        transaction.setHint("Уникальный номер подтверждённого перевода");
        transaction.setSingleLine(true);
        transaction.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        body.addView(transaction);
        body.addView(text(activity,
                "Для каждого перевода укажите отдельный номер или учётную метку длиной от 6 символов, " +
                "без пробелов. Один номер нельзя активировать дважды."));

        Button activate=new Button(activity);
        activate.setText("Открыть доступ после проверки оплаты");
        body.addView(activate);

        TextView result=text(activity,"");
        body.addView(result);

        AlertDialog dialog=new AlertDialog.Builder(activity)
                .setTitle("Режим владельца")
                .setView(scroll)
                .setNegativeButton("Закрыть",null)
                .create();

        check.setOnClickListener(v -> {
            check.setEnabled(false);
            current.setText("Проверяем ID покупателя…");
            client.lookupCustomer(key.getText().toString(),
                    buyer.getText().toString().trim(),(ok,message,data) -> {
                if(!activity.isFinishing() && dialog.isShowing()) {
                    check.setEnabled(true);
                    current.setText(ok ? displayExpiry(data) : message);
                }
            });
        });

        activate.setOnClickListener(v -> {
            String id=buyer.getText().toString().trim();
            String reference=transaction.getText().toString().trim();
            if (!id.matches("[a-fA-F0-9-]{36}")) {
                buyer.setError("Проверьте ID покупателя");return;
            }
            if (!reference.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{5,127}")) {
                transaction.setError("Нужен уникальный номер перевода, минимум 6 символов");return;
            }
            if (key.getText().toString().length()<32) {
                key.setError("Введите секретный ключ владельца");return;
            }
            boolean isMonthly=plans.getCheckedRadioButtonId()==monthly.getId();
            String period=isMonthly?"30 дней":"навсегда";
            new AlertDialog.Builder(activity)
                    .setTitle("Подтверждение оплаты")
                    .setMessage("Ты лично проверил, что перевод за лицензию «Замер потолка» " +
                            "поступил на банковский счёт?\n\nID: "+id+"\nЛицензия: "+period+
                            "\nНомер перевода: "+reference+
                            "\n\nСкриншота от покупателя недостаточно.")
                    .setNegativeButton("Отмена",null)
                    .setPositiveButton("Да, деньги получены",(confirmed,which) -> {
                        activate.setEnabled(false);
                        result.setText("Выдаём подписанную лицензию…");
                        client.grantManually(
                                key.getText().toString(),id,
                                isMonthly?"monthly":"lifetime",reference,
                                (success,message,details) -> {
                                    if(!activity.isFinishing() && dialog.isShowing()) {
                                        activate.setEnabled(true);
                                        result.setText(message);
                                        if(success) {
                                            transaction.setText("");
                                            current.setText(displayExpiry(details));
                                        }
                                    }
                                });
                    }).show();
        });
        dialog.show();
    }
}
