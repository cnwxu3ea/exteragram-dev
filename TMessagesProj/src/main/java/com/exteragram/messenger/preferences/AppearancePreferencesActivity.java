/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.preferences;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.preferences.components.AvatarCornersPreviewCell;
import com.exteragram.messenger.preferences.components.ChatListPreviewCell;
import com.exteragram.messenger.preferences.components.FabShapeCell;
import com.exteragram.messenger.preferences.components.FoldersPreviewCell;
import com.exteragram.messenger.preferences.components.SolarIconsPreview;
import com.exteragram.messenger.utils.AppUtils;
import com.exteragram.messenger.utils.ChatUtils;
import com.exteragram.messenger.utils.LocaleUtils;
import com.exteragram.messenger.utils.PopupUtils;
import com.exteragram.messenger.utils.SystemUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckCell2;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SlideChooseView;
import org.telegram.ui.LaunchActivity;

import java.util.Arrays;
import java.util.Locale;

public class AppearancePreferencesActivity extends BasePreferencesActivity {

    private Parcelable recyclerViewState = null;

    SolarIconsPreview solarIconsPreview;
    AvatarCornersPreviewCell avatarCornersPreviewCell;
    ChatListPreviewCell chatListPreviewCell;
    FoldersPreviewCell foldersPreviewCell;

    private final CharSequence[] styles = new CharSequence[]{
            LocaleController.getString("Default", R.string.Default),
            LocaleController.getString("TabStyleRounded", R.string.TabStyleRounded),
            LocaleController.getString("TabStyleTextOnly", R.string.TabStyleTextOnly),
            LocaleController.getString("TabStyleChips", R.string.TabStyleChips),
            LocaleController.getString("TabStylePills", R.string.TabStylePills),
    }, titles = new CharSequence[]{
            LocaleController.getString("exteraAppName", R.string.exteraAppName),
            LocaleController.getString("ActionBarTitleUsername", R.string.ActionBarTitleUsername),
            LocaleController.getString("ActionBarTitleName", R.string.ActionBarTitleName),
            LocaleController.getString("FilterChats", R.string.FilterChats)
    }, tabIcons = new CharSequence[]{
            LocaleController.getString("TabTitleStyleTextWithIcons", R.string.TabTitleStyleTextWithIcons),
            LocaleController.getString("TabTitleStyleTextOnly", R.string.TabTitleStyleTextOnly),
            LocaleController.getString("TabTitleStyleIconsOnly", R.string.TabTitleStyleIconsOnly)
    }, events = new CharSequence[]{
            LocaleController.getString("DependsOnTheDate", R.string.DependsOnTheDate),
            LocaleController.getString("Default", R.string.Default),
            LocaleController.getString("NewYear", R.string.NewYear),
            LocaleController.getString("ValentinesDay", R.string.ValentinesDay),
            LocaleController.getString("Halloween", R.string.Halloween)
    }, blurSmoothnessOptions = new String[]{
            LocaleController.getString("Default", R.string.Default),
            LocaleController.getString(R.string.BlurSmoothnessSmooth),
            LocaleController.getString(R.string.BlurSmoothnessSmoothest)
    }, tabletMode = new CharSequence[]{
            LocaleController.getString("DistanceUnitsAutomatic", R.string.DistanceUnitsAutomatic),
            LocaleController.getString("PasswordOn", R.string.PasswordOn),
            LocaleController.getString("PasswordOff", R.string.PasswordOff)
    };

    private int avatarCornersPreviewRow;
    private int singleCornerRadiusRow;
    private int avatarCornersDividerRow;

    private int foldersHeaderRow;
    private int foldersPreviewRow;
    private int hideAllChatsRow;
    private int tabCounterRow;
    private int tabTitleRow;
    private int tabStyleRow;
    private int foldersDividerRow;

    private int chatListHeaderRow;
    private int chatListPreviewRow;
    private int hideStoriesRow;
    private int hideActionBarStatusRow;
    private int centerTitleRow;
    private int actionBarTitleRow;
    private int chatListDividerRow;

