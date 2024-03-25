/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.preferences;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.utils.LocaleUtils;
import com.exteragram.messenger.utils.PopupUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.SlideChooseView;

public class GeneralPreferencesActivity extends BasePreferencesActivity {

    private final CharSequence[] id = new CharSequence[]{
            LocaleController.getString("Hide", R.string.Hide),
            "Telegram API",
            "Bot API"
    };

    private int speedBoostersHeaderRow;
    private int downloadSpeedChooserRow;
    private int uploadSpeedBoostRow;
    private int speedBoostersDividerRow;

    private int generalHeaderRow;
    private int formatTimeWithSecondsRow;
    private int disableNumberRoundingRow;
    private int inAppVibrationRow;
    private int filterZalgoRow;
    private int generalDividerRow;

    private int profileHeaderRow;
    private int showIdAndDcRow;
    private int hidePhoneNumberRow;
    private int profileDividerRow;

    private int archiveHeaderRow;
    private int archiveOnPullRow;
    private int disableUnarchiveSwipeRow;
    private int archiveDividerRow;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        generalHeaderRow = newRow();
        disableNumberRoundingRow = newRow();
        formatTimeWithSecondsRow = newRow();
        inAppVibrationRow = newRow();
        filterZalgoRow = newRow();
        generalDividerRow = newRow();

        speedBoostersHeaderRow = newRow();
        downloadSpeedChooserRow = newRow();
        uploadSpeedBoostRow = newRow();
        speedBoostersDividerRow = newRow();

        profileHeaderRow = newRow();
        hidePhoneNumberRow = newRow();
        showIdAndDcRow = newRow();
        profileDividerRow = newRow();

