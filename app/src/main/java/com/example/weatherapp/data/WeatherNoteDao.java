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
    @Query("SELECT * FROM weather_notes WHERE " +
            "(title LIKE :search OR description LIKE :search) AND " +
            "(city LIKE :city) AND " +
            "(temperature BETWEEN :minTemp AND :maxTemp) AND " +
            "(date BETWEEN :startDate AND :endDate) " +
            "ORDER BY " +
            "CASE WHEN :sortMethod = 'date_asc' THEN date END ASC, " +
            "CASE WHEN :sortMethod = 'date_desc' THEN date END DESC, " +
            "CASE WHEN :sortMethod = 'temp_asc' THEN temperature END ASC, " +
            "CASE WHEN :sortMethod = 'temp_desc' THEN temperature END DESC")
    LiveData<List<WeatherNote>> getFilteredNotes(
            String search, String city,
            double minTemp, double maxTemp,
            long startDate, long endDate,
            String sortMethod);

    @Insert
    void insert(WeatherNote note);

    @Update
    void update(WeatherNote note);

    @Delete
    void delete(WeatherNote note);

    // Оставляем старый для совместимости или дефолтной загрузки
    @Query("SELECT * FROM weather_notes ORDER BY date DESC")
    LiveData<List<WeatherNote>> getAllNotes();

    @Query("DELETE FROM weather_notes WHERE id = :id")
    void deleteById(int id);
}