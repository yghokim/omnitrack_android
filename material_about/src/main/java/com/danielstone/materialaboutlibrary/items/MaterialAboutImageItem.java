package com.danielstone.materialaboutlibrary.items;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.danielstone.materialaboutlibrary.R;
import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder;
import com.danielstone.materialaboutlibrary.util.ViewTypeManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import static android.view.View.GONE;

public class MaterialAboutImageItem extends MaterialAboutItem {

    private CharSequence text = null;

    /*
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({GRAVITY_TOP, GRAVITY_MIDDLE, GRAVITY_BOTTOM})
    public @interface IconGravity {
    }

    public static final int GRAVITY_TOP = 0;
    public static final int GRAVITY_MIDDLE = 1;
    public static final int GRAVITY_BOTTOM = 2;*/
    private int textRes = 0;
    private Drawable icon = null;
    /*
        private CharSequence subText = null;
        private int subTextRes = 0;*/
    private int iconRes = 0;
    /*
    private boolean showIcon = true;
    private int iconGravity = GRAVITY_MIDDLE;
*/
    private Integer iconTint = null;
    private int iconTintRes = 0;
    private Integer textColorOverride = null;
    private int textColorOverrideRes = 0;
    private MaterialAboutImageItem.OnClickListener onClickListener = null;

    private MaterialAboutImageItem(Builder builder) {
        this.text = builder.text;
        this.textRes = builder.textRes;

        this.icon = builder.icon;
        this.iconRes = builder.iconRes;

        this.onClickListener = builder.onClickListener;

        this.iconTint = builder.iconTint;
        this.iconTintRes = builder.iconTintRes;

        this.textColorOverride = builder.textColorOverride;
        this.textColorOverrideRes = builder.textColorOverrideRes;
    }

    public static MaterialAboutItemViewHolder getViewHolder(View view) {
        return new MaterialAboutImageItemViewHolder(view);
    }

