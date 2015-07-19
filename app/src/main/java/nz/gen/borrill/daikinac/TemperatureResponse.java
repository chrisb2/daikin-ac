package nz.gen.borrill.daikinac;

import java.text.DecimalFormat;

/**
 * Created by Chris on 16/07/2015.
 */
public class TemperatureResponse {

    private static final String DEGREES_C = "\u2103";

    private double result;

    public double getResult() {
        return result;
    }

    public String getFormattedCentigrade() {
        return formatValue("##") + DEGREES_C;
    }

    public String getFormattedValue() {
        return formatValue("##.0");
    }

    private String formatValue(final String format) {
        return new DecimalFormat(format).format(result);
    }

    public void setResult(final double result) {
        this.result = result;
    }
}
