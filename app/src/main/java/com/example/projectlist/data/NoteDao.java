package com.example.projectlist.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.projectlist.model.Group;
import com.example.projectlist.model.Note;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM Note")
    List<Note> getAll();
    @Query("UPDATE Note SET sync = 0")
    void clearSync();
    @Query("DELETE FROM NOTE WHERE sync = 0")
    void deleteSync();
    @Query("DELETE FROM NOTE WHERE sync = 0 and `group` == :group")
    void deleteSyncByGroup(String group);


    @Query("SELECT * FROM Note")
    LiveData<List<Note>> getAllLiveData();
    @Query("SELECT * FROM Note WHERE `group` == :group")
    LiveData<List<Note>> getLiveDataByGroup(String group);

    @Query("SELECT * FROM Note WHERE uid IN (:userIds)")
    List<Note> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM Note WHERE uid = :uid LIMIT 1")
    Note findById(int uid);

    @Query("DELETE FROM Note WHERE done = 1")
    void deleteDoneNotes();
    @Query("DELETE FROM Note WHERE done = 1 AND `group` = :group")
    void deleteDoneNoteByGroup(String group);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);
    @Query("SELECT * FROM Note WHERE update_flag = 'click_update'")
    List<Note> getClickatedNotes();
    @Query("SELECT * FROM Note WHERE done = 1")
    List<Note> getDoneNotes();
    @Query("UPDATE Note SET update_flag = NULL")
    void clearClickatedNotes();

    @Update
    void update(Note note);


    @Delete
    void delete(Note note);

    @Update
    void update(Group group);

    @Delete
    void delete(Group group);


    @Query("SELECT * FROM Note WHERE done = 1 AND `group` = :group")
    List<Note> getDoneNoteByGroup(String group);

    @Query("SELECT * FROM Note WHERE update_flag = 1")
    List<Note>getUpdateNotes();

    @Query("SELECT * FROM names_group")
    List<Group> getAllNames();

    @Query("SELECT * FROM names_group LIMIT 1")
    Group getOneName();
    @Query("DELETE FROM names_group WHERE uid = :uid")
    void deleteName(String uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Group group);

}
