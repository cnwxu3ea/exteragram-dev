/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatThemeController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChannelAdminLogActivity;
import org.telegram.ui.ChatBackgroundDrawable;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.ButtonBounce;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.ImageUpdater;
import org.telegram.ui.Components.LoadingDrawable;
import org.telegram.ui.Components.MediaActionDrawable;
import org.telegram.ui.Components.Premium.StarParticlesView;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RadialProgress2;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.spoilers.SpoilerEffect;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.Stories.StoriesUtilities;
import org.telegram.ui.Stories.UploadingDotsSpannable;
import org.telegram.ui.Stories.recorder.HintView2;
import org.telegram.ui.Stories.recorder.PreviewView;

import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import com.exteragram.messenger.ExteraConfig;

public class ChatActionCell extends BaseCell implements DownloadController.FileDownloadProgressListener, NotificationCenter.NotificationCenterDelegate {
    private final static boolean USE_PREMIUM_GIFT_LOCAL_STICKER = false;
    private final static boolean USE_PREMIUM_GIFT_MONTHS_AS_EMOJI_NUMBERS = false;

    private static Map<Integer, String> monthsToEmoticon = new HashMap<>();

    static {
        monthsToEmoticon.put(1, 1 + "\u20E3");
        monthsToEmoticon.put(3, 2 + "\u20E3");
        monthsToEmoticon.put(6, 3 + "\u20E3");
        monthsToEmoticon.put(12, 4 + "\u20E3");
        monthsToEmoticon.put(24, 5 + "\u20E3");
    }

