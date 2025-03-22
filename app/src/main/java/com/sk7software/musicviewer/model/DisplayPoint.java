package com.sk7software.musicviewer.model;

import java.io.Serializable;

public class DisplayPoint implements Serializable {
    public int x;
    public int y;
    private float pctX;
    private float pctY;

    public DisplayPoint() {
        super();
    }

    public DisplayPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setPercentages(int width, int height) {
        pctX = (float)x / (float)width;
        pctY = (float)y / (float)height;
    }
    public void convertFromPercentages(int width, int height) {
        x = (int)(pctX * (float)width);
        y = (int)(pctY * (float)height);
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public float getPctX() {
        return pctX;
    }
    public void setPctX(float pctX) {
        this.pctX = pctX;
    }
    public float getPctY() {
        return pctY;
    }
    public void setPctY(float pctY) {
        this.pctY = pctY;
    }
}
