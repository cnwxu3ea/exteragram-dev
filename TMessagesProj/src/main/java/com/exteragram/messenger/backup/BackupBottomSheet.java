/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.backup;

import android.app.Activity;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.StickerImageView;

public class BackupBottomSheet extends BottomSheet {

    private final int difference;
    private final BaseFragment fragment;

    public BackupBottomSheet(BaseFragment fragment, MessageObject messageObject) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
        this.fragment = fragment;

        Activity activity = fragment.getParentActivity();
        difference = PreferencesUtils.getInstance().getDiff(messageObject);

        fixNavigationBar();

        FrameLayout frameLayout = new FrameLayout(activity);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        frameLayout.addView(linearLayout);

        StickerImageView imageView = new StickerImageView(activity, currentAccount);
        imageView.setStickerPackName("exteraGramPlaceholders");
        imageView.setStickerNum(6);
        imageView.getImageReceiver().setAutoRepeat(1);
        imageView.getImageReceiver().setAutoRepeatCount(1);
        linearLayout.addView(imageView, LayoutHelper.createLinear(144, 144, Gravity.CENTER_HORIZONTAL, 0, 16, 0, 0));

        TextView title = new TextView(activity);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setText(LocaleController.getString(R.string.ImportTitle));
        linearLayout.addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 40, 20, 40, 0));

        TextView description = new TextView(activity);
        description.setGravity(Gravity.CENTER_HORIZONTAL);
        description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        description.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        description.setText(AndroidUtilities.replaceTags(LocaleController.formatPluralString("ImportChanges", difference)));
        linearLayout.addView(description, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 15, 21, 8));

        TextView buttonTextView = new TextView(activity);
        ScaleStateListAnimator.apply(buttonTextView, 0.02f, 1.5f);
        buttonTextView.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        buttonTextView.setGravity(Gravity.CENTER);
        buttonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        buttonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        buttonTextView.setText(LocaleController.getString(R.string.ImportConfirm));
        buttonTextView.setOnClickListener(view -> {
            dismiss();
            PreferencesUtils.getInstance().importSettings(messageObject, fragment.getParentActivity(), fragment.getParentLayout());
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, LocaleController.getString("SettingsImported", R.string.SettingsImported)).show();
        });
        buttonTextView.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        buttonTextView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), getThemedColor(Theme.key_featuredStickers_addButton), ColorUtils.setAlphaComponent(getThemedColor(Theme.key_windowBackgroundWhite), 120)));
        linearLayout.addView(buttonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 16, 15, 16, 8));

        TextView textView = new TextView(activity);
        ScaleStateListAnimator.apply(textView, 0.02f, 1.5f);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setText(LocaleController.getString(R.string.CancelConfirm));
        textView.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
        textView.setOnClickListener(view -> dismiss());
        linearLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 16, 0, 16, 0));

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(frameLayout);
        setCustomView(scrollView);
    }

    public void showIfPossible() {
        if (difference > 0) {
            show();
        } else {
            AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.SameSettings)).show());
        }
    }
}