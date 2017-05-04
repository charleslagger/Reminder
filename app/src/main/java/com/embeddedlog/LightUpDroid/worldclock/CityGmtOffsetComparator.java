package com.embeddedlog.LightUpDroid.worldclock;

import java.util.Comparator;
import java.util.TimeZone;

public class CityGmtOffsetComparator implements Comparator<CityObj> {

    private CityNameComparator mNameComparator = null;

    @Override
    public int compare(CityObj c1, CityObj c2) {
        long currentTime = System.currentTimeMillis();
        int offset = TimeZone.getTimeZone(c1.mTimeZone).getOffset(currentTime);
        int offset2 = TimeZone.getTimeZone(c2.mTimeZone).getOffset(currentTime);
        return (offset < offset2) ? -1 : (offset > offset2) ? 1 :
            getCityNameComparator().compare(c1, c2);
    }

    private CityNameComparator getCityNameComparator() {
        if (mNameComparator == null) {
            mNameComparator = new CityNameComparator();
        }
        return mNameComparator;
    }
}