    private int solarIconsHeaderRow;
    private int solarIconsPreviewRow;
    private int solarIconsRow;
    private int solarIconsInfoRow;

    private int appearanceHeaderRow;
    private int fabShapeRow;
    private int tabletModeRow;
    private int forceSnowRow;
    private int useSystemFontsRow;
    private int useSystemEmojiRow;
    private int newSwitchStyleRow;
    private int disableDividersRow;
    private int alternativeNavigationRow;
    private int appearanceDividerRow;

    private int blurOptionsHeaderRow;
    private int blurSmoothnessRow;
    private int blurElementsRow;
    private int blurActionBarRow;
    private int blurBottomBarRow;
    private int blurDialogsRow;
    private int forceBlurRow;
    private int blurSettingsDividerRow;

    private int drawerOptionsHeaderRow;
    private int eventChooserRow;
    private int alternativeOpenAnimationRow;
    private int drawerOptionsDividerRow;

    private int drawerHeaderRow;
    private int statusRow;
    private int myStoriesRow;
    private int menuBotsRow;
    private int newGroupRow;
    private int newSecretChatRow;
    private int newChannelRow;
    private int contactsRow;
    private int callsRow;
    private int peopleNearbyRow;
    private int archivedChatsRow;
    private int savedMessagesRow;
    private int scanQrRow;
    private int drawerDividerRow;

    private boolean blurElementsExpanded;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        avatarCornersPreviewRow = newRow();
        singleCornerRadiusRow = newRow();
        avatarCornersDividerRow = newRow();

        chatListHeaderRow = newRow();
        chatListPreviewRow = newRow();
        actionBarTitleRow = newRow();
        hideStoriesRow = newRow();
        hideActionBarStatusRow = getUserConfig().isPremium() ? newRow() : -1;
        centerTitleRow = newRow();
        chatListDividerRow = newRow();

        foldersHeaderRow = newRow();
        foldersPreviewRow = newRow();
        tabTitleRow = newRow();
        tabStyleRow = newRow();
        tabCounterRow = newRow();
        hideAllChatsRow = newRow();
        foldersDividerRow = newRow();

        solarIconsHeaderRow = newRow();
        solarIconsPreviewRow = newRow();
        solarIconsRow = newRow();
        solarIconsInfoRow = newRow();

        appearanceHeaderRow = newRow();
        fabShapeRow = newRow();
        tabletModeRow = newRow();
        useSystemFontsRow = newRow();
        useSystemEmojiRow = newRow();
        newSwitchStyleRow = newRow();
        disableDividersRow = newRow();
        forceSnowRow = newRow();
        alternativeNavigationRow = newRow();
        appearanceDividerRow = newRow();

        blurOptionsHeaderRow = newRow();
        blurSmoothnessRow = newRow();
        blurElementsRow = newRow();
        if (blurElementsExpanded) {
            blurActionBarRow = newRow();
            blurBottomBarRow = newRow();
            blurDialogsRow = newRow();
        } else {
            blurActionBarRow = -1;
            blurBottomBarRow = -1;
            blurDialogsRow = -1;
        }
        forceBlurRow = newRow();
        blurSettingsDividerRow = newRow();

        drawerOptionsHeaderRow = newRow();
        eventChooserRow = newRow();
        alternativeOpenAnimationRow = newRow();
        drawerOptionsDividerRow = newRow();

