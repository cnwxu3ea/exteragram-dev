package com.exteragram.messenger.components;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.exteragram.messenger.utils.SystemUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.PopupSwipeBackLayout;

public class SearchPhotoPopupWrapper {

    public static final String YANDEX_SEARCH_URL = "https://yandex.com/images/search?rpt=imageview&url=";
    private static final String GOOGLE_SEARCH_URL = "https://www.google.com/searchbyimage?client=app&image_url=";
    private static final String BING_SEARCH_URL = "https://www.bing.com/images/search?view=detailv2&iss=SBI&form=SBIIDP&sbisrc=UrlPaste&q=imgurl:";
    private static final String TINEYE_SEARCH_URL = "https://tineye.com/search/?url=";

    public ActionBarPopupWindow.ActionBarPopupWindowLayout searchSwipeBackLayout;
    ActionBarMenuSubItem lensItem;
    Callback callback;

    public SearchPhotoPopupWrapper(Context context, PopupSwipeBackLayout swipeBackLayout, Callback callback) {
        this.callback = callback;

        searchSwipeBackLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, null);
        searchSwipeBackLayout.setFitItems(true);

        ActionBarMenuSubItem backItem = ActionBarMenuItem.addItem(searchSwipeBackLayout, R.drawable.msg_arrow_back, LocaleController.getString("Back", R.string.Back), false, null);
        backItem.setOnClickListener(view -> swipeBackLayout.closeForeground());
        backItem.setColors(0xfffafafa, 0xfffafafa);
        backItem.setSelectorColor(0x0fffffff);

        FrameLayout gap = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        gap.setMinimumWidth(AndroidUtilities.dp(196));
        gap.setBackgroundColor(0xff181818);
        searchSwipeBackLayout.addView(gap);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) gap.getLayoutParams();
        if (LocaleController.isRTL) {
            layoutParams.gravity = Gravity.RIGHT;
        }
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(8);
        gap.setLayoutParams(layoutParams);

        ActionBarMenuSubItem item = ActionBarMenuItem.addItem(searchSwipeBackLayout, 0, "Yandex", false, null);
        item.setColors(0xfffafafa, 0xfffafafa);
        item.setOnClickListener(v -> callback.onSelected(YANDEX_SEARCH_URL));
        item.setSelectorColor(0x0fffffff);

        item = ActionBarMenuItem.addItem(searchSwipeBackLayout, 0, "Google", false, null);
        item.setColors(0xfffafafa, 0xfffafafa);
        item.setOnClickListener(v -> callback.onSelected(GOOGLE_SEARCH_URL));
        item.setSelectorColor(0x0fffffff);

        item = ActionBarMenuItem.addItem(searchSwipeBackLayout, 0, "Bing", false, null);
        item.setColors(0xfffafafa, 0xfffafafa);
        item.setOnClickListener(v -> callback.onSelected(BING_SEARCH_URL));
        item.setSelectorColor(0x0fffffff);

        item = ActionBarMenuItem.addItem(searchSwipeBackLayout, 0, "TinEye", false, null);
        item.setColors(0xfffafafa, 0xfffafafa);
        item.setOnClickListener(v -> callback.onSelected(TINEYE_SEARCH_URL));
        item.setSelectorColor(0x0fffffff);

        if (SystemUtils.isLensSupported()) {
            lensItem = item = ActionBarMenuItem.addItem(searchSwipeBackLayout, 0, "Google Lens", false, null);
            item.setRightIcon(R.drawable.msg_mini_lock3);
            item.getRightIcon().setAlpha(.0f);
            item.setColors(0xfffafafa, 0xfffafafa);
            item.setOnClickListener(v -> callback.onLensSelected());
            item.setSelectorColor(0x0fffffff);
            if (!SystemUtils.isLensInstalled()) {
                item.getRightIcon().setAlpha(.4f);
                item.setOnClickListener(v -> callback.onLensNotInstalled());
            }
        }

        gap = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        gap.setMinimumWidth(AndroidUtilities.dp(196));
        gap.setBackgroundColor(0xff181818);
        searchSwipeBackLayout.addView(gap);
        gap.setLayoutParams(layoutParams);

        TextView textView = new LinkSpanDrawable.LinksTextView(context);
        textView.setTag(R.id.fit_width_tag, 1);
        textView.setPadding(AndroidUtilities.dp(13), 0, AndroidUtilities.dp(13), AndroidUtilities.dp(8));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView.setText(LocaleController.getString(R.string.SearchPhotoInfo));
        searchSwipeBackLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8, 0, 0));
    }

    public void update() {
        if (SystemUtils.isLensSupported()) {
            if (lensItem != null) {
                lensItem.getRightIcon().setAlpha(.0f);
                lensItem.setOnClickListener(v -> callback.onLensSelected());
                if (!SystemUtils.isLensInstalled()) {
                    lensItem.getRightIcon().setAlpha(.4f);
                    lensItem.setOnClickListener(v -> callback.onLensNotInstalled());
                }
            }
        }
    }

    public interface Callback {
        void onSelected(String url);
        void onLensSelected();
        void onLensNotInstalled();
    }
}