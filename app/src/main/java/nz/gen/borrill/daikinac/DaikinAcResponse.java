package nz.gen.borrill.daikinac;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Chris on 16/07/2015.
 */
public class DaikinAcResponse {

    @SerializedName("return_value")
    private int returnValue;

    public String getReturnValue() {
        return Integer.toString(returnValue);
    }

    public void setReturnValue(final int returnValue) {
        this.returnValue = returnValue;
    }

    public boolean isSuccess() {
        return (returnValue == 1);
    }
}
