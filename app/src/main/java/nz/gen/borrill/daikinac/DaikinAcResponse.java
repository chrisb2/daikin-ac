package nz.gen.borrill.daikinac;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Chris on 16/07/2015.
 */
public class DaikinAcResponse {

    @SerializedName("return_value")
    private String returnValue;

    public String getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(String returnValue) {
        this.returnValue = returnValue;
    }
}
