package com.sk7software.musicviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.network.NetworkRequest;

import java.util.List;

public class SettingsActivity extends AppCompatActivity implements ITurnablePage {

    private MusicFile currentFile;
    private Button apply;
    private ImageView imgPreview;
    private PdfHelper pdfHelper;

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
                overlayBanners();
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
                overlayBanners();
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
                overlayBanners();
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
        TextView label1 = (TextView) findViewById(R.id.txtLabel1);
        label1.setText("Scroll Delay: " + currentFile.getDelay() + "ms");
        TextView label2 = (TextView) findViewById(R.id.txtLabel2);
        label2.setText("End Delay: " + currentFile.getEndDelay() + "ms");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        imgPreview = (ImageView) findViewById(R.id.imgPreview);
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
        float scale = 0.95F * (float) remaining / (float) size.y;
        int wid = (int) (size.x * scale);
        int ht = (int) (size.y * scale);
        Log.d(TAG, "Height: " + size.y + "; Remain: " + remaining + "; Scale: " + scale + "; ImgHt: " + ht);
        imgPreview.getLayoutParams().width = wid;
        imgPreview.getLayoutParams().height = ht;
        imgPreview.setLeft((size.x - wid) / 2);
        imgPreview.requestLayout();

        PdfHelper.getInstance().initialise(currentFile, new Point(wid, ht), this);
        pdfHelper.loadPDF(false);
    }

    private void overlayBanners() {
        Bitmap baseImage = Bitmap.createBitmap(pdfHelper.getPdfBitmap());

        Canvas canvas = new Canvas(baseImage);

        Paint p = new Paint();
        p.setColor(Color.BLUE);
        p.setAlpha(50);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setStrokeWidth(1);

        Rect topRect = new Rect(0, 0,
                baseImage.getWidth(), baseImage.getHeight() * currentFile.getTopPct() / 100);
        canvas.drawRect(topRect, p);

        Rect bottomRect = new Rect(0, baseImage.getHeight() - (baseImage.getHeight() * currentFile.getBottomPct() / 100),
                baseImage.getWidth(), baseImage.getHeight());
        canvas.drawRect(bottomRect, p);

        if (pdfHelper.isLastPage()) {
            p.setColor(Color.RED);
            Rect lastPage = new Rect(0, baseImage.getHeight() * currentFile.getLastPageStop() / 100,
                    baseImage.getWidth(), 3 + (baseImage.getHeight() * currentFile.getLastPageStop() / 100));
            canvas.drawRect(lastPage, p);
        }

        imgPreview.setImageBitmap(baseImage);
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
    public void afterPageTurn() {
        overlayBanners();
    }

    @Override
    public void afterLoad() {
        overlayBanners();
    }

    @Override
    public boolean isAnnotating() { return false; }

    @Override
    public void handleAnnotation(MotionEvent motionEvent) {}
}