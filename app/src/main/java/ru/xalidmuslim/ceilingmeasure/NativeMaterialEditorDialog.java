package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;

final class NativeMaterialEditorDialog extends Dialog {
    interface Listener {
        void onSave(NativeProjectStore.MaterialData material);
    }

    private static final int TEXT=NativeVisualTokens.TEXT;
    private static final int MUTED=NativeVisualTokens.MUTED;
    private static final int BLUE=NativeVisualTokens.ACCENT;
    private static final int LINE=NativeVisualTokens.LINE;
    private static final int BG=NativeVisualTokens.APP_SOFT;

    private final String titleText;
    private final boolean qtyEditable;
    private final Listener listener;
    private final NativeProjectStore.MaterialData draft;
    private final NativeUiPrefs uiPrefs;
    private Button nameButton;
    private Button qtyButton;
    private Button priceButton;
    private final java.util.ArrayList<Button> unitButtons=new java.util.ArrayList<>();

    NativeMaterialEditorDialog(
            @NonNull Context context,
            @NonNull String title,
            NativeProjectStore.MaterialData initial,
            boolean qtyEditable,
            @NonNull Listener listener) {
        super(context);
        this.titleText=title;
        this.qtyEditable=qtyEditable;
        this.listener=listener;
        this.uiPrefs=NativeUiPrefs.load(context);
        this.draft=initial==null?new NativeProjectStore.MaterialData():initial.copy();
        if(this.draft.name==null||this.draft.name.trim().isEmpty())this.draft.name="Материал";
        this.draft.unit=NativeProjectStore.normalizeMaterialUnit(this.draft.unit);
        if(qtyEditable&&this.draft.qty<=0f)this.draft.qty=1f;
    }

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell=new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(10),dp(9),dp(10),dp(10));
        shell.setBackground(rounded(Color.WHITE,LINE,19,1));

        LinearLayout head=new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(titleText,15.5f,TEXT,true),new LinearLayout.LayoutParams(0,dp(48),1f));
        Button close=button("×",false);
        close.setOnClickListener(v->dismiss());
        head.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));
        shell.addView(head);

        shell.addView(text("Название",10,MUTED,false),top(4));
        nameButton=button(draft.name,false);
        nameButton.setTag("native_material_name");
        nameButton.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        nameButton.setPadding(dp(12),0,dp(12),0);
        nameButton.setOnClickListener(v->new NativeTextPadDialog(
                getContext(),
                "Название материала",
                draft.name,
                value->{
                    draft.name=value.trim();
                    nameButton.setText(draft.name);
                }).show());
        shell.addView(nameButton,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));

        shell.addView(text("Единица",10,MUTED,false),top(5));
        LinearLayout units=new LinearLayout(getContext());
        units.setOrientation(LinearLayout.HORIZONTAL);
        String[] values={"шт","м","м²","компл."};
        for(String unit:values){
            Button b=button(unit,false);
            b.setTag("native_material_unit_"+unit);
            b.setOnClickListener(v->{
                draft.unit=unit;
                refreshUnits();
            });
            units.addView(b,new LinearLayout.LayoutParams(0,dp(48),1f));
            unitButtons.add(b);
        }
        shell.addView(units,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));
        refreshUnits();

        if(qtyEditable){
            shell.addView(text("Количество",10,MUTED,false),top(5));
            qtyButton=button(format(draft.qty),false);
            qtyButton.setTag("native_material_qty");
            qtyButton.setOnClickListener(v->new NativeNumberPadDialog(
                    getContext(),
                    draft.name+" · количество",
                    draft.qty,
                    value->{
                        draft.qty=Math.max(0f,value);
                        qtyButton.setText(format(draft.qty));
                    }).show());
            shell.addView(qtyButton,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));
        }

        shell.addView(text("Цена",10,MUTED,false),top(5));
        priceButton=button(format(draft.price),false);
        priceButton.setTag("native_material_price");
        priceButton.setOnClickListener(v->new NativeNumberPadDialog(
                getContext(),
                draft.name+" · цена",
                draft.price,
                value->{
                    draft.price=Math.max(0f,value);
                    priceButton.setText(format(draft.price));
                }).show());
        shell.addView(priceButton,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));

        LinearLayout actions=new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48));
        ap.topMargin=dp(8);
        shell.addView(actions,ap);
        Button cancel=button("Отмена",false);
        cancel.setOnClickListener(v->dismiss());
        Button save=button("Сохранить",true);
        save.setTag("native_material_save");
        save.setOnClickListener(v->{
            String name=draft.name==null?"":draft.name.trim();
            if(name.isEmpty())return;
            draft.name=name;
            draft.unit=NativeProjectStore.normalizeMaterialUnit(draft.unit);
            listener.onSave(draft.copy());
            dismiss();
        });
        actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(48),1f));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(48),1f);
        sp.leftMargin=dp(6);
        actions.addView(save,sp);

        setContentView(shell);
        NativeMotion.enterCompact(shell);
        Window window=getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            // Fade-only dialog transition. Never slide or scale the window.
            window.setWindowAnimations(R.style.Animation_CeilingFade);
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width=Math.min(
                    getContext().getResources().getDisplayMetrics().widthPixels-dp(18),
                    dp(390));
            lp.height=WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity=Gravity.CENTER;
            window.setAttributes(lp);
        }
    }

    private void refreshUnits(){
        for(Button b:unitButtons){
            boolean active=String.valueOf(b.getText()).equals(draft.unit);
            b.setTextColor(active?BLUE:TEXT);
            b.setBackground(rounded(
                    active?NativeVisualTokens.ACCENT_SOFT:NativeVisualTokens.APP_SOFT,
                    active?NativeVisualTokens.ACCENT:Color.TRANSPARENT,
                    11,active?1:0));
        }
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        NativeTypography.register(b,12,true,uiPrefs);
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(5),0,dp(5),0);
        b.setBackground(NativeVisualTokens.controlButton(primary,dp(9)));
        return b;
    }

    private TextView text(String value,float sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t,sp,bold,uiPrefs);
    }

    private LinearLayout.LayoutParams top(int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(margin);
        return p;
    }

    private String format(float value){
        return String.format(Locale.getDefault(),"%.2f",value)
                .replaceAll("0+$","")
                .replaceAll("[,.]$","");
    }

    private GradientDrawable rounded(int fill,int stroke,int radius,int width){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(dp(radius));
        if(width>0)d.setStroke(dp(width),stroke);
        return d;
    }

    private int dp(int v){
        return Math.round(v*getContext().getResources().getDisplayMetrics().density);
    }
}
