/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2022.

*/

package com.exteragram.messenger.preferences;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.components.StickerShapeCell;
import com.exteragram.messenger.components.StickerSizePreviewCell;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;

public class ChatsPreferencesActivity extends BasePreferencesActivity implements NotificationCenter.NotificationCenterDelegate {

    private ActionBarMenuItem resetItem;
    private StickerSizeCell stickerSizeCell;

    private final CharSequence[] suggestions = new CharSequence[]{
            LocaleController.getString("EmojiSuggestionTapReplace", R.string.EmojiSuggestionTapReplace),
            LocaleController.getString("EmojiSuggestionTapAfter", R.string.EmojiSuggestionTapAfter),
            LocaleController.getString("EmojiSuggestionTapAfterSpace", R.string.EmojiSuggestionTapAfterSpace)
    }, suggestionsValue = new CharSequence[]{
            LocaleController.getString("EmojiSuggestionTapReplaceShort", R.string.EmojiSuggestionTapReplaceShort),
            LocaleController.getString("EmojiSuggestionTapAfterShort", R.string.EmojiSuggestionTapAfterShort),
            LocaleController.getString("EmojiSuggestionTapAfterSpaceShort", R.string.EmojiSuggestionTapAfterSpaceShort)
    };

    private int stickerSizeHeaderRow;
    private int stickerSizeRow;

    private int stickerShapeHeaderRow;
    private int stickerShapeRow;
    private int stickerShapeDividerRow;

    private int stickersHeaderRow;
    private int hideStickerTimeRow;
    private int unlimitedRecentStickersRow;
    private int stickersAutoReorderRow;
    private int emojiSuggestionTapRow;
    private int stickersDividerRow;

    private int chatHeaderRow;
    private int addCommaAfterMentionRow;
    private int hideKeyboardOnScrollRow;
    private int hideShareButtonRow;
    private int hideMuteUnmuteButtonRow;
    private int disableReactionsRow;
    private int disableGreetingStickerRow;
    private int disableJumpToNextChannelRow;
    private int dateOfForwardedMsgRow;
    private int showMessageIDRow;
    private int showActionTimestampsRow;
    private int chatDividerRow;

    private int mediaHeaderRow;
    private int rearVideoMessagesRow;
    private int rememberLastUsedCameraRow;
    private int disableCameraRow;
    private int disableProximityEventsRow;
    private int pauseOnMinimizeRow;
    private int disablePlaybackRow;
    private int mediaDividerRow;

    private class StickerSizeCell extends FrameLayout {

        private final StickerSizePreviewCell messagesCell;
        private final SeekBarView sizeBar;
        private final int startStickerSize = 4;
        private final int endStickerSize = 20;

        private final TextPaint textPaint;
        private int lastWidth;

