package com.example.sunshineanalogwatchface;

import android.content.Context;
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
}
