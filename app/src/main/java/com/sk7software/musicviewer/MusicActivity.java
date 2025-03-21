package com.sk7software.musicviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.sk7software.musicviewer.model.DisplayPoint;
import com.sk7software.musicviewer.model.MusicAnnotation;
import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.model.Preferences;
import com.sk7software.musicviewer.network.NetworkRequest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MusicActivity extends AppCompatActivity implements IUpdateable, IAnnotatable {

    private SpeechRecognizer recognizer;
    private MusicView musicView;
    private long lastTurn = 0;
    private int scrollMode;
    private TextView txtRate;
    private MusicFile selectedFile = null;
    private Button btnMenu;
    private LinearLayout slideMenu;
    private boolean changed = false;
    private Dialog progressDialog;

    private static final String TAG = MusicActivity.class.getSimpleName();
    private static final long MIN_TURN_INTERVAL = 5000;
    private static final int MODE_GO = 0;
    private static final int MODE_PAUSE = 1;
    ActivityResultLauncher<Intent> settingsResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "Result: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent i = result.getData();
                        if (i.getSerializableExtra("file") != null) {
                            selectedFile = (MusicFile) i.getSerializableExtra("file");
                            txtRate.setText(String.valueOf(selectedFile.getDelay()));
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scrollMode = MODE_PAUSE;

        //createSpeechRecogniser();

        selectedFile = (MusicFile) getIntent().getSerializableExtra("file");
        setTitle(selectedFile.getDisplayName());

        Button go = (Button)findViewById(R.id.go);
        Button slower = (Button)findViewById(R.id.slower);
        Button faster = (Button)findViewById(R.id.faster);
        txtRate = (TextView)findViewById(R.id.rate);
        txtRate.setText(String.valueOf(selectedFile.getDelay()));
        btnMenu = (Button)findViewById(R.id.btnMenu);
        slideMenu = (LinearLayout)findViewById(R.id.slideMenu);

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scrollMode == MODE_PAUSE) {
                    PdfHelper.getInstance().setStopScroll(false);
                    PdfHelper.getInstance().setScrolling(true);
                    PdfHelper.getInstance().scroll();
                    scrollMode = MODE_GO;
                    go.setText("PAUSE");
                } else {
                    PdfHelper.getInstance().setStopScroll(true);
                    scrollMode = MODE_PAUSE;
                    go.setText("GO");
                }
            }
        });

        slower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int delay = selectedFile.getDelay();
                delay += delay/20;
                selectedFile.setDelay(delay);
                txtRate.setText(String.valueOf(selectedFile.getDelay()));
                changed = true;
            }
        });

        faster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int delay = selectedFile.getDelay();
                delay -= delay/20;
                selectedFile.setDelay(delay);
                txtRate.setText(String.valueOf(selectedFile.getDelay()));
                changed = true;
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateMenu(false);
            }
        });

        // Show progress dialog
        AlertDialog.Builder progressDialogBuilder;
        progressDialogBuilder = new AlertDialog.Builder(MusicActivity.this);
        progressDialogBuilder.setView(R.layout.progress);
        progressDialog = progressDialogBuilder
                .setMessage("Loading " + selectedFile.getName())
                .create();
        progressDialog.show();

        musicView = (MusicView) findViewById(R.id.imgView);
        musicView.setMusicFile(selectedFile);

        musicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return musicView.handleTouchEvent(motionEvent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        final View rootView = this.getWindow().getDecorView().getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "Layout listener");
                // Ensure you call it only once :
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                NetworkRequest.fetchAnnotations(MusicActivity.this, selectedFile.getId(), new NetworkRequest.NetworkCallback() {
                    @Override
                    public void onRequestCompleted(Object callbackData) {
                        List<MusicAnnotation> annotations = (List<MusicAnnotation>)callbackData;
                        for (MusicAnnotation a : annotations) {
                            selectedFile.addAnnotation(a);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error: " + e);
                    }
                });
                Point dimensions = new Point(musicView.getWidth(), musicView.getHeight());
                musicView.setMinimumHeight(dimensions.y);
                PdfHelper.getInstance().initialise(selectedFile, dimensions, MusicActivity.this);
                PdfHelper.getInstance().loadPDF(false);
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (changed) {
            Log.d(TAG, "Updating file settings");
            NetworkRequest.updateFile(ApplicationContextProvider.getContext(), selectedFile, new NetworkRequest.NetworkCallback() {
                @Override
                public void onRequestCompleted(Object callbackData) {
                    Log.d(TAG, "Music document updated successfully");
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error: " + e);
                }
            });
        }

        if (recognizer != null) {
            recognizer.destroy();
        }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                // Open settings dialog
                Intent settings = new Intent(ApplicationContextProvider.getContext(), SettingsActivity.class);
                settings.putExtra("file", selectedFile);
                settingsResultLauncher.launch(settings);
                return true;
            case R.id.action_annotate:
                // Change annotation mode
                btnMenu.setVisibility(View.INVISIBLE);
                musicView.setAnnotationMode(0);
                animateMenu(true);
                toggleEditColour(item, 0xFFFFFFFF);
                return true;
            case R.id.action_annotate_freehand:
                btnMenu.setVisibility(View.VISIBLE);
                musicView.setAnnotationMode(MusicView.MODE_ANNOTATE_FREEHAND);
                toggleEditColour(item, 0xFFFF0000);
                changed = true;
                return true;
            case R.id.action_annotate_text:
                btnMenu.setVisibility(View.VISIBLE);
                toggleEditColour(item, 0xFFFF0000);
                musicView.setAnnotationMode(MusicView.MODE_ANNOTATE_TEXT);
                TextDialog cdd = new TextDialog(this);
                cdd.show();
                return true;
            case R.id.action_annotate_fingers:
                btnMenu.setVisibility(View.VISIBLE);
                musicView.setAnnotationMode(MusicView.MODE_ANNOTATE_FINGERS);
                toggleEditColour(item, 0xFFFF0000);
                FingersDialog fdd = new FingersDialog(this);
                fdd.show();
                return true;
            case R.id.action_annotate_edit:
                btnMenu.setVisibility(View.VISIBLE);
                musicView.setAnnotationMode(MusicView.MODE_ANNOTATE_EDIT);
                toggleEditColour(item, 0xFFFF0000);
                changed = true;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toggleEditColour(MenuItem item, int filter) {
        Drawable drawable = item.getIcon();
        if(drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(filter, PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Override
    public void afterLoad() {
        Log.d(TAG, "*** After load");
        if (recognizer != null) {
            recognizer.startListening(createSpeechIntent());
        }
        if (PdfHelper.getInstance().getPdfBitmap() == null) {
            PdfHelper.getInstance().getPdfBitmap(0);
            musicView.setPageNo(0);
            musicView.updateAnnotations();
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        musicView.invalidate();
    }

    private void createSpeechRecogniser() {
        if (SpeechRecognizer.isRecognitionAvailable(ApplicationContextProvider.getContext())) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this, ComponentName.unflattenFromString("com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService"));
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {
                    Log.d(TAG, "Ready for speech");
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech");
                }

                @Override
                public void onRmsChanged(float v) {
                }

                @Override
                public void onBufferReceived(byte[] bytes) {
                    Log.d(TAG, "Buffer received");
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "End of speech");
                }

                @Override
                public void onError(int i) {
                    Log.d(TAG, "Error: " + i);

                    if (i == SpeechRecognizer.ERROR_NO_MATCH) {
                        Log.d(TAG, "Restarting recogniser");
                        recognizer.cancel();
                        recognizer.startListening(createSpeechIntent());
                    }
                }

                @Override
                public void onResults(Bundle bundle) {
                    Log.d(TAG, "On results");
                    handleCommand(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                    recognizer.cancel();
                    recognizer.startListening(createSpeechIntent());
                }

                @Override
                public void onPartialResults(Bundle bundle) {
                    Log.d(TAG, "Partial results");
                    handleCommand(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                }

                @Override
                public void onEvent(int i, Bundle bundle) {
                    Log.d(TAG, "Event");
                }
            });
        } else {
            Log.d(TAG, "No speech recogniser available");
        }
    }

    private void handleCommand(List<String> matches) {
        if (matches != null && !matches.isEmpty()) {
            String[] words = matches.get(0).split(" ");
            Log.d(TAG, words.length + ":" + Arrays.toString(words));
            if (Arrays.stream(words).anyMatch("over"::equals)) {
                checkAndTurnPage(1);
            } else if (Arrays.stream(words).anyMatch("return"::equals)) {
                checkAndTurnPage(-1);
            }
        }
    }

    private Intent createSpeechIntent() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB");
        return i;
    }

    private void checkAndTurnPage(int increment) {
        long now = new Date().getTime();
        if (now - lastTurn >= MIN_TURN_INTERVAL) {
            lastTurn = now;
        }
    }

    private void initialisePanel() {
        SeekBar seekTransparency = (SeekBar) findViewById(R.id.seekTransparency);
        seekTransparency.setProgress(255 - Preferences.getInstance().getIntPreference(Preferences.LINE_TRANSPARENCY, 0));

        SeekBar seekWidth = (SeekBar) findViewById(R.id.seekWidth);
        seekWidth.setProgress(Preferences.getInstance().getIntPreference(Preferences.LINE_WIDTH, 2));

        SeekBar seekTextSize = (SeekBar) findViewById(R.id.seekTextSize);
        seekTextSize.setProgress(Preferences.getInstance().getIntPreference(Preferences.TEXT_SIZE, 35));

        RadioGroup radColour = (RadioGroup) findViewById(R.id.radioColours);
        radColour.check(getRadioId());

        radColour.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.radioBlack:
                        musicView.setPaintColour(Color.BLACK);
                        Preferences.getInstance().addPreference(Preferences.LINE_COLOUR, Color.BLACK);
                        break;
                    case R.id.radioRed:
                        musicView.setPaintColour(Color.RED);
                        Preferences.getInstance().addPreference(Preferences.LINE_COLOUR, Color.RED);
                        break;
                    case R.id.radioBlue:
                        musicView.setPaintColour(Color.BLUE);
                        Preferences.getInstance().addPreference(Preferences.LINE_COLOUR, Color.BLUE);
                        break;
                    case R.id.radioGreen:
                        musicView.setPaintColour(Color.GREEN);
                        Preferences.getInstance().addPreference(Preferences.LINE_COLOUR, Color.GREEN);
                        break;
                }
                musicView.invalidate();
            }
        });

        seekWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                musicView.setPaintWidth(i);
                Preferences.getInstance().addPreference(Preferences.LINE_WIDTH, i);
                musicView.invalidate();
            };

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekTransparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int transparency = 255 - i;
                musicView.setPaintTransparency(transparency);
                Preferences.getInstance().addPreference(Preferences.LINE_TRANSPARENCY, transparency);
                musicView.invalidate();
            };

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                musicView.setPaintTextSize(i);
                Preferences.getInstance().addPreference(Preferences.TEXT_SIZE, i);
                musicView.invalidate();
            };

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private int getRadioId() {
        int colour = Preferences.getInstance().getIntPreference(Preferences.LINE_COLOUR, Color.BLACK);
        switch (colour) {
            case Color.BLACK:
                return R.id.radioBlack;
            case Color.RED:
                return R.id.radioRed;
            case Color.BLUE:
                return R.id.radioBlue;
            case Color.GREEN:
                return R.id.radioGreen;
            default:
                return R.id.radioBlack;
        }
    }

    private void animateMenu(boolean forceClose) {
        if ((forceClose && slideMenu.getVisibility() == View.VISIBLE) || slideMenu.getVisibility() == View.VISIBLE) {
            TranslateAnimation animate = new TranslateAnimation(
                    0,
                    -slideMenu.getWidth(),
                    0,
                    0);
            animate.setDuration(250);
            slideMenu.startAnimation(animate);
            slideMenu.setVisibility(View.INVISIBLE);
        } else if (!forceClose && slideMenu.getVisibility() == View.INVISIBLE) {
            initialisePanel();
            TranslateAnimation animate = new TranslateAnimation(
                    -slideMenu.getWidth(),
                    0,
                    0,
                    0);
            animate.setDuration(250);
            slideMenu.startAnimation(animate);
            slideMenu.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void update(boolean clearFirst) {
        if (musicView != null) {
            musicView.setClearCanvas(clearFirst);
            musicView.invalidate();
        }
    }

    @Override
    public void storeAnnotation(MusicAnnotation annotation) {
        annotation.addPoint(new Point(musicView.getWidth()/2,musicView.getHeight()/2),
                PdfHelper.getInstance().getPdfBitmap().getWidth(), PdfHelper.getInstance().getPdfBitmap().getHeight());
        annotation.setPage(musicView.getPageNo());
        annotation.calcBoundingRect();
        int annotationId = selectedFile.addAnnotation(annotation);
        musicView.setSelectedAnnotationId(annotationId);
        musicView.setAnnotationMode(MusicView.MODE_ANNOTATE_EDIT);
        musicView.invalidate();
        changed = true;
    }
}