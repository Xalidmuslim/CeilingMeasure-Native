package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Locale;

final class NativeWallMeasureDialog extends Dialog {
    interface Listener {
        void onSave(float value);
        void onDelete();
        void onBack();
        void onClose();
    }

    private static final int TEXT=NativeVisualTokens.TEXT;
    private static final int MUTED=NativeVisualTokens.MUTED;
    private static final int BLUE=NativeVisualTokens.ACCENT;
    private static final int LINE=NativeVisualTokens.LINE;
    private static final int DANGER=NativeVisualTokens.DANGER;

    private final String titleText;
    private final boolean sequence;
    private final Listener listener;
    private final NativeUiPrefs uiPrefs;
    private String buffer;
    private TextView display;
    private TextView meterHint;

    NativeWallMeasureDialog(
            @NonNull Context context,
            @NonNull String title,
            float currentValue,
            boolean sequence,
            @NonNull Listener listener) {
        super(context);
        this.titleText=title;
        this.sequence=sequence;
        this.listener=listener;
        this.uiPrefs=NativeUiPrefs.load(context);
        this.buffer=currentValue>0f
                ?(Math.abs(currentValue-Math.round(currentValue))<.01f
                    ?String.valueOf(Math.round(currentValue))
                    :String.format(Locale.US,"%.2f",currentValue).replace('.',','))
                :"0";
    }

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(false);

