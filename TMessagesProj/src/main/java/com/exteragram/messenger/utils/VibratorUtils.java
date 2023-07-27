package com.exteragram.messenger.utils;


import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;

import com.exteragram.messenger.ExteraConfig;

import org.telegram.messenger.ApplicationLoader;

public class VibratorUtils {

    private static Vibrator vibrator;

    public static void vibrate(long time) {
        if (!ExteraConfig.inAppVibration) {
            return;
        }

        if (vibrator == null) {
            vibrator = (Vibrator) ApplicationLoader.applicationContext.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                VibrationEffect effect = VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                vibrator.vibrate(time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void vibrate() {
        vibrate(200L);
    }

    public static void disableHapticFeedback(View view) {
        view.setHapticFeedbackEnabled(false);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childView = viewGroup.getChildAt(i);
                disableHapticFeedback(childView);
            }
        }
    }
}