        drawerHeaderRow = newRow();
        statusRow = getUserConfig().isPremium() ? newRow() : -1;
        myStoriesRow = getMessagesController().storiesEnabled() ? newRow() : -1;
        archivedChatsRow = ChatUtils.getInstance().hasArchivedChats() ? newRow() : -1;
        menuBotsRow = ChatUtils.getInstance().hasBotsInSideMenu() ? newRow() : -1;
        newGroupRow = newRow();
        newSecretChatRow = newRow();
        newChannelRow = newRow();
        contactsRow = newRow();
        callsRow = newRow();
        peopleNearbyRow = SystemUtils.hasGps() ? newRow() : -1;
        savedMessagesRow = newRow();
        scanQrRow = newRow();
        drawerDividerRow = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == useSystemFontsRow) {
            ExteraConfig.editor.putBoolean("useSystemFonts", ExteraConfig.useSystemFonts ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.useSystemFonts);
            AndroidUtilities.clearTypefaceCache();
            if (getListView().getLayoutManager() != null)
                recyclerViewState = getListView().getLayoutManager().onSaveInstanceState();
            parentLayout.rebuildAllFragmentViews(true, true);
            getListView().getLayoutManager().onRestoreInstanceState(recyclerViewState);
        } else if (position == useSystemEmojiRow) {
            SharedConfig.toggleUseSystemEmoji();
            ((TextCheckCell) view).setChecked(SharedConfig.useSystemEmoji);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == tabletModeRow) {
            if (getParentActivity() == null) {
                return;
            }
            PopupUtils.showDialog(tabletMode, LocaleController.getString("TabletMode", R.string.TabletMode), ExteraConfig.tabletMode, getContext(), i -> {
                ExteraConfig.editor.putInt("tabletMode", ExteraConfig.tabletMode = i).apply();
                listAdapter.notifyItemChanged(tabletModeRow, payload);
                showBulletin();
            });
        } else if (position == singleCornerRadiusRow) {
            ExteraConfig.editor.putBoolean("singleCornerRadius", ExteraConfig.singleCornerRadius ^= true).apply();
            parentLayout.rebuildAllFragmentViews(false, false);
            ((TextCheckCell) view).setChecked(ExteraConfig.singleCornerRadius);
        } else if (position == forceBlurRow) {
            ExteraConfig.editor.putBoolean("forceBlur", ExteraConfig.forceBlur ^= true).apply();
            if (!SharedConfig.chatBlurEnabled() && ExteraConfig.forceBlur) {
                setBlurElementsEnabled(true);
            }
            ((TextCheckCell) view).setChecked(ExteraConfig.forceBlur);
        } else if (position == blurElementsRow) {
            blurElementsExpanded ^= true;
            updateRowsId();
            listAdapter.notifyItemChanged(blurElementsRow, payload);
            if (blurElementsExpanded) {
                listAdapter.notifyItemRangeInserted(blurElementsRow + 1, 3);
            } else {
                listAdapter.notifyItemRangeRemoved(blurElementsRow + 1, 3);
            }
        } else if (position >= blurActionBarRow && position <= blurDialogsRow) {
            if (position == blurActionBarRow) {
                ExteraConfig.editor.putBoolean("blurActionBar", ExteraConfig.blurActionBar ^= true).apply();
                listAdapter.notifyItemChanged(blurActionBarRow, payload);
            } else if (position == blurBottomBarRow) {
                ExteraConfig.editor.putBoolean("blurBottomPanel", ExteraConfig.blurBottomPanel ^= true).apply();
                listAdapter.notifyItemChanged(blurBottomBarRow, payload);
            } else if (position == blurDialogsRow) {
                ExteraConfig.editor.putBoolean("blurDialogs", ExteraConfig.blurDialogs ^= true).apply();
                listAdapter.notifyItemChanged(blurDialogsRow, payload);
            }
            if (!SharedConfig.chatBlurEnabled() && (ExteraConfig.blurActionBar || ExteraConfig.blurBottomPanel || ExteraConfig.blurDialogs) ||
                    SharedConfig.chatBlurEnabled() && !ExteraConfig.blurActionBar && !ExteraConfig.blurBottomPanel && !ExteraConfig.blurDialogs) {
                SharedConfig.toggleChatBlur();
            }
            listAdapter.notifyItemChanged(blurElementsRow, payload);
        } else if (position == alternativeOpenAnimationRow) {
            ExteraConfig.editor.putBoolean("alternativeOpenAnimation", ExteraConfig.alternativeOpenAnimation ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.alternativeOpenAnimation);
        } else if (position == alternativeNavigationRow) {
            ExteraConfig.editor.putBoolean("useLNavigation", ExteraConfig.useLNavigation ^= true).apply();
            if (ExteraConfig.useLNavigation) {
                MessagesController.getGlobalMainSettings().edit().putBoolean("view_animations", true).apply();
                SharedConfig.setAnimationsEnabled(true);
            }
            ((TextCheckCell) view).setChecked(ExteraConfig.useLNavigation);
        } else if (position == centerTitleRow) {
            ExteraConfig.editor.putBoolean("centerTitle", ExteraConfig.centerTitle ^= true).apply();
            chatListPreviewCell.updateCenteredTitle(true);
            ((TextCheckCell) view).setChecked(ExteraConfig.centerTitle);
            showBulletin();
        } else if (position == hideAllChatsRow) {
            ExteraConfig.editor.putBoolean("hideAllChats", ExteraConfig.hideAllChats ^= true).apply();
            foldersPreviewCell.updateAllChatsTabName(true);
            ((TextCheckCell) view).setChecked(ExteraConfig.hideAllChats);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == tabCounterRow) {
            ExteraConfig.editor.putBoolean("tabCounter", ExteraConfig.tabCounter ^= true).apply();
            foldersPreviewCell.updateTabCounter(true);
            ((TextCheckCell) view).setChecked(ExteraConfig.tabCounter);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
        } else if (position == newSwitchStyleRow) {
            ExteraConfig.editor.putBoolean("newSwitchStyle", ExteraConfig.newSwitchStyle ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.newSwitchStyle);
            if (getListView().getLayoutManager() != null)
                recyclerViewState = getListView().getLayoutManager().onSaveInstanceState();
            parentLayout.rebuildAllFragmentViews(true, true);
            getListView().getLayoutManager().onRestoreInstanceState(recyclerViewState);
        } else if (position == disableDividersRow) {
            ExteraConfig.editor.putBoolean("disableDividers", ExteraConfig.disableDividers ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disableDividers);
            Theme.applyCommonTheme();
            parentLayout.rebuildAllFragmentViews(false, false);
            listAdapter.notifyDataSetChanged();
        } else if (position == statusRow) {
            ExteraConfig.toggleDrawerElements(10);
            ((TextCell) view).setChecked(ExteraConfig.changeStatus);
        } else if (position == myStoriesRow) {
            ExteraConfig.toggleDrawerElements(11);
            ((TextCell) view).setChecked(ExteraConfig.myStories);
        } else if (position == menuBotsRow) {
            ExteraConfig.toggleDrawerElements(12);
            ((TextCell) view).setChecked(ExteraConfig.menuBots);
        } else if (position == newGroupRow) {
            ExteraConfig.toggleDrawerElements(1);
            ((TextCell) view).setChecked(ExteraConfig.newGroup);
        } else if (position == newSecretChatRow) {
            ExteraConfig.toggleDrawerElements(2);
            ((TextCell) view).setChecked(ExteraConfig.newSecretChat);
        } else if (position == newChannelRow) {
            ExteraConfig.toggleDrawerElements(3);
            ((TextCell) view).setChecked(ExteraConfig.newChannel);
        } else if (position == contactsRow) {
            ExteraConfig.toggleDrawerElements(4);
            ((TextCell) view).setChecked(ExteraConfig.contacts);
        } else if (position == callsRow) {
            ExteraConfig.toggleDrawerElements(5);
            ((TextCell) view).setChecked(ExteraConfig.calls);
        } else if (position == peopleNearbyRow) {
            ExteraConfig.toggleDrawerElements(6);
            ((TextCell) view).setChecked(ExteraConfig.peopleNearby);
        } else if (position == archivedChatsRow) {
            ExteraConfig.toggleDrawerElements(7);
            ((TextCell) view).setChecked(ExteraConfig.archivedChats);
        } else if (position == savedMessagesRow) {
            ExteraConfig.toggleDrawerElements(8);
            ((TextCell) view).setChecked(ExteraConfig.savedMessages);
        } else if (position == scanQrRow) {
            ExteraConfig.toggleDrawerElements(9);
            ((TextCell) view).setChecked(ExteraConfig.scanQr);
        } else if (position == forceSnowRow) {
            ExteraConfig.editor.putBoolean("forceSnow", ExteraConfig.forceSnow ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.forceSnow);
            showBulletin();
        } else if (position == eventChooserRow) {
            if (getParentActivity() == null) {
                return;
            }
            PopupUtils.showDialog(events, new int[]{
                    R.drawable.msg_calendar2, R.drawable.msg_block,
                    R.drawable.msg_settings_ny, R.drawable.msg_saved_14, R.drawable.msg_contacts_hw
            }, LocaleController.getString("DrawerIconSet", R.string.DrawerIconSet), ExteraConfig.eventType, getContext(), which -> {
                ExteraConfig.editor.putInt("eventType", ExteraConfig.eventType = which).apply();
                listAdapter.notifyItemChanged(eventChooserRow, payload);
                listAdapter.notifyItemRangeChanged(newGroupRow, 8);
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            });
        } else if (position == hideStoriesRow) {
            ExteraConfig.editor.putBoolean("hideStories", ExteraConfig.hideStories ^= true).apply();
            //chatListPreviewCell.updateStories(true);
            ((TextCheckCell) view).setChecked(ExteraConfig.hideStories);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == hideActionBarStatusRow) {
            ExteraConfig.editor.putBoolean("hideActionBarStatus", ExteraConfig.hideActionBarStatus ^= true).apply();
            chatListPreviewCell.updateStatus(true);
            ((TextCheckCell) view).setChecked(ExteraConfig.hideActionBarStatus);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == actionBarTitleRow) {
            if (getParentActivity() == null) {
                return;
            }
            PopupUtils.showDialog(titles, LocaleController.getString("ActionBarTitle", R.string.ActionBarTitle), ExteraConfig.titleText, getContext(), i -> {
                ExteraConfig.editor.putInt("titleText", ExteraConfig.titleText = i).apply();
                chatListPreviewCell.updateTitle(true);
                listAdapter.notifyItemChanged(actionBarTitleRow, payload);
                getNotificationCenter().postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
            });
        } else if (position == tabTitleRow) {
            if (getParentActivity() == null) {
                return;
            }
            PopupUtils.showDialog(tabIcons, LocaleController.getString("TabTitleStyle", R.string.TabTitleStyle), ExteraConfig.tabIcons, getContext(), i -> {
                ExteraConfig.editor.putInt("tabIcons", ExteraConfig.tabIcons = i).apply();
                foldersPreviewCell.updateTabIcons(true);
                foldersPreviewCell.updateTabTitle(true);
                listAdapter.notifyItemChanged(tabTitleRow, payload);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            });
        } else if (position == tabStyleRow) {
            if (getParentActivity() == null) {
                return;
            }
            PopupUtils.showDialog(styles, LocaleController.getString("TabStyle", R.string.TabStyle), ExteraConfig.tabStyle, getContext(), i -> {
                ExteraConfig.editor.putInt("tabStyle", ExteraConfig.tabStyle = i).apply();
                foldersPreviewCell.updateTabStyle(true);
                listAdapter.notifyItemChanged(tabStyleRow, payload);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            });
        } else if (position == solarIconsRow) {
            ((TextCheckCell) view).setChecked(!ExteraConfig.useSolarIcons);
            solarIconsPreview.updateIcons(true);
        }
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString("Appearance", R.string.Appearance);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private int getBlurElementsSelectedCount() {
        int i = 0;
        if (ExteraConfig.blurActionBar)
            i++;
        if (ExteraConfig.blurBottomPanel)
            i++;
        if (ExteraConfig.blurDialogs)
            i++;
        return i;
    }

    private void setBlurElementsEnabled(boolean enabled) {
        if (enabled && !SharedConfig.chatBlurEnabled() || !enabled && SharedConfig.chatBlurEnabled()) {
            SharedConfig.toggleChatBlur();
        }
        ExteraConfig.toggleBlur(enabled);
        AndroidUtilities.updateVisibleRows(listView);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            switch (type) {
                case 9 -> {
                    avatarCornersPreviewCell = new AvatarCornersPreviewCell(mContext, parentLayout);
                    avatarCornersPreviewCell.setNeedDivider(true);
                    avatarCornersPreviewCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    return new RecyclerListView.Holder(avatarCornersPreviewCell);
                }
                case 12 -> {
                    FabShapeCell fabShapeCell = new FabShapeCell(mContext) {
                        @Override
                        protected void rebuildFragments() {
                            parentLayout.rebuildAllFragmentViews(false, false);
                        }
                    };
                    fabShapeCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    return new RecyclerListView.Holder(fabShapeCell);
                }
                case 14 -> {
                    foldersPreviewCell = new FoldersPreviewCell(mContext);
                    foldersPreviewCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    return new RecyclerListView.Holder(foldersPreviewCell);
                }
                case 15 -> {
                    solarIconsPreview = new SolarIconsPreview(mContext) {
                        @Override
                        protected void reloadResources() {
                            ((LaunchActivity) getParentActivity()).reloadIcons();
                            Theme.reloadAllResources(getParentActivity());
                            parentLayout.rebuildAllFragmentViews(false, false);
                        }
                    };
                    solarIconsPreview.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    return new RecyclerListView.Holder(solarIconsPreview);
                }
                case 17 -> {
                    chatListPreviewCell = new ChatListPreviewCell(mContext);
                    chatListPreviewCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    return new RecyclerListView.Holder(chatListPreviewCell);
                }
                default -> {
                    return super.onCreateViewHolder(parent, type);
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1 ->
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                case 3 -> {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == appearanceHeaderRow) {
                        headerCell.setText(LocaleController.getString("Appearance", R.string.Appearance));
                    } else if (position == blurOptionsHeaderRow) {
                        headerCell.setText(LocaleController.getString("BlurOptions", R.string.BlurOptions));
                    } else if (position == drawerHeaderRow) {
                        headerCell.setText(LocaleController.getString("DrawerElements", R.string.DrawerElements));
                    } else if (position == drawerOptionsHeaderRow) {
                        headerCell.setText(LocaleController.getString("DrawerOptions", R.string.DrawerOptions));
                    } else if (position == solarIconsHeaderRow) {
                        headerCell.setText(LocaleController.getString("IconPack", R.string.IconPack));
                    } else if (position == foldersHeaderRow) {
                        headerCell.setText(LocaleController.getString("Filters", R.string.Filters));
                    } else if (position == chatListHeaderRow) {
                        headerCell.setText(LocaleController.getString("ListOfChats", R.string.ListOfChats));
                    }
                }
                case 5 -> {
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == useSystemFontsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.UseSystemFonts), ExteraConfig.useSystemFonts, true);
                    } else if (position == useSystemEmojiRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.UseSystemEmoji), SharedConfig.useSystemEmoji, true);
                    } else if (position == forceSnowRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString("ForceSnow", R.string.ForceSnow), LocaleController.getString(R.string.ForceSnowInfo), ExteraConfig.forceSnow, true, true);
                    } else if (position == alternativeNavigationRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.AlternativeNavigation), ExteraConfig.useLNavigation, false);
                    } else if (position == singleCornerRadiusRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.SingleCornerRadius), ExteraConfig.singleCornerRadius, false);
                    } else if (position == centerTitleRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("CenterTitle", R.string.CenterTitle), ExteraConfig.centerTitle, false);
                    } else if (position == hideAllChatsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.formatString(R.string.HideAllChats, LocaleController.getString(R.string.FilterAllChats)), ExteraConfig.hideAllChats, false);
                    } else if (position == tabCounterRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.TabCounter), ExteraConfig.tabCounter, true);
                    } else if (position == newSwitchStyleRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.NewSwitchStyle), ExteraConfig.newSwitchStyle, true);
                    } else if (position == disableDividersRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.DisableDividers), ExteraConfig.disableDividers, true);
                    } else if (position == hideActionBarStatusRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.HideActionBarStatus), ExteraConfig.hideActionBarStatus, true);
                    } else if (position == hideStoriesRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.HideStories), ExteraConfig.hideStories, true);
                    } else if (position == solarIconsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.SolarIcons), ExteraConfig.useSolarIcons, false);
                    } else if (position == alternativeOpenAnimationRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.DrawerAlternativeOpeningAnimation), ExteraConfig.alternativeOpenAnimation, false);
                    } else if (position == forceBlurRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.ForceBlur), ExteraConfig.forceBlur, false);
                    }
                }
                case 2 -> {
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setEnabled(true);
                    int[] icons = AppUtils.getDrawerIconPack();
                    if (position == statusRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("ChangeEmojiStatus", R.string.ChangeEmojiStatus), ExteraConfig.changeStatus, R.drawable.msg_status_set, true);
                    } else if (position == myStoriesRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("ProfileMyStories", R.string.ProfileMyStories), ExteraConfig.myStories, R.drawable.msg_menu_stories, true);
                    } else if (position == menuBotsRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("FilterBots", R.string.FilterBots), ExteraConfig.menuBots, R.drawable.msg_bot, true);
                    } else if (position == newGroupRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("NewGroup", R.string.NewGroup), ExteraConfig.newGroup, icons[0], true);
                    } else if (position == newSecretChatRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("NewSecretChat", R.string.NewSecretChat), ExteraConfig.newSecretChat, icons[1], true);
                    } else if (position == newChannelRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("NewChannel", R.string.NewChannel), ExteraConfig.newChannel, icons[2], true);
                    } else if (position == contactsRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("Contacts", R.string.Contacts), ExteraConfig.contacts, icons[3], true);
                    } else if (position == callsRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("Calls", R.string.Calls), ExteraConfig.calls, icons[4], true);
                    } else if (position == peopleNearbyRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("PeopleNearby", R.string.PeopleNearby), ExteraConfig.peopleNearby, icons[6], true);
                    } else if (position == archivedChatsRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("ArchivedChats", R.string.ArchivedChats), ExteraConfig.archivedChats, R.drawable.msg_archive, true);
                    } else if (position == savedMessagesRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("SavedMessages", R.string.SavedMessages), ExteraConfig.savedMessages, icons[5], true);
                    } else if (position == scanQrRow) {
                        textCell.setTextAndCheckAndIcon(LocaleController.getString("AuthAnotherClient", R.string.AuthAnotherClient), ExteraConfig.scanQr, R.drawable.msg_qrcode, false);
                    }
                }
                case 7 -> {
                    TextSettingsCell textSettingsCell = (TextSettingsCell) holder.itemView;
                    if (position == eventChooserRow) {
                        textSettingsCell.setTextAndValue(LocaleController.getString(R.string.DrawerIconSet), events[ExteraConfig.eventType], payload, true);
                    } else if (position == actionBarTitleRow) {
                        textSettingsCell.setTextAndValue(LocaleController.getString(R.string.ActionBarTitle), titles[ExteraConfig.titleText], payload, true);
                    } else if (position == tabTitleRow) {
                        textSettingsCell.setTextAndValue(LocaleController.getString(R.string.TabTitleStyle), tabIcons[ExteraConfig.tabIcons], payload, true);
                    } else if (position == tabStyleRow) {
                        textSettingsCell.setTextAndValue(LocaleController.getString(R.string.TabStyle), styles[ExteraConfig.tabStyle], payload, true);
                    } else if (position == tabletModeRow) {
                        textSettingsCell.setTextAndValue(LocaleController.getString("TabletMode", R.string.TabletMode), tabletMode[ExteraConfig.tabletMode], payload, true);
                    }
                }
                case 8 -> {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == appearanceDividerRow) {
                        cell.setText(LocaleController.getString(R.string.AlternativeNavigationInfo));
                    } else if (position == solarIconsInfoRow) {
                        cell.setText(LocaleUtils.formatWithUsernames(LocaleController.getString(R.string.SolarIconsInfo), AppearancePreferencesActivity.this));
                    } else if (position == foldersDividerRow) {
                        cell.setText(LocaleController.getString(R.string.FoldersInfo));
                    } else if (position == avatarCornersDividerRow) {
                        cell.setText(LocaleController.getString(R.string.SingleCornerRadiusInfo));
                    } else if (position == chatListDividerRow) {
                        cell.setText(LocaleController.getString(R.string.ListOfChatsInfo));
                    } else if (position == blurSettingsDividerRow) {
                        cell.setText(LocaleController.getString(R.string.ForceBlurInfo));
                    }
                }
                case 13 -> {
                    SlideChooseView slide = (SlideChooseView) holder.itemView;
                    if (position == blurSmoothnessRow) {
                        slide.setNeedDivider(true);
                        slide.setCallback(index -> ExteraConfig.editor.putInt("blurSmoothness", ExteraConfig.blurSmoothness = index).apply());
                        slide.setOptions(ExteraConfig.blurSmoothness, Arrays.stream(blurSmoothnessOptions).map(CharSequence::toString).toArray(String[]::new));
                    }
                }
                case 18 -> {
                    TextCheckCell2 checkCell = (TextCheckCell2) holder.itemView;
                    if (position == blurElementsRow) {
                        int blurElementsSelectedCount = getBlurElementsSelectedCount();
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.BlurElements), blurElementsSelectedCount > 0, true, true);
                        checkCell.setCollapseArrow(String.format(Locale.US, "%d/3", blurElementsSelectedCount), !blurElementsExpanded, () -> {
                            boolean checked = !checkCell.isChecked();
                            checkCell.setChecked(checked);
                            setBlurElementsEnabled(checked);
                        });
                    }
                    checkCell.getCheckBox().setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
                    checkCell.getCheckBox().setDrawIconType(0);
                }
                case 19 -> {
                    CheckBoxCell checkBoxCell = (CheckBoxCell) holder.itemView;
                    if (position == blurActionBarRow) {
                        checkBoxCell.setText(LocaleController.getString(R.string.BlurActionBar), "", ExteraConfig.blurActionBar, true, true);
                    } else if (position == blurBottomBarRow) {
                        checkBoxCell.setText(LocaleController.getString(R.string.BlurBottomPanel), "", ExteraConfig.blurBottomPanel, true, true);
                    } else if (position == blurDialogsRow) {
                        checkBoxCell.setText(LocaleController.getString(R.string.BlurDialogs), "", ExteraConfig.blurDialogs, true, true);
                    }
                    checkBoxCell.setPad(1);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == drawerDividerRow || position == drawerOptionsDividerRow) {
                return 1;
            } else if (position == statusRow || position == myStoriesRow || position == menuBotsRow || position == archivedChatsRow || position >= newGroupRow && position <= scanQrRow) {
                return 2;
            } else if (position == appearanceHeaderRow || position == blurOptionsHeaderRow || position == drawerHeaderRow || position == drawerOptionsHeaderRow || position == solarIconsHeaderRow || position == foldersHeaderRow || position == chatListHeaderRow) {
                return 3;
            } else if (position == eventChooserRow || position == actionBarTitleRow || position == tabStyleRow || position == tabTitleRow || position == tabletModeRow) {
                return 7;
            } else if (position == appearanceDividerRow || position == solarIconsInfoRow || position == foldersDividerRow || position == avatarCornersDividerRow || position == chatListDividerRow || position == blurSettingsDividerRow) {
                return 8;
            } else if (position == avatarCornersPreviewRow) {
                return 9;
            } else if (position == fabShapeRow) {
                return 12;
            } else if (position == blurSmoothnessRow) {
                return 13;
            } else if (position == foldersPreviewRow) {
                return 14;
            } else if (position == solarIconsPreviewRow) {
                return 15;
            } else if (position == chatListPreviewRow) {
                return 17;
            } else if (position == blurElementsRow) {
                return 18;
            } else if (position >= blurActionBarRow && position <= blurDialogsRow) {
                return 19;
            }
            return 5;
        }
    }
}