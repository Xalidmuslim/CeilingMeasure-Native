package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import java.util.Locale;

/** Amount/quantity editor delegates key arrangement and language to the user's Android IME. */
final class NativeNumberPadDialog extends Dialog {
    interface Listener { void onValue(float value); }

    private final String titleText;
    private final float initial;
    private final Listener listener;
    private final NativeUiPrefs prefs;
    private EditText input;

    NativeNumberPadDialog(
            @NonNull Context context,
            @NonNull String title,
            float initialValue,
            @NonNull Listener listener) {
        super(context);
        this.titleText = title;
        this.initial = initialValue;
        this.listener = listener;
        this.prefs = NativeUiPrefs.load(context);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell = new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(15),dp(14),dp(15),dp(15));
        shell.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,NativeVisualTokens.LINE,dp(19),dp(1)));
        shell.addView(text(titleText,17,true,NativeVisualTokens.TEXT));
        TextView hint=text("Введите значение с клавиатуры телефона",11.5f,false,
                NativeVisualTokens.MUTED);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin=dp(4);
        shell.addView(hint,hp);

        input=new EditText(getContext());
        input.setTag("native_number_pad_display");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        NativeTypography.register(input,22,true,prefs);
        input.setTextColor(NativeVisualTokens.TEXT);
        input.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        input.setPadding(dp(12),0,dp(12),0);
        input.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.APP_SOFT,NativeVisualTokens.LINE,dp(11),dp(1)));
        input.setText(Math.abs(initial - Math.round(initial)) < .01f
                ? String.valueOf(Math.round(initial))
                : String.format(Locale.US,"%.2f",initial).replaceAll("0+$","").replaceAll("\\.$",""));
        input.setOnEditorActionListener((v,actionId,event)->{
            if(actionId==EditorInfo.IME_ACTION_DONE){ submit(); return true; }
            return false;
        });
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(54));
        ip.topMargin=dp(11);
        shell.addView(input,ip);

        LinearLayout actions=new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(44));
        ap.topMargin=dp(10);
        shell.addView(actions,ap);

        Button cancel=button("Отмена",false);
        cancel.setTag("native_number_pad_cancel");
        cancel.setOnClickListener(v->dismiss());
        Button save=button("Готово",true);
        save.setTag("native_number_pad_done");
        save.setOnClickListener(v->submit());
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(0,dp(44),1f);
        l.rightMargin=dp(4);
        actions.addView(cancel,l);
        LinearLayout.LayoutParams r=new LinearLayout.LayoutParams(0,dp(44),1f);
        r.leftMargin=dp(4);
        actions.addView(save,r);

        setContentView(shell);
        NativeMotion.enterCompact(shell);
        Window window=getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            // Fade-only dialog transition. Never slide or scale the window.
            window.setWindowAnimations(R.style.CeilingFadeWindowAnimation);
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width=Math.min(getContext().getResources().getDisplayMetrics().widthPixels-dp(24),dp(430));
            lp.height=WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity=Gravity.CENTER;
            window.setAttributes(lp);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    |WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override protected void onStart(){
        super.onStart();
        if(input!=null){
            input.requestFocus();
            input.post(()->{
                InputMethodManager keyboard=(InputMethodManager)getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                if(keyboard!=null)keyboard.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    private void submit(){
        if(input==null)return;
        String raw=input.getText().toString().trim().replace(',','.');
        try{
            float value=Float.parseFloat(raw);
            if(!Float.isFinite(value)||value<0f)throw new NumberFormatException();
            listener.onValue(value);
            dismiss();
        }catch(NumberFormatException ignored){
            Toast.makeText(getContext(),"Введите корректное число",Toast.LENGTH_SHORT).show();
        }
    }

    private TextView text(String value,float sp,boolean bold,int color){
        TextView t=new TextView(getContext());
        t.setText(value);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t,sp,bold,prefs);
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(primary?Color.WHITE:NativeVisualTokens.TEXT);
        NativeTypography.register(b,13,true,prefs);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setBackground(NativeVisualTokens.controlButton(primary,dp(11)));
        return b;
    }

    private int dp(int value){
        return Math.round(value*getContext().getResources().getDisplayMetrics().density);
    }
}
