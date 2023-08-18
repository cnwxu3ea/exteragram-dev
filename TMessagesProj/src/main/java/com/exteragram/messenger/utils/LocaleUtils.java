/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.utils;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.View;

import com.exteragram.messenger.ExteraConfig;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleUtils {
    public static String getActionBarTitle() {
        String title;
        int actionBarTitle = ExteraConfig.titleText;
        switch (actionBarTitle) {
            case 0 -> title = LocaleController.getString("exteraAppName", R.string.exteraAppName);
            case 3 -> title = LocaleController.getString(R.string.FilterChats);
            default -> {
                TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
                title = actionBarTitle == 1 && !TextUtils.isEmpty(UserObject.getPublicUsername(user)) ? UserObject.getPublicUsername(user) : UserObject.getFirstName(user);
            }
        }
        return title;
    }

    public static CharSequence formatWithUsernames(String text, BaseFragment fragment) {
        int start = -1, end;
        boolean parse = false;
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(text);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '@') {
                start = i;
                parse = true;
            }
            if (parse && (i + 1 == text.length() || (!Character.isAlphabetic(text.charAt(i + 1)) && !Character.isDigit(text.charAt(i + 1))))) {
                end = i + 1;
                parse = false;
                String username = text.substring(start, end);
                try {
                    URLSpanNoUnderline urlSpan = new URLSpanNoUnderline(username) {
                        @Override
                        public void onClick(View widget) {
                            ChatUtils.getMessagesController().openByUserName(username.substring(1), fragment, 1);
                        }
                    };
                    stringBuilder.setSpan(urlSpan, start, end, 0);
                    if (i + 1 == text.length()) {
                        return stringBuilder;
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                    return text;
                }
            }
        }
        return text;
    }

    public static CharSequence formatWithURLs(CharSequence charSequence) {
        Spannable spannable = new SpannableString(charSequence);
        URLSpan[] spans = spannable.getSpans(0, charSequence.length(), URLSpan.class);
        for (URLSpan urlSpan : spans) {
            URLSpan span = urlSpan;
            int start = spannable.getSpanStart(span), end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL()) {
                @Override
                public void onClick(View widget) {
                    super.onClick(widget);
                }
            };
            spannable.setSpan(span, start, end, 0);
        }
        return spannable;
    }

    public static String capitalize(String s) {
        if (s == null)
            return null;
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i == 0) {
                chars[i] = Character.toUpperCase(chars[i]);
            } else if (Character.isLetter(chars[i])) {
                chars[i] = Character.toLowerCase(chars[i]);
            }
        }
        return new String(chars);
    }

    public static String getAppName() {
        try {
            return ApplicationLoader.applicationContext.getString(R.string.exteraAppName);
        } catch (Exception e) {
            return "exteraGram";
        }
    }

    private static final Pattern ZALGO_PATTERN = Pattern.compile("\\p{Mn}{3,}");

    private static boolean canFilter(CharSequence text) {
        return ExteraConfig.filterZalgo && !TextUtils.isEmpty(text) && ZALGO_PATTERN.matcher(text).find();
    }

    public static CharSequence filterSpannable(CharSequence text) {
        if (!canFilter(text)) {
            return text;
        }

        if (!(text instanceof Spannable s)) {
            return filter(text);
        }

        final Spannable finalS = new SpannableString(filter(s));
        Object[] spans = s.getSpans(0, s.length(), Object.class);

        for (Object span : spans) {
            int start = Math.min(s.getSpanStart(span), finalS.length());
            int end = Math.min(s.getSpanEnd(span), finalS.length());
            int flags = s.getSpanFlags(span);

            finalS.setSpan(span, start, end, flags);
        }
        return finalS;
    }

    public static String filter(CharSequence text) {
        return filter(text.toString());
    }

    public static String filter(String text) {
        if (canFilter(text)) {
            Matcher matcher = ZALGO_PATTERN.matcher(text);
            StringBuilder output = new StringBuilder(text.length());
            int lastEnd = 0;

            while (matcher.find()) {
                output.append(text, lastEnd, matcher.start()).
                        append("\u2060".repeat(matcher.group().length()));
                lastEnd = matcher.end();
            }

            return output.append(text, lastEnd, text.length()).toString();
        }
        return text;
    }
}
