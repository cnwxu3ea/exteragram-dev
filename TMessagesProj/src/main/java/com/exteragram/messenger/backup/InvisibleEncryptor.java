package com.exteragram.messenger.backup;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.nio.charset.StandardCharsets;

public class InvisibleEncryptor {
    private static final String CHARACTERS = " ​‌‏ ⁪⁫⁬⁭⁮⁯";

    private static String toStr(int value) {
        StringBuilder result = new StringBuilder();
        while (value > 0) {
            result.insert(0, CHARACTERS.charAt(value % CHARACTERS.length()));
            value /= CHARACTERS.length();
        }
        return result.toString();
    }

    private static int toNum(String encoded) {
        int number = 0;
        for (int i = 0; i < encoded.length(); i++) {
            int index = encoded.length() - i - 1;
            number = (int) (number + (CHARACTERS.indexOf(encoded.substring(index, index + 1)) * Math.pow(CHARACTERS.length(), i)));
        }
        return number;
    }

    public static String encode(@NonNull String source) {
        try {
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            String[] plainText = new String[bytes.length];
            for (int i = 0; i < plainText.length; i++) {
                plainText[i] = toStr(bytes[i] & 255);
            }
            return "  " + String.join(" ", plainText);
        } catch (Exception e) {
            e.printStackTrace();
            return source;
        }
    }

    public static String decode(@NonNull String source) {
        try {
            String[] encodedText = source.replaceFirst("^" + "  ", "").split(" ");
            byte[] plainText = new byte[encodedText.length];
            for (int i = 0; i < plainText.length; i++) {
                plainText[i] = (byte) toNum(encodedText[i]);
            }
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return source;
        }
    }

    public static boolean isEncrypted(String source) {
        if (TextUtils.isEmpty(source)) {
            return false;
        }
        return source.matches("^" + "  " + "([" + CHARACTERS + "\\s]*)");
    }
}
