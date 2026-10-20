package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

/** Compact app-native alternative to stock Android AlertDialog menus and confirmations. */
final class NativeStyledAlertDialog extends Dialog {
    static final class Builder {
        private final Context context;
        private CharSequence title;
        private CharSequence message;
        private CharSequence[] items;
        private boolean[] checked;
        private DialogInterface.OnClickListener itemListener;
        private DialogInterface.OnMultiChoiceClickListener multiChoiceListener;
        private CharSequence positive;
        private CharSequence negative;
        private DialogInterface.OnClickListener positiveListener;
        private DialogInterface.OnClickListener negativeListener;
        private boolean cancelable = true;

        Builder(Context context) { this.context = context; }

        Builder setTitle(CharSequence value) { title = value; return this; }
        Builder setMessage(CharSequence value) { message = value; return this; }
        Builder setItems(CharSequence[] labels, DialogInterface.OnClickListener callback) {
            items = labels; itemListener = callback; checked = null;
            multiChoiceListener = null;
            return this;
        }
        Builder setMultiChoiceItems(CharSequence[] labels, boolean[] values,
                DialogInterface.OnMultiChoiceClickListener callback) {
            items = labels;
            checked = values;
            multiChoiceListener = callback;
            itemListener = null;
            return this;
        }
        Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener callback) {
            positive = text; positiveListener = callback; return this;
        }
        Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener callback) {
            negative = text; negativeListener = callback; return this;
        }
        Builder setCancelable(boolean value) { cancelable = value; return this; }

        NativeStyledAlertDialog create() { return new NativeStyledAlertDialog(context, this); }
        NativeStyledAlertDialog show() {
            NativeStyledAlertDialog dialog = create();
            dialog.show();
            return dialog;
        }
    }

    private final Builder config;
    private final NativeUiPrefs prefs;

    private NativeStyledAlertDialog(Context context, Builder config) {
        super(context);
        this.config = config;
        this.prefs = NativeUiPrefs.load(context);
        setCancelable(config.cancelable);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Context context = getContext();
        LinearLayout shell = new LinearLayout(context);
        shell.setTag("native_styled_alert_shell");
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(14), dp(13), dp(14), dp(14));
        shell.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE, NativeVisualTokens.LINE, dp(19), dp(1)));

        if (config.title != null) {
            TextView header = text(config.title.toString(), 17f, true,
                    NativeVisualTokens.TEXT);
            header.setPadding(dp(2), dp(3), dp(2), dp(7));
            shell.addView(header);
        }
        if (config.message != null) {
            ScrollView messageScroll = new ScrollView(context);
            messageScroll.setVerticalScrollBarEnabled(false);
            TextView message = text(config.message.toString(), 13.5f, false,
                    NativeVisualTokens.MUTED);
            message.setPadding(dp(2), dp(5), dp(2), dp(11));
            messageScroll.addView(message);
            int lines = Math.max(2, config.message.length() / 38 + 1);
            shell.addView(messageScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.min(dp(250), dp(lines * 22))));
        }
        if (config.items != null) {
            ScrollView listScroll = new ScrollView(context);
            listScroll.setVerticalScrollBarEnabled(false);
            listScroll.setFillViewport(false);
            LinearLayout list = new LinearLayout(context);
            list.setOrientation(LinearLayout.VERTICAL);
            listScroll.addView(list);
            for (int index = 0; index < config.items.length; index++) {
                final int choiceIndex = index;
                LinearLayout row = new LinearLayout(context);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(5), dp(10), dp(5));
                row.setMinimumHeight(dp(46));
                row.setBackground(NativeVisualTokens.rippleOutlined(
                        NativeVisualTokens.SURFACE_SOFT, NativeVisualTokens.DIVIDER,
                        dp(13), dp(1), Color.argb(24, 47, 143, 190)));
                TextView label = text(config.items[index].toString(), 13.5f,
                        false, NativeVisualTokens.TEXT);
                label.setMaxLines(3);
                row.addView(label, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView mark = null;
                if (config.checked != null) {
                    mark = text(config.checked[index] ? "✓" : "○", 17f,
                            true, NativeVisualTokens.ACCENT);
                    mark.setGravity(Gravity.CENTER);
                    mark.setTag("native_styled_choice_" + index);
                    row.addView(mark, new LinearLayout.LayoutParams(dp(32), dp(38)));
                }
                final TextView indicator = mark;
                row.setTag("native_styled_alert_item_" + index);
                row.setOnClickListener(v -> {
                    if (config.checked != null) {
                        config.checked[choiceIndex] = !config.checked[choiceIndex];
                        if (indicator != null) indicator.setText(
                                config.checked[choiceIndex] ? "✓" : "○");
                        if (config.multiChoiceListener != null) {
                            config.multiChoiceListener.onClick(this, choiceIndex,
                                    config.checked[choiceIndex]);
                        }
                    } else {
                        dismiss();
                        if (config.itemListener != null)
                            config.itemListener.onClick(this, choiceIndex);
                    }
                });
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = dp(5);
                list.addView(row, rowParams);
            }
            LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.min(dp(465), config.items.length * dp(60)));
            shell.addView(listScroll, listParams);
        }
        if (config.negative != null || config.positive != null) {
            LinearLayout actions = new LinearLayout(context);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.END);
            LinearLayout.LayoutParams bar = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(45));
            bar.topMargin = dp(8);
            shell.addView(actions, bar);
            if (config.negative != null) {
                Button cancel = button(config.negative.toString(), false);
                cancel.setOnClickListener(v -> {
                    dismiss();
                    if (config.negativeListener != null)
                        config.negativeListener.onClick(this, DialogInterface.BUTTON_NEGATIVE);
                });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1f);
                p.rightMargin = dp(4);
                actions.addView(cancel, p);
            }
            if (config.positive != null) {
                Button okay = button(config.positive.toString(), true);
                okay.setOnClickListener(v -> {
                    dismiss();
                    if (config.positiveListener != null)
                        config.positiveListener.onClick(this, DialogInterface.BUTTON_POSITIVE);
                });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1f);
                p.leftMargin = dp(4);
                actions.addView(okay, p);
            }
        }

        setContentView(shell);
        NativeMotion.enterCompact(shell);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams attrs = new WindowManager.LayoutParams();
            attrs.copyFrom(window.getAttributes());
            attrs.width = Math.min(context.getResources().getDisplayMetrics().widthPixels - dp(28),
                    dp(480));
            attrs.height = WindowManager.LayoutParams.WRAP_CONTENT;
            attrs.gravity = Gravity.CENTER;
            window.setAttributes(attrs);
        }
    }

    private TextView text(String value, float sp, boolean bold, int color) {
        TextView t = new TextView(getContext());
        t.setText(value);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t, sp, bold, prefs);
    }

    private Button button(String value, boolean primary) {
        Button b = new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setTextColor(primary ? Color.WHITE : NativeVisualTokens.TEXT);
        NativeTypography.register(b, 12f, true, prefs);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(6), 0, dp(6), 0);
        b.setBackground(NativeVisualTokens.controlButton(primary, dp(10)));
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
