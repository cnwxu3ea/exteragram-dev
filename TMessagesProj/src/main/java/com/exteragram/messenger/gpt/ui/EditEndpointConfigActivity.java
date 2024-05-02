/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.gpt.ui;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.exteragram.messenger.gpt.core.Client;
import com.exteragram.messenger.gpt.core.Config;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.OutlineTextContainerView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.util.ArrayList;

public class EditEndpointConfigActivity extends BaseFragment {

    private ClipboardManager clipboardManager;
    private final ClipboardManager.OnPrimaryClipChangedListener clipChangedListener = this::updateButtons;
    private String pasteString;

    private LinearLayout buttons;
    private TextView clearView;
    private TextView pasteView;
    private View separator;

    private EditTextBoldCursor urlField;
    private OutlineTextContainerView urlFieldContainer;

    private EditTextBoldCursor modelField;
    private OutlineTextContainerView modelFieldContainer;

    private EditTextBoldCursor keyField;
    private OutlineTextContainerView keyFieldContainer;

    private View doneButton;
    private TextView helpTextView;

    private final static int done_button = 1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.EndpointSetup));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    saveConfig();
                }
            }
        });

        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        ActionBarMenu menu = actionBar.createMenu();
        doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_ab_done, AndroidUtilities.dp(56));
        doneButton.setContentDescription(LocaleController.getString("Done", R.string.Done));

        fragmentView = new LinearLayout(context);
        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        fragmentView.setOnTouchListener((v, event) -> true);

        InputFilter[] urlFilters = new InputFilter[1];
        urlFilters[0] = new InputFilter.LengthFilter(100); // todo: maybe check somehow
        var urlFieldPair = createField(context, LocaleController.getString(R.string.ApiUrl), urlFilters);
        urlField = urlFieldPair.first;
        urlField.setOnEditorActionListener((textView, i, keyEvent) -> {
            updateHelpText();
            if (i == EditorInfo.IME_ACTION_NEXT || i == EditorInfo.IME_ACTION_DONE) {
                modelField.requestFocus();
                AndroidUtilities.showKeyboard(keyField);
                return true;
            }
            return false;
        });
        urlFieldContainer = urlFieldPair.second;
        linearLayout.addView(urlFieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 24, 24, 24, 0));

        InputFilter[] modelFieldFilters = new InputFilter[1];
        modelFieldFilters[0] = new InputFilter.LengthFilter(32);
        var modelFieldPair = createField(context, LocaleController.getString(R.string.Model), modelFieldFilters);
        modelField = modelFieldPair.first;
        modelField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT || i == EditorInfo.IME_ACTION_DONE) {
                keyField.requestFocus();
                AndroidUtilities.showKeyboard(keyField);
                return true;
            }
            return false;
        });
        modelFieldContainer = modelFieldPair.second;
        linearLayout.addView(modelFieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 24, 24, 24, 0));

        InputFilter[] keyFieldFilters = new InputFilter[1];
        keyFieldFilters[0] = new ApiKeyInputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source != null && source.length() > 0 && TextUtils.indexOf(source, '\n') == source.length() - 1) {
                    doneButton.performClick();
                    return "";
                }
                CharSequence result = super.filter(source, start, end, dest, dstart, dend);
                if (result != null && source != null && result.length() != source.length()) {
                    keyFieldContainer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    AndroidUtilities.shakeView(urlFieldContainer);
                    AndroidUtilities.shakeView(modelFieldContainer);
                    AndroidUtilities.shakeView(keyFieldContainer);
                }
                return result;
            }
        };
        var keyFieldPair = createField(context, LocaleController.getString(R.string.ApiKey), keyFieldFilters);
        keyField = keyFieldPair.first;
        keyField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                doneButton.performClick();
                return true;
            }
            return false;
        });
        keyFieldContainer = keyFieldPair.second;
        linearLayout.addView(keyFieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 24, 24, 24, 0));

        buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        pasteView = new TextView(context);
        ScaleStateListAnimator.apply(pasteView, 0.02f, 1.5f);
        pasteView.setGravity(Gravity.CENTER_HORIZONTAL);
        pasteView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        pasteView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append("..").setSpan(new ColoredImageSpan(ContextCompat.getDrawable(context, R.drawable.msg_copy_filled)), 0, 1, 0);
        spannableStringBuilder.setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(6)), 1, 2, 0);
        spannableStringBuilder.append(LocaleController.getString(android.R.string.paste));
        spannableStringBuilder.append(".").setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(3)), spannableStringBuilder.length() - 1, spannableStringBuilder.length(), 0);
        pasteView.setText(spannableStringBuilder);
        pasteView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        pasteView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        pasteView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        pasteView.setSingleLine(true);
        pasteView.setOnClickListener(v -> {
            if (TextUtils.isEmpty(pasteString)) {
                return;
            }
            keyField.setText(pasteString);
            keyField.setSelection(pasteString.length());
        });
        buttons.addView(pasteView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f));

        separator = new View(context);
        buttons.addView(separator, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 0.06f));

        clearView = new TextView(context);
        ScaleStateListAnimator.apply(clearView, 0.02f, 1.5f);
        clearView.setGravity(Gravity.CENTER_HORIZONTAL);
        clearView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        clearView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append("..").setSpan(new ColoredImageSpan(ContextCompat.getDrawable(context, R.drawable.msg_delete_filled)), 0, 1, 0);
        spannableStringBuilder.setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(6)), 1, 2, 0);
        spannableStringBuilder.append(LocaleController.getString("Clear", R.string.Clear));
        spannableStringBuilder.append(".").setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(3)), spannableStringBuilder.length() - 1, spannableStringBuilder.length(), 0);
        clearView.setText(spannableStringBuilder);
        clearView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        clearView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        clearView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        clearView.setSingleLine(true);
        clearView.setOnClickListener(v -> {
            Config.setApiKey(null);
            finishFragment();
        });
        buttons.addView(clearView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f));

        linearLayout.addView(buttons, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 24, 18, 24, 0));

        helpTextView = new LinkSpanDrawable.LinksTextView(context);
        helpTextView.setFocusable(true);
        helpTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        helpTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText8));
        helpTextView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        helpTextView.setHighlightColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkSelection));
        helpTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);

        linearLayout.addView(helpTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 24, 14, 24, 0));

        urlField.setText(!TextUtils.isEmpty(Config.getUrl()) ? Config.getUrl() : "https://api.openai.com/v1");
        modelField.setText(!TextUtils.isEmpty(Config.getModel()) ? Config.getModel() : "gpt-3.5-turbo");
        keyField.setText(!TextUtils.isEmpty(Config.getApiKey()) ? Config.getApiKey() : "sk-");
        keyField.setSelection(keyField.length());

        updateHelpText();
        updateButtons();

        return fragmentView;
    }

    private Pair<EditTextBoldCursor, OutlineTextContainerView> createField(Context context, String text, InputFilter[] inputFilters) {
        var fieldContainer = new OutlineTextContainerView(context);
        fieldContainer.setText(text);

        var field = new EditTextBoldCursor(context);
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setBackground(null);
        field.setMaxLines(1);
        field.setLines(1);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.setImeOptions(EditorInfo.IME_ACTION_DONE);
        field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated));
        field.setCursorWidth(1.5f);
        field.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        field.setOnFocusChangeListener((v, hasFocus) -> {
            fieldContainer.animateSelection(hasFocus ? 1 : 0);
            if (!hasFocus) {
                updateHelpText();
            }
        });
        int padding = AndroidUtilities.dp(16);
        field.setPadding(0, padding, 0, padding);
        field.setCursorSize(AndroidUtilities.dp(20));
        fieldContainer.addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 16, 0));
        fieldContainer.attachEditText(field);

        field.setFilters(inputFilters);
        field.setMinHeight(AndroidUtilities.dp(36));

        fieldContainer.setText(text);

        return new Pair<>(field, fieldContainer);
    }

    private void updateHelpText() {
        String currentUrl = String.valueOf(urlField.getText());
        String currentUrlClean = currentUrl
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "");
        if (currentUrlClean.contains("/")) {
            currentUrlClean = currentUrlClean.substring(0, currentUrlClean.indexOf("/"));
        }

        String model = String.valueOf(modelField.getText());

        if (TextUtils.isEmpty(currentUrlClean) || TextUtils.isEmpty(model)) {
            helpTextView.setText("");
            return;
        }

        SpannableStringBuilder text = AndroidUtilities.replaceTags(LocaleController.formatString(R.string.ApiKeyInfo, currentUrlClean, model));

        if (currentUrl.contains("openai.com")) {
            var s = LocaleController.getString(R.string.ApiKeyInfoOpenAI);
            SpannableStringBuilder spanned = new SpannableStringBuilder(s);
            int index1 = s.indexOf('*');
            int index2 = s.indexOf('*', index1 + 1);

            if (index1 != -1 && index2 != -1 && index1 != index2) {
                helpTextView.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
                spanned.replace(index2, index2 + 1, "");
                spanned.replace(index1, index1 + 1, "");
                spanned.setSpan(new URLSpanNoUnderline("https://platform.openai.com/account/api-keys"), index1, index2 - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            text.append("\n").append(spanned);
        }

        helpTextView.setText(text);
    }

    @Override
    public void onPause() {
        super.onPause();
        clipboardManager.removePrimaryClipChangedListener(clipChangedListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        boolean animations = preferences.getBoolean("view_animations", true);
        if (!animations) {
            keyField.requestFocus();
            AndroidUtilities.showKeyboard(keyField);
        }
        clipboardManager.addPrimaryClipChangedListener(clipChangedListener);
        updateButtons();
    }

    private void saveConfig() {
        String currentUrl = Config.getUrl();
        //noinspection ConstantValue
        if (currentUrl == null) {
            currentUrl = "";
        }
        String currentModel = Config.getModel();
        //noinspection ConstantValue
        if (currentModel == null) {
            currentModel = "";
        }
        String currentKey = Config.getApiKey();
        if (currentKey == null) {
            currentKey = "";
        }
        final String newUrl = urlField.getText().toString().replace("\n", "");
        final String newModel = modelField.getText().toString().replace("\n", "");
        final String newKey = keyField.getText().toString().replace("\n", "");
        if (!currentUrl.equals(newUrl) || !currentModel.equals(newModel) || !currentKey.equals(newKey)) {
            Client client = new Client(this);
            final AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progressDialog.setOnCancelListener(dialog -> client.stop());
            progressDialog.show();
            client.setUrlOverride(newUrl);
            client.setModelOverride(newModel);
            client.setKeyOverride(newKey);
            client.getResponse("Say 'hi'.", false, false, res -> {
                if (progressDialog.isShowing()) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (!TextUtils.isEmpty(res)) {
                    Config.setUrl(newUrl);
                    Config.setModel(newModel);
                    Config.setApiKey(newKey);
                    finishFragment();
                } else {
                    keyFieldContainer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    AndroidUtilities.shakeView(urlFieldContainer);
                    AndroidUtilities.shakeView(modelFieldContainer);
                    AndroidUtilities.shakeView(keyFieldContainer);
                }
            });
        } else {
            finishFragment();
        }
    }

    private void updateButtons() {
        final ClipData clip = clipboardManager.getPrimaryClip();

        String clipText;
        if (clip != null && clip.getItemCount() > 0) {
            try {
                clipText = clip.getItemAt(0).coerceToText(fragmentView.getContext()).toString();
            } catch (Exception e) {
                clipText = null;
            }
        } else {
            clipText = null;
        }

        final String key = keyField.getText().toString().replace("\n", "");

        if (TextUtils.isEmpty(Config.getApiKey())) {
            clearView.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(clipText) && clipText.startsWith("sk-") && !key.equals(pasteString)) {
            buttons.setVisibility(View.VISIBLE);
            pasteView.setVisibility(View.VISIBLE);
            if (clearView.getVisibility() == View.VISIBLE) {
                separator.setVisibility(View.VISIBLE);
            }
            pasteString = clipText;
        } else {
            pasteView.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);
            if (clearView.getVisibility() == View.GONE) {
                buttons.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            keyField.requestFocus();
            AndroidUtilities.showKeyboard(keyField);
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(urlField, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(urlField, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        themeDescriptions.add(new ThemeDescription(urlField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(urlField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));

        themeDescriptions.add(new ThemeDescription(keyField, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(keyField, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        themeDescriptions.add(new ThemeDescription(keyField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(keyField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));

        themeDescriptions.add(new ThemeDescription(helpTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText8));

        return themeDescriptions;
    }
}
