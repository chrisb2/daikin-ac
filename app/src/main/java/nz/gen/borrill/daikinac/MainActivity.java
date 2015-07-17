package nz.gen.borrill.daikinac;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends ActionBarActivity {

    private static final String BASE_URL = "https://api.particle.io/v1/devices/";
    private static final String DEVICE = "27002c001147343339383037";
    private static final String DEVICE_URL = BASE_URL + DEVICE;

    public static final String ACCESS_TOKEN = "c06b11a1dad6ba977fca8c75213cfac9eb8976b3";

    public static final String OFF_PARAM = "off";
    public static final String TEMP_18_PARAM = "18-2-4";
    public static final String TEMP_20_PARAM = "20-2-4";
    public static final String TEMP_22_PARAM = "22-2-4";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getRoomTemperature();

        addListenerOnButton(R.id.buttonOff, OFF_PARAM);
        addListenerOnButton(R.id.button18, TEMP_18_PARAM);
        addListenerOnButton(R.id.button20, TEMP_20_PARAM);
        addListenerOnButton(R.id.button22, TEMP_22_PARAM);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        Intent prefsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        MenuItem preferences = menu.findItem(R.id.action_settings);
        preferences.setIntent(prefsIntent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up offButton, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(item.getIntent());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addListenerOnButton (final int buttonId, final String params) {
        Button button = (Button) findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                control(params);
            }
        });
    }

    private void getRoomTemperature() {
        DaikinAcService service = getDaikinAcService();
        HashMap<String, String> queryMap = new HashMap<>();
        queryMap.put(DaikinAcService.ACCESS_TOKEN_KEY, getAccessToken());
        service.roomTemperature(queryMap, new Callback<TemperatureResponse>() {
            @Override
            public void success(TemperatureResponse temperatureResponse, Response response) {
                Log.i("temperature", temperatureResponse.getResult());
                TextView temperatureView = (TextView) findViewById(R.id.temperature);
                temperatureView.setText(temperatureResponse.getFormattedTemperature());
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("temperature", error.toString());
            }
        });
    }

    private void control(final String controlParams) {
        DaikinAcService service = getDaikinAcService();
        service.control(ACCESS_TOKEN, controlParams, new Callback<DaikinAcResponse>() {
            @Override
            public void success(DaikinAcResponse daikinAcResponse, Response response) {
                Log.i("control", daikinAcResponse.getReturnValue());
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("control", error.toString());
            }
        });
    }

    private DaikinAcService getDaikinAcService() {
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(DEVICE_URL).build();
        return restAdapter.create(DaikinAcService.class);
    }

    private String getAccessToken() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getString(DaikinAcService.ACCESS_TOKEN_KEY, ACCESS_TOKEN);
    }
}
