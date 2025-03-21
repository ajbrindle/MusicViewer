package com.sk7software.musicviewer;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.sk7software.musicviewer.model.MusicAnnotation;
import com.sk7software.musicviewer.model.Preferences;

import java.util.ArrayList;
import java.util.List;

public class FingersDialog extends Dialog implements
        android.view.View.OnClickListener {

    private IAnnotatable parentActivity;
    private Button btnOK;
    private Button btnCancel;
    private List<Button> btnFingers;
    private RadioGroup radioGroup;
    private List<Integer> fingers;

    public FingersDialog(IAnnotatable parentActivity) {
        super((Activity) parentActivity);
        this.parentActivity = parentActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_fingers);
        btnOK = (Button) findViewById(R.id.btnOK);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnOK.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.check(R.id.radioRight);
        btnFingers = new ArrayList<>();
        btnFingers.add((Button) findViewById(R.id.btn1));
        btnFingers.add((Button) findViewById(R.id.btn2));
        btnFingers.add((Button) findViewById(R.id.btn3));
        btnFingers.add((Button) findViewById(R.id.btn4));
        btnFingers.add((Button) findViewById(R.id.btn5));
        fingers = new ArrayList<>();
        setButtonListeners();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnOK:
                if (!fingers.isEmpty()) {
                    MusicAnnotation annotation = new MusicAnnotation();
                    annotation.setType(MusicAnnotation.FINGERING);
                    annotation.setHand(radioGroup.getCheckedRadioButtonId() == R.id.radioRight ? MusicAnnotation.RIGHT_HAND : MusicAnnotation.LEFT_HAND);
                    annotation.setText(fingers.stream().sorted().map(Object::toString).collect(java.util.stream.Collectors.joining(",")));
                    annotation.setTextSize(Preferences.getInstance().getIntPreference(Preferences.TEXT_SIZE));
                    annotation.setColour(Preferences.getInstance().getIntPreference(Preferences.LINE_COLOUR));
                    annotation.setTransparency(Preferences.getInstance().getIntPreference(Preferences.LINE_TRANSPARENCY));
                    parentActivity.storeAnnotation(annotation);
                }
                break;
            case R.id.btnCancel:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }

    private void setButtonListeners() {
        for (int i=0; i<btnFingers.size(); i++) {
            final int finger = i+1;
            Button btn = btnFingers.get(i);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleButton(finger);
                }
            });
        }
    }

    private void toggleButton(Integer finger) {
        if (fingers.contains(finger)) {
            fingers.remove(finger);
            btnFingers.get(finger-1).setTextColor(Color.BLACK);
            btnFingers.get(finger-1).setBackgroundResource(R.drawable.button_border);
        } else {
            fingers.add(finger);
            // set button background to the button_border drawable
            btnFingers.get(finger-1).setBackgroundColor(Color.WHITE);
            btnFingers.get(finger-1).setTextColor(Color.WHITE);
        }
    }
}
