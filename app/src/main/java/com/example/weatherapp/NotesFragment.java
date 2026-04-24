package com.example.weatherapp;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.weatherapp.data.WeatherNote;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.imagekit.android.ImageKit;
import com.imagekit.android.ImageKitCallback;
import com.imagekit.android.entity.UploadError;
import com.imagekit.android.entity.UploadResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NotesFragment extends Fragment {

    private RecyclerView notesRecyclerView;
    private NotesAdapter notesAdapter;
    private List<WeatherNote> notesList;
    private FirebaseFirestore db;

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

        notesRecyclerView = view.findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList, new NotesAdapter.OnNoteClickListener() {
            @Override public void onNoteClick(WeatherNote note) { showEditNoteDialog(note); }
            @Override public void onNoteLongClick(WeatherNote note) { showDeleteDialog(note); }
        });
        notesRecyclerView.setAdapter(notesAdapter);

        view.findViewById(R.id.addNoteButton).setOnClickListener(v -> showAddNoteDialog());

        loadNotes();
        return view;
    }

    private void loadNotes() {
        db.collection("notes").orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    notesList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        WeatherNote note = doc.toObject(WeatherNote.class);
                        note.setId(doc.getId());
                        notesList.add(note);
                    }
                    notesAdapter.notifyDataSetChanged();
                });
    }

    private void uploadAndSave(WeatherNote note, boolean isUpdate) {
        if (selectedImageUri == null) {
            finalizeFirebaseOperation(note, isUpdate);
            return;
        }

        try {
            // 1. Преобразуем Uri в Bitmap (так как метода для Uri нет)
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                    requireContext().getContentResolver(),
                    selectedImageUri
            );

            // 2. Вызываем метод upload(Bitmap, String, String, ...)
            // Мы должны передать ВСЕ 19 параметров, которые указаны в твоей сигнатуре для Bitmap
            ImageKit.Companion.getInstance().uploader().upload(
                    bitmap,                                // 1. file (Bitmap)
                    "dummy_token",                         // 2. token (нужен Unsigned Upload в консоли)
                    UUID.randomUUID().toString() + ".jpg", // 3. fileName
                    true,                                  // 4. useUniqueFileName
                    new String[]{"demo"},                  // 5. tags (Array)
                    "/",                                   // 6. folder
                    false,                                 // 7. isPrivateFile
                    "",                                    // 8. customCoordinates
                    "",                                    // 9. responseFields
                    new ArrayList<>(),                     // 10. extensions (List)
                    "",                                    // 11. webhookUrl
                    false,                                 // 12. overwriteFile
                    true,                                  // 13. overwriteAITags
                    true,                                  // 14. overwriteTags
                    true,                                  // 15. overwriteCustomMetadata
                    new HashMap<>(),                       // 16. customMetadata (Map)
                    ImageKit.Companion.getInstance().getDefaultUploadPolicy(), // 17. policy (берем дефолтную)
                    null,                                  // 18. preprocessor
                    new ImageKitCallback() {               // 19. imageKitCallback
                        @Override
                        public void onSuccess(@NonNull UploadResponse response) {
                            note.setImageUrl(response.getUrl());
                            finalizeFirebaseOperation(note, isUpdate);
                            selectedImageUri = null;
                            Log.d("ImageKit", "Успех: " + response.getUrl());
                        }

                        @Override
                        public void onError(@NonNull UploadError error) {
                            Log.e("ImageKit", "Ошибка: " + error.getMessage());
                            finalizeFirebaseOperation(note, isUpdate);
                        }
                    }
            );
        } catch (java.io.IOException e) {
            e.printStackTrace();
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

    private void showAddNoteDialog() {
        selectedImageUri = null;
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        currentPreviewIv = v.findViewById(R.id.noteImageView);

        v.findViewById(R.id.selectImageButton).setOnClickListener(view -> pickImageLauncher.launch("image/*"));

        new AlertDialog.Builder(getContext()).setTitle("Новая заметка").setView(v)
                .setPositiveButton("ОК", (d, w) -> {
                    EditText t = v.findViewById(R.id.titleEditText);
                    EditText desc = v.findViewById(R.id.descriptionEditText);
                    WeatherNote note = new WeatherNote(t.getText().toString(), desc.getText().toString(), new Date(), "Minsk", 20.0);
                    uploadAndSave(note, false);
                }).setNegativeButton("Отмена", null).show();
    }

    private void showEditNoteDialog(WeatherNote note) {
        selectedImageUri = null;
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        EditText t = v.findViewById(R.id.titleEditText);
        EditText desc = v.findViewById(R.id.descriptionEditText);
        currentPreviewIv = v.findViewById(R.id.noteImageView);

        t.setText(note.getTitle());
        desc.setText(note.getDescription());

        if (note.getImageUrl() != null) {
            currentPreviewIv.setVisibility(View.VISIBLE);
            Glide.with(this).load(note.getImageUrl()).into(currentPreviewIv);
        }

        v.findViewById(R.id.selectImageButton).setOnClickListener(view -> pickImageLauncher.launch("image/*"));

        new AlertDialog.Builder(getContext()).setTitle("Правка").setView(v)
                .setPositiveButton("Обновить", (d, w) -> {
                    note.setTitle(t.getText().toString());
                    note.setDescription(desc.getText().toString());
                    uploadAndSave(note, true);
                }).setNegativeButton("Отмена", null).show();
    }

    private void showDeleteDialog(WeatherNote note) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить заметку?")
                .setPositiveButton("Удалить", (d, w) -> {
                    if (note.getId() != null) {
                        db.collection("notes").document(note.getId()).delete();
                    }
                })
                .setNegativeButton("Отмена", null).show();
    }
}