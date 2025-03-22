package com.sk7software.musicviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.network.NetworkRequest;

import java.util.List;

public class SettingsActivity extends AppCompatActivity implements IUpdateable {

    private MusicFile currentFile;
    private Button apply;
    private MusicView musicView;

    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        currentFile = (MusicFile) getIntent().getSerializableExtra("file");
        setTitle("Settings - " + currentFile.getDisplayName());

        SeekBar seekDelay = (SeekBar) findViewById(R.id.scrollDelay);
        seekDelay.setProgress(currentFile.getDelay());
        seekDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setDelay(i);
                TextView label = (TextView) findViewById(R.id.txtLabel1);
                label.setText("Scroll Delay: " + i + "ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        SeekBar endDelay = (SeekBar) findViewById(R.id.endDelay);
        endDelay.setProgress(currentFile.getEndDelay());
        endDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setEndDelay(i);
                TextView label = (TextView) findViewById(R.id.txtLabel2);
                label.setText("End Delay: " + i + "ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        SeekBar topPct = (SeekBar) findViewById(R.id.topPct);
        topPct.setProgress(currentFile.getTopPct());
        topPct.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setTopPct(i);
                musicView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        SeekBar bottomPct = (SeekBar) findViewById(R.id.bottomPct);
        bottomPct.setProgress(currentFile.getBottomPct());
        bottomPct.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setBottomPct(i);
                musicView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        SeekBar lastStop = (SeekBar) findViewById(R.id.endStop);
        lastStop.setProgress(currentFile.getLastPageStop());
        lastStop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setLastPageStop(i);
                musicView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        apply = (Button) findViewById(R.id.btnApply);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.putExtra("file", currentFile);
                setResult(Activity.RESULT_OK, i);
                NetworkRequest.updateFile(ApplicationContextProvider.getContext(), currentFile, new NetworkRequest.NetworkCallback() {
                    @Override
                    public void onRequestCompleted(Object callbackData) {
                        Log.d(TAG, "Music document updated successfully");
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error: " + e);
                    }
                });
                finish();
            }
        });
        TextView label1 = (TextView) findViewById(R.id.txtLabel1);
        label1.setText("Scroll Delay: " + currentFile.getDelay() + "ms");
        TextView label2 = (TextView) findViewById(R.id.txtLabel2);
        label2.setText("End Delay: " + currentFile.getEndDelay() + "ms");

        musicView = (MusicView) findViewById(R.id.imgPreview);
        musicView.setMusicFile(currentFile);
        musicView.setShowOverlays(true);

        final View rootView = this.getWindow().getDecorView().getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "Settings layout listener");
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Point dimensions = new Point(musicView.getWidth(), musicView.getHeight());
                musicView.setMinimumHeight(dimensions.y);
                PdfHelper.getInstance().initialise(currentFile, dimensions, SettingsActivity.this);
                PdfHelper.getInstance().loadPDF(false);
            }
        });

        musicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return musicView.handleTouchEvent(motionEvent);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void afterLoad() {
        if (PdfHelper.getInstance().getPdfBitmap() == null) {
            PdfHelper.getInstance().getPdfBitmap(0);
            musicView.setPageNo(0);
        }
        musicView.invalidate();
    }

    @Override
    public void update(boolean clearFirst) {}
}