/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.utils;

import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;

import org.telegram.messenger.FileLog;

public class VoiceEnhancer {

    private static NoiseSuppressor noiseSuppressor;
    private static AutomaticGainControl automaticGainControl;
    private static AcousticEchoCanceler acousticEchoCanceler;

    public static void init(AudioRecord audioRecord) {
        if (NoiseSuppressor.isAvailable() || AutomaticGainControl.isAvailable() || AcousticEchoCanceler.isAvailable()) {
            int sessionId = audioRecord.getAudioSessionId();

            if (AutomaticGainControl.isAvailable()) {
                automaticGainControl = AutomaticGainControl.create(sessionId);
                automaticGainControl.setEnabled(true);
            }

            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId);
                noiseSuppressor.setEnabled(true);
            }

            if (AcousticEchoCanceler.isAvailable()) {
                acousticEchoCanceler = AcousticEchoCanceler.create(sessionId);
                acousticEchoCanceler.setEnabled(true);
            }

            FileLog.d("VOICE_ENHANCER: AGC=" + AutomaticGainControl.isAvailable() + " NS=" + NoiseSuppressor.isAvailable() + " AEC=" + AcousticEchoCanceler.isAvailable());
        }
    }

    public static void release() {
        if (automaticGainControl != null) {
            automaticGainControl.release();
            automaticGainControl = null;
        }

        if (noiseSuppressor != null) {
            noiseSuppressor.release();
            noiseSuppressor = null;
        }

        if (acousticEchoCanceler != null) {
            acousticEchoCanceler.release();
            acousticEchoCanceler = null;
        }
    }
}