    public static void setupItem(MaterialAboutImageItemViewHolder holder, MaterialAboutImageItem item, Context context) {
        CharSequence text = item.getText();
        int textRes = item.getTextRes();

        holder.text.setVisibility(View.VISIBLE);
        if (text != null) {
            holder.text.setText(text);
        } else if (textRes != 0) {
            holder.text.setText(textRes);
        } else {
            holder.text.setVisibility(GONE);
        }

        Integer textColorOverride = item.textColorOverride;
        int textColorOverrideRes = item.textColorOverrideRes;
        if (textColorOverride != null) {
            holder.text.setTextColor(textColorOverride);
        } else if (textColorOverrideRes != 0) {
            holder.text.setTextColor(ContextCompat.getColor(context, textColorOverrideRes));
        } else {
            holder.text.setTextColor(ContextCompat.getColor(context, R.color.mal_action_item_text));
        }

        holder.icon.setVisibility(View.VISIBLE);
        Drawable drawable = item.getIcon();
        int drawableRes = item.getIconRes();
        if (drawable != null) {

            if (item.iconTint != null) {
                drawable = DrawableCompat.wrap(drawable).mutate();
                DrawableCompat.setTint(drawable, item.iconTint);
            } else if (item.iconTintRes != 0) {
                drawable = DrawableCompat.wrap(drawable).mutate();
                DrawableCompat.setTint(drawable, ContextCompat.getColor(context, item.iconTintRes));
            }

            holder.icon.setImageDrawable(drawable);
        } else if (drawableRes != 0) {

            drawable = ContextCompat.getDrawable(context, drawableRes);

            boolean tinted = false;
            if (item.iconTint != null) {
                drawable = DrawableCompat.wrap(drawable).mutate();
                DrawableCompat.setTint(drawable, item.iconTint);
                tinted = true;
            } else if (item.iconTintRes != 0) {
                drawable = DrawableCompat.wrap(drawable).mutate();
                DrawableCompat.setTint(drawable, ContextCompat.getColor(context, item.iconTintRes));
                tinted = true;
            }

            if (tinted) {
                holder.icon.setImageDrawable(drawable);
            } else {
                holder.icon.setImageResource(drawableRes);
            }
        }


        int pL = 0, pT = 0, pR = 0, pB = 0;
        if (Build.VERSION.SDK_INT < 21) {
            pL = holder.view.getPaddingLeft();
            pT = holder.view.getPaddingTop();
            pR = holder.view.getPaddingRight();
            pB = holder.view.getPaddingBottom();
        }

        if (item.getOnClickListener() != null) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            holder.view.setBackgroundResource(outValue.resourceId);
            holder.onClickListener = item.getOnClickListener();
            holder.view.setSoundEffectsEnabled(true);
        } else {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, false);
            holder.view.setBackgroundResource(outValue.resourceId);
            holder.onClickListener = null;
            holder.view.setSoundEffectsEnabled(false);
        }

        if (Build.VERSION.SDK_INT < 21) {
            holder.view.setPadding(pL, pT, pR, pB);
        }
    }

    @Override
    public int getType() {
        return ViewTypeManager.ItemType.IMAGE_ITEM;
    }

    public CharSequence getText() {
        return text;
    }

    public MaterialAboutImageItem setText(CharSequence text) {
        this.textRes = 0;
        this.text = text;
        return this;
    }

    public int getTextRes() {
        return textRes;
    }

    public MaterialAboutImageItem setTextRes(int textRes) {
        this.text = null;
        this.textRes = textRes;
        return this;
    }

    public Drawable getIcon() {
        return icon;
    }

    public MaterialAboutImageItem setIcon(Drawable icon) {
        this.iconRes = 0;
        this.icon = icon;
        return this;
    }

    public int getIconRes() {
        return iconRes;
    }

    public MaterialAboutImageItem setIconRes(int iconRes) {
        this.icon = null;
        this.iconRes = iconRes;
        return this;
    }

    public MaterialAboutImageItem.OnClickListener getOnClickListener() {
        return onClickListener;
    }

    public MaterialAboutImageItem setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        return this;
    }

    public interface OnClickListener {
        void onClick();
    }

    public static class MaterialAboutImageItemViewHolder extends MaterialAboutItemViewHolder implements View.OnClickListener {
        public final View view;
        public final AppCompatImageView icon;
        public final TextView text;
        public MaterialAboutImageItem.OnClickListener onClickListener;

        MaterialAboutImageItemViewHolder(View view) {
            super(view);
            this.view = view;
            icon = view.findViewById(R.id.mal_item_image);
            text = view.findViewById(R.id.mal_item_text);
            view.setOnClickListener(this);
            onClickListener = null;
        }

        @Override
        public void onClick(View v) {
            if (onClickListener != null) {
                onClickListener.onClick();
            }
        }
    }

    public static class Builder {

        MaterialAboutImageItem.OnClickListener onClickListener;
        private CharSequence text = null;
        @StringRes
        private int textRes = 0;
        private Drawable icon = null;
        @DrawableRes
        private int iconRes = 0;
        private Integer iconTint = null;
        private int iconTintRes = 0;
        private Integer textColorOverride = null;
        private int textColorOverrideRes = 0;
        @LayoutRes
        private int layoutRes = 0;

        public Builder text(CharSequence text) {
            this.text = text;
            this.textRes = 0;
            return this;
        }

        public Builder text(@StringRes int text) {
            this.textRes = text;
            this.text = null;
            return this;
        }


        public Builder iconTint(@ColorInt int tint) {
            this.iconTint = tint;
            this.iconTintRes = 0;
            return this;
        }

        public Builder iconTintRes(@ColorRes int id) {
            this.iconTint = null;
            this.iconTintRes = id;
            return this;
        }

        public Builder textColorOverride(@ColorInt int color) {
            this.textColorOverride = color;
            this.textColorOverrideRes = 0;
            return this;
        }

        public Builder textColorOverrideRes(@ColorRes int id) {
            this.textColorOverride = null;
            this.textColorOverrideRes = id;
            return this;
        }

        public Builder icon(Drawable icon) {
            this.icon = icon;
            this.iconRes = 0;
            return this;
        }

        public Builder icon(@DrawableRes int iconRes) {
            this.icon = null;
            this.iconRes = iconRes;
            return this;
        }

        public Builder setOnClickListener(MaterialAboutImageItem.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
            return this;
        }

        public MaterialAboutImageItem build() {
            return new MaterialAboutImageItem(this);
        }
    }
}
