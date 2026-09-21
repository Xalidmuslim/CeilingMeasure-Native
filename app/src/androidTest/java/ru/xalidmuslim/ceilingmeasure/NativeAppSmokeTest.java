package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class NativeAppSmokeTest {

    @Test
    public void launcherResolvesToNativeHome() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(context.getPackageName());

        ResolveInfo resolved = context.getPackageManager().resolveActivity(intent, 0);
        assertNotNull(resolved);
        assertNotNull(resolved.activityInfo);
        assertEquals(NativeHomeActivity.class.getName(), resolved.activityInfo.name);
    }

    @Test
    public void nativeHomeHasCoreActionsAndNoWebView() {
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            AtomicReference<NativeHomeActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeHomeActivity activity = ref.get();
            assertNotNull(activity);

            View root = activity.getWindow().getDecorView();
            assertNotNull(root.findViewWithTag("native_home_new"));
            assertNotNull(root.findViewWithTag("native_home_saved"));
            assertNotNull(root.findViewWithTag("native_home_drawing"));
            assertNotNull(root.findViewWithTag("native_home_settings"));
            assertFalse(containsWebView(root));
        }
    }

    @Test
    public void nativeWorkspaceLoadsCanvasAndModesWithoutWebView() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(intent)) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            View root = activity.getWindow().getDecorView();
            assertNotNull(root.findViewWithTag("native_order_card"));
            assertNotNull(root.findViewWithTag("native_drawing_card"));
            assertNotNull(root.findViewWithTag("native_drawing_view"));
            assertNotNull(root.findViewWithTag("native_mode_wall"));
            assertNotNull(root.findViewWithTag("native_mode_points"));
            assertNotNull(root.findViewWithTag("native_mode_diagonal"));
            assertNotNull(root.findViewWithTag("native_mode_labels"));
            assertNotNull(root.findViewWithTag("native_save_measurement"));
            assertFalse(containsWebView(root));
        }
    }

    @Test
    public void nativeNumberPadUsesCustomKeysWithZeroBelowEight() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(intent)) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                NativeNumberPadDialog dialog = new NativeNumberPadDialog(
                        activity,
                        "Размер стены",
                        0f,
                        value -> {});
                dialog.show();

                View zero = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_number_pad_0");
                View eight = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_number_pad_8");
                View display = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_number_pad_display");
                assertNotNull(zero);
                assertNotNull(eight);
                assertNotNull(display);
                assertTrue(Math.abs(zero.getX() - eight.getX()) < activity.getResources()
                        .getDisplayMetrics().density * 6f);
                dialog.dismiss();
            });
        }
    }

    @Test
    public void nativeRoomControlsUseCompactButtonsAndCustomTextPad() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, NativeWorkspaceActivity.class);
        intent.putExtra(
                NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);

        try (ActivityScenario<NativeWorkspaceActivity> scenario =
                     ActivityScenario.launch(intent)) {
            AtomicReference<NativeWorkspaceActivity> ref = new AtomicReference<>();
            scenario.onActivity(ref::set);
            NativeWorkspaceActivity activity = ref.get();
            assertNotNull(activity);

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                View root = activity.getWindow().getDecorView();
                View room = root.findViewWithTag("native_room_0");
                View add = root.findViewWithTag("native_room_add");
                assertNotNull(room);
                assertNotNull(add);
                assertTrue(room.isLongClickable());
                assertTrue(room.getLayoutParams().width <= activity.getResources()
                        .getDisplayMetrics().density * 60f);

                NativeTextPadDialog dialog = new NativeTextPadDialog(
                        activity,
                        "Переименовать комнату",
                        "Комната 1",
                        value -> {});
                dialog.show();

                View keyA = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_text_pad_key_А");
                View space = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_text_pad_space");
                View done = dialog.findViewById(android.R.id.content)
                        .getRootView()
                        .findViewWithTag("native_text_pad_done");
                assertNotNull(keyA);
                assertNotNull(space);
                assertNotNull(done);
                dialog.dismiss();
            });
        }
    }

    @Test
    public void nativeProjectStorePersistsCurrentProject() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("native_ceiling_projects", Context.MODE_PRIVATE)
                .edit().clear().commit();

        NativeProjectStore first = new NativeProjectStore(context);
        NativeProjectStore.ProjectData project = first.newProject();
        project.name = "Тестовый объект";
        project.phone = "+7 900 000-00-00";
        project.address = "Тестовый адрес";
        NativeProjectStore.RoomData room = project.rooms.get(0);
        room.points.add(new NativeProjectStore.PointData(50f, 50f));
        room.points.add(new NativeProjectStore.PointData(250f, 50f));
        room.points.add(new NativeProjectStore.PointData(250f, 250f));
        room.points.add(new NativeProjectStore.PointData(50f, 250f));
        room.closed = true;
        NativeProjectStore.normalizeArrays(room);
        room.wallValues.set(0, 500f);
        room.wallValues.set(1, 500f);
        room.wallValues.set(2, 500f);
        room.wallValues.set(3, 500f);
        first.saveCurrent(project);

        NativeProjectStore second = new NativeProjectStore(context);
        NativeProjectStore.ProjectData restored = second.loadCurrent();
        assertEquals("Тестовый объект", restored.name);
        assertEquals("+7 900 000-00-00", restored.phone);
        assertEquals("Тестовый адрес", restored.address);
        assertEquals(1, restored.rooms.size());
        assertTrue(restored.rooms.get(0).closed);
        assertEquals(4, restored.rooms.get(0).points.size());
        assertEquals(4, restored.rooms.get(0).wallValues.size());
        assertEquals(500f, restored.rooms.get(0).wallValues.get(0), 0.01f);
    }

    private static boolean containsWebView(View view) {
        if ("android.webkit.WebView".equals(view.getClass().getName())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsWebView(group.getChildAt(i))) return true;
            }
        }
        return false;
    }
}
