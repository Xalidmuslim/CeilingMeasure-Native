package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

final class CeilingDrawingView extends View {
    enum Mode { WALL, POINT, DIAGONAL, LABEL }

    interface Listener {
        void onChanged();
        void onWallLabelTapped(int edgeIndex, float currentValue);
        void onDiagonalTapped(int diagonalIndex, float currentValue);
    }

    private static final int BLUE = Color.rgb(47, 143, 190);
    private static final int TEXT = Color.rgb(42, 61, 76);
    private static final int GRID = Color.rgb(232, 238, 243);
    private static final int RED = Color.rgb(205, 75, 75);

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScaleGestureDetector scaleDetector;

    private NativeProjectStore.RoomData room;
    private Mode mode = Mode.WALL;
    private Listener listener;

    private float scale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float lastCentroidX;
    private float lastCentroidY;
    private int draggingPoint = -1;
    private int diagonalStart = -1;
    private int draggingLabel = -1;
    private float downX;
    private float downY;
    private boolean moved;

    CeilingDrawingView(@NonNull Context context) {
        super(context);
        setBackgroundColor(Color.rgb(252, 253, 254));
        setFocusable(true);
        linePaint.setColor(TEXT);
        linePaint.setStrokeWidth(dp(2.2f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        pointPaint.setColor(BLUE);
        pointPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(GRID);
        gridPaint.setStrokeWidth(dp(1f));

        textPaint.setColor(TEXT);
        textPaint.setTextSize(sp(13));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));

        hitPaint.setColor(Color.argb(30, 47, 143, 190));
        hitPaint.setStyle(Paint.Style.FILL);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float old = scale;
                scale = clamp(scale * detector.getScaleFactor(), .55f, 3.5f);
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                if (old != 0f) {
                    offsetX = focusX - (focusX - offsetX) * (scale / old);
                    offsetY = focusY - (focusY - offsetY) * (scale / old);
                }
                invalidate();
                return true;
            }
        });
    }

    void setRoom(@NonNull NativeProjectStore.RoomData room) {
        this.room = room;
        NativeProjectStore.normalizeArrays(room);
        draggingPoint = -1;
        draggingLabel = -1;
        diagonalStart = -1;
        invalidate();
    }

    void setMode(@NonNull Mode mode) {
        this.mode = mode;
        draggingPoint = -1;
        draggingLabel = -1;
        diagonalStart = -1;
        invalidate();
    }

    Mode getMode() { return mode; }

    void setListener(@Nullable Listener listener) { this.listener = listener; }

    boolean hasClosedContour() { return room != null && room.closed; }

    void clearDrawing() {
        if (room == null) return;
        room.points.clear();
        room.wallValues.clear();
        room.labelDx.clear();
        room.labelDy.clear();
        room.diagonals.clear();
        room.closed = false;
        draggingPoint = -1;
        draggingLabel = -1;
        diagonalStart = -1;
        changed();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas);
        if (room == null) return;

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        drawEdges(canvas);
        drawDiagonals(canvas);
        drawPoints(canvas);
        drawLabels(canvas);

        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        float step = dp(28);
        for (float x = 0; x < getWidth(); x += step) canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        for (float y = 0; y < getHeight(); y += step) canvas.drawLine(0, y, getWidth(), y, gridPaint);
    }

    private void drawEdges(Canvas canvas) {
        if (room.points.size() < 2) return;
        Path path = new Path();
        NativeProjectStore.PointData first = room.points.get(0);
        path.moveTo(first.x, first.y);
        for (int i = 1; i < room.points.size(); i++) {
            NativeProjectStore.PointData p = room.points.get(i);
            path.lineTo(p.x, p.y);
        }
        if (room.closed) path.close();
        canvas.drawPath(path, linePaint);
    }

    private void drawDiagonals(Canvas canvas) {
        Paint p = new Paint(linePaint);
        p.setColor(Color.rgb(112, 130, 144));
        p.setStrokeWidth(dp(1.5f) / scale);
        p.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(7)/scale, dp(5)/scale}, 0));
        for (int i = 0; i < room.diagonals.size(); i++) {
            NativeProjectStore.DiagonalData d = room.diagonals.get(i);
            if (!validPoint(d.a) || !validPoint(d.b)) continue;
            NativeProjectStore.PointData a = room.points.get(d.a);
            NativeProjectStore.PointData b = room.points.get(d.b);
            canvas.drawLine(a.x, a.y, b.x, b.y, p);
            if (d.value > 0) {
                float mx=(a.x+b.x)/2f, my=(a.y+b.y)/2f;
                drawTextBadge(canvas, formatValue(d.value), mx, my);
            }
        }
    }

    private void drawPoints(Canvas canvas) {
        float r = dp(5.5f) / scale;
        for (int i = 0; i < room.points.size(); i++) {
            NativeProjectStore.PointData p = room.points.get(i);
            pointPaint.setColor(i == diagonalStart ? RED : BLUE);
            canvas.drawCircle(p.x, p.y, r, pointPaint);
        }
    }

    private void drawLabels(Canvas canvas) {
        int edges = edgeCount();
        for (int i = 0; i < edges; i++) {
            PointF mid = edgeMid(i);
            if (mid == null) continue;
            float dx = i < room.labelDx.size() ? room.labelDx.get(i) : 0;
            float dy = i < room.labelDy.size() ? room.labelDy.get(i) : 0;
            float value = i < room.wallValues.size() ? room.wallValues.get(i) : 0;
            String text = value > 0 ? formatValue(value) : String.valueOf(i + 1);
            drawTextBadge(canvas, text, mid.x + dx, mid.y + dy);
        }
    }

    private void drawTextBadge(Canvas canvas, String text, float x, float y) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.WHITE);
        float padX = dp(7)/scale, padY = dp(4)/scale;
        float textWidth = textPaint.measureText(text) / scale;
        float h = dp(21)/scale;
        RectF rect = new RectF(x - textWidth/2 - padX, y - h/2, x + textWidth/2 + padX, y + h/2);
        canvas.drawRoundRect(rect, dp(7)/scale, dp(7)/scale, bg);
        Paint tp = new Paint(textPaint);
        tp.setTextSize(textPaint.getTextSize()/scale);
        Paint.FontMetrics fm = tp.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent)/2f;
        canvas.drawText(text, x, baseline, tp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);

        if (e.getPointerCount() >= 2) {
            getParent().requestDisallowInterceptTouchEvent(true);
            float cx = centroidX(e), cy = centroidY(e);
            if (e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                lastCentroidX = cx;
                lastCentroidY = cy;
            } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE && !scaleDetector.isInProgress()) {
                offsetX += cx - lastCentroidX;
                offsetY += cy - lastCentroidY;
                lastCentroidX = cx;
                lastCentroidY = cy;
                invalidate();
            }
            return true;
        }

        float x = screenToWorldX(e.getX());
        float y = screenToWorldY(e.getY());

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                moved = false;
                draggingPoint = mode == Mode.POINT ? nearestPoint(x, y, dp(24)/scale) : -1;
                draggingLabel = mode == Mode.LABEL ? nearestLabel(x, y, dp(30)/scale) : -1;
                getParent().requestDisallowInterceptTouchEvent(!(mode == Mode.WALL && room != null && room.closed));
                return true;

            case MotionEvent.ACTION_MOVE:
                moved |= Math.hypot(e.getX() - downX, e.getY() - downY) > dp(7);
                if (mode == Mode.POINT && draggingPoint >= 0 && validPoint(draggingPoint)) {
                    NativeProjectStore.PointData p = room.points.get(draggingPoint);
                    p.x = x;
                    p.y = y;
                    invalidate();
                    if (listener != null) listener.onChanged();
                    return true;
                }
                if (mode == Mode.LABEL && draggingLabel >= 0) {
                    PointF mid = edgeMid(draggingLabel);
                    if (mid != null) {
                        ensureLabelArrays();
                        room.labelDx.set(draggingLabel, x - mid.x);
                        room.labelDy.set(draggingLabel, y - mid.y);
                        invalidate();
                        if (listener != null) listener.onChanged();
                    }
                    return true;
                }
                return true;

            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (moved) {
                    draggingPoint = -1;
                    draggingLabel = -1;
                    return true;
                }
                handleTap(x, y);
                draggingPoint = -1;
                draggingLabel = -1;
                return true;

            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                draggingPoint = -1;
                draggingLabel = -1;
                return true;

            default:
                return true;
        }
    }

    private void handleTap(float x, float y) {
        if (room == null) return;
        if (mode == Mode.WALL) {
            if (room.closed) return;
            int nearFirst = room.points.size() >= 3 ? nearestSpecificPoint(0, x, y, dp(26)/scale) : -1;
            if (nearFirst == 0) {
                room.closed = true;
                NativeProjectStore.normalizeArrays(room);
                performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                changed();
                return;
            }
            room.points.add(new NativeProjectStore.PointData(x, y));
            NativeProjectStore.normalizeArrays(room);
            changed();
            return;
        }

        if (mode == Mode.DIAGONAL) {
            int p = nearestPoint(x, y, dp(28)/scale);
            if (p < 0) {
                int d = nearestDiagonal(x, y, dp(18)/scale);
                if (d >= 0 && listener != null) {
                    listener.onDiagonalTapped(d, room.diagonals.get(d).value);
                }
                return;
            }
            if (diagonalStart < 0) {
                diagonalStart = p;
                invalidate();
            } else if (p != diagonalStart) {
                if (!diagonalExists(diagonalStart, p)) {
                    room.diagonals.add(new NativeProjectStore.DiagonalData(diagonalStart, p, 0));
                    performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    changed();
                }
                diagonalStart = -1;
                invalidate();
            }
            return;
        }

        if (mode == Mode.LABEL) {
            int label = nearestLabel(x, y, dp(34)/scale);
            if (label >= 0 && listener != null) {
                ensureLabelArrays();
                listener.onWallLabelTapped(label, room.wallValues.get(label));
            }
        }
    }

    private int edgeCount() {
        if (room == null) return 0;
        return room.closed ? room.points.size() : Math.max(0, room.points.size() - 1);
    }

    private PointF edgeMid(int edge) {
        if (room == null || edge < 0 || edge >= edgeCount()) return null;
        int a = edge;
        int b = (edge + 1) % room.points.size();
        NativeProjectStore.PointData p1 = room.points.get(a);
        NativeProjectStore.PointData p2 = room.points.get(b);
        return new PointF((p1.x+p2.x)/2f, (p1.y+p2.y)/2f);
    }

    private int nearestLabel(float x, float y, float radius) {
        int best=-1; float bd=Float.MAX_VALUE;
        for(int i=0;i<edgeCount();i++){
            PointF mid=edgeMid(i); if(mid==null)continue;
            float dx=i<room.labelDx.size()?room.labelDx.get(i):0;
            float dy=i<room.labelDy.size()?room.labelDy.get(i):0;
            float d=(float)Math.hypot(x-(mid.x+dx),y-(mid.y+dy));
            if(d<radius&&d<bd){bd=d;best=i;}
        }
        return best;
    }

    private int nearestPoint(float x, float y, float radius) {
        int best=-1; float bd=Float.MAX_VALUE;
        for(int i=0;i<room.points.size();i++){
            NativeProjectStore.PointData p=room.points.get(i);
            float d=(float)Math.hypot(x-p.x,y-p.y);
            if(d<radius&&d<bd){bd=d;best=i;}
        }
        return best;
    }

    private int nearestSpecificPoint(int index,float x,float y,float radius){
        if(!validPoint(index))return -1;
        NativeProjectStore.PointData p=room.points.get(index);
        return Math.hypot(x-p.x,y-p.y)<=radius?index:-1;
    }

    private int nearestDiagonal(float x,float y,float radius){
        int best=-1; float bd=Float.MAX_VALUE;
        for(int i=0;i<room.diagonals.size();i++){
            NativeProjectStore.DiagonalData d=room.diagonals.get(i);
            if(!validPoint(d.a)||!validPoint(d.b))continue;
            NativeProjectStore.PointData a=room.points.get(d.a),b=room.points.get(d.b);
            float dist=distancePointToSegment(x,y,a.x,a.y,b.x,b.y);
            if(dist<radius&&dist<bd){bd=dist;best=i;}
        }
        return best;
    }

    private boolean diagonalExists(int a,int b){
        for(NativeProjectStore.DiagonalData d:room.diagonals){
            if((d.a==a&&d.b==b)||(d.a==b&&d.b==a))return true;
        }
        return false;
    }

    private void ensureLabelArrays(){ NativeProjectStore.normalizeArrays(room); }
    private boolean validPoint(int i){return room!=null&&i>=0&&i<room.points.size();}
    private void changed(){invalidate();if(listener!=null)listener.onChanged();}
    private float screenToWorldX(float x){return (x-offsetX)/scale;}
    private float screenToWorldY(float y){return (y-offsetY)/scale;}
    private float centroidX(MotionEvent e){float s=0;for(int i=0;i<e.getPointerCount();i++)s+=e.getX(i);return s/e.getPointerCount();}
    private float centroidY(MotionEvent e){float s=0;for(int i=0;i<e.getPointerCount();i++)s+=e.getY(i);return s/e.getPointerCount();}
    private float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private float sp(float v){return v*getResources().getDisplayMetrics().scaledDensity;}
    private String formatValue(float v){return Math.abs(v-Math.round(v))<.01f?String.valueOf(Math.round(v)):String.format(java.util.Locale.US,"%.1f",v);}

    private static float distancePointToSegment(float px,float py,float ax,float ay,float bx,float by){
        float dx=bx-ax,dy=by-ay;
        if(dx==0&&dy==0)return (float)Math.hypot(px-ax,py-ay);
        float t=((px-ax)*dx+(py-ay)*dy)/(dx*dx+dy*dy);
        t=Math.max(0,Math.min(1,t));
        float cx=ax+t*dx,cy=ay+t*dy;
        return (float)Math.hypot(px-cx,py-cy);
    }
}