        LinearLayout shell=new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(10),dp(9),dp(10),dp(9));
        shell.setBackground(rounded(Color.WHITE,LINE,18,1));

        LinearLayout head=new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text(titleText,15,TEXT,true);
        head.addView(title,new LinearLayout.LayoutParams(0,dp(34),1f));
        Button x=button("×",false);
        x.setTag("native_wall_dismiss");
        x.setOnClickListener(v->listener.onClose());
        head.addView(x,new LinearLayout.LayoutParams(dp(40),dp(34)));
        shell.addView(head);

        TextView sub=text(
                sequence
                        ?"Нажми на любую сторону. Можно вводить 58 или 0,58 м."
                        :"Размер стены · можно вводить 58 или 0,58 м",
                10,
                MUTED,
                false);
        shell.addView(sub);

        display=text(buffer,26,TEXT,true);
        display.setGravity(Gravity.CENTER);
        display.setTag("native_wall_value");
        display.setBackground(rounded(Color.rgb(250,252,253),LINE,14,1));
        LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(52));
        dpv.topMargin=dp(7);
        shell.addView(display,dpv);

        meterHint=text("",10,MUTED,false);
        meterHint.setTag("native_wall_meter_hint");
        LinearLayout.LayoutParams mhp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mhp.topMargin=dp(3);
        shell.addView(meterHint,mhp);

        GridLayout keypad=new GridLayout(getContext());
        keypad.setTag("native_wall_keypad");
        keypad.setColumnCount(3);
        LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        kp.topMargin=dp(5);
        shell.addView(keypad,kp);

        String[] keys={"1","2","3","4","5","6","7","8","9",",","0","⌫"};
        for(String key:keys)addKey(keypad,key);

        Button delete=button("Удалить стену",false);
        delete.setTag("native_wall_delete");
        delete.setTextColor(DANGER);
        delete.setVisibility(sequence?View.GONE:View.VISIBLE);
        delete.setOnClickListener(v->listener.onDelete());
        LinearLayout.LayoutParams delp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(32));
        delp.topMargin=dp(5);
        shell.addView(delete,delp);

        LinearLayout actions=new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(39));
        ap.topMargin=dp(6);
        shell.addView(actions,ap);

        Button back=button(sequence?"Назад":"Закрыть",false);
        back.setTag("native_wall_back");
        back.setOnClickListener(v->{
            if(sequence)listener.onBack();
            else listener.onClose();
        });
        Button done=button("Готово",true);
        done.setTag("native_wall_done");
        done.setOnClickListener(v->submit());

        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,dp(39),1f);
        half.rightMargin=dp(3);
        actions.addView(back,new LinearLayout.LayoutParams(half));
        LinearLayout.LayoutParams half2=new LinearLayout.LayoutParams(0,dp(39),1f);
        half2.leftMargin=dp(3);
        actions.addView(done,half2);

        setContentView(shell);
        NativeMotion.enterCompact(shell);
        refresh();

        setOnKeyListener((dialog,keyCode,event)->{
            if(keyCode==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP){
                if(sequence)listener.onBack(); else listener.onClose();
                return true;
            }
            return false;
        });

        Window window=getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            if(sequence)window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width=Math.min(
                    getContext().getResources().getDisplayMetrics().widthPixels,
                    dp(720));
            lp.height=WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
            window.setAttributes(lp);
        }
    }

    Float currentUnitsOrNull(){
        return parseUnits(buffer);
    }

    String rawValueForTest(){
        return buffer;
    }

    private void submit(){
        Float value=parseUnits(buffer);
        if(sequence&&(value==null||value<=0f)){
            Toast.makeText(getContext(),"Введите размер стены",Toast.LENGTH_SHORT).show();
            return;
        }
        if(!"0".equals(buffer)&&value==null){
            Toast.makeText(getContext(),"Введите размер, например 58 или 0,58",Toast.LENGTH_SHORT).show();
            return;
        }
        listener.onSave(value==null?0f:value);
    }

    private void addKey(GridLayout grid,String key){
        Button b=button(key,false);
        NativeTypography.register(b,"⌫".equals(key)?22:24,true,uiPrefs);
        b.setTag(",".equals(key) ? "native_wall_decimal" : "native_wall_key_"+key);
        b.setPadding(0,0,0,0);
        b.setBackground(rounded(Color.rgb(248,250,252),LINE,18,1));
        b.setOnClickListener(v->press(key));
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();
        p.width=0;
        p.height=dp(58);
        p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        p.setMargins(dp(4),dp(4),dp(4),dp(4));
        grid.addView(b,p);
    }

    private void press(String key){
        if("⌫".equals(key)){
            buffer=buffer.length()<=1?"0":buffer.substring(0,buffer.length()-1);
        }else if(",".equals(key)){
            if(!buffer.contains(",")&&!buffer.contains(".")){
                buffer=("0".equals(buffer)||buffer.isEmpty())?"0,":buffer+",";
            }
        }else if(key.matches("\\d")){
            if("0".equals(buffer))buffer="";
            if(buffer.length()<7)buffer+=key;
        }
        refresh();
    }

    private void refresh(){
        if(display!=null)display.setText(buffer.isEmpty()?"0":buffer);
        if(meterHint!=null){
            Float units=parseUnits(buffer);
            meterHint.setText(units!=null&&units>0f
                    ?"Это "+formatMeters(units)+" м"
                    :"Введите размер стены");
        }
    }

    private Float parseUnits(String raw){
        String s=String.valueOf(raw).trim().replace(',','.');
        if(s.isEmpty()||"0".equals(s))return null;
        try{
            if(s.matches("\\d+")){
                float n=Float.parseFloat(s);
                return n>0?n:null;
            }
            if(s.matches("\\d+\\.\\d{1,2}")){
                float metres=Float.parseFloat(s);
                return metres>0?(float)Math.round(metres*100f):null;
            }
        }catch(Exception ignored){}
        return null;
    }

    private String formatMeters(float units){
        float metres=units/100f;
        return Math.abs(metres-Math.round(metres))<.001f
                ?String.valueOf(Math.round(metres))
                :String.format(Locale.getDefault(),"%.2f",metres)
                        .replaceAll("0+$","")
                        .replaceAll("[,.]$","");
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setTextColor(primary?Color.WHITE:TEXT);
        NativeTypography.register(b,13,true,uiPrefs);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(4),0,dp(4),0);
        b.setBackground(rounded(primary?BLUE:Color.rgb(248,250,252),primary?BLUE:LINE,9,1));
        return b;
    }

    private TextView text(String value,float sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t,sp,bold,uiPrefs);
    }

    private GradientDrawable rounded(int fill,int stroke,int radius,int width){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if(width>0)d.setStroke(dp(width),stroke);
        return d;
    }

    private int dp(int v){
        return Math.round(v*getContext().getResources().getDisplayMetrics().density);
    }
}
