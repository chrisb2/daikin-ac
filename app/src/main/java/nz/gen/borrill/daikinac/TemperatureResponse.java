package nz.gen.borrill.daikinac;

import java.text.DecimalFormat;

/**
 * Created by Chris on 16/07/2015.
 */
public class TemperatureResponse {

    public static final String DEGREES_C = "\u2103";

    private String result;

    public String getResult() {
        return result;
    }

    public String getFormattedTemperature() {
        return new DecimalFormat("##").format(Double.valueOf(result).doubleValue()) + DEGREES_C;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
