package com.xalid.ceilingvisualizer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;

    private EditorView editor;
    private TextView hint;
    private LinearLayout optionRow;
    private TextView compareChip;
    private final ArrayList<TextView> toolChips = new ArrayList<>();
    private EditorView.Mode selectedMode = EditorView.Mode.CEILING;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(17, 19, 21));
        getWindow().setNavigationBarColor(Color.rgb(17, 19, 21));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17, 19, 21));

        applySystemInsets(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(5), dp(8), dp(5));
        top.setBackgroundColor(Color.rgb(24, 27, 30));

        TextView title = text("Визуализатор", 16, Color.WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView photo = actionChip("Фото", 62);
        TextView undo = actionChip("↶", 44);
        compareChip = actionChip("До", 52);
        TextView clear = actionChip("×", 44);

        top.addView(photo);
        top.addView(undo);
        top.addView(compareChip);
        top.addView(clear);
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        FrameLayout editorFrame = new FrameLayout(this);
        editor = new EditorView(this);

        hint = text("Нажмите «Фото» и выберите помещение", 12, Color.WHITE);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(12), dp(7), dp(12), dp(7));
        hint.setBackground(roundRect(Color.argb(190, 20, 23, 25), 16));

        FrameLayout.LayoutParams editorLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        editorFrame.addView(editor, editorLp);

        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
        );
        hintLp.setMargins(dp(12), dp(12), dp(12), dp(10));
        editorFrame.addView(hint, hintLp);

        root.addView(editorFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        HorizontalScrollView optionsScroll = new HorizontalScrollView(this);
        optionsScroll.setHorizontalScrollBarEnabled(false);
        optionsScroll.setFillViewport(false);
        optionsScroll.setBackgroundColor(Color.rgb(24, 27, 30));

        optionRow = new LinearLayout(this);
        optionRow.setGravity(Gravity.CENTER_VERTICAL);
        optionRow.setPadding(dp(7), dp(4), dp(7), dp(4));
        optionsScroll.addView(optionRow);
        root.addView(optionsScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)
        ));

        HorizontalScrollView toolsScroll = new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        toolsScroll.setBackgroundColor(Color.rgb(18, 21, 23));

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(dp(7), dp(6), dp(7), dp(7));

        String[] names = {"Рука", "Потолок", "Линия", "Трек", "Споты", "Люстра"};
        EditorView.Mode[] modes = {
                EditorView.Mode.VIEW,
                EditorView.Mode.CEILING,
                EditorView.Mode.LINE,
                EditorView.Mode.TRACK,
                EditorView.Mode.SPOT,
                EditorView.Mode.CHANDELIER
        };

        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView chip = toolChip(names[i]);
            chip.setOnClickListener(v -> selectMode(modes[index], index));
            tools.addView(chip);
            toolChips.add(chip);
        }

        toolsScroll.addView(tools);
        root.addView(toolsScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60)
        ));

        photo.setOnClickListener(v -> pickPhoto());
        undo.setOnClickListener(v -> editor.undo());
        compareChip.setOnClickListener(v -> {
            boolean after = editor.toggleAfter();
            compareChip.setText(after ? "До" : "После");
            setHint(after ? editor.helpText() : "Показан исходный снимок");
        });
        clear.setOnClickListener(v -> {
            editor.clearAll();
            setHint("Все добавленные элементы очищены");
        });

        setContentView(root);
        selectMode(EditorView.Mode.CEILING, 1);
    }

    private void applySystemInsets(View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void selectMode(EditorView.Mode mode, int chipIndex) {
        selectedMode = mode;
        editor.setMode(mode);

        for (int i = 0; i < toolChips.size(); i++) {
            styleToolChip(toolChips.get(i), i == chipIndex);
        }

        rebuildOptions();
        setHint(editor.helpText());
    }

    private void rebuildOptions() {
        optionRow.removeAllViews();

        switch (selectedMode) {
            case VIEW:
                addOption("Сбросить вид", false, () -> editor.resetView());
                break;

            case CEILING:
                addOption("Белый", editor.getCeilingColor() == Color.rgb(248, 248, 246),
                        () -> { editor.setCeilingColor(Color.rgb(248, 248, 246)); rebuildOptions(); });
                addOption("Серый", editor.getCeilingColor() == Color.rgb(214, 216, 216),
                        () -> { editor.setCeilingColor(Color.rgb(214, 216, 216)); rebuildOptions(); });
                addOption("Бежевый", editor.getCeilingColor() == Color.rgb(225, 216, 199),
                        () -> { editor.setCeilingColor(Color.rgb(225, 216, 199)); rebuildOptions(); });
                addOption("Скрыть маску", editor.isCeilingPreviewHidden(),
                        () -> { editor.toggleCeilingPreview(); rebuildOptions(); });
                break;

            case LINE:
                addKelvinOptions();
                addWidthOptions();
                addOption("90°", editor.isOrtho(), () -> { editor.toggleOrtho(); rebuildOptions(); });
                break;

            case TRACK:
                addOption("Чёрный", editor.isTrackBlack(),
                        () -> { editor.setTrackBlack(true); rebuildOptions(); });
                addOption("Белый", !editor.isTrackBlack(),
                        () -> { editor.setTrackBlack(false); rebuildOptions(); });
                addOption("90°", editor.isOrtho(), () -> { editor.toggleOrtho(); rebuildOptions(); });
                break;

            case SPOT:
                addKelvinOptions();
                addScaleOptions();
                break;

            case CHANDELIER:
                addScaleOptions();
                break;
        }
    }

    private void addKelvinOptions() {
        addOption("3000K", editor.getKelvin() == 3000,
                () -> { editor.setKelvin(3000); rebuildOptions(); });
        addOption("3500K", editor.getKelvin() == 3500,
                () -> { editor.setKelvin(3500); rebuildOptions(); });
        addOption("4000K", editor.getKelvin() == 4000,
                () -> { editor.setKelvin(4000); rebuildOptions(); });
    }

    private void addWidthOptions() {
        addOption("20 мм", editor.getLineMm() == 20,
                () -> { editor.setLineMm(20); rebuildOptions(); });
        addOption("30 мм", editor.getLineMm() == 30,
                () -> { editor.setLineMm(30); rebuildOptions(); });
        addOption("50 мм", editor.getLineMm() == 50,
                () -> { editor.setLineMm(50); rebuildOptions(); });
    }

    private void addScaleOptions() {
        addOption("80%", editor.getFixtureScale() == 0.8f,
                () -> { editor.setFixtureScale(0.8f); rebuildOptions(); });
        addOption("100%", editor.getFixtureScale() == 1.0f,
                () -> { editor.setFixtureScale(1.0f); rebuildOptions(); });
        addOption("120%", editor.getFixtureScale() == 1.2f,
                () -> { editor.setFixtureScale(1.2f); rebuildOptions(); });
    }

    private void addOption(String label, boolean selected, Runnable action) {
        TextView chip = text(label, 12, selected ? Color.rgb(15, 20, 16) : Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(13), 0, dp(13), 0);
        chip.setBackground(roundRect(
                selected ? Color.rgb(167, 224, 75) : Color.rgb(40, 45, 49), 15
        ));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)
        );
        lp.setMargins(dp(3), dp(2), dp(3), dp(2));
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> action.run());
        optionRow.addView(chip);
    }

    private void setHint(String value) {
        hint.setText(value);
        hint.setVisibility(value == null || value.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        try {
            int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if (flags != 0) {
                getContentResolver().takePersistableUriPermission(uri, flags);
            }

            try (InputStream input = getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                if (bitmap == null) {
                    throw new IllegalStateException("Bitmap decode failed");
                }
                editor.setBitmap(bitmap);
                selectMode(EditorView.Mode.CEILING, 1);
            }
        } catch (Exception error) {
            Toast.makeText(this, "Не удалось открыть фотографию", Toast.LENGTH_LONG).show();
        }
    }

    private TextView actionChip(String label, int widthDp) {
        TextView chip = text(label, 12, Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(roundRect(Color.rgb(39, 44, 48), 15));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(widthDp), dp(40));
        lp.setMargins(dp(3), 0, dp(3), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private TextView toolChip(String label) {
        TextView chip = text(label, 13, Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), 0, dp(14), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(45)
        );
        lp.setMargins(dp(3), 0, dp(3), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void styleToolChip(TextView chip, boolean active) {
        chip.setTextColor(active ? Color.rgb(15, 20, 16) : Color.WHITE);
        chip.setBackground(roundRect(
                active ? Color.rgb(167, 224, 75) : Color.rgb(39, 44, 48), 16
        ));
    }

    private TextView text(String value, int sp, int color) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setFontFeatureSettings("kern");
        return textView;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public static class EditorView extends View {
        enum Mode { VIEW, CEILING, LINE, TRACK, SPOT, CHANDELIER }

        static class Segment {
            PointF a;
            PointF b;

            Segment(PointF a, PointF b) {
                this.a = a;
                this.b = b;
            }
        }

        static class Snapshot {
            ArrayList<PointF> mask;
            ArrayList<Segment> lines;
            ArrayList<Segment> tracks;
            ArrayList<PointF> spots;
            ArrayList<PointF> chandeliers;

            Snapshot(EditorView editor) {
                mask = copyPoints(editor.mask);
                lines = copySegments(editor.lines);
                tracks = copySegments(editor.tracks);
                spots = copyPoints(editor.spots);
                chandeliers = copyPoints(editor.chandeliers);
            }

            static ArrayList<PointF> copyPoints(List<PointF> source) {
                ArrayList<PointF> result = new ArrayList<>();
                for (PointF point : source) {
                    result.add(new PointF(point.x, point.y));
                }
                return result;
            }

            static ArrayList<Segment> copySegments(List<Segment> source) {
                ArrayList<Segment> result = new ArrayList<>();
                for (Segment segment : source) {
                    result.add(new Segment(
                            new PointF(segment.a.x, segment.a.y),
                            new PointF(segment.b.x, segment.b.y)
                    ));
                }
                return result;
            }
        }

        private Bitmap bitmap;
        private Mode mode = Mode.CEILING;
        private boolean showAfter = true;
        private boolean hideCeilingPreview = false;

        private final ArrayList<PointF> mask = new ArrayList<>();
        private final ArrayList<PointF> draft = new ArrayList<>();
        private final ArrayList<PointF> spots = new ArrayList<>();
        private final ArrayList<PointF> chandeliers = new ArrayList<>();
        private final ArrayList<Segment> lines = new ArrayList<>();
        private final ArrayList<Segment> tracks = new ArrayList<>();
        private final ArrayDeque<Snapshot> history = new ArrayDeque<>();

        private PointF start;
        private PointF current;

        private float zoom = 1f;
        private float panX = 0f;
        private float panY = 0f;
        private float lastX;
        private float lastY;

        private boolean moving = false;
        private boolean pinching = false;
        private boolean suppressSingleTouch = false;
        private float pinchStartDistance = 0f;
        private float pinchStartZoom = 1f;
        private float pinchStartPanX = 0f;
        private float pinchStartPanY = 0f;
        private float pinchStartMidX = 0f;
        private float pinchStartMidY = 0f;

        private int ceilingColor = Color.rgb(248, 248, 246);
        private int kelvin = 3500;
        private int lineMm = 30;
        private boolean ortho = false;
        private boolean trackBlack = true;
        private float fixtureScale = 1f;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;

        EditorView(Activity activity) {
            super(activity);
            density = getResources().getDisplayMetrics().density;
            setBackgroundColor(Color.BLACK);
        }

        void setBitmap(Bitmap value) {
            bitmap = value;
            clearGeometry();
            history.clear();
            resetView();
            invalidate();
        }

        void setMode(Mode value) {
            mode = value;
            draft.clear();
            start = null;
            current = null;
            invalidate();
        }

        String helpText() {
            switch (mode) {
                case VIEW:
                    return "Рука: двигайте одним пальцем, масштабируйте двумя";
                case CEILING:
                    return "Потолок: обведите границу одним движением";
                case LINE:
                    return "Линия: проведите от начала до конца; двумя пальцами — масштаб";
                case TRACK:
                    return "Трек: проведите направление; двумя пальцами — масштаб";
                case SPOT:
                    return "Споты: касайтесь мест установки";
                case CHANDELIER:
                    return "Люстра: коснитесь точки центра";
                default:
                    return "";
            }
        }

        boolean toggleAfter() {
            showAfter = !showAfter;
            invalidate();
            return showAfter;
        }

        void pushHistory() {
            if (history.size() >= 30) {
                history.removeFirst();
            }
            history.addLast(new Snapshot(this));
        }

        void undo() {
            if (history.isEmpty()) {
                return;
            }
            Snapshot snapshot = history.removeLast();
            restore(snapshot);
            invalidate();
        }

        void restore(Snapshot snapshot) {
            mask.clear();
            mask.addAll(snapshot.mask);
            lines.clear();
            lines.addAll(snapshot.lines);
            tracks.clear();
            tracks.addAll(snapshot.tracks);
            spots.clear();
            spots.addAll(snapshot.spots);
            chandeliers.clear();
            chandeliers.addAll(snapshot.chandeliers);
        }

        void clearAll() {
            if (mask.isEmpty() && lines.isEmpty() && tracks.isEmpty()
                    && spots.isEmpty() && chandeliers.isEmpty()) {
                return;
            }
            pushHistory();
            clearGeometry();
            invalidate();
        }

        private void clearGeometry() {
            mask.clear();
            draft.clear();
            lines.clear();
            tracks.clear();
            spots.clear();
            chandeliers.clear();
            start = null;
            current = null;
        }

        void resetView() {
            zoom = 1f;
            panX = 0f;
            panY = 0f;
            invalidate();
        }

        int getCeilingColor() { return ceilingColor; }
        void setCeilingColor(int value) { ceilingColor = value; invalidate(); }

        int getKelvin() { return kelvin; }
        void setKelvin(int value) { kelvin = value; invalidate(); }

        int getLineMm() { return lineMm; }
        void setLineMm(int value) { lineMm = value; invalidate(); }

        boolean isOrtho() { return ortho; }
        void toggleOrtho() { ortho = !ortho; invalidate(); }

        boolean isTrackBlack() { return trackBlack; }
        void setTrackBlack(boolean value) { trackBlack = value; invalidate(); }

        float getFixtureScale() { return fixtureScale; }
        void setFixtureScale(float value) { fixtureScale = value; invalidate(); }

        boolean isCeilingPreviewHidden() { return hideCeilingPreview; }
        void toggleCeilingPreview() { hideCeilingPreview = !hideCeilingPreview; invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (bitmap == null) {
                drawEmpty(canvas);
                return;
            }

            RectF dst = imageRect();
            canvas.drawBitmap(bitmap, null, dst, paint);

            if (!showAfter) {
                return;
            }

            if (mask.size() >= 3 && !hideCeilingPreview) {
                Path maskPath = pathFor(mask, true);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(withAlpha(ceilingColor, 108));
                canvas.drawPath(maskPath, paint);

                if (mode == Mode.CEILING) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dpF(1.5f));
                    paint.setColor(Color.rgb(167, 224, 75));
                    canvas.drawPath(maskPath, paint);
                }
            }

            if (draft.size() > 1) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dpF(2.2f));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(Color.rgb(167, 224, 75));
                canvas.drawPath(pathFor(draft, false), paint);
            }

            for (Segment segment : lines) {
                drawLight(canvas, segment.a, segment.b);
            }

            for (Segment segment : tracks) {
                drawTrack(canvas, segment.a, segment.b);
            }

            for (PointF point : spots) {
                drawSpot(canvas, point);
            }

            for (PointF point : chandeliers) {
                drawChandelier(canvas, point);
            }

            if (start != null && current != null) {
                if (mode == Mode.LINE) {
                    drawPreviewLine(canvas, start, current, true);
                } else if (mode == Mode.TRACK) {
                    drawPreviewLine(canvas, start, current, false);
                }
            }
        }

        private void drawEmpty(Canvas canvas) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.WHITE);
            paint.setTextSize(dpF(18));
            canvas.drawText("Визуализатор потолков", getWidth() / 2f, getHeight() / 2f - dpF(12), paint);

            paint.setColor(Color.rgb(155, 163, 169));
            paint.setTextSize(dpF(13));
            canvas.drawText("Выберите фотографию помещения", getWidth() / 2f, getHeight() / 2f + dpF(18), paint);
        }

        private void drawPreviewLine(Canvas canvas, PointF a, PointF b, boolean light) {
            if (!ortho) {
                if (light) {
                    drawLight(canvas, a, b);
                } else {
                    drawTrack(canvas, a, b);
                }
                return;
            }

            PointF elbow = orthogonalElbow(a, b);
            if (light) {
                drawLight(canvas, a, elbow);
                drawLight(canvas, elbow, b);
            } else {
                drawTrack(canvas, a, elbow);
                drawTrack(canvas, elbow, b);
            }
        }

        private void drawLight(Canvas canvas, PointF imageA, PointF imageB) {
            int lightColor = lightColor();

            float core = dpF(2.2f + lineMm / 20f);
            drawPerspectiveStrip(canvas, imageA, imageB, core * 4.2f, withAlpha(lightColor, 45));
            drawPerspectiveStrip(canvas, imageA, imageB, core * 2.1f, withAlpha(lightColor, 105));
            drawPerspectiveStrip(canvas, imageA, imageB, core, withAlpha(Color.WHITE, 235));
        }

        private void drawTrack(Canvas canvas, PointF imageA, PointF imageB) {
            int base = trackBlack ? Color.rgb(22, 22, 22) : Color.rgb(238, 238, 236);
            int edge = trackBlack ? Color.rgb(90, 94, 96) : Color.rgb(165, 168, 170);

            drawPerspectiveStrip(canvas, imageA, imageB, dpF(7.2f), withAlpha(edge, 230));
            drawPerspectiveStrip(canvas, imageA, imageB, dpF(5.8f), withAlpha(base, 245));

            PointF a = toScreen(imageA);
            PointF b = toScreen(imageB);
            float length = distance(a, b);
            int count = Math.max(1, (int) (length / dpF(75)));

            for (int i = 0; i <= count; i++) {
                float t = count == 0 ? 0f : i / (float) count;
                PointF imagePoint = interpolate(imageA, imageB, t);
                PointF screenPoint = toScreen(imagePoint);
                float scale = depthFactor(imagePoint);
                float radius = dpF(5.4f) * scale;

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(trackBlack ? Color.rgb(230, 232, 233) : Color.rgb(45, 47, 48));
                canvas.drawCircle(screenPoint.x, screenPoint.y, radius, paint);

                paint.setColor(trackBlack ? Color.rgb(30, 30, 30) : Color.rgb(235, 235, 233));
                canvas.drawCircle(screenPoint.x, screenPoint.y, radius * 0.58f, paint);
            }
        }

        private void drawSpot(Canvas canvas, PointF imagePoint) {
            PointF screen = toScreen(imagePoint);
            float radius = dpF(8.3f) * depthFactor(imagePoint) * fixtureScale;
            int light = lightColor();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(light, 35));
            canvas.drawCircle(screen.x, screen.y, radius * 3.2f, paint);

            paint.setColor(Color.rgb(245, 245, 242));
            canvas.drawCircle(screen.x, screen.y, radius, paint);

            paint.setColor(Color.rgb(72, 76, 78));
            canvas.drawCircle(screen.x, screen.y, radius * 0.42f, paint);

            paint.setColor(light);
            canvas.drawCircle(screen.x, screen.y, radius * 0.23f, paint);
        }

        private void drawChandelier(Canvas canvas, PointF imagePoint) {
            PointF screen = toScreen(imagePoint);
            float scale = depthFactor(imagePoint) * fixtureScale;
            float ring = dpF(22) * scale;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(38, 255, 244, 210));
            canvas.drawCircle(screen.x, screen.y, ring * 1.8f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpF(2.2f) * scale);
            paint.setColor(Color.rgb(225, 225, 222));
            canvas.drawCircle(screen.x, screen.y, ring, paint);

            paint.setStrokeWidth(dpF(1.6f) * scale);
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2.0;
                float x = screen.x + (float) Math.cos(angle) * ring * 1.5f;
                float y = screen.y + (float) Math.sin(angle) * ring * 1.5f;
                canvas.drawLine(screen.x, screen.y, x, y, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(lightColor());
                canvas.drawCircle(x, y, dpF(4f) * scale, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.rgb(225, 225, 222));
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(lightColor());
            canvas.drawCircle(screen.x, screen.y, dpF(5f) * scale, paint);
        }

        private void drawPerspectiveStrip(Canvas canvas, PointF imageA, PointF imageB, float baseWidth, int color) {
            PointF a = toScreen(imageA);
            PointF b = toScreen(imageB);

            float dx = b.x - a.x;
            float dy = b.y - a.y;
            float length = (float) Math.hypot(dx, dy);

            if (length < 0.001f) {
                return;
            }

            float nx = -dy / length;
            float ny = dx / length;

            float halfA = baseWidth * depthFactor(imageA) / 2f;
            float halfB = baseWidth * depthFactor(imageB) / 2f;

            Path path = new Path();
            path.moveTo(a.x + nx * halfA, a.y + ny * halfA);
            path.lineTo(b.x + nx * halfB, b.y + ny * halfB);
            path.lineTo(b.x - nx * halfB, b.y - ny * halfB);
            path.lineTo(a.x - nx * halfA, a.y - ny * halfA);
            path.close();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawPath(path, paint);
        }

        private int lightColor() {
            if (kelvin <= 3000) {
                return Color.rgb(255, 214, 142);
            }
            if (kelvin >= 4000) {
                return Color.rgb(244, 248, 255);
            }
            return Color.rgb(255, 231, 184);
        }

        private float depthFactor(PointF point) {
            if (bitmap == null) {
                return 1f;
            }

            float minY = 0f;
            float maxY = bitmap.getHeight();

            if (mask.size() >= 3) {
                minY = Float.MAX_VALUE;
                maxY = -Float.MAX_VALUE;

                for (PointF p : mask) {
                    minY = Math.min(minY, p.y);
                    maxY = Math.max(maxY, p.y);
                }
            }

            float range = Math.max(1f, maxY - minY);
            float t = (point.y - minY) / range;
            t = Math.max(0f, Math.min(1f, t));

            return 0.62f + t * 0.68f;
        }

        private Path pathFor(List<PointF> points, boolean close) {
            Path path = new Path();

            if (points.isEmpty()) {
                return path;
            }

            PointF first = toScreen(points.get(0));
            path.moveTo(first.x, first.y);

            for (int i = 1; i < points.size(); i++) {
                PointF p = toScreen(points.get(i));
                path.lineTo(p.x, p.y);
            }

            if (close) {
                path.close();
            }

            return path;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (bitmap == null) {
                return true;
            }

            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
                beginPinch(event);
                draft.clear();
                start = null;
                current = null;
                suppressSingleTouch = true;
                invalidate();
                return true;
            }

            if (pinching && event.getPointerCount() >= 2 && action == MotionEvent.ACTION_MOVE) {
                applyPinch(event);
                return true;
            }

            if (pinching && action == MotionEvent.ACTION_POINTER_UP) {
                pinching = false;
                return true;
            }

            if (suppressSingleTouch) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    suppressSingleTouch = false;
                }
                return true;
            }

            float x = event.getX();
            float y = event.getY();

            if (mode == Mode.VIEW) {
                handleViewPan(event, x, y);
                return true;
            }

            PointF imagePoint = toImage(x, y);

            if (imagePoint == null) {
                return true;
            }

            if (mode == Mode.SPOT || mode == Mode.CHANDELIER) {
                if (action == MotionEvent.ACTION_UP) {
                    pushHistory();
                    if (mode == Mode.SPOT) {
                        spots.add(imagePoint);
                    } else {
                        chandeliers.add(imagePoint);
                    }
                    invalidate();
                }
                return true;
            }

            if (action == MotionEvent.ACTION_DOWN) {
                start = imagePoint;
                current = imagePoint;

                if (mode == Mode.CEILING) {
                    draft.clear();
                    draft.add(imagePoint);
                }
            } else if (action == MotionEvent.ACTION_MOVE) {
                current = imagePoint;

                if (mode == Mode.CEILING) {
                    PointF last = draft.get(draft.size() - 1);
                    if (distance(last, imagePoint) > 6f) {
                        draft.add(imagePoint);
                    }
                }

                invalidate();
            } else if (action == MotionEvent.ACTION_UP) {
                current = imagePoint;

                if (mode == Mode.CEILING && draft.size() >= 3) {
                    pushHistory();
                    mask.clear();
                    mask.addAll(draft);
                    draft.clear();
                } else if (mode == Mode.LINE && distance(start, current) > 10f) {
                    pushHistory();
                    addSegmentWithOptionalElbow(lines, start, current);
                } else if (mode == Mode.TRACK && distance(start, current) > 10f) {
                    pushHistory();
                    addSegmentWithOptionalElbow(tracks, start, current);
                }

                start = null;
                current = null;
                invalidate();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                draft.clear();
                start = null;
                current = null;
                invalidate();
            }

            return true;
        }

        private void beginPinch(MotionEvent event) {
            float dx = event.getX(0) - event.getX(1);
            float dy = event.getY(0) - event.getY(1);

            pinchStartDistance = (float) Math.hypot(dx, dy);
            pinchStartZoom = zoom;
            pinchStartPanX = panX;
            pinchStartPanY = panY;
            pinchStartMidX = (event.getX(0) + event.getX(1)) / 2f;
            pinchStartMidY = (event.getY(0) + event.getY(1)) / 2f;
            pinching = true;
        }

        private void applyPinch(MotionEvent event) {
            float dx = event.getX(0) - event.getX(1);
            float dy = event.getY(0) - event.getY(1);
            float distance = (float) Math.hypot(dx, dy);

            if (pinchStartDistance <= 0f) {
                return;
            }

            float midpointX = (event.getX(0) + event.getX(1)) / 2f;
            float midpointY = (event.getY(0) + event.getY(1)) / 2f;

            zoom = clamp(pinchStartZoom * distance / pinchStartDistance, 1f, 6f);
            panX = pinchStartPanX + (midpointX - pinchStartMidX);
            panY = pinchStartPanY + (midpointY - pinchStartMidY);
            invalidate();
        }

        private void handleViewPan(MotionEvent event, float x, float y) {
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                lastX = x;
                lastY = y;
                moving = true;
            } else if (action == MotionEvent.ACTION_MOVE && moving) {
                panX += x - lastX;
                panY += y - lastY;
                lastX = x;
                lastY = y;
                invalidate();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                moving = false;
            }
        }

        private void addSegmentWithOptionalElbow(ArrayList<Segment> destination, PointF a, PointF b) {
            if (!ortho) {
                destination.add(new Segment(copy(a), copy(b)));
                return;
            }

            PointF elbow = orthogonalElbow(a, b);

            if (distance(a, elbow) > 4f) {
                destination.add(new Segment(copy(a), copy(elbow)));
            }
            if (distance(elbow, b) > 4f) {
                destination.add(new Segment(copy(elbow), copy(b)));
            }
        }

        private PointF orthogonalElbow(PointF a, PointF b) {
            float horizontalFirst = Math.abs(b.x - a.x);
            float verticalFirst = Math.abs(b.y - a.y);

            if (horizontalFirst >= verticalFirst) {
                return new PointF(b.x, a.y);
            }
            return new PointF(a.x, b.y);
        }

        private RectF imageRect() {
            float imageWidth = bitmap.getWidth();
            float imageHeight = bitmap.getHeight();

            float baseScale = Math.max(
                    getWidth() / imageWidth,
                    getHeight() / imageHeight
            );

            float width = imageWidth * baseScale * zoom;
            float height = imageHeight * baseScale * zoom;
            float centerX = getWidth() / 2f + panX;
            float centerY = getHeight() / 2f + panY;

            return new RectF(
                    centerX - width / 2f,
                    centerY - height / 2f,
                    centerX + width / 2f,
                    centerY + height / 2f
            );
        }

        private PointF toImage(float x, float y) {
            RectF rect = imageRect();

            if (!rect.contains(x, y)) {
                return null;
            }

            return new PointF(
                    (x - rect.left) * bitmap.getWidth() / rect.width(),
                    (y - rect.top) * bitmap.getHeight() / rect.height()
            );
        }

        private PointF toScreen(PointF imagePoint) {
            RectF rect = imageRect();

            return new PointF(
                    rect.left + imagePoint.x * rect.width() / bitmap.getWidth(),
                    rect.top + imagePoint.y * rect.height() / bitmap.getHeight()
            );
        }

        private PointF interpolate(PointF a, PointF b, float t) {
            return new PointF(
                    a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t
            );
        }

        private static PointF copy(PointF p) {
            return new PointF(p.x, p.y);
        }

        private float dpF(float value) {
            return value * density;
        }

        private static float distance(PointF a, PointF b) {
            return (float) Math.hypot(a.x - b.x, a.y - b.y);
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private static int withAlpha(int color, int alpha) {
            return Color.argb(
                    alpha,
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
            );
        }
    }
}
