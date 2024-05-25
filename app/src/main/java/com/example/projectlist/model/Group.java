package com.example.projectlist.model;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.versionedparcelable.VersionedParcel;

import java.util.Objects;

@Entity(tableName = "names_group")
public class Group implements Parcelable {
    @NonNull
    @PrimaryKey()
    public String uid;

    @ColumnInfo(name = "group")
    public String group;

    @Override
    public int describeContents() {
        return 0;
    }
    public Group(){}

    protected Group(Parcel in) {
        uid = in.readString();
        group = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(group);
    }

    public static final Creator<Group> CREATOR = new Creator<Group>() {
        @Override
        public Group createFromParcel(Parcel in) {
            return new Group(in);
        }

        @Override
        public Group[] newArray(int size) {
            return new Group[size];
        }
    };

}


