package com.exteragram.messenger.gpt.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.exteragram.messenger.gpt.core.Role;
import com.exteragram.messenger.gpt.core.RoleList;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CodepointsLengthInputFilter;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.OutlineTextContainerView;

public class EditRoleActivity extends BaseFragment {

    private EditTextBoldCursor nameField;
    private OutlineTextContainerView nameFieldContainer;

    private EditTextBoldCursor promptField;
    private OutlineTextContainerView promptFieldContainer;
    private TextView helpTextView;

    private View doneButton;

    private final static int done_button = 1;

    private final static int NAME_LIMIT = 50;
    private final static int PROMPT_LIMIT = 600;

    private static Role currentRole;

    public EditRoleActivity(Role role) {
        currentRole = role;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(currentRole != null ? R.string.EditRole : R.string.NewRole));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    saveName();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_ab_done, AndroidUtilities.dp(56));
        doneButton.setContentDescription(LocaleController.getString("Done", R.string.Done));

        fragmentView = new LinearLayout(context);
        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        fragmentView.setOnTouchListener((v, event) -> true);

        nameFieldContainer = new OutlineTextContainerView(context);
        linearLayout.addView(nameFieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 24, 24, 24, 0));

        nameField = new EditTextBoldCursor(context);
        nameField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameField.setBackground(null);
        nameField.setSingleLine(true);
        nameField.setMaxLines(1);
        nameField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        nameField.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        nameField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        nameField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated));
        nameField.setCursorWidth(1.5f);
        nameField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        nameField.setOnFocusChangeListener((v, hasFocus) -> nameFieldContainer.animateSelection(hasFocus ? 1 : 0));
        int padding = AndroidUtilities.dp(16);
        nameField.setPadding(padding, padding, padding, padding);
        nameField.setCursorSize(AndroidUtilities.dp(20));
        nameFieldContainer.addView(nameField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        nameFieldContainer.attachEditText(nameField);
        nameField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT || i == EditorInfo.IME_ACTION_DONE) {
                doneButton.performClick();
                return true;
            }
            return false;
        });

        InputFilter[] inputFilters = new InputFilter[1];
        inputFilters[0] = new CodepointsLengthInputFilter(NAME_LIMIT) {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                CharSequence result = super.filter(source, start, end, dest, dstart, dend);
                if (result != null && source != null && result.length() != source.length()) {
                    nameFieldContainer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    AndroidUtilities.shakeView(nameFieldContainer);
                }
                return result;
            }
        };
        nameField.setFilters(inputFilters);
        nameField.setMinHeight(AndroidUtilities.dp(36));
        nameField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                doneButton.performClick();
                return true;
            }
            return false;
        });
        nameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                nameFieldContainer.setText(String.format("%s • %d", LocaleController.getString(R.string.RoleName), NAME_LIMIT - Character.codePointCount(s, 0, s.length())));
            }
        });

        nameFieldContainer.setText(String.format("%s • %d", LocaleController.getString(R.string.RoleName), NAME_LIMIT));


        promptFieldContainer = new OutlineTextContainerView(context);
        linearLayout.addView(promptFieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 24, 24, 24, 0));

        promptField = new EditTextBoldCursor(context);
        promptField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        promptField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        promptField.setBackground(null);
        //promptField.setMaxLines(4);
        promptField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        promptField.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        promptField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        promptField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated));
        promptField.setCursorWidth(1.5f);
        promptField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        promptField.setOnFocusChangeListener((v, hasFocus) -> promptFieldContainer.animateSelection(hasFocus ? 1 : 0));
        promptField.setPadding(padding, padding, padding, padding);
        promptField.setCursorSize(AndroidUtilities.dp(20));
        promptFieldContainer.addView(promptField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        promptFieldContainer.attachEditText(promptField);
        promptField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT || i == EditorInfo.IME_ACTION_DONE) {
                doneButton.performClick();
                return true;
            }
            return false;
        });

        inputFilters = new InputFilter[1];
        inputFilters[0] = new CodepointsLengthInputFilter(PROMPT_LIMIT) {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                CharSequence result = super.filter(source, start, end, dest, dstart, dend);
                if (result != null && source != null && result.length() != source.length()) {
                    promptFieldContainer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    AndroidUtilities.shakeView(promptFieldContainer);
                }
                return result;
            }
        };
        promptField.setFilters(inputFilters);
        promptField.setMinHeight(AndroidUtilities.dp(36));
        promptField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                doneButton.performClick();
                return true;
            }
            return false;
        });
        promptField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                promptFieldContainer.setText(String.format("%s • %d", LocaleController.getString(R.string.RolePrompt), PROMPT_LIMIT - Character.codePointCount(s, 0, s.length())));
            }
        });

        promptFieldContainer.setText(String.format("%s • %d", LocaleController.getString(R.string.RolePrompt), PROMPT_LIMIT));

        helpTextView = new LinkSpanDrawable.LinksTextView(context);
        helpTextView.setFocusable(true);
        helpTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        helpTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText8));
        helpTextView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        helpTextView.setHighlightColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkSelection));
        helpTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        helpTextView.setText(LocaleController.getString(R.string.PromptInfo));
        linearLayout.addView(helpTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 24, 12, 24, 0));

        if (currentRole != null) {
            nameField.setText(currentRole.getName());
            nameField.setSelection(currentRole.getName().length());
            promptField.setText(currentRole.getPrompt());
        } else {
            nameField.setText("");
            promptField.setText("");
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        boolean animations = preferences.getBoolean("view_animations", true);
        if (!animations) {
            nameField.requestFocus();
            AndroidUtilities.showKeyboard(nameField);
        }
    }

    @Override
    public void onFragmentDestroy() {
        currentRole = null;
        super.onFragmentDestroy();
    }

    private void saveName() {
        RoleList roleList = new RoleList();
        roleList.fill();
        Role role = new Role(nameField.getText().toString(), promptField.getText().toString());

        boolean nameIsEmpty = TextUtils.isEmpty(role.getName());
        boolean promptIsEmpty = TextUtils.isEmpty(role.getPrompt());

        if (nameIsEmpty || promptIsEmpty) {
            nameFieldContainer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            if (nameIsEmpty) {
                AndroidUtilities.shakeView(nameFieldContainer);
            }
            if (promptIsEmpty) {
                AndroidUtilities.shakeView(promptFieldContainer);
            }
            return;
        }

        if (currentRole != null) {
            roleList.edit(currentRole, role);
        } else {
            roleList.add(role);
        }
        getNotificationCenter().postNotificationName(NotificationCenter.rolesUpdated);
        finishFragment();
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            nameField.requestFocus();
            AndroidUtilities.showKeyboard(nameField);
        }
    }
}
