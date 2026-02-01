package com.example.weatherapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        initViews(view);
        setupRecyclerViews();
        updateTexts();
        loadWeatherData();

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
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadWeatherData();
            swipeRefreshLayout.setRefreshing(false);
        });

        hourlyRecyclerView = view.findViewById(R.id.hourlyRecyclerView);
        dailyRecyclerView = view.findViewById(R.id.dailyRecyclerView);
    }

    private void updateTexts() {
        if (hourlyForecastLabel != null) {
            hourlyForecastLabel.setText(getString(R.string.hourly_forecast));
        }
        if (dailyForecastLabel != null) {
            dailyForecastLabel.setText(getString(R.string.daily_forecast));
        }
        if (cityTextView != null) {
            cityTextView.setText(getString(R.string.moscow));
        }
        if (weatherDescriptionTextView != null) {
            weatherDescriptionTextView.setText(getString(R.string.sunny));
        }
        if (humidityLabel != null) {
            humidityLabel.setText(getString(R.string.humidity));
        }
        if (windLabel != null) {
            windLabel.setText(getString(R.string.wind));
        }
        if (pressureLabel != null) {
            pressureLabel.setText(getString(R.string.pressure));
        }
    }

    private void setupRecyclerViews() {
        hourlyItems = new ArrayList<>();
        dailyItems = new ArrayList<>();

        hourlyAdapter = new WeatherAdapter(getContext(), hourlyItems, true);
        dailyAdapter = new WeatherAdapter(getContext(), dailyItems, false);

        hourlyRecyclerView.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false));
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        hourlyRecyclerView.setAdapter(hourlyAdapter);
        dailyRecyclerView.setAdapter(dailyAdapter);
    }

    private void loadWeatherData() {
        // В реальном приложении здесь должен быть API ключ
        new FetchWeatherTask().execute(getString(R.string.moscow));
    }

    private void updateUI(String city, double temp, String description,
                          int humidity, double wind, int pressure) {
        cityTextView.setText(city);
        temperatureTextView.setText(String.format(Locale.getDefault(), "%.0f°C", temp));
        weatherDescriptionTextView.setText(description);
        humidityTextView.setText(String.format(Locale.getDefault(), "%d%s", humidity, getString(R.string.percent)));
        windTextView.setText(String.format(Locale.getDefault(), "%.1f %s", wind, getString(R.string.meters_per_second)));
        pressureTextView.setText(String.format(Locale.getDefault(), "%d %s", pressure, getString(R.string.millimeters)));

        // Обновляем списки с тестовыми данными, используя строки из ресурсов
        updateForecastLists();
    }

    private void updateForecastLists() {
        hourlyItems.clear();
        dailyItems.clear();

        // Тестовые данные для почасового прогноза
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        for (int i = 0; i < 8; i++) {
            Date time = new Date(System.currentTimeMillis() + i * 3600000);
            WeatherItem item = new WeatherItem(
                    sdf.format(time),
                    getString(R.string.sunny),  // Используем строку из ресурсов
                    25 + i,
                    R.drawable.ic_sunny
            );
            hourlyItems.add(item);
        }

        // Тестовые данные для прогноза на 5 дней
        String[] dayNames = {
                getString(R.string.today),
                getString(R.string.tomorrow),
                getString(R.string.tuesday),
                getString(R.string.wednesday),
                getString(R.string.thursday)
        };

        String[] descriptions = {
                getString(R.string.sunny),
                getString(R.string.cloudy),
                getString(R.string.sunny),
                getString(R.string.rainy),
                getString(R.string.cloudy)
        };

        int[] icons = {
                R.drawable.ic_sunny,
                R.drawable.ic_cloudy,
                R.drawable.ic_sunny,
                R.drawable.ic_sunny, // Замени на ic_rainy если есть
                R.drawable.ic_cloudy
        };

        for (int i = 0; i < 5; i++) {
            WeatherItem item = new WeatherItem(
                    dayNames[i],
                    descriptions[i],  // Используем строку из ресурсов
                    22 + i,
                    icons[i]
            );
            dailyItems.add(item);
        }

        hourlyAdapter.notifyDataSetChanged();
        dailyAdapter.notifyDataSetChanged();
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String city = params[0];
            // Здесь должен быть реальный API запрос
            // Для примера возвращаем тестовые данные
            return String.format(Locale.getDefault(),
                    "{\"city\":\"%s\",\"temp\":25,\"description\":\"%s\",\"humidity\":65,\"wind\":5,\"pressure\":760}",
                    city, getString(R.string.sunny));
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);
                updateUI(
                        json.getString("city"),
                        json.getDouble("temp"),
                        json.getString("description"),
                        json.getInt("humidity"),
                        json.getDouble("wind"),
                        json.getInt("pressure")
                );
            } catch (Exception e) {
                Toast.makeText(getContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                // Показываем тестовые данные даже при ошибке
                updateUI(getString(R.string.moscow), 25, getString(R.string.sunny), 65, 5, 760);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTexts();
        updateForecastLists();
    }
}