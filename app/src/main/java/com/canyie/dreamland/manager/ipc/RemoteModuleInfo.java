package com.canyie.dreamland.manager.ipc;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author canyie
 * @date 2019/12/22.
 */
public final class RemoteModuleInfo implements Parcelable {
    public String name;
    //public String packageName;
    public String path;

    public RemoteModuleInfo() {
    }

    public RemoteModuleInfo(@NonNull String name, @NonNull String path) {
        this.name = name;
        this.path = path;
    }

    public RemoteModuleInfo(Parcel in) {
        name = in.readString();
        //packageName = in.readString();
        path = in.readString();
    }

    public static final Creator<RemoteModuleInfo> CREATOR = new Creator<RemoteModuleInfo>() {
        @Override
        public RemoteModuleInfo createFromParcel(Parcel in) {
            return new RemoteModuleInfo(in);
        }

        @Override
        public RemoteModuleInfo[] newArray(int size) {
            return new RemoteModuleInfo[size];
        }
    };

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        //dest.writeString(packageName);
        dest.writeString(path);
    }

    @NonNull @Override public String toString() {
        return "com.canyie.dreamland.manager.ipc.RemoteModuleInfo{name=" + name + ", path=" + path + "}";
    }

    @Override public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RemoteModuleInfo)) return false;
        RemoteModuleInfo that = (RemoteModuleInfo) obj;
        return TextUtils.equals(name, that.name) && TextUtils.equals(path, that.path);
    }

    @Override public int hashCode() {
        int h = 41;
        h = 31 * h + name.hashCode();
        h = 31 * h + path.hashCode();
        return h;
    }
}
