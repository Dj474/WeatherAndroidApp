package com.example.weatherapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.weatherapp.data.WeatherDatabase;
import com.example.weatherapp.data.WeatherNote;
import com.example.weatherapp.data.WeatherNoteDao;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesFragment extends Fragment {

    private RecyclerView notesRecyclerView;
    private FloatingActionButton addNoteButton;
    private TextView emptyTextView;
    private SearchView searchView;
    private ImageButton sortButton;

    private NotesAdapter notesAdapter;
    private List<WeatherNote> notesList;
    private WeatherNoteDao noteDao;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Переменные для хранения актуальной погоды
    private double currentTempFromApi = 0.0;
    private String currentCityFromApi = "Minsk";

    // Параметры фильтрации
    private String currentSearch = "%%";
    private String currentSort = "date_desc";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        initViews(view);
        noteDao = WeatherDatabase.getDatabase(requireContext()).weatherNoteDao();

        // Подключаемся к погодной ViewModel через Activity
        WeatherViewModel weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        weatherViewModel.weatherData.observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                currentTempFromApi = response.current.temp_c;
                currentCityFromApi = response.location.name;
            }
        });

        setupRecyclerView();
        setupListeners();
        loadNotes();

        return view;
    }

    private void initViews(View view) {
        notesRecyclerView = view.findViewById(R.id.notesRecyclerView);
        addNoteButton = view.findViewById(R.id.addNoteButton);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        searchView = view.findViewById(R.id.searchView);
        sortButton = view.findViewById(R.id.sortButton);
    }

    private void setupRecyclerView() {
        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList, new NotesAdapter.OnNoteClickListener() {
            @Override public void onNoteClick(WeatherNote note) { showEditNoteDialog(note); }
            @Override public void onNoteLongClick(WeatherNote note) { showDeleteDialog(note); }
        });
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notesRecyclerView.setAdapter(notesAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                WeatherNote note = notesList.get(position);
                deleteNote(note);
                Snackbar.make(notesRecyclerView, "Заметка удалена", Snackbar.LENGTH_LONG)
                        .setAction("Отмена", v -> saveNote(note)).show();
            }
        });
        itemTouchHelper.attachToRecyclerView(notesRecyclerView);
    }

    private void setupListeners() {
        addNoteButton.setOnClickListener(v -> showAddNoteDialog());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                currentSearch = "%" + newText + "%";
                loadNotes();
                return true;
            }
        });
        sortButton.setOnClickListener(v -> showSortDialog());
    }

    private void loadNotes() {
        noteDao.getFilteredNotes(currentSearch, "%%", -100.0, 100.0, 0, Long.MAX_VALUE, currentSort)
                .observe(getViewLifecycleOwner(), notes -> {
                    notesList.clear();
                    if (notes != null) notesList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                    emptyTextView.setVisibility(notesList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void showSortDialog() {
        String[] options = {"Новые сначала", "Старые сначала", "Температура ↑", "Температура ↓"};
        String[] codes = {"date_desc", "date_asc", "temp_asc", "temp_desc"};
        new AlertDialog.Builder(getContext()).setTitle("Сортировка")
                .setItems(options, (dialog, which) -> {
                    currentSort = codes[which];
                    loadNotes();
                }).show();
    }

    private void saveNote(WeatherNote note) { executorService.execute(() -> noteDao.insert(note)); }
    private void updateNote(WeatherNote note) { executorService.execute(() -> noteDao.update(note)); }
    private void deleteNote(WeatherNote note) { executorService.execute(() -> noteDao.delete(note)); }

    private void showAddNoteDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        EditText titleEt = dialogView.findViewById(R.id.titleEditText);
        EditText descEt = dialogView.findViewById(R.id.descriptionEditText);

        new AlertDialog.Builder(getContext()).setTitle("Новая заметка").setView(dialogView)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String title = titleEt.getText().toString().trim();
                    if (!title.isEmpty()) {
                        // Используем ПОЛУЧЕННУЮ ТЕМПЕРАТУРУ И ГОРОД
                        saveNote(new WeatherNote(title, descEt.getText().toString(), new Date(), currentCityFromApi, currentTempFromApi));
                    }
                })
                .setNegativeButton("Отмена", null).show();
    }

    private void showEditNoteDialog(WeatherNote note) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        EditText titleEt = dialogView.findViewById(R.id.titleEditText);
        EditText descEt = dialogView.findViewById(R.id.descriptionEditText);
        titleEt.setText(note.getTitle());
        descEt.setText(note.getDescription());
        new AlertDialog.Builder(getContext()).setTitle("Редактировать").setView(dialogView)
                .setPositiveButton("Обновить", (d, w) -> {
                    note.setTitle(titleEt.getText().toString());
                    note.setDescription(descEt.getText().toString());
                    updateNote(note);
                }).setNegativeButton("Отмена", null).show();
    }

    private void showDeleteDialog(WeatherNote note) {
        new AlertDialog.Builder(getContext()).setTitle("Удалить?").setPositiveButton("Да", (d, w) -> deleteNote(note)).setNegativeButton("Нет", null).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}