/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.components;

import android.annotation.SuppressLint;
import android.content.Context;
import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.utils.PopupUtils;
import com.exteragram.messenger.utils.TranslatorUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("ViewConstructor")
public class TranslateBeforeSendWrapper extends ActionBarMenuSubItem {

    public TranslateBeforeSendWrapper(Context context, boolean top, boolean bottom, Theme.ResourcesProvider resourcesProvider) {
        super(context, top, bottom, resourcesProvider);
        setTextAndIcon(LocaleController.getString("TranslateTo", R.string.TranslateTo), R.drawable.msg_translate);
        setSubtext(ExteraConfig.getCurrentLangName());
        setMinimumWidth(AndroidUtilities.dp(196));
        setItemHeight(56);
        setOnClickListener(v -> onClick());
        setOnLongClickListener(v -> showDialog(context));
        setRightIcon(R.drawable.msg_arrowright);
        getRightIcon().setOnClickListener(v -> showDialog(context));
    }

    protected void onClick() {
    }

    private boolean showDialog(Context context) {
        PopupUtils.showDialog(TranslatorUtils.getLanguageTitles(), LocaleController.getString("Language", R.string.Language), TranslatorUtils.getLanguageIndexByIso(ExteraConfig.targetLang), context, i -> {
            ExteraConfig.editor.putString("targetLang", ExteraConfig.targetLang = TranslatorUtils.getLangCodeByIndex(i)).apply();
            setSubtext(ExteraConfig.getCurrentLangName());
        });
        return true;
    }
}
