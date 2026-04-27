package com.example.weatherapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.data.WeatherNote;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NotesFragment extends Fragment {

    private RecyclerView notesRecyclerView;
    private NotesAdapter notesAdapter;
    private TextView emptyTextView;
    private SearchView searchView;
    private ImageButton sortButton, filterButton;

    private List<WeatherNote> allNotesList = new ArrayList<>();
    private List<WeatherNote> filteredList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration notesListener;

    private String currentSortMode = "date_desc";
    private double minTemp = -100.0;
    private double maxTemp = 100.0;
    private long startDate = 0L;
    private long endDate = Long.MAX_VALUE;

    private double currentTempFromApi = 0.0;
    private String currentCityFromApi = "Minsk";

    private Uri selectedImageUri = null;
    private Bitmap cameraBitmap = null;
    private ImageView currentPreviewIv = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    cameraBitmap = null;
                    if (currentPreviewIv != null) {
                        currentPreviewIv.setImageURI(uri);
                        currentPreviewIv.setVisibility(View.VISIBLE);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    cameraBitmap = bitmap;
                    selectedImageUri = null;
                    if (currentPreviewIv != null) {
                        currentPreviewIv.setImageBitmap(bitmap);
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

        WeatherViewModel weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        weatherViewModel.weatherData.observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                currentTempFromApi = response.current.temp_c;
                currentCityFromApi = response.location.name;
            }
        });

        startNotesRealtimeUpdates();
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
            @Override public void onNoteLongClick(WeatherNote note) { showActionMenu(note); } // Изменено на меню действий
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

    private void startNotesRealtimeUpdates() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        if (notesListener != null) notesListener.remove();

        notesListener = db.collection("notes")
                .whereEqualTo("userId", currentUid)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firebase", "Ошибка подписки", error);
                        return;
                    }
                    if (value != null) {
                        allNotesList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            WeatherNote note = doc.toObject(WeatherNote.class);
                            note.setId(doc.getId());
                            allNotesList.add(note);
                        }
                        applyAllFilters();
                    }
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
        if (notesAdapter != null) {
            notesAdapter.notifyDataSetChanged();
            emptyTextView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
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

    private String encodeBitmapToBase64(Bitmap bitmap) {
        int maxSize = 600;
        float ratio = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
        int width = Math.round(ratio * bitmap.getWidth());
        int height = Math.round(ratio * bitmap.getHeight());
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    private void uploadAndSave(WeatherNote note, boolean isUpdate) {
        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), selectedImageUri);
                note.setImageUrl(encodeBitmapToBase64(bitmap));
            } catch (IOException e) { e.printStackTrace(); }
        } else if (cameraBitmap != null) {
            note.setImageUrl(encodeBitmapToBase64(cameraBitmap));
        }

        if (isUpdate) {
            db.collection("notes").document(note.getId()).set(note);
        } else {
            db.collection("notes").add(note);
        }
        selectedImageUri = null;
        cameraBitmap = null;
    }

    // --- ФУНКЦИЯ ПОДЕЛИТЬСЯ (Соцсети) ---
    private void shareNote(WeatherNote note) {
        String shareText = "🌤 Моя погодная заметка: " + note.getTitle() + "\n" +
                "📍 Город: " + note.getCity() + "\n" +
                "🌡 Температура: " + note.getTemperature() + "°C\n" +
                "📝 Описание: " + note.getDescription();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Поделиться через:");
        startActivity(shareIntent);
    }

    private void showActionMenu(WeatherNote note) {
        String[] options = {"Поделиться", "Удалить", "Отмена"};
        new AlertDialog.Builder(getContext())
                .setTitle("Выберите действие")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) shareNote(note);
                    else if (which == 1) showDeleteDialog(note);
                }).show();
    }

    private void showImageSourceDialog() {
        String[] options = {"Сделать фото", "Выбрать из галереи"};
        new AlertDialog.Builder(getContext())
                .setTitle("Добавить фото")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) takePictureLauncher.launch(null);
                    else pickImageLauncher.launch("image/*");
                }).show();
    }

    private void showAddNoteDialog() {
        selectedImageUri = null;
        cameraBitmap = null;
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        currentPreviewIv = v.findViewById(R.id.noteImageView);
        v.findViewById(R.id.selectImageButton).setOnClickListener(view -> showImageSourceDialog());

        new AlertDialog.Builder(getContext()).setTitle("Новая заметка").setView(v)
                .setPositiveButton("ОК", (d, w) -> {
                    String t = ((EditText)v.findViewById(R.id.titleEditText)).getText().toString();
                    String desc = ((EditText)v.findViewById(R.id.descriptionEditText)).getText().toString();
                    WeatherNote newNote = new WeatherNote(t, desc, new Date(), currentCityFromApi, currentTempFromApi);
                    newNote.setUserId(FirebaseAuth.getInstance().getUid());
                    uploadAndSave(newNote, false);
                }).setNegativeButton("Отмена", null).show();
    }

    private void showEditNoteDialog(WeatherNote note) {
        selectedImageUri = null;
        cameraBitmap = null;
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        EditText tEt = v.findViewById(R.id.titleEditText);
        EditText dEt = v.findViewById(R.id.descriptionEditText);
        currentPreviewIv = v.findViewById(R.id.noteImageView);

        tEt.setText(note.getTitle());
        dEt.setText(note.getDescription());

        if (note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
            currentPreviewIv.setVisibility(View.VISIBLE);
            byte[] decodedString = Base64.decode(note.getImageUrl(), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            currentPreviewIv.setImageBitmap(decodedByte);
        }

        v.findViewById(R.id.selectImageButton).setOnClickListener(view -> showImageSourceDialog());

        new AlertDialog.Builder(getContext()).setTitle("Правка").setView(v)
                .setPositiveButton("Обновить", (d, w) -> {
                    note.setTitle(tEt.getText().toString());
                    note.setDescription(dEt.getText().toString());
                    uploadAndSave(note, true);
                }).setNegativeButton("Отмена", null).show();
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

        new AlertDialog.Builder(getContext()).setTitle("Фильтр").setView(v)
                .setPositiveButton("ОК", (d, w) -> {
                    try {
                        minTemp = minTempEt.getText().toString().isEmpty() ? -100.0 : Double.parseDouble(minTempEt.getText().toString());
                        maxTemp = maxTempEt.getText().toString().isEmpty() ? 100.0 : Double.parseDouble(maxTempEt.getText().toString());
                        applyAllFilters();
                    } catch (Exception ignored) {}
                })
                .setNeutralButton("Сброс", (d, w) -> {
                    minTemp = -100.0; maxTemp = 100.0;
                    applyAllFilters();
                }).show();
    }

    private void showDeleteDialog(WeatherNote note) {
        new AlertDialog.Builder(getContext()).setTitle("Удалить заметку?")
                .setPositiveButton("Да", (d, w) -> db.collection("notes").document(note.getId()).delete())
                .setNegativeButton("Нет", null).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notesListener != null) {
            notesListener.remove();
        }
    }
}