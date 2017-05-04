package com.embeddedlog.LightUpDroid.widget;

import java.util.LinkedHashSet;
import java.util.Locale;

public final class ICU {

    private static String[] isoLanguages;

    private static String[] isoCountries;


    public static String[] getISOLanguages() {
        if (isoLanguages == null) {
            isoLanguages = getISOLanguagesNative();
        }
        return isoLanguages.clone();
    }

    public static String[] getISOCountries() {
        if (isoCountries == null) {
            isoCountries = getISOCountriesNative();
        }
        return isoCountries.clone();
    }

    public static Locale localeFromString(String localeName) {
        int first = localeName.indexOf('_');
        int second = localeName.indexOf('_', first + 1);
        if (first == -1) {

            return new Locale(localeName);
        } else if (second == -1) {

            return new Locale(localeName.substring(0, first), localeName.substring(first + 1));
        } else {

            return new Locale(localeName.substring(0, first), localeName.substring(first + 1, second), localeName.substring(second + 1));
        }
    }

    public static Locale[] localesFromStrings(String[] localeNames) {

        LinkedHashSet<Locale> set = new LinkedHashSet<Locale>();
        for (String localeName : localeNames) {
            set.add(localeFromString(localeName));
        }
        return set.toArray(new Locale[set.size()]);
    }

    private static Locale[] availableLocalesCache;
    public static Locale[] getAvailableLocales() {
        if (availableLocalesCache == null) {
            availableLocalesCache = localesFromStrings(getAvailableLocalesNative());
        }
        return availableLocalesCache.clone();
    }

    public static Locale[] getAvailableBreakIteratorLocales() {
        return localesFromStrings(getAvailableBreakIteratorLocalesNative());
    }

    public static Locale[] getAvailableCalendarLocales() {
        return localesFromStrings(getAvailableCalendarLocalesNative());
    }

    public static Locale[] getAvailableCollatorLocales() {
        return localesFromStrings(getAvailableCollatorLocalesNative());
    }

    public static Locale[] getAvailableDateFormatLocales() {
        return localesFromStrings(getAvailableDateFormatLocalesNative());
    }

    public static Locale[] getAvailableDateFormatSymbolsLocales() {
        return getAvailableDateFormatLocales();
    }

    public static Locale[] getAvailableDecimalFormatSymbolsLocales() {
        return getAvailableNumberFormatLocales();
    }

    public static Locale[] getAvailableNumberFormatLocales() {
        return localesFromStrings(getAvailableNumberFormatLocalesNative());
    }

    public static native String getBestDateTimePattern(String skeleton, String localeName);

    public static char[] getDateFormatOrder(String pattern) {
        char[] result = new char[3];
        int resultIndex = 0;
        boolean sawDay = false;
        boolean sawMonth = false;
        boolean sawYear = false;

        for (int i = 0; i < pattern.length(); ++i) {
            char ch = pattern.charAt(i);
            if (ch == 'd' || ch == 'L' || ch == 'M' || ch == 'y') {
                if (ch == 'd' && !sawDay) {
                    result[resultIndex++] = 'd';
                    sawDay = true;
                } else if ((ch == 'L' || ch == 'M') && !sawMonth) {
                    result[resultIndex++] = 'M';
                    sawMonth = true;
                } else if ((ch == 'y') && !sawYear) {
                    result[resultIndex++] = 'y';
                    sawYear = true;
                }
            } else if (ch == 'G') {
                // Ignore the era specifier, if present.
            } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                throw new IllegalArgumentException("Bad pattern character '" + ch + "' in " + pattern);
            } else if (ch == '\'') {
                if (i < pattern.length() - 1 && pattern.charAt(i + 1) == '\'') {
                    ++i;
                } else {
                    i = pattern.indexOf('\'', i + 1);
                    if (i == -1) {
                        throw new IllegalArgumentException("Bad quoting in " + pattern);
                    }
                    ++i;
                }
            } else {
                // Ignore spaces and punctuation.
            }
        }
        return result;
    }


    public static native String getCldrVersion();


    public static native String getIcuVersion();


    public static native String getUnicodeVersion();



    public static native String toLowerCase(String s, String localeName);
    public static native String toUpperCase(String s, String localeName);

    public static final int U_ZERO_ERROR = 0;
    public static final int U_INVALID_CHAR_FOUND = 10;
    public static final int U_TRUNCATED_CHAR_FOUND = 11;
    public static final int U_ILLEGAL_CHAR_FOUND = 12;
    public static final int U_BUFFER_OVERFLOW_ERROR = 15;

    public static boolean U_FAILURE(int error) {
        return error > U_ZERO_ERROR;
    }



    private static native String[] getAvailableBreakIteratorLocalesNative();
    private static native String[] getAvailableCalendarLocalesNative();
    private static native String[] getAvailableCollatorLocalesNative();
    private static native String[] getAvailableDateFormatLocalesNative();
    private static native String[] getAvailableLocalesNative();
    private static native String[] getAvailableNumberFormatLocalesNative();

    public static native String[] getAvailableCurrencyCodes();
    public static native String getCurrencyCode(String countryCode);
    public static native String getCurrencyDisplayName(String locale, String currencyCode);
    public static native int getCurrencyFractionDigits(String currencyCode);
    public static native String getCurrencySymbol(String locale, String currencyCode);

    public static native String getDisplayCountryNative(String countryCode, String locale);
    public static native String getDisplayLanguageNative(String languageCode, String locale);
    public static native String getDisplayVariantNative(String variantCode, String locale);

    public static native String getISO3CountryNative(String locale);
    public static native String getISO3LanguageNative(String locale);

    public static native String addLikelySubtags(String locale);
    public static native String getScript(String locale);

    private static native String[] getISOLanguagesNative();
    private static native String[] getISOCountriesNative();

    static native boolean initLocaleDataImpl(String locale, LocaleData result);
}