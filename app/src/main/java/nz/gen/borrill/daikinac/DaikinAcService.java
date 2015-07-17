package nz.gen.borrill.daikinac;

import java.util.Map;

import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.QueryMap;

/**
 * Created by Chris on 16/07/2015.
 */
public interface DaikinAcService {

    String ACCESS_TOKEN_KEY = "access_token";
    String PARAMS_KEY = "params";

    @GET("/temperature")
    void roomTemperature(@QueryMap Map<String, String> options, Callback<TemperatureResponse> resp);

    //void start(Callback<DaikinAcResponse> resp);

    @FormUrlEncoded
    @POST("/daikin")
    void control(@Field(ACCESS_TOKEN_KEY) String accessToken, @Field(PARAMS_KEY) String params, Callback<DaikinAcResponse> resp);
}
