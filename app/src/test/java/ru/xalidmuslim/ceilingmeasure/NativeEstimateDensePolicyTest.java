package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;
import org.junit.Test;

public final class NativeEstimateDensePolicyTest {
    @Test public void standardMaterialsFitOnOneLineOnModernPhones(){
        for(String material:new String[]{"Квадратура","Вставка","Светильники",
                "Карниз","Углы","ТРЕК","БЛОК","НИША"}){
            assertTrue(material,NativeEstimateDensePolicy.oneRow(
                    393,1f,1f,material,"шт","4 000","15 062,62","31,38"));
        }
    }

    @Test public void narrowScreensAndAccessibilityUseReadableTwoRows(){
        assertFalse(NativeEstimateDensePolicy.oneRow(
                360,1f,1f,"Вставка","м","150","1 500","10"));
        assertFalse(NativeEstimateDensePolicy.oneRow(
                393,1.2f,1f,"Вставка","м","150","1 500","10"));
        assertFalse(NativeEstimateDensePolicy.oneRow(
                393,1f,1.2f,"Вставка","м","150","1 500","10"));
    }

    @Test public void highPricesAndLongMaterialsNeverClipSingleRow(){
        assertFalse(NativeEstimateDensePolicy.oneRow(
                393,1f,1f,"Декоративная световая линия","м",
                "1 000","12 000","12"));
        assertFalse(NativeEstimateDensePolicy.oneRow(
                393,1f,1f,"Карниз","м","1 250 000","3 750 000","3"));
        assertFalse(NativeEstimateDensePolicy.oneRow(
                393,1f,1f,"Углы","шт","300","15 000 062,62","500"));
    }
}