        public StickerSizeCell(Context context) {
            super(context);

            setWillNotDraw(false);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(16));

            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    sizeBar.getSeekBarAccessibilityDelegate().postAccessibilityEventRunnable(StickerSizeCell.this);
                    ExteraConfig.editor.putFloat("stickerSize", ExteraConfig.stickerSize = startStickerSize + (endStickerSize - startStickerSize) * progress).apply();
                    StickerSizeCell.this.invalidate();
                    if (resetItem.getVisibility() != VISIBLE) {
                        AndroidUtilities.updateViewVisibilityAnimated(resetItem, true, 0.5f, true);
                    }
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {

                }
            });
            sizeBar.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 5, 5, 43, 11));

            messagesCell = new StickerSizePreviewCell(context, parentLayout);
            messagesCell.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            addView(messagesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 53, 0, 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            canvas.drawText(String.valueOf(Math.round(ExteraConfig.stickerSize)), getMeasuredWidth() - AndroidUtilities.dp(39), AndroidUtilities.dp(28), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (lastWidth != width) {
                sizeBar.setProgress((ExteraConfig.stickerSize - startStickerSize) / (float) (endStickerSize - startStickerSize));
                lastWidth = width;
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            lastWidth = -1;
            messagesCell.invalidate();
            sizeBar.invalidate();
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityEvent(this, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityNodeInfoInternal(this, info);
        }

        @Override
        public boolean performAccessibilityAction(int action, Bundle arguments) {
            return super.performAccessibilityAction(action, arguments) || sizeBar.getSeekBarAccessibilityDelegate().performAccessibilityActionInternal(this, action, arguments);
        }
    }

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);

        ActionBarMenu menu = actionBar.createMenu();
        resetItem = menu.addItem(0, R.drawable.msg_reset);
        resetItem.setContentDescription(LocaleController.getString("Reset", R.string.Reset));
        resetItem.setVisibility(ExteraConfig.stickerSize != 14.0f ? View.VISIBLE : View.GONE);
        resetItem.setTag(null);
        resetItem.setOnClickListener(v -> {
            AndroidUtilities.updateViewVisibilityAnimated(resetItem, false, 0.5f, true);
            ValueAnimator animator = ValueAnimator.ofFloat(ExteraConfig.stickerSize, 14.0f);
            animator.setDuration(200);
            animator.addUpdateListener(valueAnimator -> {
                ExteraConfig.editor.putFloat("stickerSize", ExteraConfig.stickerSize = (Float) valueAnimator.getAnimatedValue()).apply();
                stickerSizeCell.invalidate();
            });
            animator.start();
        });

        return fragmentView;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
        return true;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.emojiLoaded) {
            if (getListView() != null) {
                getListView().invalidateViews();
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
    }

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        stickerSizeHeaderRow = newRow();
        stickerSizeRow = newRow();

        stickerShapeHeaderRow = newRow();
        stickerShapeRow = newRow();
        stickerShapeDividerRow = newRow();

        stickersHeaderRow = newRow();
        hideStickerTimeRow = newRow();
        unlimitedRecentStickersRow = newRow();
        stickersAutoReorderRow = newRow();
        emojiSuggestionTapRow = newRow();
        stickersDividerRow = newRow();

        chatHeaderRow = newRow();
        addCommaAfterMentionRow = newRow();
        hideMuteUnmuteButtonRow = newRow();
        hideShareButtonRow = newRow();
        hideKeyboardOnScrollRow = newRow();
        disableReactionsRow = newRow();
        disableGreetingStickerRow = newRow();
        disableJumpToNextChannelRow = newRow();
        dateOfForwardedMsgRow = newRow();
        showMessageIDRow = newRow();
        showActionTimestampsRow = newRow();
        chatDividerRow = newRow();

        mediaHeaderRow = newRow();
        disableCameraRow = newRow();
        disableProximityEventsRow = newRow();
        rearVideoMessagesRow = newRow();
        rememberLastUsedCameraRow = newRow();
        pauseOnMinimizeRow = newRow();
        disablePlaybackRow = newRow();
        mediaDividerRow = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == hideStickerTimeRow) {
            ExteraConfig.editor.putBoolean("hideStickerTime", ExteraConfig.hideStickerTime ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.hideStickerTime);
            stickerSizeCell.invalidate();
        } else if (position == unlimitedRecentStickersRow) {
            ExteraConfig.editor.putBoolean("unlimitedRecentStickers", ExteraConfig.unlimitedRecentStickers ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.unlimitedRecentStickers);
        } else if (position == stickersAutoReorderRow) {
            ExteraConfig.editor.putBoolean("stickersAutoReorder", ExteraConfig.stickersAutoReorder ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.stickersAutoReorder);
        } else if (position == addCommaAfterMentionRow) {
            ExteraConfig.editor.putBoolean("addCommaAfterMention", ExteraConfig.addCommaAfterMention ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.addCommaAfterMention);
        } else if (position == emojiSuggestionTapRow) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("EmojiSuggestionsTap", R.string.EmojiSuggestionsTap));

            LinearLayout linearLayout = new LinearLayout(getParentActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            builder.setView(linearLayout);

            for (int a = 0; a < suggestions.length; a++) {
                RadioColorCell cell = new RadioColorCell(getParentActivity());
                cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                cell.setTag(a);
                cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
                cell.setTextAndValue(suggestions[a], ExteraConfig.emojiSuggestionTap == a);
                cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
                linearLayout.addView(cell);
                cell.setOnClickListener(v -> {
                    Integer which = (Integer) v.getTag();
                    ExteraConfig.editor.putInt("emojiSuggestionTap", ExteraConfig.emojiSuggestionTap = which).apply();
                    ((TextSettingsCell) view).setTextAndValue(LocaleController.getString("EmojiSuggestions", R.string.EmojiSuggestions), suggestionsValue[which], true, true);
                    builder.getDismissRunnable().run();
                });
            }
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        } else if (position == hideKeyboardOnScrollRow) {
            ExteraConfig.editor.putBoolean("hideKeyboardOnScroll", ExteraConfig.hideKeyboardOnScroll ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.hideKeyboardOnScroll);
        } else if (position == hideShareButtonRow) {
            ExteraConfig.editor.putBoolean("hideShareButton", ExteraConfig.hideShareButton ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.hideShareButton);
        } else if (position == hideMuteUnmuteButtonRow) {
            ExteraConfig.editor.putBoolean("hideMuteUnmuteButton", ExteraConfig.hideMuteUnmuteButton ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.hideMuteUnmuteButton);
        } else if (position == disableReactionsRow) {
            ExteraConfig.editor.putBoolean("disableReactions", ExteraConfig.disableReactions ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disableReactions);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == disableGreetingStickerRow) {
            ExteraConfig.editor.putBoolean("disableGreetingSticker", ExteraConfig.disableGreetingSticker ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disableGreetingSticker);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == disableJumpToNextChannelRow) {
            ExteraConfig.editor.putBoolean("disableJumpToNextChannel", ExteraConfig.disableJumpToNextChannel ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disableJumpToNextChannel);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == dateOfForwardedMsgRow) {
            ExteraConfig.editor.putBoolean("dateOfForwardedMsg", ExteraConfig.dateOfForwardedMsg ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.dateOfForwardedMsg);
        } else if (position == showMessageIDRow) {
            ExteraConfig.editor.putBoolean("showMessageID", ExteraConfig.showMessageID ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.showMessageID);
        } else if (position == showActionTimestampsRow) {
            ExteraConfig.editor.putBoolean("showActionTimestamps", ExteraConfig.showActionTimestamps ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.showActionTimestamps);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == rearVideoMessagesRow) {
            ExteraConfig.editor.putBoolean("rearVideoMessages", ExteraConfig.rearVideoMessages ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.rearVideoMessages);
        } else if (position == rememberLastUsedCameraRow) {
            ExteraConfig.editor.putBoolean("rememberLastUsedCamera", ExteraConfig.rememberLastUsedCamera ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.rememberLastUsedCamera);
        } else if (position == disableCameraRow) {
            ExteraConfig.editor.putBoolean("disableCamera", ExteraConfig.disableCamera ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disableCamera);
        } else if (position == disableProximityEventsRow) {
            ExteraConfig.editor.putBoolean("disableProximityEvents", ExteraConfig.disableProximityEvents ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disableProximityEvents);
        } else if (position == pauseOnMinimizeRow) {
            ExteraConfig.editor.putBoolean("pauseOnMinimize", ExteraConfig.pauseOnMinimize ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.pauseOnMinimize);
        } else if (position == disablePlaybackRow) {
            ExteraConfig.editor.putBoolean("disablePlayback", ExteraConfig.disablePlayback ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disablePlayback);
            showBulletin();
        }
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString("SearchAllChatsShort", R.string.SearchAllChatsShort);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            switch (type) {
                case 10:
                    StickerShapeCell stickerShapeCell = new StickerShapeCell(mContext) {
                        @Override
                        protected void updateStickerPreview() {
                            parentLayout.rebuildAllFragmentViews(false, false);
                            stickerSizeCell.invalidate();
                        }
                    };
                    stickerShapeCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    stickerShapeCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    return new RecyclerListView.Holder(stickerShapeCell);
                case 11:
                    stickerSizeCell = new StickerSizeCell(mContext);
                    stickerSizeCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    stickerSizeCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    return new RecyclerListView.Holder(stickerSizeCell);
                default:
                    return super.onCreateViewHolder(parent, type);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == stickerSizeHeaderRow) {
                        headerCell.setText(LocaleController.getString("StickerSize", R.string.StickerSize));
                    } else if (position == stickersHeaderRow) {
                        headerCell.setText(LocaleController.getString("AccDescrStickers", R.string.AccDescrStickers) + " & " + LocaleController.getString("Emoji", R.string.Emoji));
                    } else if (position == chatHeaderRow) {
                        headerCell.setText(LocaleController.getString("SearchAllChatsShort", R.string.SearchAllChatsShort));
                    } else if (position == mediaHeaderRow) {
                        headerCell.setText(LocaleController.getString("MediaTab", R.string.MediaTab));
                    } else if (position == stickerShapeHeaderRow) {
                        headerCell.setText(LocaleController.getString("StickerShape", R.string.StickerShape));
                    }
                    break;
                case 5:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == hideStickerTimeRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("StickerTime", R.string.StickerTime), ExteraConfig.hideStickerTime, true);
                    } else if (position == unlimitedRecentStickersRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("UnlimitedRecentStickers", R.string.UnlimitedRecentStickers), ExteraConfig.unlimitedRecentStickers, true);
                    } else if (position == stickersAutoReorderRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("StickersAutoReorder", R.string.StickersAutoReorder), ExteraConfig.stickersAutoReorder, true);
                    } else if (position == addCommaAfterMentionRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString("AddCommaAfterMention", R.string.AddCommaAfterMention), LocaleController.getString("AddCommaAfterMentionValue", R.string.AddCommaAfterMentionValue), ExteraConfig.addCommaAfterMention, false, true);
                    } else if (position == hideKeyboardOnScrollRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("HideKeyboardOnScroll", R.string.HideKeyboardOnScroll), ExteraConfig.hideKeyboardOnScroll, true);
                    } else if (position == hideShareButtonRow) {
                        textCheckCell.setTextAndCheck(LocaleController.formatString("HideShareButton", R.string.HideShareButton, LocaleController.getString("ShareFile", R.string.ShareFile)), ExteraConfig.hideShareButton, true);
                    } else if (position == hideMuteUnmuteButtonRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.formatString("HideMuteUnmuteButton", R.string.HideMuteUnmuteButton, LocaleController.getString("ChannelMute", R.string.ChannelMute)), LocaleController.getString("HideMuteUnmuteButtonValue", R.string.HideMuteUnmuteButtonValue), ExteraConfig.hideMuteUnmuteButton, true, true);
                    } else if (position == disableReactionsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("DisableReactions", R.string.DisableReactions), ExteraConfig.disableReactions, true);
                    } else if (position == disableGreetingStickerRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("DisableGreetingSticker", R.string.DisableGreetingSticker), ExteraConfig.disableGreetingSticker, true);
                    } else if (position == disableJumpToNextChannelRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("DisableJumpToNextChannel", R.string.DisableJumpToNextChannel), ExteraConfig.disableJumpToNextChannel, true);
                    } else if (position == dateOfForwardedMsgRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("DateOfForwardedMsg", R.string.DateOfForwardedMsg), ExteraConfig.dateOfForwardedMsg, true);
                    } else if (position == showMessageIDRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("ShowMessageID", R.string.ShowMessageID), ExteraConfig.showMessageID, true);
                    } else if (position == showActionTimestampsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("ShowActionTimestamps", R.string.ShowActionTimestamps), ExteraConfig.showActionTimestamps, false);
                    } else if (position == rearVideoMessagesRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("RearVideoMessages", R.string.RearVideoMessages), ExteraConfig.rearVideoMessages, true);
                    } else if (position == rememberLastUsedCameraRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString("RememberLastUsedCamera", R.string.RememberLastUsedCamera), LocaleController.getString("RememberLastUsedCameraValue", R.string.RememberLastUsedCameraValue), ExteraConfig.rememberLastUsedCamera, true, true);
                    } else if (position == disableCameraRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("DisableCamera", R.string.DisableCamera), ExteraConfig.disableCamera, true);
                    } else if (position == disableProximityEventsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("DisableProximityEvents", R.string.DisableProximityEvents), ExteraConfig.disableProximityEvents, true);
                    } else if (position == pauseOnMinimizeRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString("PauseOnMinimize", R.string.PauseOnMinimize), LocaleController.getString("POMDescription", R.string.POMDescription), ExteraConfig.pauseOnMinimize, true, true);
                    } else if (position == disablePlaybackRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString("DisablePlayback", R.string.DisablePlayback), LocaleController.getString("DPDescription", R.string.DPDescription), ExteraConfig.disablePlayback, true, false);
                    }
                    break;
                case 7:
                    TextSettingsCell textSettingsCell = (TextSettingsCell) holder.itemView;
                    if (position == emojiSuggestionTapRow) {
                        textSettingsCell.setTextAndValue(LocaleController.getString("EmojiSuggestions", R.string.EmojiSuggestions), suggestionsValue[ExteraConfig.emojiSuggestionTap], false);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == stickersDividerRow || position == mediaDividerRow || position == stickerShapeDividerRow || position == chatDividerRow) {
                return 1;
            } else if (position == stickerSizeHeaderRow || position == stickersHeaderRow || position == chatHeaderRow || position == mediaHeaderRow || position == stickerShapeHeaderRow) {
                return 3;
            } else if (position == emojiSuggestionTapRow) {
                return 7;
            } else if (position == stickerShapeRow) {
                return 10;
            } else if (position == stickerSizeRow) {
                return 11;
            }
            return 5;
        }
    }
}