package com.sk7software.musicviewer;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk7software.musicviewer.list.MusicListActivity;
import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.network.FileDownloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MusicActivity extends AppCompatActivity implements ITurnablePage {

    private SpeechRecognizer recognizer;
    private ImageView img;
    private long lastTurn = 0;
    private int scrollMode;
    private TextView txtRate;
    private MusicFile selectedFile;
    private PdfHelper pdfHelper;


    private static final String TAG = MusicActivity.class.getSimpleName();
    private static final long MIN_TURN_INTERVAL = 5000;
    private static final int MODE_GO = 0;
    private static final int MODE_PAUSE = 1;

    ActivityResultLauncher<Intent> settingsResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "Result: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent i = result.getData();
                        selectedFile = (MusicFile) i.getSerializableExtra("file");
                        txtRate.setText(String.valueOf(selectedFile.getDelay()));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scrollMode = MODE_PAUSE;

        createSpeechRecogniser();

        selectedFile = (MusicFile)getIntent().getSerializableExtra("file");

        Button go = (Button)findViewById(R.id.go);
        Button slower = (Button)findViewById(R.id.slower);
        Button faster = (Button)findViewById(R.id.faster);
        txtRate = (TextView)findViewById(R.id.rate);
        txtRate.setText(String.valueOf(selectedFile.getDelay()));

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scrollMode == MODE_PAUSE) {
                    pdfHelper.setStopScroll(false);
                    pdfHelper.scroll();
                    scrollMode = MODE_GO;
                    go.setText("PAUSE");
                } else {
                    pdfHelper.setStopScroll(true);
                    scrollMode = MODE_PAUSE;
                    go.setText("GO");
                }
            }
        });

        slower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int delay = selectedFile.getDelay();
                delay += delay/20;
                selectedFile.setDelay(delay);
                txtRate.setText(String.valueOf(selectedFile.getDelay()));
            }
        });

        faster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int delay = selectedFile.getDelay();
                delay -= delay/20;
                selectedFile.setDelay(delay);
                txtRate.setText(String.valueOf(selectedFile.getDelay()));
            }
        });

        img = (ImageView) findViewById(R.id.imgView);
        pdfHelper = new PdfHelper(img, selectedFile, getScreenSize(), this);
        pdfHelper.showPDF();

        if (recognizer != null) {
            recognizer.startListening(createSpeechIntent());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recognizer != null) {
            recognizer.destroy();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                // Open settings dialog
                Intent settings = new Intent(ApplicationContextProvider.getContext(), SettingsActivity.class);
                settings.putExtra("file", selectedFile);
                settingsResultLauncher.launch(settings);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void afterPageTurn() {
        // Do nothing
    }

    private void createSpeechRecogniser() {
        if (SpeechRecognizer.isRecognitionAvailable(ApplicationContextProvider.getContext())) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this, ComponentName.unflattenFromString("com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService"));
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {
                    Log.d(TAG, "Ready for speech");
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech");
                }

                @Override
                public void onRmsChanged(float v) {
                }

                @Override
                public void onBufferReceived(byte[] bytes) {
                    Log.d(TAG, "Buffer received");
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "End of speech");
                }

                @Override
                public void onError(int i) {
                    Log.d(TAG, "Error: " + i);

                    if (i == SpeechRecognizer.ERROR_NO_MATCH) {
                        Log.d(TAG, "Restarting recogniser");
                        recognizer.cancel();
                        recognizer.startListening(createSpeechIntent());
                    }
                }

                @Override
                public void onResults(Bundle bundle) {
                    Log.d(TAG, "On results");
                    handleCommand(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                    recognizer.cancel();
                    recognizer.startListening(createSpeechIntent());
                }

                @Override
                public void onPartialResults(Bundle bundle) {
                    Log.d(TAG, "Partial results");
                    handleCommand(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                }

                @Override
                public void onEvent(int i, Bundle bundle) {
                    Log.d(TAG, "Event");
                }
            });
        } else {
            Log.d(TAG, "No speech recogniser available");
        }
    }

    private void handleCommand(List<String> matches) {
        if (matches != null && !matches.isEmpty()) {
            String[] words = matches.get(0).split(" ");
            Log.d(TAG, words.length + ":" + Arrays.toString(words));
            if (Arrays.stream(words).anyMatch("over"::equals)) {
                checkAndTurnPage(1);
            } else if (Arrays.stream(words).anyMatch("return"::equals)) {
                checkAndTurnPage(-1);
            }
        }
    }

    private Intent createSpeechIntent() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB");
        return i;
    }

    private Point getScreenSize() {
        // Find screen width
        WindowManager wm = (WindowManager) ApplicationContextProvider.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private void checkAndTurnPage(int increment) {
        long now = new Date().getTime();
        if (now - lastTurn >= MIN_TURN_INTERVAL) {
            lastTurn = now;
        }
    }
}