    private int backgroundRectHeight;
    private int backgroundButtonTop;
    private ButtonBounce bounce = new ButtonBounce(this);
    private LoadingDrawable loadingDrawable;

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.startSpoilers) {
            setSpoilersSuppressed(false);
        } else if (id == NotificationCenter.stopSpoilers) {
            setSpoilersSuppressed(true);
        } else if (id == NotificationCenter.didUpdatePremiumGiftStickers) {
            MessageObject messageObject = currentMessageObject;
            if (messageObject != null) {
                setMessageObject(messageObject, true);
            }
        } else if (id == NotificationCenter.diceStickersDidLoad) {
            if (Objects.equals(args[0], UserConfig.getInstance(currentAccount).premiumGiftsStickerPack)) {
                MessageObject messageObject = currentMessageObject;
                if (messageObject != null) {
                    setMessageObject(messageObject, true);
                }
            }
        }
    }

    public void setSpoilersSuppressed(boolean s) {
        for (SpoilerEffect eff : spoilers) {
            eff.setSuppressUpdates(s);
        }
    }

    private boolean canDrawInParent;
    private View invalidateWithParent;

    public void setInvalidateWithParent(View viewToInvalidate) {
        invalidateWithParent = viewToInvalidate;
    }

    public boolean hasButton() {
        return currentMessageObject != null && isButtonLayout(currentMessageObject) && giftPremiumButtonLayout != null;
    }

    public interface ChatActionCellDelegate {
        default void didClickImage(ChatActionCell cell) {
        }

        default void didClickButton(ChatActionCell cell) {
        }

        default void didOpenPremiumGift(ChatActionCell cell, TLRPC.TL_premiumGiftOption giftOption, String slug, boolean animateConfetti) {
        }

        default void didOpenPremiumGiftChannel(ChatActionCell cell, String slug, boolean animateConfetti) {
        }

        default boolean didLongPress(ChatActionCell cell, float x, float y) {
            return false;
        }

        default void needOpenUserProfile(long uid) {
        }

        default void didPressBotButton(MessageObject messageObject, TLRPC.KeyboardButton button) {
        }

        default void didPressReplyMessage(ChatActionCell cell, int id) {
        }

        default void needOpenInviteLink(TLRPC.TL_chatInviteExported invite) {
        }

        default void needShowEffectOverlay(ChatActionCell cell, TLRPC.Document document, TLRPC.VideoSize videoSize) {
        }

        default BaseFragment getBaseFragment() {
            return null;
        }

        default long getDialogId() {
            return 0;
        }

        default long getTopicId() {
            return 0;
        }

        default boolean canDrawOutboundsContent() {
            return true;
        }
    }

    public interface ThemeDelegate extends Theme.ResourcesProvider {

        int getCurrentColor();
    }

    private int TAG;

    private URLSpan pressedLink;
    private int currentAccount = UserConfig.selectedAccount;
    private ImageReceiver imageReceiver;
    private Drawable wallpaperPreviewDrawable;
    private Path clipPath;
    private AvatarDrawable avatarDrawable;
    private StaticLayout textLayout;
    private int textWidth;
    private int textHeight;
    private int textX;
    private int textY;
    private int textXLeft;
    private int previousWidth;
    private boolean imagePressed;
    private boolean giftButtonPressed;
    RadialProgressView progressView;
    float progressToProgress;
    StoriesUtilities.AvatarStoryParams avatarStoryParams = new StoriesUtilities.AvatarStoryParams(false);

    private RectF giftButtonRect = new RectF();

    public List<SpoilerEffect> spoilers = new ArrayList<>();
    private Stack<SpoilerEffect> spoilersPool = new Stack<>();
    private AnimatedEmojiSpan.EmojiGroupedSpans animatedEmojiStack;

    TextPaint textPaint;

    private float viewTop;
    private float viewTranslationX;
    private int backgroundHeight;
    private boolean visiblePartSet;

    private ImageLocation currentVideoLocation;

    private float lastTouchX;
    private float lastTouchY;

    private boolean wasLayout;

    private boolean hasReplyMessage;

    private MessageObject currentMessageObject;
    private int customDate;
    private CharSequence customText;

    private int overrideBackground = -1;
    private int overrideText = -1;
    private Paint overrideBackgroundPaint;
    private TextPaint overrideTextPaint;
    private int overrideColor;
    private ArrayList<Integer> lineWidths = new ArrayList<>();
    private ArrayList<Integer> lineHeights = new ArrayList<>();
    private Path backgroundPath = new Path();
    private int backgroundLeft, backgroundRight;
    private RectF rect = new RectF();
    private boolean invalidatePath = true;
    private boolean invalidateColors = false;

    private ChatActionCellDelegate delegate;
    private Theme.ResourcesProvider themeDelegate;

    private int stickerSize;
    private int giftRectSize;
    private StaticLayout giftPremiumTitleLayout;
    private int giftPremiumSubtitleWidth;
    private StaticLayout giftPremiumSubtitleLayout;
    private StaticLayout giftPremiumButtonLayout;
    private boolean buttonClickableAsImage = true;
    TextPaint settingWallpaperPaint;
    private StaticLayout settingWallpaperLayout;
    private float settingWallpaperProgress;
    private StaticLayout settingWallpaperProgressTextLayout;
    private float giftPremiumButtonWidth;

    private TextPaint giftTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint giftSubtitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private TLRPC.Document giftSticker;
    private TLRPC.VideoSize giftEffectAnimation;
    private RadialProgress2 radialProgress = new RadialProgress2(this);
    private int giftPremiumAdditionalHeight;
    private boolean forceWasUnread;
    private RectF backgroundRect;
    private ImageReceiver.ImageReceiverDelegate giftStickerDelegate = (imageReceiver1, set, thumb, memCache) -> {
        if (set) {
            RLottieDrawable drawable = imageReceiver.getLottieAnimation();
            if (drawable != null) {
                MessageObject messageObject = currentMessageObject;
                if (messageObject != null && !messageObject.playedGiftAnimation) {
                    messageObject.playedGiftAnimation = true;
                    drawable.setCurrentFrame(0, false);
                    AndroidUtilities.runOnUIThread(drawable::restart);

                    if (messageObject != null && messageObject.wasUnread || forceWasUnread) {
                        forceWasUnread = messageObject.wasUnread = false;

                        try {
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                        } catch (Exception ignored) {
                        }

                        if (getContext() instanceof LaunchActivity) {
                            ((LaunchActivity) getContext()).getFireworksOverlay().start();
                        }

                        if (giftEffectAnimation != null && delegate != null) {
                            delegate.needShowEffectOverlay(ChatActionCell.this, giftSticker, giftEffectAnimation);
                        }
                    }
                } else {
                    drawable.stop();
                    drawable.setCurrentFrame(drawable.getFramesCount() - 1, false);
                }
            }
        }
    };

    private View rippleView;

    private Path starsPath = new Path();
    private StarParticlesView.Drawable starParticlesDrawable;
    private int starsSize;

    public ChatActionCell(Context context) {
        this(context, false, null);
    }

    public ChatActionCell(Context context, boolean canDrawInParent, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        avatarStoryParams.drawSegments = false;
        this.canDrawInParent = canDrawInParent;
        this.themeDelegate = resourcesProvider;
        imageReceiver = new ImageReceiver(this);
        imageReceiver.setRoundRadius(ExteraConfig.getAvatarCorners(AndroidUtilities.roundMessageSize, true));
        avatarDrawable = new AvatarDrawable();
        TAG = DownloadController.getInstance(currentAccount).generateObserverTag();

        giftTitlePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()));
        giftSubtitlePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics()));

        rippleView = new View(context);
        rippleView.setBackground(Theme.createSelectorDrawable(Theme.multAlpha(Color.BLACK, .1f), Theme.RIPPLE_MASK_ROUNDRECT_6DP, dp(16)));
        rippleView.setVisibility(GONE);
        addView(rippleView);

        starParticlesDrawable = new StarParticlesView.Drawable(10);
        starParticlesDrawable.type = 100;
        starParticlesDrawable.isCircle = false;
        starParticlesDrawable.roundEffect = true;
        starParticlesDrawable.useRotate = false;
        starParticlesDrawable.useBlur = true;
        starParticlesDrawable.checkBounds = true;
        starParticlesDrawable.size1 = 1;
        starParticlesDrawable.k1 = starParticlesDrawable.k2 = starParticlesDrawable.k3 = 0.98f;
        starParticlesDrawable.paused = false;
        starParticlesDrawable.speedScale = 0f;
        starParticlesDrawable.minLifeTime = 750;
        starParticlesDrawable.randLifeTime = 750;
        starParticlesDrawable.init();
    }

    public void setDelegate(ChatActionCellDelegate delegate) {
        this.delegate = delegate;
    }

    public void setCustomDate(int date, boolean scheduled, boolean inLayout) {
        if (customDate == date || customDate / 3600 == date / 3600) {
            return;
        }
        CharSequence newText;
        if (scheduled) {
            if (date == 0x7ffffffe) {
                newText = LocaleController.getString("MessageScheduledUntilOnline", R.string.MessageScheduledUntilOnline);
            } else {
                newText = LocaleController.formatString("MessageScheduledOn", R.string.MessageScheduledOn, LocaleController.formatDateChat(date));
            }
        } else {
            newText = LocaleController.formatDateChat(date);
        }
        customDate = date;
        if (customText != null && TextUtils.equals(newText, customText)) {
            return;
        }
        customText = newText;
        accessibilityText = null;
        updateTextInternal(inLayout);
    }

    private void updateTextInternal(boolean inLayout) {
        if (getMeasuredWidth() != 0) {
            createLayout(customText, getMeasuredWidth());
            invalidate();
        }
        if (!wasLayout) {
            if (inLayout) {
                AndroidUtilities.runOnUIThread(this::requestLayout);
            } else {
                requestLayout();
            }
        } else {
            buildLayout();
        }
    }

    public void setCustomText(CharSequence text) {
        customText = text;
        if (customText != null) {
            updateTextInternal(false);
        }
    }

    public void setOverrideColor(int background, int text) {
        overrideBackground = background;
        overrideText = text;
    }

    public void setMessageObject(MessageObject messageObject) {
        setMessageObject(messageObject, false);
    }

    public void setMessageObject(MessageObject messageObject, boolean force) {
        if (currentMessageObject == messageObject && (textLayout == null || TextUtils.equals(textLayout.getText(), messageObject.messageText)) && (hasReplyMessage || messageObject.replyMessageObject == null) && !force && messageObject.type != MessageObject.TYPE_SUGGEST_PHOTO && !messageObject.forceUpdate) {
            return;
        }
        if (BuildVars.DEBUG_PRIVATE_VERSION && Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
            FileLog.e(new IllegalStateException("Wrong thread!!!"));
        }
        accessibilityText = null;
        boolean messageIdChanged = currentMessageObject == null || currentMessageObject.stableId != messageObject.stableId;
        currentMessageObject = messageObject;
        messageObject.forceUpdate = false;
        hasReplyMessage = messageObject.replyMessageObject != null;
        DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
        previousWidth = 0;
        imageReceiver.setAutoRepeatCount(0);
        imageReceiver.clearDecorators();
        if (messageObject.type != MessageObject.TYPE_ACTION_WALLPAPER) {
            wallpaperPreviewDrawable = null;
        }
        if (messageObject.actionDeleteGroupEventId != -1) {
            ScaleStateListAnimator.apply(this, .02f, 1.2f);
            overriddenMaxWidth = Math.max(dp(250), HintView2.cutInFancyHalf(messageObject.messageText, (TextPaint) getThemedPaint(Theme.key_paint_chatActionText)));
            ProfileActivity.ShowDrawable showDrawable = ChannelAdminLogActivity.findDrawable(messageObject.messageText);
            if (showDrawable != null) {
                showDrawable.setView(this);
            }
        } else {
            ScaleStateListAnimator.reset(this);
            overriddenMaxWidth = 0;
        }
        if (messageObject.isStoryMention()) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(messageObject.messageOwner.media.user_id);
            avatarDrawable.setInfo(currentAccount, user);
            TL_stories.StoryItem storyItem = messageObject.messageOwner.media.storyItem;
            if (storyItem != null && storyItem.noforwards) {
                imageReceiver.setForUserOrChat(user, avatarDrawable, null, true, 0, true);
            } else {
                StoriesUtilities.setImage(imageReceiver, storyItem);
            }
            imageReceiver.setRoundRadius(ExteraConfig.getAvatarCorners(stickerSize * 0.7f, true));
        } else if (messageObject.type == MessageObject.TYPE_ACTION_WALLPAPER) {
            TLRPC.PhotoSize strippedPhotoSize = null;
            if (messageObject.strippedThumb == null) {
                for (int a = 0, N = messageObject.photoThumbs.size(); a < N; a++) {
                    TLRPC.PhotoSize photoSize = messageObject.photoThumbs.get(a);
                    if (photoSize instanceof TLRPC.TL_photoStrippedSize) {
                        strippedPhotoSize = photoSize;
                        break;
                    }
                }
            }
            TLRPC.WallPaper wallPaper = null;
            if (messageObject.currentEvent != null && messageObject.currentEvent.action instanceof TLRPC.TL_channelAdminLogEventActionChangeWallpaper) {
                wallPaper = ((TLRPC.TL_channelAdminLogEventActionChangeWallpaper) messageObject.currentEvent.action).new_value;
            } else if (messageObject.messageOwner != null && messageObject.messageOwner.action != null) {
                TLRPC.MessageAction action = messageObject.messageOwner.action;
                wallPaper = action.wallpaper;
            }
            if (!TextUtils.isEmpty(ChatThemeController.getWallpaperEmoticon(wallPaper))) {
                final boolean isDark = themeDelegate != null ? themeDelegate.isDark() : Theme.isCurrentThemeDark();
                imageReceiver.clearImage();
                wallpaperPreviewDrawable = PreviewView.getBackgroundDrawableFromTheme(currentAccount, ChatThemeController.getWallpaperEmoticon(wallPaper), isDark, false);
                if (wallpaperPreviewDrawable != null) {
                    wallpaperPreviewDrawable.setCallback(this);
                }
            } else if (wallPaper != null && wallPaper.uploadingImage != null) {
                imageReceiver.setImage(ImageLocation.getForPath(wallPaper.uploadingImage), "150_150_wallpaper" + wallPaper.id + ChatBackgroundDrawable.hash(wallPaper.settings), null, null, ChatBackgroundDrawable.createThumb(wallPaper), 0, null, wallPaper, 1);
                wallpaperPreviewDrawable = null;
            } else if (wallPaper != null) {
                TLRPC.Document document = null;
                if (messageObject.photoThumbsObject instanceof TLRPC.Document) {
                    document = (TLRPC.Document) messageObject.photoThumbsObject;
                } else if (wallPaper != null) {
                    document = wallPaper.document;
                }
                imageReceiver.setImage(ImageLocation.getForDocument(document), "150_150_wallpaper" + wallPaper.id + ChatBackgroundDrawable.hash(wallPaper.settings), null, null, ChatBackgroundDrawable.createThumb(wallPaper), 0, null, wallPaper, 1);
                wallpaperPreviewDrawable = null;
            } else {
                wallpaperPreviewDrawable = null;
            }
            imageReceiver.setRoundRadius(ExteraConfig.getAvatarCorners(stickerSize * 0.7f, true));

            float uploadingInfoProgress = getUploadingInfoProgress(messageObject);
            if (uploadingInfoProgress == 1f) {
                radialProgress.setProgress(1f, !messageIdChanged);
                radialProgress.setIcon(MediaActionDrawable.ICON_NONE, !messageIdChanged, !messageIdChanged);
            } else {
                radialProgress.setIcon(MediaActionDrawable.ICON_CANCEL, !messageIdChanged, !messageIdChanged);
            }
        } else if (messageObject.type == MessageObject.TYPE_SUGGEST_PHOTO) {
            imageReceiver.setRoundRadius(ExteraConfig.getAvatarCorners(stickerSize * 0.7f, true));
            imageReceiver.setAllowStartLottieAnimation(true);
            imageReceiver.setDelegate(null);
            TLRPC.TL_messageActionSuggestProfilePhoto action = (TLRPC.TL_messageActionSuggestProfilePhoto) messageObject.messageOwner.action;

            TLRPC.VideoSize videoSize = FileLoader.getClosestVideoSizeWithSize(action.photo.video_sizes, 1000);
            ImageLocation videoLocation;
            if (action.photo.video_sizes != null && !action.photo.video_sizes.isEmpty()) {
                videoLocation = ImageLocation.getForPhoto(videoSize, action.photo);
            } else {
                videoLocation = null;
            }
            TLRPC.Photo photo = messageObject.messageOwner.action.photo;
            TLRPC.PhotoSize strippedPhotoSize = null;
            if (messageObject.strippedThumb == null) {
                for (int a = 0, N = messageObject.photoThumbs.size(); a < N; a++) {
                    TLRPC.PhotoSize photoSize = messageObject.photoThumbs.get(a);
                    if (photoSize instanceof TLRPC.TL_photoStrippedSize) {
                        strippedPhotoSize = photoSize;
                        break;
                    }
                }
            }
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 1000);
            if (photoSize != null) {
                if (videoSize != null) {
                    imageReceiver.setImage(videoLocation, ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForPhoto(photoSize, photo), "150_150", ImageLocation.getForObject(strippedPhotoSize, messageObject.photoThumbsObject), "50_50_b", messageObject.strippedThumb, 0, null, messageObject, 0);
                } else {
                    imageReceiver.setImage(ImageLocation.getForPhoto(photoSize, photo), "150_150", ImageLocation.getForObject(strippedPhotoSize, messageObject.photoThumbsObject), "50_50_b", messageObject.strippedThumb, 0, null, messageObject, 0);
                }
            }

            imageReceiver.setAllowStartLottieAnimation(false);
            ImageUpdater imageUpdater = MessagesController.getInstance(currentAccount).photoSuggestion.get(messageObject.messageOwner.local_id);
            if (imageUpdater == null || imageUpdater.getCurrentImageProgress() == 1f) {
                radialProgress.setProgress(1f, !messageIdChanged);
                radialProgress.setIcon(MediaActionDrawable.ICON_NONE, !messageIdChanged, !messageIdChanged);
            } else {
                radialProgress.setIcon(MediaActionDrawable.ICON_CANCEL, !messageIdChanged, !messageIdChanged);
            }
        } else if (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM || messageObject.type == MessageObject.TYPE_GIFT_PREMIUM_CHANNEL) {
            imageReceiver.setRoundRadius(0);

            if (USE_PREMIUM_GIFT_LOCAL_STICKER) {
                forceWasUnread = messageObject.wasUnread;
                imageReceiver.setAllowStartLottieAnimation(false);
                imageReceiver.setDelegate(giftStickerDelegate);
                imageReceiver.setImageBitmap(new RLottieDrawable(R.raw.premium_gift, messageObject.getId() + "_" + R.raw.premium_gift, dp(160), dp(160)));
            } else {
                TLRPC.TL_messages_stickerSet set;
                TLRPC.Document document = null;

                String packName = UserConfig.getInstance(currentAccount).premiumGiftsStickerPack;
                if (packName == null) {
                    MediaDataController.getInstance(currentAccount).checkPremiumGiftStickers();
                    return;
                }
                set = MediaDataController.getInstance(currentAccount).getStickerSetByName(packName);
                if (set == null) {
                    set = MediaDataController.getInstance(currentAccount).getStickerSetByEmojiOrName(packName);
                }
                if (set != null) {
                    int  months = messageObject.messageOwner.action.months;
                    String monthsEmoticon;
                    if (USE_PREMIUM_GIFT_MONTHS_AS_EMOJI_NUMBERS) {
                        StringBuilder monthsEmoticonBuilder = new StringBuilder();
                        while (months > 0) {
                            monthsEmoticonBuilder.insert(0, (months % 10) + "\u20E3");
                            months /= 10;
                        }
                        monthsEmoticon = monthsEmoticonBuilder.toString();
                    } else {
                        monthsEmoticon = monthsToEmoticon.get(months);
                    }
                    for (TLRPC.TL_stickerPack pack : set.packs) {
                        if (Objects.equals(pack.emoticon, monthsEmoticon)) {
                            for (long id : pack.documents) {
                                for (TLRPC.Document doc : set.documents) {
                                    if (doc.id == id) {
                                        document = doc;
                                        break;
                                    }
                                }
                                if (document != null) {
                                    break;
                                }
                            }
                        }
                        if (document != null) {
                            break;
                        }
                    }

                    if (document == null && !set.documents.isEmpty()) {
                        document = set.documents.get(0);
                    }
                }

                forceWasUnread = messageObject.wasUnread;
                giftSticker = document;
                if (document != null) {
                    imageReceiver.setAllowStartLottieAnimation(true);
                    imageReceiver.setDelegate(giftStickerDelegate);

                    giftEffectAnimation = null;
                    for (int i = 0; i < document.video_thumbs.size(); i++) {
                        if ("f".equals(document.video_thumbs.get(i).type)) {
                            giftEffectAnimation = document.video_thumbs.get(i);
                            break;
                        }
                    }
                    SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 0.3f);
                    imageReceiver.setAutoRepeat(0);
                    imageReceiver.setImage(ImageLocation.getForDocument(document), String.format(Locale.US, "%d_%d_nr_messageId=%d", 160, 160, messageObject.stableId), svgThumb, "tgs", set, 1);
                } else {
                    MediaDataController.getInstance(currentAccount).loadStickersByEmojiOrName(packName, false, set == null);
                }
            }
        } else if (messageObject.type == MessageObject.TYPE_ACTION_PHOTO) {
            imageReceiver.setAllowStartLottieAnimation(true);
            imageReceiver.setDelegate(null);
            imageReceiver.setRoundRadius(ExteraConfig.getAvatarCorners(AndroidUtilities.roundMessageSize, true));
            imageReceiver.setAutoRepeatCount(1);
            long id = messageObject.getDialogId();
            avatarDrawable.setInfo(id, null, null);
            if (messageObject.messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                imageReceiver.setImage(null, null, avatarDrawable, null, messageObject, 0);
            } else {
                TLRPC.PhotoSize strippedPhotoSize = null;
                if (messageObject.strippedThumb == null) {
                    for (int a = 0, N = messageObject.photoThumbs.size(); a < N; a++) {
                        TLRPC.PhotoSize photoSize = messageObject.photoThumbs.get(a);
                        if (photoSize instanceof TLRPC.TL_photoStrippedSize) {
                            strippedPhotoSize = photoSize;
                            break;
                        }
                    }
                }
                TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 640);
                if (photoSize != null) {
                    TLRPC.Photo photo = messageObject.messageOwner.action.photo;
                    TLRPC.VideoSize videoSize = null;
                    if (!photo.video_sizes.isEmpty() && SharedConfig.isAutoplayGifs()) {
                        videoSize = FileLoader.getClosestVideoSizeWithSize(photo.video_sizes, 1000);
                        if (!messageObject.mediaExists && !DownloadController.getInstance(currentAccount).canDownloadMedia(DownloadController.AUTODOWNLOAD_TYPE_VIDEO, videoSize.size)) {
                            currentVideoLocation = ImageLocation.getForPhoto(videoSize, photo);
                            String fileName = FileLoader.getAttachFileName(videoSize);
                            DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, messageObject, this);
                            videoSize = null;
                        }
                    }
                    if (videoSize != null) {
                        imageReceiver.setImage(ImageLocation.getForPhoto(videoSize, photo), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(strippedPhotoSize, messageObject.photoThumbsObject), "50_50_b", messageObject.strippedThumb, 0, null, messageObject, 1);
                    } else {
                        imageReceiver.setImage(ImageLocation.getForObject(photoSize, messageObject.photoThumbsObject), "150_150", ImageLocation.getForObject(strippedPhotoSize, messageObject.photoThumbsObject), "50_50_b", messageObject.strippedThumb, 0, null, messageObject, 1);
                    }
                } else {
                    imageReceiver.setImageBitmap(avatarDrawable);
                }
            }
            imageReceiver.setVisible(!PhotoViewer.isShowingImage(messageObject), false);
        } else {
            imageReceiver.setAllowStartLottieAnimation(true);
            imageReceiver.setDelegate(null);
            imageReceiver.setImageBitmap((Bitmap) null);
        }
        rippleView.setVisibility(isButtonLayout(messageObject) ? VISIBLE : GONE);
        ForumUtilities.applyTopicToMessage(messageObject);
        requestLayout();
    }

    private float getUploadingInfoProgress(MessageObject messageObject) {
        if (messageObject != null && messageObject.type == MessageObject.TYPE_ACTION_WALLPAPER) {
            MessagesController messagesController = MessagesController.getInstance(currentAccount);
            if (messagesController.uploadingWallpaper != null && TextUtils.equals(messageObject.messageOwner.action.wallpaper.uploadingImage, messagesController.uploadingWallpaper)) {
                return messagesController.uploadingWallpaperInfo.uploadingProgress;
            }
        }
        return 1;
    }

    public MessageObject getMessageObject() {
        return currentMessageObject;
    }

    public ImageReceiver getPhotoImage() {
        return imageReceiver;
    }

    public void setVisiblePart(float visibleTop, int parentH) {
        visiblePartSet = true;
        backgroundHeight = parentH;
        viewTop = visibleTop;
        viewTranslationX = 0;
    }

    private float dimAmount;
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public void setVisiblePart(float visibleTop, float tx, int parentH, float dimAmount) {
        visiblePartSet = true;
        backgroundHeight = parentH;
        viewTop = visibleTop;
        viewTranslationX = tx;

        this.dimAmount = dimAmount;
        dimPaint.setColor(ColorUtils.setAlphaComponent(Color.BLACK, (int) (0xFF * dimAmount)));
        invalidate();
    }

    @Override
    protected boolean onLongPress() {
        if (delegate != null) {
            return delegate.didLongPress(this, lastTouchX, lastTouchY);
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        rippleView.layout((int) giftButtonRect.left, (int) giftButtonRect.top, (int) giftButtonRect.right, (int) giftButtonRect.bottom);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
        imageReceiver.onDetachedFromWindow();
        setStarsPaused(true);
        wasLayout = false;
        AnimatedEmojiSpan.release(this, animatedEmojiStack);

        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didUpdatePremiumGiftStickers);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.diceStickersDidLoad);
        avatarStoryParams.onDetachFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        imageReceiver.onAttachedToWindow();
        setStarsPaused(false);

        animatedEmojiStack = AnimatedEmojiSpan.update(AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, this, canDrawInParent && (delegate != null && !delegate.canDrawOutboundsContent()), animatedEmojiStack, textLayout);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdatePremiumGiftStickers);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.diceStickersDidLoad);

        if (currentMessageObject != null && currentMessageObject.type == MessageObject.TYPE_SUGGEST_PHOTO) {
            setMessageObject(currentMessageObject, true);
        }
    }

    private void setStarsPaused(boolean paused) {
        if (paused == starParticlesDrawable.paused) {
            return;
        }
        starParticlesDrawable.paused = paused;
        if (paused) {
            starParticlesDrawable.pausedTime = System.currentTimeMillis();
        } else {
            for (int i = 0; i < starParticlesDrawable.particles.size(); i++) {
                starParticlesDrawable.particles.get(i).lifeTime += System.currentTimeMillis() - starParticlesDrawable.pausedTime;
            }
            invalidate();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        MessageObject messageObject = currentMessageObject;
        if (messageObject == null) {
            return super.onTouchEvent(event);
        }
        float x = lastTouchX = event.getX();
        float y = lastTouchY = event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (delegate != null) {
                if ((messageObject.type == MessageObject.TYPE_ACTION_PHOTO || isButtonLayout(messageObject)) && imageReceiver.isInsideImage(x, y)) {
                    imagePressed = true;
                    result = true;
                }
                if (radialProgress.getIcon() == MediaActionDrawable.ICON_NONE && (messageObject.type == MessageObject.TYPE_SUGGEST_PHOTO || messageObject.type == MessageObject.TYPE_ACTION_WALLPAPER) && backgroundRect.contains(x, y)) {
                    imagePressed = true;
                    result = true;
                }
                if (isButtonLayout(messageObject) && giftPremiumButtonLayout != null && (giftButtonRect.contains(x, y) || buttonClickableAsImage && backgroundRect.contains(x, y))) {
                    rippleView.setPressed(giftButtonPressed = true);
                    bounce.setPressed(true);
                    result = true;
                }
                if (result) {
                    startCheckLongPress();
                }
            }
        } else {
            if (event.getAction() != MotionEvent.ACTION_MOVE) {
                cancelCheckLongPress();
            }
            if (giftButtonPressed) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        imagePressed = false;
                        rippleView.setPressed(giftButtonPressed = false);
                        bounce.setPressed(false);
                        if (delegate != null) {
                            if (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM_CHANNEL) {
                                openPremiumGiftChannel();
                            } else if (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM) {
                                playSoundEffect(SoundEffectConstants.CLICK);
                                openPremiumGiftPreview();
                            } else {
                                ImageUpdater imageUpdater = MessagesController.getInstance(currentAccount).photoSuggestion.get(messageObject.messageOwner.local_id);
                                if (imageUpdater == null) {
                                    if (buttonClickableAsImage) {
                                        delegate.didClickImage(this);
                                    } else {
                                        delegate.didClickButton(this);
                                    }
                                }
                            }
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        imagePressed = false;
                        rippleView.setPressed(giftButtonPressed = false);
                        bounce.setPressed(false);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!(isButtonLayout(messageObject) && (giftButtonRect.contains(x, y) || backgroundRect.contains(x, y)))) {
                            rippleView.setPressed(giftButtonPressed = false);
                            bounce.setPressed(false);
                        }
                        break;
                }
            } else if (imagePressed) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        imagePressed = false;
                        if (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM_CHANNEL) {
                            openPremiumGiftChannel();
                        } else if (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM) {
                            openPremiumGiftPreview();
                        } else if (delegate != null) {
                            boolean consumed = false;
                            if (messageObject.type == MessageObject.TYPE_SUGGEST_PHOTO) {
                                ImageUpdater imageUpdater = MessagesController.getInstance(currentAccount).photoSuggestion.get(messageObject.messageOwner.local_id);
                                if (imageUpdater != null) {
                                    consumed = true;
                                    imageUpdater.cancel();
                                }
                            }
                            if (!consumed) {
                                delegate.didClickImage(this);
                                playSoundEffect(SoundEffectConstants.CLICK);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        imagePressed = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isNewStyleButtonLayout()) {
                            if (!backgroundRect.contains(x, y)) {
                                imagePressed = false;
                            }
                        } else {
                            if (!imageReceiver.isInsideImage(x, y)) {
                                imagePressed = false;
                            }
                        }
                        break;
                }
            }
        }
        if (!result) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || pressedLink != null && event.getAction() == MotionEvent.ACTION_UP) {
                if (textLayout != null && x >= textX && y >= textY && x <= textX + textWidth && y <= textY + textHeight) {
                    y -= textY;
                    x -= textXLeft;

                    final int line = textLayout.getLineForVertical((int) y);
                    final int off = textLayout.getOffsetForHorizontal(line, x);
                    final float left = textLayout.getLineLeft(line);
                    if (left <= x && left + textLayout.getLineWidth(line) >= x && messageObject.messageText instanceof Spannable) {
                        Spannable buffer = (Spannable) messageObject.messageText;
                        URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);

                        if (link.length != 0) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                pressedLink = link[0];
                                result = true;
                            } else {
                                if (link[0] == pressedLink) {
                                    openLink(pressedLink);
                                    result = true;
                                }
                            }
                        } else {
                            pressedLink = null;
                        }
                    } else {
                        pressedLink = null;
                    }
                } else {
                    pressedLink = null;
                }
            }
        }

        if (!result) {
            result = super.onTouchEvent(event);
        }

        return result;
    }

    private void openPremiumGiftChannel() {
        if (delegate != null) {
            TLRPC.TL_messageActionGiftCode gifCodeAction = (TLRPC.TL_messageActionGiftCode) currentMessageObject.messageOwner.action;
            AndroidUtilities.runOnUIThread(() -> delegate.didOpenPremiumGiftChannel(ChatActionCell.this, gifCodeAction.slug, false));
        }
    }

    private boolean isSelfGiftCode() {
        if (currentMessageObject != null && currentMessageObject.messageOwner.action instanceof TLRPC.TL_messageActionGiftCode) {
            if (currentMessageObject.messageOwner.from_id instanceof TLRPC.TL_peerUser) {
                return MessagesController.getInstance(currentAccount).getUser(currentMessageObject.messageOwner.from_id.user_id).self;
            }
        }
        return false;
    }

    private boolean isGiftCode() {
        return currentMessageObject != null && currentMessageObject.messageOwner.action instanceof TLRPC.TL_messageActionGiftCode;
    }

    private void openPremiumGiftPreview() {
        TLRPC.TL_premiumGiftOption giftOption = new TLRPC.TL_premiumGiftOption();
        TLRPC.MessageAction action = currentMessageObject.messageOwner.action;
        giftOption.amount = action.amount;
        giftOption.months = action.months;
        giftOption.currency = action.currency;
        String slug;
        if (isGiftCode()) {
            slug = isSelfGiftCode() ? null : ((TLRPC.TL_messageActionGiftCode) currentMessageObject.messageOwner.action).slug;
        } else {
            slug = null;
        }
        if (delegate != null) {
            AndroidUtilities.runOnUIThread(() -> delegate.didOpenPremiumGift(ChatActionCell.this, giftOption, slug, false));
        }
    }

    private void openLink(CharacterStyle link) {
        if (delegate != null && link instanceof URLSpan) {
            String url = ((URLSpan) link).getURL();
            if (url.startsWith("topic") && pressedLink instanceof URLSpanNoUnderline) {
                URLSpanNoUnderline spanNoUnderline = (URLSpanNoUnderline) pressedLink;
                TLObject object = spanNoUnderline.getObject();
                if (object instanceof TLRPC.TL_forumTopic) {
                    TLRPC.TL_forumTopic forumTopic = (TLRPC.TL_forumTopic) object;
                    ForumUtilities.openTopic(delegate.getBaseFragment(), -delegate.getDialogId(), forumTopic, 0);
                }
            } else if (url.startsWith("invite") && pressedLink instanceof URLSpanNoUnderline) {
                URLSpanNoUnderline spanNoUnderline = (URLSpanNoUnderline) pressedLink;
                TLObject object = spanNoUnderline.getObject();
                if (object instanceof TLRPC.TL_chatInviteExported) {
                    TLRPC.TL_chatInviteExported invite = (TLRPC.TL_chatInviteExported) object;
                    delegate.needOpenInviteLink(invite);
                }
            } else if (url.startsWith("game")) {
                delegate.didPressReplyMessage(this, currentMessageObject.getReplyMsgId());
                /*TLRPC.KeyboardButton gameButton = null;
                MessageObject messageObject = currentMessageObject.replyMessageObject;
                if (messageObject != null && messageObject.messageOwner.reply_markup != null) {
                    for (int a = 0; a < messageObject.messageOwner.reply_markup.rows.size(); a++) {
                        TLRPC.TL_keyboardButtonRow row = messageObject.messageOwner.reply_markup.rows.get(a);
                        for (int b = 0; b < row.buttons.size(); b++) {
                            TLRPC.KeyboardButton button = row.buttons.get(b);
                            if (button instanceof TLRPC.TL_keyboardButtonGame && button.game_id == currentMessageObject.messageOwner.action.game_id) {
                                gameButton = button;
                                break;
                            }
                        }
                        if (gameButton != null) {
                            break;
                        }
                    }
                }
                if (gameButton != null) {
                    delegate.didPressBotButton(messageObject, gameButton);
                }*/
            } else if (url.startsWith("http")) {
                Browser.openUrl(getContext(), url);
            } else {
                delegate.needOpenUserProfile(Long.parseLong(url));
            }
        }
    }

    private int overriddenMaxWidth;
    public void setOverrideTextMaxWidth(int width) {
        overriddenMaxWidth = width;
    }

    private void createLayout(CharSequence text, int width) {
        int maxWidth = width - dp(30);
        if (maxWidth < 0) {
            return;
        }
        if (overriddenMaxWidth > 0) {
            maxWidth = Math.min(overriddenMaxWidth, maxWidth);
        }
        invalidatePath = true;
        TextPaint paint;
        if (currentMessageObject != null && currentMessageObject.drawServiceWithDefaultTypeface) {
            paint = (TextPaint) getThemedPaint(Theme.key_paint_chatActionText2);
        } else {
            paint = (TextPaint) getThemedPaint(Theme.key_paint_chatActionText);
        }
        paint.linkColor = paint.getColor();
        textLayout = new StaticLayout(text, paint, maxWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        animatedEmojiStack = AnimatedEmojiSpan.update(AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, this, canDrawInParent && (delegate != null && !delegate.canDrawOutboundsContent()), animatedEmojiStack, textLayout);

        textHeight = 0;
        textWidth = 0;
        try {
            int linesCount = textLayout.getLineCount();
            for (int a = 0; a < linesCount; a++) {
                float lineWidth;
                try {
                    lineWidth = textLayout.getLineWidth(a);
                    if (lineWidth > maxWidth) {
                        lineWidth = maxWidth;
                    }
                    textHeight = (int) Math.max(textHeight, Math.ceil(textLayout.getLineBottom(a)));
                } catch (Exception e) {
                    FileLog.e(e);
                    return;
                }
                textWidth = (int) Math.max(textWidth, Math.ceil(lineWidth));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        textX = (width - textWidth) / 2;
        textY = dp(7);
        textXLeft = (width - textLayout.getWidth()) / 2;

        spoilersPool.addAll(spoilers);
        spoilers.clear();
        if (text instanceof Spannable) {
            SpoilerEffect.addSpoilers(this, textLayout, textX, textX + textWidth, (Spannable) text, spoilersPool, spoilers, null);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        MessageObject messageObject = currentMessageObject;
        if (messageObject == null && customText == null) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), textHeight + dp(14));
            return;
        }
        if (isButtonLayout(messageObject)) {
            giftRectSize = Math.min((int) (AndroidUtilities.isTablet() ? AndroidUtilities.getMinTabletSide() * 0.6f : AndroidUtilities.displaySize.x * 0.62f - dp(34)), AndroidUtilities.displaySize.y - ActionBar.getCurrentActionBarHeight() - AndroidUtilities.statusBarHeight - dp(64));
            if (!AndroidUtilities.isTablet() && messageObject.type == MessageObject.TYPE_GIFT_PREMIUM) {
                giftRectSize = (int) (giftRectSize * 1.2f);
            }
            stickerSize = giftRectSize - dp(106);
            if (isNewStyleButtonLayout()) {
                imageReceiver.setRoundRadius(ExteraConfig.getAvatarCorners(stickerSize * 0.7f, true));
            } else {
                imageReceiver.setRoundRadius(0);
            }
        }
        int width = Math.max(dp(30), MeasureSpec.getSize(widthMeasureSpec));
        if (previousWidth != width) {
            wasLayout = true;
            previousWidth = width;
            buildLayout();
        }
        int additionalHeight = 0;
        if (messageObject != null) {
            if (messageObject.type == MessageObject.TYPE_ACTION_PHOTO) {
                additionalHeight = AndroidUtilities.roundMessageSize + dp(10);
            } else if (isButtonLayout(messageObject)) {
                additionalHeight = giftRectSize + dp(12);
            }
        }

        int exactlyHeight = 0;
        if (isButtonLayout(messageObject)) {
            boolean isGiftChannel = isGiftChannel(messageObject);
            int imageSize = getImageSize(messageObject);
            float y;
            if (isNewStyleButtonLayout()) {
                y = textY + textHeight + dp(4) + dp(16) * 2 + imageSize + giftPremiumSubtitleLayout.getHeight() + dp(4);
            } else {
                y = textY + textHeight + giftRectSize * 0.075f + imageSize + dp(4) + dp(4) + giftPremiumSubtitleLayout.getHeight();
            }
            giftPremiumAdditionalHeight = 0;
            if (giftPremiumTitleLayout != null) {
                y += giftPremiumTitleLayout.getHeight();
                y += dp(isGiftChannel ? 6 : 0);
            } else {
                y -= dp(12);
                giftPremiumAdditionalHeight -= dp(30);
            }

            if (giftPremiumSubtitleLayout.getLineCount() > 2) {
                giftPremiumAdditionalHeight += (giftPremiumSubtitleLayout.getLineBottom(0) - giftPremiumSubtitleLayout.getLineTop(0)) * giftPremiumSubtitleLayout.getLineCount() - 2;
            }

            giftPremiumAdditionalHeight -= dp(isGiftChannel ? 14 : 0);

            additionalHeight += giftPremiumAdditionalHeight;

            int h = textHeight + additionalHeight + dp(14);

            if (giftPremiumButtonLayout != null) {
                y += (h - y - (giftPremiumButtonLayout != null ? giftPremiumButtonLayout.getHeight() : 0) - dp(8)) / 2f;
                float rectX = (previousWidth - giftPremiumButtonWidth) / 2f;
                giftButtonRect.set(rectX - dp(18), y - dp(8), rectX + giftPremiumButtonWidth + dp(18), y + (giftPremiumButtonLayout != null ? giftPremiumButtonLayout.getHeight() : 0) + dp(8));
            } else {
                additionalHeight -= dp(40);
                giftPremiumAdditionalHeight -= dp(40);
            }
            int sizeInternal = getMeasuredWidth() << 16 + getMeasuredHeight();
            starParticlesDrawable.rect.set(giftButtonRect);
            starParticlesDrawable.rect2.set(giftButtonRect);
            if (starsSize != sizeInternal) {
                starsSize = sizeInternal;
                starParticlesDrawable.resetPositions();
            }

            if (isNewStyleButtonLayout()) {
                exactlyHeight = textY + textHeight + dp(4);
                backgroundRectHeight = 0;
                backgroundRectHeight += dp(16) * 2 + imageSize;
                backgroundRectHeight += giftPremiumSubtitleLayout.getHeight();
                if (giftPremiumButtonLayout != null) {
                    backgroundButtonTop = exactlyHeight + backgroundRectHeight + dp(10);
                    float rectX = (previousWidth - giftPremiumButtonWidth) / 2f;
                    giftButtonRect.set(rectX - dp(18), backgroundButtonTop, rectX + giftPremiumButtonWidth + dp(18), backgroundButtonTop + giftPremiumButtonLayout.getHeight() + dp(8) * 2);
                    backgroundRectHeight += dp(10) + giftButtonRect.height();
                }
                backgroundRectHeight += dp(16);
                exactlyHeight += backgroundRectHeight;
                exactlyHeight += dp(6);
            }
        }
        if (messageObject != null && (isNewStyleButtonLayout())) {
            setMeasuredDimension(width, exactlyHeight);
        } else {
            setMeasuredDimension(width, textHeight + additionalHeight + dp(14));
        }
    }

    private boolean isNewStyleButtonLayout() {
        return currentMessageObject.type == MessageObject.TYPE_SUGGEST_PHOTO || currentMessageObject.type == MessageObject.TYPE_ACTION_WALLPAPER || currentMessageObject.isStoryMention();
    }

    private int getImageSize(MessageObject messageObject) {
        int imageSize = stickerSize;
        if (messageObject.type == MessageObject.TYPE_SUGGEST_PHOTO || isNewStyleButtonLayout()) {
            imageSize = dp(78);//Math.max(, (int) (stickerSize * 0.7f));
        }
        return imageSize;
    }

    private void buildLayout() {
        CharSequence text = null;
        MessageObject messageObject = currentMessageObject;
        if (messageObject != null) {
            if (messageObject.isExpiredStory()) {
                long dialogId = messageObject.messageOwner.media.user_id;
                if (dialogId != UserConfig.getInstance(currentAccount).getClientUserId()) {
                    text = StoriesUtilities.createExpiredStoryString(true, "ExpiredStoryMention", R.string.ExpiredStoryMention);
                } else {
                    text = StoriesUtilities.createExpiredStoryString(true, "ExpiredStoryMentioned", R.string.ExpiredStoryMentioned, MessagesController.getInstance(currentAccount).getUser(messageObject.getDialogId()).first_name);
                }
            } else if (delegate.getTopicId() == 0 && MessageObject.isTopicActionMessage(messageObject)) {
                TLRPC.TL_forumTopic topic = MessagesController.getInstance(currentAccount).getTopicsController().findTopic(-messageObject.getDialogId(), MessageObject.getTopicId(currentAccount, messageObject.messageOwner, true));
                text = ForumUtilities.createActionTextWithTopic(topic, messageObject);
            }
            if (text == null) {
                if (messageObject.messageOwner != null && messageObject.messageOwner.media != null && messageObject.messageOwner.media.ttl_seconds != 0) {
                    if (messageObject.messageOwner.media.photo != null) {
                        text = LocaleController.getString(R.string.AttachPhotoExpired);
                    } else if (messageObject.messageOwner.media.document instanceof TLRPC.TL_documentEmpty || messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument && messageObject.messageOwner.media.document == null) {
                        if (messageObject.messageOwner.media.voice) {
                            text = LocaleController.getString(R.string.AttachVoiceExpired);
                        } else if (messageObject.messageOwner.media.round) {
                            text = LocaleController.getString(R.string.AttachRoundExpired);
                        } else {
                            text = LocaleController.getString(R.string.AttachVideoExpired);
                        }
                    } else {
                        text = AnimatedEmojiSpan.cloneSpans(messageObject.messageText);
                    }
                } else {
                    text = AnimatedEmojiSpan.cloneSpans(messageObject.messageText);
                    if (currentMessageObject.messageOwner != null && ExteraConfig.showActionTimestamps) {
                        if (currentMessageObject.currentEvent != null || currentMessageObject.messageOwner.action != null) {
                            boolean d = text.charAt(text.length() - 1) == ':';
                            long date = currentMessageObject.messageOwner.date;
                            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
                            if (d)
                                ssb.delete(text.length() - 1, text.length());
                            ssb.append(" ").append(LocaleController.formatString("TodayAtFormatted", R.string.TodayAtFormatted, LocaleController.getInstance().formatterDay.format(date * 1000)));
                            text = ssb + (d ? ":" : "");
                        }
                    }
                }
            }
        } else {
            text = customText;
        }
        createLayout(text, previousWidth);
        if (messageObject != null) {
            if (messageObject.type == MessageObject.TYPE_ACTION_PHOTO) {
                imageReceiver.setImageCoords((previousWidth - AndroidUtilities.roundMessageSize) / 2f, textHeight + dp(19), AndroidUtilities.roundMessageSize, AndroidUtilities.roundMessageSize);
            } else if (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM_CHANNEL) {
                createGiftPremiumChannelLayouts();
            } else if (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM) {
                String actionName = isGiftCode() && !isSelfGiftCode() ? LocaleController.getString("GiftPremiumUseGiftBtn", R.string.GiftPremiumUseGiftBtn) :
                        LocaleController.getString("ActionGiftPremiumView", R.string.ActionGiftPremiumView);
                createGiftPremiumLayouts(LocaleController.getString(R.string.ActionGiftPremiumTitle), LocaleController.formatString(R.string.ActionGiftPremiumSubtitle, LocaleController.formatPluralString("Months", messageObject.messageOwner.action.months)), actionName, giftRectSize, true);
            } else if (messageObject.type == MessageObject.TYPE_SUGGEST_PHOTO) {
                TLRPC.TL_messageActionSuggestProfilePhoto actionSuggestProfilePhoto = (TLRPC.TL_messageActionSuggestProfilePhoto) messageObject.messageOwner.action;
                String description;
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(messageObject.isOutOwner() ? 0 : messageObject.getDialogId());
                boolean isVideo = actionSuggestProfilePhoto.video || (actionSuggestProfilePhoto.photo != null && actionSuggestProfilePhoto.photo.video_sizes != null && !actionSuggestProfilePhoto.photo.video_sizes.isEmpty());
                if (user.id == UserConfig.getInstance(currentAccount).clientUserId) {
                    TLRPC.User user2 = MessagesController.getInstance(currentAccount).getUser(messageObject.getDialogId());
                    if (isVideo) {
                        description = LocaleController.formatString(R.string.ActionSuggestVideoFromYouDescription, user2.first_name);
                    } else {
                        description = LocaleController.formatString(R.string.ActionSuggestPhotoFromYouDescription, user2.first_name);
                    }
                } else {
                    if (isVideo) {
                        description = LocaleController.formatString(R.string.ActionSuggestVideoToYouDescription, user.first_name);
                    } else {
                        description = LocaleController.formatString(R.string.ActionSuggestPhotoToYouDescription, user.first_name);
                    }
                }
                String action;
                if (actionSuggestProfilePhoto.video || (actionSuggestProfilePhoto.photo.video_sizes != null && !actionSuggestProfilePhoto.photo.video_sizes.isEmpty())) {
                    action = LocaleController.getString(R.string.ViewVideoAction);
                } else {
                    action = LocaleController.getString(R.string.ViewPhotoAction);
                }
                createGiftPremiumLayouts(null, description, action, giftRectSize, true);
                textLayout = null;
                textHeight = 0;
                textY = 0;
            } else if (messageObject.type == MessageObject.TYPE_ACTION_WALLPAPER) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(messageObject.isOutOwner() ? 0 : messageObject.getDialogId());
                CharSequence description;
                String action = null;
                boolean actionClickableAsImage = true;
                if (messageObject.getDialogId() < 0) {
                    description = messageObject.messageText;
                } else if (!messageObject.isOutOwner() && messageObject.isWallpaperForBoth() && messageObject.isCurrentWallpaper()) {
                    description = messageObject.messageText;
                    action = LocaleController.getString(R.string.RemoveWallpaperAction);
                    actionClickableAsImage = false;
                } else if (user != null && user.id == UserConfig.getInstance(currentAccount).clientUserId) {
                    description = messageObject.messageText;
                } else {
                    description = messageObject.messageText;
                    action = LocaleController.getString(R.string.ViewWallpaperAction);
                }
                createGiftPremiumLayouts(null, description, action, giftRectSize, actionClickableAsImage);
                textLayout = null;
                textHeight = 0;
                textY = 0;
            } else if (messageObject.isStoryMention()) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(messageObject.messageOwner.media.user_id);
                CharSequence description;
                String action = null;

                if (user.self) {
                    TLRPC.User user2 = MessagesController.getInstance(currentAccount).getUser(messageObject.getDialogId());
                    description = AndroidUtilities.replaceTags(LocaleController.formatString("StoryYouMentionedTitle", R.string.StoryYouMentionedTitle, user2.first_name));
                } else {
                    description = AndroidUtilities.replaceTags(LocaleController.formatString("StoryMentionedTitle", R.string.StoryMentionedTitle, user.first_name));
                }
                action = LocaleController.getString(R.string.StoryMentionedAction);

                createGiftPremiumLayouts(null, description, action, giftRectSize, true);
                textLayout = null;
                textHeight = 0;
                textY = 0;
            }
        }
    }

    private void createGiftPremiumChannelLayouts() {
        int width = giftRectSize;
        width -= dp(16);
        giftTitlePaint.setTextSize(dp(14));
        giftSubtitlePaint.setTextSize(dp(13));
        TLRPC.TL_messageActionGiftCode gifCodeAction = (TLRPC.TL_messageActionGiftCode) currentMessageObject.messageOwner.action;
        int months = gifCodeAction.months;
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-DialogObject.getPeerDialogId(gifCodeAction.boost_peer));
        String from = chat == null ? null : chat.title;
        boolean isPrize = gifCodeAction.via_giveaway;
        CharSequence title = gifCodeAction.unclaimed ?
                LocaleController.getString("BoostingUnclaimedPrize", R.string.BoostingUnclaimedPrize)
                : LocaleController.getString("BoostingCongratulations", R.string.BoostingCongratulations);
        SpannableStringBuilder subtitle;
        CharSequence monthsStr = months == 12 ? LocaleController.formatPluralString("BoldYears", 1) : LocaleController.formatPluralString("BoldMonths", months);
        if (isPrize) {
            if (gifCodeAction.unclaimed) {
                subtitle = new SpannableStringBuilder(AndroidUtilities.replaceTags(LocaleController.formatString("BoostingYouHaveUnclaimedPrize", R.string.BoostingYouHaveUnclaimedPrize, from)));
                subtitle.append("\n\n");
                subtitle.append(AndroidUtilities.replaceTags(LocaleController.formatString("BoostingUnclaimedPrizeDuration", R.string.BoostingUnclaimedPrizeDuration, monthsStr)));
            } else {
                subtitle = new SpannableStringBuilder(AndroidUtilities.replaceTags(LocaleController.formatString("BoostingReceivedPrizeFrom", R.string.BoostingReceivedPrizeFrom, from)));
                subtitle.append("\n\n");
                subtitle.append(AndroidUtilities.replaceTags(LocaleController.formatString("BoostingReceivedPrizeDuration", R.string.BoostingReceivedPrizeDuration, monthsStr)));
            }
        } else {
            subtitle = new SpannableStringBuilder(AndroidUtilities.replaceTags(from == null ? LocaleController.getString("BoostingReceivedGiftNoName", R.string.BoostingReceivedGiftNoName) : LocaleController.formatString("BoostingReceivedGiftFrom", R.string.BoostingReceivedGiftFrom, from)));
            subtitle.append("\n\n");
            subtitle.append(AndroidUtilities.replaceTags(LocaleController.formatString("BoostingReceivedGiftDuration", R.string.BoostingReceivedGiftDuration, monthsStr)));
        }

        String btnText = LocaleController.getString("BoostingReceivedGiftOpenBtn", R.string.BoostingReceivedGiftOpenBtn);

        SpannableStringBuilder titleBuilder = SpannableStringBuilder.valueOf(title);
        titleBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM)), 0, titleBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        giftPremiumTitleLayout = new StaticLayout(titleBuilder, giftTitlePaint, width, Layout.Alignment.ALIGN_CENTER, 1.1f, 0.0f, false);

        giftPremiumSubtitleWidth = width;
        giftPremiumSubtitleLayout = new StaticLayout(subtitle, giftSubtitlePaint, width, Layout.Alignment.ALIGN_CENTER, 1.1f, 0.0f, false);
        SpannableStringBuilder buttonBuilder = SpannableStringBuilder.valueOf(btnText);
        buttonBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM)), 0, buttonBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        giftPremiumButtonLayout = new StaticLayout(buttonBuilder, (TextPaint) getThemedPaint(Theme.key_paint_chatActionText), width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        buttonClickableAsImage = true;
        giftPremiumButtonWidth = measureLayoutWidth(giftPremiumButtonLayout);
    }

    private void createGiftPremiumLayouts(CharSequence title, CharSequence subtitle, CharSequence button, int width, boolean buttonClickableAsImage) {
        width -= dp(16);
        if (title != null) {
            giftTitlePaint.setTextSize(dp(16));
            SpannableStringBuilder titleBuilder = SpannableStringBuilder.valueOf(title);
            titleBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM)), 0, titleBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            giftPremiumTitleLayout = new StaticLayout(titleBuilder, giftTitlePaint, width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        } else {
            giftPremiumTitleLayout = null;
        }
        if (currentMessageObject != null && isNewStyleButtonLayout()) {
            giftSubtitlePaint.setTextSize(dp(13));
        } else {
            giftSubtitlePaint.setTextSize(dp(15));
        }
        int subtitleWidth = giftPremiumSubtitleWidth = width;
        if (currentMessageObject != null && (currentMessageObject.type == MessageObject.TYPE_ACTION_WALLPAPER && currentMessageObject.getDialogId() >= 0)) {
            final int recommendedWidthForTwoLines = HintView2.cutInFancyHalf(subtitle, giftSubtitlePaint);
            if (recommendedWidthForTwoLines < subtitleWidth && recommendedWidthForTwoLines > subtitleWidth / 5f) {
                subtitleWidth = recommendedWidthForTwoLines;
            }
        }
        try {
            subtitle = Emoji.replaceEmoji(subtitle, giftSubtitlePaint.getFontMetricsInt(), false);
        } catch (Exception ignore) {}
        giftPremiumSubtitleLayout = new StaticLayout(subtitle, giftSubtitlePaint, subtitleWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, dp(1.66f), false);
        if (button != null) {
            SpannableStringBuilder buttonBuilder = SpannableStringBuilder.valueOf(button);
            buttonBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM)), 0, buttonBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            giftPremiumButtonLayout = new StaticLayout(buttonBuilder, (TextPaint) getThemedPaint(Theme.key_paint_chatActionText), width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
            this.buttonClickableAsImage = buttonClickableAsImage;
            giftPremiumButtonWidth = measureLayoutWidth(giftPremiumButtonLayout);
        } else {
            giftPremiumButtonLayout = null;
            this.buttonClickableAsImage = false;
            giftPremiumButtonWidth = 0;
        }
    }

    private float measureLayoutWidth(Layout layout) {
        float maxWidth = 0;
        for (int i = 0; i < layout.getLineCount(); i++) {
            int lineWidth = (int) Math.ceil(layout.getLineWidth(i));
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
            }
        }
        return maxWidth;
    }

    public boolean showingCancelButton() {
        return radialProgress != null && radialProgress.getIcon() == MediaActionDrawable.ICON_CANCEL;
    }

    public int getCustomDate() {
        return customDate;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        MessageObject messageObject = currentMessageObject;
        int imageSize = stickerSize;
        if (isButtonLayout(messageObject)) {
            stickerSize = giftRectSize - dp(106);
            if (isNewStyleButtonLayout()) {
                imageSize = getImageSize(messageObject);
                int top = textY + textHeight + dp(4) + dp(16);
                float x = (previousWidth - imageSize) / 2f;
                float y = top;
                if (messageObject.isStoryMention()) {
                    avatarStoryParams.storyItem = messageObject.messageOwner.media.storyItem;
                }
                avatarStoryParams.originalAvatarRect.set(x, y, x + imageSize, y + imageSize);
                imageReceiver.setImageCoords(x, y, imageSize, imageSize);
                imageReceiver.setRoundRadius(ExteraConfig.getAvatarCorners(imageSize, true));
            } else if (messageObject.type == MessageObject.TYPE_ACTION_PHOTO) {
                imageReceiver.setImageCoords((previousWidth - stickerSize) / 2f, textY + textHeight + giftRectSize * 0.075f, stickerSize, stickerSize);
            } else if (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM_CHANNEL) {
                imageSize = (int) (stickerSize * (AndroidUtilities.isTablet() ? 1.0f : 1.2f));
                imageReceiver.setImageCoords((previousWidth - imageSize) / 2f, textY + textHeight + giftRectSize * 0.075f - dp(22), imageSize, imageSize);
            } else {
                imageSize = (int) (stickerSize * 1f);
                imageReceiver.setImageCoords((previousWidth - imageSize) / 2f, textY + textHeight + giftRectSize * 0.075f - dp(4), imageSize, imageSize);
            }
            textPaint = (TextPaint) getThemedPaint(Theme.key_paint_chatActionText);
            if (textPaint != null) {
                if (giftTitlePaint != null && giftTitlePaint.getColor() != textPaint.getColor()) {
                    giftTitlePaint.setColor(textPaint.getColor());
                }
                if (giftSubtitlePaint != null && giftSubtitlePaint.getColor() != textPaint.getColor()) {
                    giftSubtitlePaint.setColor(textPaint.getColor());
                    giftSubtitlePaint.linkColor = textPaint.getColor();
                }
            }
        }

        drawBackground(canvas, false);

        if (isButtonLayout(messageObject) || (messageObject != null && messageObject.type == MessageObject.TYPE_ACTION_PHOTO)) {
            if (wallpaperPreviewDrawable != null) {
                canvas.save();
                canvas.translate(imageReceiver.getImageX(), imageReceiver.getImageY());
                if (clipPath == null) {
                    clipPath = new Path();
                } else {
                    clipPath.rewind();
                }
                clipPath.addCircle(imageReceiver.getImageWidth() / 2f, imageReceiver.getImageHeight() / 2f, imageReceiver.getImageWidth() / 2f, Path.Direction.CW);
                canvas.clipPath(clipPath);
                wallpaperPreviewDrawable.setBounds(0, 0, (int) imageReceiver.getImageWidth(), (int) imageReceiver.getImageHeight());
                wallpaperPreviewDrawable.draw(canvas);
                canvas.restore();
            } else if (messageObject.isStoryMention()) {
                long dialogId = messageObject.messageOwner.media.user_id;
                avatarStoryParams.storyId = messageObject.messageOwner.media.id;
                StoriesUtilities.drawAvatarWithStory(dialogId, canvas, imageReceiver, avatarStoryParams);
             //   imageReceiver.draw(canvas);
            } else {
                imageReceiver.draw(canvas);
            }
            radialProgress.setProgressRect(
                    imageReceiver.getImageX(),
                    imageReceiver.getImageY(),
                    imageReceiver.getImageX() + imageReceiver.getImageWidth(),
                    imageReceiver.getImageY() + imageReceiver.getImageHeight()
            );
            if (messageObject.type == MessageObject.TYPE_SUGGEST_PHOTO) {
                ImageUpdater imageUpdater = MessagesController.getInstance(currentAccount).photoSuggestion.get(messageObject.messageOwner.local_id);
                if (imageUpdater != null) {
                    radialProgress.setProgress(imageUpdater.getCurrentImageProgress(), true);
                    radialProgress.setCircleRadius((int) (imageReceiver.getImageWidth() * 0.5f) + 1);
                    radialProgress.setMaxIconSize(dp(24));
                    radialProgress.setColorKeys(Theme.key_chat_mediaLoaderPhoto, Theme.key_chat_mediaLoaderPhotoSelected, Theme.key_chat_mediaLoaderPhotoIcon, Theme.key_chat_mediaLoaderPhotoIconSelected);
                    if (imageUpdater.getCurrentImageProgress() == 1f) {
                        radialProgress.setIcon(MediaActionDrawable.ICON_NONE, true, true);
                    } else {
                        radialProgress.setIcon(MediaActionDrawable.ICON_CANCEL, true, true);
                    }
                }
                radialProgress.draw(canvas);
            } else if (messageObject.type == MessageObject.TYPE_ACTION_WALLPAPER) {
                float progress = getUploadingInfoProgress(messageObject);
                radialProgress.setProgress(progress, true);
                radialProgress.setCircleRadius(dp(26));
                radialProgress.setMaxIconSize(dp(24));
                radialProgress.setColorKeys(Theme.key_chat_mediaLoaderPhoto, Theme.key_chat_mediaLoaderPhotoSelected, Theme.key_chat_mediaLoaderPhotoIcon, Theme.key_chat_mediaLoaderPhotoIconSelected);
                if (progress == 1f) {
                    radialProgress.setIcon(MediaActionDrawable.ICON_NONE, true, true);
                } else {
                    radialProgress.setIcon(MediaActionDrawable.ICON_CANCEL, true, true);
                }
                radialProgress.draw(canvas);
            }
        }

        if (textPaint != null && textLayout != null) {
            canvas.save();
            canvas.translate(textXLeft, textY);
            if (textLayout.getPaint() != textPaint) {
                buildLayout();
            }
            canvas.save();
            SpoilerEffect.clipOutCanvas(canvas, spoilers);
            textLayout.draw(canvas);
            if (delegate == null || delegate.canDrawOutboundsContent()) {
                AnimatedEmojiSpan.drawAnimatedEmojis(canvas, textLayout, animatedEmojiStack, 0, spoilers, 0, 0, 0, 1f, textLayout == null ? null : getAdaptiveEmojiColorFilter(textLayout.getPaint().getColor()));
            }
            canvas.restore();

            for (SpoilerEffect eff : spoilers) {
                eff.setColor(textLayout.getPaint().getColor());
                eff.draw(canvas);
            }

            canvas.restore();
        }

        if (isButtonLayout(messageObject)) {
            canvas.save();
            float x = (previousWidth - giftRectSize) / 2f + dp(8);
            float y;
            if (isNewStyleButtonLayout()) {
                float top = backgroundRect != null ? backgroundRect.top : (textY + textHeight + dp(4));
                y = top + dp(16) * 2 + imageSize;
            } else {
                y = textY + textHeight + giftRectSize * 0.075f + (messageObject.type == MessageObject.TYPE_SUGGEST_PHOTO ? imageSize : stickerSize) + dp(4);
                if (messageObject.type == MessageObject.TYPE_SUGGEST_PHOTO) {
                    y += dp(16);
                }
                if (giftPremiumButtonLayout == null) {
                    y -= dp(24);
                }
            }

            canvas.translate(x, y);
            if (giftPremiumTitleLayout != null) {
                giftPremiumTitleLayout.draw(canvas);
                y += giftPremiumTitleLayout.getHeight();
                y += dp(messageObject.type == MessageObject.TYPE_GIFT_PREMIUM_CHANNEL ? 6 : 0);
            } else {
                y -= dp(4);
            }
            canvas.restore();

            y += dp(4);
            canvas.save();
            canvas.translate(x, y);
            if (messageObject.type == MessageObject.TYPE_ACTION_WALLPAPER) {
                if (radialProgress.getTransitionProgress() != 1f || radialProgress.getIcon() != MediaActionDrawable.ICON_NONE) {
                    if (settingWallpaperLayout == null) {
                        settingWallpaperPaint = new TextPaint();
                        settingWallpaperPaint.setTextSize(dp(13));
                        SpannableStringBuilder cs = new SpannableStringBuilder(LocaleController.getString(R.string.ActionSettingWallpaper));
                        int index = cs.toString().indexOf("..."), len = 3;
                        if (index < 0) {
                            index = cs.toString().indexOf("…");
                            len = 1;
                        }
                        if (index >= 0) {
                            SpannableString loading = new SpannableString("…");
                            UploadingDotsSpannable loadingDots = new UploadingDotsSpannable();
                            loadingDots.fixTop = true;
                            loadingDots.setParent(ChatActionCell.this, false);
                            loading.setSpan(loadingDots, 0, loading.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            cs.replace(index, index + len, loading);
                        }
                        settingWallpaperLayout = new StaticLayout(cs, settingWallpaperPaint, giftPremiumSubtitleWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                    }
                    float progressLocal = getUploadingInfoProgress(messageObject);
                    if (settingWallpaperProgressTextLayout == null || settingWallpaperProgress != progressLocal) {
                        settingWallpaperProgress = progressLocal;
                        settingWallpaperProgressTextLayout = new StaticLayout((int) (progressLocal * 100) + "%", giftSubtitlePaint, giftPremiumSubtitleWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                    }

                    settingWallpaperPaint.setColor(giftSubtitlePaint.getColor());
                    if (radialProgress.getIcon() == MediaActionDrawable.ICON_NONE) {
                        float p = radialProgress.getTransitionProgress();
                        int oldColor = giftSubtitlePaint.getColor();
                        settingWallpaperPaint.setAlpha((int) (Color.alpha(oldColor) * (1f - p)));
                        giftSubtitlePaint.setAlpha((int) (Color.alpha(oldColor) * p));
                        giftSubtitlePaint.linkColor = giftSubtitlePaint.getColor();

                        float s = 0.8f + 0.2f * p;
                        canvas.save();
                        canvas.scale(s, s, giftPremiumSubtitleWidth / 2f, giftPremiumSubtitleLayout.getHeight() / 2f);
                        canvas.translate((giftPremiumSubtitleWidth -giftPremiumSubtitleLayout.getWidth()) / 2f, 0);
                        giftPremiumSubtitleLayout.draw(canvas);
                        canvas.restore();

                        giftSubtitlePaint.setAlpha((int) (Color.alpha(oldColor) * (1f - p)));
                        giftSubtitlePaint.linkColor = giftSubtitlePaint.getColor();
                        s = 0.8f + 0.2f * (1f - p);
                        canvas.save();
                        canvas.scale(s, s, settingWallpaperLayout.getWidth() / 2f, settingWallpaperLayout.getHeight() / 2f);
                        settingWallpaperLayout.draw(canvas);
                        canvas.restore();

                        canvas.save();
                        canvas.translate(0, settingWallpaperLayout.getHeight() + dp(4));
                        canvas.scale(s, s, settingWallpaperProgressTextLayout.getWidth() / 2f, settingWallpaperProgressTextLayout.getHeight() / 2f);
                        settingWallpaperProgressTextLayout.draw(canvas);
                        canvas.restore();


                        giftSubtitlePaint.setColor(oldColor);
                        giftSubtitlePaint.linkColor = oldColor;
                    } else {
                        settingWallpaperLayout.draw(canvas);
                        canvas.save();
                        canvas.translate(0, settingWallpaperLayout.getHeight() + dp(4));
                        settingWallpaperProgressTextLayout.draw(canvas);
                        canvas.restore();
                    }
                } else {
                    canvas.save();
                    canvas.translate((giftPremiumSubtitleWidth - giftPremiumSubtitleLayout.getWidth()) / 2f, 0);
                    giftPremiumSubtitleLayout.draw(canvas);
                    canvas.restore();
                }
            } else if (giftPremiumSubtitleLayout != null) {
                canvas.save();
                canvas.translate((giftPremiumSubtitleWidth - giftPremiumSubtitleLayout.getWidth()) / 2f, 0);
                giftPremiumSubtitleLayout.draw(canvas);
                canvas.restore();
            }
            canvas.restore();

            if (giftPremiumTitleLayout == null) {
                y -= dp(8);
            }

            y += giftPremiumSubtitleLayout.getHeight();
            int buttonH = giftPremiumButtonLayout != null ? giftPremiumButtonLayout.getHeight() : 0;
            y += (getHeight() - y - buttonH - dp(8)) / 2f;

            if (themeDelegate != null) {
                themeDelegate.applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, viewTranslationX, viewTop + dp(4));
            } else {
                Theme.applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, viewTranslationX, viewTop + dp(4));
            }

            final float S = bounce.getScale(0.02f);
            canvas.save();
            canvas.scale(S, S, giftButtonRect.centerX(), giftButtonRect.centerY());

            if (giftPremiumButtonLayout != null) {
                Paint backgroundPaint = getThemedPaint(Theme.key_paint_chatActionBackgroundSelected);
                canvas.drawRoundRect(giftButtonRect, dp(16), dp(16), backgroundPaint);
                if (hasGradientService()) {
                    canvas.drawRoundRect(giftButtonRect, dp(16), dp(16), getThemedPaint(Theme.key_paint_chatActionBackgroundDarken));
                }
                if (dimAmount > 0) {
                    canvas.drawRoundRect(giftButtonRect, dp(16), dp(16), dimPaint);
                }

                if (getMessageObject().type != MessageObject.TYPE_SUGGEST_PHOTO && getMessageObject().type != MessageObject.TYPE_ACTION_WALLPAPER && getMessageObject().type != MessageObject.TYPE_STORY_MENTION) {
                    starsPath.rewind();
                    starsPath.addRoundRect(giftButtonRect, dp(16), dp(16), Path.Direction.CW);
                    canvas.save();
                    canvas.clipPath(starsPath);

                    starParticlesDrawable.onDraw(canvas);
                    if (!starParticlesDrawable.paused) {
                        invalidate();
                    }
                    canvas.restore();
                } else {
                    //TODO optimize
                    invalidate();
                }
            }

            if (messageObject.settingAvatar && progressToProgress != 1f) {
                progressToProgress += 16 / 150f;
            } else if (!messageObject.settingAvatar && progressToProgress != 0) {
                progressToProgress -= 16 / 150f;
            }
            progressToProgress = Utilities.clamp(progressToProgress, 1f, 0f);
            if (progressToProgress != 0) {
                if (progressView == null) {
                    progressView = new RadialProgressView(getContext());
                }
                int rad = dp(16);
                canvas.save();
                canvas.scale(progressToProgress, progressToProgress, giftButtonRect.centerX(), giftButtonRect.centerY());
                progressView.setSize(rad);
                progressView.setProgressColor(Theme.getColor(Theme.key_chat_serviceText));
                progressView.draw(canvas, giftButtonRect.centerX(), giftButtonRect.centerY());
                canvas.restore();
            }
            if (progressToProgress != 1f && giftPremiumButtonLayout != null) {
                canvas.save();
                float s = 1f - progressToProgress;
                canvas.scale(s, s, giftButtonRect.centerX(), giftButtonRect.centerY());
                canvas.translate(x, giftButtonRect.top + dp(8));
                giftPremiumButtonLayout.draw(canvas);
                canvas.restore();
            }

            if (messageObject.flickerLoading) {
                if (loadingDrawable == null) {
                    loadingDrawable = new LoadingDrawable(themeDelegate);
                    loadingDrawable.setGradientScale(2f);
                    loadingDrawable.setAppearByGradient(true);
                    loadingDrawable.setColors(
                        Theme.multAlpha(Color.WHITE, .08f),
                        Theme.multAlpha(Color.WHITE, .2f),
                        Theme.multAlpha(Color.WHITE, .2f),
                        Theme.multAlpha(Color.WHITE, .7f)
                    );
                    loadingDrawable.strokePaint.setStrokeWidth(dp(1));
                }
                loadingDrawable.resetDisappear();
                loadingDrawable.setBounds(giftButtonRect);
                loadingDrawable.setRadiiDp(16);
                loadingDrawable.draw(canvas);
            } else if (loadingDrawable != null) {
                loadingDrawable.setBounds(giftButtonRect);
                loadingDrawable.setRadiiDp(16);
                loadingDrawable.disappear();
                loadingDrawable.draw(canvas);
                if (loadingDrawable.isDisappeared()) {
                    loadingDrawable.reset();
                }
            }

            canvas.restore();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child == rippleView) {
            final float S = bounce.getScale(0.02f);
            canvas.save();
            canvas.scale(S, S, child.getX() + child.getMeasuredWidth() / 2f, child.getY() + child.getMeasuredHeight() / 2f);
            final boolean r = super.drawChild(canvas, child, drawingTime);
            canvas.restore();
            return r;
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private void checkLeftRightBounds() {
        backgroundLeft = (int) Math.min(backgroundLeft, rect.left);
        backgroundRight = (int) Math.max(backgroundRight, rect.right);
    }

    public void drawBackground(Canvas canvas, boolean fromParent) {
        if (canDrawInParent) {
            if (hasGradientService() && !fromParent) {
                return;
            }
            if (!hasGradientService() && fromParent) {
                return;
            }
        }
        Paint backgroundPaint = getThemedPaint(Theme.key_paint_chatActionBackground);
        Paint darkenBackgroundPaint = getThemedPaint(Theme.key_paint_chatActionBackgroundDarken);
        textPaint = (TextPaint) getThemedPaint(Theme.key_paint_chatActionText);
        if (overrideBackground >= 0) {
            int color = getThemedColor(overrideBackground);
            if (overrideBackgroundPaint == null) {
                overrideBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                overrideBackgroundPaint.setColor(color);
                overrideTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                overrideTextPaint.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
                overrideTextPaint.setTextSize(dp(Math.max(16, SharedConfig.fontSize) - 2));
                overrideTextPaint.setColor(getThemedColor(overrideText));
            }
            backgroundPaint = overrideBackgroundPaint;
            textPaint = overrideTextPaint;
        }
        if (invalidatePath) {
            invalidatePath = false;
            backgroundLeft = getWidth();
            backgroundRight = 0;
            lineWidths.clear();
            final int count = textLayout == null ? 0 : textLayout.getLineCount();
            final int corner = dp(11);
            final int cornerIn = dp(8);

            int prevLineWidth = 0;
            for (int a = 0; a < count; a++) {
                int lineWidth = (int) Math.ceil(textLayout.getLineWidth(a));
                if (a != 0) {
                    int diff = prevLineWidth - lineWidth;
                    if (diff > 0 && diff <= 1.5f * corner + cornerIn) {
                        lineWidth = prevLineWidth;
                    }
                }
                lineWidths.add(lineWidth);
                prevLineWidth = lineWidth;
            }
            for (int a = count - 2; a >= 0; a--) {
                int lineWidth = lineWidths.get(a);
                int diff = prevLineWidth - lineWidth;
                if (diff > 0 && diff <= 1.5f * corner + cornerIn) {
                    lineWidth = prevLineWidth;
                }
                lineWidths.set(a, lineWidth);
                prevLineWidth = lineWidth;
            }

            int y = dp(4);
            int x = getMeasuredWidth() / 2;
            int previousLineBottom = 0;

            final int cornerOffset = dp(3);
            final int cornerInSmall = dp(6);
            final int cornerRest = corner - cornerOffset;

            lineHeights.clear();
            backgroundPath.reset();
            backgroundPath.moveTo(x, y);

            for (int a = 0; a < count; a++) {
                int lineWidth = lineWidths.get(a);
                int lineBottom = textLayout.getLineBottom(a);
                int nextLineWidth = a < count - 1 ? lineWidths.get(a + 1) : 0;

                int height = lineBottom - previousLineBottom;
                if (a == 0 || lineWidth > prevLineWidth) {
                    height += dp(3);
                }
                if (a == count - 1 || lineWidth > nextLineWidth) {
                    height += dp(3);
                }

                previousLineBottom = lineBottom;

                float startX = x + lineWidth / 2.0f;

                int innerCornerRad;
                if (a != count - 1 && lineWidth < nextLineWidth && a != 0 && lineWidth < prevLineWidth) {
                    innerCornerRad = cornerInSmall;
                } else {
                    innerCornerRad = cornerIn;
                }

                if (a == 0 || lineWidth > prevLineWidth) {
                    rect.set(startX - cornerOffset - corner, y, startX + cornerRest, y + corner * 2);
                    checkLeftRightBounds();
                    backgroundPath.arcTo(rect, -90, 90);
                } else if (lineWidth < prevLineWidth) {
                    rect.set(startX + cornerRest, y, startX + cornerRest + innerCornerRad * 2, y + innerCornerRad * 2);
                    checkLeftRightBounds();
                    backgroundPath.arcTo(rect, -90, -90);
                }
                y += height;
                int yOffset = y;
                if (a != count - 1 && lineWidth < nextLineWidth) {
                    y -= dp(3);
                    height -= dp(3);
                }
                if (a != 0 && lineWidth < prevLineWidth) {
                    y -= dp(3);
                    height -= dp(3);
                }
                lineHeights.add(height);

                if (a == count - 1 || lineWidth > nextLineWidth) {
                    rect.set(startX - cornerOffset - corner, y - corner * 2, startX + cornerRest, y);
                    checkLeftRightBounds();
                    backgroundPath.arcTo(rect, 0, 90);
                } else if (lineWidth < nextLineWidth) {
                    rect.set(startX + cornerRest, y - innerCornerRad * 2, startX + cornerRest + innerCornerRad * 2, y);
                    checkLeftRightBounds();
                    backgroundPath.arcTo(rect, 180, -90);
                }

                prevLineWidth = lineWidth;
            }
            for (int a = count - 1; a >= 0; a--) {
                prevLineWidth = a != 0 ? lineWidths.get(a - 1) : 0;
                int lineWidth = lineWidths.get(a);
                int nextLineWidth = a != count - 1 ? lineWidths.get(a + 1) : 0;
                int lineBottom = textLayout.getLineBottom(a);
                float startX = x - lineWidth / 2;

                int innerCornerRad;
                if (a != count - 1 && lineWidth < nextLineWidth && a != 0 && lineWidth < prevLineWidth) {
                    innerCornerRad = cornerInSmall;
                } else {
                    innerCornerRad = cornerIn;
                }

                if (a == count - 1 || lineWidth > nextLineWidth) {
                    rect.set(startX - cornerRest, y - corner * 2, startX + cornerOffset + corner, y);
                    checkLeftRightBounds();
                    backgroundPath.arcTo(rect, 90, 90);
                } else if (lineWidth < nextLineWidth) {
                    rect.set(startX - cornerRest - innerCornerRad * 2, y - innerCornerRad * 2, startX - cornerRest, y);
                    checkLeftRightBounds();
                    backgroundPath.arcTo(rect, 90, -90);
                }

                y -= lineHeights.get(a);

                if (a == 0 || lineWidth > prevLineWidth) {
                    rect.set(startX - cornerRest, y, startX + cornerOffset + corner, y + corner * 2);
                    checkLeftRightBounds();
                    backgroundPath.arcTo(rect, 180, 90);
                } else if (lineWidth < prevLineWidth) {
                    rect.set(startX - cornerRest - innerCornerRad * 2, y, startX - cornerRest, y + innerCornerRad * 2);
                    checkLeftRightBounds();
                    backgroundPath.arcTo(rect, 0, -90);
                }
            }
            backgroundPath.close();
        }
        if (!visiblePartSet) {
            ViewGroup parent = (ViewGroup) getParent();
            backgroundHeight = parent.getMeasuredHeight();
        }
        if (themeDelegate != null) {
            themeDelegate.applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, viewTranslationX, viewTop + dp(4));
        } else {
            Theme.applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, viewTranslationX, viewTop + dp(4));
        }

        int oldAlpha = -1;
        int oldAlpha2 = -1;
        if (fromParent && (getAlpha() != 1f || isFloating())) {
            oldAlpha = backgroundPaint.getAlpha();
            oldAlpha2 = darkenBackgroundPaint.getAlpha();
            backgroundPaint.setAlpha((int) (oldAlpha * getAlpha() * (isFloating() ? .75f : 1f)));
            darkenBackgroundPaint.setAlpha((int) (oldAlpha2 * getAlpha() * (isFloating() ? .75f : 1f)));
        } else if (isFloating()) {
            oldAlpha = backgroundPaint.getAlpha();
            oldAlpha2 = darkenBackgroundPaint.getAlpha();
            backgroundPaint.setAlpha((int) (oldAlpha * (isFloating() ? .75f : 1f)));
            darkenBackgroundPaint.setAlpha((int) (oldAlpha2 * (isFloating() ? .75f : 1f)));
        }
        canvas.drawPath(backgroundPath, backgroundPaint);
        if (hasGradientService()) {
            canvas.drawPath(backgroundPath, darkenBackgroundPaint);
        }
        if (dimAmount > 0) {
            int wasAlpha = dimPaint.getAlpha();
            if (fromParent) {
                dimPaint.setAlpha((int) (wasAlpha * getAlpha()));
            }
            canvas.drawPath(backgroundPath, dimPaint);
            dimPaint.setAlpha(wasAlpha);
        }

        MessageObject messageObject = currentMessageObject;
        if (isButtonLayout(messageObject)) {
            float x = (getWidth() - giftRectSize) / 2f;
            float y = textY + textHeight;
            if (isNewStyleButtonLayout()) {
                y += dp(4);
                AndroidUtilities.rectTmp.set(x, y, x + giftRectSize, y + backgroundRectHeight);
            } else {
                y += dp(12);
                AndroidUtilities.rectTmp.set(x, y, x + giftRectSize, y + giftRectSize + giftPremiumAdditionalHeight);
            }
            if (backgroundRect == null) {
                backgroundRect = new RectF();
            }
            backgroundRect.set(AndroidUtilities.rectTmp);
            canvas.drawRoundRect(backgroundRect, dp(16), dp(16), backgroundPaint);

            if (hasGradientService()) {
                canvas.drawRoundRect(backgroundRect, dp(16), dp(16), darkenBackgroundPaint);
            }
        }

        if (oldAlpha >= 0) {
            backgroundPaint.setAlpha(oldAlpha);
            darkenBackgroundPaint.setAlpha(oldAlpha2);
        }
    }

    @Override
    public int getBoundsLeft() {
        if (isButtonLayout(currentMessageObject)) {
            return (getWidth() - giftRectSize) / 2;
        }
        int left = backgroundLeft;
        if (imageReceiver != null && imageReceiver.getVisible()) {
            left = Math.min((int) imageReceiver.getImageX(), left);
        }
        return left;
    }

    @Override
    public int getBoundsRight() {
        if (isButtonLayout(currentMessageObject)) {
            return (getWidth() + giftRectSize) / 2;
        }
        int right = backgroundRight;
        if (imageReceiver != null && imageReceiver.getVisible()) {
            right = Math.max((int) imageReceiver.getImageX2(), right);
        }
        return right;
    }

    public boolean hasGradientService() {
        return overrideBackgroundPaint == null && (themeDelegate != null ? themeDelegate.hasGradientService() : Theme.hasGradientService());
    }

    @Override
    public void onFailedDownload(String fileName, boolean canceled) {

    }

    @Override
    public void onSuccessDownload(String fileName) {
        MessageObject messageObject = currentMessageObject;
        if (messageObject != null && messageObject.type == MessageObject.TYPE_ACTION_PHOTO) {
            TLRPC.PhotoSize strippedPhotoSize = null;
            for (int a = 0, N = messageObject.photoThumbs.size(); a < N; a++) {
                TLRPC.PhotoSize photoSize = messageObject.photoThumbs.get(a);
                if (photoSize instanceof TLRPC.TL_photoStrippedSize) {
                    strippedPhotoSize = photoSize;
                    break;
                }
            }
            imageReceiver.setImage(currentVideoLocation, ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(strippedPhotoSize, messageObject.photoThumbsObject), "50_50_b", avatarDrawable, 0, null, messageObject, 1);
            DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
        }
    }

    @Override
    public void onProgressDownload(String fileName, long downloadSize, long totalSize) {

    }

    @Override
    public void onProgressUpload(String fileName, long downloadSize, long totalSize, boolean isEncrypted) {

    }

    @Override
    public int getObserverTag() {
        return TAG;
    }

    private SpannableStringBuilder accessibilityText;

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        MessageObject messageObject = currentMessageObject;
        if (TextUtils.isEmpty(customText) && messageObject == null) {
            return;
        }
        if (accessibilityText == null) {
            CharSequence text = !TextUtils.isEmpty(customText) ? customText : messageObject.messageText;
            SpannableStringBuilder sb = new SpannableStringBuilder(text);
            CharacterStyle[] links = sb.getSpans(0, sb.length(), ClickableSpan.class);
            for (CharacterStyle link : links) {
                int start = sb.getSpanStart(link);
                int end = sb.getSpanEnd(link);
                sb.removeSpan(link);

                ClickableSpan underlineSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        if (delegate != null) {
                            openLink(link);
                        }
                    }
                };
                sb.setSpan(underlineSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            accessibilityText = sb;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            info.setContentDescription(accessibilityText.toString());
        } else {
            info.setText(accessibilityText);
        }
        info.setEnabled(true);
    }

    public void setInvalidateColors(boolean invalidate) {
        if (invalidateColors == invalidate) {
            return;
        }
        invalidateColors = invalidate;
        invalidate();
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, themeDelegate);
    }

    protected Paint getThemedPaint(String paintKey) {
        Paint paint = themeDelegate != null ? themeDelegate.getPaint(paintKey) : null;
        return paint != null ? paint : Theme.getThemePaint(paintKey);
    }

    public void drawOutboundsContent(Canvas canvas) {
        canvas.save();
        canvas.translate(textXLeft, textY);
        AnimatedEmojiSpan.drawAnimatedEmojis(canvas, textLayout, animatedEmojiStack, 0, spoilers, 0, 0, 0, 1f, textLayout != null ? getAdaptiveEmojiColorFilter(textLayout.getPaint().getColor()) : null);
        canvas.restore();
    }

    private boolean isButtonLayout(MessageObject messageObject) {
        return messageObject != null && (messageObject.type == MessageObject.TYPE_GIFT_PREMIUM || messageObject.type == MessageObject.TYPE_GIFT_PREMIUM_CHANNEL || isNewStyleButtonLayout());
    }

    private boolean isGiftChannel(MessageObject messageObject) {
        return messageObject != null && messageObject.type == MessageObject.TYPE_GIFT_PREMIUM_CHANNEL;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (invalidateWithParent != null) {
            invalidateWithParent.invalidate();
        }
    }

    @Override
    public void invalidate(Rect dirty) {
        super.invalidate(dirty);
        if (invalidateWithParent != null) {
            invalidateWithParent.invalidate();
        }
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        super.invalidate(l, t, r, b);
        if (invalidateWithParent != null) {
            invalidateWithParent.invalidate();
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == wallpaperPreviewDrawable || super.verifyDrawable(who);
    }

    public boolean isFloating() {
        return false;
    }

    private ColorFilter adaptiveEmojiColorFilter;
    private int adaptiveEmojiColor;
    private ColorFilter getAdaptiveEmojiColorFilter(int color) {
        if (color != adaptiveEmojiColor || adaptiveEmojiColorFilter == null) {
            adaptiveEmojiColorFilter = new PorterDuffColorFilter(adaptiveEmojiColor = color, PorterDuff.Mode.SRC_IN);
        }
        return adaptiveEmojiColorFilter;
    }
}