        archiveHeaderRow = newRow();
        archiveOnPullRow = newRow();
        disableUnarchiveSwipeRow = newRow();
        archiveDividerRow = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == disableNumberRoundingRow) {
            ExteraConfig.editor.putBoolean("disableNumberRounding", ExteraConfig.disableNumberRounding ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disableNumberRounding);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == formatTimeWithSecondsRow) {
            ExteraConfig.editor.putBoolean("formatTimeWithSeconds", ExteraConfig.formatTimeWithSeconds ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.formatTimeWithSeconds);
            LocaleController.getInstance().recreateFormatters();
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == inAppVibrationRow) {
            ExteraConfig.editor.putBoolean("inAppVibration", ExteraConfig.inAppVibration ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.inAppVibration);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == filterZalgoRow) {
            ExteraConfig.editor.putBoolean("filterZalgo", ExteraConfig.filterZalgo ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.filterZalgo);
            listAdapter.notifyItemChanged(generalDividerRow);
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == archiveOnPullRow) {
            ExteraConfig.editor.putBoolean("archiveOnPull", ExteraConfig.archiveOnPull ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.archiveOnPull);
        } else if (position == disableUnarchiveSwipeRow) {
            ExteraConfig.editor.putBoolean("disableUnarchiveSwipe", ExteraConfig.disableUnarchiveSwipe ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.disableUnarchiveSwipe);
        } else if (position == hidePhoneNumberRow) {
            ExteraConfig.editor.putBoolean("hidePhoneNumber", ExteraConfig.hidePhoneNumber ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.hidePhoneNumber);
            parentLayout.rebuildAllFragmentViews(false, false);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == showIdAndDcRow) {
            if (getParentActivity() == null) {
                return;
            }
            PopupUtils.showDialog(id, LocaleController.getString("ShowIdAndDc", R.string.ShowIdAndDc), ExteraConfig.showIdAndDc, getContext(), i -> {
                ExteraConfig.editor.putInt("showIdAndDc", ExteraConfig.showIdAndDc = i).apply();
                parentLayout.rebuildAllFragmentViews(false, false);
                listAdapter.notifyItemChanged(showIdAndDcRow, payload);
            });
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == uploadSpeedBoostRow) {
            ExteraConfig.editor.putBoolean("uploadSpeedBoost", ExteraConfig.uploadSpeedBoost ^= true).apply();
            ((TextCheckCell) view).setChecked(ExteraConfig.uploadSpeedBoost);
        }
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString("General", R.string.General);
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
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1 ->
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                case 3 -> {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == generalHeaderRow) {
                        headerCell.setText(LocaleController.getString("General", R.string.General));
                    } else if (position == archiveHeaderRow) {
                        headerCell.setText(LocaleController.getString("ArchivedChats", R.string.ArchivedChats));
                    } else if (position == profileHeaderRow) {
                        headerCell.setText(LocaleController.getString("Profile", R.string.Profile));
                    } else if (position == speedBoostersHeaderRow) {
                        headerCell.setText(LocaleController.getString("DownloadSpeedBoost", R.string.DownloadSpeedBoost));
                    }
                }
                case 5 -> {
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == disableNumberRoundingRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString("DisableNumberRounding", R.string.DisableNumberRounding), "1.23K -> 1,234", ExteraConfig.disableNumberRounding, true, true);
                    } else if (position == formatTimeWithSecondsRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString("FormatTimeWithSeconds", R.string.FormatTimeWithSeconds), "12:34 -> 12:34:56", ExteraConfig.formatTimeWithSeconds, true, true);
                    } else if (position == inAppVibrationRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.InAppVibration), ExteraConfig.inAppVibration, true);
                    } else if (position == filterZalgoRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.FilterZalgo), ExteraConfig.filterZalgo, false);
                    } else if (position == disableUnarchiveSwipeRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("DisableUnarchiveSwipe", R.string.DisableUnarchiveSwipe), ExteraConfig.disableUnarchiveSwipe, false);
                    } else if (position == archiveOnPullRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("ArchiveOnPull", R.string.ArchiveOnPull), ExteraConfig.archiveOnPull, true);
                    } else if (position == hidePhoneNumberRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("HidePhoneNumber", R.string.HidePhoneNumber), ExteraConfig.hidePhoneNumber, true);
                    } else if (position == uploadSpeedBoostRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("UploadSpeedBoost", R.string.UploadSpeedBoost), ExteraConfig.uploadSpeedBoost, false);}
                }
                case 7 -> {
                    TextSettingsCell textSettingsCell = (TextSettingsCell) holder.itemView;
                    if (position == showIdAndDcRow) {
                        textSettingsCell.setTextAndValue(LocaleController.getString("ShowIdAndDc", R.string.ShowIdAndDc), id[ExteraConfig.showIdAndDc], payload, false);
                    }
                }
                case 8 -> {
                    TextInfoPrivacyCell textInfoPrivacyCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == speedBoostersDividerRow) {
                        textInfoPrivacyCell.setText(LocaleController.getString("SpeedBoostInfo", R.string.SpeedBoostInfo));
                    } else if (position == profileDividerRow) {
                        textInfoPrivacyCell.setText(LocaleController.getString("ShowIdAndDcInfo", R.string.ShowIdAndDcInfo));
                    } else if (position == archiveDividerRow) {
                        textInfoPrivacyCell.setText(LocaleController.getString("DisableUnarchiveSwipeInfo", R.string.DisableUnarchiveSwipeInfo));
                    } else if (position == generalDividerRow) {
                        textInfoPrivacyCell.getTextView().setMovementMethod(null);
                        textInfoPrivacyCell.setText(LocaleController.formatString(R.string.FilterZalgoInfo, LocaleUtils.filter("Z̷͍͌ā̸̜l̸̞̂g̷͍̝o̶̩̓")));
                    }
                }
                case 13 -> {
                    SlideChooseView slide = (SlideChooseView) holder.itemView;
                    if (position == downloadSpeedChooserRow) {
                        slide.setNeedDivider(true);
                        slide.setCallback(index -> ExteraConfig.editor.putInt("downloadSpeedBoost", ExteraConfig.downloadSpeedBoost = index).apply());
                        slide.setOptions(ExteraConfig.downloadSpeedBoost, LocaleController.getString("BlurOff", R.string.BlurOff), LocaleController.getString("SpeedFast", R.string.SpeedFast), LocaleController.getString("Ultra", R.string.Ultra));
                    }
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == generalHeaderRow || position == archiveHeaderRow || position == profileHeaderRow ||
                    position == speedBoostersHeaderRow) {
                return 3;
            } else if (position == showIdAndDcRow) {
                return 7;
            } else if (position == speedBoostersDividerRow || position == profileDividerRow  || position == archiveDividerRow || position == generalDividerRow) {
                return 8;
            } else if (position == downloadSpeedChooserRow) {
                return 13;
            }
            return 5;
        }
    }
}
