package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

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

    static final class PointData {
        float x;
        float y;

        PointData(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    static final class DiagonalData {
        int a;
        int b;
        float value;

        DiagonalData(int a, int b, float value) {
            this.a = a;
            this.b = b;
            this.value = value;
        }
    }

    static final class RoomData {
        String id = UUID.randomUUID().toString();
        String name = "Комната 1";
        final ArrayList<PointData> points = new ArrayList<>();
        final ArrayList<Float> wallValues = new ArrayList<>();
        final ArrayList<Float> labelDx = new ArrayList<>();
        final ArrayList<Float> labelDy = new ArrayList<>();
        final ArrayList<DiagonalData> diagonals = new ArrayList<>();
        boolean closed;
    }

    static final class ProjectData {
        String id = UUID.randomUUID().toString();
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
        o.put("id", r.id);
        o.put("name", r.name);
        o.put("closed", r.closed);

        JSONArray points = new JSONArray();
        for (PointData p : r.points) {
            JSONArray xy = new JSONArray();
            xy.put((double) p.x);
            xy.put((double) p.y);
            points.put(xy);
        }
        o.put("points", points);

        JSONArray walls = new JSONArray();
        for (Float v : r.wallValues) walls.put((double) v);
        o.put("wallValues", walls);

        JSONArray dx = new JSONArray();
        for (Float v : r.labelDx) dx.put((double) v);
        o.put("labelDx", dx);

        JSONArray dy = new JSONArray();
        for (Float v : r.labelDy) dy.put((double) v);
        o.put("labelDy", dy);

        JSONArray diagonals = new JSONArray();
        for (DiagonalData d : r.diagonals) {
            JSONObject j = new JSONObject();
            j.put("a", d.a);
            j.put("b", d.b);
            j.put("value", (double) d.value);
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
        r.closed = o.optBoolean("closed", false);

        r.points.clear();
        JSONArray points = o.optJSONArray("points");
        if (points != null) {
            for (int i = 0; i < points.length(); i++) {
                JSONArray xy = points.getJSONArray(i);
                r.points.add(new PointData((float) xy.optDouble(0), (float) xy.optDouble(1)));
            }
        }

        copyFloatArray(o.optJSONArray("wallValues"), r.wallValues);
        copyFloatArray(o.optJSONArray("labelDx"), r.labelDx);
        copyFloatArray(o.optJSONArray("labelDy"), r.labelDy);

        r.diagonals.clear();
        JSONArray diagonals = o.optJSONArray("diagonals");
        if (diagonals != null) {
            for (int i = 0; i < diagonals.length(); i++) {
                JSONObject d = diagonals.getJSONObject(i);
                r.diagonals.add(new DiagonalData(
                        d.optInt("a", 0),
                        d.optInt("b", 0),
                        (float) d.optDouble("value", 0)));
            }
        }
        normalizeArrays(r);
        return r;
    }

    static void normalizeArrays(RoomData r) {
        int edges = r.closed ? r.points.size() : Math.max(0, r.points.size() - 1);
        while (r.wallValues.size() < edges) r.wallValues.add(0f);
        while (r.labelDx.size() < edges) r.labelDx.add(0f);
        while (r.labelDy.size() < edges) r.labelDy.add(0f);
        while (r.wallValues.size() > edges) r.wallValues.remove(r.wallValues.size() - 1);
        while (r.labelDx.size() > edges) r.labelDx.remove(r.labelDx.size() - 1);
        while (r.labelDy.size() > edges) r.labelDy.remove(r.labelDy.size() - 1);
    }

    private static void copyFloatArray(JSONArray arr, ArrayList<Float> out) {
        out.clear();
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) out.add((float) arr.optDouble(i, 0));
    }
}
