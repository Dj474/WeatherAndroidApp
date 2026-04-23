package com.example.weatherapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private RadioGroup themeRadioGroup, languageRadioGroup, unitsRadioGroup, notificationRadioGroup;
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
        notificationRadioGroup = view.findViewById(R.id.notificationRadioGroup);
        saveButton = view.findViewById(R.id.saveButton);

        sharedPreferences = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        loadCurrentSettings();

        saveButton.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void loadCurrentSettings() {
        // ... (код загрузки темы, языка и единиц остается прежним)
        String theme = sharedPreferences.getString("theme", "system");
        if (theme.equals("light")) ((RadioButton) themeRadioGroup.findViewById(R.id.lightThemeRadio)).setChecked(true);
        else if (theme.equals("dark")) ((RadioButton) themeRadioGroup.findViewById(R.id.darkThemeRadio)).setChecked(true);
        else ((RadioButton) themeRadioGroup.findViewById(R.id.systemThemeRadio)).setChecked(true);

        String language = sharedPreferences.getString("language", "ru");
        if (language.equals("en")) ((RadioButton) languageRadioGroup.findViewById(R.id.englishRadio)).setChecked(true);
        else ((RadioButton) languageRadioGroup.findViewById(R.id.russianRadio)).setChecked(true);

        String units = sharedPreferences.getString("units", "celsius");
        if (units.equals("fahrenheit")) ((RadioButton) unitsRadioGroup.findViewById(R.id.fahrenheitRadio)).setChecked(true);
        else ((RadioButton) unitsRadioGroup.findViewById(R.id.celsiusRadio)).setChecked(true);

        // Загрузка настроек уведомлений
        int savedNotificationId = sharedPreferences.getInt("notification_id", R.id.notifyOff);
        RadioButton rb = notificationRadioGroup.findViewById(savedNotificationId);
        if (rb != null) rb.setChecked(true);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Тема
        String theme;
        int selectedThemeId = themeRadioGroup.getCheckedRadioButtonId();
        if (selectedThemeId == R.id.lightThemeRadio) theme = "light";
        else if (selectedThemeId == R.id.darkThemeRadio) theme = "dark";
        else theme = "system";

        // Язык и единицы
        String language = (languageRadioGroup.getCheckedRadioButtonId() == R.id.englishRadio) ? "en" : "ru";
        String units = (unitsRadioGroup.getCheckedRadioButtonId() == R.id.fahrenheitRadio) ? "fahrenheit" : "celsius";

        // Уведомления
        int selectedNotifyId = notificationRadioGroup.getCheckedRadioButtonId();
        int minutes = 0;
        if (selectedNotifyId == R.id.notify30s) minutes = -1; // специальный код для 30 сек
        else if (selectedNotifyId == R.id.notify5m) minutes = 5;
        else if (selectedNotifyId == R.id.notify60m) minutes = 60;

        editor.putString("theme", theme);
        editor.putString("language", language);
        editor.putString("units", units);
        editor.putInt("notification_id", selectedNotifyId);
        editor.apply();

        // Применяем уведомления
        setupNotifications(minutes);

        ThemeManager.setThemeMode(theme);
        LocaleHelper.applyLocale(requireContext(), language);
        restartApp();
    }

    private void setupNotifications(int minutes) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (minutes == 0) {
            alarmManager.cancel(pendingIntent);
            return;
        }

        long intervalMillis;
        if (minutes == -1) intervalMillis = 30 * 1000L; // 30 секунд
        else intervalMillis = minutes * 60 * 1000L;

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                pendingIntent
        );

        Toast.makeText(getContext(), "Уведомления настроены", Toast.LENGTH_SHORT).show();
    }

    private void restartApp() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}