package com.sk7software.musicviewer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sk7software.musicviewer.list.MusicListActivity;
import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.network.FileDownloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

public class PdfHelper {

    private ImageView imageView;
    private MusicFile filename;
    private Point dimensions;
    private PdfRenderer renderer;
    private Bitmap pdfBitmap;
    private int imgTop;
    private int pageNo;
    private boolean start;
    private boolean stopScroll;
    private ITurnablePage activity;
    Bitmap bitmap1;
    Bitmap bitmap2;


    private static final String REMOTE_FILE_URL = "http://www.sk7software.co.uk/sheetmusic/pdfs/";
    private static String TAG = PdfHelper.class.getSimpleName();

    public PdfHelper(ImageView imageView, MusicFile filename, Point dimensions, ITurnablePage activity) {
        this.imageView = imageView;
        this.filename = filename;
        this.dimensions = dimensions;
        this.activity = activity;
        this.start = true;
        this.bitmap1 = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);
        this.bitmap2 = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);
    }

    public Bitmap getPdfBitmap() {
        return pdfBitmap;
    }

    public void showPDF(boolean inThread) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ParcelFileDescriptor pfd = getPFD(filename.getName());
                if (pfd != null) {
                    showPDF(pfd);
                    activity.afterLoad();
                }
            }
        });

        if (inThread) {
            t.start();
        } else {
            t.run();
        }
    }

    private void showPDF(ParcelFileDescriptor file) {
        try {
            renderer = new PdfRenderer(file);
            pageNo = 0;

            imageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getX() < dimensions.x / 4) {
                        if (pageNo > 0) pageNo--;
                    } else {
                        pageNo++;
                    }
                    if (showPage()) {
                        activity.afterPageTurn();
                    }
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

    private boolean showPage() {
        // let us just render all pages
        final int pageCount = renderer.getPageCount();
        imgTop = 0;
        imageView.setMinimumHeight(dimensions.y);
        imageView.setTop(imgTop);
        if (pageCount > pageNo) {
            Log.d(TAG, "Showing page: " + pageNo);
            pdfBitmap = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);
            PdfRenderer.Page page = renderer.openPage(pageNo);
            page.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            imageView.setImageBitmap(pdfBitmap);
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp.addRule(RelativeLayout.CENTER_IN_PARENT);
            imageView.setLayoutParams(rlp);
            return true;
        } else {
            // Reverse the increment
            pageNo--;
            return false;
        }
    }

    public void setStopScroll(boolean stopScroll) {
        this.stopScroll = stopScroll;
    }

    public void scroll() {
        // Start first page half way down
        if (start) {
            pageNo = -1;
            start = false;
            imgTop = -dimensions.y/2;
        }

        Runnable runnable = () -> {
            long lastScrollTime = 0;
            int currentPage = -99;
            Bitmap combined = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);

            while (!stopScroll)  {
                int delay = 0;

                if (adjustForPageBreak(dimensions.y)) {
                    delay = filename.getEndDelay();
                } else {
                    delay = filename.getDelay();
                }

                while (new Date().getTime() - lastScrollTime < delay) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {}
                }

                //Log.d(TAG, "DELAY: " + (new Date().getTime() - lastScrollTime));
                lastScrollTime = new Date().getTime();
                imgTop -= 20;

                if (imgTop <= -dimensions.y) {
                    imgTop = 0;
                    pageNo++;
                }

                int pixels[] = new int[dimensions.x * dimensions.y];

                if (pageNo != currentPage) {
                    if (pageNo >= 0) {
                        PdfRenderer.Page page = renderer.openPage(pageNo);
                        page.render(bitmap1, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page.close();
                    } else {
                        // Do nothing
                    }

                    if (pageNo < renderer.getPageCount() - 1) {
                        PdfRenderer.Page page2 = renderer.openPage(pageNo + 1);
                        page2.render(bitmap2, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page2.close();
                    } else {
                        stopScroll = true;
                    }
                }

                bitmap1.getPixels(pixels, 0, dimensions.x, 0, -imgTop, dimensions.x, dimensions.y + imgTop);
                bitmap2.getPixels(pixels, ((dimensions.y + imgTop) * dimensions.x), dimensions.x, 0, 0, dimensions.x, -imgTop);
                combined.setPixels(pixels, 0, dimensions.x, 0, 0, dimensions.x, dimensions.y);
                imageView.setImageBitmap(combined);
                currentPage = pageNo;

                // Check whether last page is in view
                if (pageNo == renderer.getPageCount() - 2 && stopReached()) {
                    stopScroll = true;
                }
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
                activity.afterLoad();
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

    public boolean isLastPage() {
        return pageNo == renderer.getPageCount() - 1;
    }

    private boolean stopReached() {
        float pctShowing = 1 - ((float)(dimensions.y + imgTop)/(float)dimensions.y);
        return pctShowing * 100 >= filename.getLastPageStop();
    }
}
