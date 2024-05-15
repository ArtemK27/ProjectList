package com.example.projectlist.screens;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.projectlist.App;
import com.example.projectlist.data.NoteDao;
import com.example.projectlist.model.Group;
import com.example.projectlist.model.Note;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends ViewModel {
//    LiveData<List<Group>> groupsNameLiveData = App.getInstance().getNoteDao().getAllNames();;
    private NoteDao repo = App.getInstance().getNoteDao();
    private LiveData<List<Note>> noteLiveData;
    private LiveData<List<Note>> searchByLiveData;
    private MutableLiveData<String> filterLiveData = new MutableLiveData<>();
    private List<Group> namesLists;

    public List<Group> getAllNames() {

        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                namesLists = App.getInstance().getNoteDao().getAllNames();
            }
        });

        databaseExecutor.shutdown();


        return namesLists;
    };
    public MainViewModel () {
        noteLiveData = repo.getAllLiveData();
        searchByLiveData = Transformations.switchMap(filterLiveData,
                v -> repo.getLiveDataByGroup(v));

    }

    public LiveData<List<Note>> getNoteLiveData() {
        return noteLiveData;
    }
    void setFilter(String filter) { filterLiveData.setValue(filter); }
    public LiveData<List<Note>> getLiveDataByGroup() {
        return searchByLiveData;
    }

    public void updateNote(Note note) {
        // Обновите заметку в базе данных
        repo.update(note);
    }


    public void deleteDoneNotes() {
        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                App.getInstance().getNoteDao().deleteDoneNotes();
            }
        });

        databaseExecutor.shutdown();
    }


}
