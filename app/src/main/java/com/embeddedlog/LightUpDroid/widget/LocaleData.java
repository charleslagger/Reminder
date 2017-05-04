package com.embeddedlog.LightUpDroid.widget;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Locale;


public final class LocaleData {

    private static final HashMap<String, LocaleData> localeDataCache = new HashMap<String, LocaleData>();
    static {

        get(Locale.ROOT);
        get(Locale.US);
        get(Locale.getDefault());
    }

    public Integer firstDayOfWeek;
    public Integer minimalDaysInFirstWeek;


    public String[] amPm; // "AM", "PM".
    public String[] eras; // "BC", "AD".

    public String[] longMonthNames; // "January", ...
    public String[] shortMonthNames; // "Jan", ...
    public String[] tinyMonthNames; // "J", ...
    public String[] longStandAloneMonthNames; // "January", ...
    public String[] shortStandAloneMonthNames; // "Jan", ...
    public String[] tinyStandAloneMonthNames; // "J", ...

    public String[] longWeekdayNames; // "Sunday", ...
    public String[] shortWeekdayNames; // "Sun", ...
    public String[] tinyWeekdayNames; // "S", ...
    public String[] longStandAloneWeekdayNames; // "Sunday", ...
    public String[] shortStandAloneWeekdayNames; // "Sun", ...
    public String[] tinyStandAloneWeekdayNames; // "S", ...

    public String yesterday; // "Yesterday".
    public String today; // "Today".
    public String tomorrow; // "Tomorrow".

    public String fullTimeFormat;
    public String longTimeFormat;
    public String mediumTimeFormat;
    public String shortTimeFormat;

    public String fullDateFormat;
    public String longDateFormat;
    public String mediumDateFormat;
    public String shortDateFormat;


    public String shortDateFormat4;


    public String timeFormat12; // "hh:mm a"
    public String timeFormat24; // "HH:mm"

    public char zeroDigit;
    public char decimalSeparator;
    public char groupingSeparator;
    public char patternSeparator;
    public char percent;
    public char perMill;
    public char monetarySeparator;
    public char minusSign;
    public String exponentSeparator;
    public String infinity;
    public String NaN;
    public String currencySymbol;
    public String internationalCurrencySymbol;

    public String numberPattern;
    public String integerPattern;
    public String currencyPattern;
    public String percentPattern;

    private LocaleData() {
    }


    public static LocaleData get(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String localeName = locale.toString();
        synchronized (localeDataCache) {
            LocaleData localeData = localeDataCache.get(localeName);
            if (localeData != null) {
                return localeData;
            }
        }
        LocaleData newLocaleData = initLocaleData(locale);
        synchronized (localeDataCache) {
            LocaleData localeData = localeDataCache.get(localeName);
            if (localeData != null) {
                return localeData;
            }
            localeDataCache.put(localeName, newLocaleData);
            return newLocaleData;
        }
    }

    public String getDateFormat(int style) {
        switch (style) {
            case DateFormat.SHORT:
                return shortDateFormat;
            case DateFormat.MEDIUM:
                return mediumDateFormat;
            case DateFormat.LONG:
                return longDateFormat;
            case DateFormat.FULL:
                return fullDateFormat;
        }
        throw new AssertionError();
    }

    public String getTimeFormat(int style) {
        switch (style) {
            case DateFormat.SHORT:
                return shortTimeFormat;
            case DateFormat.MEDIUM:
                return mediumTimeFormat;
            case DateFormat.LONG:
                return longTimeFormat;
            case DateFormat.FULL:
                return fullTimeFormat;
        }
        throw new AssertionError();
    }

    private static LocaleData initLocaleData(Locale locale) {
        LocaleData localeData = new LocaleData();
        if (!ICU.initLocaleDataImpl(locale.toString(), localeData)) {
            throw new AssertionError("couldn't initialize LocaleData for locale " + locale);
        }


        localeData.timeFormat12 = ICU.getBestDateTimePattern("hm", locale.toString());
        localeData.timeFormat24 = ICU.getBestDateTimePattern("Hm", locale.toString());

        if (localeData.fullTimeFormat != null) {

            localeData.fullTimeFormat = localeData.fullTimeFormat.replace('v', 'z');
        }
        if (localeData.numberPattern != null) {

            localeData.integerPattern = localeData.numberPattern.replaceAll("\\.[#,]*", "");
        }
        localeData.shortDateFormat4 = localeData.shortDateFormat.replaceAll("\\byy\\b", "y");
        return localeData;
    }
}