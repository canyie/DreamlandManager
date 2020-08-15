package top.canyie.dreamland.manager.ui.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.AppInfo;
import top.canyie.dreamland.manager.utils.Threads;

/**
 * @author canyie
 */
public class MaseListAdapter extends RecyclerView.Adapter implements Filterable {
    private static final int POSITION_HEADER = 0;
    private LayoutInflater mLayoutInflater;
    private List<AppInfo> mSourceList;
    private List<AppInfo> mFilteredList;
    private boolean mMasEnabled;
    private OnStateChangeListener mListener;
    private F mFilter;

    public MaseListAdapter(Context context, OnStateChangeListener l) {
        mLayoutInflater = LayoutInflater.from(context);
        mListener = l;
    }

    public synchronized void setData(boolean masEnabled, List<AppInfo> apps) {
        mMasEnabled = masEnabled;
        mSourceList = apps;
        mFilteredList = apps;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == POSITION_HEADER) {
            return new HeaderHolder(mLayoutInflater.inflate(R.layout.mase_list_header, parent, false));
        }
        return new ItemHolder(mLayoutInflater.inflate(R.layout.appslist_item, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == POSITION_HEADER) {
            HeaderHolder headerHolder = (HeaderHolder) holder;
            headerHolder.enableSwitch.setChecked(mMasEnabled);
            headerHolder.enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mMasEnabled = isChecked;
                mListener.onChanged();
                // Wait for switch animation finish
                Threads.getMainThreadHandler().postDelayed(this::notifyDataSetChanged, 250);
            });
        } else {
            ItemHolder itemHolder = (ItemHolder) holder;
            int positionInList = position - 1; // Exclude header
            AppInfo appInfo = mFilteredList.get(positionInList);
            itemHolder.appName.setText(appInfo.name);
            itemHolder.appPackageName.setText(appInfo.packageName);
            itemHolder.appIcon.setImageDrawable(appInfo.icon);
            itemHolder.appCheckbox.setChecked(appInfo.enabled);
            itemHolder.appCheckbox.setEnabled(mMasEnabled);
            itemHolder.appCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mFilteredList.get(positionInList).enabled = isChecked;
                mListener.onChanged();
            });
            itemHolder.itemView.setOnClickListener(v -> {
                if (mMasEnabled) itemHolder.appCheckbox.performClick();
            });
        }
    }

    @Override public int getItemCount() {
        return (mFilteredList != null ? mFilteredList.size() : 0) + 1;
    }

    @Override public int getItemViewType(int position) {
        return position;
    }

    @MainThread @Override public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new F();
        }
        return mFilter;
    }

    public boolean isMasEnabled() {
        return mMasEnabled;
    }

    public interface OnStateChangeListener {
        void onChanged();
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        SwitchCompat enableSwitch;
        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            enableSwitch = itemView.findViewById(R.id.mas_switch);
        }
    }

    static final class ItemHolder extends RecyclerView.ViewHolder {
        TextView appName;
        TextView appPackageName;
        ImageView appIcon;
        CheckBox appCheckbox;

        ItemHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            appPackageName = itemView.findViewById(R.id.app_package_name);
            appIcon = itemView.findViewById(R.id.app_icon);
            appCheckbox = itemView.findViewById(R.id.app_checkbox);
        }
    }

    final class F extends Filter {
        @Override protected FilterResults performFiltering(CharSequence constraint) {
            List<AppInfo> sourceList;
            synchronized (MaseListAdapter.this) {
                sourceList = mSourceList;
            }
            List<AppInfo> filteredList = new ArrayList<>();
            if (TextUtils.isEmpty(constraint)) {
                filteredList.addAll(sourceList);
            } else {
                String constraintStrLowerCase = constraint.toString().toLowerCase();
                for (AppInfo appInfo : sourceList) {
                    if (appInfo.name.toLowerCase().contains(constraintStrLowerCase)
                            || appInfo.packageName.toLowerCase().contains(constraintStrLowerCase)) {
                        filteredList.add(appInfo);
                    }
                }
            }

            FilterResults filterResults = new FilterResults();
            filterResults.count = filteredList.size();
            filterResults.values = filteredList;
            return filterResults;
        }

        @SuppressWarnings("unchecked") @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            synchronized (MaseListAdapter.this) {
                mFilteredList = (List<AppInfo>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}
