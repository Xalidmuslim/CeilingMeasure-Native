package ru.xalidmuslim.ceilingmeasure;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class NativeClientEstimatePdf {
    static final class Result {
        final byte[] bytes;
        final String fileName;
        final int pricedLines;

        Result(byte[] bytes, String fileName, int pricedLines) {
            this.bytes = bytes;
            this.fileName = fileName;
            this.pricedLines = pricedLines;
        }
    }

    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;
    private static final float LOGICAL_W = 794f;
    private static final float LOGICAL_H = 1123f;
    private static final float SCALE = PAGE_W / LOGICAL_W;

    private static final int BG = Color.rgb(238, 244, 247);
    private static final int WHITE = Color.WHITE;
    private static final int TEXT = Color.rgb(32, 57, 72);
    private static final int MUTED = Color.rgb(107, 129, 143);
    private static final int LINE = Color.rgb(216, 228, 235);
    private static final int BLUE = Color.rgb(47, 143, 203);
    private static final int SOFT_BLUE = Color.rgb(237, 246, 251);
    private static final int SOFT = Color.rgb(249, 252, 253);

    private NativeClientEstimatePdf() {}

    @NonNull
    static Result build(@NonNull List<NativeProjectStore.ProjectData> projects) {
        if (projects.isEmpty()) return new Result(new byte[0], "smeta.pdf", 0);

        NativeProjectStore.ProjectData base = projects.get(0);
        boolean multi = projects.size() > 1;

        ArrayList<RoomBlock> rooms = new ArrayList<>();
        double area = 0d;
        double perimeter = 0d;
        double total = 0d;
        int pricedLines = 0;

        for (NativeProjectStore.ProjectData project : projects) {
            String orderLabel = projectLabel(project);
            for (NativeProjectStore.RoomData room : project.rooms) {
                NativeEstimateEngine.RoomCost cost = NativeEstimateEngine.clientCost(room);
                area += cost.metrics.areaM2;
                perimeter += cost.metrics.perimeterM;
                total += cost.total;

                ArrayList<NativeEstimateEngine.Line> lines = new ArrayList<>();
                for (NativeEstimateEngine.Line line : cost.lines) {
                    if (line.price > 0d) {
                        lines.add(line);
                        pricedLines++;
                    }
                }
                if (lines.isEmpty()) continue;

                String name = multi ? orderLabel + " · " + room.name : room.name;
                rooms.add(new RoomBlock(name, room.notes, cost, lines));
            }
        }

        if (pricedLines == 0) {
            return new Result(new byte[0], fileName(base), 0);
        }

        PdfDocument document = new PdfDocument();
        Writer writer = new Writer(document);
        writer.beginPage();

        float y = 20f;
        y = writer.drawHeader(y, customerNames(projects), dateText());
        y = writer.drawCustomer(y, base);
        y = writer.drawSummary(y, rooms.size(), area, perimeter);

        for (RoomBlock room : rooms) {
            float need = writer.roomHeight(room);
            if (y + need > LOGICAL_H - 72f) {
                writer.endPage();
                writer.beginPage();
                y = 24f;
            }
            y = writer.drawRoom(y, room);
        }

        if (y + 58f > LOGICAL_H - 30f) {
            writer.endPage();
            writer.beginPage();
            y = 30f;
        }
        y = writer.drawTotal(y, total);
        writer.drawFooter(y + 10f, multi);
        writer.endPage();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.writeTo(out);
            document.close();
            return new Result(out.toByteArray(), fileName(base), pricedLines);
        } catch (Exception e) {
            document.close();
            return new Result(new byte[0], fileName(base), pricedLines);
        }
    }

    private static final class RoomBlock {
        final String name;
        final String notes;
        final NativeEstimateEngine.RoomCost cost;
        final ArrayList<NativeEstimateEngine.Line> lines;

        RoomBlock(
                String name,
                String notes,
                NativeEstimateEngine.RoomCost cost,
                ArrayList<NativeEstimateEngine.Line> lines) {
            this.name = name;
            this.notes = notes == null ? "" : notes.trim();
            this.cost = cost;
            this.lines = lines;
        }
    }

    private static final class Writer {
        final PdfDocument document;
        PdfDocument.Page page;
        Canvas canvas;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNumber = 0;

        Writer(PdfDocument document) {
            this.document = document;
        }

        void beginPage() {
            pageNumber++;
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                    PAGE_W, PAGE_H, pageNumber).create();
            page = document.startPage(info);
            canvas = page.getCanvas();
            canvas.drawColor(BG);
            canvas.save();
            canvas.scale(SCALE, SCALE);
            roundRect(12f, 10f, LOGICAL_W - 24f, LOGICAL_H - 20f, 14f, WHITE, LINE);
        }

        void endPage() {
            if (page == null) return;
            canvas.restore();
            document.finishPage(page);
            page = null;
            canvas = null;
        }

        float drawHeader(float y, String customers, String date) {
            roundRect(26f, y, 30f, 30f, 9f, BLUE, null);
            font(16f, true, WHITE, Paint.Align.CENTER);
            canvas.drawText("С", 41f, y + 21f, paint);

            font(21f, true, TEXT, Paint.Align.LEFT);
            canvas.drawText("Смета", 66f, y + 18f, paint);
            font(8.8f, true, MUTED, Paint.Align.LEFT);
            canvas.drawText("НАТЯЖНЫЕ ПОТОЛКИ", 66f, y + 32f, paint);

            float docW = 142f;
            float x = 26f + (LOGICAL_W - 52f) - docW;
            roundRect(x, y, docW, 38f, 10f, Color.rgb(245,249,251), Color.rgb(220,232,238));
            font(8.5f, false, Color.rgb(123,141,153), Paint.Align.RIGHT);
            canvas.drawText("Документ", x + docW - 8f, y + 10f, paint);
            font(12.5f, true, TEXT, Paint.Align.RIGHT);
            canvas.drawText(ellipsize(customers, 24), x + docW - 8f, y + 23f, paint);
            font(8.8f, false, MUTED, Paint.Align.RIGHT);
            canvas.drawText(date, x + docW - 8f, y + 34f, paint);

            y += 40f;
            line(26f, y, LOGICAL_W - 26f, y, LINE, 1f);
            return y + 6f;
        }

        float drawCustomer(float y, NativeProjectStore.ProjectData order) {
            int rows = order.address == null || order.address.trim().isEmpty() ? 1 : 2;
            float h = 21f + rows * 34f;
            roundRect(26f, y, LOGICAL_W - 52f, h, 10f, SOFT, Color.rgb(220,231,237));
            font(9f, true, Color.rgb(104,128,142), Paint.Align.LEFT);
            canvas.drawText("ДАННЫЕ ЗАКАЗЧИКА", 35f, y + 12f, paint);

            float innerW = LOGICAL_W - 52f;
            float gap = 6f;
            float colW = (innerW - 24f - gap) / 2f;
            float boxY = y + 17f;

            infoBox(35f, boxY, colW, "Заказчик", empty(order.name, "Не указан"), true);
            infoBox(35f + colW + gap, boxY, colW, "Телефон", empty(order.phone, "Не указан"), false);
            if (rows == 2) {
                infoBox(35f, boxY + 34f, innerW - 18f, "Адрес", order.address, false);
            }
            return y + h + 6f;
        }

        float drawSummary(float y, int rooms, double area, double perimeter) {
            float innerX = 26f;
            float innerW = LOGICAL_W - 52f;
            float gap = 5f;
            float w = (innerW - gap * 2f) / 3f;
            String[][] values = new String[][]{
                    {"Помещений", String.valueOf(rooms)},
                    {"Общая площадь", qty(area) + " м²"},
                    {"Общий периметр", qty(perimeter) + " м"}
            };
            for (int i = 0; i < 3; i++) {
                float x = innerX + i * (w + gap);
                roundRect(x, y, w, 36f, 9f, WHITE, Color.rgb(220,231,237));
                font(8.4f, false, Color.rgb(122,139,151), Paint.Align.CENTER);
                canvas.drawText(values[i][0], x + w / 2f, y + 12f, paint);
                font(13f, true, TEXT, Paint.Align.CENTER);
                canvas.drawText(values[i][1], x + w / 2f, y + 28f, paint);
            }
            return y + 42f;
        }

        float roomHeight(RoomBlock room) {
            float h = 34f + 22f + 24f + 8f;
            for (NativeEstimateEngine.Line line : room.lines) h += rowHeight(line);
            if (!room.notes.isEmpty()) {
                h += 9f + wrap(room.notes, 610f, 4, 9f).size() * 10f;
            }
            return h;
        }

        float drawRoom(float y, RoomBlock room) {
            float innerX = 26f;
            float innerW = LOGICAL_W - 52f;
            float h = roomHeight(room);
            roundRect(innerX, y, innerW, h, 10f, WHITE, Color.rgb(220,231,237));
            float ry = y + 7f;

            font(13.5f, true, TEXT, Paint.Align.LEFT);
            canvas.drawText(ellipsize(room.name, 56), innerX + 9f, ry + 11f, paint);
            font(8.8f, false, Color.rgb(116,134,145), Paint.Align.LEFT);
            String meta = (room.cost.metrics.areaM2 > 0d ? qty(room.cost.metrics.areaM2) + " м²" : "")
                    + (room.cost.metrics.areaM2 > 0d && room.cost.metrics.perimeterM > 0d ? " · " : "")
                    + (room.cost.metrics.perimeterM > 0d ? qty(room.cost.metrics.perimeterM) + " м" : "");
            canvas.drawText(meta, innerX + 9f, ry + 23f, paint);

            float badgeW = 94f;
            float badgeX = innerX + innerW - badgeW - 9f;
            roundRect(badgeX, ry, badgeW, 24f, 12f, SOFT_BLUE, Color.rgb(214,230,240));
            font(10.5f, true, Color.rgb(36,78,100), Paint.Align.CENTER);
            canvas.drawText(money(room.cost.total) + " ₽", badgeX + badgeW / 2f, ry + 16f, paint);
            ry += 30f;

            float tx = innerX + 8f;
            float tw = innerW - 16f;
            float[] cols = new float[]{270f, 76f, 54f, 120f, 206f};
            String[] heads = new String[]{"Наименование","Кол-во","Ед.","Цена","Сумма"};
            roundRect(tx, ry, tw, 22f, 7f, Color.rgb(237,244,248), Color.rgb(226,235,240));
            float cx = tx;
            for (int i = 0; i < heads.length; i++) {
                font(8.6f, true, Color.rgb(97,117,130), i == 0 ? Paint.Align.LEFT : Paint.Align.RIGHT);
                canvas.drawText(heads[i], i == 0 ? cx + 6f : cx + cols[i] - 6f, ry + 15f, paint);
                cx += cols[i];
            }
            ry += 22f;

            int idx = 0;
            for (NativeEstimateEngine.Line line : room.lines) {
                float rh = rowHeight(line);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(idx % 2 == 0 ? WHITE : Color.rgb(251,253,254));
                canvas.drawRect(tx, ry, tx + tw, ry + rh, paint);
                line(tx, ry + rh, tx + tw, ry + rh, Color.rgb(237,242,245), .8f);

                float x = tx;
                font(9.4f, false, Color.rgb(38,62,77), Paint.Align.LEFT);
                List<String> nameLines = wrap(line.name, cols[0] - 12f, 2, 9.4f);
                float start = nameLines.size() > 1 ? ry + 10f : ry + rh / 2f + 3f;
                for (int j = 0; j < nameLines.size(); j++) {
                    canvas.drawText(nameLines.get(j), x + 6f, start + j * 10f, paint);
                }
                x += cols[0];

                String[] vals = new String[]{
                        qty(line.qty),
                        line.unit == null ? "" : line.unit,
                        money(line.price) + " ₽",
                        money(line.total) + " ₽"
                };
                for (int j = 0; j < vals.length; j++) {
                    float cw = cols[j + 1];
                    font(9.4f, j == 3, Color.rgb(38,62,77), Paint.Align.RIGHT);
                    canvas.drawText(vals[j], x + cw - 6f, ry + rh / 2f + 3f, paint);
                    x += cw;
                }
                ry += rh;
                idx++;
            }

            font(9.4f, false, Color.rgb(101,120,132), Paint.Align.RIGHT);
            canvas.drawText("Итого по помещению", innerX + innerW - 145f, ry + 16f, paint);
            font(12.5f, true, TEXT, Paint.Align.RIGHT);
            canvas.drawText(money(room.cost.total) + " ₽", innerX + innerW - 9f, ry + 16f, paint);
            ry += 24f;

            if (!room.notes.isEmpty()) {
                font(9f, true, Color.rgb(96,116,130), Paint.Align.LEFT);
                canvas.drawText("Примечание:", innerX + 9f, ry + 9f, paint);
                font(9f, false, Color.rgb(96,116,130), Paint.Align.LEFT);
                List<String> lines = wrap(room.notes, innerW - 78f, 4, 9f);
                for (int i = 0; i < lines.size(); i++) {
                    canvas.drawText(lines.get(i), innerX + 67f, ry + 9f + i * 10f, paint);
                }
            }
            return y + h + 4f;
        }

        float drawTotal(float y, double total) {
            float innerX = 26f;
            float innerW = LOGICAL_W - 52f;
            roundRect(innerX, y, innerW, 36f, 10f, SOFT_BLUE, Color.rgb(214,230,240));
            font(12f, false, Color.rgb(76,100,116), Paint.Align.LEFT);
            canvas.drawText("Итого к оплате", innerX + 11f, y + 23f, paint);
            font(19f, true, TEXT, Paint.Align.RIGHT);
            canvas.drawText(money(total) + " ₽", innerX + innerW - 11f, y + 24f, paint);
            return y + 45f;
        }

        void drawFooter(float y, boolean multi) {
            font(8.8f, false, Color.rgb(123,141,153), Paint.Align.CENTER);
            canvas.drawText(
                    multi
                            ? "Смета объединяет несколько выбранных заказов."
                            : "Смета сформирована по данным текущего замера.",
                    LOGICAL_W / 2f,
                    y,
                    paint);
        }

        float rowHeight(NativeEstimateEngine.Line line) {
            return wrap(line.name, 258f, 2, 9.4f).size() > 1 ? 29f : 23f;
        }

        void infoBox(float x, float y, float w, String label, String value, boolean bold) {
            roundRect(x, y, w, 29f, 8f, WHITE, Color.rgb(227,237,242));
            font(8f, false, Color.rgb(122,139,151), Paint.Align.LEFT);
            canvas.drawText(label, x + 7f, y + 10f, paint);
            font(9.7f, bold, Color.rgb(38,62,77), Paint.Align.LEFT);
            List<String> lines = wrap(value, w - 14f, 2, 9.7f);
            for (int i = 0; i < lines.size(); i++) {
                canvas.drawText(lines.get(i), x + 7f, y + 22f + i * 9f, paint);
            }
        }

        List<String> wrap(String value, float maxWidth, int maxLines, float size) {
            ArrayList<String> lines = new ArrayList<>();
            String text = value == null ? "" : value.trim();
            if (text.isEmpty()) {
                lines.add("");
                return lines;
            }
            font(size, false, TEXT, Paint.Align.LEFT);
            String[] words = text.split("\\s+");
            String current = "";
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                    current = candidate;
                } else {
                    lines.add(current);
                    current = word;
                    if (lines.size() == maxLines - 1) break;
                }
            }
            if (!current.isEmpty() && lines.size() < maxLines) lines.add(current);
            if (lines.isEmpty()) lines.add(ellipsize(text, 32));
            return lines;
        }

        void roundRect(float x, float y, float w, float h, float radius, int fill, Integer stroke) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fill);
            canvas.drawRoundRect(new RectF(x, y, x + w, y + h), radius, radius, paint);
            if (stroke != null) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1f);
                paint.setColor(stroke);
                canvas.drawRoundRect(new RectF(x, y, x + w, y + h), radius, radius, paint);
            }
        }

        void line(float x1, float y1, float x2, float y2, int color, float width) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(width);
            paint.setColor(color);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        void font(float size, boolean bold, int color, Paint.Align align) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(align);
            paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        }
    }

    private static String projectLabel(NativeProjectStore.ProjectData p) {
        String name = p.name == null ? "" : p.name.trim();
        if (!name.isEmpty()) return name;
        String address = p.address == null ? "" : p.address.trim();
        return address.isEmpty() ? "Без имени" : address;
    }

    private static String customerNames(List<NativeProjectStore.ProjectData> projects) {
        Set<String> names = new LinkedHashSet<>();
        for (NativeProjectStore.ProjectData p : projects) {
            String name = p.name == null ? "" : p.name.trim();
            if (!name.isEmpty()) names.add(name);
        }
        if (names.isEmpty()) return "Без имени";
        String joined = String.join(", ", names);
        return joined.length() > 30 ? joined.substring(0, 29) + "…" : joined;
    }

    private static String fileName(NativeProjectStore.ProjectData base) {
        String client = base.name == null || base.name.trim().isEmpty()
                ? "bez_imeni"
                : safe(base.name.trim());
        return "smeta_" + client + "_" + dateText().replace('.', '-') + ".pdf";
    }

    private static String safe(String value) {
        String s = value.replaceAll("[^\\p{L}\\p{N}._-]+", "_");
        s = s.replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "bez_imeni" : s;
    }

    private static String dateText() {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
    }

    private static String empty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String ellipsize(String value, int max) {
        String s = value == null ? "" : value;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String qty(double value) {
        String s = String.format(Locale.getDefault(), "%.2f", value);
        return s.replaceAll("0+$", "").replaceAll("[,.]$", "");
    }

    private static String money(double value) {
        String s = String.format(Locale.getDefault(), "%.2f", value);
        return s.replaceAll("0+$", "").replaceAll("[,.]$", "");
    }
}
