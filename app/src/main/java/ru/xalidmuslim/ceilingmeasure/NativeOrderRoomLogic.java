package ru.xalidmuslim.ceilingmeasure;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure Phase-3 Orders/Rooms parity rules extracted from web v184. */
final class NativeOrderRoomLogic {
    static final long LONG_PRESS_MS = 330L;
    static final float LONG_PRESS_CANCEL_PX = 22f;
    static final int ROOM_NAME_MAX = 40;

    static final String[] ROOM_TYPES =
            new String[]{"Комната", "Коридор", "Ванная", "Другое"};

    static final String[][] ROOM_KEY_ROWS = new String[][]{
            {"1","2","3","4","5","6","7","8","9","0"},
            {"й","ц","у","к","е","н","г","ш","щ","з","х"},
            {"ф","ы","в","а","п","р","о","л","д","ж","э"},
            {"я","ч","с","м","и","т","ь","б","ю","ё"}
    };

    private static final Pattern ROOM_NUMBER =
            Pattern.compile("^Комната\\s+(\\d+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private NativeOrderRoomLogic() {}

    @NonNull
    static String orderDisplayName(@Nullable String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "Без имени" : trimmed;
    }

    @NonNull
    static String nextRoomTypeName(
            @NonNull List<NativeProjectStore.RoomData> rooms,
            @NonNull String base) {
        if ("Комната".equals(base)) {
            int max = 0;
            for (NativeProjectStore.RoomData room : rooms) {
                Matcher m = ROOM_NUMBER.matcher(room.name == null ? "" : room.name);
                if (!m.matches()) continue;
                try {
                    max = Math.max(max, Integer.parseInt(m.group(1)));
                } catch (Exception ignored) {
                }
            }
            return "Комната " + (max + 1);
        }

        int exact = 0;
        int maxNumber = 0;
        Pattern numbered = Pattern.compile(
                "^" + Pattern.quote(base) + "\\s+(\\d+)$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        for (NativeProjectStore.RoomData room : rooms) {
            String name = room.name == null ? "" : room.name;
            if (base.equals(name)) exact++;
            Matcher m = numbered.matcher(name);
            if (m.matches()) {
                try {
                    maxNumber = Math.max(maxNumber, Integer.parseInt(m.group(1)));
                } catch (Exception ignored) {
                }
            }
        }
        if (exact == 0 && maxNumber == 0) return base;
        return base + " " + (Math.max(1, maxNumber) + 1);
    }

    @NonNull
    static String nextDuplicateName(
            @NonNull List<NativeProjectStore.RoomData> rooms,
            @Nullable String sourceName) {
        String root = (sourceName == null ? "" : sourceName) + " копия";
        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (NativeProjectStore.RoomData room : rooms) {
            names.add(room.name == null ? "" : room.name);
        }
        if (!names.contains(root)) return root;
        int number = 2;
        while (names.contains(root + " " + number)) number++;
        return root + " " + number;
    }

    @NonNull
    static String applyRoomNameKey(
            @Nullable String current,
            @Nullable String key,
            @Nullable String action) {
        String value = current == null ? "" : current;

        if (key != null) {
            if (value.length() >= ROOM_NAME_MAX) return value;
            String out = key;
            if (isRussianLetter(out)
                    && (value.trim().isEmpty()
                    || endsWithWhitespaceOrDash(value))) {
                out = out.toUpperCase(new Locale("ru"));
            }
            return value + out;
        }

        if ("space".equals(action)) {
            if (!value.isEmpty()
                    && value.length() < ROOM_NAME_MAX
                    && !value.endsWith(" ")) {
                return value + " ";
            }
            return value;
        }
        if ("dash".equals(action)) {
            return value.length() < ROOM_NAME_MAX ? value + "-" : value;
        }
        if ("back".equals(action)) {
            return value.isEmpty() ? value : value.substring(0, value.length() - 1);
        }
        if ("clear".equals(action)) return "";
        return value;
    }

    @NonNull
    static NativeProjectStore.RoomData duplicateRoom(
            @NonNull NativeProjectStore.RoomData source,
            @NonNull List<NativeProjectStore.RoomData> rooms,
            @NonNull String newId) {
        NativeProjectStore.RoomData copy = NativeProjectStore.cloneRoomExact(source);
        copy.id = newId;
        copy.name = nextDuplicateName(rooms, source.name);
        copy.savedAt = 0L;
        copy.geometryLocked = false;
        return copy;
    }

    static boolean shouldCancelLongPress(float startX, float startY, float x, float y) {
        return Math.hypot(x - startX, y - startY) > LONG_PRESS_CANCEL_PX;
    }

    static boolean roomReady(@Nullable NativeProjectStore.RoomData room) {
        return room != null
                && !room.edges.isEmpty()
                && NativeMeasurementValidator.validate(room).ok();
    }

    private static boolean endsWithWhitespaceOrDash(String value) {
        if (value.isEmpty()) return false;
        char last = value.charAt(value.length() - 1);
        return Character.isWhitespace(last) || last == '-';
    }

    private static boolean isRussianLetter(String value) {
        return value != null
                && value.length() == 1
                && "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
                    .contains(value.toLowerCase(new Locale("ru")));
    }
}
