/*
 * Copyright (C) 2014-2018 The MoKee Open Source Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mokee.center.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.lzy.okgo.model.Progress;

import java.io.Serializable;

import androidx.annotation.Keep;

@Keep
public class UpdateInfo implements Parcelable, Serializable {

    public static final Parcelable.Creator<UpdateInfo> CREATOR = new Parcelable.Creator<UpdateInfo>() {
        public UpdateInfo createFromParcel(Parcel in) {
            return new UpdateInfo(in);
        }

        public UpdateInfo[] newArray(int size) {
            return new UpdateInfo[size];
        }
    };

    private static final long serialVersionUID = 5499890003569313403L;
    private String mName;
    private String mMd5;
    private long mDiffSize;
    private long mFileSize;
    private long mTimestamp;
    private String mDownloadUrl;
    private String mChangelogUrl;
    private Progress mProgress;
    private String mDisplayVersion;

    public UpdateInfo() {
    }

    public UpdateInfo(Parcel in) {
        readFromParcel(in);
    }

    public String getName() {
        return mName;
    }

    public String getDisplayVersion() {
        return mDisplayVersion;
    }

    public String getMd5() {
        return mMd5;
    }

    public long getDiffSize() {
        return mDiffSize;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public String getChangelogUrl() {
        return mChangelogUrl;
    }

    public Progress getProgress() {
        return mProgress;
    }

    public void setProgress(Progress progress) {
        this.mProgress = progress;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mDisplayVersion);
        dest.writeString(mMd5);
        dest.writeLong(mDiffSize);
        dest.writeLong(mFileSize);
        dest.writeLong(mTimestamp);
        dest.writeString(mDownloadUrl);
        dest.writeString(mChangelogUrl);
    }

    private void readFromParcel(Parcel in) {
        mName = in.readString();
        mDisplayVersion = in.readString();
        mMd5 = in.readString();
        mDiffSize = in.readLong();
        mFileSize = in.readLong();
        mTimestamp = in.readLong();
        mDownloadUrl = in.readString();
        mChangelogUrl = in.readString();
    }

    public static class Builder {
        private String mName;
        private String mDisplayVersion;
        private String mMd5;
        private long mDiffSize;
        private long mFileSize;
        private long mTimestamp;
        private String mDownloadUrl;
        private String mChangelogUrl;

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setDisplayVersion(String version) {
            mDisplayVersion = version;
            return this;
        }

        public Builder setMD5Sum(String md5Sum) {
            mMd5 = md5Sum;
            return this;
        }

        public Builder setDiffSize(long diffSize) {
            mDiffSize = diffSize;
            return this;
        }

        public Builder setFileSize(long fileSize) {
            mFileSize = fileSize;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        public Builder setChangelogUrl(String changelogUrl) {
            mChangelogUrl = changelogUrl;
            return this;
        }

        public Builder setDownloadUrl(String downloadUrl) {
            mDownloadUrl = downloadUrl;
            return this;
        }

        public UpdateInfo build() {
            UpdateInfo info = new UpdateInfo();
            info.mName = mName;
            info.mDisplayVersion = mDisplayVersion;
            info.mMd5 = mMd5;
            info.mDiffSize = mDiffSize;
            info.mFileSize = mFileSize;
            info.mTimestamp = mTimestamp;
            info.mChangelogUrl = mChangelogUrl;
            info.mDownloadUrl = mDownloadUrl;
            return info;
        }
    }
}
