package com.sk7software.musicviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.sk7software.musicviewer.model.DisplayPoint;
import com.sk7software.musicviewer.model.MusicAnnotation;
import com.sk7software.musicviewer.model.MusicFile;
import com.sk7software.musicviewer.model.Preferences;

public class MusicView extends View {

    private static final String TAG = MusicView.class.getSimpleName();

    private MusicFile musicFile;
    private MusicAnnotation currentAnnotation;
    private int pageNo;
    private int annotationMode;
    private int selectedAnnotationId;
    private Point dragPoint;
    private boolean clearFirst;
    private Button btnDel;
    private boolean showOverlays = false;

    public static final int MODE_ANNOTATE_FREEHAND = 2;
    public static final int MODE_ANNOTATE_TEXT = 3;
    public static final int MODE_ANNOTATE_FINGERS = 4;
    public static final int MODE_ANNOTATE_EDIT = 5;

    public MusicView(Context context) {
        super(context);
    }

    public MusicView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MusicView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MusicView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setMusicFile(MusicFile musicFile) {
        this.musicFile = musicFile;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (clearFirst) {
            canvas.drawColor(Color.WHITE, android.graphics.PorterDuff.Mode.CLEAR);
            clearFirst = false;
        }

        Bitmap pdfBitmap = PdfHelper.getInstance().getDisplayBitmap();
        if (pdfBitmap == null || musicFile == null) {
            return;
        }
        int left = (this.getWidth() - pdfBitmap.getWidth()) / 2;
        canvas.drawBitmap(pdfBitmap, left, 0, new Paint());

        if (currentAnnotation != null) {
            drawAnnotation(canvas, currentAnnotation);
        }

        if (showOverlays) {
            overlayBanners(pdfBitmap, canvas, left);
            return;
        }

        if (musicFile.getAnnotations() == null || PdfHelper.getInstance().isScrolling()) {
            return;
        }

        for (MusicAnnotation annotation : musicFile.getAnnotations()) {
            if (annotation.getPage() == pageNo && annotation.getPoints() != null) {
                drawAnnotation(canvas, annotation);
            }
        }
    }

    private void drawAnnotation(Canvas canvas, MusicAnnotation annotation) {
        switch (annotation.getType()) {
            case MusicAnnotation.FREEHAND:
                AnnotationRenderer.drawFreehandAnnotation(canvas, annotation);
                break;
            case MusicAnnotation.TEXT:
                AnnotationRenderer.drawTextAnnotation(canvas, annotation);
                break;
            case MusicAnnotation.FINGERING:
                AnnotationRenderer.drawFingersAnnotation(canvas, annotation);
        }
        showSelectedAnnotation(canvas, annotation);
    }

