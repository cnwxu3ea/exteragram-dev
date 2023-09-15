/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.gpt.core;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.TextView;

import com.exteragram.messenger.gpt.ui.EditKeyActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private ExecutorService executor;
    private HttpURLConnection connection;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    public static final String SYSTEM_ROLE = "system";
    public static final String USER_ROLE = "user";

    private String testKey;

    private static final int STREAM_SYMBOLS_LIMIT = 10; // decreasing this limit can f*ck off optimization
    private int streamSymbolsCount = 0;
    private static int timeout = 10000;

    private final BaseFragment fragment;

    private boolean isGenerating;
    private Runnable onStartFinishRunnable;

    public interface ResponseCallback {
        void onResponse(String response);
    }

    private ArrayList<Message> conversationHistory = new ArrayList<>();
    private final RoleList roleList = new RoleList();

    public Client(BaseFragment fragment) {
        this.fragment = fragment;
    }

    public void getResponse(String prompt, ResponseCallback callback) {
        getResponse(prompt, Config.saveHistory, Config.responseStreaming, callback);
    }

    public void getResponse(String prompt, boolean saveHistory, boolean stream, ResponseCallback callback) {
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String response = "";

            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + (testKey != null ? testKey : Config.getApiKey()));
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(timeout);
                if (stream) {
                    connection.setReadTimeout(timeout);
                }

                JSONObject jsonObject = new JSONObject();
                JSONArray messagesArray = new JSONArray();

                if (testKey == null) {
                    roleList.fill();
                    Role role = roleList.getSelected();
                    if (role != null) {
                        JSONObject msg = new JSONObject();
                        msg.put("role", SYSTEM_ROLE);
                        msg.put("content", role.getPrompt());
                        messagesArray.put(msg);
                    }
                }

                if (saveHistory) {
                    conversationHistory = Config.getConversationHistory();
                    if (conversationHistory == null) {
                        conversationHistory = new ArrayList<>();
                    }

                    conversationHistory.add(
                            new Message(USER_ROLE, prompt)
                    );

                    for (Message message : conversationHistory) {
                        JSONObject msg = new JSONObject();
                        msg.put("role", message.getRole());
                        msg.put("content", message.getContent());
                        messagesArray.put(msg);
                    }
                } else {
                    JSONObject msg = new JSONObject();
                    msg.put("role", USER_ROLE);
                    msg.put("content", prompt);
                    messagesArray.put(msg);
                }

                jsonObject.put("messages", messagesArray);
                jsonObject.put("stream", stream);
                jsonObject.put("temperature", 0.4);
                //jsonObject.put("max_tokens", 4096);
                jsonObject.put("model", "gpt-3.5-turbo" + (Config.use16KModel ? "-16k" : ""));

                String jsonString = jsonObject.toString();

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
                }

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    isGenerating = true;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onStartFinishRunnable != null) {
                            onStartFinishRunnable.run();
                        }
                    });
                    StringBuilder res = new StringBuilder();
                    try {
                        InputStream inputStream = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        reader.lines()
                                .filter(line -> !TextUtils.isEmpty(line))
                                .forEach(line -> {
                                    if (stream) {
                                        if (line.startsWith("data: ")) {
                                            line = line.replace("data: ", "");
                                            if (line.equals("[DONE]")) {
                                                return;
                                            }
                                        }
                                        String content = getContent(line);
                                        if (!TextUtils.isEmpty(content)) {
                                            res.append(content);
                                            streamSymbolsCount += content.length();
                                            if (streamSymbolsCount >= STREAM_SYMBOLS_LIMIT) {
                                                AndroidUtilities.runOnUIThread(() -> {
                                                    if (callback != null) {
                                                        callback.onResponse(res.toString());
                                                    }
                                                }, 10);
                                                streamSymbolsCount = 0;
                                            }
                                        }
                                    } else {
                                        res.append(line);
                                    }
                                });
                    } catch (Exception e) {
                        FileLog.e(e);
                    }

                    response = res.toString();

                    if (!stream) {
                        response = getContent(response);
                    }

                    if (saveHistory) {
                        conversationHistory.add(
                                new Message(SYSTEM_ROLE, response)
                        );
                        Config.saveConversationHistory(conversationHistory);
                    }
                } else {
                    int errorCode = connection.getResponseCode();
                    String errorMessage = connection.getResponseMessage();

                    FileLog.e("GPT ERROR: " + errorMessage + " " + errorCode);

                    if (fragment != null && testKey == null) {
                        AndroidUtilities.runOnUIThread(() -> showErrorBulletin(errorCode, errorMessage.toLowerCase(Locale.ROOT)));
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            String finalResponse = response;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onResponse(finalResponse);
                }
                stop();
            }, 10);
        });
    }

    private void showErrorBulletin(int errorCode, String errorMessage) {
        int icon, title, subtitle;
        String button = null;
        Runnable onButtonClick = null;

        switch (errorCode) {
            case 400 -> {
                icon = 2;
                title = R.string.GPTError400;
                subtitle = R.string.GPTError400Info;
                button = LocaleController.getString("Clear", R.string.Clear);
                onButtonClick = this::clearHistory;
            }
            case 401 -> {
                icon = 3;
                title = R.string.GPTError401;
                subtitle = R.string.GPTError401Info;
                button = LocaleController.getString("Open", R.string.Open);
                onButtonClick = () -> fragment.presentFragment(new EditKeyActivity());
            }
            case 429 -> {
                if (errorMessage.equals("too many requests")) {
                    icon = 5;
                    title = R.string.GPTError429Limit;
                    subtitle = R.string.GPTError429LimitInfo;
                } else {
                    icon = 3;
                    title = R.string.GPTError429Quota;
                    subtitle = R.string.GPTError429QuotaInfo;
                    button = LocaleController.getString("Open", R.string.Open);
                    onButtonClick = () -> Browser.openUrl(fragment.getParentActivity(), "https://platform.openai.com/account/usage");
                }
            }
            case 503 -> {
                icon = 4;
                title = R.string.GPTError503;
                subtitle = R.string.GPTError503Info;
            }
            default -> {
                icon = 3;
                title = R.string.GPTError;
                subtitle = R.string.GPTErrorInfo;
            }
        }

        if (!TextUtils.isEmpty(button)) {
            BulletinFactory.of(fragment).createSimpleBulletin(LocaleController.getString(title), LocaleController.getString(subtitle), icon, button, onButtonClick).show();
        } else {
            BulletinFactory.of(fragment).createSimpleBulletin(LocaleController.getString(title), LocaleController.getString(subtitle), icon).show();
        }
    }

    public String getContent(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject(choice.has("delta") ? "delta" : "message");
                if (message.has("content")) {
                    return message.getString("content");
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }


    public void setTestKey(String key) {
        testKey = key;
    }

    public void setTimeout(int time) {
        timeout = time;
    }

    public void setOnStartFinishRunnable(Runnable onStartFinishRunnable) {
        this.onStartFinishRunnable = onStartFinishRunnable;
    }

    public boolean isGenerating() {
        return isGenerating;
    }

    public void clearHistory() {
        if (fragment == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setMessage(AndroidUtilities.replaceTags(LocaleController.getString(R.string.ClearConversationHistoryInfo)));
        builder.setTitle(LocaleController.getString("ClearHistory", R.string.ClearHistory));
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton), (dialog, which) -> {
            Config.clearConversationHistory();
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_delete, LocaleController.getString("HistoryCleared", R.string.HistoryCleared)).show();
        });
        AlertDialog dialog = builder.create();
        fragment.showDialog(dialog);
        TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    public void stop() {
        if (executor == null) {
            return;
        }

        executor.shutdownNow();
        connection.disconnect();

        isGenerating = false;

        if (testKey != null) {
            testKey = null;
        }

        if (onStartFinishRunnable != null) {
            onStartFinishRunnable.run();
        }
    }
}

