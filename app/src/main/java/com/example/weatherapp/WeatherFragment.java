package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);
        initViews(view);
        setupRecyclerViews();
        // Мы не вызываем loadWeatherData здесь, так как onResume сделает это за нас
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
        new FetchWeatherTask().execute(getString(R.string.moscow));
    }

    private void updateUI(String city, double temp, String description, int humidity, double wind, int pressure) {
        // --- СИНХРОНИЗАЦИЯ С SettingsFragment ---
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String units = prefs.getString("units", "celsius"); // Читаем строку "celsius" или "fahrenheit"
        boolean isCelsius = !units.equals("fahrenheit");

        double displayTemp = isCelsius ? temp : (temp * 1.8) + 32;
        String unitSymbol = isCelsius ? "°C" : "°F";
        // ---------------------------------------

        cityTextView.setText(city);
        temperatureTextView.setText(String.format(Locale.getDefault(), "%.0f%s", displayTemp, unitSymbol));
        weatherDescriptionTextView.setText(description);

        humidityTextView.setText(String.format(Locale.getDefault(), "%d%%", humidity));
        windTextView.setText(String.format(Locale.getDefault(), "%.1f %s", wind, getString(R.string.meters_per_second)));
        pressureTextView.setText(String.format(Locale.getDefault(), "%d %s", pressure, getString(R.string.millimeters)));

        updateStaticLabels();

        updateForecastLists(isCelsius);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void updateStaticLabels() {
        if (hourlyForecastLabel != null) hourlyForecastLabel.setText(getString(R.string.hourly_forecast));
        if (dailyForecastLabel != null) dailyForecastLabel.setText(getString(R.string.daily_forecast));
        if (humidityLabel != null) humidityLabel.setText(getString(R.string.humidity));
        if (windLabel != null) windLabel.setText(getString(R.string.wind));
        if (pressureLabel != null) pressureLabel.setText(getString(R.string.pressure));
    }

    private void updateForecastLists(boolean isCelsius) {
        hourlyItems.clear();
        dailyItems.clear();

        hourlyAdapter.setUnits(isCelsius);
        dailyAdapter.setUnits(isCelsius);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        for (int i = 0; i < 8; i++) {
            hourlyItems.add(new WeatherItem(sdf.format(new Date(System.currentTimeMillis() + i * 3600000)),
                    getString(R.string.sunny), 25 + i, R.drawable.ic_sunny));
        }

        String[] dayNames = {getString(R.string.today), getString(R.string.tomorrow), "Вт", "Ср", "Чт"};
        for (int i = 0; i < 5; i++) {
            dailyItems.add(new WeatherItem(dayNames[i], getString(R.string.sunny), 22 + i, R.drawable.ic_sunny));
        }

        hourlyAdapter.notifyDataSetChanged();
        dailyAdapter.notifyDataSetChanged();
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try { Thread.sleep(300); } catch (Exception ignored) {}
            return String.format(Locale.getDefault(),
                    "{\"city\":\"%s\",\"temp\":25,\"description\":\"%s\",\"humidity\":65,\"wind\":5.0,\"pressure\":760}",
                    params[0], getString(R.string.sunny));
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);
                updateUI(json.getString("city"), json.getDouble("temp"), json.getString("description"),
                        json.getInt("humidity"), json.getDouble("wind"), json.getInt("pressure"));
            } catch (Exception e) {
                updateUI(getString(R.string.moscow), 25, getString(R.string.sunny), 65, 5, 760);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWeatherData();
    }
}