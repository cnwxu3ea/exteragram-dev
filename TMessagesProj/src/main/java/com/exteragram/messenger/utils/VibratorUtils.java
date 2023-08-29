/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;

import com.exteragram.messenger.ExteraConfig;

import org.telegram.messenger.ApplicationLoader;

public class VibratorUtils {

    private static final Vibrator vibrator;

    static {
        vibrator = (Vibrator) ApplicationLoader.applicationContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private static boolean isVibrationAllowed() {
        return ExteraConfig.inAppVibration && vibrator.hasVibrator();
    }

    public static void vibrate(long time) {
        if (!isVibrationAllowed()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                VibrationEffect effect = VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } catch (Exception e) {
                // ignored
            }
        } else {
            try {
                vibrator.vibrate(time);
            } catch (Exception e) {
                // ignored
            }
        }
    }

    public static void vibrate() {
        vibrate(200L);
    }

    public static void vibrateEffect(VibrationEffect effect) {
        if (!isVibrationAllowed()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                vibrator.cancel();
            } catch (Exception e) {
                // ignored
            }

            try {
                vibrator.vibrate(effect);
            } catch (Exception e) {
                // ignored
            }
        }
    }

    public static void disableHapticFeedback(View view) {
        if (view == null) {
            return;
        }

        view.setHapticFeedbackEnabled(false);

        if (view instanceof ViewGroup viewGroup) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childView = viewGroup.getChildAt(i);
                disableHapticFeedback(childView);
            }
        }
    }

    public static int getType(int original) {
        return ExteraConfig.inAppVibration ? original : -1;
    }
}
