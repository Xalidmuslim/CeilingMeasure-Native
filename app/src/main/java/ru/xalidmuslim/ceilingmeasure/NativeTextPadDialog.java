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

/** Text editor uses the installed Russian/Latin keyboard, autocorrect and system layout. */
final class NativeTextPadDialog extends Dialog {
    interface Listener { void onValue(String value); }

    private final String titleText;
    private final String initial;
    private final Listener listener;
    private final NativeUiPrefs prefs;
    private EditText input;

    NativeTextPadDialog(@NonNull Context context, @NonNull String title,
            String initialValue, @NonNull Listener listener) {
        super(context);
        this.titleText=title;
        this.initial=initialValue==null?"":initialValue;
        this.listener=listener;
        this.prefs=NativeUiPrefs.load(context);
    }

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout shell=new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(15),dp(14),dp(15),dp(15));
        shell.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,NativeVisualTokens.LINE,dp(19),dp(1)));
        shell.addView(text(titleText,17,true,NativeVisualTokens.TEXT));

        TextView hint=text("Используется клавиатура телефона",11.5f,false,
                NativeVisualTokens.MUTED);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin=dp(4);
        shell.addView(hint,hp);

        input=new EditText(getContext());
        input.setTag("native_text_pad_display");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64)});
        NativeTypography.register(input,18,true,prefs);
        input.setTextColor(NativeVisualTokens.TEXT);
        input.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        input.setPadding(dp(12),0,dp(12),0);
        input.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.APP_SOFT,NativeVisualTokens.LINE,dp(11),dp(1)));
        input.setText(initial);
        input.setOnEditorActionListener((v,actionId,event)->{
            if(actionId==EditorInfo.IME_ACTION_DONE){submit();return true;}
            return false;
        });
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(52));
        ip.topMargin=dp(11);
        shell.addView(input,ip);

        LinearLayout actions=new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(44));
        ap.topMargin=dp(10);
        shell.addView(actions,ap);
        Button cancel=button("Отмена",false);
        cancel.setTag("native_text_pad_cancel");
        cancel.setOnClickListener(v->dismiss());
        Button done=button("Готово",true);
        done.setTag("native_text_pad_done");
        done.setOnClickListener(v->submit());
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(0,dp(44),1f);
        l.rightMargin=dp(4);
        actions.addView(cancel,l);
        LinearLayout.LayoutParams r=new LinearLayout.LayoutParams(0,dp(44),1f);
        r.leftMargin=dp(4);
        actions.addView(done,r);

        setContentView(shell);
        NativeMotion.enterCompact(shell);
        Window window=getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            // Fade-only dialog transition. Never slide or scale the window.
            window.setWindowAnimations(R.style.CeilingFadeWindowAnimation);
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width=Math.min(getContext().getResources().getDisplayMetrics().widthPixels-dp(24),dp(460));
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
        String value=input.getText().toString().trim();
        if(value.isEmpty()){
            Toast.makeText(getContext(),"Введите название",Toast.LENGTH_SHORT).show();
            return;
        }
        listener.onValue(value);
        dismiss();
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
        b.setAllCaps(false);
        b.setText(value);
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
