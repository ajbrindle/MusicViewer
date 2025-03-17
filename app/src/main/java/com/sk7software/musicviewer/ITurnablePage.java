package com.sk7software.musicviewer;

import android.view.MotionEvent;

public interface ITurnablePage {
    public void afterPageTurn();
    public void afterLoad();
    public boolean isAnnotating();
    public void handleAnnotation(MotionEvent motionEvent);
}
