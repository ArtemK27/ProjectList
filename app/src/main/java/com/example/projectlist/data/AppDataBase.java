package com.example.projectlist.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.projectlist.model.Group;
import com.example.projectlist.model.Note;

@Database(entities = {Note.class, Group.class}, version = 1, exportSchema = false)
public abstract class AppDataBase extends RoomDatabase {
    public abstract NoteDao noteDao();
}
