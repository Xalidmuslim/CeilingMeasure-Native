package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class NativeProfessionalEstimateTableUiTest {
    @Test public void editableColumnsAndActionsStayAlignedWithLongNamesAndLargeAmounts(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                NativeProjectStore.RoomData room=store.createRoom("Гостиная");
                NativeProjectStore.MaterialData first=material(
                        "Очень длинное название дополнительной декоративной вставки",
                        "шт",3f,1250000f);
                NativeProjectStore.MaterialData second=material("Лампы","шт",2f,500f);
                room.clientCustomItems.add(first);
                room.clientCustomItems.add(second);
                project.rooms.add(room);
                NativeEstimateDialog dialog=new NativeEstimateDialog(activity,store,
                        project,NativeEstimateDialog.Mode.CLIENT,()->{});
                dialog.show();
                try{
                    View root=dialog.getWindow().getDecorView();
                    NativeEstimateTableColumns widths=NativeEstimateTableColumns
                            .forScreenWidthDp(activity.getResources()
                                    .getConfiguration().screenWidthDp);
                    assertNull("repeated per-room header wastes a line",
                            root.findViewWithTag("native_estimate_table_header_"+room.id));
                    TextView roomMetrics=root.findViewWithTag(
                            "native_estimate_room_metrics_"+room.id);
                    assertNotNull(roomMetrics);
                    int expectedOrientation=
                            activity.getResources().getConfiguration().screenWidthDp>=360
                            && activity.getResources().getConfiguration().fontScale<=1.10f
                            && NativeUiPrefs.load(activity).scale
                                   <=1.10f*NativeUiPrefs.defaults().scale
                            ?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL;
                    assertEquals("room headings adapt to text size without clipping",
                            expectedOrientation,
                            ((LinearLayout)roomMetrics.getParent()).getOrientation());
                    String firstKey="custom:"+first.id+"_"+room.id;
                    String secondKey="custom:"+second.id+"_"+room.id;
                    LinearLayout formulaA=root.findViewWithTag(
                            "native_estimate_formula_"+firstKey);
                    LinearLayout formulaB=root.findViewWithTag(
                            "native_estimate_formula_"+secondKey);
                    assertNotNull(formulaA);
                    assertNotNull(formulaB);
                    assertEquals("rows use the same six-column layout",6,
                            formulaA.getChildCount());
                    assertEquals(formulaA.getChildCount(),formulaB.getChildCount());
                    assertEquals(dp(activity,widths.quantityDp),
                            formulaA.getChildAt(0).getLayoutParams().width);
                    assertEquals(dp(activity,widths.unitDp),
                            formulaA.getChildAt(1).getLayoutParams().width);
                    assertEquals(dp(activity,widths.multiplyDp),
                            formulaA.getChildAt(2).getLayoutParams().width);
                    assertEquals(dp(activity,widths.priceDp),
                            formulaA.getChildAt(3).getLayoutParams().width);
                    assertEquals(1f,
                            ((LinearLayout.LayoutParams)formulaA.getChildAt(4)
                                    .getLayoutParams()).weight,0f);
                    assertEquals(dp(activity,widths.actionsDp),
                            formulaA.getChildAt(5).getLayoutParams().width);
                    assertEquals("right-hand action cell is never mixed into formula",
                            "native_estimate_actions_"+firstKey,
                            formulaA.getChildAt(5).getTag());
                    assertEquals("right action stays at same width for all rows",
                            formulaA.getChildAt(5).getLayoutParams().width,
                            formulaB.getChildAt(5).getLayoutParams().width);

                    TextView title=root.findViewWithTag("native_estimate_title_"+firstKey);
                    assertNotNull(title);
                    assertEquals(2,title.getMaxLines());
                    assertEquals(first.name,title.getText().toString());

                    Button qty=root.findViewWithTag("native_estimate_qty_"+firstKey);
                    Button price=root.findViewWithTag("native_estimate_price_"+firstKey);
                    assertNotNull(qty);
                    assertNotNull(price);
                    assertEquals(dp(activity,42),qty.getLayoutParams().height);
                    assertEquals(dp(activity,42),price.getLayoutParams().height);
                    assertTrue("material row should not keep old blank vertical gutters",
                            formulaA.getParent() instanceof LinearLayout);
                    LinearLayout item=(LinearLayout)formulaA.getParent();
                    assertTrue(item.getPaddingTop()<=dp(activity,1));
                    assertEquals(0,item.getPaddingBottom());
                    assertEquals("1 250 000 ₽",price.getText().toString());
                    TextView subtotal=root.findViewWithTag(
                            "native_estimate_total_"+firstKey);
                    assertEquals("3 750 000 ₽",subtotal.getText().toString());
                    assertNotNull(root.findViewWithTag("native_fullscreen_dialog_nav"));
                    assertNotNull(root.findViewWithTag(
                            "native_client_add_material_"+room.id));
                }finally{dialog.dismiss();}
            });
        }
    }

    @Test public void numericEditsUpdateOnlyCellsWithoutRebuildingNavigationOrScroll(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                NativeProjectStore.RoomData room=store.createRoom("Зал");
                room.extraQty.put("light",2f);
                room.clientPricing.put("light",500f);
                project.rooms.add(room);
                NativeEstimateDialog dialog=new NativeEstimateDialog(activity,store,
                        project,NativeEstimateDialog.Mode.CLIENT,()->{});
                dialog.show();
                try{
                    View root=dialog.getWindow().getDecorView();
                    View dock=root.findViewWithTag("native_fullscreen_dialog_nav");
                    View shell=root.findViewWithTag("native_fullscreen_dialog_shell");
                    ScrollView scroll=(ScrollView)((FrameLayout)shell).getChildAt(0);
                    View row=root.findViewWithTag(
                            "native_estimate_line_light_"+room.id);
                    assertNotNull(row);
                    room.clientPricing.put("light",1000f);
                    assertTrue(dialog.refreshVisibleValues(project,room));
                    assertSame(dock,root.findViewWithTag("native_fullscreen_dialog_nav"));
                    assertSame(shell,root.findViewWithTag("native_fullscreen_dialog_shell"));
                    assertSame(scroll,((FrameLayout)shell).getChildAt(0));
                    assertSame(row,root.findViewWithTag(
                            "native_estimate_line_light_"+room.id));
                    TextView price=root.findViewWithTag(
                            "native_estimate_price_light_"+room.id);
                    TextView rowTotal=root.findViewWithTag(
                            "native_estimate_total_light_"+room.id);
                    assertEquals("1 000 ₽",price.getText().toString());
                    assertEquals("2 000 ₽",rowTotal.getText().toString());
                    TextView grand=root.findViewWithTag("native_estimate_grand_total");
                    assertEquals(
                            NativeMoney.format(NativeEstimateEngine.clientCost(room).total)+" ₽",
                            grand.getText().toString());

                    // A removed row requires exactly one structural rebuild.
                    room.extraQty.put("light",0f);
                    assertFalse(dialog.refreshVisibleValues(project,room));
                }finally{dialog.dismiss();}
            });
        }
    }

    @Test public void oneFourAndTenRoomsPreserveTableAndLastRoomScrollAccess(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(intent)){
            for(int n:new int[]{1,4,10}){
                AtomicReference<NativeEstimateDialog> dialogRef=new AtomicReference<>();
                AtomicReference<NativeProjectStore.ProjectData> projectRef=
                        new AtomicReference<>();
                scenario.onActivity(activity->{
                    NativeProjectStore store=new NativeProjectStore(activity);
                    NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
                    for(int i=0;i<n;i++){
                        NativeProjectStore.RoomData room=store.createRoom("Комната "+(i+1));
                        room.extraQty.put("light",1f);
                        room.clientPricing.put("light",500f);
                        project.rooms.add(room);
                    }
                    projectRef.set(project);
                    NativeEstimateDialog dialog=new NativeEstimateDialog(activity,store,
                            project,NativeEstimateDialog.Mode.CLIENT,()->{});
                    dialogRef.set(dialog);
                    dialog.show();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity->{
                    NativeEstimateDialog dialog=dialogRef.get();
                    try{
                        View root=dialog.getWindow().getDecorView();
                        View shell=root.findViewWithTag("native_fullscreen_dialog_shell");
                        assertNotNull(shell);
                        View nav=root.findViewWithTag("native_fullscreen_dialog_nav");
                        assertNotNull(nav);
                        ScrollView scroll=(ScrollView)((FrameLayout)shell).getChildAt(0);
                        for(NativeProjectStore.RoomData room:projectRef.get().rooms){
                            assertNotNull(root.findViewWithTag(
                                    "native_client_room_"+room.id));
                            assertNull("room captions must not repeat",
                                    root.findViewWithTag(
                                            "native_estimate_table_header_"+room.id));
                            View add=root.findViewWithTag(
                                    "native_client_add_material_"+room.id);
                            assertNotNull(add);
                            assertTrue("compact add button must not be a full-width panel",
                                    add.getLayoutParams().width==ViewGroup.LayoutParams.WRAP_CONTENT);
                            assertTrue("compact add control height",
                                    add.getLayoutParams().height<=dp(activity,36));
                        }
                        FrameLayout.LayoutParams body=
                                (FrameLayout.LayoutParams)scroll.getLayoutParams();
                        assertTrue("last room can scroll above floating nav",
                                body.bottomMargin>nav.getLayoutParams().height);
                        scroll.fullScroll(View.FOCUS_DOWN);
                        assertNotNull(root.findViewWithTag(
                                "native_client_add_material_"
                                        +projectRef.get().rooms.get(n-1).id));
                        assertEquals(View.VISIBLE,nav.getVisibility());
                    }finally{dialog.dismiss();}
                });
            }
        }
    }

    private static NativeProjectStore.MaterialData material(
            String name,String unit,float qty,float price){
        NativeProjectStore.MaterialData m=new NativeProjectStore.MaterialData();
        m.name=name;
        m.unit=unit;
        m.qty=qty;
        m.price=price;
        return m;
    }

    private static int dp(Context c,int value){
        return Math.round(value*c.getResources().getDisplayMetrics().density);
    }
}
