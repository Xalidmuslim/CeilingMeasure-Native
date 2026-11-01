package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.view.Window;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;

@RunWith(AndroidJUnit4.class)
public final class NativeExplicitDialogMotionTest {
    private static final String ANDROID_NS="http://schemas.android.com/apk/res/android";

    @Test public void fullscreenEstimateAndPriceEnterIsActuallyVisible() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        int enter=animationFromStyle(context,R.style.CeilingFullscreenEnterOnly,
                android.R.attr.windowEnterAnimation);
        assertVisibleFade(context,enter,true);
        assertEquals("Fullscreen dialog exit is handled once by the shared shell",
                0,animationFromStyle(context,R.style.CeilingFullscreenEnterOnly,
                        android.R.attr.windowExitAnimation));
        assertTrue("Do not dismiss the fullscreen dialog before a visible exit",
                NativeFullScreenDialogShell.EXIT_DURATION_MS>=190L);
    }

    @Test public void moreMenusHavePerceptibleOrdinaryEntryAndExit() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        int style=R.style.CeilingActionMenuFadeWindowAnimation;
        assertVisibleFade(context,animationFromStyle(context,style,
                android.R.attr.windowEnterAnimation),true);
        assertVisibleFade(context,animationFromStyle(context,style,
                android.R.attr.windowExitAnimation),false);

        try (ActivityScenario<NativeHomeActivity> scenario=
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scenario.onActivity(activity -> {
                NativeStyledAlertDialog popup=new NativeStyledAlertDialog.Builder(activity)
                        .setTitle("Ещё").setItems(new String[]{"Действие"},(d,i)->{}).show();
                try {
                    assertEquals("Estimate More must use visible action-menu fade",style,
                            popup.getWindow().getAttributes().windowAnimations);
                } finally {
                    popup.dismiss();
                }
                NativeProjectStore store=new NativeProjectStore(activity);
                NativeDrawToolsDialog tools=new NativeDrawToolsDialog(activity,
                        store.createRoom("Комната"),action->{});
                tools.show();
                try {
                    assertEquals("Drawing More must use the same motion",style,
                            tools.getWindow().getAttributes().windowAnimations);
                } finally {
                    tools.dismiss();
                }
            });
        }
    }

    private static int animationFromStyle(Context context,int style,int attribute) {
        TypedArray values=context.obtainStyledAttributes(style,new int[]{attribute});
        try {return values.getResourceId(0,0);}
        finally {values.recycle();}
    }

    private static void assertVisibleFade(Context context,int resource,
            boolean opening) throws Exception {
        assertTrue("A genuine window animation resource must exist",resource!=0);
        try(XmlResourceParser parser=context.getResources().getAnimation(resource)) {
            while(parser.getEventType()!=XmlPullParser.START_TAG
                    &&parser.getEventType()!=XmlPullParser.END_DOCUMENT) parser.next();
            assertEquals("alpha",parser.getName());
            float from=Float.parseFloat(parser.getAttributeValue(ANDROID_NS,"fromAlpha"));
            float to=Float.parseFloat(parser.getAttributeValue(ANDROID_NS,"toAlpha"));
            int duration=Integer.parseInt(parser.getAttributeValue(ANDROID_NS,"duration"));
            assertTrue("Fade must be visible, not just a 4-20% opacity tick",
                    opening ? from<=.15f&&to>=.99f : from>=.99f&&to<=.15f);
            assertTrue("The fade must last enough frames to be smooth",duration>=190);
            assertTrue("Avoid a sluggish transition",duration<=280);
        }
    }
}
