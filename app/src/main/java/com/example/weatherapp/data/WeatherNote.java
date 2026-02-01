package com.example.weatherapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "weather_notes")
public class WeatherNote {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String description;
    private Date date;
    private String city;
    private double temperature;

    public WeatherNote(String title, String description, Date date, String city, double temperature) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.city = city;
        this.temperature = temperature;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}