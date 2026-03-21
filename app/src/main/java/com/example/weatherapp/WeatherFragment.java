package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.weatherapp.data.WeatherResponse;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherFragment extends Fragment {

    private TextView cityTextView, temperatureTextView, weatherDescriptionTextView;
    private TextView humidityTextView, windTextView, pressureTextView;
    private TextView hourlyForecastLabel, dailyForecastLabel;
    private TextView humidityLabel, windLabel, pressureLabel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView hourlyRecyclerView, dailyRecyclerView;
    private WeatherAdapter hourlyAdapter, dailyAdapter;
    private List<WeatherItem> hourlyItems, dailyItems;
    private WeatherViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);
        initViews(view);
        setupRecyclerViews();

        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        viewModel.weatherData.observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                updateUI(response.location.name,
                        response.current.temp_c,
                        response.current.condition.text,
                        response.current.humidity,
                        response.current.wind_kph,
                        (int) response.current.pressure_mb,
                        response);
            }
        });

        return view;
    }

    private void initViews(View view) {
        cityTextView = view.findViewById(R.id.cityTextView);
        temperatureTextView = view.findViewById(R.id.temperatureTextView);
        weatherDescriptionTextView = view.findViewById(R.id.weatherDescriptionTextView);
        humidityTextView = view.findViewById(R.id.humidityTextView);
        windTextView = view.findViewById(R.id.windTextView);
        pressureTextView = view.findViewById(R.id.pressureTextView);

        hourlyForecastLabel = view.findViewById(R.id.hourlyForecastLabel);
        dailyForecastLabel = view.findViewById(R.id.dailyForecastLabel);
        humidityLabel = view.findViewById(R.id.humidityLabel);
        windLabel = view.findViewById(R.id.windLabel);
        pressureLabel = view.findViewById(R.id.pressureLabel);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::loadWeatherData);

        hourlyRecyclerView = view.findViewById(R.id.hourlyRecyclerView);
        dailyRecyclerView = view.findViewById(R.id.dailyRecyclerView);
    }

    private void setupRecyclerViews() {
        hourlyItems = new ArrayList<>();
        dailyItems = new ArrayList<>();
        hourlyAdapter = new WeatherAdapter(getContext(), hourlyItems, true);
        dailyAdapter = new WeatherAdapter(getContext(), dailyItems, false);

        hourlyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        hourlyRecyclerView.setAdapter(hourlyAdapter);
        dailyRecyclerView.setAdapter(dailyAdapter);
    }

    private void loadWeatherData() {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "ru");
        // Запрашиваем Минск напрямую, как ты и хотел
        viewModel.refreshWeather("Minsk", lang);
    }

    private void updateUI(String city, double temp, String description, int humidity, double wind, int pressure, WeatherResponse data) {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String units = prefs.getString("units", "celsius");
        boolean isCelsius = !units.equals("fahrenheit");

        // Расчет для главного экрана
        double displayTemp = isCelsius ? temp : (temp * 1.8) + 32;
        String unitSymbol = isCelsius ? "°C" : "°F";

        cityTextView.setText(getString(R.string.minsk));
        temperatureTextView.setText(String.format(Locale.getDefault(), "%.0f%s", displayTemp, unitSymbol));
        weatherDescriptionTextView.setText(description);

        humidityTextView.setText(String.format(Locale.getDefault(), "%d%%", humidity));
        windTextView.setText(String.format(Locale.getDefault(), "%.1f %s", wind, getString(R.string.meters_per_second)));
        pressureTextView.setText(String.format(Locale.getDefault(), "%d %s", pressure, getString(R.string.millimeters)));

        updateStaticLabels();
        updateForecastLists(isCelsius, data);

        swipeRefreshLayout.setRefreshing(false);
    }

    private void updateStaticLabels() {
        if (hourlyForecastLabel != null) hourlyForecastLabel.setText(getString(R.string.hourly_forecast));
        if (dailyForecastLabel != null) dailyForecastLabel.setText(getString(R.string.daily_forecast));
        if (humidityLabel != null) humidityLabel.setText(getString(R.string.humidity));
        if (windLabel != null) windLabel.setText(getString(R.string.wind));
        if (pressureLabel != null) pressureLabel.setText(getString(R.string.pressure));
    }

    private void updateForecastLists(boolean isCelsius, WeatherResponse data) {
        hourlyItems.clear();
        dailyItems.clear();

        hourlyAdapter.setUnits(isCelsius);
        dailyAdapter.setUnits(isCelsius);

        if (data != null && data.forecast != null && !data.forecast.forecastday.isEmpty()) {
            // Часы
            for (WeatherResponse.Hour h : data.forecast.forecastday.get(0).hour) {
                String time = h.time.substring(11);
                // Передаем сырую температуру (адаптер сам пересчитает) и определяем иконку
                hourlyItems.add(new WeatherItem(time, h.condition.text, (int) h.temp_c, getIcon(h.condition.text)));
            }

            // Дни
            for (WeatherResponse.ForecastDay d : data.forecast.forecastday) {
                dailyItems.add(new WeatherItem(d.date, d.day.condition.text, (int) d.day.avgtemp_c, getIcon(d.day.condition.text)));
            }
        }

        hourlyAdapter.notifyDataSetChanged();
        dailyAdapter.notifyDataSetChanged();
    }

    // Тот самый метод для иконок. Проверяет и русские, и английские фразы от API.
    private int getIcon(String desc) {
        if (desc == null) return R.drawable.ic_sunny;
        String d = desc.toLowerCase();

        // Дождь (Rain / Дождь / Морось / Drizzle)
        if (d.contains("rain") || d.contains("дождь") || d.contains("drizzle") || d.contains("морось")) {
            return R.drawable.ic_rain;
        }
        // Снег (Snow / Снег / Blizzard / Метель)
        if (d.contains("snow") || d.contains("снег") || d.contains("sleet") || d.contains("метель")) {
            return R.drawable.ic_snow;
        }
        // Облака (Cloud / Облачно / Overcast / Пасмурно)
        if (d.contains("cloud") || d.contains("облач") || d.contains("overcast") || d.contains("пасмур")) {
            return R.drawable.ic_cloudy;
        }

        // Если ничего не подошло — солнце
        return R.drawable.ic_sunny;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWeatherData();
    }
}