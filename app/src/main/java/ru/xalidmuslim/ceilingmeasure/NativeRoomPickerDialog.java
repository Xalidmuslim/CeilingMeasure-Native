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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

final class NativeRoomPickerDialog extends Dialog {
    interface Listener { void onRoomType(String type); }

    private final Listener listener;

    NativeRoomPickerDialog(@NonNull Context context, @NonNull Listener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell = new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(12),dp(12),dp(12),dp(14));
        shell.setBackground(rounded(Color.WHITE,NativeVisualTokens.LINE,19,1));

        LinearLayout head = new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Добавить помещение",18,NativeVisualTokens.TEXT,true);
        head.addView(title,new LinearLayout.LayoutParams(0,dp(40),1f));
        Button close = button("×");
        close.setTag("native_room_picker_close");
        close.setOnClickListener(v->dismiss());
        head.addView(close,new LinearLayout.LayoutParams(dp(42),dp(40)));
        shell.addView(head);

        GridLayout grid = new GridLayout(getContext());
        grid.setColumnCount(2);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin=dp(8);
        shell.addView(grid,gp);

        add(grid,"","Комната");
        add(grid,"","Коридор");
        add(grid,"","Ванная");
        add(grid,"","Другое");

        TextView tip = text(
                "Название потом можно изменить долгим нажатием по комнате.",
                11,
                NativeVisualTokens.MUTED,
                false);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin=dp(8);
        shell.addView(tip,tp);

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
                    dp(360));
            lp.height=WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity=Gravity.CENTER;
            window.setAttributes(lp);
        }
    }

    private void add(GridLayout grid,String icon,String label){
        Button b=button(label);
        b.setTag("native_room_type_"+label);
        b.setOnClickListener(v->{dismiss();listener.onRoomType(label);});
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();
        p.width=0;
        p.height=dp(62);
        p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        p.setMargins(dp(4),dp(4),dp(4),dp(4));
        grid.addView(b,p);
    }

    private Button button(String value){
        Button b=new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setTextSize(13);
        b.setTextColor(NativeVisualTokens.TEXT);
        b.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        b.setGravity(Gravity.CENTER);
        b.setBackground(NativeVisualTokens.rippleOutlined(
                NativeVisualTokens.SURFACE_SOFT,NativeVisualTokens.LINE,
                dp(13),dp(1),Color.argb(20,33,110,147)));
        return b;
    }

    private TextView text(String value,int sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private GradientDrawable rounded(int fill,int stroke,int radius,int strokeWidth){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if(strokeWidth>0)d.setStroke(dp(strokeWidth),stroke);
        return d;
    }

    private int dp(int value){
        return Math.round(value*getContext().getResources().getDisplayMetrics().density);
    }
}
