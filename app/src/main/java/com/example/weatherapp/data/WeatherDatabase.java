package com.example.weatherapp.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {WeatherNote.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class}) // Добавьте эту аннотацию
public abstract class WeatherDatabase extends RoomDatabase {

    public abstract WeatherNoteDao weatherNoteDao();

    private static volatile WeatherDatabase INSTANCE;

    public static WeatherDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (WeatherDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    WeatherDatabase.class, "weather_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}