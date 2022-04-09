package com.sk7software.musicviewer.model;

import java.io.Serializable;

public class MusicFile implements Serializable {
    String name;
    int delay;
    int endDelay;
    int topPct;
    int bottomPct;
    int lastPageStop;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getEndDelay() {
        return endDelay;
    }

    public void setEndDelay(int endDelay) {
        this.endDelay = endDelay;
    }

    public int getTopPct() {
        return topPct;
    }

    public void setTopPct(int topPct) {
        this.topPct = topPct;
    }

    public int getBottomPct() {
        return bottomPct;
    }

    public void setBottomPct(int bottomPct) {
        this.bottomPct = bottomPct;
    }

    public int getLastPageStop() { return lastPageStop; }

    public void setLastPageStop(int lastPageStop) { this.lastPageStop = lastPageStop; }
}
