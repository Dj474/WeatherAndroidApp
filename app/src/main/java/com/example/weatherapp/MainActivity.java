package com.example.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private ViewPagerAdapter adapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Применяем сохраненный язык до отрисовки интерфейса
        String language = LocaleHelper.getLanguage(this);
        LocaleHelper.applyLocale(this, language);

        super.onCreate(savedInstanceState);

        // 2. ПРОВЕРКА АВТОРИЗАЦИИ
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            // Если пользователь не авторизован, перенаправляем на LoginActivity
            navigateToLogin();
            return;
        }

        // 3. Инициализация интерфейса (только если юзер вошел)
        setContentView(R.layout.activity_main);

        // Настройка Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupViewPager();
        setupBottomNavigation();

        // Проверка разрешений на уведомления (для Android 13+)
        checkNotificationPermission();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // Закрываем MainActivity, чтобы нельзя было вернуться назад
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Применяем сохраненный язык к базовому контексту для корректной локализации
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(this);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // Отключаем свайп, чтобы не конфликтовал с картами/графиками

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateToolbarTitle(position);
                updateBottomNavigation(position);
            }
        });
    }

    private void updateToolbarTitle(int position) {
        if (getSupportActionBar() != null) {
            switch (position) {
                case 0:
                    getSupportActionBar().setTitle(getString(R.string.weather));
                    break;
                case 1:
                    getSupportActionBar().setTitle(getString(R.string.notes));
                    break;
                case 2:
                    getSupportActionBar().setTitle(getString(R.string.settings));
                    break;
            }
        }
    }

    private void updateBottomNavigation(int position) {
        switch (position) {
            case 0:
                bottomNavigation.setSelectedItemId(R.id.nav_weather);
                break;
            case 1:
                bottomNavigation.setSelectedItemId(R.id.nav_notes);
                break;
            case 2:
                bottomNavigation.setSelectedItemId(R.id.nav_settings);
                break;
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Настройка цветов навигации из ресурсов
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };

        int[] colors = new int[] {
                ContextCompat.getColor(this, R.color.nav_selected),
                ContextCompat.getColor(this, R.color.nav_unselected)
        };

        android.content.res.ColorStateList colorStateList = new android.content.res.ColorStateList(states, colors);
        bottomNavigation.setItemIconTintList(colorStateList);
        bottomNavigation.setItemTextColor(colorStateList);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_weather) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_notes) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_settings) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}