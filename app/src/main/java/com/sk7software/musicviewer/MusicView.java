package com.sk7software.musicviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.sk7software.musicviewer.model.MusicAnnotation;
import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.model.Preferences;

import java.util.ArrayList;
import java.util.List;

public class MusicView extends View {

    private static final String TAG = MusicView.class.getSimpleName();

    private MusicFile musicFile;
    private MusicAnnotation currentAnnotation;
    private int pageNo;
    private int selectedAnnotationId;
    public MusicView(Context context) {
        super(context);
    }

    public MusicView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MusicView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MusicView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setMusicFile(MusicFile musicFile) {
        this.musicFile = musicFile;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap pdfBitmap = PdfHelper.getInstance().getPdfBitmap();
        if (pdfBitmap == null || musicFile == null) {
            return;
        }
        canvas.drawBitmap(pdfBitmap, 0, 0, new Paint());

        if (currentAnnotation != null) {
            drawAnnotation(canvas, currentAnnotation);
        }

        if (musicFile.getAnnotations() == null) {
            return;
        }

        for (MusicAnnotation annotation : musicFile.getAnnotations()) {
            if (annotation.getPage() != pageNo || annotation.getPoints() == null) {
                continue;
            }
            drawAnnotation(canvas, annotation);
        }
    }

    private void drawAnnotation(Canvas canvas, MusicAnnotation annotation) {
        boolean first = true;
        Point lastPoint = new Point();

        Paint p = new Paint();
        p.setColor(annotation.getColour());
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(annotation.getLineWidth());
        p.setAlpha(annotation.getTransparency());

        for (Point pt : annotation.getPoints()) {
            if (!first) {
                canvas.drawLine(lastPoint.x, lastPoint.y, pt.x, pt.y, p);
            }
            lastPoint.x = pt.x;
            lastPoint.y = pt.y;
            first = false;
        }

        if (annotation.getId() == selectedAnnotationId) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.YELLOW);
            p.setAlpha(100);
            canvas.drawRect(annotation.getBoundingRect(), p);
        }
    }
    public void addAnnotation(int type, int pageNo) {
        MusicAnnotation annotation = new MusicAnnotation();
        annotation.setType(type);
        annotation.setPage(pageNo);
        annotation.setColour(Preferences.getInstance().getIntPreference(Preferences.LINE_COLOUR, Color.BLACK));
        annotation.setLineWidth(Preferences.getInstance().getIntPreference(Preferences.LINE_WIDTH, 2));
        annotation.setTransparency(Preferences.getInstance().getIntPreference(Preferences.LINE_TRANSPARENCY, 255));
        currentAnnotation = annotation;
    }

    public void endAnnotation() {
        currentAnnotation.calcBoundingRect();
        musicFile.addAnnotation(currentAnnotation);
    }

    public void resetAnnotationPoints() {
        if (currentAnnotation == null || currentAnnotation.getPoints() == null) {
            return;
        }
        currentAnnotation.getPoints().clear();
    }

    public void addAnnotationPoint(Point point) {
        if (currentAnnotation.getPoints() == null) {
            currentAnnotation.setPoints(new ArrayList<>());

        }
        currentAnnotation.getPoints().add(point);
    }

    public void setPaintColour(int colour) {
        if (currentAnnotation != null) {
            currentAnnotation.setColour(colour);
        }
    }

    public void setPaintWidth(int width) {
        if (currentAnnotation != null) {
            currentAnnotation.setLineWidth(width);
        }
    }

    public void setPaintTransparency(int transparency) {
        if (currentAnnotation != null) {
            currentAnnotation.setTransparency(transparency);
        }
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public void setSelectedAnnotationId(int id) {
        this.selectedAnnotationId = id;
        currentAnnotation = musicFile.getAnnotations().stream().filter(a -> a.getId() == id).findFirst().orElse(null);
    }

    public int getSelectedAnnotationId() {
        return this.selectedAnnotationId;
    }

    public void moveAnnotation(int dx, int dy) {
        if (currentAnnotation != null) {
            currentAnnotation.shiftPoints(dx, dy);
        }
    }
}
