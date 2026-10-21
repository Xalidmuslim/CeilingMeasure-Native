package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public final class NativeProfessionalEstimatePdfPersistenceTest {
    @Test public void restoredMaterialValuesProduceValidClientPdfAndConsistentTotal()
            throws Exception {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=ctx.getSharedPreferences(
                "v238_pdf_contract_"+System.nanoTime(),Context.MODE_PRIVATE);
        NativeProjectStore store=new NativeProjectStore(prefs);
        NativeProjectStore.ProjectData project=new NativeProjectStore.ProjectData();
        NativeProjectStore.RoomData room=store.createRoom("Холл");
        room.extraQty.put("light",2f);
        room.clientPricing.put("light",500f);

        NativeProjectStore.MaterialData custom=new NativeProjectStore.MaterialData();
        custom.name="Дополнительный потолочный материал";
        custom.unit="шт";
        custom.qty=3f;
        custom.price=1234.56f;
        room.clientCustomItems.add(custom);
        project.rooms.add(room);
        double expected=NativeEstimateEngine.projectCost(project,true).total;

        store.saveCurrent(project);
        NativeProjectStore.ProjectData restored=store.loadCurrent();
        assertEquals(1,restored.rooms.size());
        assertEquals(project.rooms.get(0).clientCustomItems.size(),
                restored.rooms.get(0).clientCustomItems.size());
        assertEquals("prices and line totals must not be affected by the UI",
                expected,NativeEstimateEngine.projectCost(restored,true).total,0.01);

        NativeClientEstimatePdf.Result pdf=NativeClientEstimatePdf.build(
                Collections.singletonList(restored));
        assertTrue("priced materials must generate client PDF",pdf.pricedLines>0);
        assertTrue("PDF output cannot be empty",pdf.bytes.length>400);
        assertEquals("%PDF",new String(pdf.bytes,0,4,
                StandardCharsets.US_ASCII));

        // Verify the actual PDF renders, not just that it has a PDF signature.
        File file=File.createTempFile("v238_estimate_", ".pdf",ctx.getCacheDir());
        try{
            try(FileOutputStream output=new FileOutputStream(file)){
                output.write(pdf.bytes);
            }
            try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(file,
                        ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer renderer=new PdfRenderer(descriptor)){
                assertTrue("PDF must have at least one printable page",
                        renderer.getPageCount()>=1);
            }
        }finally{
            file.delete();
            prefs.edit().clear().commit();
        }
    }
}
