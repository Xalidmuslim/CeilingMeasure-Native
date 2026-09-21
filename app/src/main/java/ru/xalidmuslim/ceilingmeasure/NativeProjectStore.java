package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class NativeProjectStore {
    private static final String PREFS = "native_ceiling_projects";
    private static final String KEY_CURRENT = "current_project";
    private static final String KEY_SAVED = "saved_projects";

    static String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString();
    }

    static final class PointData {
        String id;
        float x;
        float y;
        String customLabel = "";

        PointData(float x, float y) {
            this(newId("p"), x, y);
        }

        PointData(@NonNull String id, float x, float y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    static final class EdgeData {
        String id;
        String a;
        String b;
        float value;
        float labelDx;
        float labelDy;
        float labelAngle;
        float labelSize = 1f;
        boolean labelManual;

        EdgeData(@NonNull String a, @NonNull String b) {
            this(newId("e"), a, b, 0f);
        }

        EdgeData(@NonNull String id, @NonNull String a, @NonNull String b, float value) {
            this.id = id;
            this.a = a;
            this.b = b;
            this.value = value;
        }
    }

    static final class DiagonalData {
        String id;
        String a;
        String b;
        float value;
        float labelDx;
        float labelDy;
        float labelAngle;
        float labelSize = 1f;
        boolean labelManual;

        DiagonalData(@NonNull String a, @NonNull String b) {
            this(newId("d"), a, b, 0f);
        }

        DiagonalData(@NonNull String id, @NonNull String a, @NonNull String b, float value) {
            this.id = id;
            this.a = a;
            this.b = b;
            this.value = value;
        }
    }

    static final class RoomData {
        String id = newId("room");
        String name = "Комната 1";
        final ArrayList<PointData> points = new ArrayList<>();
        final ArrayList<EdgeData> edges = new ArrayList<>();
        final ArrayList<DiagonalData> diagonals = new ArrayList<>();
        boolean point90Magnet = true;
        boolean wallAngleMagnet = true;
        boolean showAutoLetters = false;
        boolean geometryLocked = false;
        boolean hintCollapsed = false;
        float viewScale = 1f;
        float panX = 0f;
        float panY = 0f;
    }

    static final class ProjectData {
        String id = newId("project");
        String name = "";
        String phone = "";
        String address = "";
        long updatedAt = System.currentTimeMillis();
        long savedAt = 0L;
        int activeRoom = 0;
        final ArrayList<RoomData> rooms = new ArrayList<>();
    }

    private final SharedPreferences prefs;

    NativeProjectStore(@NonNull Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    ProjectData newProject() {
        ProjectData p = new ProjectData();
        p.rooms.add(new RoomData());
        saveCurrent(p);
        return p;
    }

    ProjectData loadCurrent() {
        String raw = prefs.getString(KEY_CURRENT, null);
        if (raw == null || raw.isEmpty()) return newProject();
        try {
            return projectFromJson(new JSONObject(raw));
        } catch (Exception e) {
            return newProject();
        }
    }

    void saveCurrent(@NonNull ProjectData project) {
        project.updatedAt = System.currentTimeMillis();
        prefs.edit().putString(KEY_CURRENT, projectToJson(project).toString()).apply();
    }

    List<ProjectData> loadSaved() {
        ArrayList<ProjectData> result = new ArrayList<>();
        String raw = prefs.getString(KEY_SAVED, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                result.add(projectFromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    void saveSnapshot(@NonNull ProjectData project) {
        project.savedAt = System.currentTimeMillis();
        List<ProjectData> saved = loadSaved();
        ArrayList<ProjectData> next = new ArrayList<>();
        next.add(cloneProject(project));
        for (ProjectData p : saved) {
            if (!p.id.equals(project.id)) next.add(p);
            if (next.size() >= 50) break;
        }
        JSONArray arr = new JSONArray();
        for (ProjectData p : next) arr.put(projectToJson(p));
        prefs.edit().putString(KEY_SAVED, arr.toString()).apply();
        saveCurrent(project);
    }

    void deleteSaved(@NonNull String id) {
        List<ProjectData> saved = loadSaved();
        JSONArray arr = new JSONArray();
        for (ProjectData p : saved) {
            if (!id.equals(p.id)) arr.put(projectToJson(p));
        }
        prefs.edit().putString(KEY_SAVED, arr.toString()).apply();
    }

    String exportBackup() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("app", "ceilingMeasure");
            payload.put("version", 184);
            payload.put("exportedAt", System.currentTimeMillis());

            String current = prefs.getString(KEY_CURRENT, null);
            payload.put("current", current == null ? JSONObject.NULL : new JSONObject(current));

            JSONArray saved = new JSONArray(prefs.getString(KEY_SAVED, "[]"));
            payload.put("saved", saved);
        } catch (Exception ignored) {
        }
        return payload.toString();
    }

    boolean importBackup(String raw) {
        try {
            JSONObject payload = new JSONObject(raw);
            JSONArray saved = payload.optJSONArray("saved");
            JSONObject current = payload.optJSONObject("current");

            // Native backup format.
            if (saved != null) {
                for (int i = 0; i < saved.length(); i++) {
                    projectFromJson(saved.getJSONObject(i));
                }
                SharedPreferences.Editor editor = prefs.edit()
                        .putString(KEY_SAVED, saved.toString());
                if (current != null) {
                    projectFromJson(current);
                    editor.putString(KEY_CURRENT, current.toString());
                } else if (saved.length() > 0) {
                    editor.putString(KEY_CURRENT, saved.getJSONObject(0).toString());
                }
                editor.apply();
                return true;
            }

            // Accept the web backup envelope when its orders already use a
            // compatible graph format. Unsupported room fields are ignored.
            JSONObject db = payload.optJSONObject("db");
            if (db == null) db = payload;
            JSONArray orders = db.optJSONArray("orders");
            if (orders == null) return false;

            JSONArray converted = new JSONArray();
            for (int i = 0; i < orders.length(); i++) {
                JSONObject web = orders.getJSONObject(i);
                JSONObject nativeProject = new JSONObject();
                nativeProject.put("geometryVersion", 3);
                nativeProject.put("id", web.optString("id", newId("project")));
                nativeProject.put("name", web.optString("name", ""));
                nativeProject.put("phone", web.optString("phone", ""));
                nativeProject.put("address", web.optString("address", ""));
                nativeProject.put("updatedAt", System.currentTimeMillis());

                long savedAt = System.currentTimeMillis();
                String savedText = web.optString("savedAt", "");
                if (!savedText.isEmpty()) {
                    try {
                        savedAt = java.time.Instant.parse(savedText).toEpochMilli();
                    } catch (Exception ignored) {
                    }
                }
                nativeProject.put("savedAt", savedAt);
                nativeProject.put("activeRoom", 0);

                JSONArray rooms = web.optJSONArray("rooms");
                JSONArray nativeRooms = new JSONArray();
                if (rooms != null) {
                    for (int j = 0; j < rooms.length(); j++) {
                        JSONObject wr = rooms.getJSONObject(j);
                        JSONObject nr = new JSONObject();
                        nr.put("geometryVersion", 3);
                        nr.put("id", wr.optString("id", newId("room")));
                        nr.put("name", wr.optString("name", "Комната " + (j + 1)));
                        nr.put("point90Magnet", wr.optBoolean("point90Magnet", true));
                        nr.put("wallAngleMagnet", true);
                        nr.put("showAutoLetters", wr.optBoolean("showAutoLetters", false));
                        nr.put("geometryLocked", wr.optBoolean("geometryLocked", false));
                        nr.put("hintCollapsed", wr.optBoolean("hintCollapsed", true));

                        JSONObject view = wr.optJSONObject("view");
                        nr.put("viewScale", view == null ? 1d : view.optDouble("scale", 1d));
                        nr.put("panX", view == null ? 0d : view.optDouble("panX", 0d));
                        nr.put("panY", view == null ? 0d : view.optDouble("panY", 0d));

                        JSONArray points = wr.optJSONArray("points");
                        nr.put("points", points == null ? new JSONArray() : points);

                        JSONArray edges = new JSONArray();
                        JSONArray webEdges = wr.optJSONArray("edges");
                        if (webEdges != null) {
                            for (int k = 0; k < webEdges.length(); k++) {
                                JSONObject we = webEdges.getJSONObject(k);
                                JSONObject ne = new JSONObject();
                                ne.put("id", we.optString("id", newId("e")));
                                ne.put("a", we.optString("a", ""));
                                ne.put("b", we.optString("b", ""));
                                ne.put("value", parseWebMeasurement(we.opt("mm")));
                                JSONObject lp = we.optJSONObject("labelPose");
                                if (lp != null) {
                                    ne.put("labelAngle", lp.optDouble("angle", 0));
                                    ne.put("labelSize", lp.optDouble("size", 1));
                                    ne.put("labelManual", lp.optBoolean("manual", false));
                                }
                                edges.put(ne);
                            }
                        }
                        nr.put("edges", edges);

                        JSONArray diagonals = new JSONArray();
                        JSONArray webDiags = wr.optJSONArray("diagList");
                        if (webDiags != null) {
                            for (int k = 0; k < webDiags.length(); k++) {
                                JSONObject wd = webDiags.getJSONObject(k);
                                JSONObject nd = new JSONObject();
                                nd.put("id", wd.optString("id", newId("d")));
                                nd.put("a", wd.optString("a", ""));
                                nd.put("b", wd.optString("b", ""));
                                nd.put("value", parseWebMeasurement(wd.opt("mm")));
                                JSONObject lp = wd.optJSONObject("labelPose");
                                if (lp != null) {
                                    nd.put("labelAngle", lp.optDouble("angle", 0));
                                    nd.put("labelSize", lp.optDouble("size", 1));
                                    nd.put("labelManual", lp.optBoolean("manual", false));
                                }
                                diagonals.put(nd);
                            }
                        }
                        nr.put("diagonals", diagonals);
                        nativeRooms.put(nr);
                    }
                }
                nativeProject.put("rooms", nativeRooms);
                projectFromJson(nativeProject);
                converted.put(nativeProject);
            }

            SharedPreferences.Editor editor = prefs.edit()
                    .putString(KEY_SAVED, converted.toString());
            if (converted.length() > 0) {
                editor.putString(KEY_CURRENT, converted.getJSONObject(0).toString());
            }
            editor.apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static float parseWebMeasurement(Object value) {
        if (value == null || value == JSONObject.NULL) return 0f;
        try {
            String raw = String.valueOf(value).trim().replace(',', '.');
            if (raw.isEmpty()) return 0f;
            return Math.max(0f, Float.parseFloat(raw));
        } catch (Exception ignored) {
            return 0f;
        }
    }

    private ProjectData cloneProject(ProjectData p) {
        try {
            return projectFromJson(projectToJson(p));
        } catch (Exception e) {
            return p;
        }
    }

    private JSONObject projectToJson(ProjectData p) {
        JSONObject o = new JSONObject();
        try {
            o.put("geometryVersion", 3);
            o.put("id", p.id);
            o.put("name", p.name);
            o.put("phone", p.phone);
            o.put("address", p.address);
            o.put("updatedAt", p.updatedAt);
            o.put("savedAt", p.savedAt);
            o.put("activeRoom", p.activeRoom);
            JSONArray rooms = new JSONArray();
            for (RoomData r : p.rooms) rooms.put(roomToJson(r));
            o.put("rooms", rooms);
        } catch (JSONException ignored) {
        }
        return o;
    }

    private static JSONObject roomToJson(RoomData r) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("geometryVersion", 3);
        o.put("id", r.id);
        o.put("name", r.name);
        o.put("point90Magnet", r.point90Magnet);
        o.put("wallAngleMagnet", r.wallAngleMagnet);
        o.put("showAutoLetters", r.showAutoLetters);
        o.put("geometryLocked", r.geometryLocked);
        o.put("hintCollapsed", r.hintCollapsed);
        o.put("viewScale", r.viewScale);
        o.put("panX", r.panX);
        o.put("panY", r.panY);

        JSONArray points = new JSONArray();
        for (PointData p : r.points) {
            JSONObject j = new JSONObject();
            j.put("id", p.id);
            j.put("x", p.x);
            j.put("y", p.y);
            if (p.customLabel != null && !p.customLabel.isEmpty()) {
                j.put("customLabel", p.customLabel);
            }
            points.put(j);
        }
        o.put("points", points);

        JSONArray edges = new JSONArray();
        for (EdgeData e : r.edges) {
            JSONObject j = new JSONObject();
            j.put("id", e.id);
            j.put("a", e.a);
            j.put("b", e.b);
            j.put("value", e.value);
            j.put("labelDx", e.labelDx);
            j.put("labelDy", e.labelDy);
            j.put("labelAngle", e.labelAngle);
            j.put("labelSize", e.labelSize);
            j.put("labelManual", e.labelManual);
            edges.put(j);
        }
        o.put("edges", edges);

        JSONArray diagonals = new JSONArray();
        for (DiagonalData d : r.diagonals) {
            JSONObject j = new JSONObject();
            j.put("id", d.id);
            j.put("a", d.a);
            j.put("b", d.b);
            j.put("value", d.value);
            j.put("labelDx", d.labelDx);
            j.put("labelDy", d.labelDy);
            j.put("labelAngle", d.labelAngle);
            j.put("labelSize", d.labelSize);
            j.put("labelManual", d.labelManual);
            diagonals.put(j);
        }
        o.put("diagonals", diagonals);
        return o;
    }

    private ProjectData projectFromJson(JSONObject o) throws JSONException {
        ProjectData p = new ProjectData();
        p.id = o.optString("id", p.id);
        p.name = o.optString("name", "");
        p.phone = o.optString("phone", "");
        p.address = o.optString("address", "");
        p.updatedAt = o.optLong("updatedAt", System.currentTimeMillis());
        p.savedAt = o.optLong("savedAt", p.updatedAt);
        p.activeRoom = o.optInt("activeRoom", 0);
        p.rooms.clear();
        JSONArray rooms = o.optJSONArray("rooms");
        if (rooms != null) {
            for (int i = 0; i < rooms.length(); i++) p.rooms.add(roomFromJson(rooms.getJSONObject(i)));
        }
        if (p.rooms.isEmpty()) p.rooms.add(new RoomData());
        p.activeRoom = Math.max(0, Math.min(p.activeRoom, p.rooms.size() - 1));
        return p;
    }

    private static RoomData roomFromJson(JSONObject o) throws JSONException {
        RoomData r = new RoomData();
        r.id = o.optString("id", r.id);
        r.name = o.optString("name", r.name);
        r.point90Magnet = o.optBoolean("point90Magnet", true);
        r.wallAngleMagnet = o.optBoolean("wallAngleMagnet", true);
        r.showAutoLetters = o.optBoolean("showAutoLetters", false);
        r.geometryLocked = o.optBoolean("geometryLocked", false);
        r.hintCollapsed = o.optBoolean("hintCollapsed", false);
        r.viewScale = clamp((float) o.optDouble("viewScale", 1d), .45f, 12f);
        r.panX = (float) o.optDouble("panX", 0d);
        r.panY = (float) o.optDouble("panY", 0d);

        JSONArray points = o.optJSONArray("points");
        boolean graphFormat = points != null
                && points.length() > 0
                && points.opt(0) instanceof JSONObject;

        if (graphFormat || o.optJSONArray("edges") != null) {
            readGraphRoom(o, r);
        } else {
            readLegacyNativeRoom(o, r);
        }

        NativeGeometry.sanitize(r);
        return r;
    }

    private static void readGraphRoom(JSONObject o, RoomData r) throws JSONException {
        r.points.clear();
        JSONArray points = o.optJSONArray("points");
        if (points != null) {
            for (int i = 0; i < points.length(); i++) {
                JSONObject j = points.getJSONObject(i);
                PointData point = new PointData(
                        j.optString("id", newId("p")),
                        (float) j.optDouble("x", 0),
                        (float) j.optDouble("y", 0));
                point.customLabel = "A".equals(j.optString("customLabel", ""))
                        ? "A"
                        : "";
                r.points.add(point);
            }
        }

        r.edges.clear();
        JSONArray edges = o.optJSONArray("edges");
        if (edges != null) {
            for (int i = 0; i < edges.length(); i++) {
                JSONObject j = edges.getJSONObject(i);
                EdgeData e = new EdgeData(
                        j.optString("id", newId("e")),
                        j.optString("a", ""),
                        j.optString("b", ""),
                        (float) j.optDouble("value", 0));
                e.labelDx = (float) j.optDouble("labelDx", 0);
                e.labelDy = (float) j.optDouble("labelDy", 0);
                e.labelAngle = normalizeAngle((float) j.optDouble("labelAngle", 0));
                e.labelSize = clamp((float) j.optDouble("labelSize", 1), .55f, 2.5f);
                e.labelManual = j.optBoolean(
                        "labelManual",
                        Math.abs(e.labelDx) > .01f || Math.abs(e.labelDy) > .01f);
                r.edges.add(e);
            }
        }

        r.diagonals.clear();
        JSONArray diagonals = o.optJSONArray("diagonals");
        if (diagonals != null) {
            for (int i = 0; i < diagonals.length(); i++) {
                JSONObject j = diagonals.getJSONObject(i);
                DiagonalData d = new DiagonalData(
                        j.optString("id", newId("d")),
                        j.optString("a", ""),
                        j.optString("b", ""),
                        (float) j.optDouble("value", 0));
                d.labelDx = (float) j.optDouble("labelDx", 0);
                d.labelDy = (float) j.optDouble("labelDy", 0);
                d.labelAngle = normalizeAngle((float) j.optDouble("labelAngle", 0));
                d.labelSize = clamp((float) j.optDouble("labelSize", 1), .55f, 2.5f);
                d.labelManual = j.optBoolean(
                        "labelManual",
                        Math.abs(d.labelDx) > .01f || Math.abs(d.labelDy) > .01f);
                r.diagonals.add(d);
            }
        }
    }

    private static void readLegacyNativeRoom(JSONObject o, RoomData r) throws JSONException {
        r.points.clear();
        JSONArray points = o.optJSONArray("points");
        if (points != null) {
            for (int i = 0; i < points.length(); i++) {
                JSONArray xy = points.optJSONArray(i);
                if (xy != null) {
                    r.points.add(new PointData(
                            (float) xy.optDouble(0, 0),
                            (float) xy.optDouble(1, 0)));
                }
            }
        }

        JSONArray walls = o.optJSONArray("wallValues");
        JSONArray labelDx = o.optJSONArray("labelDx");
        JSONArray labelDy = o.optJSONArray("labelDy");
        boolean closed = o.optBoolean("closed", false);

        r.edges.clear();
        int edgeCount = closed ? r.points.size() : Math.max(0, r.points.size() - 1);
        for (int i = 0; i < edgeCount; i++) {
            PointData a = r.points.get(i);
            PointData b = r.points.get((i + 1) % r.points.size());
            EdgeData edge = new EdgeData(a.id, b.id);
            edge.value = walls == null ? 0f : (float) walls.optDouble(i, 0);
            edge.labelDx = labelDx == null ? 0f : (float) labelDx.optDouble(i, 0);
            edge.labelDy = labelDy == null ? 0f : (float) labelDy.optDouble(i, 0);
            edge.labelManual = Math.abs(edge.labelDx) > .01f || Math.abs(edge.labelDy) > .01f;
            r.edges.add(edge);
        }

        r.diagonals.clear();
        JSONArray diagonals = o.optJSONArray("diagonals");
        if (diagonals != null) {
            for (int i = 0; i < diagonals.length(); i++) {
                JSONObject j = diagonals.optJSONObject(i);
                if (j == null) continue;
                int ai = j.optInt("a", -1);
                int bi = j.optInt("b", -1);
                if (ai < 0 || bi < 0 || ai >= r.points.size() || bi >= r.points.size()) continue;
                DiagonalData d = new DiagonalData(r.points.get(ai).id, r.points.get(bi).id);
                d.value = (float) j.optDouble("value", 0);
                r.diagonals.add(d);
            }
        }
    }

    static String snapshotRoom(@NonNull RoomData room) {
        try {
            return roomToJson(room).toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @NonNull
    static RoomData roomFromSnapshot(String raw) {
        try {
            return roomFromJson(new JSONObject(raw));
        } catch (Exception e) {
            return new RoomData();
        }
    }

    @Nullable
    static PointData pointById(RoomData r, String id) {
        if (r == null || id == null) return null;
        for (PointData p : r.points) if (id.equals(p.id)) return p;
        return null;
    }

    static float normalizeAngle(float value) {
        float angle = value % 360f;
        if (angle < 0f) angle += 360f;
        return Math.round(angle * 10f) / 10f;
    }

    static float normalizedLabelSize(float value) {
        return Math.round(clamp(value, .55f, 2.5f) * 100f) / 100f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
