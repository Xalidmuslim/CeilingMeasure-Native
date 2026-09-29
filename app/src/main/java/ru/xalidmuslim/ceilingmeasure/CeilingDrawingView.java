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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ScrollView;

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
        void onHistoryCheckpoint();
        void onVertexLongPressed(@NonNull String pointId);
        void onWallLongPressed(@NonNull String edgeId, float currentValue);
        void onWallSequencePick(@NonNull String edgeId);
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
    private final Handler handler = new Handler(Looper.getMainLooper());

    private NativeProjectStore.RoomData room;
    private Mode mode = Mode.WALL;
    private Listener listener;

    private float scale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private boolean multiTouch;
    private boolean pinchActive;
    private float pinchStartDistance;
    private float pinchStartScale;
    private float pinchWorldX;
    private float pinchWorldY;

    private boolean pageScrollCandidate;
    private boolean pageScrollActive;
    private float pageScrollLastY;
    private boolean wallSequenceActive;
    private String wallSequenceTapEdgeId;

    private float downScreenX;
    private float downScreenY;
    private boolean moved;
    private boolean longPressFired;
    private Runnable longPressRunnable;

    private WallDraft wallDraft;
    private String draggingPointId;
    private DiagDraft diagDraft;
    private LabelHit draggingLabel;
    private LabelHit rotatingLabel;
    private LabelHit selectedLabel;
    private float labelTouchStartX;
    private float labelTouchStartY;
    private float labelStartDx;
    private float labelStartDy;
    private float labelRotateCenterX;
    private float labelRotateCenterY;
    private Guide activeGuide;
    private boolean redGuideSnapActive;
    private boolean point90SnapActive;
    private String editingEdgeId;
    private String lastWallCornerSnapId;
    private String lastDiagonalSnapId;
    private PointF magnifierFocus;
    private MagnifierKind magnifierKind;

    CeilingDrawingView(@NonNull Context context) {
        super(context);
        setBackgroundColor(Color.rgb(252, 253, 254));
        setFocusable(true);

        linePaint.setColor(TEXT);
        linePaint.setStrokeWidth(dp(1.65f));
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
        previewPaint.setStrokeWidth(dp(1.9f));
        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeCap(Paint.Cap.ROUND);

        guidePaint.setColor(RED);
        guidePaint.setStrokeWidth(dp(1.15f));
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(5)}, 0));


    }

    void setRoom(@NonNull NativeProjectStore.RoomData room) {
        cancelPendingLongPress();
        cancelTransientGestures();
        this.room = room;
        NativeGeometry.sanitize(room);
        scale = clamp(room.viewScale, .45f, 12f);
        offsetX = room.panX;
        offsetY = room.panY;
        postInvalidateOnAnimation();
    }

    void setMode(@NonNull Mode mode) {
        cancelPendingLongPress();
        cancelTransientGestures();
        this.mode = mode;
        if (mode != Mode.LABEL) selectedLabel = null;
        postInvalidateOnAnimation();
    }

    Mode getMode() {
        return mode;
    }

    void setWallSequenceActive(boolean active) {
        wallSequenceActive = active;
        wallSequenceTapEdgeId = null;
    }

    void setEditingEdgeId(@Nullable String edgeId) {
        editingEdgeId = edgeId;
        postInvalidateOnAnimation();
    }

    @Nullable
    String getEditingEdgeIdForTest() {
        return editingEdgeId;
    }

    void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    void applySolvedGeometry(@NonNull Map<String, PointF> positions) {
        if (room == null || positions.isEmpty()) return;
        List<String> order = NativeGeometry.cycleOrder(room);
        if (order == null || order.size() < 2) return;

        NativeProjectStore.PointData worldA =
                NativeGeometry.pointById(room, order.get(0));
        NativeProjectStore.PointData worldB =
                NativeGeometry.pointById(room, order.get(1));
        PointF solvedA = positions.get(order.get(0));
        PointF solvedB = positions.get(order.get(1));
        if (worldA == null || worldB == null || solvedA == null || solvedB == null) return;

        float sourceDx = solvedB.x - solvedA.x;
        float sourceDy = solvedB.y - solvedA.y;
        float sourceLength = (float) Math.hypot(sourceDx, sourceDy);
        float targetDx = worldB.x - worldA.x;
        float targetDy = worldB.y - worldA.y;
        float targetLength = (float) Math.hypot(targetDx, targetDy);
        if (sourceLength < 1e-6f || targetLength < 1e-6f) return;

        List<SegmentLabelAnchor> labelAnchors = captureManualLabelAnchors();

        // Keep the existing sketch's first wall as the visual anchor. This changes
        // only the room geometry; the user's current zoom and pan are untouched.
        float cos = (sourceDx * targetDx + sourceDy * targetDy)
                / (sourceLength * targetLength);
        float sin = (sourceDx * targetDy - sourceDy * targetDx)
                / (sourceLength * targetLength);
        float worldPerMeasuredUnit = targetLength / sourceLength;
        float anchorX = worldA.x;
        float anchorY = worldA.y;

        for (NativeProjectStore.PointData point : room.points) {
            PointF solved = positions.get(point.id);
            if (solved == null) continue;
            float dx = solved.x - solvedA.x;
            float dy = solved.y - solvedA.y;
            float rx = dx * cos - dy * sin;
            float ry = dx * sin + dy * cos;
            point.x = anchorX + rx * worldPerMeasuredUnit;
            point.y = anchorY + ry * worldPerMeasuredUnit;
        }

        restoreManualLabelAnchors(labelAnchors);
        cancelTransientGestures();
        changed();
    }

    private static final class SegmentLabelAnchor {
        final boolean diagonal;
        final String id;
        final float t;
        final float normalOffset;

        SegmentLabelAnchor(boolean diagonal, String id, float t, float normalOffset) {
            this.diagonal = diagonal;
            this.id = id;
            this.t = t;
            this.normalOffset = normalOffset;
        }
    }

    private List<SegmentLabelAnchor> captureManualLabelAnchors() {
        ArrayList<SegmentLabelAnchor> result = new ArrayList<>();
        for (NativeProjectStore.EdgeData edge : room.edges) {
            if (!edge.labelManual || edge.labelX == null || edge.labelY == null) continue;
            SegmentLabelAnchor anchor = makeLabelAnchor(
                    false, edge.id, edge.a, edge.b, edge.labelX, edge.labelY);
            if (anchor != null) result.add(anchor);
        }
        for (NativeProjectStore.DiagonalData diagonal : room.diagonals) {
            if (!diagonal.labelManual
                    || diagonal.labelX == null
                    || diagonal.labelY == null) continue;
            SegmentLabelAnchor anchor = makeLabelAnchor(
                    true,
                    diagonal.id,
                    diagonal.a,
                    diagonal.b,
                    diagonal.labelX,
                    diagonal.labelY);
            if (anchor != null) result.add(anchor);
        }
        return result;
    }

    @Nullable
    private SegmentLabelAnchor makeLabelAnchor(
            boolean diagonal,
            String id,
            String aId,
            String bId,
            float labelX,
            float labelY) {
        NativeProjectStore.PointData a = NativeGeometry.pointById(room, aId);
        NativeProjectStore.PointData b = NativeGeometry.pointById(room, bId);
        if (a == null || b == null) return null;
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float length2 = dx * dx + dy * dy;
        float length = (float) Math.sqrt(length2);
        if (length < 1e-6f) return null;

        float lx = labelX - a.x;
        float ly = labelY - a.y;
        float t = (lx * dx + ly * dy) / length2;
        float normalOffset = lx * (-dy / length) + ly * (dx / length);
        return new SegmentLabelAnchor(diagonal, id, t, normalOffset);
    }

    private void restoreManualLabelAnchors(List<SegmentLabelAnchor> anchors) {
        for (SegmentLabelAnchor anchor : anchors) {
            String aId;
            String bId;
            if (anchor.diagonal) {
                NativeProjectStore.DiagonalData diagonal = diagonalById(anchor.id);
                if (diagonal == null) continue;
                aId = diagonal.a;
                bId = diagonal.b;
            } else {
                NativeProjectStore.EdgeData edge = edgeById(anchor.id);
                if (edge == null) continue;
                aId = edge.a;
                bId = edge.b;
            }

            NativeProjectStore.PointData a = NativeGeometry.pointById(room, aId);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, bId);
            if (a == null || b == null) continue;
            float dx = b.x - a.x;
            float dy = b.y - a.y;
            float length = (float) Math.hypot(dx, dy);
            if (length < 1e-6f) continue;
            float nx = -dy / length;
            float ny = dx / length;
            float x = a.x + anchor.t * dx + anchor.normalOffset * nx;
            float y = a.y + anchor.t * dy + anchor.normalOffset * ny;

            if (anchor.diagonal) {
                NativeProjectStore.DiagonalData diagonal = diagonalById(anchor.id);
                if (diagonal != null) {
                    diagonal.labelX = x;
                    diagonal.labelY = y;
                    diagonal.labelDx = 0f;
                    diagonal.labelDy = 0f;
                }
            } else {
                NativeProjectStore.EdgeData edge = edgeById(anchor.id);
                if (edge != null) {
                    edge.labelX = x;
                    edge.labelY = y;
                    edge.labelDx = 0f;
                    edge.labelDy = 0f;
                }
            }
        }
    }

    boolean hasClosedContour() {
        return room != null && NativeGeometry.isClosed(room);
    }

    boolean restoreSketchGeometryPreservingLabels() {
        if (room == null) return false;
        List<SegmentLabelAnchor> labelAnchors = captureManualLabelAnchors();
        if (!NativeGeometry.restoreSketchGeometry(room)) return false;
        restoreManualLabelAnchors(labelAnchors);
        cancelTransientGestures();
        changed();
        return true;
    }

    void clearDrawing() {
        if (room == null) return;
        checkpoint();
        cancelPendingLongPress();
        cancelTransientGestures();
        room.points.clear();
        room.edges.clear();
        room.diagonals.clear();
        NativeGeometry.discardFactualGeometrySession(room);
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
        // Stroke widths are screen-space constants. Previously linePaint was
        // scaled by Canvas, so zooming to 200–300% made walls look several
        // times thicker than at 100%.
        Paint normalPaint = new Paint(linePaint);
        normalPaint.setStrokeWidth(dp(1.65f) / Math.max(.45f, scale));

        Paint activePaint = new Paint(normalPaint);
        activePaint.setColor(BLUE);
        activePaint.setStrokeWidth(dp(2.45f) / Math.max(.45f, scale));

        for (NativeProjectStore.EdgeData edge : room.edges) {
            NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
            NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
            if (a == null || b == null) continue;

            Paint paint = edge.id.equals(editingEdgeId)
                    ? activePaint
                    : normalPaint;
            canvas.drawLine(a.x, a.y, b.x, b.y, paint);
        }
    }

    private void drawDiagonals(Canvas canvas) {
        Paint paint = new Paint(linePaint);
        paint.setColor(Color.rgb(112, 130, 144));
        paint.setStrokeWidth(dp(1.15f) / Math.max(.45f, scale));
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
        Paint wallPreview = new Paint(previewPaint);
        wallPreview.setStrokeWidth(dp(1.9f) / Math.max(.45f, scale));
        canvas.drawLine(
                wallDraft.start.x,
                wallDraft.start.y,
                wallDraft.preview.x,
                wallDraft.preview.y,
                wallPreview);

        if (activeGuide != null) {
            float half = 1000f / Math.max(.45f, scale);
            float nx = -activeGuide.uy;
            float ny = activeGuide.ux;
            Paint guide = new Paint(guidePaint);
            guide.setStrokeWidth(dp(1.15f) / Math.max(.45f, scale));
            guide.setPathEffect(new DashPathEffect(
                    new float[]{
                            dp(6) / Math.max(.45f, scale),
                            dp(5) / Math.max(.45f, scale)},
                    0));
            canvas.drawLine(
                    activeGuide.projected.x - nx * half,
                    activeGuide.projected.y - ny * half,
                    activeGuide.projected.x + nx * half,
                    activeGuide.projected.y + ny * half,
                    guide);
        }
    }

    private void drawPoints(Canvas canvas) {
        float radius = dp(4.7f) / Math.max(.45f, scale);
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
        List<NativeLabelEngine.Placement> placements = labelPlacements();
        for (NativeLabelEngine.Placement placement : placements) {
            drawStyledLabel(canvas, placement);
            if (mode == Mode.LABEL
                    && selectedLabel != null
                    && selectedLabel.matches(placement)) {
                drawLabelSelection(canvas, placement);
            }
        }
    }

    private List<NativeLabelEngine.Placement> labelPlacements() {
        float density = getResources().getDisplayMetrics().density;
        return NativeLabelEngine.layout(room, scale, density);
    }

    private void drawStyledLabel(
            Canvas canvas,
            NativeLabelEngine.Placement placement) {
        Paint fill = new Paint(textPaint);
        fill.setColor(TEXT);
        fill.setTextSize(dp(placement.fontDp) * placement.size / scale);
        fill.setStyle(Paint.Style.FILL);
        fill.setStrokeWidth(0f);

        Paint outline = new Paint(fill);
        outline.setColor(Color.WHITE);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(dp(4f) / scale);
        outline.setStrokeJoin(Paint.Join.ROUND);

        Paint.FontMetrics fm = fill.getFontMetrics();
        float baseline = -(fm.ascent + fm.descent) / 2f;

        canvas.save();
        canvas.translate(placement.x, placement.y);
        canvas.rotate(placement.angle);
        canvas.drawText(placement.text, 0f, baseline, outline);
        canvas.drawText(placement.text, 0f, baseline, fill);
        canvas.restore();
    }

    private void drawLabelSelection(
            Canvas canvas,
            NativeLabelEngine.Placement placement) {
        float width = placement.width * placement.size;
        float height = placement.height * placement.size;
        float radius = labelSelectionRadius(placement);
        float controlY = radius + dp(26f) / scale;
        float controlX = dp(48f) / scale;

        Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
        guide.setColor(BLUE);
        guide.setStyle(Paint.Style.STROKE);
        guide.setStrokeWidth(dp(1.4f) / scale);
        guide.setPathEffect(new DashPathEffect(
                new float[]{dp(5f) / scale, dp(4f) / scale},
                0));

        Paint soft = new Paint(guide);
        soft.setColor(Color.rgb(144, 191, 215));
        soft.setStrokeWidth(dp(1f) / scale);
        soft.setPathEffect(new DashPathEffect(
                new float[]{dp(3f) / scale, dp(5f) / scale},
                0));

        canvas.save();
        canvas.translate(placement.x, placement.y);
        canvas.rotate(placement.angle);

        RectF rect = new RectF(
                -width / 2f - dp(8f) / scale,
                -height / 2f - dp(6f) / scale,
                width / 2f + dp(8f) / scale,
                height / 2f + dp(6f) / scale);
        canvas.drawRoundRect(rect, dp(8f) / scale, dp(8f) / scale, guide);
        canvas.drawCircle(0f, 0f, radius, soft);

        Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
        white.setColor(Color.WHITE);
        Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);
        handle.setColor(BLUE);
        handle.setStyle(Paint.Style.STROKE);
        handle.setStrokeWidth(dp(2f) / scale);

        float handleRadius = dp(11f) / scale;
        canvas.drawCircle(0f, -radius, handleRadius, white);
        canvas.drawCircle(0f, -radius, handleRadius, handle);

        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(BLUE);
        canvas.drawCircle(0f, -radius, dp(4.5f) / scale, dot);

        drawLabelControl(canvas, -controlX, controlY, "−");
        drawLabelControl(canvas, 0f, controlY, Math.round(placement.size * 100f) + "%");
        drawLabelControl(canvas, controlX, controlY, "+");

        Paint readout = new Paint(textPaint);
        readout.setTextSize(dp(11f) / scale);
        readout.setColor(Color.rgb(71, 113, 135));
        Paint.FontMetrics rfm = readout.getFontMetrics();
        float rb = -radius - dp(20f) / scale - (rfm.ascent + rfm.descent) / 2f;
        canvas.drawText(Math.round(placement.angle) + "°", 0f, rb, readout);

        canvas.restore();
    }

    private void drawLabelControl(Canvas canvas, float x, float y, String text) {
        float r = dp(text.endsWith("%") ? 22f : 20f) / scale;
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.WHITE);
        canvas.drawCircle(x, y, r, bg);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.rgb(170, 200, 216));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1.2f) / scale);
        canvas.drawCircle(x, y, r, border);

        Paint tp = new Paint(textPaint);
        tp.setTextSize(dp(text.endsWith("%") ? 10f : 18f) / scale);
        tp.setColor(Color.rgb(56, 107, 132));
        Paint.FontMetrics fm = tp.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, x, baseline, tp);
    }

    private float labelSelectionRadius(NativeLabelEngine.Placement placement) {
        float maxScreenDp = Math.max(
                placement.width * placement.size * scale
                        / getResources().getDisplayMetrics().density,
                placement.height * placement.size * scale
                        / getResources().getDisplayMetrics().density);
        float radiusDp = Math.max(48f, Math.min(76f, maxScreenDp * .74f));
        return dp(radiusDp) / scale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (room == null) return false;

        final int action = event.getActionMasked();

        // Web v184 parity: two fingers always own the drawing gesture and
        // simultaneously pan + zoom around the world point under their centroid.
        if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() == 2) {
            cancelPendingLongPress();
            cancelSingleGestureForMultiTouch();
            beginPinch(event);
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }
        if (event.getPointerCount() >= 2 && action == MotionEvent.ACTION_MOVE) {
            if (!pinchActive) beginPinch(event);
            applyPinch(event);
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_UP && multiTouch) {
            // Do not turn the remaining finger into a drawing gesture. This
            // matches the web pointer-map contract: the gesture ends only when
            // the last finger is lifted.
            pinchActive = false;
            syncViewToRoom();
            postInvalidateOnAnimation();
            return true;
        }

        float worldX = screenToWorldX(event.getX());
        float worldY = screenToWorldY(event.getY());

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (multiTouch) return true;
                downScreenX = event.getX();
                downScreenY = event.getY();
                moved = false;
                longPressFired = false;
                activeGuide = null;
                redGuideSnapActive = false;
                pageScrollActive = false;
                pageScrollCandidate = false;

                // Editing measurements is mode-independent: holding a wall or
                // its numeric label must open the same editor in Wall, Point,
                // Diagonal and Numbers modes.
                scheduleMeasurementLongPressIfHit(worldX, worldY);

                if (mode == Mode.WALL) {
                    // Open contour keeps one-finger ownership for wall drawing.
                    // Once the contour is closed, vertical movement is handed
                    // back to the ancestor ScrollView so Android provides the
                    // exact same drag/fling/inertia as the rest of the page.
                    if (NativeGeometry.isClosed(room)) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return beginWallGesture(worldX, worldY);
                }

                if (mode == Mode.POINT) {
                    NativeProjectStore.PointData point =
                            nearestPoint(worldX, worldY, dp(48) / scale);
                    if (point == null) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    }
                    getParent().requestDisallowInterceptTouchEvent(true);
                    draggingPointId = point.id;
                    checkpoint();
                    scheduleLongPress("vertex", point.id);
                    return true;
                }

                if (mode == Mode.DIAGONAL) {
                    NativeProjectStore.PointData point =
                            nearestPoint(worldX, worldY, dp(48) / scale);
                    NativeProjectStore.DiagonalData diagonal =
                            nearestDiagonal(worldX, worldY, dp(20) / scale);
                    if (point == null && diagonal == null) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    }
                    if (point == null && diagonal != null && NativeGeometry.isClosed(room)) {
                        // Existing diagonal can still be long-pressed, but a
                        // vertical drag belongs to the page scroll.
                        getParent().requestDisallowInterceptTouchEvent(false);
                    } else {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return beginDiagonalGesture(worldX, worldY);
                }

                beginLabelGesture(worldX, worldY);
                if (draggingLabel == null && rotatingLabel == null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (multiTouch) return true;

                float dxScreen = event.getX() - downScreenX;
                float dyScreen = event.getY() - downScreenY;
                float distance = (float) Math.hypot(dxScreen, dyScreen);
                if (distance > dp(9)) {
                    moved = true;
                    cancelPendingLongPress();
                }

                if (pageScrollCandidate && !pageScrollActive) {
                    float threshold = mode == Mode.WALL ? dp(8) : dp(11);
                    float dominance = mode == Mode.WALL ? .72f : .78f;
                    if (Math.abs(dyScreen) > threshold
                            && Math.abs(dyScreen) >= Math.abs(dxScreen) * dominance) {
                        pageScrollActive = true;
                        cancelPendingLongPress();
                        longPressFired = false;
                        wallDraft = null;
                        diagDraft = null;
                        draggingPointId = null;
                        rotatingLabel = null;
                        draggingLabel = null;
                        activeGuide = null;
                        magnifierFocus = null;
                        magnifierKind = null;
                    }
                }

                if (pageScrollActive) {
                    ScrollView scroll = findAncestorScrollView();
                    if (scroll != null) {
                        // Incremental deltas avoid the jump caused by repeatedly
                        // recalculating from the original ACTION_DOWN position.
                        float currentY = event.getY();
                        int delta = Math.round(pageScrollLastY - currentY);
                        if (delta != 0) {
                            scroll.scrollBy(0, delta);
                            scroll.postInvalidateOnAnimation();
                        }
                        pageScrollLastY = currentY;
                    }
                    return true;
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
                if (mode == Mode.LABEL && rotatingLabel != null) {
                    updateLabelRotate(worldX, worldY);
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
                    pinchActive = false;
                    pageScrollCandidate = false;
                    pageScrollActive = false;
                    syncViewToRoom();
                    changed();
                    cancelTransientGestures();
                    return true;
                }

                if (pageScrollActive) {
                    pageScrollCandidate = false;
                    pageScrollActive = false;
                    cancelTransientGestures();
                    return true;
                }
                pageScrollCandidate = false;

                if (longPressFired) {
                    longPressFired = false;
                    cancelTransientGestures();
                    return true;
                }

                if (mode == Mode.WALL && wallSequenceActive && wallSequenceTapEdgeId != null) {
                    String picked = wallSequenceTapEdgeId;
                    wallSequenceTapEdgeId = null;
                    if (!moved && listener != null) listener.onWallSequencePick(picked);
                } else if (mode == Mode.WALL && wallDraft != null) {
                    finishWallDraft();
                } else if (mode == Mode.POINT && draggingPointId != null) {
                    finishPointDrag();
                } else if (mode == Mode.DIAGONAL && diagDraft != null) {
                    finishDiagonalDraft();
                } else if (mode == Mode.LABEL
                        && (draggingLabel != null || rotatingLabel != null)) {
                    draggingLabel = null;
                    rotatingLabel = null;
                    magnifierFocus = null;
                    magnifierKind = null;
                    changed();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                cancelPendingLongPress();
                multiTouch = false;
                pinchActive = false;
                pageScrollCandidate = false;
                pageScrollActive = false;
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
            // Do not disallow ancestor interception here. A stationary press
            // still reaches the wall/vertex editor, while a vertical drag is
            // intercepted by ScrollView and receives native fling physics.
            getParent().requestDisallowInterceptTouchEvent(false);
            if (nearest != null) {
                scheduleLongPress("vertex", nearest.id);
                return true;
            }
            if (edge != null) {
                if (wallSequenceActive) {
                    wallSequenceTapEdgeId = edge.id;
                } else {
                    scheduleLongPress("edge", edge.id);
                }
                return true;
            }
            return true;
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
        List<NativeLabelEngine.Placement> placements = labelPlacements();

        if (selectedLabel != null) {
            NativeLabelEngine.Placement selected = NativeLabelEngine.find(
                    placements,
                    selectedLabel.toEngineKind(),
                    selectedLabel.id);
            if (selected != null) {
                float[] local = toLabelLocal(selected, x, y);
                float radius = labelSelectionRadius(selected);
                float controlY = radius + dp(26f) / scale;
                float controlX = dp(48f) / scale;
                float hit = dp(25f) / scale;

                if (distance(local[0], local[1], -controlX, controlY) <= hit) {
                    checkpoint();
                    changeSelectedLabelSize(-.1f);
                    return;
                }
                if (distance(local[0], local[1], controlX, controlY) <= hit) {
                    checkpoint();
                    changeSelectedLabelSize(.1f);
                    return;
                }
                if (distance(local[0], local[1], 0f, controlY) <= hit) {
                    checkpoint();
                    resetSelectedLabelSize();
                    return;
                }

                float radial = (float) Math.hypot(local[0], local[1]);
                boolean onRing = Math.abs(radial - radius) <= dp(18f) / scale;
                boolean onHandle = distance(local[0], local[1], 0f, -radius) <= hit;
                if (onRing || onHandle) {
                    checkpoint();
                    rotatingLabel = selectedLabel;
                    draggingLabel = null;
                    labelRotateCenterX = selected.x;
                    labelRotateCenterY = selected.y;
                    return;
                }
            }
        }

        NativeLabelEngine.Placement placement = NativeLabelEngine.nearest(
                placements,
                x,
                y,
                dp(38f) / scale);
        if (placement == null) {
            selectedLabel = null;
            draggingLabel = null;
            rotatingLabel = null;
            postInvalidateOnAnimation();
            return;
        }

        checkpoint();
        selectedLabel = new LabelHit(
                placement.kind == NativeLabelEngine.Kind.EDGE
                        ? LabelKind.EDGE
                        : LabelKind.DIAGONAL,
                placement.id);
        draggingLabel = selectedLabel;
        rotatingLabel = null;
        labelTouchStartX = x;
        labelTouchStartY = y;

        if (selectedLabel.kind == LabelKind.EDGE) {
            NativeProjectStore.EdgeData edge = edgeById(selectedLabel.id);
            if (edge != null) {
                // Convert any legacy offset runtime state to canonical v184 absolute
                // coordinates at the current visual center before drag begins.
                edge.labelX = placement.x;
                edge.labelY = placement.y;
                edge.labelDx = 0f;
                edge.labelDy = 0f;
                labelStartDx = placement.x;
                labelStartDy = placement.y;
                edge.labelManual = true;
            }
        } else {
            NativeProjectStore.DiagonalData diagonal = diagonalById(selectedLabel.id);
            if (diagonal != null) {
                diagonal.labelX = placement.x;
                diagonal.labelY = placement.y;
                diagonal.labelDx = 0f;
                diagonal.labelDy = 0f;
                labelStartDx = placement.x;
                labelStartDy = placement.y;
                diagonal.labelManual = true;
            }
        }
        postInvalidateOnAnimation();
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
            redGuideSnapActive = false;
            if (!near.id.equals(lastWallCornerSnapId)) vibrateSnap();
            lastWallCornerSnapId = near.id;
        } else {
            lastWallCornerSnapId = null;
            Guide guide = wallStopGuide(finish);
            if (guide != null) {
                float screenDistance = (float) Math.hypot(
                        finish.x - guide.projected.x,
                        finish.y - guide.projected.y) * Math.max(.45f, scale);
                boolean snappedToRedGuide = screenDistance <= dp(26);
                if (snappedToRedGuide) {
                    finish.set(guide.projected.x, guide.projected.y);
                    if (!redGuideSnapActive) vibrateSnap();
                    redGuideSnapActive = true;
                } else {
                    redGuideSnapActive = false;
                }
                activeGuide = guide;
            } else {
                activeGuide = null;
                redGuideSnapActive = false;
            }
            wallDraft.preview.set(finish.x, finish.y);
        }
        magnifierFocus = new PointF(wallDraft.preview.x, wallDraft.preview.y);
        magnifierKind = MagnifierKind.WALL;
        postInvalidateOnAnimation();
    }

    private void finishWallDraft() {
        WallDraft draft = wallDraft;
        wallDraft = null;
        activeGuide = null;
        redGuideSnapActive = false;
        if (draft == null) return;

        float length = (float) Math.hypot(
                draft.preview.x - draft.start.x,
                draft.preview.y - draft.start.y);
        if (length <= dp(18) / scale) {
            postInvalidateOnAnimation();
            return;
        }

        if (NativeGeometry.isClosed(room)) {
            postInvalidateOnAnimation();
            return;
        }

        checkpoint();
        NativeProjectStore.PointData start = NativeGeometry.pointById(room, draft.startId);
        if (draft.newStart) {
            start = new NativeProjectStore.PointData(
                    draft.startId,
                    draft.start.x,
                    draft.start.y);
            room.points.add(start);
        }
        if (start == null || NativeGeometry.vertexDegree(room, start.id) >= 2) {
            postInvalidateOnAnimation();
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
        NativeGeometry.discardFactualGeometrySession(room);
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
            vibrateSnap();
        } else if (!right) {
            point90SnapActive = false;
        }
        postInvalidateOnAnimation();
    }

    private void finishPointDrag() {
        draggingPointId = null;
        point90SnapActive = false;
        magnifierFocus = null;
        magnifierKind = null;
        NativeGeometry.repairNearlyClosedContour(
                room,
                dp(24) / Math.max(.45f, scale));
        NativeGeometry.discardFactualGeometrySession(room);
        changed();
    }

    private void updateDiagonalDraft(float x, float y) {
        if (diagDraft == null) return;
        NativeProjectStore.PointData near = nearestPoint(x, y, dp(48) / scale);
        if (near != null && !near.id.equals(diagDraft.startId)) {
            diagDraft.preview.set(near.x, near.y);
            if (!near.id.equals(lastDiagonalSnapId)) vibrateSnap();
            lastDiagonalSnapId = near.id;
        } else {
            diagDraft.preview.set(x, y);
            lastDiagonalSnapId = null;
        }
        magnifierFocus = new PointF(diagDraft.preview.x, diagDraft.preview.y);
        magnifierKind = MagnifierKind.DIAGONAL;
        postInvalidateOnAnimation();
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
            postInvalidateOnAnimation();
            return;
        }
        if (NativeGeometry.edgeBetween(room, draft.startId, end.id) != null) {
            postInvalidateOnAnimation();
            return;
        }

        NativeProjectStore.DiagonalData diagonal =
                NativeGeometry.diagonalBetween(room, draft.startId, end.id);
        if (diagonal == null) {
            checkpoint();
            diagonal = new NativeProjectStore.DiagonalData(draft.startId, end.id);
            room.diagonals.add(diagonal);
            changed();
        }
        if (listener != null) listener.onDiagonalCreated(diagonal.id, diagonal.value);
    }

    private void updateLabelDrag(float x, float y) {
        if (draggingLabel == null) return;
        float dx = x - labelTouchStartX;
        float dy = y - labelTouchStartY;
        if (draggingLabel.kind == LabelKind.EDGE) {
            NativeProjectStore.EdgeData edge = edgeById(draggingLabel.id);
            if (edge != null) {
                edge.labelX = labelStartDx + dx;
                edge.labelY = labelStartDy + dy;
                edge.labelDx = 0f;
                edge.labelDy = 0f;
                edge.labelManual = true;
            }
        } else {
            NativeProjectStore.DiagonalData diagonal = diagonalById(draggingLabel.id);
            if (diagonal != null) {
                diagonal.labelX = labelStartDx + dx;
                diagonal.labelY = labelStartDy + dy;
                diagonal.labelDx = 0f;
                diagonal.labelDy = 0f;
                diagonal.labelManual = true;
            }
        }
        magnifierFocus = new PointF(x, y);
        magnifierKind = MagnifierKind.LABEL;
        postInvalidateOnAnimation();
    }

    private void updateLabelRotate(float x, float y) {
        if (rotatingLabel == null) return;
        float distance = (float) Math.hypot(
                x - labelRotateCenterX,
                y - labelRotateCenterY);
        if (distance <= dp(2f) / scale) return;

        float angle = NativeProjectStore.normalizeAngle(
                (float) Math.toDegrees(Math.atan2(
                        y - labelRotateCenterY,
                        x - labelRotateCenterX)) + 90f);
        if (rotatingLabel.kind == LabelKind.EDGE) {
            NativeProjectStore.EdgeData edge = edgeById(rotatingLabel.id);
            if (edge != null) {
                edge.labelAngle = angle;
                edge.labelManual = true;
            }
        } else {
            NativeProjectStore.DiagonalData diagonal = diagonalById(rotatingLabel.id);
            if (diagonal != null) {
                diagonal.labelAngle = angle;
                diagonal.labelManual = true;
            }
        }
        magnifierFocus = new PointF(x, y);
        magnifierKind = MagnifierKind.LABEL;
        postInvalidateOnAnimation();
    }

    private void changeSelectedLabelSize(float delta) {
        if (selectedLabel == null) return;
        if (selectedLabel.kind == LabelKind.EDGE) {
            NativeProjectStore.EdgeData edge = edgeById(selectedLabel.id);
            if (edge != null) {
                edge.labelSize = NativeProjectStore.normalizedLabelSize(edge.labelSize + delta);
                edge.labelManual = true;
            }
        } else {
            NativeProjectStore.DiagonalData diagonal = diagonalById(selectedLabel.id);
            if (diagonal != null) {
                diagonal.labelSize = NativeProjectStore.normalizedLabelSize(
                        diagonal.labelSize + delta);
                diagonal.labelManual = true;
            }
        }
        changed();
    }

    private void resetSelectedLabelSize() {
        if (selectedLabel == null) return;
        if (selectedLabel.kind == LabelKind.EDGE) {
            NativeProjectStore.EdgeData edge = edgeById(selectedLabel.id);
            if (edge != null) {
                edge.labelSize = 1f;
                edge.labelAngle = 0f;
            }
        } else {
            NativeProjectStore.DiagonalData diagonal = diagonalById(selectedLabel.id);
            if (diagonal != null) {
                diagonal.labelSize = 1f;
                diagonal.labelAngle = 0f;
            }
        }
        changed();
    }

    private float[] toLabelLocal(
            NativeLabelEngine.Placement placement,
            float worldX,
            float worldY) {
        float dx = worldX - placement.x;
        float dy = worldY - placement.y;
        double radians = Math.toRadians(-placement.angle);
        float lx = (float) (dx * Math.cos(radians) - dy * Math.sin(radians));
        float ly = (float) (dx * Math.sin(radians) + dy * Math.cos(radians));
        return new float[]{lx, ly};
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x1 - x2, y1 - y2);
    }

    private void scheduleMeasurementLongPressIfHit(float x, float y) {
        LabelHit label = nearestLabel(x, y, dp(42f) / scale);
        if (label != null) {
            scheduleLongPress(label.kind == LabelKind.EDGE ? "edge" : "diag", label.id);
            return;
        }

        NativeProjectStore.EdgeData edge = nearestEdge(x, y, dp(28f) / scale);
        NativeProjectStore.DiagonalData diagonal = nearestDiagonal(x, y, dp(20f) / scale);
        if (edge != null && diagonal != null) {
            float edgeDistance = distanceToEdge(x, y, edge);
            float diagonalDistance = distanceToDiagonal(x, y, diagonal);
            if (edgeDistance <= diagonalDistance) scheduleLongPress("edge", edge.id);
            else scheduleLongPress("diag", diagonal.id);
        } else if (edge != null) {
            scheduleLongPress("edge", edge.id);
        } else if (diagonal != null) {
            scheduleLongPress("diag", diagonal.id);
        }
    }

    private float distanceToEdge(float x, float y, NativeProjectStore.EdgeData edge) {
        NativeProjectStore.PointData a = NativeGeometry.pointById(room, edge.a);
        NativeProjectStore.PointData b = NativeGeometry.pointById(room, edge.b);
        if (a == null || b == null) return Float.MAX_VALUE;
        return distancePointToSegment(x, y, a.x, a.y, b.x, b.y);
    }

    private float distanceToDiagonal(float x, float y, NativeProjectStore.DiagonalData diagonal) {
        NativeProjectStore.PointData a = NativeGeometry.pointById(room, diagonal.a);
        NativeProjectStore.PointData b = NativeGeometry.pointById(room, diagonal.b);
        if (a == null || b == null) return Float.MAX_VALUE;
        return distancePointToSegment(x, y, a.x, a.y, b.x, b.y);
    }

    private void scheduleLongPress(String kind, String id) {
        cancelPendingLongPress();
        longPressRunnable = () -> {
            longPressFired = true;
            pageScrollCandidate = false;
            pageScrollActive = false;
            wallDraft = null;
            diagDraft = null;
            draggingPointId = null;
            draggingLabel = null;
            magnifierFocus = null;
            magnifierKind = null;
            postInvalidateOnAnimation();
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
        wallSequenceTapEdgeId = null;
        draggingPointId = null;
        diagDraft = null;
        draggingLabel = null;
        rotatingLabel = null;
        activeGuide = null;
        redGuideSnapActive = false;
        point90SnapActive = false;
        magnifierFocus = null;
        magnifierKind = null;
        lastWallCornerSnapId = null;
        lastDiagonalSnapId = null;
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
        NativeLabelEngine.Placement placement = NativeLabelEngine.nearest(
                labelPlacements(),
                x,
                y,
                radius);
        if (placement == null) return null;
        return new LabelHit(
                placement.kind == NativeLabelEngine.Kind.EDGE
                        ? LabelKind.EDGE
                        : LabelKind.DIAGONAL,
                placement.id);
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

        if (magnifierKind == MagnifierKind.DIAGONAL && diagDraft != null) {
            Paint p = new Paint(previewPaint);
            p.setStrokeWidth(dp(2.5f) / lensScale);
            canvas.drawLine(
                    diagDraft.start.x,
                    diagDraft.start.y,
                    diagDraft.preview.x,
                    diagDraft.preview.y,
                    p);
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

    List<NativeLabelEngine.Placement> labelPlacementsForTest() {
        return labelPlacements();
    }

    private void checkpoint() {
        if (listener != null) listener.onHistoryCheckpoint();
    }

    private void vibrateSnap() {
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        28L,
                        140));
            } else {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
        } catch (Throwable ignored) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    void zoomBy(float factor) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float old = scale;
        scale = clamp(scale * factor, .45f, 12f);
        if (old != 0f) {
            offsetX = cx - (cx - offsetX) * (scale / old);
            offsetY = cy - (cy - offsetY) * (scale / old);
        }
        syncViewToRoom();
        postInvalidateOnAnimation();
        if (listener != null) listener.onChanged();
    }

    void fitView() {
        if (room == null || room.points.isEmpty()) {
            scale = 1f;
            offsetX = 0f;
            offsetY = 0f;
        } else {
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (NativeProjectStore.PointData p : room.points) {
                minX = Math.min(minX, p.x);
                maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y);
                maxY = Math.max(maxY, p.y);
            }
            float bw = Math.max(1f, maxX - minX);
            float bh = Math.max(1f, maxY - minY);
            scale = clamp(Math.min(
                    (getWidth() - dp(90f)) / bw,
                    (getHeight() - dp(110f)) / bh), .45f, 12f);
            offsetX = getWidth() / 2f - ((minX + maxX) / 2f) * scale;
            offsetY = getHeight() / 2f - ((minY + maxY) / 2f) * scale;
        }
        syncViewToRoom();
        postInvalidateOnAnimation();
        if (listener != null) listener.onChanged();
    }

    int getZoomPercent() {
        return Math.round(scale * 100f);
    }

    float getScaleForTest() {
        return scale;
    }

    float getPanXForTest() {
        return offsetX;
    }

    float getPanYForTest() {
        return offsetY;
    }

    private void changed() {
        syncViewToRoom();
        postInvalidateOnAnimation();
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

    private void beginPinch(MotionEvent event) {
        if (event.getPointerCount() < 2) return;
        float cx = (event.getX(0) + event.getX(1)) / 2f;
        float cy = (event.getY(0) + event.getY(1)) / 2f;
        float distance = (float) Math.hypot(
                event.getX(1) - event.getX(0),
                event.getY(1) - event.getY(0));
        pinchStartDistance = Math.max(distance, 1f);
        pinchStartScale = scale;
        pinchWorldX = (cx - offsetX) / scale;
        pinchWorldY = (cy - offsetY) / scale;
        pinchActive = true;
        multiTouch = true;
        pageScrollCandidate = false;
        pageScrollActive = false;
    }

    private void applyPinch(MotionEvent event) {
        if (!pinchActive || event.getPointerCount() < 2) return;
        float cx = (event.getX(0) + event.getX(1)) / 2f;
        float cy = (event.getY(0) + event.getY(1)) / 2f;
        float distance = (float) Math.hypot(
                event.getX(1) - event.getX(0),
                event.getY(1) - event.getY(0));
        float next = clamp(
                pinchStartScale * Math.max(distance, 1f) / pinchStartDistance,
                .45f,
                12f);
        scale = next;
        offsetX = cx - pinchWorldX * scale;
        offsetY = cy - pinchWorldY * scale;
        syncViewToRoom();
        postInvalidateOnAnimation();
    }

    private void beginPageScrollCandidate(MotionEvent event) {
        pageScrollCandidate = true;
        pageScrollActive = false;
        pageScrollLastY = event.getY();
    }

    @Nullable
    private ScrollView findAncestorScrollView() {
        ViewParent parent = getParent();
        while (parent != null) {
            if (parent instanceof ScrollView) return (ScrollView) parent;
            parent = parent.getParent();
        }
        return null;
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

    private enum MagnifierKind { WALL, POINT, DIAGONAL, LABEL }

    private enum LabelKind { EDGE, DIAGONAL }

    private static final class LabelHit {
        final LabelKind kind;
        final String id;

        LabelHit(LabelKind kind, String id) {
            this.kind = kind;
            this.id = id;
        }

        NativeLabelEngine.Kind toEngineKind() {
            return kind == LabelKind.EDGE
                    ? NativeLabelEngine.Kind.EDGE
                    : NativeLabelEngine.Kind.DIAGONAL;
        }

        boolean matches(NativeLabelEngine.Placement placement) {
            return id.equals(placement.id) && toEngineKind() == placement.kind;
        }
    }
}
