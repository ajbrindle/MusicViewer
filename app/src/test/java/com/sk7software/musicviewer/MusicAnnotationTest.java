package com.sk7software.musicviewer;

import static org.junit.Assert.assertEquals;

import android.util.Log;

import com.sk7software.musicviewer.model.MusicAnnotation;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class MusicAnnotationTest {

    @Test
    public void testFromJson() throws JSONException {
        PowerMockito.mockStatic(Log.class);
        JSONArray jsonArray = new JSONArray(JSON);
        for (int i=0; i< jsonArray.length(); i++) {
            MusicAnnotation annotation = MusicAnnotation.fromJson(jsonArray.getJSONObject(i));
            checkAnnotation(annotation);
        }
    }

    private void checkAnnotation(MusicAnnotation annotation) {
        if (annotation.getId() == 1) {
            checkFreehand(annotation);
        } else if (annotation.getId() == 2) {
            checkText(annotation);
        } else {
            checkFingering(annotation);
        }
    }

    private void checkFreehand(MusicAnnotation annotation) {
        assertEquals(1, annotation.getId());
        assertEquals(MusicAnnotation.FREEHAND, annotation.getType());
        assertEquals(0, annotation.getPage());
        assertEquals(13, annotation.getLineWidth());
        assertEquals(-16776961, annotation.getColour());
        assertEquals(177, annotation.getTransparency());
        assertEquals(0, annotation.getHand());
        assertEquals(0, annotation.getTextSize());
        assertEquals(3, annotation.getPoints().size());
        assertEquals(0.514215, annotation.getPoints().get(0).getPctX(), 0.001);
        assertEquals(0.241259, annotation.getPoints().get(0).getPctY(), 0.001);
        assertEquals(0.385043, annotation.getPoints().get(1).getPctX(), 0.001);
        assertEquals(0.252185, annotation.getPoints().get(1).getPctY(), 0.001);
        assertEquals(0.384425, annotation.getPoints().get(2).getPctX(), 0.001);
        assertEquals(0.257982, annotation.getPoints().get(2).getPctY(), 0.001);
    }

    private void checkText(MusicAnnotation annotation) {
        assertEquals(MusicAnnotation.TEXT, annotation.getType());
        assertEquals(0, annotation.getPage());
        assertEquals(-16776961, annotation.getColour());
        assertEquals(177, annotation.getTransparency());
        assertEquals(35, annotation.getTextSize());
        assertEquals("Hello world", annotation.getText());
        assertEquals(1, annotation.getPoints().size());
        assertEquals(0.580346, annotation.getPoints().get(0).getPctX(), 0.001);
        assertEquals(0.371504, annotation.getPoints().get(0).getPctY(), 0.001);
    }

    private void checkFingering(MusicAnnotation annotation) {
        assertEquals(3, annotation.getId());
        assertEquals(MusicAnnotation.FINGERING, annotation.getType());
        assertEquals(0, annotation.getPage());
        assertEquals(-16776961, annotation.getColour());
        assertEquals(177, annotation.getTransparency());
        assertEquals(1, annotation.getHand());
        assertEquals(35, annotation.getTextSize());
        assertEquals("1,3,5", annotation.getText());
        assertEquals(1, annotation.getPoints().size());
        assertEquals(0.282447, annotation.getPoints().get(0).getPctX(), 0.001);
        assertEquals(0.828671, annotation.getPoints().get(0).getPctY(), 0.001);
    }

    private static final String JSON = "[\n" +
            "  {\n" +
            "    \"id\": \"1\",\n" +
            "    \"seqNo\": \"0\",\n" +
            "    \"page\": \"0\",\n" +
            "    \"type\": \"0\",\n" +
            "    \"lineWidth\": \"13\",\n" +
            "    \"colour\": \"-16776961\",\n" +
            "    \"transparency\": \"177\",\n" +
            "    \"handId\": \"0\",\n" +
            "    \"textSize\": \"0\",\n" +
            "    \"text\": null,\n" +
            "    \"x\": [\n" +
            "      \"0.514215\",\n" +
            "      \"0.385043\",\n" +
            "      \"0.384425\"\n" +
            "    ],\n" +
            "    \"y\": [\n" +
            "      \"0.241259\",\n" +
            "      \"0.252185\",\n" +
            "      \"0.257982\"\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"id\": \"2\",\n" +
            "    \"seqNo\": \"0\",\n" +
            "    \"page\": \"0\",\n" +
            "    \"type\": \"1\",\n" +
            "    \"lineWidth\": \"0\",\n" +
            "    \"colour\": \"-16776961\",\n" +
            "    \"transparency\": \"177\",\n" +
            "    \"handId\": \"0\",\n" +
            "    \"textSize\": \"35\",\n" +
            "    \"text\": \"Hello world\",\n" +
            "    \"x\": [\n" +
            "      \"0.580346\"\n" +
            "    ],\n" +
            "    \"y\": [\n" +
            "      \"0.371504\"\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"id\": \"3\",\n" +
            "    \"seqNo\": \"0\",\n" +
            "    \"page\": \"0\",\n" +
            "    \"type\": \"2\",\n" +
            "    \"lineWidth\": \"0\",\n" +
            "    \"colour\": \"-16776961\",\n" +
            "    \"transparency\": \"177\",\n" +
            "    \"handId\": \"1\",\n" +
            "    \"textSize\": \"35\",\n" +
            "    \"text\": \"1,3,5\",\n" +
            "    \"x\": [\n" +
            "      \"0.282447\"\n" +
            "    ],\n" +
            "    \"y\": [\n" +
            "      \"0.828671\"\n" +
            "    ]\n" +
            "  }\n" +
            "]";
}
