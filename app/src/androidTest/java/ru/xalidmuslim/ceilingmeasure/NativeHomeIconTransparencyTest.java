package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Medicine-style preloaded artwork, with the embedded white square removed. */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeIconTransparencyTest {
    @Test
    public void removeOnlyWhiteBackgroundConnectedToEdgesWithoutErasingIcon() {
        Bitmap original = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        original.eraseColor(Color.rgb(252, 253, 254));
        for (int y=5; y<27; y++) {
            for (int x=5; x<27; x++) {
                original.setPixel(x,y,Color.rgb(24, 106, 181));
            }
        }
        original.setPixel(16,16,Color.WHITE); // An enclosed white detail of the logo.
        Bitmap cleaned = NativeHomePreloader.removeWhiteBackdrop(original);
        assertNotSame("Must not destroy the packaged launcher icon", original, cleaned);
        assertEquals("White outside must become transparent",
                0, Color.alpha(cleaned.getPixel(0,0)));
        assertEquals(0, Color.alpha(cleaned.getPixel(31,31)));
        assertEquals("Blue artwork must remain unchanged",
                original.getPixel(6,6), cleaned.getPixel(6,6));
        assertEquals("White highlights enclosed by artwork must be preserved",
                Color.WHITE, cleaned.getPixel(16,16));
        original.recycle();
        cleaned.recycle();
    }

    @Test
    public void actualHeaderArtworkHasNoOffWhiteMatteOrGraySquareHalo() {
        // Pixel coordinates are from the exact packaged 256px WebP artwork;
        // these are background shadow/matte pixels, not ceiling/calculator.
        Bitmap original = BitmapFactory.decodeResource(
                InstrumentationRegistry.getInstrumentation().getTargetContext()
                        .getResources(), R.drawable.app_icon_master);
        assertNotNull(original);
        Bitmap cleaned = NativeHomePreloader.removeWhiteBackdrop(original);

        int[][] matte = {{170,21},{20,104},{230,137},{218,228}};
        for (int[] point : matte) {
            int x = point[0] * (cleaned.getWidth()-1) / 255;
            int y = point[1] * (cleaned.getHeight()-1) / 255;
            assertEquals("Remaining matte at (" + point[0] + "," + point[1] + ")",
                    0, Color.alpha(cleaned.getPixel(x,y)));
        }

        // Important: the white INSIDE the perimeter is the ceiling itself,
        // not backdrop. Preserve it and blue measurement/calculator details.
        int ceilingX = 140 * (cleaned.getWidth()-1) / 255;
        int ceilingY = 90 * (cleaned.getHeight()-1) / 255;
        int blueX = 210 * (cleaned.getWidth()-1) / 255;
        int blueY = 180 * (cleaned.getHeight()-1) / 255;
        assertTrue("Ceiling surface must remain", Color.alpha(
                cleaned.getPixel(ceilingX,ceilingY)) >= 220);
        assertTrue("Calculator and blue measurement details must remain",
                Color.alpha(cleaned.getPixel(blueX,blueY)) >= 220);

        if (cleaned != original) cleaned.recycle();
        original.recycle();
    }

    @Test
    public void actualPackagedIconLosesWhiteSquareButRetainsArtwork() {
        Bitmap original = BitmapFactory.decodeResource(
                InstrumentationRegistry.getInstrumentation().getTargetContext()
                        .getResources(), R.drawable.app_icon_master);
        assertNotNull(original);
        Bitmap result = NativeHomePreloader.removeWhiteBackdrop(original);
        assertEquals(0, Color.alpha(result.getPixel(0,0)));
        assertEquals(0, Color.alpha(result.getPixel(result.getWidth()-1,0)));
        assertEquals(0, Color.alpha(result.getPixel(0,result.getHeight()-1)));

        int remaining = 0;
        for(int y=0; y<result.getHeight(); y+=Math.max(1,result.getHeight()/48)) {
            for(int x=0; x<result.getWidth(); x+=Math.max(1,result.getWidth()/48)) {
                if(Color.alpha(result.getPixel(x,y))>200) remaining++;
            }
        }
        assertTrue("Logo artwork was accidentally erased", remaining > 35);
        if(result!=original) result.recycle();
        original.recycle();
    }

    @Test
    public void homeLogoHasNoViewBackgroundOrForcedGraphicsLayer() {
        try (androidx.test.core.app.ActivityScenario<NativeHomeActivity> scene =
                     androidx.test.core.app.ActivityScenario.launch(NativeHomeActivity.class)) {
            scene.onActivity(activity -> {
                View logo = activity.getWindow().getDecorView()
                        .findViewWithTag("native_home_app_icon");
                assertNotNull(logo);
                assertNull("White view background must not be drawn behind logo",
                        logo.getBackground());
                assertEquals(View.LAYER_TYPE_NONE, logo.getLayerType());
            });
        }
    }
}
