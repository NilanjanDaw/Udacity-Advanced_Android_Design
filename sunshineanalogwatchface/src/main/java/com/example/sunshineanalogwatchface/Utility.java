package com.example.sunshineanalogwatchface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;

/**
 * Created by nilan on 26-Jul-16.
 */
public class Utility {

    public Paint setUpPaintLarge(Context context) {
        Paint paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.white));
        //paint.setTypeface(Typeface.SERIF);
        paint.setTextSize(context.getResources().getDimension(R.dimen.primary_text_large));
        paint.setAlpha(255);
        paint.setAntiAlias(true);
        return paint;
    }

    public Paint setUpPaintSmall(Context context) {
        Paint paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.white));
        //paint.setTypeface(Typeface.SERIF);
        paint.setTextSize(context.getResources().getDimension(R.dimen.primary_text_small));
        paint.setAlpha(255);
        paint.setAntiAlias(true);
        return paint;
    }

    public Paint setUpPaintMedium(Context context) {
        Paint paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.white));
        //paint.setTypeface(Typeface.SERIF);
        paint.setTextSize(context.getResources().getDimension(R.dimen.primary_text_medium));
        paint.setAlpha(255);
        paint.setAntiAlias(true);
        return paint;
    }

    public static int setUpDrawable(int weatherId) {
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return R.drawable.ic_clear;
    }
}
