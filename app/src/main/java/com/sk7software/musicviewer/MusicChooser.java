package com.sk7software.musicviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.sk7software.musicviewer.list.MusicListActivity;
import com.sk7software.musicviewer.model.MusicFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicChooser extends AppCompatActivity {

    private List<Map<String, String>> musicMapList = new ArrayList<>();
    private SimpleAdapter musicListAdapter;
    private int selectedItem;
    private MusicFile selectedFile;

    private static final String TAG = MusicChooser.class.getSimpleName();
    private static final int AUDIO_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION);
        }

        ListView musicList = (ListView)findViewById(R.id.musicListSel);
        Button btnShow = (Button)findViewById(R.id.showMusicBtn);

        HashMap<String,String> musicMap = new HashMap<String,String>();
        musicMap.put("name", "Selected Music");
        musicMap.put("value", "Tap to choose");
        musicMapList.add(musicMap);

        musicListAdapter = new SimpleAdapter(this, musicMapList, R.layout.list_item,
                new String[]{"name", "value"}, new int[]{R.id.firstLine, R.id.secondLine});

        musicList.setAdapter(musicListAdapter);
        musicList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = position;
                Intent i = new Intent(ApplicationContextProvider.getContext(), MusicListActivity.class);
                startActivityForResult(i,1);
            }
        });

        btnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ApplicationContextProvider.getContext(), MusicActivity.class);
                i.putExtra("file", selectedFile);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        Path p = Paths.get(MusicListActivity.MUSIC_DIR + "tmp/tmp.pdf");
        if (Files.exists(p)) {
            try {
                Files.delete(p);
            } catch (IOException e) {
                Log.d(TAG, "Unable to delete tmp pdf file: " + e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                selectedFile = new MusicFile();

                // Get extra data
                if (data.hasExtra("file")) {
                    selectedFile = (MusicFile)data.getSerializableExtra("file");
                    Map<String, String> h = new HashMap<String, String>();
                    h = musicMapList.get(0);
                    h.put("value", selectedFile.getName());
                    musicListAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Selected file: " + selectedFile.getName());
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case AUDIO_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Record audio permission granted");
                }  else {
                    Log.d(TAG, "Record audio permission not granted");
                }
                return;
        }
    }
}