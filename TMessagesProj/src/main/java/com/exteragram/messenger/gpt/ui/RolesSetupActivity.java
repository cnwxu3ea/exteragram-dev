/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.gpt.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.exteragram.messenger.gpt.core.Config;
import com.exteragram.messenger.gpt.core.Role;
import com.exteragram.messenger.gpt.core.RoleList;
import com.exteragram.messenger.gpt.core.Suggestions;
import com.exteragram.messenger.preferences.BasePreferencesActivity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.RadioButtonCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ListView.AdapterWithDiffUtils;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.StickerImageView;

import java.util.ArrayList;
import java.util.Objects;

public class RolesSetupActivity extends BasePreferencesActivity implements NotificationCenter.NotificationCenterDelegate {

    @SuppressLint("ViewConstructor")
    @SuppressWarnings("FieldCanBeLocal")
    public static class HintInnerCell extends FrameLayout {

        private final StickerImageView imageView;
        private final TextView messageTextView;

        public HintInnerCell(Context context, CharSequence text) {
            super(context);

            imageView = new StickerImageView(context, UserConfig.selectedAccount);
            imageView.setStickerPackName("exteraGramPlaceholders");
            imageView.setStickerNum(1);
            imageView.getImageReceiver().setAutoRepeatCount(1);
            imageView.setOnClickListener(v -> imageView.getImageReceiver().startAnimation());
            imageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(imageView, LayoutHelper.createFrame(120, 120, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 14, 0, 0));

            messageTextView = new TextView(context);
            messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
            messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            messageTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_REGULAR));
            messageTextView.setGravity(Gravity.CENTER);
            messageTextView.setText(text);
            addView(messageTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 40, 151, 40, 24));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), heightMeasureSpec);
        }
    }
    
    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(0, R.drawable.msg_add);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 0) {
                    presentFragment(new EditRoleActivity(null));
                }
            }
        });
        return view;
    }

    @Override
    public boolean onFragmentCreate() {
        updateRows(false);
        getNotificationCenter().addObserver(this, NotificationCenter.rolesUpdated);
        return super.onFragmentCreate();
    }

    private final ArrayList<ItemInner> oldItems = new ArrayList<>();
    private final ArrayList<ItemInner> items = new ArrayList<>();
    private final RoleList roles = new RoleList();

    @SuppressLint("NotifyDataSetChanged")
    private void updateRows(boolean animated) {
        oldItems.clear();
        oldItems.addAll(items);
        items.clear();
        roles.fill();

        items.add(ItemInner.asHint());
        items.add(ItemInner.asHeader(LocaleController.getString(R.string.Suggestions)));
        for (Suggestions c : Suggestions.values()) {
            items.add(ItemInner.asRole(c.getRole()));
        }

        items.add(ItemInner.asShadow());

        if (!roles.isEmpty()) {
            items.add(ItemInner.asHeader(LocaleController.getString(R.string.Roles)));
            for (Role role : roles) {
                items.add(ItemInner.asRole(role));
            }
            items.add(ItemInner.asShadow());
        }

        if (listAdapter != null) {
            if (animated) {
                listAdapter.setItems(oldItems, items);
            } else {
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    public void updateRadio() {
        if (listAdapter != null) {
            int count = listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = listView.getChildAt(a);
                if (!(child instanceof RadioButtonCell)) {
                    continue;
                }
                RecyclerView.ViewHolder holder = listView.findContainingViewHolder(child);
                if (holder == null) {
                    continue;
                }
                int position = holder.getAdapterPosition();
                ItemInner item = items.get(position);
                RadioButtonCell radioButtonCell = (RadioButtonCell) child;
                radioButtonCell.setChecked(item.role.isSelected(), true);
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.rolesUpdated);
        super.onFragmentDestroy();
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.Roles);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        ItemInner item = items.get(position);
        if (item == null || item.role.isSuggestion()) {
            return false;
        }
        if (item.viewType == VIEW_TYPE_ROLE) {
            ItemOptions options = ItemOptions.makeOptions(RolesSetupActivity.this, view);
            options.add(R.drawable.msg_edit, LocaleController.getString("Edit", R.string.Edit), () -> presentFragment(new EditRoleActivity(item.role)));
            options.add(R.drawable.msg_delete, LocaleController.getString("Delete", R.string.Delete), true, () -> {
                if (item.role.isSelected()) {
                    Config.clearSelectedRole();
                }
                roles.remove(item.role);
                updateRadio();
                updateRows(true);
            });
            if (LocaleController.isRTL) {
                options.setGravity(Gravity.LEFT);
            }
            options.show();
        }
        return true;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        ItemInner item = items.get(position);
        if (item == null || item.role.isSelected()) {
            return;
        }
        if (item.viewType == VIEW_TYPE_ROLE) {
            Config.setSelectedRole(item.role);
        }
        updateRadio();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SHADOW = 1;
    private static final int VIEW_TYPE_ROLE = 2;
    private static final int VIEW_TYPE_BUTTON = 3;
    private static final int VIEW_TYPE_HINT = 4;

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.rolesUpdated) {
            updateRows(true);
        }
    }

    private static class ItemInner extends AdapterWithDiffUtils.Item {
        public ItemInner(int viewType) {
            super(viewType, false);
        }

        CharSequence text;
        Role role;

        public static ItemInner asHeader(CharSequence text) {
            ItemInner i = new ItemInner(VIEW_TYPE_HEADER);
            i.text = text;
            return i;
        }

        public static ItemInner asShadow() {
            ItemInner i = new ItemInner(VIEW_TYPE_SHADOW);
            i.text = null;
            return i;
        }

        public static ItemInner asRole(Role role) {
            ItemInner i = new ItemInner(VIEW_TYPE_ROLE);
            i.role = role;
            return i;
        }

        public static ItemInner asButton(CharSequence text) {
            ItemInner i = new ItemInner(VIEW_TYPE_BUTTON);
            i.text = text;
            return i;
        }

        public static ItemInner asHint() {
            return new ItemInner(VIEW_TYPE_HINT);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ItemInner)) {
                return false;
            }
            ItemInner other = (ItemInner) obj;
            if (other.viewType != viewType) {
                return false;
            }
            if (viewType == VIEW_TYPE_HEADER || viewType == VIEW_TYPE_BUTTON || viewType == VIEW_TYPE_SHADOW) {
                if (!TextUtils.equals(text, other.text)) {
                    return false;
                }
            }
            if (viewType == VIEW_TYPE_ROLE) {
                return Objects.equals(role, other.role);
            }
            return true;
        }
    }

    private class ListAdapter extends BaseListAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type != VIEW_TYPE_SHADOW && type != VIEW_TYPE_HEADER && type != VIEW_TYPE_HINT;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_HEADER -> {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                }
                case VIEW_TYPE_ROLE -> {
                    view = new RadioButtonCell(mContext);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                }
                case VIEW_TYPE_HINT -> {
                    view = new HintInnerCell(mContext, LocaleController.getString(R.string.RolesInfo));
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                }
                case VIEW_TYPE_BUTTON -> {
                    view = new TextCell(mContext);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                }
                default -> view = new ShadowSectionCell(mContext);
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            if (item == null) {
                return;
            }
            boolean divider = position + 1 < items.size() && items.get(position + 1).viewType != VIEW_TYPE_SHADOW;
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_HEADER -> {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(item.text);
                }
                case VIEW_TYPE_ROLE -> {
                    RadioButtonCell radioButtonCell = (RadioButtonCell) holder.itemView;
                    boolean checked = Objects.equals(Config.getSelectedRole(), item.role.getName());
                    radioButtonCell.setTextAndValue(item.role.getName(), item.role.getPrompt(), divider, checked);
                }
                case VIEW_TYPE_BUTTON -> {
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.getTextView().setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText2));
                    Drawable drawable1 = mContext.getResources().getDrawable(R.drawable.poll_add_circle);
                    Drawable drawable2 = mContext.getResources().getDrawable(R.drawable.poll_add_plus);
                    drawable1.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_switchTrackChecked), PorterDuff.Mode.MULTIPLY));
                    drawable2.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_checkboxCheck), PorterDuff.Mode.MULTIPLY));
                    CombinedDrawable combinedDrawable = new CombinedDrawable(drawable1, drawable2);

                    textCell.setTextAndIcon(String.valueOf(item.text), combinedDrawable, true);
                }
                case VIEW_TYPE_SHADOW ->
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= items.size()) {
                return VIEW_TYPE_SHADOW;
            }
            ItemInner item = items.get(position);
            if (item == null) {
                return VIEW_TYPE_SHADOW;
            }
            return item.viewType;
        }
    }
}

