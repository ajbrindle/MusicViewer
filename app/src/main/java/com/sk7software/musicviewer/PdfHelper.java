package com.sk7software.musicviewer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.sk7software.musicviewer.list.MusicListActivity;
import com.sk7software.musicviewer.model.DisplayPoint;
import com.sk7software.musicviewer.model.MusicAnnotation;
import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.network.FileDownloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

public class PdfHelper {

    private MusicFile filename;
    private Point dimensions;
    private PdfRenderer renderer;
    private Bitmap pdfBitmap;
    private int imgTop;
    private int pageNo;
    private boolean start;
    private boolean stopScroll;
    private IUpdateable activity;
    private Bitmap bitmap1;
    private Bitmap bitmap2;
    private Bitmap combined;
    private boolean scrolling;

    private static final String REMOTE_FILE_URL = "http://www.sk7software.co.uk/sheetmusic/pdfs/";
    private static String TAG = PdfHelper.class.getSimpleName();
    private static PdfHelper instance;

    private PdfHelper() {}

    public static PdfHelper getInstance() {
        if (instance == null) {
            instance = new PdfHelper();
        }
        return instance;
    }

    public void initialise(MusicFile filename, Point dimensions, IUpdateable activity) {
        this.filename = filename;
        this.dimensions = dimensions;
        this.activity = activity;
        this.start = true;
        this.bitmap1 = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);
        this.bitmap2 = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);
        this.pdfBitmap = null;
        this.combined = null;
        this.scrolling = false;
    }

    public Bitmap getPdfBitmap() {
        return pdfBitmap;
    }

    public void loadPDF(boolean inThread) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ParcelFileDescriptor pfd = getPFD(filename.getName());
                if (pfd != null) {
                    fetchPDF(pfd);
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

    private void fetchPDF(ParcelFileDescriptor file) {
        try {
            renderer = new PdfRenderer(file);
            pageNo = 0;
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

    public Bitmap getPdfBitmap(int pageNo) {
        final int pageCount = renderer.getPageCount();
        imgTop = 0;
        if (pageCount > pageNo) {
            Log.d(TAG, "Showing page: " + pageNo);
            PdfRenderer.Page page = renderer.openPage(pageNo);
            // assume page is portrait
            float scaleY = (float)dimensions.y / (float)page.getHeight();
            int bitmapWidthX = (int) ((float)page.getWidth() * scaleY);
            pdfBitmap = Bitmap.createBitmap(bitmapWidthX, dimensions.y, Bitmap.Config.ARGB_4444);
            page.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            return pdfBitmap;
        }
        return null;
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
            boolean clear = true;
            combined = Bitmap.createBitmap(dimensions.x, dimensions.y, Bitmap.Config.ARGB_4444);

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
                        addAnnotations(bitmap1, pageNo);
                    }
                    if (pageNo < renderer.getPageCount() - 1) {
                        PdfRenderer.Page page2 = renderer.openPage(pageNo + 1);
                        page2.render(bitmap2, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page2.close();
                        addAnnotations(bitmap2, pageNo+1);
                    } else {
                        stopScroll = true;
                    }
                }

                bitmap1.getPixels(pixels, 0, dimensions.x, 0, -imgTop, dimensions.x, dimensions.y + imgTop);
                bitmap2.getPixels(pixels, ((dimensions.y + imgTop) * dimensions.x), dimensions.x, 0, 0, dimensions.x, -imgTop);
                combined.setPixels(pixels, 0, dimensions.x, 0, 0, dimensions.x, dimensions.y);
                currentPage = pageNo;
                activity.update(clear);
                clear = false;

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
                fetchPDF(ParcelFileDescriptor.open(new File(MusicListActivity.MUSIC_DIR + "tmp/tmp.pdf"), ParcelFileDescriptor.MODE_READ_ONLY));
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

    public Bitmap getCombinedBitmap() {
        return combined;
    }

    public boolean isLastPage(int num) {
        return num == renderer.getPageCount() - 1;
    }

    private boolean stopReached() {
        float pctShowing = 1 - ((float)(dimensions.y + imgTop)/(float)dimensions.y);
        return pctShowing * 100 >= filename.getLastPageStop();
    }

    private void addAnnotations(Bitmap bitmap, int pageNo) {
        if (filename.getAnnotations() == null || filename.getAnnotations().isEmpty()) {
            return;
        }

        Canvas canvas = new Canvas(bitmap);

        for (MusicAnnotation annotation : filename.getAnnotations()) {
            if (annotation.getPage() != pageNo) {
                continue;
            }

            annotation.convertPoints(bitmap.getWidth(), bitmap.getHeight());

            if (annotation.getType() == MusicAnnotation.FREEHAND) {
                drawFreehand(canvas, annotation);
            } else if (annotation.getType() == MusicAnnotation.TEXT) {
                drawText(canvas, annotation);
            } else if (annotation.getType() == MusicAnnotation.FINGERING) {
                drawFingering(canvas, annotation);
            }
        }
    }

    private void drawFreehand(Canvas canvas, MusicAnnotation annotation) {
        boolean first = true;
        Point lastPoint = new Point();

        Paint p = new Paint();
        p.setColor(annotation.getColour());
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(annotation.getLineWidth());
        p.setAlpha(annotation.getTransparency());

        for (DisplayPoint pt : annotation.getPoints()) {
            if (!first) {
                canvas.drawLine(lastPoint.x, lastPoint.y, pt.x, pt.y, p);
            }
            lastPoint.x = pt.x;
            lastPoint.y = pt.y;
            first = false;
        }
    }

    private void drawText(Canvas canvas, MusicAnnotation annotation) {
        Paint p = new Paint();
        p.setColor(annotation.getColour());
        p.setTextSize(annotation.getTextSize());
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setAlpha(annotation.getTransparency());
        canvas.drawText(annotation.getText(), annotation.getPoints().get(0).x, annotation.getPoints().get(0).y, p);
    }

    private void drawFingering(Canvas canvas, MusicAnnotation annotation) {
        Paint p = new Paint();
        p.setColor(annotation.getColour());
        p.setTextSize(annotation.getTextSize());
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setAlpha(annotation.getTransparency());
        int y = annotation.getPoints().get(0).y;
        for (String finger : annotation.getText().split(",")) {
            canvas.drawText(finger, annotation.getPoints().get(0).x, y, p);
            y += annotation.getTextSize();
        }
    }

    public void setScrolling(boolean scrolling) {
        this.scrolling = scrolling;
    }

    public boolean isScrolling() {
        return scrolling;
    }

    public Bitmap getDisplayBitmap() {
        return scrolling ? combined : pdfBitmap;
    }
}
