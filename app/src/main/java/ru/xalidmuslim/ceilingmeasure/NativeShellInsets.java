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
            scroll.setPadding(safe.left, safe.top, safe.right, safe.bottom);
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
        ViewCompat.setOnApplyWindowInsetsListener(owner, (view, windowInsets) -> {
            Insets safe = safeInsets(windowInsets);

            // Scroll owns system insets exactly once. The root content has a
            // separate fixed-navigation reserve that does not contain system inset.
            scroll.setPadding(safe.left, safe.top, safe.right, safe.bottom);

            FrameLayout.LayoutParams navParams =
                    (FrameLayout.LayoutParams) historyNav.getLayoutParams();
            navParams.bottomMargin = safe.bottom + navBaseBottomPx;
            historyNav.setLayoutParams(navParams);

            owner.setTag(R.id.native_safe_top, safe.top);
            owner.setTag(R.id.native_safe_bottom, safe.bottom);
            owner.setTag(R.id.native_safe_left, safe.left);
            owner.setTag(R.id.native_safe_right, safe.right);
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
