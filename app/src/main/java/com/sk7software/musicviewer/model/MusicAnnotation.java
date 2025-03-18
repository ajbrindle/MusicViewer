package com.sk7software.musicviewer.model;

import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import java.io.Serializable;
import java.util.List;

public class MusicAnnotation implements Serializable {
    public static final int FREEHAND = 0;
    public static final int TEXT = 1;
    public static final int FINGERING = 2;
    public static final int LEFT_HAND = 0;
    public static final int RIGHT_HAND = 1;
    private static final String TAG = MusicAnnotation.class.getSimpleName();

    private int id;
    private int type;
    private int page;
    private List<Point> points;
    private Rect boundingRect;
    private int lineWidth;
    private int colour;
    private int transparency;
    private String text;
    private int textSize;
    private List<Integer> fingers;
    private int hand;

    public MusicAnnotation() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getColour() {
        return colour;
    }

    public void setColour(int colour) {
        this.colour = colour;
    }

    public int getTransparency() {
        return transparency;
    }

    public void setTransparency(int transparency) {
        this.transparency = transparency;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Integer> getFingers() {
        return fingers;
    }

    public void setFingers(List<Integer> fingers) {
        this.fingers = fingers;
    }

    public int getHand() {
        return hand;
    }

    public void setHand(int hand) {
        this.hand = hand;
    }

    public Rect getBoundingRect() {
        if (boundingRect == null) {
            calcBoundingRect();
        }
        return boundingRect;
    }

    public void setBoundingRect(Rect boundingRect) {
        this.boundingRect = boundingRect;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void calcBoundingRect() {
        boundingRect = new Rect();

        if (points != null) {
            int x0 = points.get(0).x;
            int y0 = points.get(0).y;
            int x1 = points.get(0).x;
            int y1 = points.get(0).y;

            if (type == FREEHAND) {
                for (Point p : points) {
                    if (p.x < x0) {
                        x0 = p.x;
                    }
                    if (p.x > x1) {
                        x1 = p.x;
                    }
                    if (p.y < y0) {
                        y0 = p.y;
                    }
                    if (p.y > y1) {
                        y1 = p.y;
                    }
                }
                boundingRect.set(x0, y0, x1, y1);
            } else if (type == TEXT) {
                Paint p = new Paint();
                Rect textBounds = new Rect();
                p.setTextSize(textSize);
                p.getTextBounds(text, 0, text.length(), textBounds);
                boundingRect.set(points.get(0).x - (textSize*2), points.get(0).y - textBounds.height(),
                        points.get(0).x + textBounds.width() + (textSize*2), points.get(0).y + textBounds.height());
            }
        }
    }

    public boolean contains(float x, float y) {
        if (boundingRect == null) {
            calcBoundingRect();
        }

        return boundingRect.contains((int)x, (int)y);
    }

    public void shiftPoints(int dx, int dy) {
        for (Point p : points) {
            p.x += dx;
            p.y += dy;
        }
        calcBoundingRect();
    }
}
