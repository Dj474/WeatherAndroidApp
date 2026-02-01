package com.example.weatherapp;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем сохраненный язык
        String language = LocaleHelper.getLanguage(this);
        LocaleHelper.applyLocale(this, language);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupViewPager();
        setupBottomNavigation();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Применяем сохраненный язык к базовому контексту
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(this);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

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

        // Настройка цветов
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };

        int[] colors = new int[] {
                getResources().getColor(R.color.nav_selected),
                getResources().getColor(R.color.nav_unselected)
        };

        android.content.res.ColorStateList colorStateList = new android.content.res.ColorStateList(states, colors);
        bottomNavigation.setItemIconTintList(colorStateList);
        bottomNavigation.setItemTextColor(colorStateList);

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_weather) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (item.getItemId() == R.id.nav_notes) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });
    }
}