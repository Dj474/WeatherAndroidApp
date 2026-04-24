package com.example.weatherapp;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.weatherapp.data.WeatherNote;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.imagekit.android.ImageKit;
import com.imagekit.android.ImageKitCallback;
import com.imagekit.android.entity.UploadError;
import com.imagekit.android.entity.UploadResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class NotesFragment extends Fragment {

    private RecyclerView notesRecyclerView;
    private NotesAdapter notesAdapter;
    private TextView emptyTextView;
    private SearchView searchView;
    private ImageButton sortButton, filterButton;

    private List<WeatherNote> allNotesList = new ArrayList<>();
    private List<WeatherNote> filteredList = new ArrayList<>();

    private FirebaseFirestore db;

    // Состояние фильтров и сортировки
    private String currentSortMode = "date_desc";
    private double minTemp = -100.0;
    private double maxTemp = 100.0;
    private long startDate = 0L;
    private long endDate = Long.MAX_VALUE;

    // Данные из API
    private double currentTempFromApi = 0.0;
    private String currentCityFromApi = "Minsk";

    private Uri selectedImageUri = null;
    private ImageView currentPreviewIv = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (currentPreviewIv != null) {
                        currentPreviewIv.setImageURI(uri);
                        currentPreviewIv.setVisibility(View.VISIBLE);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupRecyclerView();
        setupListeners();

        // ПОДКЛЮЧАЕМ VIEWMODEL ДЛЯ РЕАЛЬНОЙ ПОГОДЫ
        WeatherViewModel weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        weatherViewModel.weatherData.observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                currentTempFromApi = response.current.temp_c;
                currentCityFromApi = response.location.name;
                Log.d("WeatherSync", "API Data: " + currentCityFromApi + ", " + currentTempFromApi + "°C");
            }
        });

        loadNotes();
        return view;
    }

    private void initViews(View view) {
        notesRecyclerView = view.findViewById(R.id.notesRecyclerView);
        searchView = view.findViewById(R.id.searchView);
        sortButton = view.findViewById(R.id.sortButton);
        filterButton = view.findViewById(R.id.filterButton);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        view.findViewById(R.id.addNoteButton).setOnClickListener(v -> showAddNoteDialog());
    }

    private void setupRecyclerView() {
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notesAdapter = new NotesAdapter(filteredList, new NotesAdapter.OnNoteClickListener() {
            @Override public void onNoteClick(WeatherNote note) { showEditNoteDialog(note); }
            @Override public void onNoteLongClick(WeatherNote note) { showDeleteDialog(note); }
        });
        notesRecyclerView.setAdapter(notesAdapter);
    }

    private void setupListeners() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                applyAllFilters();
                return true;
            }
        });

        sortButton.setOnClickListener(v -> showSortDialog());
        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    private void loadNotes() {
        db.collection("notes").orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    allNotesList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        WeatherNote note = doc.toObject(WeatherNote.class);
                        note.setId(doc.getId());
                        allNotesList.add(note);
                    }
                    applyAllFilters();
                });
    }

    private void applyAllFilters() {
        String query = (searchView != null) ? searchView.getQuery().toString().toLowerCase().trim() : "";
        filteredList.clear();

        for (WeatherNote note : allNotesList) {
            boolean matchesSearch = query.isEmpty() ||
                    (note.getTitle() != null && note.getTitle().toLowerCase().contains(query)) ||
                    (note.getDescription() != null && note.getDescription().toLowerCase().contains(query));

            long noteTime = (note.getDate() != null) ? note.getDate().getTime() : 0;
            boolean matchesDate = noteTime >= startDate && noteTime <= endDate;

            double noteTemp = note.getTemperature();
            boolean matchesTemp = (noteTemp >= minTemp) && (noteTemp <= maxTemp);

            if (matchesSearch && matchesDate && matchesTemp) {
                filteredList.add(note);
            }
        }

        sortFilteredList();

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                notesAdapter.notifyDataSetChanged();
                emptyTextView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }
    }

    private void sortFilteredList() {
        Collections.sort(filteredList, (n1, n2) -> {
            switch (currentSortMode) {
                case "date_asc": return n1.getDate().compareTo(n2.getDate());
                case "temp_asc": return Double.compare(n1.getTemperature(), n2.getTemperature());
                case "temp_desc": return Double.compare(n2.getTemperature(), n1.getTemperature());
                default: return n2.getDate().compareTo(n1.getDate());
            }
        });
    }

    private void uploadAndSave(WeatherNote note, boolean isUpdate) {
        if (selectedImageUri == null) {
            finalizeFirebaseOperation(note, isUpdate);
            return;
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), selectedImageUri);
            ImageKit.Companion.getInstance().uploader().upload(
                    bitmap, UUID.randomUUID().toString(), UUID.randomUUID().toString() + ".jpg",
                    true, new String[]{"weather_note"}, "/", false, "", "",
                    new ArrayList<>(), "", false, true, true, true,
                    new HashMap<>(), ImageKit.Companion.getInstance().getDefaultUploadPolicy(),
                    null, new ImageKitCallback() {
                        @Override public void onSuccess(@NonNull UploadResponse response) {
                            note.setImageUrl(response.getUrl());
                            finalizeFirebaseOperation(note, isUpdate);
                            selectedImageUri = null;
                        }
                        @Override public void onError(@NonNull UploadError error) {
                            finalizeFirebaseOperation(note, isUpdate);
                        }
                    }
            );
        } catch (IOException e) {
            finalizeFirebaseOperation(note, isUpdate);
        }
    }

    private void finalizeFirebaseOperation(WeatherNote note, boolean isUpdate) {
        if (isUpdate) {
            db.collection("notes").document(note.getId()).set(note);
        } else {
            db.collection("notes").add(note);
        }
    }

    private void showSortDialog() {
        String[] options = {"Сначала новые", "Сначала старые", "Температура ↑", "Температура ↓"};
        new AlertDialog.Builder(getContext()).setTitle("Сортировка")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: currentSortMode = "date_desc"; break;
                        case 1: currentSortMode = "date_asc"; break;
                        case 2: currentSortMode = "temp_asc"; break;
                        case 3: currentSortMode = "temp_desc"; break;
                    }
                    applyAllFilters();
                }).show();
    }

    private void showFilterDialog() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter, null);
        EditText minTempEt = v.findViewById(R.id.minTempEt);
        EditText maxTempEt = v.findViewById(R.id.maxTempEt);

        new AlertDialog.Builder(getContext()).setTitle("Фильтр температуры").setView(v)
                .setPositiveButton("Применить", (d, w) -> {
                    try {
                        minTemp = minTempEt.getText().toString().isEmpty() ? -100.0 : Double.parseDouble(minTempEt.getText().toString());
                        maxTemp = maxTempEt.getText().toString().isEmpty() ? 100.0 : Double.parseDouble(maxTempEt.getText().toString());
                        applyAllFilters();
                    } catch (Exception ignored) {}
                })
                .setNeutralButton("Сбросить", (d, w) -> {
                    minTemp = -100.0; maxTemp = 100.0;
                    applyAllFilters();
                }).show();
    }

    private void showAddNoteDialog() {
        selectedImageUri = null;
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        currentPreviewIv = v.findViewById(R.id.noteImageView);
        v.findViewById(R.id.selectImageButton).setOnClickListener(view -> pickImageLauncher.launch("image/*"));

        new AlertDialog.Builder(getContext()).setTitle("Новая заметка").setView(v)
                .setPositiveButton("ОК", (d, w) -> {
                    String t = ((EditText)v.findViewById(R.id.titleEditText)).getText().toString();
                    String desc = ((EditText)v.findViewById(R.id.descriptionEditText)).getText().toString();

                    // ЮЗАЕМ ДАННЫЕ ИЗ API
                    WeatherNote newNote = new WeatherNote(t, desc, new Date(), currentCityFromApi, currentTempFromApi);
                    uploadAndSave(newNote, false);
                }).setNegativeButton("Отмена", null).show();
    }

    private void showEditNoteDialog(WeatherNote note) {
        selectedImageUri = null;
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        EditText tEt = v.findViewById(R.id.titleEditText);
        EditText dEt = v.findViewById(R.id.descriptionEditText);
        currentPreviewIv = v.findViewById(R.id.noteImageView);

        tEt.setText(note.getTitle());
        dEt.setText(note.getDescription());
        if (note.getImageUrl() != null) {
            currentPreviewIv.setVisibility(View.VISIBLE);
            Glide.with(this).load(note.getImageUrl()).into(currentPreviewIv);
        }

        v.findViewById(R.id.selectImageButton).setOnClickListener(view -> pickImageLauncher.launch("image/*"));

        new AlertDialog.Builder(getContext()).setTitle("Правка").setView(v)
                .setPositiveButton("Обновить", (d, w) -> {
                    note.setTitle(tEt.getText().toString());
                    note.setDescription(dEt.getText().toString());
                    uploadAndSave(note, true);
                }).setNegativeButton("Отмена", null).show();
    }

    private void showDeleteDialog(WeatherNote note) {
        new AlertDialog.Builder(getContext()).setTitle("Удалить заметку?")
                .setPositiveButton("Да", (d, w) -> db.collection("notes").document(note.getId()).delete())
                .setNegativeButton("Нет", null).show();
    }
}