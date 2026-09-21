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
            o.put("geometryVersion", 2);
            o.put("id", p.id);
            o.put("name", p.name);
            o.put("phone", p.phone);
            o.put("address", p.address);
            o.put("updatedAt", p.updatedAt);
            o.put("activeRoom", p.activeRoom);
            JSONArray rooms = new JSONArray();
            for (RoomData r : p.rooms) rooms.put(roomToJson(r));
            o.put("rooms", rooms);
        } catch (JSONException ignored) {
        }
        return o;
    }

    private JSONObject roomToJson(RoomData r) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("geometryVersion", 2);
        o.put("id", r.id);
        o.put("name", r.name);
        o.put("point90Magnet", r.point90Magnet);
        o.put("wallAngleMagnet", r.wallAngleMagnet);
        o.put("showAutoLetters", r.showAutoLetters);
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

    private RoomData roomFromJson(JSONObject o) throws JSONException {
        RoomData r = new RoomData();
        r.id = o.optString("id", r.id);
        r.name = o.optString("name", r.name);
        r.point90Magnet = o.optBoolean("point90Magnet", true);
        r.wallAngleMagnet = o.optBoolean("wallAngleMagnet", true);
        r.showAutoLetters = o.optBoolean("showAutoLetters", false);
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

    private void readGraphRoom(JSONObject o, RoomData r) throws JSONException {
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
                r.diagonals.add(d);
            }
        }
    }

    private void readLegacyNativeRoom(JSONObject o, RoomData r) throws JSONException {
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

    @Nullable
    static PointData pointById(RoomData r, String id) {
        if (r == null || id == null) return null;
        for (PointData p : r.points) if (id.equals(p.id)) return p;
        return null;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
