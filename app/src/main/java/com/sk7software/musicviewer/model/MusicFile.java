package com.sk7software.musicviewer.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MusicFile implements Serializable {
    int id;
    String name;
    String title;
    String artist;
    int delay;
    int endDelay;
    int topPct;
    int bottomPct;
    int lastPageStop;
    String displayName;
    boolean annotationsFetched;
    List<MusicAnnotation> annotations;

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

    public int addAnnotation(MusicAnnotation annotation) {
        if (annotations == null) {
            annotations = new ArrayList<MusicAnnotation>();
        }
        int id = annotations.stream().mapToInt(MusicAnnotation::getId).max().orElse(0) + 1;
        annotation.setId(id);
        annotations.add(annotation);
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public boolean isAnnotationsFetched() {
        return annotationsFetched;
    }

    public void setAnnotationsFetched(boolean annotationsFetched) {
        this.annotationsFetched = annotationsFetched;
    }

    public List<MusicAnnotation> getAnnotations() {
        return annotations;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDisplayName() {
        if (name == null) {
            return null;
        }

        int extPos = name.lastIndexOf(".");
        if (extPos > 0) {
            return name.substring(0, extPos);
        }

        return name;
    }

    public MusicAnnotation findAnnotation(float x, float y, int pageNo) {
        for (MusicAnnotation annotation : annotations) {
            if (annotation.getPage() == pageNo && annotation.contains(x, y)) {
                return annotation;
            }
        }
        return null;
    }

    public void removeAnnotation(int id) {
        annotations.removeIf(a -> a.getId() == id);
    }
}
