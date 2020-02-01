package com.canyie.dreamland.manager.ui.adapters;

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
import androidx.recyclerview.widget.RecyclerView;

import com.canyie.dreamland.manager.R;
import com.canyie.dreamland.manager.core.AppInfo;
import com.canyie.dreamland.manager.core.AppManager;
import com.canyie.dreamland.manager.core.Dreamland;
import com.canyie.dreamland.manager.core.ModuleInfo;
import com.canyie.dreamland.manager.core.ModuleManager;
import com.canyie.dreamland.manager.utils.Intents;
import com.canyie.dreamland.manager.utils.ToastCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * @author canyie
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> implements Filterable {
    private Context context;
    private List<AppInfo> mSourceList;
    private List<AppInfo> mFilteredList;
    private AppsFilter mFilter;
    private OnAppStateChangedListener mAppStateChangedListener;

    public AppListAdapter(Context context) {
        this.context = context;
    }

    @NonNull @Override
    public AppListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.appslist_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppListAdapter.ViewHolder holder, int position) {
        AppInfo appInfo = mFilteredList.get(position);
        holder.appName.setText(appInfo.name);
        holder.appPackageName.setText(appInfo.packageName);
        holder.appIcon.setImageDrawable(appInfo.icon);
        holder.appCheckbox.setChecked(appInfo.enabled);
        holder.itemView.setOnClickListener(v -> {
            if (!Intents.openAppUserInterface(context, appInfo.packageName)) {
                ToastCompat.showToast(context, R.string.alert_app_cannot_open);
            }
        });
        if (Dreamland.isInstalled()) {
            holder.appCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appInfo.setEnabled(isChecked);
                if (mAppStateChangedListener != null) mAppStateChangedListener.onAppStateChanged();
            });
        } else {
            holder.appError.setVisibility(View.VISIBLE);
            holder.appError.setText(R.string.framework_state_not_installed);
            holder.appCheckbox.setEnabled(false);
        }
    }

    @Override public int getItemCount() {
        return mFilteredList != null ? mFilteredList.size() : 0;
    }

    public void setOnAppStateChangedListener(OnAppStateChangedListener listener) {
        this.mAppStateChangedListener = listener;
    }

    public AppInfo getAppInfoForPosition(int position) {
        return mFilteredList.get(position);
    }

    @MainThread @Override public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new AppsFilter();
        }
        return mFilter;
    }

    public synchronized void setApps(List<AppInfo> apps) {
        this.mSourceList = apps;
        this.mFilteredList = apps;
        notifyDataSetChanged();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        TextView appName;
        TextView appPackageName;
        ImageView appIcon;
        CheckBox appCheckbox;
        TextView appError;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            appPackageName = itemView.findViewById(R.id.app_package_name);
            appIcon = itemView.findViewById(R.id.app_icon);
            appCheckbox = itemView.findViewById(R.id.app_checkbox);
            appError = itemView.findViewById(R.id.app_error);
        }
    }

    final class AppsFilter extends Filter {
        @Override protected FilterResults performFiltering(CharSequence constraint) {
            List<AppInfo> sourceList;
            synchronized (AppListAdapter.this) {
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
            synchronized (AppListAdapter.this) {
                mFilteredList = (List<AppInfo>) results.values;
                notifyDataSetChanged();
            }
        }
    }

    public interface OnAppStateChangedListener {
        void onAppStateChanged();
    }
}
