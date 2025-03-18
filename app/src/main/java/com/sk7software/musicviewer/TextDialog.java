package com.sk7software.musicviewer;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.sk7software.musicviewer.model.MusicAnnotation;
import com.sk7software.musicviewer.model.Preferences;

public class TextDialog extends Dialog implements
        android.view.View.OnClickListener {

    private IAnnotatable parentActivity;
    private Button btnOK;
    private Button btnCancel;
    private EditText txtAnnotation;

    public TextDialog(IAnnotatable parentActivity) {
        super((Activity) parentActivity);
        this.parentActivity = parentActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_text);
        btnOK = (Button) findViewById(R.id.btnOK);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnOK.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        txtAnnotation = (EditText) findViewById(R.id.annotationText);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnOK:
                MusicAnnotation annotation = new MusicAnnotation();
                annotation.setType(MusicAnnotation.TEXT);
                annotation.setText(txtAnnotation.getText().toString());
                annotation.setTextSize(Preferences.getInstance().getIntPreference(Preferences.TEXT_SIZE));
                annotation.setColour(Preferences.getInstance().getIntPreference(Preferences.LINE_COLOUR));
                annotation.setTransparency(Preferences.getInstance().getIntPreference(Preferences.LINE_TRANSPARENCY));
                parentActivity.storeAnnotation(annotation);
                break;
            case R.id.btnCancel:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }
}
