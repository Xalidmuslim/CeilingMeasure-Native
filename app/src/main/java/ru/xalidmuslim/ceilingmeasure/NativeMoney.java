package ru.xalidmuslim.ceilingmeasure;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Legible Russian grouping for financial values, without changing calculation precision. */
final class NativeMoney {
    private NativeMoney() {}
    static String format(double amount) {
        DecimalFormat df = new DecimalFormat("#,##0.##",
                DecimalFormatSymbols.getInstance(new Locale("ru","RU")));
        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(2);
        return df.format(amount)
                .replace('\u00a0',' ')
                .replace('\u202f',' ');
    }
}
