package com.example.weatherapp;

public class WeatherItem {
    private String time;
    private String description;
    private double temperature;
    private int iconResId;

    public WeatherItem(String time, String description, double temperature, int iconResId) {
        this.time = time;
        this.description = description;
        this.temperature = temperature;
        this.iconResId = iconResId;
    }

    public String getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getIconResId() {
        return iconResId;
    }
}