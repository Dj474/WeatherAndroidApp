package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private RadioGroup themeRadioGroup, languageRadioGroup, unitsRadioGroup;
    private Button saveButton;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        themeRadioGroup = view.findViewById(R.id.themeRadioGroup);
        languageRadioGroup = view.findViewById(R.id.languageRadioGroup);
        unitsRadioGroup = view.findViewById(R.id.unitsRadioGroup);
        saveButton = view.findViewById(R.id.saveButton);

        sharedPreferences = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        loadCurrentSettings();

        saveButton.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void loadCurrentSettings() {
        // Загрузка темы
        String theme = sharedPreferences.getString("theme", "system");
        if (theme.equals("light")) {
            ((RadioButton) themeRadioGroup.findViewById(R.id.lightThemeRadio)).setChecked(true);
        } else if (theme.equals("dark")) {
            ((RadioButton) themeRadioGroup.findViewById(R.id.darkThemeRadio)).setChecked(true);
        } else {
            ((RadioButton) themeRadioGroup.findViewById(R.id.systemThemeRadio)).setChecked(true);
        }

        // Загрузка языка
        String language = sharedPreferences.getString("language", "ru");
        if (language.equals("en")) {
            ((RadioButton) languageRadioGroup.findViewById(R.id.englishRadio)).setChecked(true);
        } else {
            ((RadioButton) languageRadioGroup.findViewById(R.id.russianRadio)).setChecked(true);
        }

        // Загрузка единиц измерения
        String units = sharedPreferences.getString("units", "celsius");
        if (units.equals("fahrenheit")) {
            ((RadioButton) unitsRadioGroup.findViewById(R.id.fahrenheitRadio)).setChecked(true);
        } else {
            ((RadioButton) unitsRadioGroup.findViewById(R.id.celsiusRadio)).setChecked(true);
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Сохранение темы
        String theme;
        if (((RadioButton) themeRadioGroup.findViewById(R.id.lightThemeRadio)).isChecked()) {
            theme = "light";
        } else if (((RadioButton) themeRadioGroup.findViewById(R.id.darkThemeRadio)).isChecked()) {
            theme = "dark";
        } else {
            theme = "system";
        }
        editor.putString("theme", theme);

        // Сохранение языка
        String language = ((RadioButton) languageRadioGroup.findViewById(R.id.englishRadio)).isChecked()
                ? "en" : "ru";
        editor.putString("language", language);

        // Сохранение единиц измерения
        String units = ((RadioButton) unitsRadioGroup.findViewById(R.id.fahrenheitRadio)).isChecked()
                ? "fahrenheit" : "celsius";
        editor.putString("units", units);

        editor.apply();

        // Применение настроек
        applyTheme(theme);

        // Применяем язык через LocaleHelper
        LocaleHelper.applyLocale(requireContext(), language);

        // Перезапуск активности для применения изменений
        restartApp();
    }


    private void applyTheme(String theme) {
        switch (theme) {
            case "light":
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }


    private void applyLanguage(String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    private void restartApp() {
        // Перезапускаем MainActivity
        android.content.Intent intent = new android.content.Intent(getActivity(), MainActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Завершаем текущую активность
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}