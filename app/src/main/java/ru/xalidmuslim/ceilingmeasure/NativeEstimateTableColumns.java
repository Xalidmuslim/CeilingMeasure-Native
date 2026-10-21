package ru.xalidmuslim.ceilingmeasure;

/**
 * Stable column widths for the two-level estimate table. Fixed widths make
 * prices and quantities align between materials while remaining phone-safe.
 */
final class NativeEstimateTableColumns {
    final int quantityDp;
    final int unitDp;
    final int multiplyDp;
    final int priceDp;
    final int actionsDp;
    final int operandGapDp;

    private NativeEstimateTableColumns(
            int qty,int unit,int multiply,int price,int actions,int gap) {
        quantityDp=qty;
        unitDp=unit;
        multiplyDp=multiply;
        priceDp=price;
        actionsDp=actions;
        operandGapDp=gap;
    }

    static NativeEstimateTableColumns forScreenWidthDp(int widthDp) {
        if (widthDp < 310) {
            return new NativeEstimateTableColumns(54,27,14,82,48,3);
        }
        if (widthDp < 370) {
            return new NativeEstimateTableColumns(60,30,16,90,48,4);
        }
        return new NativeEstimateTableColumns(68,34,16,104,48,5);
    }

    int totalMinimumDp() {
        return quantityDp+unitDp+multiplyDp+priceDp+actionsDp
                +operandGapDp*3;
    }
}
