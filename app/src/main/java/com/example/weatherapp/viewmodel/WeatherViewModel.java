package com.example.weatherapp.viewmodel;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.weatherapp.data.WeatherApiService;
import com.example.weatherapp.data.WeatherResponse;
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
    private final String API_KEY = "bf185754ce004edcaa8191124262103";

    public WeatherViewModel(@NonNull Application application) {
        super(application);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(WeatherApiService.class);
    }

    public void refreshWeather(String city, String lang) {
        if (!isNetworkAvailable()) return;

        // Запрашиваем прогноз на 5 дней
        apiService.getForecast(API_KEY, city, 5, lang).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _weatherData.setValue(response.body());
                } else {
                    _errorMessage.setValue("Ошибка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                _errorMessage.setValue("Ошибка сети");
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm != null ? cm.getActiveNetworkInfo() : null;
        return netInfo != null && netInfo.isConnected();
    }
}