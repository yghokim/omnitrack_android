package com.danielstone.materialaboutlibrary.util;

import android.content.Context;
import android.view.View;

import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutImageItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;

import static com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager.ItemLayout.ACTION_LAYOUT;
import static com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager.ItemLayout.IMAGE_LAYOUT;
import static com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager.ItemLayout.TITLE_LAYOUT;
import static com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager.ItemType.ACTION_ITEM;
import static com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager.ItemType.IMAGE_ITEM;
import static com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager.ItemType.TITLE_ITEM;

public class DefaultViewTypeManager extends ViewTypeManager {

    public int getLayout(int itemType) {
        switch (itemType) {
            case ACTION_ITEM:
                return ACTION_LAYOUT;
            case TITLE_ITEM:
                return TITLE_LAYOUT;
            case IMAGE_ITEM:
                return IMAGE_LAYOUT;
            default:
                return -1;
        }
    }

    public MaterialAboutItemViewHolder getViewHolder(int itemType, View view) {
        switch (itemType) {
            case ACTION_ITEM:
                return MaterialAboutActionItem.getViewHolder(view);
            case TITLE_ITEM:
                return MaterialAboutTitleItem.getViewHolder(view);
            case IMAGE_ITEM:
                return MaterialAboutImageItem.getViewHolder(view);
            default:
                return null;
        }
    }

    public void setupItem(int itemType, MaterialAboutItemViewHolder holder, MaterialAboutItem item, Context context) {
        switch (itemType) {
            case ACTION_ITEM:
                MaterialAboutActionItem.setupItem((MaterialAboutActionItem.MaterialAboutActionItemViewHolder) holder, (MaterialAboutActionItem) item, context);
                break;
            case TITLE_ITEM:
                MaterialAboutTitleItem.setupItem((MaterialAboutTitleItem.MaterialAboutTitleItemViewHolder) holder, (MaterialAboutTitleItem) item, context);
                break;
            case IMAGE_ITEM:
                MaterialAboutImageItem.setupItem((MaterialAboutImageItem.MaterialAboutImageItemViewHolder) holder, (MaterialAboutImageItem) item, context);
                break;
        }
    }

    public static final class ItemType {
        public static final int ACTION_ITEM = ViewTypeManager.ItemType.ACTION_ITEM;
        public static final int TITLE_ITEM = ViewTypeManager.ItemType.TITLE_ITEM;
        public static final int IMAGE_ITEM = ViewTypeManager.ItemType.IMAGE_ITEM;
    }

    public static final class ItemLayout {
        public static final int ACTION_LAYOUT = ViewTypeManager.ItemLayout.ACTION_LAYOUT;
        public static final int TITLE_LAYOUT = ViewTypeManager.ItemLayout.TITLE_LAYOUT;
        public static final int IMAGE_LAYOUT = ViewTypeManager.ItemLayout.IMAGE_LAYOUT;
    }
}
