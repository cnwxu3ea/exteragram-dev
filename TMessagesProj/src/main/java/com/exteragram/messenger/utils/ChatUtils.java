/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import com.exteragram.messenger.ExteraConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.TranscribeButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ChatUtils {

    private static final ChatUtils[] Instance = new ChatUtils[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];

    private static int selectedAccount;

    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            lockObjects[i] = new Object();
        }
    }

    private static SpannableStringBuilder editedIcon;
    private static SpannableStringBuilder channelIcon;

    public static CharSequence getEditedIcon() {
        if (editedIcon == null) {
            editedIcon = new SpannableStringBuilder("\u200D");
            var pencil = new ColoredImageSpan(Theme.chat_pencilIconDrawable, true);
            pencil.setTranslateX(-AndroidUtilities.dp(1));
            editedIcon.setSpan(pencil, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return editedIcon;
    }

    public static CharSequence getChannelIcon() {
        if (channelIcon == null) {
            channelIcon = new SpannableStringBuilder("\u200D");
            channelIcon.setSpan(new ColoredImageSpan(Theme.chat_channelIconDrawable, true), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return channelIcon;
    }

    public ChatUtils(int num) {
        selectedAccount = num;
    }

    public static ChatUtils getInstance() {
        return getInstance(UserConfig.selectedAccount);
    }

    public static ChatUtils getInstance(int num) {
        ChatUtils localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (lockObjects) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new ChatUtils(num);
                }
            }
        }
        return localInstance;
    }

    private static boolean useFallback;
    private static final CharsetDecoder textDecoder = StandardCharsets.UTF_8.newDecoder();

    public static String getDC(TLRPC.User user) {
        return getDC(user, null);
    }

    public static String getDC(TLRPC.Chat chat) {
        return getDC(null, chat);
    }

    public static String getDC(TLRPC.User user, TLRPC.Chat chat) {
        int DC = 0, myDC = getConnectionsManager().getCurrentDatacenterId();
        if (user != null) {
            if (UserObject.isUserSelf(user) && myDC != -1) {
                DC = myDC;
            } else {
                DC = user.photo != null ? user.photo.dc_id : -1;
            }
        } else if (chat != null) {
            DC = chat.photo != null ? chat.photo.dc_id : -1;
        }
        if (DC == -1 || DC == 0) {
            return getDCName(0);
        } else {
            return String.format(Locale.ROOT, "DC%d, %s", DC, getDCName(DC));
        }
    }

    public static String getDCName(int dc) {
        return switch (dc) {
            case 1, 3 -> "Miami FL, USA";
            case 2, 4 -> "Amsterdam, NL";
            case 5 -> "Singapore, SG";
            default -> null;
        };
    }

    public static String getName(long did) {
        String name = null;
        if (DialogObject.isEncryptedDialog(did)) {
            TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(did));
            if (encryptedChat != null) {
                TLRPC.User user = getMessagesController().getUser(encryptedChat.user_id);
                if (user != null)
                    name = ContactsController.formatName(user.first_name, user.last_name);
            }
        } else if (DialogObject.isUserDialog(did)) {
            TLRPC.User user = getMessagesController().getUser(did);
            if (user != null) name = ContactsController.formatName(user.first_name, user.last_name);
        } else {
            TLRPC.Chat chat = getMessagesController().getChat(-did);
            if (chat != null) name = chat.title;
        }
        return did == getUserConfig().getClientUserId() ? LocaleController.getString("SavedMessages", R.string.SavedMessages) : name;
    }

    public boolean canSaveSticker(MessageObject messageObject) {
        return canSaveSticker(messageObject.getDocument());
    }

    public boolean canSaveSticker(TLRPC.Document document) {
        return MessageObject.isStaticStickerDocument(document) || MessageObject.isVideoStickerDocument(document);
    }

    public void saveStickerToGallery(Activity activity, MessageObject messageObject, Utilities.Callback<Uri> callback) {
        saveStickerToGallery(activity, getPathToMessage(messageObject), messageObject.isVideoSticker(), callback);
    }

    public void saveStickerToGallery(Activity activity, TLRPC.Document document, Utilities.Callback<Uri> callback) {
        String path = getFileLoader().getPathToAttach(document, true).toString();
        File temp = new File(path);
        if (!temp.exists()) {
            return;
        }
        saveStickerToGallery(activity, path, MessageObject.isVideoSticker(document), callback);
    }

    public void saveStickerToGallery(Activity activity, String path, boolean isVideo, Utilities.Callback<Uri> callback) {
        utilsQueue.postRunnable(() -> {
            if (!TextUtils.isEmpty(path)) {
                try {
                    FileLog.e(path);
                    if (isVideo) {
                        MediaController.saveFile(path, activity, 1, null, null, callback);
                    } else {
                        Bitmap image = BitmapFactory.decodeFile(path);
                        if (image != null) {
                            File file = new File(path.endsWith(".webp") ? path.replace(".webp", ".png") : path + ".png");
                            FileOutputStream stream = new FileOutputStream(file);
                            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            stream.close();
                            MediaController.saveFile(file.toString(), activity, 0, null, null, callback);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        });
    }


    public interface SearchCallback {
        void run(TLRPC.User user);
    }

    public static void searchById(Long userId, SearchCallback callback) {
        if (userId == 0) {
            return;
        }
        TLRPC.User user = getMessagesController().getUser(userId);
        if (user != null) {
            useFallback = false;
            callback.run(user);
        } else {
            searchUser(userId, true, true, user1 -> {
                if (user1 != null && user1.access_hash != 0) {
                    useFallback = false;
                    callback.run(user1);
                } else {
                    if (!useFallback) {
                        useFallback = true;
                        searchById(0x100000000L + userId, callback);
                    } else {
                        useFallback = false;
                        callback.run(null);
                    }
                }
            });
        }
    }

    private static void searchUser(long userId, boolean searchUser, boolean cache, SearchCallback callback) {
        final long bot_id = 1696868284L;
        TLRPC.User bot = getMessagesController().getUser(bot_id);
        if (bot == null) {
            if (searchUser) {
                resolveUser("tgdb_bot", bot_id, user -> searchUser(userId, false, false, callback));
            } else {
                callback.run(null);
            }
            return;
        }

        String key = "user_search_" + userId;
        RequestDelegate requestDelegate = (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (cache && (!(response instanceof TLRPC.messages_BotResults) || ((TLRPC.messages_BotResults) response).results.isEmpty())) {
                searchUser(userId, searchUser, false, callback);
                return;
            }

            if (response instanceof TLRPC.messages_BotResults res) {
                if (!cache && res.cache_time != 0) {
                    getMessageStorage().saveBotCache(key, res);
                }
                if (res.results.isEmpty()) {
                    callback.run(null);
                    return;
                }
                TLRPC.BotInlineResult result = res.results.get(0);
                if (result.send_message == null || TextUtils.isEmpty(result.send_message.message)) {
                    callback.run(null);
                    return;
                }
                String[] lines = result.send_message.message.split("\n");
                if (lines.length < 3) {
                    callback.run(null);
                    return;
                }
                var user1 = new TLRPC.TL_user();
                for (String line : lines) {
                    line = line.replaceAll("\\p{C}", "").trim();
                    if (line.startsWith("\uD83C\uDD94")) {
                        user1.id = Utilities.parseLong(line.replaceAll("\\D+", "").trim());
                    } else if (line.startsWith("\uD83D\uDCE7")) {
                        user1.username = line.substring(line.indexOf('@') + 1).trim();
                    }
                }
                if (user1.id == 0) {
                    callback.run(null);
                    return;
                }
                if (user1.username != null) {
                    resolveUser(user1.username, user1.id, user -> {
                        if (user != null) {
                            callback.run(user);
                        } else {
                            user1.username = null;
                            callback.run(user1);
                        }
                    });
                } else {
                    callback.run(user1);
                }
            } else {
                callback.run(null);
            }
        });

        if (cache) {
            getMessageStorage().getBotCache(key, requestDelegate);
        } else {
            TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
            req.query = String.valueOf(userId);
            req.bot = getMessagesController().getInputUser(bot);
            req.offset = "";
            req.peer = new TLRPC.TL_inputPeerEmpty();
            getConnectionsManager().sendRequest(req, requestDelegate, ConnectionsManager.RequestFlagFailOnServerErrors);
        }
    }

    private static void resolveUser(String userName, long userId, SearchCallback callback) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = userName;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response != null) {
                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                getMessagesController().putUsers(res.users, false);
                getMessagesController().putChats(res.chats, false);
                getMessageStorage().putUsersAndChats(res.users, res.chats, true, true);
                callback.run(res.peer.user_id == userId ? getMessagesController().getUser(userId) : null);
            } else {
                callback.run(null);
            }
        }));
    }

    public static String getOwnerIds(long stickerSetId) {
        return "int32: " + (stickerSetId >> 32) + '\n' +
                "int64: " + (0x100000000L + (stickerSetId >> 32));
    }

    public static MessagesController getMessagesController() {
        return MessagesController.getInstance(selectedAccount);
    }

    public static MessagesStorage getMessageStorage() {
        return MessagesStorage.getInstance(selectedAccount);
    }

    public static ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(selectedAccount);
    }

    public static FileLoader getFileLoader() {
        return FileLoader.getInstance(selectedAccount);
    }

    public static UserConfig getUserConfig() {
        return UserConfig.getInstance(selectedAccount);
    }

    public static void addMessageToClipboard(MessageObject selectedObject, Runnable callback) {
        String path = getPathToMessage(selectedObject);
        if (!TextUtils.isEmpty(path)) {
            SystemUtils.addFileToClipboard(new File(path), callback);
        }
    }

    public static String getPathToMessage(MessageObject messageObject) {
        if (messageObject == null) {
            return null;
        }
        String path = messageObject.messageOwner.attachPath;
        if (!TextUtils.isEmpty(path)) {
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = getFileLoader().getPathToMessage(messageObject.messageOwner).toString();
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = getFileLoader().getPathToAttach(messageObject.getDocument(), true).toString();
            File temp = new File(path);
            if (!temp.exists()) {
                return null;
            }
        }
        return path;
    }

    public boolean hasArchivedChats() {
        // todo
        return true; // getMessagesController().dialogs_dict.get(DialogObject.makeFolderDialogId(1)) != null;
    }

    public static CharSequence getMessageText(MessageObject selectedObject, MessageObject.GroupedMessages selectedObjectGroup) {
        CharSequence messageTextToTranslate = null;
        if (selectedObject.type != MessageObject.TYPE_EMOJIS && selectedObject.type != MessageObject.TYPE_ANIMATED_STICKER && selectedObject.type != MessageObject.TYPE_STICKER) {
            messageTextToTranslate = getMessageCaption(selectedObject, selectedObjectGroup);
            if (messageTextToTranslate == null && selectedObject.isPoll()) {
                try {
                    TLRPC.Poll poll = ((TLRPC.TL_messageMediaPoll) selectedObject.messageOwner.media).poll;
                    StringBuilder pollText;
                    pollText = new StringBuilder(poll.question.text).append("\n");
                    for (TLRPC.PollAnswer answer : poll.answers)
                        pollText.append("\n\uD83D\uDD18 ").append(answer.text == null ? "" : answer.text.text);
                    messageTextToTranslate = pollText.toString();
                } catch (Exception ignored) {
                }
            }
            if (messageTextToTranslate == null && MessageObject.isMediaEmpty(selectedObject.messageOwner)) {
                messageTextToTranslate = getMessageContent(selectedObject);
            }
            if (messageTextToTranslate != null && Emoji.fullyConsistsOfEmojis(messageTextToTranslate)) {
                messageTextToTranslate = null;
            }
        }
        if (selectedObject.translated || selectedObject.isRestrictedMessage) {
            messageTextToTranslate = null;
        }
        return messageTextToTranslate;
    }

    private static CharSequence getMessageCaption(MessageObject messageObject, MessageObject.GroupedMessages group) {
        String restrictionReason = MessagesController.getRestrictionReason(messageObject.messageOwner.restriction_reason);
        if (!TextUtils.isEmpty(restrictionReason)) {
            return restrictionReason;
        }
        if (messageObject.isVoiceTranscriptionOpen() && !TranscribeButton.isTranscribing(messageObject)) {
            return messageObject.getVoiceTranscription();
        }
        if (messageObject.caption != null) {
            return messageObject.caption;
        }
        if (group == null) {
            return null;
        }
        CharSequence caption = null;
        for (int a = 0, N = group.messages.size(); a < N; a++) {
            MessageObject message = group.messages.get(a);
            if (message.caption != null) {
                if (caption != null) {
                    return null;
                }
                caption = message.caption;
            }
        }
        return caption;
    }

    private static CharSequence getMessageContent(MessageObject messageObject) {
        SpannableStringBuilder str = new SpannableStringBuilder();
        String restrictionReason = MessagesController.getRestrictionReason(messageObject.messageOwner.restriction_reason);
        if (!TextUtils.isEmpty(restrictionReason)) {
            str.append(restrictionReason);
        } else if (messageObject.caption != null) {
            str.append(messageObject.caption);
        } else {
            str.append(messageObject.messageText);
        }
        return str.toString();
    }

    public static String getTextFromCallback(byte[] data) {
        try {
            return textDecoder.decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return Base64.encodeToString(data, Base64.NO_PADDING | Base64.NO_WRAP);
        }
    }

    public void setLikeDialog(long did) {
        ExteraConfig.editor.putLong("channelToSave" + selectedAccount, did).apply();
    }

    public long getLikeDialog() {
        return ExteraConfig.preferences.getLong("channelToSave" + selectedAccount, getUserConfig().getClientUserId());
    }

    public boolean hasBotsInSideMenu() {
        TLRPC.TL_attachMenuBots menuBots = MediaDataController.getInstance(selectedAccount).getAttachMenuBots();
        if (menuBots != null && menuBots.bots != null) {
            for (int i = 0; i < menuBots.bots.size(); i++) {
                TLRPC.TL_attachMenuBot bot = menuBots.bots.get(i);
                if (bot.show_in_side_menu) {
                    return true;
                }
            }
        }
        return false;
    }

    public static long getEmojiIdFrom(MessageObject messageObject, TLRPC.User currentUser) {
        if (messageObject != null && messageObject.messageOwner != null && messageObject.replyMessageObject != null && messageObject.replyMessageObject.messageOwner != null && messageObject.replyMessageObject.messageOwner.from_id != null) {
            if (DialogObject.isEncryptedDialog(messageObject.replyMessageObject.getDialogId())) {
                TLRPC.User user = messageObject.replyMessageObject.isOutOwner() ? UserConfig.getInstance(messageObject.replyMessageObject.currentAccount).getCurrentUser() : currentUser;
                if (user != null) {
                    return UserObject.getEmojiId(user);
                }
            } else if (messageObject.replyMessageObject.isFromUser()) {
                TLRPC.User user = MessagesController.getInstance(messageObject.currentAccount).getUser(messageObject.replyMessageObject.messageOwner.from_id.user_id);
                if (user != null) {
                    return UserObject.getEmojiId(user);
                }
            } else if (messageObject.replyMessageObject.isFromChannel()) {
                TLRPC.Chat chat = MessagesController.getInstance(messageObject.currentAccount).getChat(messageObject.replyMessageObject.messageOwner.from_id.channel_id);
                if (chat != null) {
                    return ChatObject.getEmojiId(chat);
                }
            }
        }
        return 0;
    }

    public static TLRPC.InputStickerSet getSetFrom(MessageObject messageObject, TLRPC.User currentUser) {
        return AnimatedEmojiDrawable.findStickerSet(UserConfig.selectedAccount, getEmojiIdFrom(messageObject, currentUser));
    }

    public static TLRPC.InputStickerSet getSetFrom(TLRPC.User user) {
        return AnimatedEmojiDrawable.findStickerSet(UserConfig.selectedAccount, UserObject.getProfileEmojiId(user));
    }

    public static TLRPC.InputStickerSet getSetFrom(TLRPC.Chat chat) {
        return AnimatedEmojiDrawable.findStickerSet(UserConfig.selectedAccount, ChatObject.getProfileEmojiId(chat));
    }

    public static final DispatchQueue utilsQueue = new DispatchQueue("utilsQueue");

    public static void uploadImage(File file, Utilities.Callback<String> callback) {
        utilsQueue.postRunnable(() -> {
            try {
                URL url = new URL("https://telegra.ph/upload");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");

                try (OutputStream os = connection.getOutputStream()) {
                    writeFormField(os, file.getName(), new FileInputStream(file));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = connection.getInputStream()) {
                        callback.run("https://telegra.ph" + extractUrlFromResponse(is));
                    }
                } else {
                    callback.run(null);
                }
            } catch (Exception e) {
                callback.run(null);
            }
        });
    }

    private static void writeFormField(OutputStream os, String fileName, InputStream inputStream) throws IOException {
        os.write(("--*****\r\n").getBytes());
        os.write(("Content-Disposition: form-data; name=file; filename=\"" + fileName + "\"\r\n").getBytes());
        os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }

        os.write(("\r\n--*****--\r\n").getBytes());
    }

    private static String extractUrlFromResponse(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JsonArray jsonArray = JsonParser.parseString(response.toString()).getAsJsonArray();

            if (!jsonArray.isEmpty()) {
                JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
                return jsonObject.get("src").getAsString();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

}
