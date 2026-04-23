package com.example.weatherapp.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.weatherapp.data.WeatherApiService;
import com.example.weatherapp.data.WeatherResponse;
import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherViewModel extends AndroidViewModel {

    private final MutableLiveData<WeatherResponse> _weatherData = new MutableLiveData<>();
    public LiveData<WeatherResponse> weatherData = _weatherData;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final WeatherApiService apiService;
    private final String API_KEY = "89349377ed834bcd846103229262304";
    private static final String PREFS_NAME = "weather_prefs";
    private static final String KEY_LAST_WEATHER = "last_weather_json";

    public WeatherViewModel(@NonNull Application application) {
        super(application);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(WeatherApiService.class);
    }

    public void refreshWeather(String city, String lang) {
        if (!isNetworkAvailable()) {
            loadFromCache();
            _errorMessage.setValue("Нет сети. Загружены последние данные.");
            return;
        }

        apiService.getForecast(API_KEY, city, 5, lang).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveToCache(response.body());
                    _weatherData.setValue(response.body());
                } else {
                    _errorMessage.setValue("Ошибка сервера: " + response.code());
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                _errorMessage.setValue("Ошибка сети");
                loadFromCache();
            }
        });
    }
    private void saveToCache(WeatherResponse data) {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(data);
        prefs.edit().putString(KEY_LAST_WEATHER, json).apply();
    }

    private void loadFromCache() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_LAST_WEATHER, null);
        if (json != null) {
            try {
                WeatherResponse cachedData = new Gson().fromJson(json, WeatherResponse.class);
                _weatherData.setValue(cachedData);
            } catch (Exception e) {
                _errorMessage.setValue("Ошибка чтения кэша");
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm != null ? cm.getActiveNetworkInfo() : null;
        return netInfo != null && netInfo.isConnected();
    }
}