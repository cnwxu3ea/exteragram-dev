/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.exteragram.messenger.backup.PreferencesUtils;
import com.exteragram.messenger.icons.BaseIconSet;
import com.exteragram.messenger.icons.DefaultIconSet;
import com.exteragram.messenger.icons.SolarIconSet;
import com.exteragram.messenger.utils.TranslatorUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.camera.Size;
import org.telegram.tgnet.TLRPC;

import java.util.Arrays;

public class ExteraConfig {

    private static final Object sync = new Object();

    // Appearance
    public static float avatarCorners;
    public static boolean singleCornerRadius;

    public static boolean hideActionBarStatus;
    public static boolean hideStories;
    public static boolean hideAllChats;
    public static boolean tabCounter;
    public static boolean centerTitle;
    public static int tabIcons; // icons with titles - 0, titles - 1, icons - 2
    public static int tabStyle;
    public static int titleText;

    public static boolean useSolarIcons;

    public static boolean squareFab;
    public static boolean forceSnow;
    public static boolean useSystemFonts;
    public static boolean newSwitchStyle;
    public static boolean disableDividers;
    public static boolean springAnimations;

    // Blur Preferences
    public static int blurSmoothness;
    public static boolean forceBlur;
    public static boolean blurActionBar;
    public static boolean blurBottomPanel;
    public static boolean blurDialogs;

    public static int eventType;
    public static boolean immersiveDrawerAnimation;
    public static boolean changeStatus, myProfile, menuBots, newGroup, newSecretChat, newChannel, contacts, calls, peopleNearby, archivedChats, savedMessages, scanQr;

    // General
    public static boolean disableNumberRounding;
    public static boolean formatTimeWithSeconds;
    public static boolean inAppVibration;
    public static boolean filterZalgo;
    public static int tabletMode;

    public static int downloadSpeedBoost;
    public static boolean uploadSpeedBoost;

    public static boolean hidePhoneNumber;
    public static int showIdAndDc;

    public static boolean archiveOnPull;
    public static boolean disableUnarchiveSwipe;

    // Chats
    public static float stickerSize;

    public static boolean replyColors;
    public static boolean replyEmoji;
    public static boolean replyBackground;

    public static int stickerShape;

    public static boolean hideStickerTime;
    public static boolean unlimitedRecentStickers;
    public static boolean hideSendAsPeer;
    public static boolean hideReactions;

    public static int doubleTapAction;
    public static int doubleTapActionOutOwner;

    public static int bottomButton;
    public static boolean hideKeyboardOnScroll;
    public static boolean permissionsShortcut;
    public static boolean administratorsShortcut;
    public static boolean membersShortcut;
    public static boolean recentActionsShortcut;
    public static boolean quickTransitionForChannels;
    public static boolean quickTransitionForTopics;
    public static boolean showActionTimestamps;
    public static boolean hideShareButton;
    public static boolean replaceEditedWithIcon;
    public static boolean showCopyPhotoButton;
    public static boolean showClearButton;
    public static boolean showSaveMessageButton;
    public static boolean showDetailsButton;
    public static boolean showGenerateButton;
    public static boolean showReportButton;
    public static boolean showHistoryButton;
    public static boolean addCommaAfterMention;

    public static int sendPhotosQuality;
    public static boolean hideCameraTile;
    public static boolean hidePhotoCounter;

    public static boolean useCamera2;
    public static int cameraAspectRatio;
    public static boolean staticZoom;
    public static int videoMessagesCamera; // front rear ask
    public static boolean rememberLastUsedCamera;

    public static int doubleTapSeekDuration;
    public static boolean swipeToPip;
    public static boolean pauseOnMinimize;
    public static boolean disablePlayback;

    // Updates
    public static long lastUpdateCheckTime;
    public static long updateScheduleTimestamp;
    public static boolean checkUpdatesOnLaunch;

    // Other
    private static final long[] OFFICIAL_CHANNELS = {
            1233768168,
            1524581881,
            1571726392,
            1632728092,
            1172503281,
            1877362358
    };
    private static final long[] DEVS = {
            963080346,
            1282540315,
            1374434073,
            1972014627,
            168769611,
            1773117711,
            5330087923L,
            666154369,
            139303278,
            668557709
    };
    public static String targetLang;
    public static int voiceHintShowcases;
    public static boolean useGoogleCrashlytics;
    public static boolean useGoogleAnalytics;

