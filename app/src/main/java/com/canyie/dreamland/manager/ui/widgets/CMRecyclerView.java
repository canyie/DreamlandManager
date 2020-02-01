package com.canyie.dreamland.manager.ui.widgets;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.canyie.dreamland.manager.utils.Preconditions;

/**
 * @author canyie
 */
public class CMRecyclerView extends RecyclerView {
    private CMContextMenuInfo mContextMenuInfo = new CMContextMenuInfo();
    public CMRecyclerView(@NonNull Context context) {
        super(context);
    }

    public CMRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CMRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    @Override public boolean showContextMenuForChild(View originalView) {
        mContextMenuInfo.position = getChildAdapterPosition(originalView);
        mContextMenuInfo.targetView = originalView;
        return super.showContextMenuForChild(originalView);
    }

    @SuppressWarnings("WeakerAccess") public static final class CMContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public int position;
        public View targetView;
    }
}
