package nz.gen.borrill.daikinac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.SystemClock;
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
    private static final String DEVICE_ID_KEY = "device_id";

    private static final String OFF_PARAM = "off";
    private static final String FAN_PARAM = "2";
    private static final String MODE_HEAT_PARAM = "4";
    private static final String MODE_COOL_PARAM = "3";
    private static final String TEMP_18_PARAM = "18";
    private static final String TEMP_20_PARAM = "20";
    private static final String TEMP_22_PARAM = "22";

    private static final int SCHEDULE_INTERVAL_SECS = 30;
    private static final int SCHEDULE_DELAY_SECS = 30;
    private static final int TWO_SECOND_DELAY = 2000;
    private static final long MIN_CLICK_INTERVAL = 1000;

    private SharedPreferences prefs;

    private StatusUpdateReceiver receiver = new StatusUpdateReceiver();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture future;

    private DaikinAcService service;

    private HashMap<String, String > queryMap = new HashMap<>();

    private String currentTemperatureParam = TEMP_18_PARAM;
    private String currentModeParam = MODE_HEAT_PARAM;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.service = getDaikinAcService(getParticleIoUrl());
        this.queryMap.put(DaikinAcService.ACCESS_TOKEN_KEY, getAccessToken());

        addListenerOnButton(R.id.buttonOff, OFF_PARAM);
        addListenerOnButton(R.id.button18, TEMP_18_PARAM);
        addListenerOnButton(R.id.button20, TEMP_20_PARAM);
        addListenerOnButton(R.id.button22, TEMP_22_PARAM);
        addListenerOnButton(R.id.buttonMode, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        unConfigureScheduledTemperature();
        unConfigureUpdateTemperature();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set temperature text to red, so its clear to user that it may be out of date, if updates fail.
        setTemperatureTextColour(null, R.color.text_colour_error);
        getRoomTemperature();
        configureScheduledTemperature();
        configureUpdateTemperature();
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
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(item.getIntent());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void configureUpdateTemperature() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new StatusUpdateReceiver();
        this.registerReceiver(receiver, filter);
    }

    private void unConfigureUpdateTemperature() {
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    private void configureScheduledTemperature() {
        this.future = scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        getRoomTemperature();
                    }
                }, SCHEDULE_DELAY_SECS, SCHEDULE_INTERVAL_SECS, TimeUnit.SECONDS);
    }

    private void unConfigureScheduledTemperature() {
        if (this.future != null) {
            this.future.cancel(true);
        }
    }

    private void addListenerOnButton(final int buttonId, final String param) {
        final Button button = (Button) findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {

            private long lastClickTime = 0;

            @Override
            public void onClick(View view) {
                long currentTime = SystemClock.elapsedRealtime();
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime;
                    String params = "";
                    switch ((String) button.getTag()) {
                        case "temperature":
                            currentTemperatureParam = param;
                            params = getCommandParams();
                            break;
                        case "mode":
                            if (MODE_HEAT_PARAM.equals(currentModeParam)) {
                                currentModeParam = MODE_COOL_PARAM;
                                button.setText(R.string.mode_cool);
                            } else {
                                currentModeParam = MODE_HEAT_PARAM;
                                button.setText(R.string.mode_heat);
                            }
                            params = getCommandParams();
                            break;
                        case "power":
                            params = param;
                            break;
                    }
                    control(button, params);
                }
            }
        });
    }

    private String getCommandParams() {
        return String.format("%s-%s-%s", currentTemperatureParam, FAN_PARAM, currentModeParam);
    }

    private void getRoomTemperature() {
        this.service.roomTemperature(this.queryMap, new Callback<TemperatureResponse>() {
            @Override
            public void success(final TemperatureResponse temperatureResponse, final Response response) {
                Log.i("temperature", temperatureResponse.getFormattedValue());
                setTemperatureTextColour(temperatureResponse.getFormattedCentigrade(), R.color.text_colour_default);
            }

            @Override
            public void failure(final RetrofitError error) {
                Log.e("temperature", error.toString());
                setTemperatureTextColour(null, R.color.text_colour_error);
            }
        });
    }

    private void setTemperatureTextColour(final String text, final int colour) {
        final TextView temperatureView = (TextView) findViewById(R.id.temperature);
        if (text != null) {
            temperatureView.setText(text);
        }
        temperatureView.setTextColor(getResources().getColor(colour));
    }

    private void control(final Button button, final String controlParams) {
        Log.i("control", controlParams);
        this.service.control(getAccessToken(), controlParams, new Callback<DaikinAcResponse>() {
            @Override
            public void success(final DaikinAcResponse daikinAcResponse, final Response response) {
                Log.i("control", daikinAcResponse.getReturnValue());
                flashButtonText(button, daikinAcResponse.isSuccess());
            }

            @Override
            public void failure(final RetrofitError error) {
                Log.e("control", error.toString());
                flashButtonText(button, false);
            }
        });
    }

    private void flashButtonText(final Button button, final boolean success) {
        final int currentColour = button.getCurrentTextColor();
        final int successColour = getResources().getColor(R.color.text_colour_success);
        final int errorColour = getResources().getColor(R.color.text_colour_success);
        if (currentColour != successColour && currentColour != errorColour) {
            button.setClickable(false);
            if (success) {
                button.setTextColor(successColour);
            } else {
                button.setTextColor(errorColour);
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    button.setTextColor(currentColour);
                    button.setClickable(true);
                }
            }, TWO_SECOND_DELAY);
        }
    }

    private DaikinAcService getDaikinAcService(final String url) {
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(url).build();
        return restAdapter.create(DaikinAcService.class);
    }

    private String getAccessToken() {
        return prefs.getString(DaikinAcService.ACCESS_TOKEN_KEY, getResources().getString(R.string.default_access_token));
    }

    private String getParticleIoUrl() {
        return BASE_URL + prefs.getString(DEVICE_ID_KEY, getResources().getString(R.string.default_device));
    }

    private class StatusUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            ConnectivityManager conn = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conn.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                Log.i("network", networkInfo.toString());
                getRoomTemperature();
            } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                Log.i("user-present", intent.getAction());
                getRoomTemperature();
            }
        }
    }
}