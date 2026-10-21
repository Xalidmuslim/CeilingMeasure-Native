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

    /**
     * Keep floating navigation physically docked above the system navigation
     * bar. Never apply the IME height to the dock: on Android 15/16 devices
     * adjustResize may already shrink the window, and offsetting again pushes
     * the bar outside the visible Activity. Dialog IMEs own their own window.
     */
    static void bindWorkspace(
            @NonNull View owner,
            @NonNull ScrollView scroll,
            @NonNull View historyNav,
            int navBaseBottomPx) {
        ViewCompat.setOnApplyWindowInsetsListener(owner,(view,windowInsets)->{
            Insets system=safeInsets(windowInsets);
            if(scroll.getPaddingLeft()!=system.left
                    || scroll.getPaddingTop()!=system.top
                    || scroll.getPaddingRight()!=system.right
                    || scroll.getPaddingBottom()!=system.bottom){
                scroll.setPadding(system.left,system.top,system.right,system.bottom);
            }

            FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)historyNav.getLayoutParams();
            int side=Math.round(12f*owner.getResources().getDisplayMetrics().density);
            int bottom=system.bottom+navBaseBottomPx;
            int left=side+system.left;
            int right=side+system.right;
            if(p.bottomMargin!=bottom||p.leftMargin!=left||p.rightMargin!=right){
                p.bottomMargin=bottom;
                p.leftMargin=left;
                p.rightMargin=right;
                historyNav.setLayoutParams(p);
            }
            if(historyNav.getVisibility()!=View.VISIBLE){
                historyNav.setVisibility(View.VISIBLE);
            }
            owner.setTag(R.id.native_safe_top,system.top);
            owner.setTag(R.id.native_safe_bottom,system.bottom);
            owner.setTag(R.id.native_safe_left,system.left);
            owner.setTag(R.id.native_safe_right,system.right);
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
