package com.example.weatherapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.weatherapp.data.WeatherDatabase;
import com.example.weatherapp.data.WeatherNote;
import com.example.weatherapp.data.WeatherNoteDao;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesFragment extends Fragment {

    private RecyclerView notesRecyclerView;
    private FloatingActionButton addNoteButton;
    private TextView emptyTextView;
    private NotesAdapter notesAdapter;
    private List<WeatherNote> notesList;
    private WeatherNoteDao noteDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        notesRecyclerView = view.findViewById(R.id.notesRecyclerView);
        addNoteButton = view.findViewById(R.id.addNoteButton);
        emptyTextView = view.findViewById(R.id.emptyTextView);

        WeatherDatabase database = WeatherDatabase.getDatabase(requireContext());
        noteDao = database.weatherNoteDao();

        setupRecyclerView();
        loadNotes();

        addNoteButton.setOnClickListener(v -> showAddNoteDialog());

        return view;
    }

    private void setupRecyclerView() {
        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList, new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(WeatherNote note) {
                showEditNoteDialog(note);
            }

            @Override
            public void onNoteLongClick(WeatherNote note) {
                showDeleteDialog(note);
            }
        });

        notesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notesRecyclerView.setAdapter(notesAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        WeatherNote note = notesList.get(position);
                        deleteNote(note);
                    }
                }
        );
        itemTouchHelper.attachToRecyclerView(notesRecyclerView);
    }

    private void loadNotes() {
        new AsyncTask<Void, Void, List<WeatherNote>>() {
            @Override
            protected List<WeatherNote> doInBackground(Void... voids) {
                return noteDao.getAllNotes().getValue();
            }

            @Override
            protected void onPostExecute(List<WeatherNote> notes) {
                notesList.clear();
                if (notes != null) {
                    notesList.addAll(notes);
                }
                notesAdapter.notifyDataSetChanged();

                if (notesList.isEmpty()) {
                    emptyTextView.setVisibility(View.VISIBLE);
                    notesRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyTextView.setVisibility(View.GONE);
                    notesRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    private void showAddNoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.new_note));

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_note, null);

        EditText titleEditText = dialogView.findViewById(R.id.titleEditText);
        EditText descriptionEditText = dialogView.findViewById(R.id.descriptionEditText);

        builder.setView(dialogView);

        builder.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleEditText.getText().toString().trim();
                String description = descriptionEditText.getText().toString().trim();

                if (!title.isEmpty()) {
                    WeatherNote note = new WeatherNote(
                            title,
                            description,
                            new Date(),
                            getString(R.string.moscow),
                            25.0
                    );
                    saveNote(note);
                } else {
                    Toast.makeText(getContext(), getString(R.string.enter_title), Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), null);

        builder.show();
    }

    private void showEditNoteDialog(WeatherNote note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.edit_note));

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_note, null);

        EditText titleEditText = dialogView.findViewById(R.id.titleEditText);
        EditText descriptionEditText = dialogView.findViewById(R.id.descriptionEditText);

        titleEditText.setText(note.getTitle());
        descriptionEditText.setText(note.getDescription());

        builder.setView(dialogView);

        builder.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleEditText.getText().toString().trim();
                String description = descriptionEditText.getText().toString().trim();

                if (!title.isEmpty()) {
                    note.setTitle(title);
                    note.setDescription(description);
                    note.setDate(new Date());
                    updateNote(note);
                } else {
                    Toast.makeText(getContext(), getString(R.string.enter_title), Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), null);

        builder.show();
    }

    private void showDeleteDialog(WeatherNote note) {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.delete_note))
                .setMessage(getString(R.string.delete_note_confirm))
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteNote(note);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void saveNote(final WeatherNote note) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                noteDao.insert(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(getContext(), getString(R.string.note_saved), Toast.LENGTH_SHORT).show();
                loadNotes();
            }
        }.execute();
    }

    private void updateNote(final WeatherNote note) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                noteDao.update(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(getContext(), getString(R.string.note_updated), Toast.LENGTH_SHORT).show();
                loadNotes();
            }
        }.execute();
    }

    private void deleteNote(final WeatherNote note) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                noteDao.delete(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(getContext(), getString(R.string.note_deleted), Toast.LENGTH_SHORT).show();
                loadNotes();
            }
        }.execute();
    }
}