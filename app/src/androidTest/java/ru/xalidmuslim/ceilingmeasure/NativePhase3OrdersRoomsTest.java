package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class NativePhase3OrdersRoomsTest {
    private Instrumentation instrumentation;
    private Context context;

    @Before
    public void resetState() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        context.getSharedPreferences(NativeProjectStore.PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();
        context.getSharedPreferences(NativeUiPrefs.PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test
    public void homeAndWorkspaceNewOrderUseSameCanonicalFactoryAndNoOrderNumber()
            throws Exception {
        NativeProjectStore.ProjectData homeProject;

        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                NativeWorkspaceActivity.class.getName(), null, false);
        try (ActivityScenario<NativeHomeActivity> homeScenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            AtomicReference<NativeHomeActivity> homeRef = new AtomicReference<>();
            homeScenario.onActivity(homeRef::set);
            NativeHomeActivity home = homeRef.get();
            assertNotNull(home);

            instrumentation.runOnMainSync(() -> {
                View newOrder = home.getWindow().getDecorView()
                        .findViewWithTag("native_home_new");
                assertNotNull(newOrder);
                newOrder.performClick();
            });

            Activity opened = instrumentation.waitForMonitorWithTimeout(monitor, 5000);
            assertTrue(opened instanceof NativeWorkspaceActivity);
            NativeWorkspaceActivity workspace = (NativeWorkspaceActivity) opened;
            instrumentation.waitForIdleSync();
            homeProject = workspace.getProjectForTest();
            assertCanonicalNewProject(homeProject);
            assertTrue(workspace.getWindow().getDecorView()
                    .findViewWithTag("native_order_number") == null);
            instrumentation.runOnMainSync(workspace::finish);
        } finally {
            instrumentation.removeMonitor(monitor);
        }

        context.getSharedPreferences(NativeProjectStore.PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();

        try (ActivityScenario<NativeWorkspaceActivity> direct =
                     ActivityScenario.launch(newOrderIntent())) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            direct.onActivity(ref::set);
            NativeWorkspaceActivity workspace = ref.get();
            assertNotNull(workspace);
            NativeProjectStore.ProjectData directProject = workspace.getProjectForTest();
            assertCanonicalNewProject(directProject);

            assertEquals(homeProject.name, directProject.name);
            assertEquals(homeProject.phone, directProject.phone);
            assertEquals(homeProject.address, directProject.address);
            assertEquals(homeProject.status, directProject.status);
            assertEquals(homeProject.rooms.size(), directProject.rooms.size());
            assertEquals(homeProject.rooms.get(0).name, directProject.rooms.get(0).name);

            JSONObject db = new JSONObject(
                    new NativeProjectStore(context).rawDatabaseForTest());
            JSONObject order = db.getJSONArray("orders").getJSONObject(0);
            assertFalse(order.has("number"));
            assertFalse(order.has("savedName"));
            assertFalse(order.has("activeRoom"));
        }
    }

    @Test
    public void orderFieldsAutosaveLiveAndBlankHeadingMatchesGolden() throws Exception {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            instrumentation.runOnMainSync(() -> {
                EditText name = tagged(activity, "native_order_name", EditText.class);
                EditText phone = tagged(activity, "native_order_phone", EditText.class);
                EditText address = tagged(activity, "native_order_address", EditText.class);
                name.setText("   ");
                phone.setText(" +7 (999)  12 ");
                address.setText("  ул. Мира, 5  ");
            });
            instrumentation.waitForIdleSync();

            TextView heading = tagged(activity, "native_order_title", TextView.class);
            assertEquals("Без имени", heading.getText().toString());
            assertEquals("   ", activity.getProjectForTest().name);
            assertEquals(" +7 (999)  12 ", activity.getProjectForTest().phone);
            assertEquals("  ул. Мира, 5  ", activity.getProjectForTest().address);

            instrumentation.runOnMainSync(() ->
                    tagged(activity, "native_order_name", EditText.class)
                            .setText("  Иван  "));
            instrumentation.waitForIdleSync();
            assertEquals("Иван", heading.getText().toString());

            NativeProjectStore.ProjectData restored =
                    new NativeProjectStore(context).loadCurrent();
            assertEquals("  Иван  ", restored.name);
            assertEquals(" +7 (999)  12 ", restored.phone);
            assertEquals("  ул. Мира, 5  ", restored.address);
        }
    }

    @Test
    public void orderDeleteCancelPositiveAndFinalRegenerationMatchGolden() throws Exception {
        NativeProjectStore store = new NativeProjectStore(context);
        NativeProjectStore.ProjectData older = store.newProject();
        older.name = "Первый";
        store.saveCurrent(older);
        NativeProjectStore.ProjectData current = store.newProject();
        current.name = "Второй";
        store.saveCurrent(current);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(loadCurrentIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            assertEquals(current.id, activity.getProjectForTest().id);

            click(activity, "native_order_delete");
            NativePhase3ConfirmDialog confirm = activity.getPhase3ConfirmDialogForTest();
            assertNotNull(confirm);
            assertTrue(confirm.isShowing());
            assertDialogText(confirm, "Удалить заказ?");
            assertDialogText(confirm, "Карточка заказчика и все комнаты будут удалены.");

            clickDialog(confirm, "native_delete_order_confirm_cancel");
            instrumentation.waitForIdleSync();
            assertEquals(2, orderCount(new NativeProjectStore(context)));
            assertEquals(current.id, activity.getProjectForTest().id);

            click(activity, "native_order_delete");
            confirm = activity.getPhase3ConfirmDialogForTest();
            assertNotNull(confirm);
            clickDialog(confirm, "native_delete_order_confirm_confirm");
            instrumentation.waitForIdleSync();

            assertEquals(older.id, activity.getProjectForTest().id);
            assertEquals(1, orderCount(new NativeProjectStore(context)));

            click(activity, "native_order_delete");
            confirm = activity.getPhase3ConfirmDialogForTest();
            assertNotNull(confirm);
            clickDialog(confirm, "native_delete_order_confirm_confirm");
            instrumentation.waitForIdleSync();

            NativeProjectStore.ProjectData fresh = activity.getProjectForTest();
            assertCanonicalNewProject(fresh);
            assertNotEquals(older.id, fresh.id);
            assertEquals(1, orderCount(new NativeProjectStore(context)));
        }
    }

    @Test
    public void roomPickerShowsExactTypesAndAddsExactNames() {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);

            click(activity, "native_room_add");
            NativeRoomPickerDialog picker = activity.getRoomPickerDialogForTest();
            assertNotNull(picker);
            assertTrue(picker.isShowing());
            View root = dialogRoot(picker);
            assertNotNull(root.findViewWithTag("native_room_type_Комната"));
            assertNotNull(root.findViewWithTag("native_room_type_Коридор"));
            assertNotNull(root.findViewWithTag("native_room_type_Ванная"));
            assertNotNull(root.findViewWithTag("native_room_type_Другое"));
            assertTrue(containsText(root, "Добавить помещение"));
            assertTrue(containsText(
                    root,
                    "Название потом можно изменить долгим нажатием по комнате."));
            clickDialog(picker, "native_room_picker_close");

            addRoom(activity, "Комната");
            addRoom(activity, "Коридор");
            addRoom(activity, "Коридор");
            addRoom(activity, "Ванная");
            addRoom(activity, "Другое");
            addRoom(activity, "Другое");

            assertRoomNames(activity,
                    "Комната 1",
                    "Комната 2",
                    "Коридор",
                    "Коридор 2",
                    "Ванная",
                    "Другое",
                    "Другое 2");
            assertEquals(6, activity.getProjectForTest().activeRoom);
        }
    }

    @Test
    public void roomTapSelectsRuntimeOnlyAndLeavesRoomDataUntouched() throws Exception {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            addRoom(activity, "Коридор");
            assertEquals(1, activity.getProjectForTest().activeRoom);

            String before0 = NativeProjectStore.snapshotRoom(
                    activity.getProjectForTest().rooms.get(0));
            String before1 = NativeProjectStore.snapshotRoom(
                    activity.getProjectForTest().rooms.get(1));

            click(activity, "native_room_0");
            assertEquals(0, activity.getProjectForTest().activeRoom);
            assertTrue(activity.getRoomActionDialogForTest() == null);
            assertEquals(before0, NativeProjectStore.snapshotRoom(
                    activity.getProjectForTest().rooms.get(0)));
            assertEquals(before1, NativeProjectStore.snapshotRoom(
                    activity.getProjectForTest().rooms.get(1)));

            JSONObject order = new JSONObject(
                    new NativeProjectStore(context).rawDatabaseForTest())
                    .getJSONArray("orders").getJSONObject(0);
            assertFalse(order.has("activeRoom"));
        }
    }

    @Test
    public void roomLongPressUses330MsCancelsBelowThresholdAndAfter22PxMovement() {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            addRoom(activity, "Коридор");
            assertEquals(1, activity.getProjectForTest().activeRoom);

            View first = tagged(activity, "native_room_0", View.class);
            dispatchDirect(first, MotionEvent.ACTION_DOWN, 10f, 10f);
            SystemClock.sleep(250);
            dispatchDirect(first, MotionEvent.ACTION_UP, 10f, 10f);
            instrumentation.waitForIdleSync();
            assertTrue(activity.getRoomActionDialogForTest() == null);

            // Short tap rebuilds the row, so every subsequent gesture must target
            // the currently attached room-card view.
            first = tagged(activity, "native_room_0", View.class);
            dispatchDirect(first, MotionEvent.ACTION_DOWN, 10f, 10f);
            SystemClock.sleep(80);
            dispatchDirect(first, MotionEvent.ACTION_MOVE, 33.1f, 10f);
            SystemClock.sleep(300);
            dispatchDirect(first, MotionEvent.ACTION_UP, 33.1f, 10f);
            instrumentation.waitForIdleSync();
            assertTrue(activity.getRoomActionDialogForTest() == null);

            first = tagged(activity, "native_room_0", View.class);
            dispatchDirect(first, MotionEvent.ACTION_DOWN, 10f, 10f);
            SystemClock.sleep(360);
            instrumentation.waitForIdleSync();
            NativeRoomActionDialog action = activity.getRoomActionDialogForTest();
            assertNotNull(action);
            assertTrue(action.isShowing());
            assertEquals(0, activity.getProjectForTest().activeRoom);
            dispatchDirect(first, MotionEvent.ACTION_UP, 10f, 10f);
            clickDialog(action, "native_room_action_close");
        }
    }

    @Test
    public void verticalSwipeStartingOnRoomCardScrollsWorkspaceAndDoesNotOpenMenu() {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            View room = tagged(activity, "native_room_0", View.class);
            ScrollView scroll = tagged(activity, "native_editor_scroll", ScrollView.class);
            int before = scroll.getScrollY();

            int[] loc = new int[2];
            instrumentation.runOnMainSync(() -> room.getLocationOnScreen(loc));
            float x = loc[0] + room.getWidth() / 2f;
            float y = loc[1] + room.getHeight() / 2f;
            float endY = Math.max(30f, y - 180f);

            long down = SystemClock.uptimeMillis();
            sendScreenPointer(down, down, MotionEvent.ACTION_DOWN, x, y);
            sendScreenPointer(down, down + 80, MotionEvent.ACTION_MOVE, x, endY);
            sendScreenPointer(down, down + 140, MotionEvent.ACTION_UP, x, endY);
            SystemClock.sleep(300);
            instrumentation.waitForIdleSync();

            assertTrue("room-origin swipe must scroll page",
                    scroll.getScrollY() > before);
            assertTrue(activity.getRoomActionDialogForTest() == null);
        }
    }

    @Test
    public void roomActionSheetUsesCustomKeyboardAndRenameRules() {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            openRoomAction(activity, 0);
            NativeRoomActionDialog action = activity.getRoomActionDialogForTest();
            assertNotNull(action);
            View root = dialogRoot(action);

            assertTrue(containsText(root, "Комната"));
            assertTrue(containsText(
                    root,
                    "Удерживай комнату примерно полсекунды → переименовать или удалить"));
            assertFalse(containsEditText(root));
            assertNotNull(root.findViewWithTag("native_room_alpha_key_й"));
            assertNotNull(root.findViewWithTag("native_room_alpha_space"));
            assertNotNull(root.findViewWithTag("native_room_alpha_dash"));
            assertNotNull(root.findViewWithTag("native_room_alpha_back"));
            assertNotNull(root.findViewWithTag("native_room_alpha_clear"));

            clickDialog(action, "native_room_alpha_clear");
            clickDialog(action, "native_room_action_rename");
            instrumentation.waitForIdleSync();
            assertTrue(action.isShowing());
            assertEquals("Введите название комнаты", activity.getPhase3LastToastForTest());
            assertEquals("Комната 1", activity.getProjectForTest().rooms.get(0).name);

            clickDialog(action, "native_room_alpha_key_з");
            clickDialog(action, "native_room_alpha_key_а");
            clickDialog(action, "native_room_alpha_key_л");
            assertEquals("Зал", action.currentValueForTest());
            clickDialog(action, "native_room_action_rename");
            instrumentation.waitForIdleSync();

            assertFalse(action.isShowing());
            assertEquals("Зал", activity.getProjectForTest().rooms.get(0).name);
            assertEquals("Комната переименована", activity.getPhase3LastToastForTest());
            assertEquals("Зал",
                    new NativeProjectStore(context).loadCurrent().rooms.get(0).name);
        }
    }

    @Test
    public void duplicateRoomFromActionSheetInsertsAfterSourceSelectsCopyAndUnlocksIt()
            throws Exception {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            NativeProjectStore.RoomData source = activity.getProjectForTest().rooms.get(0);
            instrumentation.runOnMainSync(() -> {
                source.notes = "Не менять";
                source.geometryLocked = true;
                source.savedAt = 123456L;
                source.pricing.put("canvas", 321f);
                source.extraQty.put("light", 4f);
                try {
                    source.raw.put("phase3Future", "keep");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                new NativeProjectStore(activity).saveCurrent(activity.getProjectForTest());
            });
            String sourceId = source.id;

            openRoomAction(activity, 0);
            NativeRoomActionDialog action = activity.getRoomActionDialogForTest();
            clickDialog(action, "native_room_action_duplicate");
            instrumentation.waitForIdleSync();

            assertEquals(2, activity.getProjectForTest().rooms.size());
            NativeProjectStore.RoomData original = activity.getProjectForTest().rooms.get(0);
            NativeProjectStore.RoomData copy = activity.getProjectForTest().rooms.get(1);
            assertEquals(sourceId, original.id);
            assertNotEquals(original.id, copy.id);
            assertEquals("Комната 1 копия", copy.name);
            assertEquals(0L, copy.savedAt);
            assertFalse(copy.geometryLocked);
            assertEquals("Не менять", copy.notes);
            assertEquals(321f, copy.pricing.get("canvas"), .0001f);
            assertEquals(4f, copy.extraQty.get("light"), .0001f);
            assertEquals("keep", copy.raw.getString("phase3Future"));
            assertEquals(1, activity.getProjectForTest().activeRoom);
            assertEquals("Комната продублирована", activity.getPhase3LastToastForTest());

            copy.notes = "Изменено";
            assertEquals("Не менять", original.notes);
            assertTrue(original.geometryLocked);
            assertEquals(123456L, original.savedAt);
        }
    }

    @Test
    public void deleteRoomProtectsLastSupportsCancelActiveAndInactiveTargets() {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);

            openRoomAction(activity, 0);
            NativeRoomActionDialog action = activity.getRoomActionDialogForTest();
            clickDialog(action, "native_room_action_delete");
            instrumentation.waitForIdleSync();
            assertEquals("В заказе должна остаться хотя бы одна комната",
                    activity.getPhase3LastToastForTest());
            assertEquals(1, activity.getProjectForTest().rooms.size());
            assertTrue(activity.getPhase3ConfirmDialogForTest() == null);

            addRoom(activity, "Коридор");
            String firstId = activity.getProjectForTest().rooms.get(0).id;
            String secondId = activity.getProjectForTest().rooms.get(1).id;

            openRoomAction(activity, 1);
            action = activity.getRoomActionDialogForTest();
            clickDialog(action, "native_room_action_delete");
            NativePhase3ConfirmDialog confirm = activity.getPhase3ConfirmDialogForTest();
            assertNotNull(confirm);
            assertDialogText(confirm, "Удалить комнату «Коридор»?");
            assertDialogText(confirm, "Будут удалены её чертёж, размеры и диагонали.");
            clickDialog(confirm, "native_delete_room_confirm_cancel");
            assertEquals(2, activity.getProjectForTest().rooms.size());

            openRoomAction(activity, 1);
            action = activity.getRoomActionDialogForTest();
            clickDialog(action, "native_room_action_delete");
            confirm = activity.getPhase3ConfirmDialogForTest();
            clickDialog(confirm, "native_delete_room_confirm_confirm");
            instrumentation.waitForIdleSync();
            assertEquals(1, activity.getProjectForTest().rooms.size());
            assertEquals(firstId, activity.getProjectForTest().rooms.get(0).id);
            assertEquals(0, activity.getProjectForTest().activeRoom);

            addRoom(activity, "Коридор");
            assertEquals(2, activity.getProjectForTest().rooms.size());
            String targetId = activity.getProjectForTest().rooms.get(1).id;
            openRoomAction(activity, 1);
            action = activity.getRoomActionDialogForTest();

            instrumentation.runOnMainSync(() ->
                    activity.getProjectForTest().activeRoom = 0);
            clickDialog(action, "native_room_action_delete");
            confirm = activity.getPhase3ConfirmDialogForTest();
            clickDialog(confirm, "native_delete_room_confirm_confirm");
            instrumentation.waitForIdleSync();

            assertEquals(1, activity.getProjectForTest().rooms.size());
            assertEquals(firstId, activity.getProjectForTest().rooms.get(0).id);
            assertNotEquals(targetId, activity.getProjectForTest().rooms.get(0).id);
            assertEquals(0, activity.getProjectForTest().activeRoom);
            assertNotEquals(secondId, targetId);
        }
    }

    @Test
    public void roomReadyIndicatorUsesExistingValidatorWithoutChangingIt() {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            NativeProjectStore.RoomData room = activity.getProjectForTest().rooms.get(0);

            instrumentation.runOnMainSync(() -> {
                makeMeasuredSquare(room, 500f);
                activity.onChanged();
            });
            instrumentation.waitForIdleSync();
            Button roomButton = tagged(activity, "native_room_0", Button.class);
            assertTrue(roomButton.getText().toString().contains("✓"));

            instrumentation.runOnMainSync(() -> {
                room.edges.get(0).value = 0f;
                activity.onChanged();
            });
            instrumentation.waitForIdleSync();
            assertFalse(roomButton.getText().toString().contains("✓"));
        }
    }

    @Test
    public void orderAndRoomsCollapsePersistWithoutTouchingOtherCollapseKeys() throws Exception {
        NativeProjectStore store = new NativeProjectStore(context);
        JSONObject db = new JSONObject(store.rawDatabaseForTest());
        db.getJSONObject("uiCollapse").put("drawing", true).put("estimate", true);
        store.replaceDatabaseForTest(db);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            click(activity, "native_collapse_order");
            click(activity, "native_collapse_rooms");
            SystemClock.sleep(220);
            instrumentation.waitForIdleSync();
            assertEquals(View.GONE,
                    tagged(activity, "native_order_body", View.class).getVisibility());
            assertEquals(View.GONE,
                    tagged(activity, "native_rooms_body", View.class).getVisibility());
            NativeProjectStore now = new NativeProjectStore(context);
            assertTrue(now.isUiCollapsed("order"));
            assertTrue(now.isUiCollapsed("rooms"));
            JSONObject persisted = new JSONObject(now.rawDatabaseForTest())
                    .getJSONObject("uiCollapse");
            assertTrue(persisted.getBoolean("drawing"));
            assertTrue(persisted.getBoolean("estimate"));
        }

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(loadCurrentIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            assertEquals(View.GONE,
                    tagged(activity, "native_order_body", View.class).getVisibility());
            assertEquals(View.GONE,
                    tagged(activity, "native_rooms_body", View.class).getVisibility());
        }
    }

    @Test
    public void androidBackClosesPhase3OverlaysBeforeLeavingWorkspace() {
        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);

            click(activity, "native_room_add");
            assertTrue(activity.getRoomPickerDialogForTest().isShowing());
            instrumentation.runOnMainSync(() ->
                    activity.getOnBackPressedDispatcher().onBackPressed());
            instrumentation.waitForIdleSync();
            assertTrue(activity.getRoomPickerDialogForTest() == null);

            openRoomAction(activity, 0);
            assertTrue(activity.getRoomActionDialogForTest().isShowing());
            instrumentation.runOnMainSync(() ->
                    activity.getOnBackPressedDispatcher().onBackPressed());
            instrumentation.waitForIdleSync();
            assertTrue(activity.getRoomActionDialogForTest() == null);

            click(activity, "native_order_delete");
            assertTrue(activity.getPhase3ConfirmDialogForTest().isShowing());
            instrumentation.runOnMainSync(() ->
                    activity.getOnBackPressedDispatcher().onBackPressed());
            instrumentation.waitForIdleSync();
            assertTrue(activity.getPhase3ConfirmDialogForTest() == null);

            assertFalse(activity.isFinishing());
        }
    }

    @Test
    public void phase3VisualEvidenceAtRequestedWidth() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        String requested = args.getString("phase3Width");
        Assume.assumeTrue("Phase-3 screenshot evidence runs only in its API35 job",
                requested != null && !requested.isEmpty());

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(newOrderIntent())) {
            NativeWorkspaceActivity activity = activity(scenario);
            int actual = activity.getResources().getConfiguration().screenWidthDp;
            int expected = Integer.parseInt(requested);
            assertTrue("expected ~" + expected + "dp but was " + actual,
                    Math.abs(actual - expected) <= 2);

            capture("01_fresh_order");

            instrumentation.runOnMainSync(() -> {
                tagged(activity, "native_order_name", EditText.class).setText("Квартира 189");
                tagged(activity, "native_order_phone", EditText.class)
                        .setText("+7 900 000-00-00");
                tagged(activity, "native_order_address", EditText.class).setText("Нальчик");
            });
            capture("02_order_fields");

            addRoom(activity, "Коридор");
            addRoom(activity, "Ванная");
            addRoom(activity, "Другое");
            capture("03_four_rooms");

            click(activity, "native_room_2");
            capture("04_active_non_first");

            NativeProjectStore.RoomData active = activity.getProjectForTest().rooms.get(2);
            instrumentation.runOnMainSync(() -> {
                makeMeasuredSquare(active, 500f);
                activity.onChanged();
            });
            capture("05_room_ready");

            click(activity, "native_room_add");
            capture("06_room_picker");
            clickDialog(activity.getRoomPickerDialogForTest(), "native_room_picker_close");

            openRoomAction(activity, 1);
            capture("07_room_action_sheet");
            capture("08_room_rename_keyboard");
            clickDialog(activity.getRoomActionDialogForTest(), "native_room_action_close");

            click(activity, "native_collapse_order");
            SystemClock.sleep(180);
            capture("09_order_collapsed");
            click(activity, "native_collapse_order");
            SystemClock.sleep(180);

            click(activity, "native_collapse_rooms");
            SystemClock.sleep(180);
            capture("10_rooms_collapsed");
            click(activity, "native_collapse_rooms");
            SystemClock.sleep(180);

            click(activity, "native_order_delete");
            capture("11_delete_order_confirm");
            clickDialog(activity.getPhase3ConfirmDialogForTest(),
                    "native_delete_order_confirm_cancel");

            openRoomAction(activity, 1);
            clickDialog(activity.getRoomActionDialogForTest(), "native_room_action_delete");
            capture("12_delete_room_confirm");
            clickDialog(activity.getPhase3ConfirmDialogForTest(),
                    "native_delete_room_confirm_cancel");

            writeEvidenceMetric(activity, expected);
        }
    }

    private Intent newOrderIntent() {
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private Intent loadCurrentIntent() {
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private NativeWorkspaceActivity activity(
            ActivityScenario<NativeWorkspaceActivity> scenario) {
        AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
        scenario.onActivity(ref::set);
        instrumentation.waitForIdleSync();
        assertNotNull(ref.get());
        return ref.get();
    }

    private int orderCount(NativeProjectStore store) {
        try {
            return new JSONObject(store.rawDatabaseForTest())
                    .getJSONArray("orders").length();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void assertCanonicalNewProject(NativeProjectStore.ProjectData project) {
        assertNotNull(project);
        assertEquals("", project.name);
        assertEquals("", project.phone);
        assertEquals("", project.address);
        assertEquals("Замер", project.status);
        assertTrue(project.pricingMigratedV59);
        assertEquals(1, project.rooms.size());
        assertEquals("Комната 1", project.rooms.get(0).name);
        assertEquals(0f, project.legacyPricing.get("canvas"), .0001f);
        assertEquals(0f, project.legacyPricing.get("baguette"), .0001f);
        assertEquals(0f, project.legacyPricing.get("insert"), .0001f);
    }

    private void assertRoomNames(NativeWorkspaceActivity activity, String... names) {
        assertEquals(names.length, activity.getProjectForTest().rooms.size());
        for (int i = 0; i < names.length; i++) {
            assertEquals(names[i], activity.getProjectForTest().rooms.get(i).name);
        }
    }

    private void addRoom(NativeWorkspaceActivity activity, String type) {
        click(activity, "native_room_add");
        NativeRoomPickerDialog picker = activity.getRoomPickerDialogForTest();
        assertNotNull(picker);
        assertTrue(picker.isShowing());
        clickDialog(picker, "native_room_type_" + type);
        instrumentation.waitForIdleSync();
        assertTrue(activity.getRoomPickerDialogForTest() == null);
    }

    private void openRoomAction(NativeWorkspaceActivity activity, int index) {
        View room = tagged(activity, "native_room_" + index, View.class);
        dispatchDirect(room, MotionEvent.ACTION_DOWN, 10f, 10f);
        SystemClock.sleep(360);
        instrumentation.waitForIdleSync();
        assertNotNull(activity.getRoomActionDialogForTest());
        assertTrue(activity.getRoomActionDialogForTest().isShowing());
        dispatchDirect(room, MotionEvent.ACTION_UP, 10f, 10f);
        instrumentation.waitForIdleSync();
    }

    private void click(NativeWorkspaceActivity activity, String tag) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.getWindow().getDecorView().findViewWithTag(tag);
            assertNotNull("Missing view tag " + tag, view);
            assertTrue("Click failed for " + tag, view.performClick());
        });
        instrumentation.waitForIdleSync();
    }

    private void clickDialog(android.app.Dialog dialog, String tag) {
        assertNotNull(dialog);
        instrumentation.runOnMainSync(() -> {
            View view = dialogRoot(dialog).findViewWithTag(tag);
            assertNotNull("Missing dialog tag " + tag, view);
            assertTrue("Dialog click failed for " + tag, view.performClick());
        });
        instrumentation.waitForIdleSync();
    }

    private void assertDialogText(android.app.Dialog dialog, String text) {
        assertTrue("Missing dialog text: " + text, containsText(dialogRoot(dialog), text));
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T tagged(Activity activity, String tag, Class<T> type) {
        View view = activity.getWindow().getDecorView().findViewWithTag(tag);
        assertNotNull("Missing view tag " + tag, view);
        assertTrue("Wrong view type for " + tag, type.isInstance(view));
        return (T) view;
    }

    private View dialogRoot(android.app.Dialog dialog) {
        View content = dialog.findViewById(android.R.id.content);
        assertNotNull(content);
        return content.getRootView();
    }

    private void dispatchDirect(View view, int action, float x, float y) {
        instrumentation.runOnMainSync(() -> {
            long now = SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            view.dispatchTouchEvent(event);
            event.recycle();
        });
    }

    private void sendScreenPointer(
            long downTime,
            long eventTime,
            int action,
            float x,
            float y) {
        MotionEvent event = MotionEvent.obtain(
                downTime, eventTime, action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        instrumentation.sendPointerSync(event);
        event.recycle();
    }

    private boolean containsText(View root, String needle) {
        if (root instanceof TextView) {
            CharSequence text = ((TextView) root).getText();
            if (text != null && text.toString().contains(needle)) return true;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsText(group.getChildAt(i), needle)) return true;
            }
        }
        return false;
    }

    private boolean containsEditText(View root) {
        if (root instanceof EditText) return true;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsEditText(group.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private void makeMeasuredSquare(NativeProjectStore.RoomData room, float size) {
        room.points.clear();
        room.edges.clear();
        room.diagonals.clear();

        NativeProjectStore.PointData a =
                new NativeProjectStore.PointData(100f, 100f);
        NativeProjectStore.PointData b =
                new NativeProjectStore.PointData(300f, 100f);
        NativeProjectStore.PointData c =
                new NativeProjectStore.PointData(300f, 300f);
        NativeProjectStore.PointData d =
                new NativeProjectStore.PointData(100f, 300f);
        room.points.add(a);
        room.points.add(b);
        room.points.add(c);
        room.points.add(d);

        NativeProjectStore.EdgeData ab =
                new NativeProjectStore.EdgeData(a.id, b.id);
        NativeProjectStore.EdgeData bc =
                new NativeProjectStore.EdgeData(b.id, c.id);
        NativeProjectStore.EdgeData cd =
                new NativeProjectStore.EdgeData(c.id, d.id);
        NativeProjectStore.EdgeData da =
                new NativeProjectStore.EdgeData(d.id, a.id);
        ab.value = size;
        bc.value = size;
        cd.value = size;
        da.value = size;
        room.edges.add(ab);
        room.edges.add(bc);
        room.edges.add(cd);
        room.edges.add(da);
    }

    private void capture(String state) throws Exception {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(180);
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull("Phase-3 screenshot capture failed for " + state, bitmap);

        int samples = 0;
        int stepX = Math.max(8, bitmap.getWidth() / 60);
        int stepY = Math.max(8, bitmap.getHeight() / 100);
        for (int y = bitmap.getHeight() / 8;
             y < bitmap.getHeight() * 7 / 8;
             y += stepY) {
            for (int x = 0; x < bitmap.getWidth(); x += stepX) {
                if ((bitmap.getPixel(x, y) & 0x00FFFFFF) < 0x00F4F4F4) samples++;
            }
        }
        assertTrue("Phase-3 screenshot is blank for " + state, samples > 40);

        File dir = new File(context.getExternalFilesDir(null), "phase3");
        assertTrue(dir.exists() || dir.mkdirs());
        int widthDp = context.getResources().getConfiguration().screenWidthDp;
        File out = new File(dir, String.format(
                Locale.US,
                "api%d_%ddp_%s.png",
                Build.VERSION.SDK_INT,
                widthDp,
                state));
        try (FileOutputStream stream = new FileOutputStream(out)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        assertTrue(out.length() > 0L);
        bitmap.recycle();
    }

    private void writeEvidenceMetric(Activity activity, int requested) throws Exception {
        File dir = new File(context.getExternalFilesDir(null), "phase3");
        assertTrue(dir.exists() || dir.mkdirs());
        int widthDp = activity.getResources().getConfiguration().screenWidthDp;
        File out = new File(dir, String.format(
                Locale.US,
                "api%d_%ddp_metrics.txt",
                Build.VERSION.SDK_INT,
                widthDp));
        try (FileWriter writer = new FileWriter(out, false)) {
            writer.write("requested_width_dp=" + requested + "\n");
            writer.write("actual_width_dp=" + widthDp + "\n");
            writer.write("states=12\n");
            writer.write("long_press_ms=" + NativeOrderRoomLogic.LONG_PRESS_MS + "\n");
            writer.write("movement_cancel_px="
                    + NativeOrderRoomLogic.LONG_PRESS_CANCEL_PX + "\n");
        }
    }
}
