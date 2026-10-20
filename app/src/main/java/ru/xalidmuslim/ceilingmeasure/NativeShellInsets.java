package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * One owner for system-bar/cutout insets. System insets are applied once to
 * scroll content; the floating history navigation gets its own bottom margin.
 */
final class NativeShellInsets {
    private NativeShellInsets() {}

    static void configureWindow(@NonNull Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(NativeVisualTokens.SURFACE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    static void bindHome(@NonNull View owner, @NonNull ScrollView scroll) {
        ViewCompat.setOnApplyWindowInsetsListener(owner, (view, windowInsets) -> {
            Insets safe = safeInsets(windowInsets);
            if(scroll.getPaddingLeft()!=safe.left
                    || scroll.getPaddingTop()!=safe.top
                    || scroll.getPaddingRight()!=safe.right
                    || scroll.getPaddingBottom()!=safe.bottom){
                scroll.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            }
            owner.setTag(R.id.native_safe_top, safe.top);
            owner.setTag(R.id.native_safe_bottom, safe.bottom);
            owner.setTag(R.id.native_safe_left, safe.left);
            owner.setTag(R.id.native_safe_right, safe.right);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(owner);
    }

    static void bindWorkspace(
            @NonNull View owner,
            @NonNull ScrollView scroll,
            @NonNull View historyNav,
            int navBaseBottomPx) {
        final int[] safe={0,0,0,0};
        final int[] requestedBottom={navBaseBottomPx};
        final Runnable placeNavigation=()->{
            FrameLayout.LayoutParams navParams=
                    (FrameLayout.LayoutParams)historyNav.getLayoutParams();
            int dp=Math.round(12f*owner.getResources().getDisplayMetrics().density);
            int left=dp+safe[0];
            int right=dp+safe[2];
            int baseHeight=historyNav.getHeight()>0
                    ?historyNav.getHeight()
                    :Math.round(NativeVisualTokens.HISTORY_HEIGHT_DP
                        *owner.getResources().getDisplayMetrics().density);
            int availableBottom=requestedBottom[0];
            if(owner.getHeight()>0) {
                // With edge-to-edge + ADJUST_RESIZE some manufacturers shrink
                // the page AND deliver the full IME inset. Counting it twice
                // places the navigation above the viewport (it "disappears").
                int room=owner.getHeight()-safe[1]-baseHeight-dp;
                availableBottom=Math.min(requestedBottom[0],
                        Math.max(navBaseBottomPx,room));
            }
            if(navParams.bottomMargin!=availableBottom
                    || navParams.leftMargin!=left
                    || navParams.rightMargin!=right){
                navParams.bottomMargin=availableBottom;
                navParams.leftMargin=left;
                navParams.rightMargin=right;
                historyNav.setLayoutParams(navParams);
            }
            if(historyNav.getVisibility()!=View.VISIBLE)historyNav.setVisibility(View.VISIBLE);
        };
        owner.addOnLayoutChangeListener((v,l,t,r,b,oldL,oldT,oldR,oldB)->{
            if((b-t)!=(oldB-oldT))placeNavigation.run();
        });

        ViewCompat.setOnApplyWindowInsetsListener(owner,(view,windowInsets)->{
            Insets system=safeInsets(windowInsets);
            Insets keyboard=windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom=Math.max(system.bottom,keyboard.bottom);
            safe[0]=system.left;
            safe[1]=system.top;
            safe[2]=system.right;
            safe[3]=system.bottom;
            requestedBottom[0]=bottom+navBaseBottomPx;

            if(scroll.getPaddingLeft()!=system.left || scroll.getPaddingTop()!=system.top
                    || scroll.getPaddingRight()!=system.right
                    || scroll.getPaddingBottom()!=bottom){
                scroll.setPadding(system.left,system.top,system.right,bottom);
            }

            placeNavigation.run();

            owner.setTag(R.id.native_safe_top, system.top);
            owner.setTag(R.id.native_safe_bottom, system.bottom);
            owner.setTag(R.id.native_safe_left, system.left);
            owner.setTag(R.id.native_safe_right, system.right);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(owner);
    }

    static Insets currentSafeInsets(@NonNull View view) {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
        return insets == null ? Insets.NONE : safeInsets(insets);
    }

    private static Insets safeInsets(@NonNull WindowInsetsCompat insets) {
        return insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout());
    }
}
