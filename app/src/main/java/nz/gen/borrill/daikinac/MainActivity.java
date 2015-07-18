package nz.gen.borrill.daikinac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://api.particle.io/v1/devices/";
    public static final String DEVICE_ID_KEY = "device_id";

    public static final String OFF_PARAM = "off";
    public static final String TEMP_18_PARAM = "18-2-4";
    public static final String TEMP_20_PARAM = "20-2-4";
    public static final String TEMP_22_PARAM = "22-2-4";

    public static final int TWO_SECONDS = 2000;

    private SharedPreferences prefs;

    private NetworkReceiver receiver = new NetworkReceiver();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture future;

    private DaikinAcService service;

    private HashMap<String, String > queryMap = new HashMap<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.service = getDaikinAcService(getUrl());
        this.queryMap.put(DaikinAcService.ACCESS_TOKEN_KEY, getAccessToken());

        getRoomTemperature();

        addListenerOnButton(R.id.buttonOff, OFF_PARAM);
        addListenerOnButton(R.id.button18, TEMP_18_PARAM);
        addListenerOnButton(R.id.button20, TEMP_20_PARAM);
        addListenerOnButton(R.id.button22, TEMP_22_PARAM);

        configureNetworkConnectTemperature();
        configureScheduledTemperature();
    }

    private void configureNetworkConnectTemperature() {
        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);
    }

    private void configureScheduledTemperature() {
        this.future = scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        getRoomTemperature();
                    }
                }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
        if (this.future != null) {
            this.future.cancel(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getRoomTemperature();
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
        final Button button = (Button) findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                control(button, params);
            }
        });
    }

    private void getRoomTemperature() {
        final TextView temperatureView = (TextView) findViewById(R.id.temperature);

        this.service.roomTemperature(this.queryMap, new Callback<TemperatureResponse>() {
            @Override
            public void success(TemperatureResponse temperatureResponse, Response response) {
                Log.i("temperature", temperatureResponse.getResult());
                setTemperatureTextColour(temperatureView, R.color.text_colour_default);
                temperatureView.setText(temperatureResponse.getFormattedTemperature());
            }

            @Override
            public void failure(RetrofitError error) {
                setTemperatureTextColour(temperatureView, R.color.text_colour_error);
                Log.e("temperature", error.toString());
            }
        });
    }

    private void setTemperatureTextColour(TextView temperatureView, int color) {
        temperatureView.setTextColor(getResources().getColor(color));
    }

    private void control(final Button button, final String controlParams) {
        this.service.control(getAccessToken(), controlParams, new Callback<DaikinAcResponse>() {
            @Override
            public void success(DaikinAcResponse daikinAcResponse, Response response) {
                flashButtonText(button, daikinAcResponse.isSuccess());
                Log.i("control", daikinAcResponse.getReturnValue());
            }

            @Override
            public void failure(RetrofitError error) {
                flashButtonText(button, false);
                Log.e("control", error.toString());
            }
        });
    }

    private void flashButtonText(final Button button, final boolean success) {
        final int currentColour = button.getCurrentTextColor();
        int flashColour;
        if (success) {
            flashColour = getResources().getColor(R.color.text_colour_success);
        } else {
            flashColour = getResources().getColor(R.color.text_colour_error);
        }
        button.setTextColor(flashColour);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                button.setTextColor(currentColour);
            }
        }, TWO_SECONDS);
    }

    private DaikinAcService getDaikinAcService(final String url) {
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(url).build();
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