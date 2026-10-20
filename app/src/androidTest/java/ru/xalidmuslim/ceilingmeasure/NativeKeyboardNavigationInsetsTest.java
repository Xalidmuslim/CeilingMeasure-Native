package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
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

/** Edge-to-edge regression: keyboard and system bars must not hide bottom controls. */
@RunWith(AndroidJUnit4.class)
public final class NativeKeyboardNavigationInsetsTest {
    @Test
    public void navigationFollowsImeAndReturnsToSystemNavigationBar() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent start=new Intent(context,NativeWorkspaceActivity.class);
        start.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(start)){
            scenario.onActivity(activity->{
                View root=activity.getWindow().getDecorView();
                View page=root.findViewWithTag("native_workspace_page");
                ScrollView scroll=root.findViewWithTag("native_editor_scroll");
                View navigation=root.findViewWithTag("native_history_nav");
                assertNotNull(page);
                assertNotNull(scroll);
                assertNotNull(navigation);

                WindowInsetsCompat keyboard=new WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.systemBars(),
                                Insets.of(0,24,0,34))
                        .setInsets(WindowInsetsCompat.Type.ime(),
                                Insets.of(0,0,0,460))
                        .build();
                ViewCompat.dispatchApplyWindowInsets(page,keyboard);
                assertEquals(View.VISIBLE,navigation.getVisibility());
                assertEquals(460,scroll.getPaddingBottom());
                FrameLayout.LayoutParams layout=
                        (FrameLayout.LayoutParams)navigation.getLayoutParams();
                assertTrue("navigation must be above the system keyboard",
                        layout.bottomMargin>=460);
                assertTrue("navigation must use full safe width",
                        layout.width==FrameLayout.LayoutParams.MATCH_PARENT);

                WindowInsetsCompat withoutKeyboard=new WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.systemBars(),
                                Insets.of(0,24,0,34))
                        .setInsets(WindowInsetsCompat.Type.ime(),Insets.NONE)
                        .build();
                ViewCompat.dispatchApplyWindowInsets(page,withoutKeyboard);
                assertEquals(View.VISIBLE,navigation.getVisibility());
                assertEquals(34,scroll.getPaddingBottom());
                FrameLayout.LayoutParams restored=
                        (FrameLayout.LayoutParams)navigation.getLayoutParams();
                assertTrue("navigation returns to system bar after keyboard closes",
                        restored.bottomMargin>=34);
                assertTrue("no excessive bottom offset after keyboard close",
                        restored.bottomMargin<100*activity.getResources()
                                .getDisplayMetrics().density);
            });
        }
    }
}
