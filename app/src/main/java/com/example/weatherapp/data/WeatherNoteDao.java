package com.example.weatherapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WeatherNoteDao {

    @Query("SELECT * FROM weather_notes ORDER BY date DESC")
    LiveData<List<WeatherNote>> getAllNotes();

    @Query("SELECT * FROM weather_notes WHERE id = :id")
    WeatherNote getNoteById(int id);

    @Insert
    void insert(WeatherNote note);

    @Update
    void update(WeatherNote note);

    @Delete
    void delete(WeatherNote note);

    @Query("DELETE FROM weather_notes WHERE id = :id")
    void deleteById(int id);
}