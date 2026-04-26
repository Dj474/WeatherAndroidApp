package com.example.weatherapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

public class WeatherNote {
    //@PrimaryKey(autoGenerate = true)
    private String userId;
    private String id;
    private String title;
    private String description;
    private String imageUrl; // Ссылка на фото в ImageKit
    private Date date;
    private String city;
    private double temperature;

    public WeatherNote() {} // Обязательно для Firebase

    public WeatherNote(String title, String description, Date date, String city, double temperature) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.city = city;
        this.temperature = temperature;
    }

    // Геттеры и сеттеры для imageUrl
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}