    public static float flashWarmth, flashIntensity;

    private static boolean configLoaded;

    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }

            preferences = PreferencesUtils.getPreferences("exteraconfig");
            editor = preferences.edit();

            // General
            disableNumberRounding = preferences.getBoolean("disableNumberRounding", false);
            formatTimeWithSeconds = preferences.getBoolean("formatTimeWithSeconds", false);
            inAppVibration = preferences.getBoolean("inAppVibration", true);
            filterZalgo = preferences.getBoolean("filterZalgo", true);
            tabletMode = preferences.getInt("tabletMode", 0);

            downloadSpeedBoost = preferences.getInt("downloadSpeedBoost", 0);
            uploadSpeedBoost = preferences.getBoolean("uploadSpeedBoost", false);

            hidePhoneNumber = preferences.getBoolean("hidePhoneNumber", false);
            showIdAndDc = preferences.getInt("showIdAndDc", 1);

            archiveOnPull = preferences.getBoolean("archiveOnPull", false);
            disableUnarchiveSwipe = preferences.getBoolean("disableUnarchiveSwipe", true);

            // Appearance
            avatarCorners = preferences.getFloat("avatarCorners", 28.0f);
            singleCornerRadius = preferences.getBoolean("singleCornerRadius", false);

            hideActionBarStatus = preferences.getBoolean("hideActionBarStatus", false);
            hideStories = preferences.getBoolean("hideStories", false);
            centerTitle = preferences.getBoolean("centerTitle", false);
            titleText = preferences.getInt("titleText", 0);

            tabCounter = preferences.getBoolean("tabCounter", true);
            tabIcons = preferences.getInt("tabIcons", 1);
            tabStyle = preferences.getInt("tabStyle", 1);
            hideAllChats = preferences.getBoolean("hideAllChats", false);

            useSolarIcons = preferences.getBoolean("useSolarIcons", true);

            squareFab = preferences.getBoolean("squareFab", true);
            forceSnow = preferences.getBoolean("forceSnow", false);
            useSystemFonts = preferences.getBoolean("useSystemFonts", true);
            newSwitchStyle = preferences.getBoolean("newSwitchStyle", true);
            disableDividers = preferences.getBoolean("disableDividers", false);
            springAnimations = preferences.getBoolean("springAnimations", true);

            blurSmoothness = preferences.getInt("blurSmoothness", SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_HIGH ? 2 : 0);
            forceBlur = preferences.getBoolean("forceBlur", false);

            boolean blur = PreferencesUtils.getPreferences("mainconfig").getBoolean("chatBlur", true);
            blurActionBar = preferences.getBoolean("blurActionBar", blur);
            blurBottomPanel = preferences.getBoolean("blurBottomPanel", blur);
            blurDialogs = preferences.getBoolean("blurDialogs", blur);

            eventType = preferences.getInt("eventType", 0);
            immersiveDrawerAnimation = preferences.getBoolean("immersiveDrawerAnimation", false);

            changeStatus = preferences.getBoolean("changeStatus", true);
            myProfile = preferences.getBoolean("myProfile", true);
            menuBots = preferences.getBoolean("menuBots", true);
            newGroup = preferences.getBoolean("newGroup", true);
            newSecretChat = preferences.getBoolean("newSecretChat", false);
            newChannel = preferences.getBoolean("newChannel", false);
            contacts = preferences.getBoolean("contacts", true);
            calls = preferences.getBoolean("calls", true);
            peopleNearby = preferences.getBoolean("peopleNearby", false);
            archivedChats = preferences.getBoolean("archivedChats", true);
            savedMessages = preferences.getBoolean("savedMessages", true);
            scanQr = preferences.getBoolean("scanQr", true);

            // Chats
            stickerSize = preferences.getFloat("stickerSize", 12.0f);
            stickerShape = preferences.getInt("stickerShape", 1);

            replyColors = preferences.getBoolean("replyColors", true);
            replyEmoji = preferences.getBoolean("replyEmoji", true);
            replyBackground = preferences.getBoolean("replyBackground", true);

            hideStickerTime = preferences.getBoolean("hideStickerTime", false);
            unlimitedRecentStickers = preferences.getBoolean("unlimitedRecentStickers", false);
            hideSendAsPeer = preferences.getBoolean("hideSendAsPeer", false);
            hideReactions = preferences.getBoolean("hideReactions", false);

            doubleTapAction = preferences.getInt("doubleTapAction", 1);
            doubleTapActionOutOwner = preferences.getInt("doubleTapActionOutOwner", 1);

            bottomButton = preferences.getInt("bottomButton", 2);
            hideKeyboardOnScroll = preferences.getBoolean("hideKeyboardOnScroll", true);
            permissionsShortcut = preferences.getBoolean("permissionsShortcut", false);
            administratorsShortcut = preferences.getBoolean("administratorsShortcut", false);
            membersShortcut = preferences.getBoolean("membersShortcut", false);
            recentActionsShortcut = preferences.getBoolean("recentActionsShortcut", true);
            quickTransitionForChannels = preferences.getBoolean("quickTransitionForChannels", true);
            quickTransitionForTopics = preferences.getBoolean("quickTransitionForTopics", true);
            showActionTimestamps = preferences.getBoolean("showActionTimestamps", true);
            hideShareButton = preferences.getBoolean("hideShareButton", true);
            replaceEditedWithIcon = preferences.getBoolean("replaceEditedWithIcon", true);
            showDetailsButton = preferences.getBoolean("showDetailsButton", false);
            showGenerateButton = preferences.getBoolean("showGenerateButton", true);
            showSaveMessageButton = preferences.getBoolean("showSaveMessageButton", false);
            showCopyPhotoButton = preferences.getBoolean("showCopyPhotoButton", true);
            showClearButton = preferences.getBoolean("showClearButton", true);
            showReportButton = preferences.getBoolean("showReportButton", true);
            showHistoryButton = preferences.getBoolean("showHistoryButton", false);

            addCommaAfterMention = preferences.getBoolean("addCommaAfterMention", true);

            sendPhotosQuality = preferences.getInt("sendPhotosQuality", 1);
            hidePhotoCounter = preferences.getBoolean("hidePhotoCounter", false);
            hideCameraTile = preferences.getBoolean("hideCameraTile", false);

            useCamera2 = preferences.getBoolean("useCamera2", false);
            cameraAspectRatio = preferences.getInt("cameraAspectRatio", 0);
            staticZoom = preferences.getBoolean("staticZoom", false);
            videoMessagesCamera = preferences.getInt("videoMessagesCamera", 0);
            rememberLastUsedCamera = preferences.getBoolean("rememberLastUsedCamera", false);
            pauseOnMinimize = preferences.getBoolean("pauseOnMinimize", true);
            doubleTapSeekDuration = preferences.getInt("doubleTapSeekDuration", 1);
            swipeToPip = preferences.getBoolean("swipeToPip", false);
            disablePlayback = preferences.getBoolean("disablePlayback", true);

            // Updates
            lastUpdateCheckTime = preferences.getLong("lastUpdateCheckTime", 0);
            updateScheduleTimestamp = preferences.getLong("updateScheduleTimestamp", 0);
            checkUpdatesOnLaunch = preferences.getBoolean("checkUpdatesOnLaunch", true);

            // Other
            targetLang = preferences.getString("targetLang", "en");
            voiceHintShowcases = preferences.getInt("voiceHintShowcases", 0);
            useGoogleCrashlytics = preferences.getBoolean("useGoogleCrashlytics", BuildVars.isBetaApp());
            useGoogleAnalytics = preferences.getBoolean("useGoogleAnalytics", false);

            flashWarmth = preferences.getFloat("flashWarmth", 0.75f);
            flashIntensity = preferences.getFloat("flashIntensity", 1f);

            configLoaded = true;
        }
    }

    public static boolean isExtera(@NonNull TLRPC.Chat chat) {
        return Arrays.stream(OFFICIAL_CHANNELS).anyMatch(id -> id == chat.id);
    }

    public static boolean isExteraDev(@NonNull TLRPC.User user) {
        return Arrays.stream(DEVS).anyMatch(id -> id == user.id);
    }

    public static int getAvatarCorners(float size) {
        return getAvatarCorners(size, 0, false, false);
    }

    public static int getAvatarCorners(float size, boolean px) {
        return getAvatarCorners(size, 0, px, false);
    }

    public static int getAvatarCorners(float size, boolean px, boolean forum) {
        return getAvatarCorners(size, 0, px, forum);
    }

    public static int getAvatarCorners(float size, float fix, boolean px, boolean forum) {
        if (avatarCorners == 0) {
            return 0;
        }

        float radius = avatarCorners * (size + size / 56 * dp(fix)) / 56;

        if (!px) {
            radius = dp(radius);
        }

        if (forum && !singleCornerRadius) {
            radius *= 0.65f;
        }

        return (int) Math.ceil(radius);
    }

    public static int getBlurRedrawTimeout() {
        return switch (blurSmoothness) {
            case 1 -> 6;
            case 2 -> 1;
            default -> 16;
        };
    }

    public static int getBlurCrossfadeDuration() {
        return switch (blurSmoothness) {
            case 1 -> 25;
            case 2 -> 1;
            default -> 50;
        };
    }

    public static void toggleBlur(boolean enabled) {
        ExteraConfig.editor.putBoolean("blurActionBar", ExteraConfig.blurActionBar = enabled).apply();
        ExteraConfig.editor.putBoolean("blurBottomPanel", ExteraConfig.blurBottomPanel = enabled).apply();
        ExteraConfig.editor.putBoolean("blurDialogs", ExteraConfig.blurDialogs = enabled).apply();
    }

    public static void toggleDrawerElements(int id) {
        switch (id) {
            case 1 -> editor.putBoolean("newGroup", newGroup ^= true).apply();
            case 2 -> editor.putBoolean("newSecretChat", newSecretChat ^= true).apply();
            case 3 -> editor.putBoolean("newChannel", newChannel ^= true).apply();
            case 4 -> editor.putBoolean("contacts", contacts ^= true).apply();
            case 5 -> editor.putBoolean("calls", calls ^= true).apply();
            case 6 -> editor.putBoolean("peopleNearby", peopleNearby ^= true).apply();
            case 7 -> editor.putBoolean("archivedChats", archivedChats ^= true).apply();
            case 8 -> editor.putBoolean("savedMessages", savedMessages ^= true).apply();
            case 9 -> editor.putBoolean("scanQr", scanQr ^= true).apply();
            case 10 -> editor.putBoolean("changeStatus", changeStatus ^= true).apply();
            case 11 -> editor.putBoolean("myProfile", myProfile ^= true).apply();
            case 12 -> editor.putBoolean("menuBots", menuBots ^= true).apply();
        }
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
    }

    public static void toggleLogging() {
        ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Activity.MODE_PRIVATE).edit().putBoolean("logsEnabled", BuildVars.LOGS_ENABLED ^= true).apply();
        if (!BuildVars.LOGS_ENABLED) FileLog.cleanupLogs();
    }

    public static boolean getLogging() {
        return ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Activity.MODE_PRIVATE).getBoolean("logsEnabled", false); //BuildVars.DEBUG_VERSION);
    }

    public static String getCurrentLangName() {
        return TranslatorUtils.getLanguageTitleSystem(targetLang);
    }

    public static BaseIconSet getIconPack() {
        return useSolarIcons ? new SolarIconSet() : new DefaultIconSet();
    }

    public static int getPhotosQuality() {
        return switch (sendPhotosQuality) {
            case 0 -> 800;
            default -> 1280;
            case 2 -> 2560;
        };
    }

    public static int getDoubleTapSeekDuration() {
        return switch (doubleTapSeekDuration) {
            case 0, 1, 2 -> (doubleTapSeekDuration + 1) * 5000;
            default -> 30000;
        };
    }

    public static void reloadConfig() {
        configLoaded = false;
        loadConfig();
    }

    public static Size getCameraAspectRatio() {
        return switch (cameraAspectRatio) {
            case 1 -> new Size(1, 1);
            case 2 -> new Size(4, 3);
            default -> new Size(16, 9);
        };
    }

    public static Pair<Integer, Integer> getSizeForRatio() {
        return switch (cameraAspectRatio) {
            case 1 -> new Pair<>(960, 960);
            case 2 -> new Pair<>(1280, 960);
            default -> new Pair<>(1280, 720);
        };
    }
}
