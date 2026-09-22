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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NativeDiagonalMeasureDialog extends Dialog {
    interface Listener {
        void onSave(float value);
        void onUseAuto(float value);
        void onDelete();
        void onClose();
    }

    private static final int TEXT=Color.rgb(35,56,72);
    private static final int MUTED=Color.rgb(116,136,151);
    private static final int BLUE=Color.rgb(47,143,190);
    private static final int LINE=Color.rgb(220,229,235);
    private static final int DANGER=Color.rgb(190,72,79);

    private final String titleText;
    private final Float autoValue;
    private final Listener listener;
    private String buffer;
    private TextView display;

    NativeDiagonalMeasureDialog(
            @NonNull Context context,
            @NonNull String title,
            float currentValue,
            @Nullable Float autoValue,
            @NonNull Listener listener){
        super(context);
        this.titleText=title;
        this.autoValue=autoValue;
        this.listener=listener;
        this.buffer=currentValue>0f?String.valueOf(Math.round(currentValue)):"0";
    }

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell=new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(12),dp(12),dp(12),dp(14));
        shell.setBackground(rounded(Color.WHITE,LINE,18,1));

        LinearLayout head=new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(titleText,17,TEXT,true),new LinearLayout.LayoutParams(0,dp(40),1f));
        Button x=button("×",false);
        x.setOnClickListener(v->listener.onClose());
        head.addView(x,new LinearLayout.LayoutParams(dp(42),dp(40)));
        shell.addView(head);

        shell.addView(text(
                "Расчётное значение — только подсказка. Для фактической геометрии введи измеренную диагональ.",
                11,MUTED,false));

        LinearLayout calc=new LinearLayout(getContext());
        calc.setOrientation(LinearLayout.VERTICAL);
        calc.setPadding(dp(10),dp(8),dp(10),dp(8));
        calc.setBackground(rounded(Color.rgb(247,250,252),LINE,11,1));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin=dp(9);
        shell.addView(calc,cp);
        calc.addView(text("Автоматический расчёт",10,MUTED,false));
        TextView auto=text(autoValue==null
                ?"Недостаточно размеров стен"
                :Math.round(autoValue)+" мм",17,TEXT,true);
        auto.setTag("native_diag_auto");
        calc.addView(auto);

        display=text(buffer,24,TEXT,true);
        display.setGravity(Gravity.CENTER);
        display.setTag("native_diag_value");
        display.setBackground(rounded(Color.rgb(250,252,253),LINE,11,1));
        LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(46));
        dpv.topMargin=dp(9);
        shell.addView(display,dpv);

        GridLayout grid=new GridLayout(getContext());
        grid.setColumnCount(3);
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin=dp(7);
        shell.addView(grid,gp);
        String[] keys={"1","2","3","4","5","6","7","8","9","Очистить","0","⌫"};
        for(String key:keys)addKey(grid,key);

        Button delete=button("Удалить диагональ",false);
        delete.setTextColor(DANGER);
        delete.setTag("native_diag_delete");
        delete.setOnClickListener(v->listener.onDelete());
        LinearLayout.LayoutParams delp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(34));
        delp.topMargin=dp(7);
        shell.addView(delete,delp);

        LinearLayout actions=new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(42));
        ap.topMargin=dp(7);
        shell.addView(actions,ap);
        Button cancel=button("Отмена",false);
        cancel.setOnClickListener(v->listener.onClose());
        Button save=button("Сохранить замер",true);
        save.setTag("native_diag_save");
        save.setOnClickListener(v->submit());
        actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(42),1f));
        actions.addView(save,new LinearLayout.LayoutParams(0,dp(42),1f));

        Button useAuto=button("Подставить расчётное (не замер)",false);
        useAuto.setTag("native_diag_use_auto");
        useAuto.setEnabled(autoValue!=null);
        useAuto.setAlpha(autoValue==null?.45f:1f);
        useAuto.setOnClickListener(v->{
            if(autoValue!=null)listener.onUseAuto(Math.round(autoValue));
        });
        LinearLayout.LayoutParams uap=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(42));
        uap.topMargin=dp(8);
        shell.addView(useAuto,uap);

        setContentView(shell);
        Window window=getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
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

    private void submit(){
        if(buffer.isEmpty()||"0".equals(buffer)){
            listener.onSave(0f);
            return;
        }
        try{
            listener.onSave(Float.parseFloat(buffer));
        }catch(Exception e){
            Toast.makeText(getContext(),
                    "Введите диагональ, например 4120",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void addKey(GridLayout grid,String key){
        Button b=button(key,false);
        b.setTextSize("Очистить".equals(key)?10:18);
        b.setOnClickListener(v->{
            if("Очистить".equals(key))buffer="0";
            else if("⌫".equals(key))buffer=buffer.length()<=1?"0":buffer.substring(0,buffer.length()-1);
            else{
                if("0".equals(buffer))buffer="";
                if(buffer.length()<7)buffer+=key;
            }
            display.setText(buffer);
        });
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();
        p.width=0;p.height=dp(42);
        p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        p.setMargins(dp(3),dp(3),dp(3),dp(3));
        grid.addView(b,p);
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setTextSize(13);
        b.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(primary?BLUE:Color.rgb(248,250,252),primary?BLUE:LINE,10,1));
        return b;
    }

    private TextView text(String value,float sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
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
