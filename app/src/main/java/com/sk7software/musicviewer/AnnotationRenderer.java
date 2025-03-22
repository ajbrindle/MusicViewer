package com.sk7software.musicviewer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import com.sk7software.musicviewer.model.DisplayPoint;
import com.sk7software.musicviewer.model.MusicAnnotation;

public class AnnotationRenderer {
    private static final Typeface CUSTOM_FONT = ResourcesCompat.getFont(ApplicationContextProvider.getContext(), R.font.itim);
    private static final Typeface CUSTOM_FONT_BOLD = Typeface.create(CUSTOM_FONT, Typeface.BOLD);

    public static void drawFreehandAnnotation(Canvas canvas, MusicAnnotation annotation) {
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

    public static void drawTextAnnotation(Canvas canvas, MusicAnnotation annotation) {
        Paint p = new Paint();
        p.setColor(annotation.getColour());
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setTextSize(annotation.getTextSize());
        p.setAntiAlias(true);
        p.setTypeface(CUSTOM_FONT_BOLD);
        p.setAlpha(annotation.getTransparency());
        canvas.drawText(annotation.getText(), annotation.getPoints().get(0).x, annotation.getPoints().get(0).y, p);
    }

    public static void drawFingersAnnotation(Canvas canvas, MusicAnnotation annotation) {
        Paint p = new Paint();
        p.setColor(annotation.getColour());
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setTextSize(annotation.getTextSize());
        p.setAntiAlias(true);
        p.setTypeface(CUSTOM_FONT_BOLD);
        p.setAlpha(annotation.getTransparency());
        String[] fingers = annotation.getText().split(",");
        int iStart = 0;
        int iEnd = fingers.length;
        int incr = 1;
        if (annotation.getHand() == MusicAnnotation.RIGHT_HAND) {
            iStart = fingers.length - 1;
            iEnd = -1;
            incr = -1;
        }
        int y = annotation.getPoints().get(0).y;
        for (int i=iStart; i!=iEnd; i+=incr) {
            canvas.drawText(fingers[i], annotation.getPoints().get(0).x, y, p);
            y += (int)p.getTextSize();
        }
    }

}
