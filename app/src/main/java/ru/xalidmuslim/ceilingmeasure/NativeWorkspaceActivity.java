package ru.xalidmuslim.ceilingmeasure;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.TextWatcher;
import android.text.Editable;
import android.net.Uri;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import android.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
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
    private static final int REQUEST_EXPORT_BACKUP = 7201;
    private static final int REQUEST_IMPORT_BACKUP = 7202;
    private static final int REQUEST_EXPORT_CLIENT_PDF = 7203;

    private static final int BG = NativeVisualTokens.APP_BG;
    private static final int PANEL = NativeVisualTokens.SURFACE;
    private static final int TEXT = NativeVisualTokens.TEXT;
    private static final int MUTED = NativeVisualTokens.MUTED;
    private static final int BLUE = NativeVisualTokens.ACCENT;
    private static final int LINE = NativeVisualTokens.LINE;
    private static final int DANGER = NativeVisualTokens.DANGER;

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
    private EditText notesInput;
    private ScrollView editorScroll;
    private LinearLayout drawingCard;
    private FrameLayout drawingCanvasWrap;
    private View wallScrollStrip;
    private int normalCanvasHeight;
    private float stripLastY;
    private final ArrayDeque<String> undoRooms = new ArrayDeque<>();
    private final ArrayDeque<String> redoRooms = new ArrayDeque<>();
    private boolean restoringHistory;
    private NativeUiPrefs uiPrefs;
    private byte[] pendingClientPdf;
    private String pendingClientPdfName;
    private View historyNavView;
    private View workspacePage;
    private NativeUiSettingsDialog settingsDialog;
    private boolean wallSequenceMode;
    private String wallEditId;
    private NativeWallMeasureDialog wallDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NativeShellInsets.configureWindow(this);
        store = new NativeProjectStore(this);
        uiPrefs = NativeUiPrefs.load(this);

        String action = getIntent().getStringExtra(EXTRA_LAUNCH_ACTION);
        if (ACTION_NEW_ORDER.equals(action)) project = store.newProject();
        else project = store.loadCurrent();

        if (ACTION_SAVED_MEASUREMENTS.equals(action)) {
            showSavedMeasurements();
        } else {
            showEditor(ACTION_DRAWING.equals(action));
            if (ACTION_SETTINGS.equals(action)) {
                getWindow().getDecorView().post(this::showNativeSettings);
            }
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (settingsDialog != null && settingsDialog.isShowing()) {
                    settingsDialog.dismiss();
                    return;
                }
                // v184 shell-level Back leaves Workspace for Home without changing
                // active-order/room persistence semantics.
                if (nameInput != null && project != null) syncProjectFields();
                finish();
            }
        });
    }

    private void showEditor(boolean jumpToDrawing) {
        undoRooms.clear();
        redoRooms.clear();

        FrameLayout page = new FrameLayout(this);
        page.setTag("native_workspace_page");
        page.setBackground(NativeVisualTokens.workspaceBackground());
        workspacePage = page;

        editorScroll = new ScrollView(this);
        editorScroll.setTag("native_editor_scroll");
        editorScroll.setFillViewport(true);
        editorScroll.setBackgroundColor(Color.TRANSPARENT);
        // Keep scrolled content out of the status-bar safe area. With
        // clipToPadding=false labels could slide underneath the clock/icons.
        editorScroll.setClipToPadding(true);
        editorScroll.setSmoothScrollingEnabled(true);
        editorScroll.setVerticalScrollBarEnabled(false);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setTag("native_workspace_scroll_content");
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        editorScroll.addView(scrollContent, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(editorScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        scrollContent.addView(editorTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        root = new LinearLayout(this);
        root.setTag("native_workspace_main");
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                dp(4),
                0,
                dp(4),
                dp(NativeVisualTokens.SCROLL_NAV_RESERVE_DP));
        scrollContent.addView(root, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout orderCard = card();
        orderCard.setTag("native_order_card");
        root.addView(orderCard, marginParams(0,4));

        nameInput = field(orderCard, "Имя / объект", project.name, InputType.TYPE_CLASS_TEXT);
        phoneInput = field(orderCard, "Телефон", project.phone, InputType.TYPE_CLASS_PHONE);
        addressInput = field(orderCard, "Адрес", project.address, InputType.TYPE_CLASS_TEXT);

        View.OnFocusChangeListener saver=(v,has)->{if(!has)syncProjectFields();};
        nameInput.setOnFocusChangeListener(saver);
        phoneInput.setOnFocusChangeListener(saver);
        addressInput.setOnFocusChangeListener(saver);

        LinearLayout roomsCard = card();
        root.addView(roomsCard, marginParams(0,4));
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

        drawingCard = card();
        drawingCard.setTag("native_drawing_card");
        root.addView(drawingCard, marginParams(0,4));

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
        drawingCard.addView(drawingHead);

        LinearLayout modeBar = new LinearLayout(this);
        modeBar.setOrientation(LinearLayout.HORIZONTAL);
        modeBar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mbp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mbp.topMargin=dp(8);
        drawingCard.addView(modeBar,mbp);

        wallMode = modeButton("Стена", CeilingDrawingView.Mode.WALL, "native_mode_wall");
        pointMode = modeButton("Точки", CeilingDrawingView.Mode.POINT, "native_mode_points");
        diagonalMode = modeButton("Диагональ", CeilingDrawingView.Mode.DIAGONAL, "native_mode_diagonal");
        labelMode = modeButton("Цифры", CeilingDrawingView.Mode.LABEL, "native_mode_labels");
        modeBar.addView(wallMode, compactModeParams());
        modeBar.addView(pointMode, compactModeParams());
        modeBar.addView(diagonalMode, compactModeParams());
        modeBar.addView(labelMode, compactModeParams());

        drawingCanvasWrap = new FrameLayout(this);
        drawingCanvasWrap.setTag("native_canvas_wrap");
        int screenWidthDp = Math.round(
                getResources().getDisplayMetrics().widthPixels
                        / getResources().getDisplayMetrics().density);
        normalCanvasHeight = dp(screenWidthDp <= 500 ? 500 : 560);
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                normalCanvasHeight);
        canvasParams.topMargin = dp(8);
        drawingCard.addView(drawingCanvasWrap, canvasParams);

        drawingView = new CeilingDrawingView(this);
        drawingView.setTag("native_drawing_view");
        drawingView.setListener(this);
        drawingView.setRoom(activeRoom());
        drawingCanvasWrap.addView(drawingView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Web v184: the right 74px strip is the only native pan-y entry
        // while an open contour is being drawn in Wall mode.
        wallScrollStrip = new View(this);
        wallScrollStrip.setTag("native_wall_scroll_strip");
        wallScrollStrip.setBackgroundColor(Color.TRANSPARENT);
        wallScrollStrip.setOnTouchListener((v,event)->{
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    stripLastY=event.getRawY();
                    if(editorScroll!=null)editorScroll.requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if(editorScroll!=null){
                        float currentY=event.getRawY();
                        int delta=Math.round(stripLastY-currentY);
                        if(delta!=0){
                            editorScroll.scrollBy(0,delta);
                            editorScroll.postInvalidateOnAnimation();
                        }
                        stripLastY=currentY;
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if(editorScroll!=null)editorScroll.requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return true;
            }
        });
        FrameLayout.LayoutParams stripParams = new FrameLayout.LayoutParams(
                dp(74),
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END);
        drawingCanvasWrap.addView(wallScrollStrip, stripParams);

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
        drawingCanvasWrap.addView(zoom,zoomParams);

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
        drawingCanvasWrap.addView(drawingHint,hintParams);

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin=dp(6);
        drawingCard.addView(status,statusParams);
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
        drawingCard.addView(saveRow);

        LinearLayout estimateCard=card();
        estimateCard.setTag("native_estimate_card");
        estimateCard.setContentDescription("native_last_content");
        root.addView(estimateCard,marginParams(0,4));

        LinearLayout estimateHead=new LinearLayout(this);
        estimateHead.setOrientation(LinearLayout.HORIZONTAL);
        estimateHead.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout estimateTitleBox=new LinearLayout(this);
        estimateTitleBox.setOrientation(LinearLayout.VERTICAL);
        estimateTitleBox.addView(text("Смета",11,MUTED,Typeface.BOLD));
        estimateTitleBox.addView(text("Для клиента",18,TEXT,Typeface.BOLD));
        estimateHead.addView(estimateTitleBox,new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));
        estimateHead.addView(text(activeRoom().name,11,MUTED,Typeface.NORMAL));
        estimateCard.addView(estimateHead);

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

        TextView validation=text("",11,MUTED,Typeface.NORMAL);
        validation.setTag("native_validation_card");
        validation.setPadding(dp(10),dp(8),dp(10),dp(8));
        validation.setBackground(rounded(Color.rgb(248,250,251),LINE,10,1));
        validation.setOnClickListener(v->showMeasurementValidation());
        estimateCard.addView(validation,marginParams(0,8));
        NativeMeasurementValidator.Result vr=NativeMeasurementValidator.validate(activeRoom());
        validation.setText(vr.ok()
                ?(vr.warnings.isEmpty()?"Проверка пройдена":"Проверка: "+vr.warnings.size()+" замеч.")
                :"Есть ошибки: "+vr.errors.size());

        Button clientEstimate=button("Смета для клиента",true);
        clientEstimate.setTag("native_open_client_estimate");
        clientEstimate.setOnClickListener(v->openEstimate(NativeEstimateDialog.Mode.CLIENT));
        estimateCard.addView(clientEstimate,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(44)));

        Button internalEstimate=button("Закупочный расчёт для себя",false);
        internalEstimate.setTag("native_open_internal_estimate");
        internalEstimate.setOnClickListener(v->openEstimate(NativeEstimateDialog.Mode.INTERNAL));
        LinearLayout.LayoutParams internalParams=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(44));
        internalParams.topMargin=dp(6);
        estimateCard.addView(internalEstimate,internalParams);

        TextView notesLabel=text("Заметка",11,TEXT,Typeface.BOLD);
        estimateCard.addView(notesLabel,marginParams(0,9));
        notesInput=new EditText(this);
        notesInput.setTag("native_room_notes");
        notesInput.setText(activeRoom().notes);
        notesInput.setHint("Короткая заметка по комнате");
        NativeTypography.register(notesInput,12,false,uiPrefs);
        notesInput.setTextColor(TEXT);
        notesInput.setHintTextColor(MUTED);
        notesInput.setGravity(Gravity.TOP|Gravity.START);
        notesInput.setMinHeight(dp(64));
        notesInput.setPadding(dp(10),dp(8),dp(10),dp(8));
        notesInput.setBackground(rounded(Color.WHITE,LINE,10,1));
        notesInput.setOnFocusChangeListener((v,has)->{
            if(!has){
                activeRoom().notes=notesInput.getText().toString();
                store.saveCurrent(project);
            }
        });
        estimateCard.addView(notesInput,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        historyNavView=createHistoryNav();
        page.addView(historyNavView, historyNavParams());
        NativeShellInsets.bindWorkspace(
                page,
                editorScroll,
                historyNavView,
                dp(NativeVisualTokens.HISTORY_BOTTOM_DP));

        setContentView(page);
        page.requestApplyInsets();
        setMode(CeilingDrawingView.Mode.WALL);
        updateStats();
        updateDrawingChrome();

        if (jumpToDrawing) {
            editorScroll.post(() -> drawingCard.requestFocus());
            editorScroll.postDelayed(
                    () -> editorScroll.smoothScrollTo(0, drawingCard.getTop()),
                    120);
        }
    }

    private LinearLayout editorTopBar(){
        LinearLayout bar=new LinearLayout(this);
        bar.setTag("native_workspace_topbar");
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8),dp(7),dp(8),dp(7));
        bar.setMinimumHeight(dp(60));
        bar.setBackgroundColor(NativeVisualTokens.SURFACE);
        bar.setElevation(dp(2));

        LinearLayout brand=new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView main=text("Натяжные потолки",18,TEXT,Typeface.BOLD);
        main.setTag("native_workspace_brand_title");
        main.setSingleLine(true);
        main.setMinWidth(0);
        main.setAutoSizeTextTypeUniformWithConfiguration(
                14,18,1,TypedValue.COMPLEX_UNIT_SP);
        TextView sub=text("замер · расчёт · смета",11,MUTED,Typeface.NORMAL);
        sub.setTag("native_workspace_brand_subtitle");
        sub.setSingleLine(true);
        sub.setMinWidth(0);
        sub.setAutoSizeTextTypeUniformWithConfiguration(
                9,11,1,TypedValue.COMPLEX_UNIT_SP);
        brand.addView(main);
        LinearLayout.LayoutParams subLp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin=dp(2);
        brand.addView(sub,subLp);
        bar.addView(brand,new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        LinearLayout actions=new LinearLayout(this);
        actions.setTag("native_workspace_top_actions");
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(2),dp(2),dp(2),dp(2));
        actions.setBackground(NativeVisualTokens.solid(
                Color.rgb(244,247,249),
                dp(12)));
        bar.addView(actions);

        ImageView home=topIconButton(R.drawable.ic_shell_home,"native_top_home","Главная");
        home.setOnClickListener(v->{
            syncProjectFields();
            finish();
        });
        actions.addView(home,new LinearLayout.LayoutParams(dp(32),dp(32)));

        View saved=topActionButton(
                R.drawable.ic_shell_saved,
                "Замеры",
                false,
                "native_top_saved");
        saved.setOnClickListener(v->{
            syncProjectFields();
            showSavedMeasurements();
        });
        LinearLayout.LayoutParams savedLp=new LinearLayout.LayoutParams(dp(70),dp(46));
        savedLp.leftMargin=dp(3);
        actions.addView(saved,savedLp);

        View fresh=topActionButton(
                R.drawable.ic_shell_add,
                "Заказ",
                true,
                "native_top_new_order");
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
        LinearLayout.LayoutParams freshLp=new LinearLayout.LayoutParams(dp(68),dp(46));
        freshLp.leftMargin=dp(3);
        actions.addView(fresh,freshLp);

        ImageView settings=topIconButton(
                R.drawable.ic_shell_sliders,
                "native_top_settings",
                "Настройки интерфейса");
        settings.setOnClickListener(v->showNativeSettings());
        LinearLayout.LayoutParams settingsLp=new LinearLayout.LayoutParams(dp(32),dp(32));
        settingsLp.leftMargin=dp(3);
        actions.addView(settings,settingsLp);
        return bar;
    }

    private ImageView topIconButton(
            @DrawableRes int iconRes,
            String tag,
            String description){
        ImageView view=new ImageView(this);
        view.setTag(tag);
        view.setContentDescription(description);
        view.setImageResource(iconRes);
        view.setPadding(dp(7),dp(7),dp(7),dp(7));
        view.setClickable(true);
        view.setFocusable(true);
        GradientDrawable content=NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                NativeVisualTokens.LINE,
                dp(9),
                dp(1));
        GradientDrawable mask=NativeVisualTokens.solid(Color.WHITE,dp(9));
        view.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(22,30,49,68)),
                content,
                mask));
        return view;
    }

    private View topActionButton(
            @DrawableRes int iconRes,
            String label,
            boolean primary,
            String tag){
        LinearLayout button=new LinearLayout(this);
        button.setTag(tag);
        button.setContentDescription(label);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(4),0,dp(4),0);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(primary
                ?NativeVisualTokens.primaryButton(dp(14),dp(1))
                :NativeVisualTokens.outlined(
                        NativeVisualTokens.SURFACE,
                        Color.rgb(223,231,237),
                        dp(14),
                        dp(1)));

        ImageView icon=new ImageView(this);
        icon.setImageResource(iconRes);
        if(!primary)icon.setImageTintList(ColorStateList.valueOf(Color.rgb(54,82,103)));
        icon.setPadding(dp(1),dp(1),dp(1),dp(1));
        button.addView(icon,new LinearLayout.LayoutParams(dp(14),dp(14)));

        TextView text=text(
                label,
                11,
                primary?Color.WHITE:Color.rgb(54,82,103),
                Typeface.BOLD);
        // The user can raise the global UI scale to 130%. Keep compact top
        // actions on one line and let Android shrink only this label when
        // necessary instead of wrapping "Замеры"/"Заказ" outside the card.
        text.setSingleLine(true);
        text.setMaxLines(1);
        text.setMinWidth(0);
        text.setAutoSizeTextTypeUniformWithConfiguration(
                8,
                10,
                1,
                TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams textLp=new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        textLp.leftMargin=dp(3);
        button.addView(text,textLp);
        return button;
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
            b.setMinWidth(dp(58));
            b.setMinimumWidth(dp(58));
            b.setPadding(dp(11),0,dp(11),0);
            b.setAutoSizeTextTypeUniformWithConfiguration(
                    9,11,1,TypedValue.COMPLEX_UNIT_SP);
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
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,dp(48));
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
        int same=0;
        for(NativeProjectStore.RoomData existing:project.rooms){
            if(existing.name!=null&&existing.name.startsWith(type))same++;
        }
        String roomName="Другое".equals(type)
                ?"Помещение "+(project.rooms.size()+1)
                :type+" "+(same+1);
        NativeProjectStore.RoomData room=store.createRoom(roomName);
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
        b.setTextSize(11);
        b.setSingleLine(true);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(4),0,dp(4),0);
        b.setAutoSizeTextTypeUniformWithConfiguration(
                8,11,1,TypedValue.COMPLEX_UNIT_SP);
        b.setOnClickListener(v->{
            v.animate().scaleX(.985f).scaleY(.985f).setDuration(70).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start();
            setMode(mode);
        });
        return b;
    }

    private LinearLayout.LayoutParams compactModeParams(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(40),1f);
        p.leftMargin=dp(2);
        p.rightMargin=dp(2);
        return p;
    }

    private void setMode(CeilingDrawingView.Mode mode){
        NativeProjectStore.RoomData room=activeRoom();
        if(room.geometryLocked&&mode!=CeilingDrawingView.Mode.LABEL){
            Toast.makeText(this,"Геометрия зафиксирована",Toast.LENGTH_SHORT).show();
            return;
        }
        // A closed contour blocks creation of extra walls inside the drawing
        // view, but Wall mode itself remains available for editing dimensions.
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
        nav.setPadding(
                dp(NativeVisualTokens.HISTORY_PADDING_DP),
                dp(NativeVisualTokens.HISTORY_PADDING_DP),
                dp(NativeVisualTokens.HISTORY_PADDING_DP),
                dp(NativeVisualTokens.HISTORY_PADDING_DP));
        nav.setElevation(dp(8));
        nav.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                NativeVisualTokens.LINE,
                dp(NativeVisualTokens.HISTORY_RADIUS_DP),
                dp(1)));

        View redo=historyButton("↷","Вперёд","native_nav_redo",v->redoRoom());
        View undo=historyButton("↶","Назад","native_nav_undo",v->undoRoom());
        View more=historyButton("⋯","Еще","native_nav_more",v->openDrawTools());

        nav.addView(redo,historyButtonParams(0));
        nav.addView(undo,historyButtonParams(NativeVisualTokens.HISTORY_GAP_DP));
        nav.addView(more,historyButtonParams(NativeVisualTokens.HISTORY_GAP_DP));
        return nav;
    }

    private View historyButton(
            String icon,
            String label,
            String tag,
            View.OnClickListener listener){
        LinearLayout button=new LinearLayout(this);
        button.setTag(tag);
        button.setContentDescription(label);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(3),dp(2),dp(3),dp(2));
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,
                Color.TRANSPARENT,
                dp(NativeVisualTokens.HISTORY_BUTTON_RADIUS_DP),
                0));
        button.setOnClickListener(listener);
        TextView arrow=text(icon,14,TEXT,Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        arrow.setIncludeFontPadding(false);
        TextView small=text(label,10.5f,TEXT,Typeface.BOLD);
        small.setGravity(Gravity.CENTER);
        small.setSingleLine(true);
        small.setMinWidth(0);
        small.setAutoSizeTextTypeUniformWithConfiguration(
                9,10,1,TypedValue.COMPLEX_UNIT_SP);
        button.addView(arrow);
        button.addView(small);
        return button;
    }

    private LinearLayout.LayoutParams historyButtonParams(int leftMarginDp){
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(
                dp(NativeVisualTokens.HISTORY_BUTTON_WIDTH_DP),
                dp(NativeVisualTokens.HISTORY_BUTTON_HEIGHT_DP));
        params.leftMargin=dp(leftMarginDp);
        return params;
    }

    private FrameLayout.LayoutParams historyNavParams(){
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(
                dp(NativeVisualTokens.HISTORY_WIDTH_DP),
                dp(NativeVisualTokens.HISTORY_HEIGHT_DP),
                Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        p.bottomMargin=dp(NativeVisualTokens.HISTORY_BOTTOM_DP);
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
            case "sketchGeometry":
                restoreSketchGeometry();
                updateDrawingChrome();
                return;
            case "actualGeometry":
            case "autoGeometry":
                applyFactualGeometry();
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
        if(wallMode!=null)wallMode.setEnabled(!room.geometryLocked);
        if(pointMode!=null)pointMode.setEnabled(!room.geometryLocked);
        if(diagonalMode!=null)diagonalMode.setEnabled(!room.geometryLocked);
        if(labelMode!=null)labelMode.setEnabled(true);
        if(wallScrollStrip!=null){
            // The 74dp strip is only needed while an OPEN contour reserves
            // the drawing surface for wall creation. After closure the whole
            // canvas must behave like the normal page and use native ScrollView
            // drag/fling, so the overlay is removed completely.
            wallScrollStrip.setVisibility(
                    drawingView.getMode()==CeilingDrawingView.Mode.WALL
                            && !NativeGeometry.isClosed(room)
                            ?View.VISIBLE
                            :View.GONE);
        }
    }

    private String drawingHintText(NativeProjectStore.RoomData room){
        CeilingDrawingView.Mode mode=drawingView.getMode();
        if(mode==CeilingDrawingView.Mode.LABEL){
            return "Цифры размеров\nПеретащи цифру куда нужно. Вращай пальцем за круг или верхний маркер. Размер меняй кнопками − / +; процент по центру сбрасывает к 100%.";
        }
        if(NativeGeometry.isClosed(room)){
            return "Контур замкнут. В режиме «Стена» можно зажать стену или цифру и изменить размер; новые стены не создаются. Два пальца двигают и масштабируют чертёж.";
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
        if(room.geometryLocked)return "Геометрия зафиксирована. Доступны просмотр и расчёт.";
        if(NativeGeometry.isClosed(room)){
            if(NativeGeometry.isFactualGeometryDirty(room)){
                return "Фактическая геометрия требует обновления после изменения замеров.";
            }
            if(NativeGeometry.isFactualGeometry(room)){
                return "Контур замкнут · фактическая геометрия по замерам.";
            }
            if(mode==CeilingDrawingView.Mode.LABEL){
                return "Режим цифр · геометрия эскиза сохранена.";
            }
            return "Контур замкнут · геометрия эскиза. Размеры не меняют форму.";
        }
        if(mode==CeilingDrawingView.Mode.LABEL)return "Режим цифр: выбери число на чертеже.";
        if(mode==CeilingDrawingView.Mode.DIAGONAL)return "Веди диагональ к нужному углу.";
        if(mode==CeilingDrawingView.Mode.POINT)return "Зажми угол и перетащи его.";
        return "Рисование стен.";
    }

    private void styleMode(Button b,boolean active){
        b.setBackground(active
                ?NativeVisualTokens.primaryButton(dp(10),dp(1))
                :rounded(Color.rgb(248,250,251),LINE,10,1));
        b.setTextColor(active?Color.WHITE:TEXT);
    }

    private void styleSelected(Button b){
        b.setBackground(rounded(NativeVisualTokens.ACCENT_SOFT,BLUE,10,1));
        b.setTextColor(NativeVisualTokens.ACCENT_STRONG);
    }

    private EditText field(LinearLayout parent,String label,String value,int inputType){
        TextView l=text(label,11.5f,MUTED,Typeface.NORMAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin=dp(8);
        parent.addView(l,lp);

        EditText e=new EditText(this);
        e.setText(value);
        NativeTypography.register(e,15,false,uiPrefs);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setPadding(dp(9),0,dp(9),0);
        e.setBackground(rounded(Color.rgb(249,251,252),LINE,11,1));
        parent.addView(e,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(42)));
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
                    NativeGeometry.discardFactualGeometrySession(r);
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
        NativeProjectStore.RoomData room=activeRoom();
        NativeProjectStore.EdgeData edge=findEdge(room,edgeId);
        if(edge==null)return;

        wallSequenceMode=sequence||wallSequenceMode;
        wallEditId=edgeId;
        if(drawingView!=null)drawingView.setEditingEdgeId(edgeId);
        List<NativeProjectStore.EdgeData> ordered=NativeGeometry.orderedEdges(room);
        int index=ordered.indexOf(edge);
        String title=wallSequenceMode&&index>=0
                ?"Стена "+(index+1)+" из "+ordered.size()
                :(room.showAutoLetters?"Стена "+edgeLabel(edge):"Стена");

        if(wallDialog!=null)wallDialog.dismiss();
        drawingView.setWallSequenceActive(wallSequenceMode);
        if(historyNavView!=null)historyNavView.setVisibility(View.GONE);

        wallDialog=new NativeWallMeasureDialog(
                this,
                title,
                currentValue,
                wallSequenceMode,
                new NativeWallMeasureDialog.Listener(){
                    @Override public void onSave(float value){
                        NativeProjectStore.EdgeData current=findEdge(activeRoom(),edgeId);
                        if(current==null){closeWallDialog();return;}
                        pushHistory();
                        current.value=value;
                        NativeGeometry.markFactualGeometryDirty(activeRoom());
                        store.saveCurrent(project);
                        drawingView.invalidate();
                        updateStats();
                        if(wallSequenceMode&&value>0f){
                            NativeProjectStore.EdgeData next=firstUnmeasuredWallAfter(activeRoom(),edgeId);
                            if(next!=null){
                                wallDialog.dismiss();
                                wallDialog=null;
                                drawingView.postDelayed(
                                        ()->editWall(next.id,next.value,true),
                                        70);
                                return;
                            }
                            Toast.makeText(
                                    NativeWorkspaceActivity.this,
                                    "Все размеры стен введены",
                                    Toast.LENGTH_SHORT).show();
                        }
                        closeWallDialog();
                    }

                    @Override public void onDelete(){
                        closeWallDialog();
                        new AlertDialog.Builder(NativeWorkspaceActivity.this)
                                .setTitle("Удалить стену?")
                                .setMessage("Удаление произойдёт только после отдельного подтверждения.")
                                .setNegativeButton("Отмена",null)
                                .setPositiveButton("Удалить",(d,w)->deleteEdge(edgeId))
                                .show();
                    }

                    @Override public void onBack(){
                        if(!wallSequenceMode){closeWallDialog();return;}
                        commitOpenWallValueIfValid();
                        NativeProjectStore.EdgeData previous=previousWall(activeRoom(),edgeId);
                        if(previous==null){
                            closeWallDialog();
                        }else{
                            wallDialog.dismiss();
                            wallDialog=null;
                            editWall(previous.id,previous.value,true);
                        }
                    }

                    @Override public void onClose(){
                        commitOpenWallValueIfValid();
                        closeWallDialog();
                    }
                });
        wallDialog.show();
        enterWallEditorLayout();
    }

    private void enterWallEditorLayout(){
        if(wallDialog==null||drawingCanvasWrap==null)return;
        Window window=wallDialog.getWindow();
        View decor=window==null?null:window.getDecorView();
        if(decor==null)return;
        decor.post(()->{
            int dockHeight=Math.max(dp(220),decor.getHeight());
            int viewport=getResources().getDisplayMetrics().heightPixels;
            int usable=Math.max(dp(300),Math.min(dp(560),viewport-dockHeight-dp(155)));
            ViewGroup.LayoutParams lp=drawingCanvasWrap.getLayoutParams();
            if(lp!=null){
                lp.height=usable;
                drawingCanvasWrap.setLayoutParams(lp);
            }
            if(editorScroll!=null&&drawingCard!=null){
                editorScroll.post(()->editorScroll.smoothScrollTo(0,drawingCard.getTop()));
            }
        });
    }

    private void restoreDrawingCanvasHeight(){
        if(drawingCanvasWrap==null)return;
        ViewGroup.LayoutParams lp=drawingCanvasWrap.getLayoutParams();
        if(lp!=null){
            lp.height=normalCanvasHeight>0?normalCanvasHeight:dp(500);
            drawingCanvasWrap.setLayoutParams(lp);
        }
    }

    private void closeWallDialog(){
        if(wallDialog!=null){
            wallDialog.dismiss();
            wallDialog=null;
        }
        wallEditId=null;
        wallSequenceMode=false;
        if(drawingView!=null){
            drawingView.setEditingEdgeId(null);
            drawingView.setWallSequenceActive(false);
        }
        if(historyNavView!=null)historyNavView.setVisibility(View.VISIBLE);
        restoreDrawingCanvasHeight();
        updateDrawingChrome();
    }

    private void commitOpenWallValueIfValid(){
        if(wallDialog==null||wallEditId==null)return;
        Float value=wallDialog.currentUnitsOrNull();
        if(value==null||value<=0f)return;
        NativeProjectStore.EdgeData edge=findEdge(activeRoom(),wallEditId);
        if(edge!=null&&Math.abs(edge.value-value)>.001f){
            pushHistory();
            edge.value=value;
            NativeGeometry.markFactualGeometryDirty(activeRoom());
            store.saveCurrent(project);
            drawingView.invalidate();
            updateStats();
        }
    }

    @Override public void onWallSequencePick(String edgeId){
        if(!wallSequenceMode)return;
        commitOpenWallValueIfValid();
        NativeProjectStore.EdgeData edge=findEdge(activeRoom(),edgeId);
        if(edge==null)return;
        if(wallDialog!=null){wallDialog.dismiss();wallDialog=null;}
        editWall(edge.id,edge.value,true);
    }

    private void editDiagonal(String diagonalId,float currentValue){
        NativeProjectStore.RoomData room=activeRoom();
        NativeProjectStore.DiagonalData diagonal=findDiagonal(room,diagonalId);
        if(diagonal==null)return;

        Float auto=NativeMeasurementSolver.autoDiagonalValue(room,diagonal);
        String title=room.showAutoLetters
                ?"Диагональ "+diagonalLabel(diagonal)
                :"Диагональ";

        final NativeDiagonalMeasureDialog[] holder=new NativeDiagonalMeasureDialog[1];
        NativeDiagonalMeasureDialog dialog=new NativeDiagonalMeasureDialog(
                this,
                title,
                currentValue,
                auto,
                new NativeDiagonalMeasureDialog.Listener(){
                    @Override public void onSave(float value){
                        NativeProjectStore.DiagonalData current=findDiagonal(activeRoom(),diagonalId);
                        if(current!=null){
                            pushHistory();
                            current.value=value;
                            NativeGeometry.markDiagonalMeasured(current);
                            NativeGeometry.markFactualGeometryDirty(activeRoom());
                            store.saveCurrent(project);
                            drawingView.invalidate();
                        }
                        holder[0].dismiss();
                    }

                    @Override public void onUseAuto(float value){
                        NativeProjectStore.DiagonalData current=findDiagonal(activeRoom(),diagonalId);
                        if(current!=null){
                            pushHistory();
                            current.value=Math.round(value);
                            NativeGeometry.markDiagonalAuto(current);
                            store.saveCurrent(project);
                            drawingView.invalidate();
                        }
                        holder[0].dismiss();
                    }

                    @Override public void onDelete(){
                        holder[0].dismiss();
                        new AlertDialog.Builder(NativeWorkspaceActivity.this)
                                .setTitle("Удалить диагональ?")
                                .setMessage("Удаление произойдёт только после отдельного подтверждения.")
                                .setNegativeButton("Отмена",null)
                                .setPositiveButton("Удалить",(d,w)->{
                                    NativeProjectStore.DiagonalData current=
                                            findDiagonal(activeRoom(),diagonalId);
                                    if(current!=null){
                                        pushHistory();
                                        activeRoom().diagonals.remove(current);
                                        NativeGeometry.markFactualGeometryDirty(activeRoom());
                                        store.saveCurrent(project);
                                        drawingView.invalidate();
                                    }
                                }).show();
                    }

                    @Override public void onClose(){
                        holder[0].dismiss();
                    }
                });
        holder[0]=dialog;
        dialog.show();
    }

    private void startWallSequence(){
        NativeProjectStore.RoomData room=activeRoom();
        if(!NativeGeometry.isClosed(room)){
            Toast.makeText(this,"Сначала замкни контур",Toast.LENGTH_SHORT).show();
            return;
        }
        for(NativeProjectStore.EdgeData edge:NativeGeometry.orderedEdges(room)){
            if(edge.value<=0f){
                wallSequenceMode=true;
                editWall(edge.id,edge.value,true);
                return;
            }
        }
        wallSequenceMode=false;
        Toast.makeText(this,"Все размеры стен уже введены",Toast.LENGTH_SHORT).show();
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
        int start=Math.max(0,current);
        for(int step=1;step<=ordered.size();step++){
            NativeProjectStore.EdgeData edge=ordered.get((start+step)%ordered.size());
            if(edge.value<=0f)return edge;
        }
        return null;
    }

    private NativeProjectStore.EdgeData previousWall(
            NativeProjectStore.RoomData room,
            String currentId){
        List<NativeProjectStore.EdgeData> ordered=NativeGeometry.orderedEdges(room);
        int index=-1;
        for(int i=0;i<ordered.size();i++){
            if(ordered.get(i).id.equals(currentId)){index=i;break;}
        }
        return index>0?ordered.get(index-1):null;
    }

    private void deleteEdge(String edgeId){
        NativeProjectStore.RoomData room=activeRoom();
        NativeProjectStore.EdgeData edge=findEdge(room,edgeId);
        if(edge==null)return;
        pushHistory();
        room.edges.remove(edge);
        room.diagonals.removeIf(d->
                (d.a.equals(edge.a)&&d.b.equals(edge.b))
                        ||(d.a.equals(edge.b)&&d.b.equals(edge.a)));
        NativeGeometry.sanitize(room);
        NativeGeometry.discardFactualGeometrySession(room);
        store.saveCurrent(project);
        drawingView.invalidate();
        updateStats();
        updateDrawingChrome();
    }

    private String pointLetter(String pointId){
        NativeProjectStore.PointData point=NativeGeometry.pointById(activeRoom(),pointId);
        if(point!=null&&"A".equals(point.customLabel))return"A";
        int index=NativeGeometry.pointIndex(activeRoom(),pointId);
        return index>=0&&index<26?String.valueOf((char)('A'+index)):String.valueOf(index+1);
    }

    private String edgeLabel(NativeProjectStore.EdgeData edge){
        return pointLetter(edge.a)+pointLetter(edge.b);
    }

    private String diagonalLabel(NativeProjectStore.DiagonalData diagonal){
        return pointLetter(diagonal.a)+pointLetter(diagonal.b);
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


    private void restoreSketchGeometry(){
        NativeProjectStore.RoomData room=activeRoom();
        if(!NativeGeometry.hasSketchGeometryBackup(room)){
            Toast.makeText(
                    this,
                    "Используется геометрия эскиза. Размеры не меняют форму.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        pushHistory();
        if(!drawingView.restoreSketchGeometryPreservingLabels()){
            Toast.makeText(
                    this,
                    "Эскиз не восстановлен: после расчёта изменилась топология углов.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        store.saveCurrent(project);
        updateStats();
        Toast.makeText(
                this,
                "Геометрия эскиза восстановлена. Замеры сохранены.",
                Toast.LENGTH_SHORT).show();
    }

    private void applyFactualGeometry(){
        NativeProjectStore.RoomData room=activeRoom();
        NativeMeasurementSolver.Result result=
                NativeMeasurementSolver.solveFactual(room);

        if(!result.ok){
            new AlertDialog.Builder(this)
                    .setTitle("Фактическая геометрия")
                    .setMessage(result.message)
                    .setPositiveButton("Понятно",null)
                    .show();
            return;
        }

        // Factual geometry is sketch-first. A measurement mismatch is shown
        // as a residual, but it must not block applying a usable geometry.
        pushHistory();
        if(!NativeGeometry.captureSketchGeometry(room)){
            Toast.makeText(
                    this,
                    "Не удалось сохранить исходную геометрию эскиза",
                    Toast.LENGTH_LONG).show();
            return;
        }

        drawingView.applySolvedGeometry(result.positions);
        NativeGeometry.markFactualGeometry(room);
        store.saveCurrent(project);
        updateStats();

        if(result.maxError>NativeMeasurementSolver.FACTUAL_CONFLICT_TOL){
            new AlertDialog.Builder(this)
                    .setTitle("Фактическая геометрия построена")
                    .setMessage(result.message)
                    .setPositiveButton("Готово",null)
                    .show();
        }else{
            Toast.makeText(this,result.message,Toast.LENGTH_LONG).show();
        }
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

    private void openEstimate(NativeEstimateDialog.Mode mode){
        syncProjectFields();
        if(notesInput!=null)activeRoom().notes=notesInput.getText().toString();
        store.saveCurrent(project);
        new NativeEstimateDialog(
                this,
                store,
                project,
                mode,
                new NativeEstimateDialog.Listener(){
                    @Override public void onChanged(){
                        store.saveCurrent(project);
                        updateStats();
                    }

                    @Override public void onClientPdfRequested(
                            List<NativeProjectStore.ProjectData> projects){
                        beginClientPdfExport(projects);
                    }
                }).show();
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

    private void beginClientPdfExport(
            List<NativeProjectStore.ProjectData> projects){
        NativeClientEstimatePdf.Result pdf=NativeClientEstimatePdf.build(projects);
        if(pdf.pricedLines<=0){
            Toast.makeText(this,"Сначала укажи клиентские цены",Toast.LENGTH_SHORT).show();
            return;
        }
        if(pdf.bytes.length==0){
            Toast.makeText(this,"Не удалось создать PDF на этом устройстве",Toast.LENGTH_SHORT).show();
            return;
        }

        pendingClientPdf=pdf.bytes;
        pendingClientPdfName=pdf.fileName;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE,pdf.fileName);
        startActivityForResult(intent,REQUEST_EXPORT_CLIENT_PDF);
    }

    private void showSavedMeasurements(){
        ScrollView scroll=new ScrollView(this);
        scroll.setTag("native_saved_scroll");
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackground(NativeVisualTokens.workspaceBackground());

        LinearLayout content=new LinearLayout(this);
        content.setTag("native_saved_panel");
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12),dp(10),dp(12),dp(24));
        scroll.addView(content,new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(topBar("Сохранённые замеры"));

        EditText search=new EditText(this);
        search.setTag("native_saved_search");
        search.setHint("Имя, телефон или адрес");
        search.setSingleLine(true);
        search.setTextSize(uiPrefs.textSize(13));
        search.setTypeface(uiPrefs.typeface(false));
        search.setTextColor(TEXT);
        search.setHintTextColor(MUTED);
        search.setBackground(rounded(Color.WHITE,LINE,11,1));
        search.setPadding(dp(12),0,dp(12),0);
        content.addView(search,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(46)));

        LinearLayout filters=new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(42));
        fp.topMargin=dp(8);
        content.addView(filters,fp);

        Button recent=button("Последние",true);
        recent.setTag("native_saved_recent");
        Button all=button("Все",false);
        all.setTag("native_saved_all");
        filters.addView(recent,weightParams());
        filters.addView(all,weightParams());

        LinearLayout backup=new LinearLayout(this);
        backup.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(44));
        bp.topMargin=dp(8);
        content.addView(backup,bp);

        Button export=button("Экспорт всех замеров",false);
        export.setTag("native_backup_export");
        export.setTextSize(uiPrefs.textSize(10));
        export.setOnClickListener(v->beginBackupExport());
        Button restore=button("Восстановить из файла",false);
        restore.setTag("native_backup_import");
        restore.setTextSize(uiPrefs.textSize(10));
        restore.setOnClickListener(v->beginBackupImport());
        backup.addView(export,weightParams());
        backup.addView(restore,weightParams());

        LinearLayout list=new LinearLayout(this);
        list.setTag("native_saved_list");
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list,marginParams(0,8));

        final boolean[] recentMode={true};
        Runnable render=()->renderSavedList(list,search.getText().toString(),recentMode[0]);

        recent.setOnClickListener(v->{
            recentMode[0]=true;
            styleMode(recent,true);
            styleMode(all,false);
            render.run();
        });
        all.setOnClickListener(v->{
            recentMode[0]=false;
            styleMode(recent,false);
            styleMode(all,true);
            render.run();
        });

        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){render.run();}
            @Override public void afterTextChanged(Editable e){}
        });

        render.run();
        setContentView(scroll);
        NativeShellInsets.bindHome(scroll,scroll);
    }

    private void renderSavedList(
            LinearLayout list,
            String query,
            boolean recentMode){
        list.removeAllViews();
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        List<NativeProjectStore.ProjectData> saved=new java.util.ArrayList<>(store.loadSaved());
        saved.sort((a,b)->Long.compare(b.savedAt,b.savedAt==a.savedAt?b.updatedAt:a.savedAt));
        // Correct deterministic descending save order.
        saved.sort((a,b)->Long.compare(
                b.savedAt>0?b.savedAt:b.updatedAt,
                a.savedAt>0?a.savedAt:a.updatedAt));

        java.util.ArrayList<NativeProjectStore.ProjectData> visible=new java.util.ArrayList<>();
        for(NativeProjectStore.ProjectData p:saved){
            StringBuilder hay=new StringBuilder()
                    .append(p.name).append(' ')
                    .append(p.phone).append(' ')
                    .append(p.address);
            for(NativeProjectStore.RoomData room:p.rooms)hay.append(' ').append(room.name);
            if(!q.isEmpty()&&!hay.toString().toLowerCase(Locale.ROOT).contains(q))continue;
            visible.add(p);
            if(q.isEmpty()&&recentMode&&visible.size()>=12)break;
        }

        if(visible.isEmpty()){
            TextView empty=text(
                    q.isEmpty()
                            ?"Сохранённых замеров пока нет.\nЗаверши замер кнопкой «Завершить замер»."
                            :"Ничего не найдено.",
                    13,
                    MUTED,
                    Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(10),dp(28),dp(10),dp(28));
            list.addView(empty);
            return;
        }

        SimpleDateFormat df=new SimpleDateFormat("dd.MM HH:mm",Locale.getDefault());
        for(NativeProjectStore.ProjectData p:visible){
            LinearLayout row=card();
            row.setTag("native_saved_item_"+p.id);
            row.setClickable(true);
            row.setOnClickListener(v->{
                project=p;
                store.saveCurrent(project);
                showEditor(false);
            });
            row.setOnLongClickListener(v->{
                showSavedAction(p);
                return true;
            });

            TextView title=text(
                    p.name==null||p.name.trim().isEmpty()?"Без имени":p.name.trim(),
                    15,TEXT,Typeface.BOLD);
            row.addView(title);

            String address=p.address==null||p.address.trim().isEmpty()
                    ?"Адрес не указан"
                    :p.address.trim();
            row.addView(text(
                    address+" · комнат: "+p.rooms.size(),
                    11,MUTED,Typeface.NORMAL),
                    marginParams(0,3));

            long when=p.savedAt>0?p.savedAt:p.updatedAt;
            row.addView(text(
                    "Завершено: "+df.format(new Date(when)),
                    11,MUTED,Typeface.NORMAL),
                    marginParams(0,2));

            list.addView(row,marginParams(0,7));
        }
    }

    private void showSavedAction(NativeProjectStore.ProjectData saved){
        new AlertDialog.Builder(this)
                .setTitle("Замер")
                .setMessage(saved.name==null||saved.name.isEmpty()?"Заказчик":saved.name)
                .setNegativeButton("Закрыть",null)
                .setPositiveButton("Удалить",(d,w)->new AlertDialog.Builder(this)
                        .setTitle("Удалить замер?")
                        .setMessage("Будут удалены этот заказ, его комнаты и чертежи.")
                        .setNegativeButton("Отмена",null)
                        .setPositiveButton("Удалить",(dd,ww)->{
                            store.deleteSaved(saved.id);
                            if(project!=null&&saved.id.equals(project.id)){
                                project=store.newProject();
                            }
                            showSavedMeasurements();
                        }).show())
                .show();
    }

    private void beginBackupExport(){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE,
                "zamery_backup_"+
                        new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())+
                        ".json");
        startActivityForResult(intent,REQUEST_EXPORT_BACKUP);
    }

    private void beginBackupImport(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent,REQUEST_IMPORT_BACKUP);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,@Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();

        if(requestCode==REQUEST_EXPORT_CLIENT_PDF){
            byte[] bytes=pendingClientPdf;
            pendingClientPdf=null;
            pendingClientPdfName=null;
            if(bytes==null||bytes.length==0)return;
            try(OutputStream out=getContentResolver().openOutputStream(uri)){
                if(out==null)throw new IllegalStateException("stream");
                out.write(bytes);
                out.flush();
                Toast.makeText(this,"PDF сохранён",Toast.LENGTH_SHORT).show();
            }catch(Exception e){
                Toast.makeText(this,"Не удалось сохранить PDF",Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if(requestCode==REQUEST_EXPORT_BACKUP){
            try(OutputStream out=getContentResolver().openOutputStream(uri)){
                if(out==null)throw new IllegalStateException("stream");
                out.write(store.exportBackup().getBytes(StandardCharsets.UTF_8));
                out.flush();
                Toast.makeText(this,"Резервная копия создана",Toast.LENGTH_SHORT).show();
            }catch(Exception e){
                Toast.makeText(this,"Не удалось сохранить файл",Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if(requestCode==REQUEST_IMPORT_BACKUP){
            try(InputStream in=getContentResolver().openInputStream(uri);
                ByteArrayOutputStream out=new ByteArrayOutputStream()){
                if(in==null)throw new IllegalStateException("stream");
                byte[] buffer=new byte[8192];
                int n;
                while((n=in.read(buffer))>0)out.write(buffer,0,n);
                String raw=out.toString(StandardCharsets.UTF_8.name());
                new AlertDialog.Builder(this)
                        .setTitle("Восстановить замеры?")
                        .setMessage("Текущие данные приложения будут заменены данными из выбранной резервной копии.")
                        .setNegativeButton("Отмена",null)
                        .setPositiveButton("Восстановить",(d,w)->{
                            if(store.importBackup(raw)){
                                project=store.loadCurrent();
                                showSavedMeasurements();
                                Toast.makeText(this,"Резервная копия восстановлена",Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(this,"Не удалось прочитать резервную копию",Toast.LENGTH_SHORT).show();
                            }
                        }).show();
            }catch(Exception e){
                Toast.makeText(this,"Не удалось прочитать резервную копию",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showNativeSettings(){
        if(settingsDialog!=null&&settingsDialog.isShowing())return;
        settingsDialog=new NativeUiSettingsDialog(this,uiPrefs,next->{
            uiPrefs=next;
            View decor=getWindow().getDecorView();
            NativeTypography.applyTree(decor,uiPrefs);
        });
        settingsDialog.setOnDismissListener(dialog->settingsDialog=null);
        settingsDialog.show();
    }

    NativeUiPrefs getUiPrefsForTest(){
        return uiPrefs;
    }

    @Nullable
    NativeUiSettingsDialog getSettingsDialogForTest(){
        return settingsDialog;
    }



    private LinearLayout card(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(8),dp(8),dp(8),dp(8));
        l.setBackground(rounded(
                NativeVisualTokens.SURFACE,
                NativeVisualTokens.LINE,
                NativeVisualTokens.CARD_RADIUS_DP,
                1));
        l.setElevation(dp(1));
        return l;
    }

    private Button button(String value,boolean primary){
        Button b=new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        NativeTypography.register(b,14,true,uiPrefs);
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10),0,dp(10),0);
        b.setBackground(primary
                ?NativeVisualTokens.primaryButton(dp(12),dp(1))
                :rounded(NativeVisualTokens.SURFACE,LINE,12,1));
        return b;
    }

    private TextView text(String value,int sp,int color,int style){
        return text(value,(float)sp,color,style);
    }

    private TextView text(String value,float sp,int color,int style){
        TextView t=new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t,sp,style==Typeface.BOLD,uiPrefs);
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
