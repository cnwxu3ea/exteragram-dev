/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.utils;

import android.net.Uri;
import android.text.TextUtils;
import android.text.style.URLSpan;

import org.json.JSONArray;
import org.json.JSONTokener;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.RestrictedLanguagesSelectActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class TranslatorUtils {

    private static final ArrayList<TranslateController.Language> languages = new ArrayList<>(TranslateController.getLanguages());

    public static final DispatchQueue translateQueue = new DispatchQueue("translateQueue", false);

    public static final String[] deviceModels = {
            "Galaxy S6", "Galaxy S7", "Galaxy S8", "Galaxy S9", "Galaxy S10", "Galaxy S21",
            "Pixel 3", "Pixel 4", "Pixel 5",
            "OnePlus 6", "OnePlus 7", "OnePlus 8", "OnePlus 9", "Xperia XZ", "Xperia XZ2", "Xperia XZ3", "Xperia 1", "Xperia 5", "Xperia 10", "Xperia L4"
    };
    private static final String[] chromeVersions = {
            "111.0.5563.57", "94.0.4606.81", "80.0.3987.119", "69.0.3497.100", "92.0.4515.159", "71.0.3578.99"
    };

    public static String formatUserAgent() {
        String androidVersion = String.valueOf(Utilities.random.nextInt(7) + 6);
        String deviceModel = deviceModels[Utilities.random.nextInt(deviceModels.length)];
        String chromeVersion = chromeVersions[Utilities.random.nextInt(chromeVersions.length)];
        return String.format("Mozilla/5.0 (Linux; Android %s; %s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Mobile Safari/537.36", androidVersion, deviceModel, chromeVersion);
    }

    public interface OnTranslationSuccess {
        void run(CharSequence translated);
    }

    public interface OnTranslationFail {
        void run();
    }

    public static String getLanguageTitleSystem(String isoCode) {
        TranslateController.Language language = languages.stream()
                .filter(lang -> lang.code.equals(isoCode))
                .findFirst()
                .orElse(null);

        return language != null ? language.displayName : null;
    }

    public static boolean isLanguageSupported(String isoCode) {
        return languages.stream().anyMatch(lang -> lang.code.equals(isoCode));
    }

    public static String getLangCodeByIndex(int idx) {
        if (idx >= 0 && idx < languages.size()) {
            return languages.get(idx).code;
        }
        return null;
    }

    public static int getLanguageIndexByIso(String iso) {
        return IntStream.range(0, languages.size())
                .filter(idx -> languages.get(idx).code.equals(iso))
                .findFirst()
                .orElse(-1);
    }

    public static String[] getLanguageTitles() {
        return languages
                .stream()
                .map(lang -> lang.displayName + (lang.ownDisplayName == null ? "" : " – " + lang.ownDisplayName))
                .toArray(String[]::new);
    }

    public static void translateWithAlert(MessageObject selectedObject, MessageObject.GroupedMessages selectedObjectGroup, BaseFragment fragment) {
        if (selectedObject == null) {
            return;
        }

        ChatActivity chatActivity = (ChatActivity) fragment;

        Utilities.CallbackReturn<URLSpan, Boolean> onLinkPress = (link) -> {
            chatActivity.didPressMessageUrl(link, false, selectedObject, null);
            return true;
        };

        ArrayList<TLRPC.MessageEntity> entities = selectedObject.messageOwner != null ? selectedObject.messageOwner.entities : null;
        final CharSequence text = ChatUtils.getMessageText(selectedObject, selectedObjectGroup);
        LanguageDetector.detectLanguage(text == null ? "" : text.toString(), lang -> {
            String toLang = LocaleController.getInstance().getCurrentLocale().getLanguage();
            if (lang != null && (!lang.equals(toLang) || lang.equals("und")) && !RestrictedLanguagesSelectActivity.getRestrictedLanguages().contains(lang)) {
                TranslateAlert2.showAlert(fragment.getContext(), fragment, UserConfig.selectedAccount, lang, toLang, text, entities, false, onLinkPress, () -> chatActivity.dimBehindView(false));
            }
        }, err -> {
        });
    }

    public static void translate(CharSequence text, String toLang, OnTranslationSuccess onSuccess, OnTranslationFail onFail) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        if (LanguageDetector.hasSupport()) {
            LanguageDetector.detectLanguage(text.toString(), lng -> {
                String fromLang = "auto";
                if (lng != null && !lng.equals("und")) {
                    fromLang = lng;
                }
                translate(text, fromLang, toLang, onSuccess, onFail);
            }, e -> {
                FileLog.e(e);
                translate(text, "auto", toLang, onSuccess, onFail);
            });
        } else {
            translate(text, "auto", toLang, onSuccess, onFail);
        }
    }

    public static void translate(CharSequence text, String fromLang, String toLang, OnTranslationSuccess onSuccess, OnTranslationFail onFail) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        if (!translateQueue.isAlive()) {
            translateQueue.start();
        }
        translateQueue.postRunnable(() -> {
            String uri;
            HttpURLConnection connection;
            try {
                uri = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=";
                uri += fromLang + "&tl=";
                uri += Uri.encode(toLang);
                uri += "&dt=t&ie=UTF-8&oe=UTF-8&otf=1&ssel=0&tsel=0&kc=7&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&q=";
                uri += Uri.encode(text.toString());
                connection = (HttpURLConnection) new URI(uri).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", formatUserAgent());
                connection.setRequestProperty("Content-Type", "application/json");

                StringBuilder textBuilder = new StringBuilder();
                try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    int c;
                    while ((c = reader.read()) != -1) textBuilder.append((char) c);
                }
                JSONTokener tokener = new JSONTokener(textBuilder.toString());
                JSONArray array = new JSONArray(tokener);
                JSONArray array1 = array.getJSONArray(0);
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < array1.length(); ++i) {
                    String blockText = array1.getJSONArray(i).getString(0);
                    if (blockText != null && !blockText.equals("null"))
                        result.append(blockText);
                }
                if (text.length() > 0 && text.charAt(0) == '\n') result.insert(0, "\n");
                if (onSuccess != null)
                    AndroidUtilities.runOnUIThread(() -> onSuccess.run(result.toString()));
            } catch (Exception e) {
                e.printStackTrace();
                if (onFail != null)
                    AndroidUtilities.runOnUIThread(onFail::run);
            }
        });
    }
}
