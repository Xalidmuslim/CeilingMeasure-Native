package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;

/**
 * Small in-dialog number pad. It replaces only numeric Android IMEs; text fields
 * retain the system keyboard. All edits go through EditText for existing watchers.
 */
final class NativeCompactNumericKeypad {
    private static final String[] KEYS={
            "1","2","3","4","5","6","7","8","9",",","0","⌫"
    };

    private NativeCompactNumericKeypad(){}

    static View attach(EditText input,NativeUiPrefs prefs){
        // Accept comma on any device locale, including English Android layouts.
        // Raw numeric inputType preserves numeric semantics without replacing
        // our explicit punctuation filter with the user's IME conventions.
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789,."));
        input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setShowSoftInputOnFocus(false);

        Context context=input.getContext();
        LinearLayout panel=new LinearLayout(context);
        panel.setTag("native_compact_number_keypad");
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(context,2),dp(context,2),dp(context,2),dp(context,2));
        panel.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.APP_SOFT,NativeVisualTokens.LINE,dp(context,12),dp(context,1)));

        GridLayout grid=new GridLayout(context);
        grid.setColumnCount(3);
        grid.setRowCount(4);
        panel.addView(grid,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        for(String key:KEYS){
            Button button=new Button(context);
            button.setAllCaps(false);
            button.setText(key);
            button.setTag(",".equals(key)?"native_numeric_key_decimal":
                    "⌫".equals(key)?"native_numeric_key_delete":"native_numeric_key_"+key);
            button.setContentDescription("⌫".equals(key)?"Удалить символ":
                    ",".equals(key)?"Десятичная запятая":"Цифра "+key);
            button.setGravity(Gravity.CENTER);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setMinHeight(0);
            button.setMinimumHeight(0);
            button.setPadding(0,0,0,0);
            button.setTextColor("⌫".equals(key)
                    ?NativeVisualTokens.MUTED:NativeVisualTokens.TEXT);
            NativeTypography.register(button,"⌫".equals(key)?18:19,true,prefs);
            button.setBackground(NativeVisualTokens.rippleOutlined(
                    NativeVisualTokens.SURFACE,NativeVisualTokens.LINE,
                    dp(context,9),dp(context,1),Color.argb(24,30,49,68)));
            button.setOnClickListener(v->type(input,key));

            GridLayout.LayoutParams item=new GridLayout.LayoutParams();
            item.width=0;
            item.height=dp(context,38);
            item.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            item.setMargins(dp(context,3),dp(context,3),
                    dp(context,3),dp(context,3));
            grid.addView(button,item);
        }
        return panel;
    }

    private static void type(EditText input,String key){
        Editable editable=input.getText();
        int length=editable.length();
        int start=Math.max(0,Math.min(length,input.getSelectionStart()));
        int end=Math.max(0,Math.min(length,input.getSelectionEnd()));
        if(start>end){int tmp=start;start=end;end=tmp;}
        if("⌫".equals(key)){
            if(start==end && start>0)start--;
            editable.delete(start,end);
            if(editable.length()==0){
                editable.append("0");
                input.selectAll();
            }else{
                input.setSelection(Math.min(start,editable.length()));
            }
            return;
        }

        String value=key;
        if(",".equals(key)){
            String remaining=editable.subSequence(0,start).toString()
                    +editable.subSequence(end,length);
            if(remaining.contains(",")||remaining.contains("."))return;
            if(start==0 && remaining.isEmpty())value="0,";
        }else if(!key.matches("[0-9]")){
            return;
        }
        if(start==0 && end==length && "0".contentEquals(editable)
                && !"0".equals(value) && !",".equals(value)){
            editable.replace(0,length,"");
            start=0;end=0;
        }
        if(editable.length()-(end-start)+value.length()>12)return;
        editable.replace(start,end,value);
        input.setSelection(Math.min(start+value.length(),editable.length()));
    }

    private static int dp(Context context,int value){
        return Math.round(context.getResources().getDisplayMetrics().density*value);
    }
}