    private void showSelectedAnnotation(Canvas canvas, MusicAnnotation annotation) {
        if (annotation.getId() == selectedAnnotationId) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.YELLOW);
            p.setAlpha(100);
            canvas.drawRect(annotation.getBoundingRect(), p);
        }
    }

    private void overlayBanners(Bitmap baseImage, Canvas canvas, int left) {
        Paint p = new Paint();
        p.setColor(Color.BLUE);
        p.setAlpha(50);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setStrokeWidth(1);

        Rect topRect = new Rect(left, 0,
                baseImage.getWidth() + left, baseImage.getHeight() * musicFile.getTopPct() / 100);
        canvas.drawRect(topRect, p);

        Rect bottomRect = new Rect(left, baseImage.getHeight() - (baseImage.getHeight() * musicFile.getBottomPct() / 100),
                baseImage.getWidth() + left, baseImage.getHeight());
        canvas.drawRect(bottomRect, p);

        if (PdfHelper.getInstance().isLastPage(pageNo)) {
            p.setColor(Color.RED);
            Rect lastPage = new Rect(left, baseImage.getHeight() * musicFile.getLastPageStop() / 100,
                    baseImage.getWidth() + left, 3 + (baseImage.getHeight() * musicFile.getLastPageStop() / 100));
            canvas.drawRect(lastPage, p);
        }
    }

    public void addFreehandAnnotation(int pageNo) {
        MusicAnnotation annotation = new MusicAnnotation();
        annotation.setType(MusicAnnotation.FREEHAND);
        annotation.setPage(pageNo);
        annotation.setColour(Preferences.getInstance().getIntPreference(Preferences.LINE_COLOUR, Color.BLACK));
        annotation.setLineWidth(Preferences.getInstance().getIntPreference(Preferences.LINE_WIDTH, 2));
        annotation.setTransparency(Preferences.getInstance().getIntPreference(Preferences.LINE_TRANSPARENCY, 255));
        currentAnnotation = annotation;
    }

    public void endAnnotation() {
        currentAnnotation.calcBoundingRect();
        musicFile.addAnnotation(currentAnnotation);
    }

    public void addAnnotationPoint(Point point) {
        currentAnnotation.addPoint(point,
                PdfHelper.getInstance().getPdfBitmap().getWidth(), PdfHelper.getInstance().getPdfBitmap().getHeight());
    }

    public void setPaintColour(int colour) {
        if (currentAnnotation != null) {
            currentAnnotation.setColour(colour);
        }
    }

    public void setPaintWidth(int width) {
        if (currentAnnotation != null) {
            currentAnnotation.setLineWidth(width);
        }
    }

    public void setPaintTransparency(int transparency) {
        if (currentAnnotation != null) {
            currentAnnotation.setTransparency(transparency);
        }
    }

    public void setPaintTextSize(int textSize) {
        if (currentAnnotation != null) {
            currentAnnotation.setTextSize(textSize);
            currentAnnotation.calcBoundingRect();
        }
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public void setSelectedAnnotationId(int id) {
        this.selectedAnnotationId = id;
        currentAnnotation = musicFile.getAnnotations().stream().filter(a -> a.getId() == id).findFirst().orElse(null);
    }

    public int getSelectedAnnotationId() {
        return this.selectedAnnotationId;
    }

    public void moveAnnotation(int dx, int dy) {
        if (currentAnnotation != null) {
            currentAnnotation.shiftPoints(dx, dy,
                    PdfHelper.getInstance().getPdfBitmap().getWidth(), PdfHelper.getInstance().getPdfBitmap().getHeight());
        }
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setClearCanvas(boolean clearCanvas) {
        this.clearFirst = clearCanvas;
    }

    public boolean handleTouchEvent(MotionEvent motionEvent) {
        if (annotationMode != 0) {
            handleAnnotation(motionEvent);
        } else {
            if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                return true;
            }

            // Come out of scrolling mode if view is touched
            PdfHelper.getInstance().setScrolling(false);

            if (motionEvent.getX() < this.getWidth() / 2) {
                if (pageNo > 0) pageNo--;
            } else {
                pageNo++;
            }
            if (PdfHelper.getInstance().getPdfBitmap(pageNo) != null) {
                setPageNo(pageNo);
                updateAnnotations();
                invalidate();
            } else {
                if (pageNo > 0) pageNo--;
            }
        }
        return true;
    }

    public void updateAnnotations() {
        if (musicFile.getAnnotations() != null) {
            for (MusicAnnotation annotation : musicFile.getAnnotations()) {
                if (annotation.getPage() == pageNo) {
                    annotation.convertPoints(PdfHelper.getInstance().getPdfBitmap().getWidth(),
                            PdfHelper.getInstance().getPdfBitmap().getHeight());
                    annotation.calcBoundingRect();
                }
            }
        }
    }

    public void handleAnnotation(MotionEvent motionEvent) {
        int x = (int)motionEvent.getX();
        int y = (int)motionEvent.getY();

        if (annotationMode == MODE_ANNOTATE_FREEHAND) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                addFreehandAnnotation(pageNo);
                addAnnotationPoint(new Point(x, y));
            } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                addAnnotationPoint(new Point(x, y));
                invalidate();
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                endAnnotation();
            }
        } else if (annotationMode == MODE_ANNOTATE_EDIT) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                MusicAnnotation annotation = musicFile.findAnnotation(motionEvent.getX(), motionEvent.getY(), pageNo);
                if (annotation != null) {
                    setSelectedAnnotationId(annotation.getId());
                    dragPoint = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
                    btnDel = btnDel == null ? createDeleteButton() : btnDel;
                    btnDel.setVisibility(View.VISIBLE);
                    btnDel.setX(annotation.getBoundingRect().left - 15);
                    btnDel.setY(annotation.getBoundingRect().top - 15);
                } else {
                    setSelectedAnnotationId(-1);
                    if (btnDel != null) {
                        btnDel.setVisibility(View.INVISIBLE);
                    }
                }
                invalidate();
            } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                if (getSelectedAnnotationId() > 0) {
                    moveAnnotation((int)(motionEvent.getX() - dragPoint.x), (int)(motionEvent.getY() - dragPoint.y));
                    btnDel.setX(btnDel.getX() + (int)(motionEvent.getX() - dragPoint.x));
                    btnDel.setY(btnDel.getY() + (int)(motionEvent.getY() - dragPoint.y));
                    dragPoint.x = (int) motionEvent.getX();
                    dragPoint.y = (int) motionEvent.getY();
                    invalidate();
                }
            }
        }
    }

    public void setAnnotationMode(int annotationMode) {
        this.annotationMode = annotationMode;
        if (annotationMode == 0) {
            currentAnnotation = null;
        }
    }

    private Button createDeleteButton() {
        Button btnDelete = new Button(getContext());
        btnDelete.setVisibility(View.INVISIBLE);
        btnDelete.setBackgroundColor(Color.RED);
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setText("X");
        btnDelete.setTextSize(15);
        btnDelete.setPadding(0, 0, 0, 0);

        btnDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                musicFile.removeAnnotation(getSelectedAnnotationId());
                setSelectedAnnotationId(-1);
                btnDelete.setVisibility(View.INVISIBLE);
                invalidate();
            }
        });
        ((MusicActivity) getContext()).addContentView(btnDelete, new RelativeLayout.LayoutParams(50, 50));
        return btnDelete;
    }

    public void setShowOverlays(boolean showOverlays) {
        this.showOverlays = showOverlays;
    }
}
