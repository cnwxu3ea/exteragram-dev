/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.gpt.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.exteragram.messenger.gpt.core.Config;
import com.exteragram.messenger.preferences.BasePreferencesActivity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.StickerImageView;
import org.telegram.ui.Components.TextStyleSpan;

public class SetupActivity extends BasePreferencesActivity {

    private View actionBarBackground;
    private AnimatorSet actionBarAnimator;

    private HeaderSettingsCell headerSettingsCell;

    private int headerRow;
    private int headerDividerRow;

    private int historyHeaderRow;
    private int keyRow;
    private int roleRow;
    private int saveHistoryRow;
    private int historyDividerRow;

    private int otherHeaderRow;
    private int use16KModelRow;
    private int responseStreamingRow;
    private int showResponseOnlyRow;
    private int otherDividerRow;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setBackground(null);
        actionBar.setTitleColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setItemsColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_listSelector), false);
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setOccupyStatusBar(!AndroidUtilities.isTablet());
        actionBar.setTitle(getTitle());
        actionBar.getTitleTextView().setAlpha(0.0f);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) actionBarBackground.getLayoutParams();
                layoutParams.height = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + AndroidUtilities.dp(3);

                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                checkScroll(false);
            }
        };
        fragmentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        fragmentView.setTag(Theme.key_windowBackgroundGray);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter = createAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener(this::onItemClick);
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll(true);
            }
        });

        actionBarBackground = new View(context) {

            private final Paint paint = new Paint();

            @Override
            protected void onDraw(Canvas canvas) {
                paint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                int h = getMeasuredHeight() - AndroidUtilities.dp(3);
                canvas.drawRect(0, 0, getMeasuredWidth(), h, paint);
                parentLayout.drawHeaderShadow(canvas, h);
            }
        };
        actionBarBackground.setAlpha(0.0f);
        frameLayout.addView(actionBarBackground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        frameLayout.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        updateRowsId();
        return fragmentView;
    }

    private final int[] location = new int[2];

    private void checkScroll(boolean animated) {
        int first = layoutManager.findFirstVisibleItemPosition();
        boolean show;
        if (first != 0) {
            show = true;
        } else {
            RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(first);
            if (holder == null) {
                show = true;
            } else {
                headerSettingsCell = (HeaderSettingsCell) holder.itemView;
                headerSettingsCell.backupImageView.getLocationOnScreen(location);
                show = location[1] + headerSettingsCell.backupImageView.getMeasuredHeight() < actionBar.getBottom();
            }
        }
        boolean visible = actionBarBackground.getTag() == null;
        if (show != visible) {
            actionBarBackground.setTag(show ? null : 1);
            if (actionBarAnimator != null) {
                actionBarAnimator.cancel();
                actionBarAnimator = null;
            }
            if (animated) {
                actionBarAnimator = new AnimatorSet();
                actionBarAnimator.playTogether(
                        ObjectAnimator.ofFloat(actionBarBackground, View.ALPHA, show ? 1.0f : 0.0f),
                        ObjectAnimator.ofFloat(actionBar.getTitleTextView(), View.ALPHA, show ? 1.0f : 0.0f)
                );
                actionBarAnimator.setDuration(250);
                actionBarAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (animation.equals(actionBarAnimator)) {
                            actionBarAnimator = null;
                        }
                    }
                });
                actionBarAnimator.start();
            } else {
                actionBarBackground.setAlpha(show ? 1.0f : 0.0f);
                actionBar.getTitleTextView().setAlpha(show ? 1.0f : 0.0f);
            }
        }
    }

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        headerRow = newRow();
        headerDividerRow = newRow();

        historyHeaderRow = newRow();
        keyRow = newRow();
        roleRow = newRow();
        saveHistoryRow = newRow();
        historyDividerRow = newRow();

        otherHeaderRow = newRow();
        responseStreamingRow = newRow();
        showResponseOnlyRow = newRow();
        use16KModelRow = newRow();
        otherDividerRow = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == keyRow) {
            presentFragment(new EditKeyActivity());
        } else if (position == roleRow) {
            presentFragment(new RolesSetupActivity());
        } else if (position == saveHistoryRow) {
            Config.editor.putBoolean("saveHistory", Config.saveHistory ^= true).apply();
            if (!Config.saveHistory) {
                Config.clearConversationHistory();
            }
            ((TextCell) view).setChecked(Config.saveHistory);
        } else if (position == use16KModelRow) {
            Config.editor.putBoolean("use16KModel", Config.use16KModel ^= true).apply();
            if (!Config.use16KModel) {
                Config.clearConversationHistory();
            }
            ((TextCheckCell) view).setChecked(Config.use16KModel);
        } else if (position == responseStreamingRow) {
            Config.editor.putBoolean("responseStreaming", Config.responseStreaming ^= true).apply();
            ((TextCheckCell) view).setChecked(Config.responseStreaming);
        } else if (position == showResponseOnlyRow) {
            Config.editor.putBoolean("showResponseOnly", Config.showResponseOnly ^= true).apply();
            ((TextCheckCell) view).setChecked(Config.showResponseOnly);
        }
    }

    @Override
    protected String getTitle() {
        return "ChatGPT";
    }

    @Override
    protected boolean hasWhiteActionBar() {
        return true;
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            if (type == 9) {
                headerSettingsCell = new HeaderSettingsCell(mContext);
                headerSettingsCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                return new RecyclerListView.Holder(headerSettingsCell);
            }
            return super.onCreateViewHolder(parent, type);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1 ->
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                case 2 -> {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == keyRow) {
                        String key = Config.getApiKey();
                        SpannableStringBuilder spannable;
                        if (!TextUtils.isEmpty(key) && key.length() > 16) {
                            spannable = SpannableStringBuilder.valueOf(key);
                            spannable.delete(8, spannable.length() - 6);
                            TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                            run.flags |= TextStyleSpan.FLAG_STYLE_SPOILER;
                            run.start = 6;
                            run.end = spannable.length() - 3;
                            spannable.setSpan(new TextStyleSpan(run), run.start, run.end, 0);
                        } else {
                            spannable = SpannableStringBuilder.valueOf(LocaleController.getString("BlockedEmpty", R.string.BlockedEmpty));
                        }
                        textCell.setPrioritizeTitleOverValue(true);
                        textCell.setTextAndSpoilersValueAndIcon(LocaleController.getString(R.string.ApiKey), spannable, R.drawable.msg_permissions, true);
                    } else if (position == roleRow) {
                        textCell.setPrioritizeTitleOverValue(true);
                        textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.Roles), Config.getSelectedRole(), R.drawable.msg_openprofile, true);
                    } else if (position == saveHistoryRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString(R.string.MessageHistory), Config.saveHistory, R.drawable.msg_discuss, false);
                    }
                }
                case 3 -> {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == historyHeaderRow) {
                        headerCell.setText(LocaleController.getString("General", R.string.General));
                    } else if (position == otherHeaderRow) {
                        headerCell.setText(LocaleController.getString("LocalOther", R.string.LocalOther));
                    }
                }
                case 5 -> {
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == use16KModelRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.Use16kModel), Config.use16KModel, false);
                    } else if (position == responseStreamingRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString(R.string.ResponseStreaming), LocaleController.getString(R.string.ResponseStreamingInfo), Config.responseStreaming, true, true);
                    } else if (position == showResponseOnlyRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.ShowResponseOnly), Config.showResponseOnly, true);
                    }
                }
                case 8 -> {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == historyDividerRow) {
                        cell.setText(LocaleController.getString(R.string.HistoryInfo));
                    } else if (position == otherDividerRow) {
                        cell.setText(LocaleController.getString(R.string.Use16kModelInfo));
                    }
                }
                case 9 -> {
                    headerSettingsCell = (HeaderSettingsCell) holder.itemView;
                    headerSettingsCell.setPadding(0, ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) - AndroidUtilities.dp(40), 0, 0);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerDividerRow) {
                return 1;
            } else if (position == historyHeaderRow || position == otherHeaderRow) {
                return 3;
            } else if (position == use16KModelRow || position == responseStreamingRow || position == showResponseOnlyRow) {
                return 5;
            } else if (position == historyDividerRow || position == otherDividerRow) {
                return 8;
            } else if (position == headerRow) {
                return 9;
            }
            return 2;
        }
    }

    public static class HeaderSettingsCell extends FrameLayout {

        public StickerImageView backupImageView;

        public HeaderSettingsCell(Context context) {
            super(context);

            LinearLayout stickerHeaderCell = new LinearLayout(context);
            stickerHeaderCell.setOrientation(LinearLayout.VERTICAL);

            backupImageView = new StickerImageView(context, UserConfig.selectedAccount);
            backupImageView.setStickerPackName("exteraGramPlaceholders");
            backupImageView.setStickerNum(0);
            backupImageView.getImageReceiver().setAutoRepeatCount(1);
            backupImageView.setOnClickListener(v -> backupImageView.getImageReceiver().startAnimation());
            stickerHeaderCell.addView(backupImageView, LayoutHelper.createLinear(130, 130, Gravity.CENTER_HORIZONTAL, 0, 27, 0, 30));

            addView(stickerHeaderCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER | Gravity.TOP));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }
    }
}