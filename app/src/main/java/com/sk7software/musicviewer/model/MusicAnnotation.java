package com.sk7software.musicviewer.model;

import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
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
    private List<DisplayPoint> points;
    private int lineWidth;
    private int colour;
    private int transparency;
    private String text;
    private int textSize;
    private int hand;
    private transient Rect boundingRect;

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

    public List<DisplayPoint> getPoints() {
        return points;
    }

    public void setPoints(List<DisplayPoint> points) {
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
                for (DisplayPoint p : points) {
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
                boundingRect.set(x0-5, y0-5, x1+5, y1+5);
            } else if (type == TEXT) {
                Paint p = new Paint();
                Rect textBounds = new Rect();
                p.setTextSize(textSize);
                p.getTextBounds(text, 0, text.length(), textBounds);
                boundingRect.set(points.get(0).x - (textSize*2), points.get(0).y - textBounds.height(),
                        points.get(0).x + textBounds.width() + (textSize*2), points.get(0).y + textBounds.height());
            } else if (type == FINGERING) {
                String[] fingers = text.split(",");
                Paint p = new Paint();
                Rect textBounds = new Rect();
                p.setTextSize(textSize);
                p.getTextBounds("8", 0, 1, textBounds);
                boundingRect.set(points.get(0).x - textSize, points.get(0).y - textBounds.height(),
                        points.get(0).x + textBounds.width() + textSize, points.get(0).y + (textBounds.height() * fingers.length));
            } else {
                Log.e(TAG, "Unknown annotation type: " + type);
            }
        }
    }

    public boolean contains(float x, float y) {
        if (boundingRect == null) {
            calcBoundingRect();
        }

        return boundingRect.contains((int)x, (int)y);
    }

    public void shiftPoints(int dx, int dy, int width, int height) {
        for (DisplayPoint p : points) {
            p.x += dx;
            p.y += dy;
            p.setPercentages(width, height);
        }
        calcBoundingRect();
    }

    public void convertPoints(int width, int height) {
        for (DisplayPoint p : points) {
            p.convertFromPercentages(width, height);
        }
        calcBoundingRect();
    }

    public void addPoint(Point p, int width, int height) {
        if (points == null) {
            points = new ArrayList<DisplayPoint>();
        }
        DisplayPoint dp = new DisplayPoint(p.x, p.y);
        points.add(dp);
        dp.setPercentages(width, height);
    }

    private void setPercentagePoints(float[] x, float[] y) {
        if (points == null) {
            points = new ArrayList<DisplayPoint>();
        }
        for (int i = 0; i < x.length; i++) {
            DisplayPoint dp = new DisplayPoint();
            dp.setPctX(x[i]);
            dp.setPctY(y[i]);
            points.add(dp);
        }
    }

    public static MusicAnnotation fromJson(JSONObject json) {
        MusicAnnotation annotation = new MusicAnnotation();
        try {
            // set fields surrounding each one with !json.isNull("fieldName")
            annotation.setId(json.getInt("id"));
            annotation.setType(json.getInt("type"));
            annotation.setPage(json.getInt("page"));
            annotation.setLineWidth(json.getInt("lineWidth"));
            annotation.setColour(json.getInt("colour"));
            annotation.setTransparency(json.getInt("transparency"));
            if (!json.isNull("text")) annotation.setText(json.getString("text"));
            if (!json.isNull("textSize")) annotation.setTextSize(json.getInt("textSize"));
            if (!json.isNull("handId")) annotation.setHand(json.getInt("handId"));
            float[] x = new float[json.getJSONArray("x").length()];
            float[] y = new float[json.getJSONArray("y").length()];
            for (int i = 0; i < json.getJSONArray("x").length(); i++) {
                x[i] = (float)json.getJSONArray("x").getDouble(i);
                y[i] = (float)json.getJSONArray("y").getDouble(i);
            }
            annotation.setPercentagePoints(x, y);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e);
        }
        return annotation;
    }
}
