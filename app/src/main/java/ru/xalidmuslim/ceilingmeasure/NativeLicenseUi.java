package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Compact native license sheets matching the v184/v224 shell visual tokens.
 * Presentation only: no drawing, measurement or persisted project state changes.
 */
final class NativeLicenseUi {
    private NativeLicenseUi() {}

    static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    static LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams spaced(Activity activity, int top) {
        LinearLayout.LayoutParams lp = match();
        lp.topMargin = dp(activity, top);
        return lp;
    }

    static LinearLayout row(Activity activity, int gap) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTag("native_license_row");
        return row;
    }

    static LinearLayout.LayoutParams weighted(Activity activity, int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(activity, leftMargin);
        return lp;
    }

    static TextView text(Activity activity, String value, float sp, boolean emphasis) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(emphasis ? NativeVisualTokens.TEXT_STRONG : NativeVisualTokens.MUTED);
        view.setTypeface(Typeface.create("sans-serif",
                emphasis ? Typeface.BOLD : Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(dp(activity, 2), 1f);
        return view;
    }

    static TextView heading(Activity activity, String value) {
        return text(activity, value, 14f, true);
    }

    static Button button(Activity activity, String value, boolean primary) {
        Button control = new Button(activity);
        control.setText(value);
        control.setTextSize(12.5f);
        control.setAllCaps(false);
        control.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        control.setTextColor(primary ? Color.WHITE : NativeVisualTokens.HOME_CARD_TEXT);
        control.setBackgroundTintList((ColorStateList) null);
        control.setBackground(NativeVisualTokens.controlButton(
                primary, dp(activity, NativeVisualTokens.CONTROL_RADIUS_DP)));
        control.setMinWidth(0);
        control.setMinimumWidth(0);
        control.setMinHeight(0);
        control.setMinimumHeight(dp(activity, 42));
        control.setPadding(dp(activity, 10), dp(activity, 7),
                dp(activity, 10), dp(activity, 7));
        control.setMaxLines(2);
        control.setGravity(Gravity.CENTER);
        control.setElevation(0f);
        return control;
    }

    static EditText field(Activity activity, String hint, int inputType) {
        EditText field = new EditText(activity);
        field.setSingleLine(true);
        field.setTextSize(13f);
        field.setTextColor(NativeVisualTokens.TEXT);
        field.setHintTextColor(NativeVisualTokens.MUTED);
        field.setHint(hint);
        field.setInputType(inputType);
        field.setPadding(dp(activity, 11), dp(activity, 10),
                dp(activity, 11), dp(activity, 10));
        field.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                NativeVisualTokens.LINE, dp(activity, 10), dp(activity, 1)));
        field.setSelectAllOnFocus(false);
        return field;
    }

    static TextView notice(Activity activity, String value) {
        TextView note = text(activity, value, 11.5f, false);
        note.setPadding(dp(activity, 10), dp(activity, 9),
                dp(activity, 10), dp(activity, 9));
        note.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.ACCENT_SOFT,
                NativeVisualTokens.LINE, dp(activity, 10), dp(activity, 1)));
        return note;
    }

    static LinearLayout panel(Activity activity) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(activity, 12), dp(activity, 11),
                dp(activity, 12), dp(activity, 11));
        layout.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE_SOFT,
                NativeVisualTokens.LINE, dp(activity, 13), dp(activity, 1)));
        return layout;
    }

    static TextView inlineLink(Activity activity, String label) {
        TextView view = text(activity, label, 12f, true);
        view.setTextColor(NativeVisualTokens.ACCENT_STRONG);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(activity, 4), dp(activity, 7),
                dp(activity, 4), dp(activity, 7));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    static final class Sheet {
        final Activity activity;
        final LinearLayout body;
        final AlertDialog dialog;

        Sheet(Activity activity, String title, String subtitle) {
            this.activity = activity;
            ScrollView scroll = new ScrollView(activity) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int max = (int)(getResources().getDisplayMetrics().heightPixels * .82f);
                    int mode = View.MeasureSpec.getMode(heightMeasureSpec);
                    int size = View.MeasureSpec.getSize(heightMeasureSpec);
                    int allowed = mode == View.MeasureSpec.UNSPECIFIED
                            ? max : Math.min(max, size);
                    super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(
                            allowed, View.MeasureSpec.AT_MOST));
                }
            };
            scroll.setFillViewport(false);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.setClipToPadding(false);

            body = new LinearLayout(activity);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(activity, 15), dp(activity, 14),
                    dp(activity, 15), dp(activity, 12));
            body.setBackground(NativeVisualTokens.outlined(
                    NativeVisualTokens.SURFACE,
                    NativeVisualTokens.LINE, dp(activity, 19), dp(activity, 1)));
            scroll.addView(body, new ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout header = row(activity, 0);
            LinearLayout headerCopy = new LinearLayout(activity);
            headerCopy.setOrientation(LinearLayout.VERTICAL);
            headerCopy.addView(text(activity, title, 18f, true), match());
            if (subtitle != null && !subtitle.isEmpty()) {
                headerCopy.addView(text(activity, subtitle, 11.5f, false),
                        spaced(activity, 4));
            }
            header.addView(headerCopy, weighted(activity, 0));

            TextView close = text(activity, "×", 24f, false);
            close.setContentDescription("Закрыть окно");
            close.setGravity(Gravity.CENTER);
            close.setClickable(true);
            close.setFocusable(true);
            close.setBackground(NativeVisualTokens.controlButton(
                    false, dp(activity, 10)));
            LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                    dp(activity, 36), dp(activity, 36));
            closeLp.leftMargin = dp(activity, 8);
            header.addView(close, closeLp);

            body.addView(header, match());
            dialog = new AlertDialog.Builder(activity).setView(scroll).create();
            close.setOnClickListener(v -> dialog.dismiss());
        }

        void show() {
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int pixels = activity.getResources().getDisplayMetrics().widthPixels;
                int width = Math.min(pixels - dp(activity, 26), dp(activity, 430));
                window.setLayout(Math.max(dp(activity, 280), width),
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setDimAmount(.30f);
            }
        }

        void dismiss() {
            dialog.dismiss();
        }

        boolean visible() {
            return !activity.isFinishing() && dialog.isShowing();
        }
    }
}
