package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NativeClientRoomMaterialUiTest {
    @Test
    public void everyClientEstimateRoomHasDirectAddMaterialButton() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try(ActivityScenario<NativeWorkspaceActivity> scenario=
                    ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                NativeProjectStore.RoomData first=store.createRoom("Офис");
                NativeProjectStore.RoomData second=store.createRoom("Кухня");
                project.rooms.add(first);
                project.rooms.add(second);

                NativeEstimateDialog dialog=new NativeEstimateDialog(
                        activity,store,project,NativeEstimateDialog.Mode.CLIENT,()->{});
                dialog.show();
                try{
                    View root=dialog.findViewById(android.R.id.content).getRootView();
                    assertNotNull(root.findViewWithTag(
                            "native_client_add_material_"+first.id));
                    assertNotNull(root.findViewWithTag(
                            "native_client_add_material_"+second.id));
                }finally{
                    dialog.dismiss();
                }
            });
        }
    }
}
