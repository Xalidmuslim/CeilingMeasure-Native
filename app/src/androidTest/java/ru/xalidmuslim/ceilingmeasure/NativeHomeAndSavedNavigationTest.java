package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Back gesture and saved-page dock regressions reported from a real Android phone. */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeAndSavedNavigationTest {
    @Test
    public void androidBackOnLauncherNeverFinishesOrBackgroundsHome() {
        try(ActivityScenario<NativeHomeActivity> scenario=
                    ActivityScenario.launch(NativeHomeActivity.class)){
            scenario.onActivity(activity->{
                View root=activity.getWindow().getDecorView();
                View home=root.findViewWithTag("native_home_root");
                assertNotNull(home);
                activity.getOnBackPressedDispatcher().onBackPressed();
                assertFalse("Android Back must not finish the home activity",
                        activity.isFinishing());
                assertFalse("Android Back must not destroy the home activity",
                        activity.isDestroyed());
                assertSame("Android Back keeps the same home root",
                        home,root.findViewWithTag("native_home_root"));
                activity.getOnBackPressedDispatcher().onBackPressed();
                assertFalse("Repeated Back also stays on Home",activity.isFinishing());
            });
        }
    }

    @Test
    public void savedMeasurementsAlwaysHasWorkingBottomNavigation() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_SAVED_MEASUREMENTS);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                View root=activity.getWindow().getDecorView();
                View saved=root.findViewWithTag("native_saved_page");
                assertNotNull(saved);
                ViewGroup nav=(ViewGroup)root.findViewWithTag("native_history_nav");
                assertNotNull("history should not drop the navigation",nav);
                assertEquals(View.VISIBLE,nav.getVisibility());
                assertEquals(4,nav.getChildCount());
                assertNotNull(root.findViewWithTag("native_nav_saved_home"));
                assertNotNull(root.findViewWithTag("native_nav_saved_editor"));
                assertNotNull(root.findViewWithTag("native_nav_saved_all"));
                assertNotNull(root.findViewWithTag("native_nav_saved_settings"));
                assertEquals(FrameLayout.LayoutParams.MATCH_PARENT,
                        nav.getLayoutParams().width);
                assertSame(saved,nav.getParent());

                ScrollView scroll=root.findViewWithTag("native_saved_scroll");
                assertNotNull(scroll);
                WindowInsetsCompat simulatedKeyboard=new WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.systemBars(),Insets.of(0,24,0,34))
                        .setInsets(WindowInsetsCompat.Type.ime(),Insets.of(0,0,0,460))
                        .build();
                ViewCompat.dispatchApplyWindowInsets(saved,simulatedKeyboard);
                assertEquals("navigation must not jump by the IME height",
                        34,scroll.getPaddingBottom());
                FrameLayout.LayoutParams dock=(FrameLayout.LayoutParams)nav.getLayoutParams();
                assertTrue(dock.bottomMargin>=34);
                assertTrue(dock.bottomMargin<100
                        *activity.getResources().getDisplayMetrics().density);

                root.findViewWithTag("native_nav_saved_editor").performClick();
                assertNotNull("drawing page must be reachable from saved navigation",
                        activity.getWindow().getDecorView()
                                .findViewWithTag("native_editor_scroll"));
                assertNotNull(activity.getWindow().getDecorView()
                        .findViewWithTag("native_history_nav"));
            });
        }
    }
}
