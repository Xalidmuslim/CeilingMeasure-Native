package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * A full-screen Dialog has a different Window from the Activity. Activity
 * navigation is BEHIND the Dialog and cannot be made visible by invalidating
 * or bringing it to front. Give every full-screen estimate/pricing window its
 * own real bottom navigation and independently style its system bars.
 */
final class NativeFullScreenDialogShell {
    private NativeFullScreenDialogShell() {}

    static final class Action {
        final int iconRes;
        final String title;
        final String tag;
        final Runnable callback;

        Action(@DrawableRes int iconRes,@NonNull String title,
                @NonNull String tag,@NonNull Runnable callback){
            this.iconRes=iconRes;
            this.title=title;
            this.tag=tag;
            this.callback=callback;
        }
    }

    static void configure(@NonNull Dialog dialog) {
        Window window=dialog.getWindow();
        if(window==null)return;
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                |WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        WindowCompat.setDecorFitsSystemWindows(window,false);
        // Opaque, app-colored decor behind cutouts/transparent bars: this is
        // the exact area that looked like gray "caps" in full-screen Dialogs.
        window.getDecorView().setBackgroundColor(NativeVisualTokens.APP_BG);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller=WindowCompat.getInsetsController(
                window,window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    static FrameLayout create(@NonNull Dialog dialog,
            @NonNull ScrollView scroll,@NonNull Action... actions){
        final float density=dialog.getContext().getResources()
                .getDisplayMetrics().density;
        FrameLayout root=new FrameLayout(dialog.getContext());
        root.setTag("native_fullscreen_dialog_shell");
        root.setBackgroundColor(NativeVisualTokens.APP_BG);
        root.setClipChildren(false);
        root.setClipToPadding(false);

        FrameLayout.LayoutParams body=new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        // Scroll content always ends ABOVE the fixed dock.
        body.bottomMargin=px(density,NativeVisualTokens.HISTORY_HEIGHT_DP+18);
        scroll.setClipToPadding(false);
        root.addView(scroll,body);

        LinearLayout nav=new LinearLayout(dialog.getContext());
        nav.setTag("native_fullscreen_dialog_nav");
        nav.setGravity(Gravity.CENTER);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(px(density,NativeVisualTokens.HISTORY_PADDING_DP),
                px(density,NativeVisualTokens.HISTORY_PADDING_DP),
                px(density,NativeVisualTokens.HISTORY_PADDING_DP),
                px(density,NativeVisualTokens.HISTORY_PADDING_DP));
        nav.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,NativeVisualTokens.LINE,
                px(density,NativeVisualTokens.HISTORY_RADIUS_DP),px(density,1)));
        nav.setElevation(px(density,2));

        NativeUiPrefs prefs=NativeUiPrefs.load(dialog.getContext());
        for(int i=0;i<actions.length;i++){
            Action action=actions[i];
            LinearLayout button=new LinearLayout(dialog.getContext());
            button.setOrientation(LinearLayout.VERTICAL);
            button.setGravity(Gravity.CENTER);
            button.setTag(action.tag);
            button.setContentDescription(action.title);
            button.setClickable(true);
            button.setFocusable(true);
            button.setBackground(NativeVisualTokens.rippleOutlined(
                    NativeVisualTokens.SURFACE,Color.TRANSPARENT,
                    px(density,NativeVisualTokens.HISTORY_BUTTON_RADIUS_DP),0,
                    Color.argb(22,33,110,147)));
            button.setOnClickListener(v->action.callback.run());
            ImageView icon=new ImageView(dialog.getContext());
            icon.setImageResource(action.iconRes);
            icon.setImageTintList(ColorStateList.valueOf(NativeVisualTokens.TEXT));
            LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(
                    px(density,22),px(density,22));
            iconParams.bottomMargin=px(density,2);
            button.addView(icon,iconParams);
            TextView label=new TextView(dialog.getContext());
            label.setText(action.title);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setIncludeFontPadding(false);
            label.setTextColor(NativeVisualTokens.TEXT);
            NativeTypography.register(label,10.5f,true,prefs);
            button.addView(label,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams buttonParams=new LinearLayout.LayoutParams(
                    0,px(density,NativeVisualTokens.HISTORY_BUTTON_HEIGHT_DP),1f);
            if(i>0)buttonParams.leftMargin=px(density,NativeVisualTokens.HISTORY_GAP_DP);
            nav.addView(button,buttonParams);
        }
        FrameLayout.LayoutParams dock=new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                px(density,NativeVisualTokens.HISTORY_HEIGHT_DP),
                Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        dock.leftMargin=px(density,12);
        dock.rightMargin=px(density,12);
        dock.bottomMargin=px(density,NativeVisualTokens.HISTORY_BOTTOM_DP);
        root.addView(nav,dock);

        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    |WindowInsetsCompat.Type.displayCutout());
            if(root.getPaddingTop()!=safe.top
                    ||root.getPaddingBottom()!=safe.bottom
                    ||root.getPaddingLeft()!=safe.left
                    ||root.getPaddingRight()!=safe.right){
                root.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        return root;
    }

    private static int px(float density,int dp){
        return Math.round(dp*density);
    }
}
