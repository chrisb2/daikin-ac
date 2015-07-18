package nz.gen.borrill.daikinac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    public static final String DEVICE_ID_KEY = "device_id";

    public static final String OFF_PARAM = "off";
    public static final String TEMP_18_PARAM = "18-2-4";
    public static final String TEMP_20_PARAM = "20-2-4";
    public static final String TEMP_22_PARAM = "22-2-4";

    private SharedPreferences prefs;

    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver receiver = new NetworkReceiver();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        getRoomTemperature();

        addListenerOnButton(R.id.buttonOff, OFF_PARAM);
        addListenerOnButton(R.id.button18, TEMP_18_PARAM);
        addListenerOnButton(R.id.button20, TEMP_20_PARAM);
        addListenerOnButton(R.id.button22, TEMP_22_PARAM);

        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
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

    private void addListenerOnButton(final int buttonId, final String params) {
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
        Log.i("temperature", DaikinAcService.ACCESS_TOKEN_KEY + "=" + getAccessToken());
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
        service.control(getAccessToken(), controlParams, new Callback<DaikinAcResponse>() {
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
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(getUrl()).build();
        return restAdapter.create(DaikinAcService.class);
    }

    private String getAccessToken() {
        return prefs.getString(DaikinAcService.ACCESS_TOKEN_KEY, getResources().getString(R.string.default_access_token));
    }

    private String getUrl() {
        return BASE_URL + prefs.getString(DEVICE_ID_KEY, getResources().getString(R.string.default_device));
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conn = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conn.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                Log.i("network", networkInfo.toString());
                getRoomTemperature();
            }
        }
    }
}