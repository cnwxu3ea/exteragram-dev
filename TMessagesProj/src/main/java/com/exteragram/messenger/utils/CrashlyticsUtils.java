/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.utils;

import android.os.Build;
import android.os.Bundle;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.SharedConfig;

public class CrashlyticsUtils {

    private static String getPerformanceClassString() {
        return switch (SharedConfig.getDevicePerformanceClass()) {
            case SharedConfig.PERFORMANCE_CLASS_LOW -> "Low";
            case SharedConfig.PERFORMANCE_CLASS_AVERAGE -> "Average";
            case SharedConfig.PERFORMANCE_CLASS_HIGH -> "High";
            default -> "N/A";
        };
    }

    public static void logEvents() {
        if (ApplicationLoader.getFirebaseAnalytics() == null) {
            return;
        }
        Bundle params = new Bundle();
        params.putString("android_version", Build.VERSION.RELEASE);
        params.putString("version", BuildConfig.VERSION_NAME);
        params.putInt("version_code", BuildConfig.VERSION_CODE);
        params.putString("device", LocaleUtils.capitalize(Build.MANUFACTURER) + " " + Build.MODEL);
        params.putString("performance_class", getPerformanceClassString());
        params.putString("locale", LocaleController.getSystemLocaleStringIso639());
        params.putString("cache_path", AndroidUtilities.getCacheDir().getAbsolutePath());
        params.putInt("refresh_rate", (int) AndroidUtilities.screenRefreshRate);
        params.putString("display", AndroidUtilities.displaySize.x + "x" + AndroidUtilities.displaySize.y);
        params.putBoolean("debug_build", BuildVars.isBetaApp());
        ApplicationLoader.getFirebaseAnalytics().logEvent("stats", params);
    }
}
