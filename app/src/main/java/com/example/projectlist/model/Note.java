package com.example.projectlist.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity
public class Note implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "name")
    public String text;

    @ColumnInfo(name = "time")
    public long time;

    @ColumnInfo(name = "done")
    public boolean done;

    @ColumnInfo(name = "amount")
    public String amount;

    @ColumnInfo(name = "group")
    public String group;

    @ColumnInfo(name = "author")
    public String author;

    @ColumnInfo(name = "update_flag")
    public String update_flag;


    // не добавил в методы--------
    @ColumnInfo(name = "sync")
    public int sync;

    public Note(){
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return uid == note.uid && time == note.time && done == note.done && Objects.equals(text, note.text)
                && Objects.equals(amount, note.amount) && Objects.equals(group, note.group) && Objects.equals(author, note.author) && Objects.equals(update_flag, note.update_flag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, text, time, done, amount, group, author, update_flag);
    }

    protected Note(Parcel in) {
        uid = in.readInt();
        amount = in.readString();
        text = in.readString();
        time = in.readLong();
        done = in.readByte() != 0;
        group = in.readString();
        author = in.readString();
        update_flag = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(uid);
        dest.writeString(amount);
        dest.writeString(text);
        dest.writeLong(time);
        dest.writeByte((byte) (done ? 1 : 0));
        dest.writeString(group);
        dest.writeString(author);
        dest.writeString(update_flag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Note> CREATOR = new Creator<Note>() {
        @Override
        public Note createFromParcel(Parcel in) {
            return new Note(in);
        }

        @Override
        public Note[] newArray(int size) {
            return new Note[size];
        }
    };
}
