package io.github.ultimateboomer.resolutioncontrol.util;

import java.text.NumberFormat;

public final class MetricFormatter {
    private static final String[] UNITS = "KMGTPE".split("");
    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance();

    public static String format(long n) {
        if (n < 1000) {
            return n + " ";
        }

        int log10 = (int) Math.log10(n);
        int decimalPlace = (int) Math.pow(10, 2 - log10 % 3);
        int displayDigits = (int) (n / Math.pow(10, log10 - 2));

        float result = (float) displayDigits / decimalPlace;
        return String.format("%s %s", FORMAT.format(result), UNITS[log10 / 3 - 1]);
    }
}
