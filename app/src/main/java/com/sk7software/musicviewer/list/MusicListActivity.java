package com.sk7software.musicviewer.list;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sk7software.musicviewer.R;
import com.sk7software.musicviewer.network.NetworkRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MusicListActivity extends AppCompatActivity {

    // EMULATOR
    //public static final String MUSIC_DIR = "/data/data/com.sk7software.musicviewer/";
    // P20 Phone
    public static final String MUSIC_DIR = "/sdcard/Android/data/com.sk7software.musicviewer/";
    private static final String TAG = MusicListActivity.class.getSimpleName();

    private List<String> items;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);

        // Populate list
        ListView lv = (ListView) findViewById(R.id.musicList);
        items = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, items);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent();
                i.putExtra("name", items.get(position));
                setResult(Activity.RESULT_OK, i);
                finish();
            }
        });
        getMusicPDFs();
    }

    private void getMusicPDFs() {
        NetworkRequest.fetchFiles(getApplicationContext(),new NetworkRequest.NetworkCallback() {
            @Override
            public void onRequestCompleted(List<String> callbackData) {
                Log.d(TAG, "File lookup completed:");
                for (String f : callbackData) {
                    Log.d(TAG, f);
                }
                items.clear();
                items.addAll(callbackData);
                items.addAll(addLocalFiles());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "Error loading remote files: " + e.getMessage());
            }
        });
    }

    private List<String> addLocalFiles() {
        File directory = new File(MUSIC_DIR);
        List<String> musicFiles = new ArrayList<>();

        List<File> files = Arrays.asList(directory.listFiles()).stream()
                .filter(file -> file.getName().endsWith("pdf")).collect(Collectors.toList());

        for (File f : files) {
            Log.d(TAG, "Found music pdf: " + f.getAbsolutePath());
            musicFiles.add(f.getName());
        }
        return musicFiles;
    }
}