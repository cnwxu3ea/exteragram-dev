/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.backup;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.camera.CameraXUtils;
import com.exteragram.messenger.gpt.core.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ShareAlert;
import org.telegram.ui.LaunchActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PreferencesUtils {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static PreferencesUtils instance;

    public static PreferencesUtils getInstance() {
        if (instance == null) {
            instance = new PreferencesUtils();
        }
        return instance;
    }

    private final static String[] configs = new String[]{
            "exteraconfig",
            "gptconfig",
            "mainconfig"
    };

    private static class BackupItem implements Serializable {
        public String key;
        public Class<?> clazz;

        public BackupItem(String key) {
            this(key, Boolean.class);
        }

        public BackupItem(String key, Class<?> clazz) {
            this.key = key;
            this.clazz = clazz;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            BackupItem other = (BackupItem) obj;

            return key.equals(other.key);
        }
    }

    private final BackupItem[] backup = {
            new BackupItem("addCommaAfterMention"),
            new BackupItem("administratorsShortcut"),
            new BackupItem("immersiveDrawerAnimation"),
            new BackupItem("archiveOnPull"),
            new BackupItem("archivedChats"),
            new BackupItem("avatarCorners", Float.class),
            new BackupItem("blurActionBar"),
            new BackupItem("blurBottomPanel"),
            new BackupItem("blurDialogs"),
            new BackupItem("blurSmoothness"),
            new BackupItem("bottomButton"),
            new BackupItem("calls"),
            new BackupItem("cameraResolution"),
            new BackupItem("cameraType"),
            new BackupItem("centerTitle"),
            new BackupItem("changeStatus"),
            new BackupItem("channelToSave", Long.class),
            new BackupItem("checkUpdatesOnLaunch"),
            new BackupItem("contacts"),
            new BackupItem("disableDividers"),
            new BackupItem("disableJumpToNextChannel"),
            new BackupItem("disableNumberRounding"),
            new BackupItem("disablePlayback"),
            new BackupItem("disableUnarchiveSwipe"),
            new BackupItem("doubleTapAction"),
            new BackupItem("doubleTapActionOutOwner"),
            new BackupItem("doubleTapSeekDuration"),
            new BackupItem("downloadSpeedBoost"),
            new BackupItem("eventType"),
            new BackupItem("filterZalgo"),
            new BackupItem("flashIntensity"),
            new BackupItem("flashWarmth"),
            new BackupItem("forceBlur"),
            new BackupItem("forceSnow"),
            new BackupItem("formatTimeWithSeconds"),
            new BackupItem("hideActionBarStatus"),
            new BackupItem("hideAllChats"),
            new BackupItem("hideCameraTile"),
            new BackupItem("hideKeyboardOnScroll"),
            new BackupItem("hidePhoneNumber"),
            new BackupItem("hidePhotoCounter"),
            new BackupItem("hideReactions"),
            new BackupItem("hideSendAsPeer"),
            new BackupItem("hideShareButton"),
            new BackupItem("hideStickerTime"),
            new BackupItem("hideStories"),
            new BackupItem("inAppVibration"),
            new BackupItem("membersShortcut"),
            new BackupItem("menuBots"),
            new BackupItem("myStories"),
            new BackupItem("newChannel"),
            new BackupItem("newGroup"),
            new BackupItem("newSecretChat"),
            new BackupItem("newSwitchStyle"),
            new BackupItem("pauseOnMinimize"),
            new BackupItem("peopleNearby"),
            new BackupItem("permissionsShortcut"),
            new BackupItem("recentActionsShortcut"),
            new BackupItem("rememberLastUsedCamera"),
            new BackupItem("savedMessages"),
            new BackupItem("scanQr"),
            new BackupItem("sendPhotosQuality"),
            new BackupItem("showActionTimestamps"),
            new BackupItem("showClearButton"),
            new BackupItem("showCopyPhotoButton"),
            new BackupItem("showDetailsButton"),
            new BackupItem("showGenerateButton"),
            new BackupItem("showHistoryButton"),
            new BackupItem("showIdAndDc"),
            new BackupItem("showReportButton"),
            new BackupItem("showSaveMessageButton"),
            new BackupItem("singleCornerRadius"),
            new BackupItem("squareFab"),
            new BackupItem("staticZoom"),
            new BackupItem("stickerShape"),
            new BackupItem("stickerSize", Float.class),
            new BackupItem("tabCounter"),
            new BackupItem("tabIcons"),
            new BackupItem("tabStyle"),
            new BackupItem("tabletMode"),
            new BackupItem("targetLang", String.class),
            new BackupItem("titleText"),
            new BackupItem("unlimitedRecentStickers"),
            new BackupItem("uploadSpeedBoost"),
            new BackupItem("useCameraXOptimizedMode"),
            new BackupItem("useGoogleAnalytics"),
            new BackupItem("useGoogleCrashlytics"),
            new BackupItem("springAnimations"),
            new BackupItem("useSolarIcons"),
            new BackupItem("useSystemFonts"),
            new BackupItem("videoMessagesCamera"),
            new BackupItem("voiceHintShowcases"),

            // gpt
            // new BackupItem("apiKey"),
            // new BackupItem("conversationHistory"),
            new BackupItem("roles", String.class),
            new BackupItem("selectedRole", String.class),
            new BackupItem("saveHistory"),
            new BackupItem("use16KModel"),
            new BackupItem("responseStreaming"),
            new BackupItem("showResponseOnly"),

            // main
            new BackupItem("ChatSwipeAction"),
            new BackupItem("allowBigEmoji"),
            new BackupItem("archiveHidden"),
            new BackupItem("bubbleRadius"),
            new BackupItem("font_size"),
            new BackupItem("mediaColumnsCount"),
            new BackupItem("next_media_on_tap"),
            new BackupItem("pauseMusicOnMedia"),
            new BackupItem("pauseMusicOnRecord"),
            new BackupItem("raise_to_listen"),
            new BackupItem("raise_to_speak"),
            new BackupItem("record_via_sco"),
            new BackupItem("suggestAnimatedEmoji"),
            new BackupItem("suggestStickers"),
            new BackupItem("useThreeLinesLayout")
    };

    private boolean isExceptedValue(String key, Object value) {
        if (key != null) {
            for (BackupItem item : backup) {
                if (item.key.equals(key)) {
                    if (value instanceof JsonPrimitive v) {
                        if (v.isNumber()) {
                            if (item.clazz.equals(Float.class)) {
                                return isExceptedFloat(key, v.getAsFloat());
                            } else if (item.clazz.equals(Long.class)) {
                                long l = v.getAsLong();
                                if (key.equals("channelToSave" + UserConfig.selectedAccount)) {
                                    return l > 0;
                                }
                            } else {
                                return isExceptedInteger(key, v.getAsInt());
                            }
                        } else if (v.isBoolean()) {
                            return true;
                        } else if (v.isString() && item.clazz.equals(String.class)) {
                            String s = v.getAsString();
                            if (key.equals("targetLang")) {
                                return s.matches("^[a-zA-Z]{1,3}$");
                            }
                            return !TextUtils.isEmpty(s);
                        }
                    } else if (value instanceof Float f) {
                        return isExceptedFloat(key, f);
                    } else if (value instanceof Long l) {
                        if (key.equals("channelToSave" + UserConfig.selectedAccount)) {
                            return l > 0;
                        }
                    } else if (value instanceof Integer n) {
                        return isExceptedInteger(key, n);
                    } else if (value instanceof String s) {
                        if (key.equals("targetLang")) {
                            return s.matches("^[a-zA-Z]{1,3}$");
                        }
                        return !TextUtils.isEmpty(s);
                    } else if (value instanceof Boolean) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isExceptedFloat(String key, float f) {
        return switch (key) {
            case "avatarCorners" -> f >= 0 && f <= 28f;
            case "stickerSize" -> f >= 0 && f <= 14f;
            default -> false;
        };
    }

    private boolean isExceptedInteger(String key, int n) {
        return switch (key) {
            case "tabIcons", "cameraType", "tabletMode", "downloadSpeedBoost", "showIdAndDc", "stickerShape", "bottomButton", "sendPhotosQuality", "videoMessagesCamera", "blurSmoothness" ->
                    n >= 0 && n <= 2;
            case "titleText", "doubleTapSeekDuration" -> n >= 0 && n <= 3;
            case "eventType", "tabStyle" -> n >= 0 && n <= 4;
            case "ChatSwipeAction" -> n >= 0 && n <= 5;
            case "doubleTapAction", "doubleTapActionOutOwner" -> n >= 0 && n <= 8;
            case "mediaColumnsCount" -> n >= 2 && n <= 9;
            case "bubbleRadius" -> n >= 0 && n <= 17;
            case "font_size" -> n >= 12 && n <= 30;
            case "cameraResolution" -> n >= 0;
            default -> false;
        };
    }

    public void exportSettings(BaseFragment fragment) {
        File file = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), generateBackupName(null));

        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream stream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            writer.write(getBackup(true));
            writer.flush();
            writer.close();

            fragment.showDialog(new ShareAlert(fragment.getParentActivity(), null, null, file.getAbsolutePath(), null, null, false, null, null, false, false, false, null) {
                @Override
                protected void onSend(LongSparseArray<TLRPC.Dialog> dids, int count, TLRPC.TL_forumTopic topic) {
                    AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, LocaleController.getString(R.string.SettingsSaved)).show(), 250);
                }
            });
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public String getBackup(boolean encrypt) {
        JsonObject jsonPrefs = new JsonObject();

        for (String config : configs) {
            if (config.equals("mainconfig") && UserConfig.selectedAccount != 0) {
                config += UserConfig.selectedAccount;
            }
            JsonObject object = toJsonObject(getPreferences(config).getAll());
            if (object.size() > 0) {
                jsonPrefs.add(config, object);
            }
        }

        String backupData = gson.toJson(jsonPrefs);

        if (encrypt) {
            return InvisibleEncryptor.encode(backupData);
        } else {
            return backupData;
        }
    }

    private JsonObject toJsonObject(Map<String, ?> map) {
        JsonObject jsonObject = new JsonObject();

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isExceptedValue(key, value)) {
                JsonElement jsonValue = gson.toJsonTree(value);
                jsonObject.add(key, jsonValue);
            }
        }

        return jsonObject;
    }

    public static void clearPreferences() {
        Config.editor.clear().apply();
        ExteraConfig.editor.clear().apply();
        ExteraConfig.reloadConfig();
    }

    public void importSettings(MessageObject messageObject, Activity activity, INavigationLayout parentLayout) {
        if (!isBackup(messageObject)) {
            return;
        }

        File backupFile = getFileFromMessage(messageObject);
        JsonObject jsonObject = getJsonObject(backupFile);

        for (String config : configs) {
            importConfig(jsonObject, config);
        }

        ExteraConfig.reloadConfig();
        SharedConfig.reloadConfig();

        LocaleController.getInstance().recreateFormatters();
        CameraXUtils.loadSuggestedResolution();
        ((LaunchActivity) activity).reloadIcons();
        Theme.reloadAllResources(activity);

        //noinspection deprecation
        parentLayout.rebuildAllFragmentViews(false, false);

        NotificationCenter currentAccount = AccountInstance.getInstance(UserConfig.selectedAccount).getNotificationCenter();
        currentAccount.postNotificationName(NotificationCenter.reloadInterface);
        currentAccount.postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT);
        currentAccount.postNotificationName(NotificationCenter.mainUserInfoChanged);
        currentAccount.postNotificationName(NotificationCenter.dialogFiltersUpdated);
    }

    private void importConfig(JsonObject jsonObject, String configType) {
        if (jsonObject.has(configType)) {
            JsonObject configObject = jsonObject.getAsJsonObject(configType);
            SharedPreferences.Editor editor = getPreferences(configType).edit();
            for (String key : configObject.keySet()) {
                JsonElement value = configObject.get(key);
                if (isExceptedValue(key, value)) {
                    for (BackupItem item : backup) {
                        if (item.key.equals(key) && value instanceof JsonPrimitive v) {
                            if (v.isNumber()) {
                                if (item.clazz.equals(Float.class)) {
                                    editor.putFloat(key, v.getAsFloat());
                                } else if (item.clazz.equals(Long.class)) {
                                    editor.putLong(key, v.getAsLong());
                                } else {
                                    editor.putInt(key, v.getAsInt());
                                }
                            } else if (v.isBoolean()) {
                                editor.putBoolean(key, v.getAsBoolean());
                            } else if (v.isString()) {
                                editor.putString(key, v.getAsString());
                            }
                        }
                    }
                }
            }
            editor.apply();
        }
    }

    private File getFileFromMessage(MessageObject messageObject) {
        String filePath = messageObject.messageOwner.attachPath;
        if (!TextUtils.isEmpty(filePath)) {
            File temp = new File(filePath);
            if (!temp.exists()) {
                filePath = null;
            }
        }
        if (TextUtils.isEmpty(filePath)) {
            filePath = FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner).toString();
            File temp = new File(filePath);
            if (!temp.exists()) {
                filePath = null;
            }
        }
        if (TextUtils.isEmpty(filePath)) {
            filePath = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(messageObject.getDocument(), true).toString();
            File temp = new File(filePath);
            if (!temp.isFile()) {
                return null;
            }
        }
        return new File(filePath);
    }

    public static SharedPreferences getPreferences(String name) {
        return ApplicationLoader.applicationContext.getSharedPreferences(name, Activity.MODE_PRIVATE);
    }

    public JsonObject getJsonObject(File file) {
        try {
            String data = readAndDecryptFile(file);

            if (data != null) {
                return gson.fromJson(data, JsonObject.class);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private String readAndDecryptFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String data = stringBuilder.toString();

            if (InvisibleEncryptor.isEncrypted(data)) {
                data = InvisibleEncryptor.decode(data);
            }

            return data;
        }
    }

    private boolean checkKeys(JsonObject jsonObject) {
        for (String config : configs) {
            if (jsonObject.has(config)) {
                JsonObject configObject = jsonObject.getAsJsonObject(config);
                for (String key : configObject.keySet()) {
                    JsonElement value = configObject.get(key);
                    if (isExceptedValue(key, value)) {
                        return true;
                    } else {
                        FileLog.e("Unexpected value: " + key + " " + value);
                    }
                }
            }
        }
        return false;
    }

    public boolean isBackup(MessageObject messageObject) {
        return messageObject != null && messageObject.getDocumentName() != null && isBackup(getFileFromMessage(messageObject));
    }

    public boolean isBackup(File file) {
        if (file == null || !file.getName().toLowerCase().endsWith(".extera")) {
            return false;
        }
        JsonObject jsonObject = getJsonObject(file);
        return jsonObject != null && checkKeys(jsonObject);
    }

    public static String generateBackupName(String custom) {
        return (custom != null ? custom : "backup") + "-" +
                Utilities.generateRandomString(4) +
                ".extera";
    }


    public int getDiff(MessageObject messageObject) {
        return getDiff(
                getJsonObject(
                        getFileFromMessage(messageObject)
                ));
    }

    public int getDiff(JsonObject newSettings) {
        int differenceCount = 0;
        JsonObject currentSettings = gson.fromJson(getBackup(false), JsonObject.class);

        for (String configType : newSettings.keySet()) {
            if (currentSettings.has(configType)) {
                JsonObject newConfig = newSettings.getAsJsonObject(configType);
                JsonObject currentConfig = currentSettings.getAsJsonObject(configType);

                for (String key : newConfig.keySet()) {
                    Object v1 = newConfig.get(key), v2 = currentConfig.get(key);
                    if (!currentConfig.has(key) || !v1.equals(v2)) {
                        if (isExceptedValue(key, v1)) {
                            differenceCount++;
                        }
                    }
                }
            } else {
                differenceCount += newSettings.getAsJsonObject(configType).size();
            }
        }

        return differenceCount;
    }
}
