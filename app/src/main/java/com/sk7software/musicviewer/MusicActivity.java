package com.sk7software.musicviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.sk7software.musicviewer.list.MusicListActivity;
import com.sk7software.musicviewer.network.FileDownloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MusicActivity extends AppCompatActivity {

    private int pageNo;
    private ParcelFileDescriptor pfd;
    private PdfRenderer renderer;
    private SpeechRecognizer recognizer;
    private ImageView img;
    private long lastTurn = 0;

    private static final String TAG = MusicActivity.class.getSimpleName();
    private static final long MIN_TURN_INTERVAL = 5000;
    private static final String REMOTE_FILE_URL = "http://www.sk7software.co.uk/sheetmusic/pdfs/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        createSpeechRecogniser();

        String selectedFile = getIntent().getStringExtra("name");
        showPDF(selectedFile);
        recognizer.startListening(createSpeechIntent());
    }

    @Override
    protected void onStop() {
        super.onStop();
        recognizer.destroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void showPDF(String selectedFile) {
        pfd = getPFD(selectedFile);
        if (pfd == null) return;
        showPDF(pfd);
    }

    private void showPDF(ParcelFileDescriptor file) {
        try {
            renderer = new PdfRenderer(file);

            img = (ImageView) findViewById(R.id.imgView);
            pageNo = 0;

            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pageNo++;
                    showPage();
                }
            });

            showPage();
        } catch (IOException e) {
            Log.e(TAG, "I/O Error: " + e.getMessage());
        }

    }

    private ParcelFileDescriptor getPFD(String filename) {
        try {
            // See if this is a local file
            if (Files.exists(Paths.get(MusicListActivity.MUSIC_DIR + filename))) {
                return ParcelFileDescriptor.open(new File(MusicListActivity.MUSIC_DIR + filename), ParcelFileDescriptor.MODE_READ_ONLY);
            } else {
                // Retrieve from remote
                new DownloadFile().execute(REMOTE_FILE_URL + Uri.encode(filename), "tmp.pdf");
                return null;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to find file: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "I/O Error: " + e.getMessage());
        }
        return null;
    }

    private void showPage() {
        // let us just render all pages
        final int pageCount = renderer.getPageCount();
        Point screenSize = getScreenSize();
        Bitmap mBitmap = Bitmap.createBitmap(screenSize.x, screenSize.y, Bitmap.Config.ARGB_4444);
        if (pageCount > pageNo) {
            Log.d(TAG, "Showing page: " + pageNo);
            PdfRenderer.Page page = renderer.openPage(pageNo);

            // say we render for showing on the screen
            page.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            // do stuff with the bitmap
            img.setImageBitmap(mBitmap);

            // close the page
            page.close();
        }
    }

    private void createSpeechRecogniser() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
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
            pageNo += increment;
            showPage();
        }
    }

    private class DownloadFile extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            try {
                showPDF(ParcelFileDescriptor.open(new File(MusicListActivity.MUSIC_DIR + "tmp/tmp.pdf"), ParcelFileDescriptor.MODE_READ_ONLY));
            } catch (FileNotFoundException e) {
                Log.d(TAG, "Unable to open tmp file");
            }
        }

        @Override
        protected Void doInBackground(String... strings) {
            String fileUrl = strings[0];
            String fileName = strings[1];
            File folder = new File(MusicListActivity.MUSIC_DIR, "tmp");
            folder.mkdir();

            File pdfFile = new File(folder, fileName);

            try{
                pdfFile.createNewFile();
            }catch (IOException e){
                e.printStackTrace();
            }
            FileDownloader.downloadFile(fileUrl, pdfFile);
            return null;
        }
    }
}