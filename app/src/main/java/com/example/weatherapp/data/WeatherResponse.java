package com.example.weatherapp.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    @SerializedName("location")
    public Location location;

    @SerializedName("current")
    public Current current;

    @SerializedName("forecast")
    public Forecast forecast;

    public static class Location {
        public String name;
    }

    public static class Current {
        @SerializedName("temp_c")
        public double temp_c;
        public int humidity;
        @SerializedName("wind_kph")
        public double wind_kph;
        @SerializedName("pressure_mb")
        public double pressure_mb;
        public Condition condition;
    }

    public static class Condition {
        public String text;
        public String icon;
    }

    public static class Forecast {
        @SerializedName("forecastday")
        public List<ForecastDay> forecastday;
    }

    public static class ForecastDay {
        public String date;
        @SerializedName("day")
        public Day day;
        @SerializedName("hour")
        public List<Hour> hour;
    }

    public static class Day {
        @SerializedName("avgtemp_c")
        public double avgtemp_c;
        public Condition condition;
    }

    public static class Hour {
        public String time;
        @SerializedName("temp_c")
        public double temp_c;
        public Condition condition;
    }
}