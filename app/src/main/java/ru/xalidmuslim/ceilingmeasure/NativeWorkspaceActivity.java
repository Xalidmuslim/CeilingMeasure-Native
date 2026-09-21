package ru.xalidmuslim.ceilingmeasure;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import android.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class NativeWorkspaceActivity extends ComponentActivity implements CeilingDrawingView.Listener {
    public static final String EXTRA_LAUNCH_ACTION = "native_launch_action";
    public static final String ACTION_NEW_ORDER = "new_order";
    public static final String ACTION_SAVED_MEASUREMENTS = "saved_measurements";
    public static final String ACTION_DRAWING = "drawing";
    public static final String ACTION_SETTINGS = "settings";

    private static final int BG = Color.rgb(244,247,249);
    private static final int PANEL = Color.WHITE;
    private static final int TEXT = Color.rgb(35,56,72);
    private static final int MUTED = Color.rgb(116,136,151);
    private static final int BLUE = Color.rgb(47,143,190);
    private static final int LINE = Color.rgb(220,229,235);
    private static final int DANGER = Color.rgb(190,72,79);

    private NativeProjectStore store;
    private NativeProjectStore.ProjectData project;
    private LinearLayout root;
    private LinearLayout roomRow;
    private CeilingDrawingView drawingView;
    private TextView areaValue;
    private TextView perimeterValue;
    private Button wallMode;
    private Button pointMode;
    private Button diagonalMode;
    private Button labelMode;
    private EditText nameInput;
    private EditText phoneInput;
    private EditText addressInput;
    private TextView drawingRoomTitle;
    private TextView drawingStatus;
    private TextView zoomStatus;
    private TextView drawingHint;
    private ScrollView editorScroll;
    private final ArrayDeque<String> undoRooms = new ArrayDeque<>();
    private final ArrayDeque<String> redoRooms = new ArrayDeque<>();
    private boolean restoringHistory;
    private NativeUiPrefs uiPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new NativeProjectStore(this);
        uiPrefs = NativeUiPrefs.load(this);

        String action = getIntent().getStringExtra(EXTRA_LAUNCH_ACTION);
        if (ACTION_NEW_ORDER.equals(action)) project = store.newProject();
        else project = store.loadCurrent();

        if (ACTION_SAVED_MEASUREMENTS.equals(action)) {
            showSavedMeasurements();
        } else if (ACTION_SETTINGS.equals(action)) {
            showNativeSettings();
        } else {
            showEditor(ACTION_DRAWING.equals(action));
        }
    }

    private void showEditor(boolean jumpToDrawing) {
        undoRooms.clear();
        redoRooms.clear();

        FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(BG);

        editorScroll = new ScrollView(this);
        editorScroll.setFillViewport(true);
        editorScroll.setBackgroundColor(BG);
        editorScroll.setClipToPadding(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(104));
        editorScroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(editorScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ViewCompat.setOnApplyWindowInsetsListener(page, (view, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            editorScroll.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });

        root.addView(editorTopBar());

        LinearLayout orderCard = card();
        orderCard.setTag("native_order_card");
        root.addView(orderCard, marginParams(0,8));

        nameInput = field(orderCard, "Имя / объект", project.name, InputType.TYPE_CLASS_TEXT);
        phoneInput = field(orderCard, "Телефон", project.phone, InputType.TYPE_CLASS_PHONE);
        addressInput = field(orderCard, "Адрес", project.address, InputType.TYPE_CLASS_TEXT);

        View.OnFocusChangeListener saver=(v,has)->{if(!has)syncProjectFields();};
        nameInput.setOnFocusChangeListener(saver);
        phoneInput.setOnFocusChangeListener(saver);
        addressInput.setOnFocusChangeListener(saver);

        LinearLayout roomsCard = card();
        root.addView(roomsCard, marginParams(0,8));
        TextView roomsTitle = text("Комнаты",17,TEXT,Typeface.BOLD);
        roomsCard.addView(roomsTitle);

        HorizontalScrollView roomsScroll = new HorizontalScrollView(this);
        roomsScroll.setHorizontalScrollBarEnabled(false);
        roomRow = new LinearLayout(this);
        roomRow.setOrientation(LinearLayout.HORIZONTAL);
        roomsScroll.addView(roomRow, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams rs = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rs.topMargin=dp(8);
        roomsCard.addView(roomsScroll,rs);
        renderRooms();

        LinearLayout drawCard = card();
        drawCard.setTag("native_drawing_card");
        root.addView(drawCard, marginParams(0,8));

        LinearLayout drawingHead = new LinearLayout(this);
        drawingHead.setOrientation(LinearLayout.HORIZONTAL);
        drawingHead.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout leftHead = new LinearLayout(this);
        leftHead.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = text("Чертёж",11,MUTED,Typeface.BOLD);
        drawingRoomTitle = text(activeRoom().name,18,TEXT,Typeface.BOLD);
        leftHead.addView(eyebrow);
        leftHead.addView(drawingRoomTitle);
        drawingHead.addView(leftHead, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        Button check = button("✓",false);
        check.setTag("native_drawing_check");
        check.setContentDescription("Открыть проверку замера");
        check.setOnClickListener(v->showMeasurementValidation());
        drawingHead.addView(check,new LinearLayout.LayoutParams(dp(42),dp(38)));
        drawCard.addView(drawingHead);

        LinearLayout modeBar = new LinearLayout(this);
        modeBar.setOrientation(LinearLayout.HORIZONTAL);
        modeBar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mbp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mbp.topMargin=dp(8);
        drawCard.addView(modeBar,mbp);

        wallMode = modeButton("Стена", CeilingDrawingView.Mode.WALL, "native_mode_wall");
        pointMode = modeButton("Точки", CeilingDrawingView.Mode.POINT, "native_mode_points");
        diagonalMode = modeButton("Диагональ", CeilingDrawingView.Mode.DIAGONAL, "native_mode_diagonal");
        labelMode = modeButton("Цифры", CeilingDrawingView.Mode.LABEL, "native_mode_labels");
        modeBar.addView(wallMode, weightParams());
        modeBar.addView(pointMode, weightParams());
        modeBar.addView(diagonalMode, weightParams());
        modeBar.addView(labelMode, weightParams());

        FrameLayout canvasWrap = new FrameLayout(this);
        canvasWrap.setTag("native_canvas_wrap");
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(470));
        canvasParams.topMargin = dp(8);
        drawCard.addView(canvasWrap, canvasParams);

        drawingView = new CeilingDrawingView(this);
        drawingView.setTag("native_drawing_view");
        drawingView.setListener(this);
        drawingView.setRoom(activeRoom());
        canvasWrap.addView(drawingView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout zoom = new LinearLayout(this);
        zoom.setOrientation(LinearLayout.VERTICAL);
        zoom.setBackground(rounded(Color.argb(242,255,255,255),LINE,12,1));
        String[] zoomLabels={"+","−","⊙"};
        for(String z:zoomLabels){
            Button zb=button(z,false);
            zb.setTextSize(18);
            if("+".equals(z))zb.setOnClickListener(v->{drawingView.zoomBy(1.16f);updateDrawingChrome();});
            else if("−".equals(z))zb.setOnClickListener(v->{drawingView.zoomBy(1f/1.16f);updateDrawingChrome();});
            else zb.setOnClickListener(v->{drawingView.fitView();updateDrawingChrome();});
            zoom.addView(zb,new LinearLayout.LayoutParams(dp(42),dp(40)));
        }
        FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(
                dp(44),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END|Gravity.BOTTOM);
        zoomParams.rightMargin=dp(8);
        zoomParams.bottomMargin=dp(8);
        canvasWrap.addView(zoom,zoomParams);

        drawingHint = text("",11,MUTED,Typeface.NORMAL);
        drawingHint.setTag("native_drawing_hint");
        drawingHint.setPadding(dp(9),dp(7),dp(9),dp(7));
        drawingHint.setBackground(rounded(Color.argb(238,255,255,255),LINE,10,1));
        drawingHint.setOnClickListener(v->{
            NativeProjectStore.RoomData r=activeRoom();
            r.hintCollapsed=!r.hintCollapsed;
            store.saveCurrent(project);
            updateDrawingChrome();
        });
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                dp(230),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START|Gravity.TOP);
        hintParams.leftMargin=dp(8);
        hintParams.topMargin=dp(8);
        canvasWrap.addView(drawingHint,hintParams);

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin=dp(6);
        drawCard.addView(status,statusParams);
        drawingStatus=text("",11,MUTED,Typeface.NORMAL);
        zoomStatus=text("100%",11,TEXT,Typeface.BOLD);
        status.addView(drawingStatus,new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));
        status.addView(zoomStatus);

        LinearLayout saveRow = new LinearLayout(this);
        saveRow.setOrientation(LinearLayout.HORIZONTAL);
        saveRow.setGravity(Gravity.CENTER_VERTICAL);
        saveRow.setPadding(0,dp(9),0,0);
        LinearLayout saveCopy=new LinearLayout(this);
        saveCopy.setOrientation(LinearLayout.VERTICAL);
        TextView saveRoom=text(activeRoom().name,13,TEXT,Typeface.BOLD);
        saveRoom.setTag("native_save_room_name");
        TextView saveMeta=text("Изменения сохраняются автоматически",10,MUTED,Typeface.NORMAL);
        saveCopy.addView(saveRoom);
        saveCopy.addView(saveMeta);
        saveRow.addView(saveCopy,new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));
        Button save=button("✓ Завершить замер",true);
        save.setTag("native_save_measurement");
        save.setOnClickListener(v->{
            syncProjectFields();
            store.saveSnapshot(project);
            Toast.makeText(this,"Замер завершён",Toast.LENGTH_SHORT).show();
        });
        saveRow.addView(save,new LinearLayout.LayoutParams(dp(170),dp(44)));
        drawCard.addView(saveRow);

        LinearLayout estimateCard=card();
        estimateCard.setTag("native_estimate_card");
        root.addView(estimateCard,marginParams(0,8));
        TextView estimateEyebrow=text("Смета",11,MUTED,Typeface.BOLD);
        TextView estimateTitle=text("Для клиента",18,TEXT,Typeface.BOLD);
        estimateCard.addView(estimateEyebrow);
        estimateCard.addView(estimateTitle);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin=dp(9);
        estimateCard.addView(stats,sp);
        areaValue = stat(stats,"Площадь");
        perimeterValue = stat(stats,"Периметр");

        page.addView(createHistoryNav(), historyNavParams());

        setContentView(page);
        page.requestApplyInsets();
        setMode(CeilingDrawingView.Mode.WALL);
        updateStats();
        updateDrawingChrome();

        if (jumpToDrawing) {
            editorScroll.post(() -> drawCard.requestFocus());
            editorScroll.postDelayed(
                    () -> editorScroll.smoothScrollTo(0, drawCard.getTop()),
                    120);
        }
    }

    private LinearLayout editorTopBar(){
        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0,dp(4),0,dp(8));

        LinearLayout brand=new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView main=text("Натяжные потолки",17,TEXT,Typeface.BOLD);
        TextView sub=text("замер · расчёт · смета",10,MUTED,Typeface.NORMAL);
        brand.addView(main);
        brand.addView(sub);
        bar.addView(brand,new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        Button home=button("⌂",false);
        home.setTag("native_top_home");
        home.setContentDescription("Главная");
        home.setOnClickListener(v->finish());
        bar.addView(home,new LinearLayout.LayoutParams(dp(40),dp(40)));

        Button saved=button("▣\nЗамеры",false);
        saved.setTag("native_top_saved");
        saved.setTextSize(uiPrefs.textSize(9));
        saved.setOnClickListener(v->{
            syncProjectFields();
            showSavedMeasurements();
        });
        bar.addView(saved,new LinearLayout.LayoutParams(dp(58),dp(40)));

        Button fresh=button("+\nЗаказ",true);
        fresh.setTag("native_top_new_order");
        fresh.setTextSize(uiPrefs.textSize(9));
        fresh.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("Новый заказ?")
                .setMessage("Текущий замер сохранён автоматически.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Создать",(d,w)->{
                    syncProjectFields();
                    store.saveCurrent(project);
                    project=store.newProject();
                    showEditor(false);
                }).show());
        bar.addView(fresh,new LinearLayout.LayoutParams(dp(58),dp(40)));

        Button settings=button("☷",false);
        settings.setTag("native_top_settings");
        settings.setContentDescription("Настройки");
        settings.setOnClickListener(v->showNativeSettings());
        bar.addView(settings,new LinearLayout.LayoutParams(dp(40),dp(40)));
        return bar;
    }

    private LinearLayout topBar(String title) {
        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0,dp(4),0,dp(8));

        Button back=button("‹",false);
        back.setTextSize(28);
        back.setContentDescription("Назад");
        back.setOnClickListener(v->finish());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(48),dp(44));
        bar.addView(back,bp);

        TextView t=text(title,20,TEXT,Typeface.BOLD);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        tp.leftMargin=dp(8);
        bar.addView(t,tp);
        return bar;
    }

    private void renderRooms() {
        roomRow.removeAllViews();
        for(int i=0;i<project.rooms.size();i++){
            final int index=i;
            NativeProjectStore.RoomData r=project.rooms.get(i);
            Button b=button(roomButtonLabel(r.name,index),false);
            b.setTag("native_room_"+index);
            b.setTextSize(11);
            b.setSingleLine(true);
            b.setEllipsize(android.text.TextUtils.TruncateAt.END);
            b.setMinWidth(0);
            b.setPadding(dp(5),0,dp(5),0);
            if(i==project.activeRoom) styleSelected(b);
            b.setOnClickListener(v->{
                project.activeRoom=index;
                store.saveCurrent(project);
                renderRooms();
                undoRooms.clear();
                redoRooms.clear();
                if(drawingView!=null)drawingView.setRoom(activeRoom());
                if(drawingRoomTitle!=null)drawingRoomTitle.setText(activeRoom().name);
                updateStats();
                updateDrawingChrome();
            });
            b.setOnLongClickListener(v->{
                showRoomActions(index);
                return true;
            });
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(58),dp(48));
            p.rightMargin=dp(5);
            roomRow.addView(b,p);
        }
        Button add=button("+",false);
        add.setTag("native_room_add");
        add.setTextSize(22);
        add.setContentDescription("Добавить комнату");
        add.setOnClickListener(v->showRoomPicker());
        LinearLayout.LayoutParams addParams=new LinearLayout.LayoutParams(dp(48),dp(48));
        roomRow.addView(add,addParams);
    }

    private String roomButtonLabel(String name,int index){
        String value=name==null?"":name.trim();
        if(value.isEmpty())return String.valueOf(index+1);
        if(value.matches("(?i)комната\\s*\\d+"))return String.valueOf(index+1);
        return value;
    }

    private void showRoomActions(int index){
        if(index<0||index>=project.rooms.size())return;
        NativeProjectStore.RoomData room=project.rooms.get(index);
        new AlertDialog.Builder(this)
                .setTitle(room.name)
                .setItems(new String[]{"Переименовать","Дублировать","Удалить"},(dialog,which)->{
                    if(which==0){
                        showRoomRename(index);
                    }else if(which==1){
                        duplicateRoom(index);
                    }else{
                        deleteRoom(index);
                    }
                })
                .show();
    }

    private void showRoomPicker(){
        new NativeRoomPickerDialog(this,this::addRoomOfType).show();
    }

    private void addRoomOfType(String type){
        NativeProjectStore.RoomData room=new NativeProjectStore.RoomData();
        int same=0;
        for(NativeProjectStore.RoomData existing:project.rooms){
            if(existing.name!=null&&existing.name.startsWith(type))same++;
        }
        room.name="Другое".equals(type)
                ?"Помещение "+(project.rooms.size()+1)
                :type+" "+(same+1);
        project.rooms.add(room);
        project.activeRoom=project.rooms.size()-1;
        store.saveCurrent(project);
        undoRooms.clear();
        redoRooms.clear();
        renderRooms();
        if(drawingView!=null)drawingView.setRoom(activeRoom());
        updateStats();
        updateDrawingChrome();
    }

    private void duplicateRoom(int index){
        if(index<0||index>=project.rooms.size())return;
        NativeProjectStore.RoomData source=project.rooms.get(index);
        NativeProjectStore.RoomData copy=
                NativeProjectStore.roomFromSnapshot(
                        NativeProjectStore.snapshotRoom(source));
        copy.id=NativeProjectStore.newId("room");
        copy.name=source.name+" копия";
        project.rooms.add(index+1,copy);
        project.activeRoom=index+1;
        store.saveCurrent(project);
        undoRooms.clear();
        redoRooms.clear();
        renderRooms();
        if(drawingView!=null)drawingView.setRoom(activeRoom());
        updateStats();
        updateDrawingChrome();
        Toast.makeText(this,"Комната дублирована",Toast.LENGTH_SHORT).show();
    }

    private void showRoomRename(int index){
        if(index<0||index>=project.rooms.size())return;
        NativeProjectStore.RoomData room=project.rooms.get(index);
        NativeTextPadDialog dialog=new NativeTextPadDialog(
                this,
                "Переименовать комнату",
                room.name,
                value->{
                    room.name=value;
                    store.saveCurrent(project);
                    renderRooms();
                });
        dialog.show();
    }

    private void deleteRoom(int index){
        if(index<0||index>=project.rooms.size())return;
        if(project.rooms.size()<=1){
            Toast.makeText(this,"Нельзя удалить единственную комнату",Toast.LENGTH_SHORT).show();
            return;
        }

        String roomName=project.rooms.get(index).name;
        new AlertDialog.Builder(this)
                .setTitle("Удалить комнату?")
                .setMessage(roomName+" и её чертёж будут удалены.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Удалить",(dialog,which)->{
                    project.rooms.remove(index);
                    if(project.activeRoom>index)project.activeRoom--;
                    if(project.activeRoom>=project.rooms.size())project.activeRoom=project.rooms.size()-1;
                    project.activeRoom=Math.max(0,project.activeRoom);
                    store.saveCurrent(project);
                    renderRooms();
                    if(drawingView!=null)drawingView.setRoom(activeRoom());
                    updateStats();
                })
                .show();
    }

    private Button modeButton(String text, CeilingDrawingView.Mode mode, String tag){
        Button b=button(text,false);
        b.setTag(tag);
        b.setTextSize(12);
        b.setOnClickListener(v->setMode(mode));
        return b;
    }

    private void setMode(CeilingDrawingView.Mode mode){
        NativeProjectStore.RoomData room=activeRoom();
        if(room.geometryLocked&&mode!=CeilingDrawingView.Mode.LABEL){
            Toast.makeText(this,"Геометрия зафиксирована",Toast.LENGTH_SHORT).show();
            return;
        }
        if(mode==CeilingDrawingView.Mode.WALL&&NativeGeometry.isClosed(room)){
            Toast.makeText(this,"Контур уже замкнут",Toast.LENGTH_SHORT).show();
            return;
        }
        if(mode==CeilingDrawingView.Mode.WALL)room.showAutoLetters=false;
        drawingView.setMode(mode);
        styleMode(wallMode,mode==CeilingDrawingView.Mode.WALL);
        styleMode(pointMode,mode==CeilingDrawingView.Mode.POINT);
        styleMode(diagonalMode,mode==CeilingDrawingView.Mode.DIAGONAL);
        styleMode(labelMode,mode==CeilingDrawingView.Mode.LABEL);
        store.saveCurrent(project);
        updateDrawingChrome();
    }

    private View createHistoryNav(){
        LinearLayout nav=new LinearLayout(this);
        nav.setTag("native_history_nav");
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(5),dp(4),dp(5),dp(4));
        nav.setElevation(dp(10));
        nav.setBackground(rounded(Color.WHITE,LINE,18,1));

        Button redo=button("↷\nВперёд",false);
        redo.setTag("native_nav_redo");
        redo.setTextSize(10);
        redo.setOnClickListener(v->redoRoom());

        Button more=button("⋯\nЕще",false);
        more.setTag("native_nav_more");
        more.setTextSize(10);
        more.setOnClickListener(v->openDrawTools());

        Button undo=button("↶\nНазад",false);
        undo.setTag("native_nav_undo");
        undo.setTextSize(10);
        undo.setOnClickListener(v->undoRoom());

        nav.addView(redo,new LinearLayout.LayoutParams(dp(78),dp(52)));
        nav.addView(more,new LinearLayout.LayoutParams(dp(78),dp(52)));
        nav.addView(undo,new LinearLayout.LayoutParams(dp(78),dp(52)));
        return nav;
    }

    private FrameLayout.LayoutParams historyNavParams(){
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(
                dp(246),
                dp(62),
                Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        p.bottomMargin=dp(10);
        return p;
    }

    private void openDrawTools(){
        new NativeDrawToolsDialog(
                this,
                activeRoom(),
                this::handleDrawToolAction).show();
    }

    private void handleDrawToolAction(String action){
        NativeProjectStore.RoomData room=activeRoom();
        if(room.geometryLocked
                && !("letters".equals(action)
                || "clear".equals(action)
                || "validate".equals(action)
                || "lock".equals(action)
                || "wallSequence".equals(action))){
            Toast.makeText(this,"Геометрия зафиксирована",Toast.LENGTH_SHORT).show();
            return;
        }

        switch(action){
            case "magnet":
                pushHistory();
                room.wallAngleMagnet=!room.wallAngleMagnet;
                break;
            case "point90":
                pushHistory();
                room.point90Magnet=!room.point90Magnet;
                break;
            case "letters":
                pushHistory();
                room.showAutoLetters=!room.showAutoLetters;
                break;
            case "autoGeometry":
                pushHistory();
                applyGeometrySolution(false);
                updateDrawingChrome();
                return;
            case "auto90":
                pushHistory();
                applyGeometrySolution(true);
                updateDrawingChrome();
                return;
            case "autoDiags":
                pushHistory();
                generateAutoDiagonals();
                updateDrawingChrome();
                return;
            case "validate":
                showMeasurementValidation();
                return;
            case "wallSequence":
                startWallSequence();
                return;
            case "lock":
                pushHistory();
                room.geometryLocked=!room.geometryLocked;
                if(room.geometryLocked)setMode(CeilingDrawingView.Mode.LABEL);
                break;
            case "clear":
                new AlertDialog.Builder(this)
                        .setTitle("Очистить чертёж?")
                        .setMessage("Все линии и диагонали этой комнаты будут удалены.")
                        .setNegativeButton("Отмена",null)
                        .setPositiveButton("Очистить",(d,w)->drawingView.clearDrawing())
                        .show();
                return;
            default:
                return;
        }
        store.saveCurrent(project);
        drawingView.invalidate();
        updateDrawingChrome();
    }

    private void pushHistory(){
        if(restoringHistory)return;
        String snapshot=NativeProjectStore.snapshotRoom(activeRoom());
        if(!undoRooms.isEmpty()&&snapshot.equals(undoRooms.peekLast()))return;
        undoRooms.addLast(snapshot);
        while(undoRooms.size()>80)undoRooms.removeFirst();
        redoRooms.clear();
    }

    private void undoRoom(){
        if(undoRooms.isEmpty()){
            Toast.makeText(this,"Нет действий для отмены",Toast.LENGTH_SHORT).show();
            return;
        }
        restoringHistory=true;
        redoRooms.addLast(NativeProjectStore.snapshotRoom(activeRoom()));
        NativeProjectStore.RoomData restored=
                NativeProjectStore.roomFromSnapshot(undoRooms.removeLast());
        project.rooms.set(project.activeRoom,restored);
        drawingView.setRoom(restored);
        store.saveCurrent(project);
        restoringHistory=false;
        updateStats();
        updateDrawingChrome();
    }

    private void redoRoom(){
        if(redoRooms.isEmpty()){
            Toast.makeText(this,"Нет действий для возврата",Toast.LENGTH_SHORT).show();
            return;
        }
        restoringHistory=true;
        undoRooms.addLast(NativeProjectStore.snapshotRoom(activeRoom()));
        NativeProjectStore.RoomData restored=
                NativeProjectStore.roomFromSnapshot(redoRooms.removeLast());
        project.rooms.set(project.activeRoom,restored);
        drawingView.setRoom(restored);
        store.saveCurrent(project);
        restoringHistory=false;
        updateStats();
        updateDrawingChrome();
    }

    private void updateDrawingChrome(){
        if(drawingView==null)return;
        NativeProjectStore.RoomData room=activeRoom();
        if(drawingRoomTitle!=null)drawingRoomTitle.setText(room.name);
        if(zoomStatus!=null)zoomStatus.setText(drawingView.getZoomPercent()+"%");
        if(drawingStatus!=null)drawingStatus.setText(drawingStatusText(room));
        if(drawingHint!=null){
            if(room.hintCollapsed){
                drawingHint.setText("?");
                ViewGroup.LayoutParams p=drawingHint.getLayoutParams();
                if(p!=null){p.width=dp(40);drawingHint.setLayoutParams(p);}
            }else{
                drawingHint.setText(drawingHintText(room)+"\n×");
                ViewGroup.LayoutParams p=drawingHint.getLayoutParams();
                if(p!=null){p.width=dp(230);drawingHint.setLayoutParams(p);}
            }
        }
        if(wallMode!=null)wallMode.setEnabled(!room.geometryLocked&&!NativeGeometry.isClosed(room));
        if(pointMode!=null)pointMode.setEnabled(!room.geometryLocked);
        if(diagonalMode!=null)diagonalMode.setEnabled(!room.geometryLocked);
        if(labelMode!=null)labelMode.setEnabled(true);
    }

    private String drawingHintText(NativeProjectStore.RoomData room){
        CeilingDrawingView.Mode mode=drawingView.getMode();
        if(mode==CeilingDrawingView.Mode.LABEL){
            return "Цифры размеров\nПеретащи цифру куда нужно. Вращай пальцем за круг или верхний маркер. Размер меняй кнопками − / +; процент по центру сбрасывает к 100%.";
        }
        if(NativeGeometry.isClosed(room)){
            return "Контур замкнут — новые стены заблокированы. Лишние линии после замыкания не создаются. Два пальца двигают и масштабируют чертёж.";
        }
        if(mode==CeilingDrawingView.Mode.DIAGONAL){
            return "Диагональ пальцем\nНажми на один угол, веди пальцем и отпусти на другом.";
        }
        if(mode==CeilingDrawingView.Mode.POINT){
            return "Перемещение углов\nЗажми угол и перетащи его.";
        }
        if(room.edges.isEmpty()&&room.points.size()<=1){
            return "Режим 45°/90°\nПервую стену можно начать с любого места: нажми, проведи пальцем и отпусти. После первой стены рисование начинается только от существующих углов.";
        }
        return "Режим 45°/90°\nПустое место листает страницу вверх/вниз. Новая стена начинается только от существующего угла. Двумя пальцами двигай и масштабируй чертёж.";
    }

    private String drawingStatusText(NativeProjectStore.RoomData room){
        CeilingDrawingView.Mode mode=drawingView.getMode();
        if(mode==CeilingDrawingView.Mode.LABEL)return "Режим цифр: выбери число на чертеже.";
        if(room.geometryLocked)return "Геометрия зафиксирована. Доступны просмотр и расчёт.";
        if(NativeGeometry.isClosed(room))return "Контур замкнут. Площадь рассчитана.";
        if(mode==CeilingDrawingView.Mode.DIAGONAL)return "Веди диагональ к нужному углу.";
        if(mode==CeilingDrawingView.Mode.POINT)return "Зажми угол и перетащи его.";
        return "Рисование стен.";
    }

    private void styleMode(Button b,boolean active){
        GradientDrawable d=rounded(active?BLUE:Color.rgb(248,250,251),active?BLUE:LINE,10,1);
        b.setBackground(d);
        b.setTextColor(active?Color.WHITE:TEXT);
    }

    private void styleSelected(Button b){
        b.setBackground(rounded(Color.rgb(233,246,252),BLUE,10,1));
        b.setTextColor(BLUE);
    }

    private EditText field(LinearLayout parent,String label,String value,int inputType){
        TextView l=text(label,12,MUTED,Typeface.NORMAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin=dp(7);
        parent.addView(l,lp);

        EditText e=new EditText(this);
        e.setText(value);
        e.setTextSize(16);
        e.setTextColor(TEXT);
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setPadding(dp(12),0,dp(12),0);
        e.setBackground(rounded(Color.rgb(249,251,252),LINE,11,1));
        parent.addView(e,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));
        return e;
    }

    private TextView stat(LinearLayout parent,String label){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(10),dp(8),dp(10),dp(8));
        box.setBackground(rounded(Color.rgb(248,250,252),LINE,12,1));
        TextView l=text(label,11,MUTED,Typeface.NORMAL);
        TextView v=text("—",18,TEXT,Typeface.BOLD);
        box.addView(l);
        box.addView(v);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        p.leftMargin=dp(4);p.rightMargin=dp(4);
        parent.addView(box,p);
        return v;
    }

    private void syncProjectFields(){
        if(nameInput!=null)project.name=nameInput.getText().toString().trim();
        if(phoneInput!=null)project.phone=phoneInput.getText().toString().trim();
        if(addressInput!=null)project.address=addressInput.getText().toString().trim();
        store.saveCurrent(project);
    }

    private NativeProjectStore.RoomData activeRoom(){
        if(project.rooms.isEmpty())project.rooms.add(new NativeProjectStore.RoomData());
        project.activeRoom=Math.max(0,Math.min(project.activeRoom,project.rooms.size()-1));
        return project.rooms.get(project.activeRoom);
    }

    NativeProjectStore.RoomData getActiveRoomForTest(){
        return activeRoom();
    }

    @Override public void onChanged(){
        store.saveCurrent(project);
        updateStats();
        updateDrawingChrome();
    }

    @Override public void onHistoryCheckpoint(){
        pushHistory();
    }

    @Override public void onVertexLongPressed(String pointId){
        NativeProjectStore.RoomData r=activeRoom();
        NativeProjectStore.PointData point=NativeGeometry.pointById(r,pointId);
        if(point==null)return;

        String title="Угол";
        if("A".equals(point.customLabel))title+=" A";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(new String[]{"Добавить угол А","Удалить угол"},(dialog,which)->{
                    if(which==0){
                        pushHistory();
                        for(NativeProjectStore.PointData p:r.points)p.customLabel="";
                        point.customLabel="A";
                        store.saveCurrent(project);
                        drawingView.invalidate();
                        Toast.makeText(this,"Угол A добавлен",Toast.LENGTH_SHORT).show();
                    }else{
                        deleteVertex(pointId);
                    }
                })
                .show();
    }

    private void deleteVertex(String pointId){
        NativeProjectStore.RoomData r=activeRoom();
        NativeProjectStore.PointData point=NativeGeometry.pointById(r,pointId);
        if(point==null)return;

        new AlertDialog.Builder(this)
                .setTitle("Удалить угол?")
                .setMessage("Будут удалены все стены и диагонали, связанные с этим углом.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Удалить",(dialog,which)->{
                    pushHistory();
                    r.edges.removeIf(e->e.a.equals(pointId)||e.b.equals(pointId));
                    r.diagonals.removeIf(d->d.a.equals(pointId)||d.b.equals(pointId));
                    r.points.removeIf(p->p.id.equals(pointId));
                    NativeGeometry.sanitize(r);
                    store.saveCurrent(project);
                    drawingView.invalidate();
                    updateStats();
                })
                .show();
    }

    @Override public void onWallLongPressed(String edgeId,float currentValue){
        editWall(edgeId,currentValue,false);
    }

    @Override public void onDiagonalCreated(String diagonalId,float currentValue){
        editDiagonal(diagonalId,currentValue);
    }

    @Override public void onDiagonalLongPressed(String diagonalId,float currentValue){
        NativeProjectStore.RoomData r=activeRoom();
        NativeProjectStore.DiagonalData diagonal=findDiagonal(r,diagonalId);
        if(diagonal==null)return;

        String value=currentValue>0?format(currentValue):"без размера";
        new AlertDialog.Builder(this)
                .setTitle("Диагональ · "+value)
                .setItems(new String[]{"Изменить размер","Удалить"},(dialog,which)->{
                    if(which==0){
                        editDiagonal(diagonalId,currentValue);
                    }else{
                        NativeProjectStore.DiagonalData current=findDiagonal(r,diagonalId);
                        if(current!=null){
                            pushHistory();
                            r.diagonals.remove(current);
                            store.saveCurrent(project);
                            drawingView.invalidate();
                            Toast.makeText(this,"Диагональ удалена",Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    @Override public void onContourClosed(){
        store.saveCurrent(project);
        updateStats();
        drawingView.postDelayed(this::startWallSequence,120);
    }

    private void editWall(String edgeId,float currentValue,boolean sequence){
        NativeProjectStore.RoomData r=activeRoom();
        NativeProjectStore.EdgeData edge=findEdge(r,edgeId);
        if(edge==null)return;

        List<NativeProjectStore.EdgeData> ordered=NativeGeometry.orderedEdges(r);
        int index=ordered.indexOf(edge);
        String title=sequence&&index>=0
                ?"Стена "+(index+1)+" из "+ordered.size()
                :"Размер стены";

        showNumberDialog(title,currentValue,value->{
            NativeProjectStore.EdgeData current=findEdge(r,edgeId);
            if(current==null)return;
            pushHistory();
            current.value=value;
            store.saveCurrent(project);
            drawingView.invalidate();
            updateStats();

            if(sequence){
                NativeProjectStore.EdgeData next=firstUnmeasuredWallAfter(r,edgeId);
                if(next!=null){
                    drawingView.postDelayed(
                            ()->editWall(next.id,next.value,true),
                            100);
                }
            }
        });
    }

    private void editDiagonal(String diagonalId,float currentValue){
        NativeProjectStore.RoomData r=activeRoom();
        NativeProjectStore.DiagonalData diagonal=findDiagonal(r,diagonalId);
        if(diagonal==null)return;
        showNumberDialog("Диагональ",currentValue,value->{
            NativeProjectStore.DiagonalData current=findDiagonal(r,diagonalId);
            if(current==null)return;
            pushHistory();
            current.value=value;
            store.saveCurrent(project);
            drawingView.invalidate();
        });
    }

    private void startWallSequence(){
        NativeProjectStore.RoomData r=activeRoom();
        if(!NativeGeometry.isClosed(r))return;
        for(NativeProjectStore.EdgeData edge:NativeGeometry.orderedEdges(r)){
            if(edge.value<=0){
                editWall(edge.id,edge.value,true);
                return;
            }
        }
    }

    private NativeProjectStore.EdgeData firstUnmeasuredWallAfter(
            NativeProjectStore.RoomData room,
            String currentId){
        List<NativeProjectStore.EdgeData> ordered=NativeGeometry.orderedEdges(room);
        if(ordered.isEmpty())return null;
        int current=-1;
        for(int i=0;i<ordered.size();i++){
            if(ordered.get(i).id.equals(currentId)){current=i;break;}
        }
        for(int step=1;step<=ordered.size();step++){
            NativeProjectStore.EdgeData edge=ordered.get((Math.max(0,current)+step)%ordered.size());
            if(edge.value<=0)return edge;
        }
        return null;
    }

    private NativeProjectStore.EdgeData findEdge(NativeProjectStore.RoomData room,String id){
        for(NativeProjectStore.EdgeData edge:room.edges)if(edge.id.equals(id))return edge;
        return null;
    }

    private NativeProjectStore.DiagonalData findDiagonal(NativeProjectStore.RoomData room,String id){
        for(NativeProjectStore.DiagonalData diagonal:room.diagonals){
            if(diagonal.id.equals(id))return diagonal;
        }
        return null;
    }

    private interface FloatConsumer{void accept(float value);}

    private void showNumberDialog(String title,float current,FloatConsumer consumer){
        NativeNumberPadDialog dialog = new NativeNumberPadDialog(
                this,
                title,
                current,
                consumer::accept);
        dialog.show();
    }

    private void applyGeometrySolution(boolean orthogonal){
        NativeProjectStore.RoomData room=activeRoom();
        NativeMeasurementSolver.Result result=orthogonal
                ?NativeMeasurementSolver.solveOrthogonal(room)
                :NativeMeasurementSolver.solveFree(room);

        if(!result.ok){
            Toast.makeText(this,result.message,Toast.LENGTH_LONG).show();
            return;
        }
        if(result.maxError>NativeMeasurementSolver.FIELD_MEASURE_TOL){
            Toast.makeText(
                    this,
                    "Расхождение замеров превышает рабочий допуск",
                    Toast.LENGTH_LONG).show();
            return;
        }

        drawingView.applySolvedGeometry(result.positions);
        store.saveCurrent(project);
        updateStats();
        Toast.makeText(this,result.message,Toast.LENGTH_SHORT).show();
    }

    private void generateAutoDiagonals(){
        NativeProjectStore.RoomData room=activeRoom();
        if(!NativeGeometry.isClosed(room)){
            Toast.makeText(this,"Сначала замкни контур",Toast.LENGTH_SHORT).show();
            return;
        }
        for(NativeProjectStore.EdgeData edge:room.edges){
            if(edge.value<=0f){
                Toast.makeText(this,"Сначала введи размеры всех стен",Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int added=NativeMeasurementSolver.generateAutoDiagonals(room);
        if(added<=0){
            Toast.makeText(this,"Расчётные диагонали уже добавлены",Toast.LENGTH_SHORT).show();
            return;
        }
        store.saveCurrent(project);
        drawingView.invalidate();
        Toast.makeText(this,"Добавлено диагоналей: "+added,Toast.LENGTH_SHORT).show();
    }

    private void showMeasurementValidation(){
        NativeMeasurementValidator.Result result =
                NativeMeasurementValidator.validate(activeRoom());
        new AlertDialog.Builder(this)
                .setTitle(result.ok() ? "Проверка замера" : "Нужно исправить")
                .setMessage(result.message())
                .setPositiveButton("Готово", null)
                .show();
    }

    private void updateStats(){
        if(areaValue==null||perimeterValue==null)return;
        NativeProjectStore.RoomData room=activeRoom();
        List<NativeProjectStore.EdgeData> edges=NativeGeometry.orderedEdges(room);

        float perimeterUnits=0f;
        boolean allMeasured=!edges.isEmpty();
        for(NativeProjectStore.EdgeData edge:edges){
            if(edge.value<=0f){
                allMeasured=false;
            }else{
                perimeterUnits+=edge.value;
            }
        }

        perimeterValue.setText(allMeasured
                ?String.format(Locale.getDefault(),"%.2f м",perimeterUnits/100f)
                :"—");

        if(!allMeasured||!NativeGeometry.isClosed(room)){
            areaValue.setText("—");
            return;
        }

        Double rawArea=NativeMeasurementSolver.measuredAreaRaw(room);
        if(rawArea==null){
            areaValue.setText("—");
            return;
        }

        areaValue.setText(String.format(
                Locale.getDefault(),
                "%.2f м²",
                rawArea/10000d));
    }

    private void showSavedMeasurements(){
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12),dp(10),dp(12),dp(24));
        scroll.addView(content);
        content.addView(topBar("Замеры"));
        List<NativeProjectStore.ProjectData> saved=store.loadSaved();
        if(saved.isEmpty()){
            TextView empty=text("Сохранённых замеров пока нет",15,MUTED,Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            content.addView(empty,marginParams(0,24));
        }else{
            DateFormat df=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
            for(NativeProjectStore.ProjectData p:saved){
                Button b=button((p.name.isEmpty()?"Без имени":p.name)+"\n"+df.format(new Date(p.updatedAt)),false);
                b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
                b.setMinHeight(dp(68));
                b.setOnClickListener(v->{
                    project=p;
                    store.saveCurrent(project);
                    showEditor(false);
                });
                content.addView(b,marginParams(0,8));
            }
        }
        setContentView(scroll);
    }

    private void showNativeSettings(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16),dp(14),dp(16),dp(28));
        scroll.addView(content,new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(topBar("Настройки"));

        LinearLayout fontCard=card();
        content.addView(fontCard,marginParams(0,10));
        fontCard.addView(text("Шрифт",14,TEXT,Typeface.BOLD));
        LinearLayout fontRow=new LinearLayout(this);
        fontRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams frp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64));
        frp.topMargin=dp(8);
        fontCard.addView(fontRow,frp);
        addUiChoice(fontRow,"Aa\nСистемный",NativeUiPrefs.FONT_SYSTEM,uiPrefs.font,true);
        addUiChoice(fontRow,"Aa\nС засечками",NativeUiPrefs.FONT_SERIF,uiPrefs.font,true);
        addUiChoice(fontRow,"Aa\nМоно",NativeUiPrefs.FONT_MONO,uiPrefs.font,true);

        LinearLayout sizeCard=card();
        content.addView(sizeCard,marginParams(0,10));
        LinearLayout sizeHead=new LinearLayout(this);
        sizeHead.setOrientation(LinearLayout.HORIZONTAL);
        sizeHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView sizeLabel=text("Размер текста",14,TEXT,Typeface.BOLD);
        TextView sizeValue=text(Math.round(uiPrefs.scale*100)+"%",13,BLUE,Typeface.BOLD);
        sizeValue.setTag("native_ui_size_value");
        sizeHead.addView(sizeLabel,new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));
        sizeHead.addView(sizeValue);
        sizeCard.addView(sizeHead);
        sizeCard.addView(text(
                "Меняется плавно во всём приложении",
                11,
                MUTED,
                Typeface.NORMAL),
                marginParams(0,4));

        SeekBar slider=new SeekBar(this);
        slider.setTag("native_ui_size_slider");
        slider.setMax(45);
        slider.setProgress(Math.round(uiPrefs.scale*100)-85);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int progress,boolean fromUser){
                int percent=85+progress;
                sizeValue.setText(percent+"%");
                if(fromUser){
                    uiPrefs=uiPrefs.withScale(percent/100f);
                    uiPrefs.save(NativeWorkspaceActivity.this);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s){}
            @Override public void onStopTrackingTouch(SeekBar s){
                showNativeSettings();
            }
        });
        sizeCard.addView(slider,marginParams(0,8));

        LinearLayout weightCard=card();
        content.addView(weightCard,marginParams(0,10));
        weightCard.addView(text("Жирность",14,TEXT,Typeface.BOLD));
        weightCard.addView(text(
                "Меняет базовую жирность, сохраняя разницу между обычным текстом и заголовками",
                11,
                MUTED,
                Typeface.NORMAL),
                marginParams(0,4));
        LinearLayout weightRow=new LinearLayout(this);
        weightRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams wrp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58));
        wrp.topMargin=dp(8);
        weightCard.addView(weightRow,wrp);
        addUiChoice(weightRow,"Aa\nОбычная",NativeUiPrefs.WEIGHT_NORMAL,uiPrefs.weight,false);
        addUiChoice(weightRow,"Aa\nСредняя",NativeUiPrefs.WEIGHT_MEDIUM,uiPrefs.weight,false);
        addUiChoice(weightRow,"Aa\nЖирная",NativeUiPrefs.WEIGHT_BOLD,uiPrefs.weight,false);

        LinearLayout preview=card();
        preview.setTag("native_ui_preview");
        content.addView(preview,marginParams(0,10));
        preview.addView(text("Предпросмотр",10,MUTED,Typeface.NORMAL));
        preview.addView(text("Комната 1 · 28,00 м²",16,TEXT,Typeface.BOLD),marginParams(0,4));
        preview.addView(text(
                "Калькулятор, смета, кнопки и чертёж меняются сразу.",
                11,
                MUTED,
                Typeface.NORMAL),
                marginParams(0,3));

        LinearLayout actions=new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button reset=button("По умолчанию",false);
        reset.setTag("native_ui_reset");
        reset.setOnClickListener(v->{
            uiPrefs=NativeUiPrefs.defaults();
            uiPrefs.save(this);
            showNativeSettings();
        });
        Button done=button("Готово",true);
        done.setTag("native_ui_done");
        done.setOnClickListener(v->finish());
        actions.addView(reset,weightParams());
        actions.addView(done,weightParams());
        content.addView(actions,marginParams(0,6));

        setContentView(scroll);
    }

    private void addUiChoice(
            LinearLayout row,
            String label,
            String value,
            String selected,
            boolean fontChoice){
        Button b=button(label,false);
        boolean active=value.equals(selected);
        if(active)styleSelected(b);
        b.setTextSize(uiPrefs.textSize(10));
        b.setOnClickListener(v->{
            uiPrefs=fontChoice
                    ?uiPrefs.withFont(value)
                    :uiPrefs.withWeight(value);
            uiPrefs.save(this);
            showNativeSettings();
        });
        row.addView(b,weightParams());
    }

    private LinearLayout card(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12),dp(12),dp(12),dp(12));
        l.setBackground(rounded(PANEL,LINE,16,1));
        l.setElevation(dp(2));
        return l;
    }

    private Button button(String value,boolean primary){
        Button b=new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(uiPrefs.textSize(14));
        b.setTypeface(uiPrefs.typeface(true));
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10),0,dp(10),0);
        b.setBackground(rounded(primary?BLUE:Color.WHITE,primary?BLUE:LINE,11,1));
        return b;
    }

    private TextView text(String value,int sp,int color,int style){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(uiPrefs.textSize(sp));t.setTextColor(color);
        t.setTypeface(uiPrefs.typeface(style==Typeface.BOLD));
        t.setIncludeFontPadding(false);
        return t;
    }

    private LinearLayout.LayoutParams marginParams(int left,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.leftMargin=dp(left);p.topMargin=dp(top);
        return p;
    }

    private LinearLayout.LayoutParams weightParams(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1f);
        p.leftMargin=dp(3);p.rightMargin=dp(3);
        return p;
    }

    private GradientDrawable rounded(int fill,int stroke,int radius,int width){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(dp(radius));
        if(width>0)d.setStroke(dp(width),stroke);
        return d;
    }

    private String format(float v){
        return Math.abs(v-Math.round(v))<.01f?String.valueOf(Math.round(v)):String.format(Locale.US,"%.1f",v);
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
