package ru.xalidmuslim.ceilingmeasure;

/** A predictable one-row fallback policy, shared by both pricing estimates. */
final class NativeEstimateDensePolicy {
    private NativeEstimateDensePolicy(){}

    static boolean oneRow(int screenWidthDp,float systemFont,float applicationFont,
            String material,String unit,String price,String total,String quantity){
        if(screenWidthDp<390||systemFont>1.06f||applicationFont>1.06f)return false;
        if(material==null||material.length()>12
                ||unit==null||unit.length()>3)return false;
        return price!=null&&price.length()<=7
                &&total!=null&&total.length()<=10
                &&quantity!=null&&quantity.length()<=6;
    }
}
