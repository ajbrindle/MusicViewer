package com.sk7software.musicviewer.list;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sk7software.musicviewer.R;
import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.network.NetworkRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MusicListActivity extends AppCompatActivity {

    // EMULATOR and Chromebook
    public static final String MUSIC_DIR = "/data/data/com.sk7software.musicviewer/";
    // P20 Phone
    //public static final String MUSIC_DIR = "/sdcard/Android/data/com.sk7software.musicviewer/";
    private static final String TAG = MusicListActivity.class.getSimpleName();

    private List<MusicFile> items;
    private List<String> listItems;
    private ArrayAdapter<String> adapter;
    private AlertDialog.Builder progressDialogBuilder;
    private Dialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);

        // Populate list
        ListView lv = (ListView) findViewById(R.id.musicList);
        items = new ArrayList<>();
        listItems = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, listItems);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent();
                addMetaData(i, listItems.get(position));
                setResult(Activity.RESULT_OK, i);
                finish();
            }
        });
        // Create progress dialog for use later
        progressDialogBuilder = new AlertDialog.Builder(MusicListActivity.this);
        progressDialogBuilder.setView(R.layout.progress);
        setProgress(true, "Fetching music files");
        getMusicPDFs();
    }

    private void addMetaData(Intent i, String name) {
        for (MusicFile f : items) {
            if (name.equals(f.getName())) {
                i.putExtra("file", f);
                return;
            }
        }
    }

    private void getMusicPDFs() {
        NetworkRequest.fetchFiles(getApplicationContext(),new NetworkRequest.NetworkCallback() {
            @Override
            public void onRequestCompleted(List<MusicFile> callbackData) {
                Log.d(TAG, "File lookup completed:");
                for (MusicFile f : callbackData) {
                    Log.d(TAG, f.getName());
                }
                items.clear();
                items.addAll(callbackData);
                items.addAll(addLocalFiles());

                for (MusicFile f : items) {
                    listItems.add(f.getName());
                }
                adapter.notifyDataSetChanged();
                setProgress(false, "");
            }

            @Override
            public void onError(Exception e) {
                setProgress(false, "");
                Log.d(TAG, "Error loading remote files: " + e.getMessage());
            }
        });
    }

    private List<MusicFile> addLocalFiles() {
        File directory = new File(MUSIC_DIR);
        List<MusicFile> musicFiles = new ArrayList<>();

        List<File> files = Arrays.asList(directory.listFiles()).stream()
                .filter(file -> file.getName().endsWith("pdf")).collect(Collectors.toList());

        for (File f : files) {
            Log.d(TAG, "Found music pdf: " + f.getAbsolutePath());
            MusicFile mf = new MusicFile();
            mf.setName(f.getName());
            mf.setDelay(350);
            mf.setEndDelay(20);
            mf.setTopPct(10);
            mf.setBottomPct(15);
            musicFiles.add(mf);
        }
        return musicFiles;
    }

    public void setProgress(boolean showProgressDialog, String progressMessage) {
        if (showProgressDialog && progressDialog == null) {
            progressDialog = progressDialogBuilder
                    .setMessage(progressMessage)
                    .create();
            progressDialog.show();
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }
}