package com.sk7software.musicviewer;

public interface IUpdateable {
    public void afterLoad();
    public void update(boolean clearFirst);
}
