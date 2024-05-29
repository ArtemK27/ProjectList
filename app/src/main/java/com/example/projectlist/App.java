package com.example.projectlist;

import android.app.Application;

import androidx.room.Room;

import com.example.projectlist.data.AppDataBase;
import com.example.projectlist.data.NoteDao;

public class App extends Application {

    private AppDataBase dataBase;
    private NoteDao noteDao;
    private static App instance;
    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        dataBase = Room.databaseBuilder(getApplicationContext(),
                AppDataBase.class, "app-db")
//                .allowMainThreadQueries()
                .build();

        noteDao = dataBase.noteDao();
    }

    public AppDataBase getDataBase() {
        return dataBase;
    }

    public void setDataBase(AppDataBase dataBase) {
        this.dataBase = dataBase;
    }

    public NoteDao getNoteDao() {
        return noteDao;
    }

    public void setNoteDao(NoteDao noteDao) {
        this.noteDao = noteDao;
    }
}
