package com.sk7software.musicviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.network.NetworkRequest;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private MusicFile currentFile;
    private Button apply;

    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        currentFile = (MusicFile)getIntent().getSerializableExtra("file");

        SeekBar seekDelay = (SeekBar) findViewById(R.id.scrollDelay);
        seekDelay.setProgress(currentFile.getDelay());
        seekDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setDelay(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar endDelay = (SeekBar) findViewById(R.id.endDelay);
        endDelay.setProgress(currentFile.getEndDelay());
        endDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setEndDelay(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar topPct = (SeekBar) findViewById(R.id.topPct);
        topPct.setProgress(currentFile.getTopPct());
        topPct.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setTopPct(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar bottomPct = (SeekBar) findViewById(R.id.bottomPct);
        bottomPct.setProgress(currentFile.getBottomPct());
        bottomPct.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentFile.setBottomPct(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        apply = (Button)findViewById(R.id.btnApply);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.putExtra("file", currentFile);
                setResult(Activity.RESULT_OK, i);
                Log.d(TAG, "Exit settings: " + currentFile.getDelay());
                NetworkRequest.updateFile(ApplicationContextProvider.getContext(), currentFile, new NetworkRequest.NetworkCallback() {
                    @Override
                    public void onRequestCompleted(List<MusicFile> callbackData) {
                        Log.d(TAG, "File updated successfully");
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error: " + e);
                    }
                });
                finish();
            }
        });

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        ImageView imgPreview = (ImageView) findViewById(R.id.imgPreview);
        WindowManager wm = (WindowManager) ApplicationContextProvider.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);


        // How much of the screen is blank
        int[] loc = new int[2];
        apply.getLocationOnScreen(loc);
        int remaining = size.y - loc[1] - apply.getMeasuredHeight();
        int resourceId = ApplicationContextProvider.getContext().getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
             remaining -= ApplicationContextProvider.getContext().getResources().getDimensionPixelSize(resourceId);
        }
        float scale = 0.95F * (float)remaining/(float)size.y;
        int wid = (int)(size.x * scale);
        int ht = (int)(size.y * scale);
        Log.d(TAG, "Height: " + size.y + "; Remain: " + remaining + "; Scale: " + scale + "; ImgHt: " + ht);
        imgPreview.getLayoutParams().width = wid;
        imgPreview.getLayoutParams().height = ht;
        imgPreview.setLeft((size.x - wid)/2);
        imgPreview.requestLayout();

        PdfHelper pdfHelper = new PdfHelper(imgPreview, currentFile, new Point(wid, ht));
        pdfHelper.showPDF();
//        Bitmap mBitmap = Bitmap.createBitmap(wid, ht, Bitmap.Config.ARGB_4444);
//        int[] pixels = new int[wid*ht];
//        for (int i=0; i<pixels.length; i++) {
//            pixels[i] = 255;
//        }
//        mBitmap.setPixels(pixels, 0, wid, 0, 0, wid, ht);
//        imgPreview.setImageBitmap(mBitmap);
//
//        imgPreview.setBackgroundColor(Color.RED);
    }

    @Override
    protected void onStop() {
        super.onStop();
   }
}