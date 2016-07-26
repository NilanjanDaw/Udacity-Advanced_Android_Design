package com.example.android.sunshine.app;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;


public class WatchTemperatureService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = "WatchTemperatureService";
    private GoogleApiClient mGoogleApiClient;
    private static final String PATH  = "/weather";
    private static final String WEATHER_ID = "WEATHER_ID";
    private static final String MAX_TEMP = "MAX_TEMP";
    private static final String MIN_TEMP = "MIN_TEMP";

    public WatchTemperatureService() {
        super("WatchTemperatureService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent: Intent Received");
        if (intent != null && intent.getStringExtra("action").equalsIgnoreCase("UPDATE_WATCH_FACE")) {
            mGoogleApiClient = new GoogleApiClient.Builder(WatchTemperatureService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        String locationQuery = Utility.getPreferredLocation(this);

        Uri weatherUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

        Cursor cursor = getContentResolver().query(
                weatherUri,
                new String[]{WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
                },
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            int weatherID = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            String maxTemp = Utility.formatTemperature(this,
                    cursor.getFloat(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)));
            maxTemp = (Utility.isMetric(this))? maxTemp + "C": maxTemp + "F";
            String minTemp = Utility.formatTemperature(this,
                    cursor.getFloat(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)));
            minTemp = (Utility.isMetric(this))? minTemp + "C": minTemp + "F";
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);
            putDataMapRequest.getDataMap().putInt(WEATHER_ID, weatherID);
            putDataMapRequest.getDataMap().putString(MAX_TEMP, maxTemp);
            putDataMapRequest.getDataMap().putString(MIN_TEMP, minTemp);

            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest());

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}
