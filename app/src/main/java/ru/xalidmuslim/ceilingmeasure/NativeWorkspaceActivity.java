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
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import android.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DateFormat;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new NativeProjectStore(this);

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
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });

        root.addView(topBar("Замер потолков"));

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
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams rs = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rs.topMargin=dp(8);
        roomsCard.addView(roomsScroll,rs);
        renderRooms();

        LinearLayout drawCard = card();
        drawCard.setTag("native_drawing_card");
        root.addView(drawCard, marginParams(0,8));
        TextView drawingTitle = text("Чертёж",18,TEXT,Typeface.BOLD);
        drawCard.addView(drawingTitle);

        LinearLayout modeBar = new LinearLayout(this);
        modeBar.setOrientation(LinearLayout.HORIZONTAL);
        modeBar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mbp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        LinearLayout drawTools = new LinearLayout(this);
        drawTools.setOrientation(LinearLayout.HORIZONTAL);
        drawTools.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams drawToolsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        drawToolsParams.topMargin = dp(6);
        drawCard.addView(drawTools, drawToolsParams);

        Button wallMagnet = button("45°/90°", false);
        wallMagnet.setTag("native_tool_wall_magnet");
        wallMagnet.setTextSize(11);
        wallMagnet.setOnClickListener(v -> {
            NativeProjectStore.RoomData r = activeRoom();
            r.wallAngleMagnet = !r.wallAngleMagnet;
            store.saveCurrent(project);
            Toast.makeText(
                    this,
                    r.wallAngleMagnet ? "Привязка стен включена" : "Привязка стен выключена",
                    Toast.LENGTH_SHORT).show();
        });

        Button point90 = button("90° точки", false);
        point90.setTag("native_tool_point90");
        point90.setTextSize(11);
        point90.setOnClickListener(v -> {
            NativeProjectStore.RoomData r = activeRoom();
            r.point90Magnet = !r.point90Magnet;
            store.saveCurrent(project);
            Toast.makeText(
                    this,
                    r.point90Magnet ? "Привязка 90° включена" : "Привязка 90° выключена",
                    Toast.LENGTH_SHORT).show();
        });

        Button letters = button("Буквы", false);
        letters.setTag("native_tool_letters");
        letters.setTextSize(11);
        letters.setOnClickListener(v -> {
            NativeProjectStore.RoomData r = activeRoom();
            r.showAutoLetters = !r.showAutoLetters;
            store.saveCurrent(project);
            drawingView.invalidate();
        });

        Button wallSequence = button("123", false);
        wallSequence.setTag("native_tool_wall_sequence");
        wallSequence.setTextSize(12);
        wallSequence.setOnClickListener(v -> startWallSequence());

        drawTools.addView(wallMagnet, weightParams());
        drawTools.addView(point90, weightParams());
        drawTools.addView(letters, weightParams());
        drawTools.addView(wallSequence, weightParams());

        drawingView = new CeilingDrawingView(this);
        drawingView.setTag("native_drawing_view");
        drawingView.setListener(this);
        drawingView.setRoom(activeRoom());
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(470));
        dpv.topMargin=dp(8);
        drawCard.addView(drawingView,dpv);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin=dp(8);
        drawCard.addView(stats,sp);
        areaValue = stat(stats,"Площадь");
        perimeterValue = stat(stats,"Периметр");

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin=dp(8);
        drawCard.addView(actions,ap);

        Button clear=button("Очистить",false);
        clear.setTextColor(DANGER);
        clear.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("Очистить чертёж?")
                .setMessage("Все стены и диагонали этой комнаты будут удалены.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Очистить",(d,w)->drawingView.clearDrawing())
                .show());
        Button save=button("✓ Завершить замер",true);
        save.setTag("native_save_measurement");
        save.setOnClickListener(v->{
            syncProjectFields();
            store.saveSnapshot(project);
            Toast.makeText(this,"Замер сохранён",Toast.LENGTH_SHORT).show();
        });
        actions.addView(clear,weightParams());
        actions.addView(save,weightParams());

        setContentView(scroll);
        ViewCompat.requestApplyInsets(scroll);
        setMode(CeilingDrawingView.Mode.WALL);
        updateStats();

        if (jumpToDrawing) {
            scroll.post(() -> drawCard.requestFocus());
            scroll.postDelayed(() -> scroll.smoothScrollTo(0, drawCard.getTop()), 120);
        }
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
                if(drawingView!=null)drawingView.setRoom(activeRoom());
                updateStats();
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
        add.setOnClickListener(v->{
            NativeProjectStore.RoomData r=new NativeProjectStore.RoomData();
            r.name="Комната "+(project.rooms.size()+1);
            project.rooms.add(r);
            project.activeRoom=project.rooms.size()-1;
            store.saveCurrent(project);
            renderRooms();
            if(drawingView!=null)drawingView.setRoom(activeRoom());
            updateStats();
        });
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
                .setItems(new String[]{"Переименовать","Удалить"},(dialog,which)->{
                    if(which==0){
                        showRoomRename(index);
                    }else{
                        deleteRoom(index);
                    }
                })
                .show();
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
        drawingView.setMode(mode);
        styleMode(wallMode,mode==CeilingDrawingView.Mode.WALL);
        styleMode(pointMode,mode==CeilingDrawingView.Mode.POINT);
        styleMode(diagonalMode,mode==CeilingDrawingView.Mode.DIAGONAL);
        styleMode(labelMode,mode==CeilingDrawingView.Mode.LABEL);
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

    private void updateStats(){
        if(areaValue==null||perimeterValue==null)return;
        NativeProjectStore.RoomData r=activeRoom();
        List<NativeProjectStore.EdgeData> edges=NativeGeometry.orderedEdges(r);

        float perimeterUnits=0;
        int measured=0;
        for(NativeProjectStore.EdgeData edge:edges){
            if(edge.value>0){
                perimeterUnits+=edge.value;
                measured++;
            }
        }
        perimeterValue.setText(!edges.isEmpty()&&measured==edges.size()
                ?String.format(Locale.getDefault(),"%.2f м",perimeterUnits/100f)
                :"—");

        List<String> cycle=NativeGeometry.cycleOrder(r);
        if(cycle==null||cycle.size()<3||measured==0){
            areaValue.setText("—");
            return;
        }

        double pxPerUnitSum=0;
        int count=0;
        for(NativeProjectStore.EdgeData edge:edges){
            if(edge.value<=0)continue;
            NativeProjectStore.PointData a=NativeGeometry.pointById(r,edge.a);
            NativeProjectStore.PointData b=NativeGeometry.pointById(r,edge.b);
            if(a==null||b==null)continue;
            double px=Math.hypot(b.x-a.x,b.y-a.y);
            if(px>0){
                pxPerUnitSum+=px/edge.value;
                count++;
            }
        }
        if(count==0){
            areaValue.setText("—");
            return;
        }

        double pxPerUnit=pxPerUnitSum/count;
        double areaPx=0;
        for(int i=0;i<cycle.size();i++){
            NativeProjectStore.PointData a=NativeGeometry.pointById(r,cycle.get(i));
            NativeProjectStore.PointData b=NativeGeometry.pointById(r,cycle.get((i+1)%cycle.size()));
            if(a==null||b==null)continue;
            areaPx+=a.x*b.y-b.x*a.y;
        }
        areaPx=Math.abs(areaPx)/2d;
        double areaUnits=areaPx/(pxPerUnit*pxPerUnit);
        double areaM2=areaUnits/10000d;
        areaValue.setText(String.format(Locale.getDefault(),"%.2f м²",areaM2));
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
        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16),dp(14),dp(16),dp(24));
        content.setBackgroundColor(BG);
        content.addView(topBar("Настройки"));
        TextView title=text("Нативные настройки",18,TEXT,Typeface.BOLD);
        content.addView(title,marginParams(0,14));
        TextView note=text("Интерфейс этой версии уже работает без WebView. Настройки размера текста и оформления перенесу сюда отдельно.",14,MUTED,Typeface.NORMAL);
        note.setLineSpacing(0,1.2f);
        content.addView(note);
        setContentView(content);
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
        b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10),0,dp(10),0);
        b.setBackground(rounded(primary?BLUE:Color.WHITE,primary?BLUE:LINE,11,1));
        return b;
    }

    private TextView text(String value,int sp,int color,int style){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",style));
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
