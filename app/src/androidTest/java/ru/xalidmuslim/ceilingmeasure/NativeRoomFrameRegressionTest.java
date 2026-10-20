package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NativeRoomFrameRegressionTest {
    @Test public void roomButtonKeepsUnclippedFrameWithoutRecreationOnTap() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                View room=activity.getWindow().getDecorView()
                        .findViewWithTag("native_room_0");
                assertTrue(room instanceof Button);
                LinearLayout row=(LinearLayout)room.getParent();
                HorizontalScrollView scroller=(HorizontalScrollView)row.getParent();
                assertFalse(row.getClipChildren());
                assertFalse(scroller.getClipChildren());
                assertTrue("top stroke/ripple requires a guard inset",
                        row.getPaddingTop()>0);
                assertTrue("bottom stroke/ripple requires a guard inset",
                        row.getPaddingBottom()>0);
                assertTrue(room.getLayoutParams().height >=
                        Math.round(48*activity.getResources()
                                .getDisplayMetrics().density));
                room.performClick();
                assertSame("room tabs must not be recreated on selection",
                        room,activity.getWindow().getDecorView()
                                .findViewWithTag("native_room_0"));
            });
        }
    }

    @Test public void screenTransitionNeverHidesItsEntireContent() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        View view=new View(context);
        NativeMotion.enterCompact(view);
        assertEquals(0f,view.getTranslationY(),.001f);
        assertTrue("fade never starts from fully transparent",
                view.getAlpha()>=.9f);
        view.animate().cancel();
    }
}
