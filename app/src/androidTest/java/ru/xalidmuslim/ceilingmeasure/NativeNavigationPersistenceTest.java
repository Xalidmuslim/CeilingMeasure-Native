package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NativeNavigationPersistenceTest {
    @Test public void bottomNavigationStaysWideAndVisibleWhileEditingWall() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent start=new Intent(context, NativeWorkspaceActivity.class);
        start.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(start)) {
            scenario.onActivity(activity->{
                View nav=activity.getWindow().getDecorView()
                        .findViewWithTag("native_history_nav");
                assertNotNull(nav);
                assertEquals(View.VISIBLE,nav.getVisibility());
                assertTrue("wide navigation",nav.getWidth()
                        >= activity.getResources().getDisplayMetrics().widthPixels*.82f);

                NativeProjectStore.RoomData room=activity.getActiveRoomForTest();
                room.points.add(new NativeProjectStore.PointData("pA",0f,0f));
                room.points.add(new NativeProjectStore.PointData("pB",100f,0f));
                room.edges.add(new NativeProjectStore.EdgeData("eShort","pA","pB",100f));
                activity.onWallLongPressed("eShort",100f);
                assertEquals("navigation should not disappear under measurement editor",
                        View.VISIBLE,nav.getVisibility());
                View close=activity.getWindow().getDecorView()
                        .findViewWithTag("native_wall_dismiss");
                // The dialog has its own window, so it is not in the Activity decor.
                assertEquals(View.VISIBLE,nav.getVisibility());
            });
        }
    }
}
