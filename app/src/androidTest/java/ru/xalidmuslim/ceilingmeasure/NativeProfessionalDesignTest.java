package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NativeProfessionalDesignTest {
    @Test public void unifiedMatteDesignUsesProfessionalContrastAndFlatPrimary() {
        assertEquals(0xFFF6F8FA,NativeVisualTokens.APP_BG);
        assertEquals(0xFF203445,NativeVisualTokens.TEXT);
        assertEquals(0xFF586C7D,NativeVisualTokens.MUTED);
        assertEquals(0xFF216E93,NativeVisualTokens.ACCENT);
        assertEquals(0xFFDFE6EC,NativeVisualTokens.LINE);
        assertTrue("secondary text contrast on white",
                contrast(NativeVisualTokens.MUTED,0xFFFFFFFF)>=4.5);
        assertTrue("primary button white text contrast",
                contrast(NativeVisualTokens.ACCENT,0xFFFFFFFF)>=4.5);
    }

    @Test public void bottomNavigationHasUnifiedVectorIconsAndClearHistoryLabels() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,NativeWorkspaceActivity.class);
        intent.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scene=ActivityScenario.launch(intent)){
            scene.onActivity(activity->{
                ViewGroup nav=(ViewGroup)activity.getWindow().getDecorView()
                        .findViewWithTag("native_history_nav");
                assertNotNull(nav);
                assertEquals(4,nav.getChildCount());
                for(int i=0;i<4;i++){
                    ViewGroup button=(ViewGroup)nav.getChildAt(i);
                    assertTrue("vector nav icon, not font glyph",button.getChildAt(0)
                            instanceof ImageView);
                    assertTrue("Android 48dp target",button.getLayoutParams().height>=dp(activity,48));
                }
                assertEquals("Отменить",
                        ((android.widget.TextView)((ViewGroup)nav.getChildAt(1)).getChildAt(1))
                                .getText().toString());
                assertEquals("Повторить",
                        ((android.widget.TextView)((ViewGroup)nav.getChildAt(2)).getChildAt(1))
                                .getText().toString());
            });
        }
    }

    private static int dp(Context context,int value){
        return Math.round(value*context.getResources().getDisplayMetrics().density);
    }
    private static double linear(int channel){
        double srgb=channel/255d;
        return srgb<=.04045?srgb/12.92:Math.pow((srgb+.055)/1.055,2.4);
    }
    private static double luminosity(int color){
        return .2126*linear((color>>16)&255)
                +.7152*linear((color>>8)&255)+.0722*linear(color&255);
    }
    private static double contrast(int a,int b){
        double la=luminosity(a),lb=luminosity(b);
        return (Math.max(la,lb)+.05)/(Math.min(la,lb)+.05);
    }
}
