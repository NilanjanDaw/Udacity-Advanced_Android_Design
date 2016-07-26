/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.sunshineanalogwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchAnalog extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private Paint paintLarge;
    private Paint paintSmall;
    private Paint paintMedium;
    private Utility utility;
    private String maxTemp = "00\u00b0";
    private String minTemp = "00\u00b0";
    private int weatherID = 0;
    private static final String PATH  = "/weather";
    private static final String WEATHER_ID = "WEATHER_ID";
    private static final String MAX_TEMP = "MAX_TEMP";
    private static final String MIN_TEMP = "MIN_TEMP";
    public static final String TAG = "SunshineWatchAnalog";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchAnalog.Engine> mWeakReference;

        public EngineHandler(SunshineWatchAnalog.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchAnalog.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine
            implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mHandPaint;
        boolean mAmbient;
        Time mTime;
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(SunshineWatchAnalog.this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();


            }
        };
        int mTapCount;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchAnalog.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = SunshineWatchAnalog.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.primary));

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);
            utility = new Utility();
            mTime = new Time();
            paintLarge = utility.setUpPaintLarge(getBaseContext());
            paintMedium = utility.setUpPaintMedium(getBaseContext());
            paintSmall = utility.setUpPaintSmall(getBaseContext());
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = SunshineWatchAnalog.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.primary : R.color.primary_dark));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            }

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;

            String hour = Integer.toString(mTime.hour);
            hour = (mTime.hour < 10) ? "0" + hour : hour;
            String min = Integer.toString(mTime.minute);
            min = (mTime.minute < 10) ? "0" + min : min;
            String sec = Integer.toString(mTime.second);
            sec = (mTime.second < 10) ? "0" + sec : sec;
            String colon = ":";
            if (isInAmbientMode()) {
                paintLarge.setAntiAlias(false);
                String drawTimePrimary = hour + colon + min;
                canvas.drawText(drawTimePrimary,
                        centerX - (paintLarge.measureText(drawTimePrimary) / 2f),
                        centerY, paintLarge);
            } else {
                paintLarge.setAntiAlias(true);
                /**
                 * Draw Time
                 */
                String drawTimePrimary = hour + colon + min;
                String drawTimeSecondary = colon + sec;
                float offsetX = (paintLarge.measureText(drawTimePrimary) + paintSmall.measureText(drawTimeSecondary)) / 2f;
                float offsetY = getResources().getDimension(R.dimen.Y_offset);
                float lineSizeMedium = getResources().getDimension(R.dimen.primary_text_medium);
                canvas.drawText(drawTimePrimary,
                        centerX - offsetX,
                        centerY - offsetY, paintLarge);

                canvas.drawText(drawTimeSecondary,
                        centerX + (paintLarge.measureText(drawTimePrimary) - offsetX),
                        centerY - offsetY, paintSmall
                );
                /**
                 * Draw Date
                 */
                paintMedium.setColor(getResources().getColor(R.color.activated));
                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("E, MMM dd yyyy");
                String dateString = dateFormat.format(date);
                canvas.drawText(dateString,
                        centerX - (paintMedium.measureText(dateString) / 2f),
                        centerY - offsetY + lineSizeMedium,
                        paintMedium
                );
                /**
                 * Draw Divider
                 */
                float lineSize = getResources().getDimension(R.dimen.divider);
                paintMedium.setColor(getResources().getColor(R.color.white));
                canvas.drawLine(centerX - lineSize / 2f,
                        centerY - offsetY + lineSizeMedium * 2,
                        centerX + lineSize / 2f,
                        centerY - offsetY + lineSizeMedium * 2,
                        paintMedium
                );

                /**
                 * Draw Temperature
                 */
                if (getPeekCardPosition().isEmpty()) {

                    paintMedium.setColor(getResources().getColor(R.color.white));
                    canvas.drawText(
                            maxTemp,
                            centerX - paintMedium.measureText(maxTemp + " ") + 15,
                            centerY - offsetY + lineSizeMedium * 3,
                            paintMedium
                    );
                    paintMedium.setColor(getResources().getColor(R.color.activated));
                    canvas.drawText(
                            minTemp,
                            centerX + paintMedium.measureText(" ") + 15,
                            centerY - offsetY + lineSizeMedium * 3,
                            paintMedium
                    );

                    int drawable = Utility.setUpDrawable(weatherID);
                    Bitmap bitmap = BitmapFactory.decodeResource(getBaseContext().getResources(),drawable);
                    bitmap = Bitmap.createScaledBitmap(bitmap, 60, 60, true);

                    canvas.drawBitmap(bitmap,
                            centerX - paintMedium.measureText(maxTemp + " ") - 45,
                            centerY - offsetY + lineSizeMedium + 20,
                            paintMedium
                    );
                }

            }

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
                googleApiClient.connect();
            } else {
                unregisterReceiver();
                googleApiClient.disconnect();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchAnalog.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchAnalog.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }


        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(googleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent dataEvent: dataEventBuffer) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String uri = dataEvent.getDataItem(). getUri().toString();
                if (uri.equalsIgnoreCase(PATH)) {
                    Log.d(TAG, "onDataChanged: " + dataMap.getString(MAX_TEMP));
                    maxTemp = dataMap.getString(MAX_TEMP);
                    minTemp = dataMap.getString(MIN_TEMP);
                    weatherID = dataMap.getInt(WEATHER_ID);
                }
            }
            invalidate();
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

    }
}
