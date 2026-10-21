package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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

    private static final int TEXT=NativeVisualTokens.TEXT;
    private static final int MUTED=NativeVisualTokens.MUTED;
    private static final int BLUE=NativeVisualTokens.ACCENT;
    private static final int LINE=NativeVisualTokens.LINE;
    private static final int DANGER=NativeVisualTokens.DANGER;

    private final String titleText;
    private final Float autoValue;
    private final Listener listener;
    private final NativeUiPrefs uiPrefs;
    private String buffer;
    private EditText display;

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
        this.uiPrefs=NativeUiPrefs.load(context);
        this.buffer=currentValue>0f?String.valueOf(Math.round(currentValue)):"0";
    }

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell=new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(12),dp(12),dp(12),dp(14));
        shell.setBackground(rounded(Color.WHITE,LINE,19,1));

        LinearLayout head=new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(titleText,17,TEXT,true),new LinearLayout.LayoutParams(0,dp(48),1f));
        Button x=button("×",false);
        x.setOnClickListener(v->listener.onClose());
        head.addView(x,new LinearLayout.LayoutParams(dp(48),dp(48)));
        shell.addView(head);

        shell.addView(text(
                "Расчётное значение — только подсказка. Для фактической геометрии введи измеренную диагональ.",
                11,MUTED,false));

        LinearLayout calc=new LinearLayout(getContext());
        calc.setOrientation(LinearLayout.VERTICAL);
        calc.setPadding(dp(10),dp(8),dp(10),dp(8));
        calc.setBackground(rounded(NativeVisualTokens.APP_SOFT,Color.TRANSPARENT,12,0));
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

        display=new EditText(getContext());
        display.setTag("native_diag_value");
        display.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        display.setImeOptions(EditorInfo.IME_ACTION_DONE);
        display.setSingleLine(true);
        display.setSelectAllOnFocus(true);
        display.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        NativeTypography.register(display,24,true,uiPrefs);
        display.setTextColor(TEXT);
        display.setGravity(Gravity.CENTER);
        display.setPadding(dp(12),0,dp(12),0);
        display.setBackground(rounded(NativeVisualTokens.SURFACE_SOFT,LINE,12,1));
        display.setText(buffer);
        display.setOnEditorActionListener((v,actionId,event)->{
            if(actionId==EditorInfo.IME_ACTION_DONE){submit();return true;}
            return false;
        });
        display.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
            @Override public void afterTextChanged(Editable s){buffer=s.toString();}
        });
        LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(54));
        dpv.topMargin=dp(9);
        shell.addView(display,dpv);

        Button delete=button("Удалить диагональ",false);
        delete.setTextColor(DANGER);
        delete.setTag("native_diag_delete");
        delete.setOnClickListener(v->listener.onDelete());
        LinearLayout.LayoutParams delp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48));
        delp.topMargin=dp(7);
        shell.addView(delete,delp);

        LinearLayout actions=new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48));
        ap.topMargin=dp(7);
        shell.addView(actions,ap);
        Button cancel=button("Отмена",false);
        cancel.setOnClickListener(v->listener.onClose());
        Button save=button("Сохранить замер",true);
        save.setTag("native_diag_save");
        save.setOnClickListener(v->submit());
        actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(48),1f));
        actions.addView(save,new LinearLayout.LayoutParams(0,dp(48),1f));

        Button useAuto=button("Подставить расчётное (не замер)",false);
        useAuto.setTag("native_diag_use_auto");
        useAuto.setEnabled(autoValue!=null);
        useAuto.setAlpha(autoValue==null?.45f:1f);
        useAuto.setOnClickListener(v->{
            if(autoValue!=null)listener.onUseAuto(Math.round(autoValue));
        });
        LinearLayout.LayoutParams uap=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48));
        uap.topMargin=dp(8);
        shell.addView(useAuto,uap);

        setContentView(shell);
        NativeMotion.enterCompact(shell);
        Window window=getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            // Fade-only dialog transition. Never slide or scale the window.
            window.setWindowAnimations(R.style.CeilingFadeWindowAnimation);
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width=Math.min(
                    getContext().getResources().getDisplayMetrics().widthPixels-dp(18),
                    dp(390));
            lp.height=WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity=Gravity.CENTER;
            window.setAttributes(lp);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    |WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override protected void onStart(){
        super.onStart();
        if(display!=null){
            display.requestFocus();
            display.post(()->{
                InputMethodManager keyboard=(InputMethodManager)getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                if(keyboard!=null)keyboard.showSoftInput(
                        display,InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    private void submit(){
        if(buffer.isEmpty()||"0".equals(buffer)){
            listener.onSave(0f);
            return;
        }
        try{
            listener.onSave(Float.parseFloat(buffer.replace(',','.')));
        }catch(Exception e){
            Toast.makeText(getContext(),
                    "Введите диагональ, например 4120",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void addKey(GridLayout grid,String key){
        Button b=button(key,false);
        NativeTypography.register(b,"⌫".equals(key)?22:24,true,uiPrefs);
        b.setPadding(0,0,0,0);
        b.setBackground(NativeVisualTokens.rippleOutlined(
                NativeVisualTokens.APP_SOFT,
                LINE,
                dp(18),
                dp(1),
                Color.argb(24,30,49,68)));
        b.setOnClickListener(v->{
            if("⌫".equals(key)){
                buffer=buffer.length()<=1?"0":buffer.substring(0,buffer.length()-1);
            }else if(",".equals(key)){
                if(!buffer.contains(",")&&!buffer.contains(".")){
                    buffer=("0".equals(buffer)||buffer.isEmpty())?"0,":buffer+",";
                }
            }else{
                if("0".equals(buffer))buffer="";
                if(buffer.length()<8)buffer+=key;
            }
            display.setText(buffer);
        });
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();
        p.width=0;p.height=dp(58);
        p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        p.setMargins(dp(4),dp(4),dp(4),dp(4));
        grid.addView(b,p);
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setTextColor(primary?Color.WHITE:TEXT);
        NativeTypography.register(b,13,true,uiPrefs);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primary
                ?NativeVisualTokens.controlButton(true,dp(10))
                :NativeVisualTokens.rippleOutlined(
                        NativeVisualTokens.APP_SOFT,
                        LINE,
                        dp(10),
                        dp(1),
                        Color.argb(24,30,49,68)));
        return b;
    }

    private TextView text(String value,float sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t,sp,bold,uiPrefs);
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
