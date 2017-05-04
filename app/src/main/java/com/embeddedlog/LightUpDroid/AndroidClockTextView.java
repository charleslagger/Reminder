package com.embeddedlog.LightUpDroid;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;


public class AndroidClockTextView extends TextView {

    private static final String SYSTEM = "/system/fonts/";
    private static final String SYSTEM_FONT_TIME_BACKGROUND = SYSTEM + "AndroidClock.ttf";

    private static final String ATTR_USE_CLOCK_TYPEFACE = "useClockTypeface";

    private static Typeface sClockTypeface;
    private static Typeface sStandardTypeface;

    private boolean mUseClockTypeface;

    public AndroidClockTextView(Context context) {
        super(context);
    }

    public AndroidClockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mUseClockTypeface = attrs.getAttributeBooleanValue(null, ATTR_USE_CLOCK_TYPEFACE, true)
                && !isInEditMode();

        sStandardTypeface = Typeface.DEFAULT;
        if (sClockTypeface == null && mUseClockTypeface) {
            sClockTypeface = Typeface.createFromFile(SYSTEM_FONT_TIME_BACKGROUND);
        }

        Paint paint = getPaint();
        paint.setTypeface(mUseClockTypeface ? sClockTypeface : sStandardTypeface);
    }
}
