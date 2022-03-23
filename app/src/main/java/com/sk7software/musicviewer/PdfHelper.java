package com.sk7software.musicviewer;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.sk7software.musicviewer.list.MusicListActivity;
import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.network.FileDownloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PdfHelper {

    private ImageView imageView;
    private MusicFile filename;
    private Point dimensions;
    private PdfRenderer renderer;
    private int imgTop;
    private int pageNo;
    private boolean start;
    private boolean stopScroll;

    private static final String REMOTE_FILE_URL = "http://www.sk7software.co.uk/sheetmusic/pdfs/";
    private static String TAG = PdfHelper.class.getSimpleName();

    public PdfHelper(ImageView imageView, MusicFile filename, Point dimensions) {
        this.imageView = imageView;
        this.filename = filename;
        this.dimensions = dimensions;
        start = true;
    }

    public void showPDF() {
        ParcelFileDescriptor pfd = getPFD(filename.getName());
        if (pfd == null) return;
        showPDF(pfd);
    }

    private void showPDF(ParcelFileDescriptor file) {
        try {
            renderer = new PdfRenderer(file);
            pageNo = 0;

            imageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Log.d(TAG, "X: " + motionEvent.getX() + " Screen: " + dimensions.x);
                    if (motionEvent.getX() < dimensions.x / 4) {
                        if (pageNo > 0) pageNo--;
                    } else {
                        pageNo++;
                    }
                    showPage();
                    return false;
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
            } else if (Files.exists(Paths.get(MusicListActivity.MUSIC_DIR + "tmp/tmp.pdf"))) {
                return ParcelFileDescriptor.open(new File(MusicListActivity.MUSIC_DIR + "tmp/tmp.pdf"), ParcelFileDescriptor.MODE_READ_ONLY);
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
        imgTop = 0;
        imageView.setMinimumHeight(dimensions.y);
        imageView.setTop(imgTop);
        Bitmap mBitmap = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);
        if (pageCount > pageNo) {
            Log.d(TAG, "Showing page: " + pageNo);
            PdfRenderer.Page page = renderer.openPage(pageNo);
            page.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            imageView.setImageBitmap(mBitmap);
        } else {
            // Reverse the increment
            pageNo--;
        }

        if (start) {
            pageNo = -1;
            start = false;
            imgTop = -dimensions.y/2;
        }
    }

    public void setStopScroll(boolean stopScroll) {
        this.stopScroll = stopScroll;
    }

    public void scroll() {
        Runnable runnable = () -> {
            while (!stopScroll) {
                try {
                    if (adjustForPageBreak(dimensions.y)) {
                        Thread.sleep(filename.getEndDelay());
                    } else {
                        Thread.sleep(filename.getDelay());
                    }
                } catch (InterruptedException e) {}
                imgTop -= 20;

                if (imgTop <= -dimensions.y) {
                    imgTop = 0;
                    pageNo++;
                }

                Bitmap mBitmap = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);
                Bitmap combined = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);
                int pixels[] = new int[dimensions.x * dimensions.y];

                if (pageNo >= 0) {
                    PdfRenderer.Page page = renderer.openPage(pageNo);
                    page.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    mBitmap.getPixels(pixels, 0, dimensions.x, 0, -imgTop, dimensions.x, dimensions.y + imgTop);
                    page.close();
                } else {
                    // Do nothing
                }

                if (pageNo < renderer.getPageCount() - 1) {
                    PdfRenderer.Page page2 = renderer.openPage(pageNo + 1);
                    page2.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    mBitmap.getPixels(pixels, ((dimensions.y + imgTop) * dimensions.x), dimensions.x, 0, 0, dimensions.x, -imgTop);
                    page2.close();
                } else {
                    stopScroll = true;
                }

                combined.setPixels(pixels, 0, dimensions.x, 0, 0, dimensions.x, dimensions.y);
                imageView.setImageBitmap(combined);
            }
        };
        new Thread(runnable).start();
    }

    private boolean adjustForPageBreak(int screenHt) {
        return (-imgTop < (float)screenHt * (float)filename.getTopPct() / 100 ||
                -imgTop > (float)screenHt * ((float)(100-filename.getBottomPct())/100));
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
