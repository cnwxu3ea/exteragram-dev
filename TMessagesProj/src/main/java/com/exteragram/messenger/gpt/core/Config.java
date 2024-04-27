/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.gpt.core;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.telegram.messenger.ApplicationLoader;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Config {

    private static final Object sync = new Object();

    private static boolean configLoaded;

    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;

    public static boolean saveHistory;
    public static boolean responseStreaming;
    public static boolean showResponseOnly;

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }

            preferences = ApplicationLoader.applicationContext.getSharedPreferences("gptconfig", Activity.MODE_PRIVATE);
            editor = preferences.edit();

            saveHistory = preferences.getBoolean("saveHistory", true);
            responseStreaming = preferences.getBoolean("responseStreaming", true);
            showResponseOnly = preferences.getBoolean("showResponseOnly", false);

            configLoaded = true;
        }
    }

    public static String getUrl() {
        return preferences.getString("url", "https://api.openai.com/v1");
    }

    public static void setUrl(String url) {
        editor.putString("url", url).apply();
    }

    public static String getModel() {
        return preferences.getString("model", "gpt-3.5-turbo");
    }

    public static void setModel(String model) {
        editor.putString("model", model).apply();
    }

    public static String encrypt(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        return Base64.encodeToString(text.getBytes(), Base64.DEFAULT);
    }

    public static String decrypt(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        return new String(Base64.decode(text, Base64.DEFAULT));
    }

    public static String getApiKey() {
        return decrypt(preferences.getString("apiKey", null));
    }

    public static void setApiKey(String key) {
        editor.putString("apiKey", encrypt(key)).apply();
    }

    public static boolean isApiKeySet() {
        return !TextUtils.isEmpty(getApiKey());
    }

    public static void saveConversationHistory(ArrayList<Message> arrayList) {
        editor.putString("conversationHistory", (new Gson()).toJson(arrayList)).apply();
    }

    public static ArrayList<Message> getConversationHistory() {
        Type type = new TypeToken<ArrayList<Message>>() {
        }.getType();
        return (new Gson()).fromJson(preferences.getString("conversationHistory", null), type);
    }

    public static void clearConversationHistory() {
        editor.remove("conversationHistory").apply();
    }

    public static void saveRoles(ArrayList<Role> arrayList) {
        editor.putString("roles", (new Gson()).toJson(arrayList)).apply();
    }

    public static ArrayList<Role> getRoles() {
        Type type = new TypeToken<ArrayList<Role>>() {
        }.getType();
        return (new Gson()).fromJson(preferences.getString("roles", null), type);
    }

    public static String getSelectedRole() {
        return preferences.getString("selectedRole", Suggestions.values()[0].getRole().getName());
    }

    public static void setSelectedRole(Role role) {
        editor.putString("selectedRole", role.getName()).apply();
        clearConversationHistory();
    }

    public static void clearSelectedRole() {
        editor.remove("selectedRole").apply();
        clearConversationHistory();
    }
}
