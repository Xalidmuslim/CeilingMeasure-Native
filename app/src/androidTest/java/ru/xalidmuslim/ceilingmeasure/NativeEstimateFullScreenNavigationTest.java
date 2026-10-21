package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public final class NativeEstimateFullScreenNavigationTest {
    @Test public void bothEstimatesOwnAnInsetSafeVisibleDockAndCompactRows() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent start=new Intent(ctx,NativeWorkspaceActivity.class);
        start.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(start)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                NativeProjectStore.RoomData room=store.createRoom("Гостиная");
                project.rooms.add(room);
                for(NativeEstimateDialog.Mode mode:NativeEstimateDialog.Mode.values()){
                    AtomicBoolean home=new AtomicBoolean();
                    NativeEstimateDialog dialog=new NativeEstimateDialog(
                            activity,store,project,mode,
                            new NativeEstimateDialog.Listener(){
                                public void onChanged(){}
                                public void onHomeRequested(){home.set(true);}
                            });
                    dialog.show();
                    try{
                        Window window=dialog.getWindow();
                        assertNotNull(window);
                        assertEquals(Color.TRANSPARENT,window.getStatusBarColor());
                        assertEquals(Color.TRANSPARENT,window.getNavigationBarColor());
                        View root=window.getDecorView();
                        View shell=root.findViewWithTag("native_fullscreen_dialog_shell");
                        assertNotNull(shell);
                        assertNotNull(root.findViewWithTag(
                                mode==NativeEstimateDialog.Mode.CLIENT
                                        ?"native_client_summary":"native_internal_summary"));
                        LinearLayout dock=root.findViewWithTag("native_fullscreen_dialog_nav");
                        assertNotNull("fullscreen estimate must own its navigation",dock);
                        assertEquals(4,dock.getChildCount());
                        assertNotNull(dock.findViewWithTag("native_estimate_nav_home"));
                        assertNotNull(dock.findViewWithTag("native_estimate_nav_drawing"));
                        assertNotNull(dock.findViewWithTag("native_estimate_nav_price"));
                        assertNotNull(dock.findViewWithTag("native_estimate_nav_more"));
                        assertSame(shell,dock.getParent());
                        assertEquals(View.VISIBLE,dock.getVisibility());
                        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT,
                                dock.getLayoutParams().width);

                        WindowInsetsCompat synthetic=new WindowInsetsCompat.Builder()
                                .setInsets(WindowInsetsCompat.Type.systemBars(),
                                        Insets.of(0,24,0,34))
                                .setInsets(WindowInsetsCompat.Type.ime(),
                                        Insets.of(0,0,0,480))
                                .build();
                        ViewCompat.dispatchApplyWindowInsets(shell,synthetic);
                        assertEquals(24,shell.getPaddingTop());
                        assertEquals(34,shell.getPaddingBottom());
                        ScrollView scroll=(ScrollView)((FrameLayout)shell).getChildAt(0);
                        assertTrue("content never continues behind the dock",
                                ((FrameLayout.LayoutParams)scroll.getLayoutParams())
                                        .bottomMargin>dock.getLayoutParams().height);

                        View item=root.findViewWithTag(
                                "native_estimate_line_canvas_"+room.id);
                        if(item instanceof LinearLayout){
                            assertTrue("compact estimate row vertical gutters",
                                    item.getPaddingTop()<=dp(activity,2));
                            assertTrue(item.getPaddingBottom()<=dp(activity,2));
                        }
                        dock.findViewWithTag("native_estimate_nav_home").performClick();
                        assertTrue("Home must execute a real action",home.get());
                    }finally{
                        if(dialog.isShowing())dialog.dismiss();
                    }
                }
            });
        }
    }

    @Test public void priceDialogOwnsItsOwnDockRatherThanHidingActivityNavigation(){
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent start=new Intent(ctx,NativeWorkspaceActivity.class);
        start.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scene=ActivityScenario.launch(start)){
            scene.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                project.rooms.add(store.createRoom("Зал"));
                for(boolean client:new boolean[]{true,false}){
                    AtomicBoolean home=new AtomicBoolean();
                    NativePricePresetDialog dialog=new NativePricePresetDialog(
                            activity,store,project,Collections.singletonList(project),
                            client,()->{});
                    dialog.setOnHomeRequested(()->home.set(true));
                    dialog.show();
                    try{
                        View window=dialog.getWindow().getDecorView();
                        assertNotNull(window.findViewWithTag("native_fullscreen_dialog_nav"));
                        assertNotNull(window.findViewWithTag("native_price_nav_home"));
                        assertNotNull(window.findViewWithTag("native_price_nav_estimate"));
                        assertNotNull(window.findViewWithTag("native_price_nav_reorder"));
                        assertNotNull(window.findViewWithTag("native_price_nav_done"));
                        window.findViewWithTag("native_price_nav_home").performClick();
                        assertTrue(home.get());
                    }finally{
                        if(dialog.isShowing())dialog.dismiss();
                    }
                }
            });
        }
    }

    private static int dp(Context context,int value){
        return Math.round(value*context.getResources().getDisplayMetrics().density);
    }
}
