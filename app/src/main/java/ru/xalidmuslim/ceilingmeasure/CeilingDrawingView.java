package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CeilingDrawingView extends View {
    enum Mode { WALL, POINT, DIAGONAL, LABEL }

    interface Listener {
        void onChanged();
        void onVertexLongPressed(@NonNull String pointId);
        void onWallLongPressed(@NonNull String edgeId, float currentValue);
        void onDiagonalCreated(@NonNull String diagonalId, float currentValue);
        void onDiagonalLongPressed(@NonNull String diagonalId, float currentValue);
        void onContourClosed();
    }

    private static final int BLUE = Color.rgb(43, 136, 191);
    private static final int TEXT = Color.rgb(32, 48, 60);
    private static final int GRID = Color.rgb(232, 238, 243);
    private static final int RED = Color.rgb(217, 74, 74);
    private static final int GREEN = Color.rgb(47, 154, 105);
    private static final long LONG_PRESS_MS = 330L;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScaleGestureDetector scaleDetector;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private NativeProjectStore.RoomData room;
    private Mode mode = Mode.WALL;
    private Listener listener;

    private float scale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float lastCentroidX;
    private float lastCentroidY;
    private boolean multiTouch;

    private float downScreenX;
    private float downScreenY;
    private boolean moved;
    private boolean longPressFired;
    private Runnable longPressRunnable;

    private WallDraft wallDraft;
    private String draggingPointId;
    private DiagDraft diagDraft;
    private LabelHit draggingLabel;
    private float labelBaseX;
    private float labelBaseY;
    private Guide activeGuide;
    private boolean point90SnapActive;
    private PointF magnifierFocus;
    private MagnifierKind magnifierKind;

    CeilingDrawingView(@NonNull Context context) {
        super(context);
        setBackgroundColor(Color.rgb(252, 253, 254));
        setFocusable(true);

        linePaint.setColor(TEXT);
        linePaint.setStrokeWidth(dp(2.2f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        pointPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(GRID);
        gridPaint.setStrokeWidth(dp(1f));

        textPaint.setColor(TEXT);
        textPaint.setTextSize(sp(13));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create(
                "sans-serif",
                android.graphics.Typeface.BOLD));

        previewPaint.setColor(BLUE);
        previewPaint.setStrokeWidth(dp(2.6f));
        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeCap(Paint.Cap.ROUND);

        guidePaint.setColor(RED);
        guidePaint.setStrokeWidth(dp(1.4f));
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(5)}, 0));

        scaleDetector = new ScaleGestureDetector(
                context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (room == null) return false;
                        float old = scale;
                        scale = clamp(scale * detector.getScaleFactor(), .45f, 12f);
                        float focusX = detector.getFocusX();
                        float focusY = detector.getFocusY();
                        if (old != 0f) {
                            offsetX = focusX - (focusX - offsetX) * (scale / old);
                            offsetY = focusY - (focusY - offsetY) * (scale / old);
                        }
                        syncViewToRoom();
                        invalidate();
                        return true;
                    }
                });
    }

    void setRoom(@NonNull NativeProjectStore.RoomData room) {
        cancelPendingLongPress();
        cancelTransientGestures();
        this.room = room;
        NativeGeometry.sanitize(room);
        scale = clamp(room.viewScale, .45f, 12f);
        offsetX = room.panX;
        offsetY = room.panY;
        invalidate();
    }

    void setMode(@NonNull Mode mode) {
        cancelPendingLongPress();
        cancelTransientGestures();
        this.mode = mode;
        invalidate();
    }

    Mode getMode() {
        return mode;
    }

    void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    void applySolvedGeometry(@NonNull Map<String, PointF> positions) {
        if (room == null || positions.isEmpty()) return;

        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (PointF p : positions.values()) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        float contentWidth = Math.max(1f, maxX - minX);
        float contentHeight = Math.max(1f, maxY - minY);
        float viewWidth = Math.max(dp(280), getWidth());
        float viewHeight = Math.max(dp(360), getHeight());
        float fit = Math.min(
                (viewWidth - dp(80)) / contentWidth,
                (viewHeight - dp(100)) / contentHeight);
        fit = Math.max(.05f, fit);

        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;
        float targetX = viewWidth / 2f;
        float targetY = viewHeight / 2f;

        for (NativeProjectStore.PointData point : room.points) {
            PointF solved = positions.get(point.id);
            if (solved == null) continue;
            point.x = targetX + (solved.x - centerX) * fit;
            point.y = targetY + (solved.y - centerY) * fit;
        }

        scale = 1f;
        offsetX = 0f;
        offsetY = 0f;
        room.viewScale = 1f;
        room.panX = 0f;
        room.panY = 0f;
        cancelTransientGestures();
        changed();
    }

    boolean hasClosedContour() {
        return room != null && NativeGeometry.isClosed(room);
    }

    void clearDrawing() {
        if (room == null) return;
        cancelPendingLongPress();
        cancelTransientGestures();
        room.points.clear();
        room.edges.clear();
        room.diagonals.clear();
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
        drawWallDraft(canvas);
        drawPoints(canvas);
        drawLabels(canvas);

        canvas.restore();
        drawMagnifierOverlay(canvas);
    }

    private void drawGrid(Canvas canvas) {
        float step = dp(28);
        float startX = offsetX % step;
        float startY = offsetY % step;
        for (float x = startX; x < getWidth(); x += step) {
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }
        for (float y = startY; y < getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }

    private void drawEdges(Canvas canvas) {
        for (NativeProjectStore.EdgeData edge : room.edges) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
            if (a == null || b == null) continue;
            canvas.drawLine(a.x, a.y, b.x, b.y, linePaint);
        }
    }

    private void drawDiagonals(Canvas canvas) {
        Paint paint = new Paint(linePaint);
        paint.setColor(Color.rgb(112, 130, 144));
        paint.setStrokeWidth(dp(1.5f) / scale);
        paint.setPathEffect(new DashPathEffect(
                new float[]{dp(7) / scale, dp(5) / scale},
                0));
        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, diagonal.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, diagonal.b);
            if (a == null || b == null) continue;
            canvas.drawLine(a.x, a.y, b.x, b.y, paint);
        }

        if (diagDraft != null) {
            Paint preview = new Paint(paint);
            preview.setColor(BLUE);
            preview.setPathEffect(null);
            canvas.drawLine(
                    diagDraft.start.x,
                    diagDraft.start.y,
                    diagDraft.preview.x,
                    diagDraft.preview.y,
                    preview);
        }
    }

    private void drawWallDraft(Canvas canvas) {
        if (wallDraft == null) return;
        canvas.drawLine(
                wallDraft.start.x,
                wallDraft.start.y,
                wallDraft.preview.x,
                wallDraft.preview.y,
                previewPaint);

        if (activeGuide != null) {
            float half = 1000f / Math.max(.45f, scale);
            float nx = -activeGuide.uy;
            float ny = activeGuide.ux;
            canvas.drawLine(
                    activeGuide.projected.x - nx * half,
                    activeGuide.projected.y - ny * half,
                    activeGuide.projected.x + nx * half,
                    activeGuide.projected.y + ny * half,
                    guidePaint);
        }
    }

    private void drawPoints(Canvas canvas) {
        float radius = dp(5.2f) / scale;
        for (NativeProjectStore.PointData point : room.points) {
            boolean diagStart = diagDraft != null && diagDraft.startId.equals(point.id);
            boolean moving = point.id.equals(draggingPointId);
            pointPaint.setColor(diagStart ? RED : (moving ? GREEN : BLUE));
            canvas.drawCircle(point.x, point.y, radius, pointPaint);
            drawPointLetter(canvas, point);
        }
    }

    private void drawPointLetter(Canvas canvas, NativeProjectStore.PointData point) {
        String value = "";
        if ("A".equals(point.customLabel)) {
            value = "A";
        } else if (room.showAutoLetters) {
            int index = NativeGeometry.pointIndex(room, point.id);
            if (index >= 0 && index < 26) value = String.valueOf((char) ('A' + index));
        }
        if (value.isEmpty()) return;

        PointF center = drawingCenter();
        float vx = point.x - center.x;
        float vy = point.y - center.y;
        float len = (float) Math.hypot(vx, vy);
        if (len < 1f) {
            vx = 0f;
            vy = -1f;
            len = 1f;
        }
        float offset = dp(18) / scale;
        float x = point.x + vx / len * offset;
        float y = point.y + vy / len * offset;

        Paint tp = new Paint(textPaint);
        tp.setTextSize(sp(12) / scale);
        tp.setColor(TEXT);
        Paint.FontMetrics fm = tp.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(value, x, baseline, tp);
    }

    private void drawLabels(Canvas canvas) {
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.value <= 0f && !room.showAutoLetters) continue;
            PointF pos = edgeLabelBase(edge);
            if (pos == null) continue;

            String value;
            if (room.showAutoLetters) {
                String label = edgeLetterLabel(edge);
                value = edge.value > 0f
                        ? label + " · " + formatValue(edge.value)
                        : label;
            } else {
                value = formatValue(edge.value);
            }
            drawTextBadge(canvas, value, pos.x + edge.labelDx, pos.y + edge.labelDy);
        }

        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            if (diagonal.value <= 0f && !room.showAutoLetters) continue;
            PointF pos = diagonalLabelBase(diagonal);
            if (pos == null) continue;

            String value;
            if (room.showAutoLetters) {
                String label = pointLetterLabel(diagonal.a) + pointLetterLabel(diagonal.b);
                value = diagonal.value > 0f
                        ? label + " · " + formatValue(diagonal.value)
                        : label;
            } else {
                value = formatValue(diagonal.value);
            }
            drawTextBadge(
                    canvas,
                    value,
                    pos.x + diagonal.labelDx,
                    pos.y + diagonal.labelDy);
        }
    }

    private void drawTextBadge(Canvas canvas, String value, float x, float y) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.WHITE);

        Paint tp = new Paint(textPaint);
        tp.setTextSize(textPaint.getTextSize() / scale);
        float padX = dp(7) / scale;
        float h = dp(22) / scale;
        float textWidth = tp.measureText(value);
        RectF rect = new RectF(
                x - textWidth / 2f - padX,
                y - h / 2f,
                x + textWidth / 2f + padX,
                y + h / 2f);
        canvas.drawRoundRect(rect, dp(7) / scale, dp(7) / scale, bg);

        Paint.FontMetrics fm = tp.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(value, x, baseline, tp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (room == null) return false;

        scaleDetector.onTouchEvent(event);

        if (event.getPointerCount() >= 2) {
            cancelPendingLongPress();
            cancelSingleGestureForMultiTouch();
            getParent().requestDisallowInterceptTouchEvent(true);
            float cx = centroidX(event);
            float cy = centroidY(event);
            if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                lastCentroidX = cx;
                lastCentroidY = cy;
                multiTouch = true;
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                    && !scaleDetector.isInProgress()) {
                offsetX += cx - lastCentroidX;
                offsetY += cy - lastCentroidY;
                lastCentroidX = cx;
                lastCentroidY = cy;
                syncViewToRoom();
                invalidate();
            }
            return true;
        }

        float worldX = screenToWorldX(event.getX());
        float worldY = screenToWorldY(event.getY());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                multiTouch = false;
                downScreenX = event.getX();
                downScreenY = event.getY();
                moved = false;
                longPressFired = false;
                activeGuide = null;

                if (mode == Mode.WALL) {
                    return beginWallGesture(worldX, worldY);
                }
                if (mode == Mode.POINT) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    NativeProjectStore.PointData point = nearestPoint(worldX, worldY, dp(48) / scale);
                    draggingPointId = point == null ? null : point.id;
                    if (point != null) scheduleLongPress("vertex", point.id);
                    return true;
                }
                if (mode == Mode.DIAGONAL) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return beginDiagonalGesture(worldX, worldY);
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                beginLabelGesture(worldX, worldY);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (multiTouch) return true;
                float distance = (float) Math.hypot(
                        event.getX() - downScreenX,
                        event.getY() - downScreenY);
                if (distance > dp(9)) {
                    moved = true;
                    cancelPendingLongPress();
                }

                if (mode == Mode.WALL && wallDraft != null) {
                    updateWallDraft(event.getX(), event.getY());
                    return true;
                }
                if (mode == Mode.POINT && draggingPointId != null) {
                    updatePointDrag(worldX, worldY);
                    return true;
                }
                if (mode == Mode.DIAGONAL && diagDraft != null) {
                    updateDiagonalDraft(worldX, worldY);
                    return true;
                }
                if (mode == Mode.LABEL && draggingLabel != null) {
                    updateLabelDrag(worldX, worldY);
                    return true;
                }
                return true;

            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                cancelPendingLongPress();
                if (multiTouch) {
                    multiTouch = false;
                    syncViewToRoom();
                    changed();
                    cancelTransientGestures();
                    return true;
                }
                if (longPressFired) {
                    longPressFired = false;
                    cancelTransientGestures();
                    return true;
                }

                if (mode == Mode.WALL && wallDraft != null) {
                    finishWallDraft();
                } else if (mode == Mode.POINT && draggingPointId != null) {
                    finishPointDrag();
                } else if (mode == Mode.DIAGONAL && diagDraft != null) {
                    finishDiagonalDraft();
                } else if (mode == Mode.LABEL && draggingLabel != null) {
                    draggingLabel = null;
                    changed();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                cancelPendingLongPress();
                cancelTransientGestures();
                return true;

            default:
                return true;
        }
    }

    private boolean beginWallGesture(float x, float y) {
        NativeProjectStore.PointData nearest = nearestPoint(x, y, dp(48) / scale);
        NativeProjectStore.EdgeData edge = nearestEdge(x, y, dp(30) / scale);
        boolean closed = NativeGeometry.isClosed(room);

        if (closed) {
            if (edge != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
                scheduleLongPress("edge", edge.id);
                return true;
            }
            getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        }

        getParent().requestDisallowInterceptTouchEvent(true);

        if (nearest != null) {
            scheduleLongPress("vertex", nearest.id);
            if (NativeGeometry.vertexDegree(room, nearest.id) >= 2) return true;
            wallDraft = new WallDraft(
                    nearest.id,
                    new PointF(nearest.x, nearest.y),
                    new PointF(nearest.x, nearest.y),
                    new PointF(downScreenX, downScreenY),
                    false);
            return true;
        }

        if (edge != null) {
            scheduleLongPress("edge", edge.id);
            return true;
        }

        if (room.edges.isEmpty()) {
            PointF start = new PointF(roundGrid(x), roundGrid(y));
            wallDraft = new WallDraft(
                    NativeProjectStore.newId("p"),
                    start,
                    new PointF(start.x, start.y),
                    new PointF(downScreenX, downScreenY),
                    true);
        }
        return true;
    }

    private boolean beginDiagonalGesture(float x, float y) {
        NativeProjectStore.DiagonalData existing = nearestDiagonal(x, y, dp(20) / scale);
        NativeProjectStore.PointData point = nearestPoint(x, y, dp(48) / scale);

        if (point != null) {
            scheduleLongPress("vertex", point.id);
            diagDraft = new DiagDraft(
                    point.id,
                    new PointF(point.x, point.y),
                    new PointF(point.x, point.y));
            return true;
        }
        if (existing != null) scheduleLongPress("diag", existing.id);
        return true;
    }

    private void beginLabelGesture(float x, float y) {
        draggingLabel = nearestLabel(x, y, dp(36) / scale);
        if (draggingLabel == null) return;
        PointF base = draggingLabel.kind == LabelKind.EDGE
                ? edgeLabelBase(edgeById(draggingLabel.id))
                : diagonalLabelBase(diagonalById(draggingLabel.id));
        if (base == null) {
            draggingLabel = null;
            return;
        }
        labelBaseX = base.x;
        labelBaseY = base.y;
    }

    private void updateWallDraft(float screenX, float screenY) {
        float dx = (screenX - wallDraft.fingerStart.x) / scale;
        float dy = (screenY - wallDraft.fingerStart.y) / scale;
        PointF finish = new PointF(
                wallDraft.start.x + dx,
                wallDraft.start.y + dy);

        if (room.wallAngleMagnet) finish = snapAngle(wallDraft.start, finish);
        finish.x = roundPrecisionGrid(finish.x);
        finish.y = roundPrecisionGrid(finish.y);

        NativeProjectStore.PointData near = nearestPoint(
                finish.x,
                finish.y,
                dp(22) / scale);
        boolean cornerSnap = near != null
                && !near.id.equals(wallDraft.startId)
                && NativeGeometry.vertexDegree(room, near.id) < 2;

        if (!cornerSnap) {
            NativeProjectStore.PointData along = nearestOpenPointAlongSegment(
                    wallDraft.start,
                    finish,
                    dp(22) / scale);
            if (along != null) {
                near = along;
                cornerSnap = true;
            }
        }

        if (cornerSnap && near != null) {
            wallDraft.preview.set(near.x, near.y);
            activeGuide = null;
        } else {
            Guide guide = wallStopGuide(finish);
            if (guide != null) {
                float screenDistance = (float) Math.hypot(
                        finish.x - guide.projected.x,
                        finish.y - guide.projected.y) * Math.max(.45f, scale);
                if (screenDistance <= dp(26)) {
                    finish.set(guide.projected.x, guide.projected.y);
                    if (activeGuide == null) {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    activeGuide = guide;
                } else {
                    activeGuide = guide;
                }
            } else {
                activeGuide = null;
            }
            wallDraft.preview.set(finish.x, finish.y);
        }
        magnifierFocus = new PointF(wallDraft.preview.x, wallDraft.preview.y);
        magnifierKind = MagnifierKind.WALL;
        invalidate();
    }

    private void finishWallDraft() {
        WallDraft draft = wallDraft;
        wallDraft = null;
        activeGuide = null;
        if (draft == null) return;

        float length = (float) Math.hypot(
                draft.preview.x - draft.start.x,
                draft.preview.y - draft.start.y);
        if (length <= dp(18) / scale) {
            invalidate();
            return;
        }

        if (NativeGeometry.isClosed(room)) {
            invalidate();
            return;
        }

        NativeProjectStore.PointData start = NativeGeometry.pointById(room, draft.startId);
        if (draft.newStart) {
            start = new NativeProjectStore.PointData(
                    draft.startId,
                    draft.start.x,
                    draft.start.y);
            room.points.add(start);
        }
        if (start == null || NativeGeometry.vertexDegree(room, start.id) >= 2) {
            invalidate();
            return;
        }

        NativeProjectStore.PointData target = nearestPoint(
                draft.preview.x,
                draft.preview.y,
                dp(36) / scale);
        if (target != null
                && (target.id.equals(start.id) || NativeGeometry.vertexDegree(room, target.id) >= 2)) {
            target = null;
        }

        boolean extended = NativeGeometry.extendStraightWall(
                room,
                start.id,
                draft.preview.x,
                draft.preview.y,
                target);

        if (!extended) {
            if (target == null) {
                target = new NativeProjectStore.PointData(
                        draft.preview.x,
                        draft.preview.y);
                room.points.add(target);
            }

            if (!target.id.equals(start.id)
                    && NativeGeometry.edgeBetween(room, start.id, target.id) == null) {
                room.edges.add(new NativeProjectStore.EdgeData(start.id, target.id));
            }
        }

        boolean closed = NativeGeometry.repairOvershotClosedContour(
                room,
                dp(28) / Math.max(.45f, scale));
        if (!closed) {
            closed = NativeGeometry.repairNearlyClosedContour(
                    room,
                    dp(24) / Math.max(.45f, scale));
        }
        if (!closed) closed = NativeGeometry.isClosed(room);

        magnifierFocus = null;
        magnifierKind = null;
        changed();
        if (closed && listener != null) listener.onContourClosed();
    }

    private void updatePointDrag(float x, float y) {
        NativeProjectStore.PointData point = NativeGeometry.pointById(room, draggingPointId);
        if (point == null) return;

        point.x = roundGrid(x);
        point.y = roundGrid(y);

        PointF snap = nearest90Candidate(point);
        if (snap != null && room.point90Magnet) {
            point.x = snap.x;
            point.y = snap.y;
        }

        boolean right = isRightAngle(point);
        magnifierFocus = new PointF(point.x, point.y);
        magnifierKind = MagnifierKind.POINT;
        if (right && !point90SnapActive) {
            point90SnapActive = true;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        } else if (!right) {
            point90SnapActive = false;
        }
        invalidate();
    }

    private void finishPointDrag() {
        draggingPointId = null;
        point90SnapActive = false;
        magnifierFocus = null;
        magnifierKind = null;
        NativeGeometry.repairNearlyClosedContour(
                room,
                dp(24) / Math.max(.45f, scale));
        changed();
    }

    private void updateDiagonalDraft(float x, float y) {
        if (diagDraft == null) return;
        NativeProjectStore.PointData near = nearestPoint(x, y, dp(48) / scale);
        if (near != null && !near.id.equals(diagDraft.startId)) {
            diagDraft.preview.set(near.x, near.y);
        } else {
            diagDraft.preview.set(x, y);
        }
        invalidate();
    }

    private void finishDiagonalDraft() {
        DiagDraft draft = diagDraft;
        diagDraft = null;
        if (draft == null) return;

        NativeProjectStore.PointData end = nearestPoint(
                draft.preview.x,
                draft.preview.y,
                dp(48) / scale);
        if (end == null || end.id.equals(draft.startId)) {
            invalidate();
            return;
        }
        if (NativeGeometry.edgeBetween(room, draft.startId, end.id) != null) {
            invalidate();
            return;
        }

        NativeProjectStore.DiagonalData diagonal =
                NativeGeometry.diagonalBetween(room, draft.startId, end.id);
        if (diagonal == null) {
            diagonal = new NativeProjectStore.DiagonalData(draft.startId, end.id);
            room.diagonals.add(diagonal);
            changed();
        }
        if (listener != null) listener.onDiagonalCreated(diagonal.id, diagonal.value);
    }

    private void updateLabelDrag(float x, float y) {
        if (draggingLabel == null) return;
        if (draggingLabel.kind == LabelKind.EDGE) {
            NativeProjectStore.EdgeData edge = edgeById(draggingLabel.id);
            if (edge != null) {
                edge.labelDx = x - labelBaseX;
                edge.labelDy = y - labelBaseY;
            }
        } else {
            NativeProjectStore.DiagonalData diagonal = diagonalById(draggingLabel.id);
            if (diagonal != null) {
                diagonal.labelDx = x - labelBaseX;
                diagonal.labelDy = y - labelBaseY;
            }
        }
        invalidate();
    }

    private void scheduleLongPress(String kind, String id) {
        cancelPendingLongPress();
        longPressRunnable = () -> {
            longPressFired = true;
            wallDraft = null;
            diagDraft = null;
            draggingPointId = null;
            draggingLabel = null;
            magnifierFocus = null;
            magnifierKind = null;
            invalidate();
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if (listener == null) return;
            if ("vertex".equals(kind)) {
                if (NativeGeometry.pointById(room, id) != null) {
                    listener.onVertexLongPressed(id);
                }
            } else if ("edge".equals(kind)) {
                NativeProjectStore.EdgeData edge = edgeById(id);
                if (edge != null) listener.onWallLongPressed(edge.id, edge.value);
            } else {
                NativeProjectStore.DiagonalData diagonal = diagonalById(id);
                if (diagonal != null) {
                    listener.onDiagonalLongPressed(diagonal.id, diagonal.value);
                }
            }
        };
        handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
    }

    private void cancelPendingLongPress() {
        if (longPressRunnable != null) handler.removeCallbacks(longPressRunnable);
        longPressRunnable = null;
    }

    private void cancelSingleGestureForMultiTouch() {
        wallDraft = null;
        draggingPointId = null;
        diagDraft = null;
        draggingLabel = null;
        activeGuide = null;
        point90SnapActive = false;
        magnifierFocus = null;
        magnifierKind = null;
    }

    private void cancelTransientGestures() {
        cancelSingleGestureForMultiTouch();
        longPressFired = false;
        moved = false;
    }

    @Nullable
    private NativeProjectStore.PointData nearestPoint(float x, float y, float radius) {
        NativeProjectStore.PointData best = null;
        float bestDistance = radius;
        for (NativeProjectStore.PointData point : room.points) {
            float distance = (float) Math.hypot(x - point.x, y - point.y);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = point;
            }
        }
        return best;
    }

    @Nullable
    private NativeProjectStore.EdgeData nearestEdge(float x, float y, float radius) {
        NativeProjectStore.EdgeData best = null;
        float bestDistance = radius;
        for (NativeProjectStore.EdgeData edge : room.edges) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
            if (a == null || b == null) continue;
            float distance = distancePointToSegment(x, y, a.x, a.y, b.x, b.y);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = edge;
            }
        }
        return best;
    }

    @Nullable
    private NativeProjectStore.DiagonalData nearestDiagonal(float x, float y, float radius) {
        NativeProjectStore.DiagonalData best = null;
        float bestDistance = radius;
        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, diagonal.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, diagonal.b);
            if (a == null || b == null) continue;
            float distance = distancePointToSegment(x, y, a.x, a.y, b.x, b.y);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = diagonal;
            }
        }
        return best;
    }

    @Nullable
    private LabelHit nearestLabel(float x, float y, float radius) {
        LabelHit best = null;
        float bestDistance = radius;

        for (NativeProjectStore.EdgeData edge : room.edges) {
            PointF base = edgeLabelBase(edge);
            if (base == null) continue;
            float lx = base.x + edge.labelDx;
            float ly = base.y + edge.labelDy;
            float distance = (float) Math.hypot(x - lx, y - ly);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = new LabelHit(LabelKind.EDGE, edge.id);
            }
        }

        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            if (diagonal.value <= 0) continue;
            PointF base = diagonalLabelBase(diagonal);
            if (base == null) continue;
            float lx = base.x + diagonal.labelDx;
            float ly = base.y + diagonal.labelDy;
            float distance = (float) Math.hypot(x - lx, y - ly);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = new LabelHit(LabelKind.DIAGONAL, diagonal.id);
            }
        }
        return best;
    }

    @Nullable
    private NativeProjectStore.PointData nearestOpenPointAlongSegment(
            PointF start,
            PointF end,
            float maxDistance) {
        NativeProjectStore.PointData best = null;
        float bestT = Float.MAX_VALUE;
        for (NativeProjectStore.PointData point : room.points) {
            if (point.id.equals(wallDraft.startId)
                    || NativeGeometry.vertexDegree(room, point.id) >= 2) continue;
            Projection projection = projection(point.x, point.y, start.x, start.y, end.x, end.y);
            if (projection == null) continue;
            if (projection.t <= .10f || projection.t > 1f || projection.distance > maxDistance) continue;
            if (projection.t < bestT) {
                bestT = projection.t;
                best = point;
            }
        }
        return best;
    }

    @Nullable
    private Guide wallStopGuide(PointF finish) {
        if (wallDraft == null) return null;
        PointF start = wallDraft.start;
        float vx = finish.x - start.x;
        float vy = finish.y - start.y;
        float len = (float) Math.hypot(vx, vy);
        if (len < 2f) return null;

        float ux = vx / len;
        float uy = vy / len;
        float nx = -uy;
        float ny = ux;
        Guide best = null;

        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (edge.a.equals(wallDraft.startId) || edge.b.equals(wallDraft.startId)) continue;
            NativeProjectStore.PointData p1 = NativeGeometry.pointById(room, edge.a);
            NativeProjectStore.PointData p2 = NativeGeometry.pointById(room, edge.b);
            if (p1 == null || p2 == null) continue;
            float ex = p2.x - p1.x;
            float ey = p2.y - p1.y;
            float el = (float) Math.hypot(ex, ey);
            if (el < 2) continue;
            float parallel = Math.abs((ex * ux + ey * uy) / el);
            if (parallel < .93f) continue;
            best = chooseGuide(best, p1, start, len, ux, uy, nx, ny, 0f);
            best = chooseGuide(best, p2, start, len, ux, uy, nx, ny, 0f);
        }

        for (NativeProjectStore.PointData point : room.points) {
            if (point.id.equals(wallDraft.startId)) continue;
            if (NativeGeometry.edgeBetween(room, wallDraft.startId, point.id) != null) continue;
            best = chooseGuide(best, point, start, len, ux, uy, nx, ny, 10f);
        }
        return best;
    }

    @Nullable
    private Guide chooseGuide(
            @Nullable Guide current,
            NativeProjectStore.PointData candidate,
            PointF start,
            float currentLength,
            float ux,
            float uy,
            float nx,
            float ny,
            float typePenalty) {
        float stopProjection = (candidate.x - start.x) * ux + (candidate.y - start.y) * uy;
        if (stopProjection <= 1f) return current;

        PointF projected = new PointF(
                start.x + ux * stopProjection,
                start.y + uy * stopProjection);
        float offset = Math.abs((candidate.x - start.x) * nx + (candidate.y - start.y) * ny);
        float distanceToCurrent = Math.abs(stopProjection - currentLength);
        float score = offset * 2.2f + distanceToCurrent + typePenalty;
        if (current == null || score < current.score) {
            return new Guide(projected, ux, uy, score);
        }
        return current;
    }

    @Nullable
    private PointF nearest90Candidate(NativeProjectStore.PointData moving) {
        List<NativeProjectStore.EdgeData> edges = NativeGeometry.edgesAt(room, moving.id);
        if (edges.size() != 2) return null;

        NativeProjectStore.PointData a = otherPoint(edges.get(0), moving.id);
        NativeProjectStore.PointData b = otherPoint(edges.get(1), moving.id);
        if (a == null || b == null) return null;

        PointF[] candidates = new PointF[]{
                new PointF(a.x, b.y),
                new PointF(b.x, a.y)
        };
        PointF best = null;
        float bestDistance = Float.MAX_VALUE;
        for (PointF candidate : candidates) {
            float distance = (float) Math.hypot(
                    moving.x - candidate.x,
                    moving.y - candidate.y);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return bestDistance <= dp(30) / Math.max(.45f, scale) ? best : null;
    }

    private boolean isRightAngle(NativeProjectStore.PointData moving) {
        List<NativeProjectStore.EdgeData> edges = NativeGeometry.edgesAt(room, moving.id);
        if (edges.size() != 2) return false;
        NativeProjectStore.PointData a = otherPoint(edges.get(0), moving.id);
        NativeProjectStore.PointData b = otherPoint(edges.get(1), moving.id);
        if (a == null || b == null) return false;

        float ax = a.x - moving.x;
        float ay = a.y - moving.y;
        float bx = b.x - moving.x;
        float by = b.y - moving.y;
        float la = (float) Math.hypot(ax, ay);
        float lb = (float) Math.hypot(bx, by);
        if (la < 1 || lb < 1) return false;

        float dot = (ax * bx + ay * by) / (la * lb);
        dot = clamp(dot, -1f, 1f);
        float angle = (float) (Math.acos(dot) * 180d / Math.PI);
        return Math.abs(angle - 90f) <= .6f;
    }

    @Nullable
    private NativeProjectStore.PointData otherPoint(
            NativeProjectStore.EdgeData edge,
            String pointId) {
        String otherId = edge.a.equals(pointId) ? edge.b : edge.a;
        return NativeGeometry.pointById(room, otherId);
    }

    @Nullable
    private PointF edgeLabelBase(@Nullable NativeProjectStore.EdgeData edge) {
        if (edge == null) return null;
        NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
        NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
        if (a == null || b == null) return null;

        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float length = (float) Math.hypot(dx, dy);
        if (length < 1) return new PointF(a.x, a.y);

        float nx = -dy / length;
        float ny = dx / length;
        float mx = (a.x + b.x) / 2f;
        float my = (a.y + b.y) / 2f;
        float offset = dp(24) / scale;

        PointF first = new PointF(mx + nx * offset, my + ny * offset);
        PointF second = new PointF(mx - nx * offset, my - ny * offset);
        List<String> cycle = NativeGeometry.cycleOrder(room);
        if (cycle != null) {
            boolean firstInside = pointInPolygon(first, cycle);
            boolean secondInside = pointInPolygon(second, cycle);
            if (firstInside != secondInside) return firstInside ? second : first;
        }

        PointF center = drawingCenter();
        float d1 = sq(first.x - center.x) + sq(first.y - center.y);
        float d2 = sq(second.x - center.x) + sq(second.y - center.y);
        return d1 >= d2 ? first : second;
    }

    @Nullable
    private PointF diagonalLabelBase(@Nullable NativeProjectStore.DiagonalData diagonal) {
        if (diagonal == null) return null;
        NativeProjectStore.PointData a = NativeGeometry.pointById(room, diagonal.a);
        NativeProjectStore.PointData b = NativeGeometry.pointById(room, diagonal.b);
        if (a == null || b == null) return null;
        return new PointF((a.x + b.x) / 2f, (a.y + b.y) / 2f);
    }

    private PointF drawingCenter() {
        if (room.points.isEmpty()) return new PointF(0, 0);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (NativeProjectStore.PointData p : room.points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        return new PointF((minX + maxX) / 2f, (minY + maxY) / 2f);
    }

    private boolean pointInPolygon(PointF point, List<String> order) {
        boolean inside = false;
        for (int i = 0, j = order.size() - 1; i < order.size(); j = i++) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, order.get(i));
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, order.get(j));
            if (a == null || b == null) continue;
            boolean hit = ((a.y > point.y) != (b.y > point.y))
                    && point.x < (b.x - a.x) * (point.y - a.y)
                    / ((b.y - a.y) == 0 ? 1e-9f : (b.y - a.y)) + a.x;
            if (hit) inside = !inside;
        }
        return inside;
    }

    private String pointLetterLabel(String pointId) {
        NativeProjectStore.PointData point = NativeGeometry.pointById(room, pointId);
        if (point != null && "A".equals(point.customLabel)) return "A";
        int index = NativeGeometry.pointIndex(room, pointId);
        if (index >= 0 && index < 26) return String.valueOf((char) ('A' + index));
        return index >= 0 ? String.valueOf(index + 1) : "";
    }

    private String edgeLetterLabel(NativeProjectStore.EdgeData edge) {
        return pointLetterLabel(edge.a) + pointLetterLabel(edge.b);
    }

    private String edgeOrdinal(NativeProjectStore.EdgeData edge) {
        List<NativeProjectStore.EdgeData> ordered = NativeGeometry.orderedEdges(room);
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).id.equals(edge.id)) return String.valueOf(i + 1);
        }
        return "";
    }

    @Nullable
    private NativeProjectStore.EdgeData edgeById(String id) {
        for (NativeProjectStore.EdgeData edge : room.edges) if (edge.id.equals(id)) return edge;
        return null;
    }

    @Nullable
    private NativeProjectStore.DiagonalData diagonalById(String id) {
        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            if (diagonal.id.equals(id)) return diagonal;
        }
        return null;
    }

    private void drawMagnifierOverlay(Canvas canvas) {
        if (room == null || magnifierFocus == null || magnifierKind == null) return;

        float width = dp(180);
        float height = dp(108);
        float margin = dp(10);
        float left = Math.max(margin, getWidth() - width - margin);
        float top = margin;
        RectF box = new RectF(left, top, left + width, top + height);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.rgb(252, 253, 254));
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.rgb(191, 204, 213));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1));

        canvas.save();
        Path clip = new Path();
        clip.addRoundRect(box, dp(8), dp(8), Path.Direction.CW);
        canvas.clipPath(clip);
        canvas.drawRoundRect(box, dp(8), dp(8), bg);

        float lensScale = clamp(scale * 2.8f, 2f, 7.1f);
        float cx = box.centerX();
        float cy = box.centerY();

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(lensScale, lensScale);
        canvas.translate(-magnifierFocus.x, -magnifierFocus.y);

        Paint fineGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        fineGrid.setColor(Color.rgb(233, 238, 242));
        fineGrid.setStrokeWidth(dp(.7f) / lensScale);
        float halfW = width / 2f / lensScale + 20f;
        float halfH = height / 2f / lensScale + 20f;
        float minX = magnifierFocus.x - halfW;
        float maxX = magnifierFocus.x + halfW;
        float minY = magnifierFocus.y - halfH;
        float maxY = magnifierFocus.y + halfH;
        float step = 5f;
        for (float x = (float) Math.floor(minX / step) * step; x <= maxX; x += step) {
            canvas.drawLine(x, minY, x, maxY, fineGrid);
        }
        for (float y = (float) Math.floor(minY / step) * step; y <= maxY; y += step) {
            canvas.drawLine(minX, y, maxX, y, fineGrid);
        }

        Paint edgePaint = new Paint(linePaint);
        edgePaint.setStrokeWidth(dp(2.8f) / lensScale);
        for (NativeProjectStore.EdgeData edge : room.edges) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
            if (a != null && b != null) canvas.drawLine(a.x, a.y, b.x, b.y, edgePaint);
        }

        if (magnifierKind == MagnifierKind.WALL && wallDraft != null) {
            Paint p = new Paint(previewPaint);
            p.setStrokeWidth(dp(3f) / lensScale);
            canvas.drawLine(
                    wallDraft.start.x,
                    wallDraft.start.y,
                    wallDraft.preview.x,
                    wallDraft.preview.y,
                    p);
            if (activeGuide != null) {
                Paint gp = new Paint(guidePaint);
                gp.setStrokeWidth(dp(1.3f) / lensScale);
                float half = 70f / lensScale;
                float nx = -activeGuide.uy;
                float ny = activeGuide.ux;
                canvas.drawLine(
                        activeGuide.projected.x - nx * half,
                        activeGuide.projected.y - ny * half,
                        activeGuide.projected.x + nx * half,
                        activeGuide.projected.y + ny * half,
                        gp);
            }
        }

        float pointRadius = dp(3.5f) / lensScale;
        Paint pp = new Paint(Paint.ANTI_ALIAS_FLAG);
        pp.setColor(BLUE);
        for (NativeProjectStore.PointData point : room.points) {
            canvas.drawCircle(point.x, point.y, pointRadius, pp);
        }

        canvas.restore();

        int cross = magnifierKind == MagnifierKind.POINT && point90SnapActive
                ? GREEN
                : BLUE;
        Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crossPaint.setColor(cross);
        crossPaint.setStrokeWidth(dp(1.2f));
        canvas.drawLine(cx - dp(9), cy, cx + dp(9), cy, crossPaint);
        canvas.drawLine(cx, cy - dp(9), cx, cy + dp(9), crossPaint);
        canvas.drawCircle(cx, cy, dp(2.8f), crossPaint);

        canvas.restore();
        canvas.drawRoundRect(box, dp(8), dp(8), border);
    }

    boolean isMagnifierVisibleForTest() {
        return magnifierFocus != null && magnifierKind != null;
    }

    private void changed() {
        syncViewToRoom();
        invalidate();
        if (listener != null) listener.onChanged();
    }

    private void syncViewToRoom() {
        if (room == null) return;
        room.viewScale = scale;
        room.panX = offsetX;
        room.panY = offsetY;
    }

    private float roundGrid(float value) {
        float step = scale >= 8f ? 2f : (scale >= 4f ? 5f : 10f);
        return Math.round(value / step) * step;
    }

    private float roundPrecisionGrid(float value) {
        float step = scale >= 8f ? .5f : (scale >= 4f ? 1f : 2f);
        return Math.round(value / step) * step;
    }

    private PointF snapAngle(PointF start, PointF point) {
        float dx = point.x - start.x;
        float dy = point.y - start.y;
        float distance = (float) Math.hypot(dx, dy);
        if (distance < 1f) return point;
        double step = Math.PI / 4d;
        double angle = Math.atan2(dy, dx);
        double near = Math.round(angle / step) * step;
        double diff = Math.abs(angle - near);
        if (diff < .24d) {
            return new PointF(
                    start.x + (float) Math.cos(near) * distance,
                    start.y + (float) Math.sin(near) * distance);
        }
        return point;
    }

    private float screenToWorldX(float x) {
        return (x - offsetX) / scale;
    }

    private float screenToWorldY(float y) {
        return (y - offsetY) / scale;
    }

    private float centroidX(MotionEvent event) {
        float sum = 0;
        for (int i = 0; i < event.getPointerCount(); i++) sum += event.getX(i);
        return sum / event.getPointerCount();
    }

    private float centroidY(MotionEvent event) {
        float sum = 0;
        for (int i = 0; i < event.getPointerCount(); i++) sum += event.getY(i);
        return sum / event.getPointerCount();
    }

    private String formatValue(float value) {
        return Math.abs(value - Math.round(value)) < .01f
                ? String.valueOf(Math.round(value))
                : String.format(Locale.US, "%.1f", value);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float sq(float value) {
        return value * value;
    }

    private static float distancePointToSegment(
            float px,
            float py,
            float ax,
            float ay,
            float bx,
            float by) {
        Projection projection = projection(px, py, ax, ay, bx, by);
        return projection == null ? Float.MAX_VALUE : projection.distance;
    }

    @Nullable
    private static Projection projection(
            float px,
            float py,
            float ax,
            float ay,
            float bx,
            float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float length2 = dx * dx + dy * dy;
        if (length2 < 1e-9f) return null;
        float t = ((px - ax) * dx + (py - ay) * dy) / length2;
        float clamped = Math.max(0f, Math.min(1f, t));
        float cx = ax + clamped * dx;
        float cy = ay + clamped * dy;
        return new Projection(
                t,
                (float) Math.hypot(px - cx, py - cy));
    }

    private static final class Projection {
        final float t;
        final float distance;

        Projection(float t, float distance) {
            this.t = t;
            this.distance = distance;
        }
    }

    private static final class WallDraft {
        final String startId;
        final PointF start;
        final PointF preview;
        final PointF fingerStart;
        final boolean newStart;

        WallDraft(
                String startId,
                PointF start,
                PointF preview,
                PointF fingerStart,
                boolean newStart) {
            this.startId = startId;
            this.start = start;
            this.preview = preview;
            this.fingerStart = fingerStart;
            this.newStart = newStart;
        }
    }

    private static final class DiagDraft {
        final String startId;
        final PointF start;
        final PointF preview;

        DiagDraft(String startId, PointF start, PointF preview) {
            this.startId = startId;
            this.start = start;
            this.preview = preview;
        }
    }

    private static final class Guide {
        final PointF projected;
        final float ux;
        final float uy;
        final float score;

        Guide(PointF projected, float ux, float uy, float score) {
            this.projected = projected;
            this.ux = ux;
            this.uy = uy;
            this.score = score;
        }
    }

    private enum MagnifierKind { WALL, POINT }

    private enum LabelKind { EDGE, DIAGONAL }

    private static final class LabelHit {
        final LabelKind kind;
        final String id;

        LabelHit(LabelKind kind, String id) {
            this.kind = kind;
            this.id = id;
        }
    }
}
