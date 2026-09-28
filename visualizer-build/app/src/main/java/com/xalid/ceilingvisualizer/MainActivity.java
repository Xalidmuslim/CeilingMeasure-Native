package com.xalid.ceilingvisualizer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;

    private LinearLayout root;
    private LinearLayout topBar;
    private LinearLayout optionRow;
    private HorizontalScrollView optionsScroll;
    private HorizontalScrollView toolsScroll;
    private FrameLayout editorFrame;
    private TextView hint;
    private TextView compareChip;
    private final ArrayList<TextView> toolChips = new ArrayList<>();
    private EditorView editor;
    private EditorView.Mode selectedMode = EditorView.Mode.CEILING;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17,19,21));
        getWindow().setNavigationBarColor(Color.rgb(17,19,21));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17,19,21));
        applySystemInsets(root);

        topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(8),dp(5),dp(8),dp(5));
        topBar.setBackgroundColor(Color.rgb(24,27,30));

        TextView title = text("Визуализатор",16,Color.WHITE);
        topBar.addView(title,new LinearLayout.LayoutParams(0,dp(46),1f));

        TextView photo = actionChip("Фото",62);
        TextView undo = actionChip("↶",44);
        compareChip = actionChip("До",52);
        TextView clear = actionChip("×",44);
        topBar.addView(photo);
        topBar.addView(undo);
        topBar.addView(compareChip);
        topBar.addView(clear);
        root.addView(topBar,new LinearLayout.LayoutParams(-1,dp(54)));

        editorFrame = new FrameLayout(this);
        editor = new EditorView(this);
        editorFrame.addView(editor,new FrameLayout.LayoutParams(-1,-1));

        hint = text("Нажмите «Фото» и выберите помещение",12,Color.WHITE);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(12),dp(7),dp(12),dp(7));
        hint.setBackground(roundRect(Color.argb(200,20,23,25),16));
        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        hintLp.setMargins(dp(12),dp(12),dp(12),dp(10));
        editorFrame.addView(hint,hintLp);
        root.addView(editorFrame,new LinearLayout.LayoutParams(-1,0,1f));

        optionsScroll = new HorizontalScrollView(this);
        optionsScroll.setHorizontalScrollBarEnabled(false);
        optionsScroll.setBackgroundColor(Color.rgb(24,27,30));
        optionRow = new LinearLayout(this);
        optionRow.setGravity(Gravity.CENTER_VERTICAL);
        optionRow.setPadding(dp(7),dp(4),dp(7),dp(4));
        optionsScroll.addView(optionRow);
        root.addView(optionsScroll,new LinearLayout.LayoutParams(-1,dp(50)));

        toolsScroll = new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        toolsScroll.setBackgroundColor(Color.rgb(18,21,23));
        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(dp(7),dp(6),dp(7),dp(7));

        String[] names={"Просмотр","Рука","Перспектива","Потолок","Линия","Трек","Споты","Люстра"};
        EditorView.Mode[] modes={
                EditorView.Mode.PREVIEW, EditorView.Mode.VIEW, EditorView.Mode.PERSPECTIVE,
                EditorView.Mode.CEILING, EditorView.Mode.LINE, EditorView.Mode.TRACK,
                EditorView.Mode.SPOT, EditorView.Mode.CHANDELIER
        };
        for(int i=0;i<names.length;i++){
            final int index=i;
            TextView chip=toolChip(names[i]);
            chip.setOnClickListener(v->selectMode(modes[index],index));
            tools.addView(chip);
            toolChips.add(chip);
        }
        toolsScroll.addView(tools);
        root.addView(toolsScroll,new LinearLayout.LayoutParams(-1,dp(60)));

        photo.setOnClickListener(v->pickPhoto());
        undo.setOnClickListener(v->editor.undo());
        compareChip.setOnClickListener(v->{
            boolean after=editor.toggleAfter();
            compareChip.setText(after?"До":"После");
            setHint(after?editor.helpText():"Показан исходный снимок");
        });
        clear.setOnClickListener(v->{
            editor.clearAll();
            setHint("Все добавленные элементы очищены");
        });

        editor.setOnTapInPreview(()->exitCleanPreview());
        editor.setOnSelectionChanged(()->{
            if(selectedMode==EditorView.Mode.VIEW){
                rebuildOptions();
                setHint(editor.selectionHelpText());
            }
        });

        setContentView(root);
        selectMode(EditorView.Mode.CEILING,3);
    }

    private void enterCleanPreview(){
        selectedMode=EditorView.Mode.PREVIEW;
        editor.setMode(EditorView.Mode.PREVIEW);
        topBar.setVisibility(View.GONE);
        optionsScroll.setVisibility(View.GONE);
        toolsScroll.setVisibility(View.GONE);
        hint.setVisibility(View.GONE);
        root.setPadding(0,0,0,0);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void exitCleanPreview(){
        getWindow().getDecorView().setSystemUiVisibility(0);
        topBar.setVisibility(View.VISIBLE);
        optionsScroll.setVisibility(View.VISIBLE);
        toolsScroll.setVisibility(View.VISIBLE);
        applySystemInsets(root);
        selectMode(EditorView.Mode.VIEW,1);
    }

    private void applySystemInsets(View v){
        v.setOnApplyWindowInsetsListener((view,insets)->{
            int top,bottom;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                top=bars.top; bottom=bars.bottom;
            }else{
                top=insets.getSystemWindowInsetTop();
                bottom=insets.getSystemWindowInsetBottom();
            }
            view.setPadding(0,top,0,bottom);
            return insets;
        });
        v.requestApplyInsets();
    }

    private void selectMode(EditorView.Mode mode,int chipIndex){
        if(mode==EditorView.Mode.PREVIEW){
            enterCleanPreview();
            return;
        }
        selectedMode=mode;
        editor.setMode(mode);
        for(int i=0;i<toolChips.size();i++) styleToolChip(toolChips.get(i),i==chipIndex);
        rebuildOptions();
        setHint(editor.helpText());
        if(chipIndex>=0 && chipIndex<toolChips.size()){
            final View target=toolChips.get(chipIndex);
            toolsScroll.post(()->toolsScroll.smoothScrollTo(Math.max(0,target.getLeft()-dp(70)),0));
        }
    }

    private void rebuildOptions(){
        optionRow.removeAllViews();
        switch(selectedMode){
            case VIEW:
                if(editor.hasSelection()){
                    if(editor.selectedIsFixture()){
                        addOption("− размер",false,()->{editor.scaleSelectedBy(0.88f);rebuildOptions();});
                        addOption("+ размер",false,()->{editor.scaleSelectedBy(1.14f);rebuildOptions();});
                        addOption("↺ 15°",false,()->{editor.rotateSelected(-15);rebuildOptions();});
                        addOption("↻ 15°",false,()->{editor.rotateSelected(15);rebuildOptions();});
                    }
                    addOption("Удалить",false,()->{editor.deleteSelected();rebuildOptions();});
                    addOption("Снять выбор",false,()->{editor.deselect();rebuildOptions();});
                }else{
                    addOption("Сбросить вид",false,()->editor.resetView());
                    addOption("Выберите объект",false,()->{});
                }
                break;
            case PERSPECTIVE:
                addOption(editor.hasPerspective()?"Перспектива ✓":"4 точки",editor.hasPerspective(),()->{});
                addOption("Сбросить",false,()->{editor.clearPerspective();rebuildOptions();});
                break;
            case CEILING:
                addOption("Белый",editor.getCeilingColor()==Color.rgb(248,248,246),()->{editor.setCeilingColor(Color.rgb(248,248,246));rebuildOptions();});
                addOption("Серый",editor.getCeilingColor()==Color.rgb(214,216,216),()->{editor.setCeilingColor(Color.rgb(214,216,216));rebuildOptions();});
                addOption("Бежевый",editor.getCeilingColor()==Color.rgb(225,216,199),()->{editor.setCeilingColor(Color.rgb(225,216,199));rebuildOptions();});
                addOption("Скрыть маску",editor.isCeilingPreviewHidden(),()->{editor.toggleCeilingPreview();rebuildOptions();});
                break;
            case LINE:
                addKelvinOptions();
                addWidthOptions();
                addOption("90°",editor.isOrtho(),()->{editor.toggleOrtho();rebuildOptions();});
                break;
            case TRACK:
                addOption("Чёрный",editor.isTrackBlack(),()->{editor.setTrackBlack(true);rebuildOptions();});
                addOption("Белый",!editor.isTrackBlack(),()->{editor.setTrackBlack(false);rebuildOptions();});
                String[] th={"Цилиндр","Компакт","Магнитный","Линейный","Подвес","Двойной"};
                for(int i=0;i<th.length;i++){ final int k=i; addOption(th[i],editor.getTrackHeadStyle()==k,()->{editor.setTrackHeadStyle(k);rebuildOptions();}); }
                addOption("90°",editor.isOrtho(),()->{editor.toggleOrtho();rebuildOptions();});
                break;
            case SPOT:
                addKelvinOptions();
                addOption("Белый",editor.getSpotStyle()==0,()->{editor.setSpotStyle(0);rebuildOptions();});
                addOption("Чёрный",editor.getSpotStyle()==1,()->{editor.setSpotStyle(1);rebuildOptions();});
                addScaleOptions();
                break;
            case CHANDELIER:
                addOption("Кольцо",editor.getChandelierStyle()==0,()->{editor.setChandelierStyle(0);rebuildOptions();});
                addOption("3 плафона",editor.getChandelierStyle()==3,()->{editor.setChandelierStyle(3);rebuildOptions();});
                addScaleOptions();
                break;
            default: break;
        }
    }

    private void addKelvinOptions(){
        addOption("3000K",editor.getKelvin()==3000,()->{editor.setKelvin(3000);rebuildOptions();});
        addOption("3500K",editor.getKelvin()==3500,()->{editor.setKelvin(3500);rebuildOptions();});
        addOption("4000K",editor.getKelvin()==4000,()->{editor.setKelvin(4000);rebuildOptions();});
    }
    private void addWidthOptions(){
        addOption("20 мм",editor.getLineMm()==20,()->{editor.setLineMm(20);rebuildOptions();});
        addOption("30 мм",editor.getLineMm()==30,()->{editor.setLineMm(30);rebuildOptions();});
        addOption("50 мм",editor.getLineMm()==50,()->{editor.setLineMm(50);rebuildOptions();});
    }
    private void addScaleOptions(){
        addOption("70%",editor.getFixtureScale()==0.7f,()->{editor.setFixtureScale(0.7f);rebuildOptions();});
        addOption("85%",editor.getFixtureScale()==0.85f,()->{editor.setFixtureScale(0.85f);rebuildOptions();});
        addOption("100%",editor.getFixtureScale()==1f,()->{editor.setFixtureScale(1f);rebuildOptions();});
    }

    private void addOption(String label,boolean selected,Runnable action){
        TextView chip=text(label,12,selected?Color.rgb(15,20,16):Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(13),0,dp(13),0);
        chip.setBackground(roundRect(selected?Color.rgb(167,224,75):Color.rgb(40,45,49),15));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(38));
        lp.setMargins(dp(3),dp(2),dp(3),dp(2));
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v->action.run());
        optionRow.addView(chip);
    }

    private void pickPhoto(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i,PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_IMAGE||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{
            int flags=data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if(flags!=0)getContentResolver().takePersistableUriPermission(uri,flags);
            try(InputStream input=getContentResolver().openInputStream(uri)){
                Bitmap bmp=BitmapFactory.decodeStream(input);
                if(bmp==null)throw new IllegalStateException("decode");
                editor.setBitmap(bmp);
                selectMode(EditorView.Mode.PERSPECTIVE,2);
            }
        }catch(Exception e){
            Toast.makeText(this,"Не удалось открыть фотографию",Toast.LENGTH_LONG).show();
        }
    }

    private void setHint(String s){
        hint.setText(s);
        hint.setVisibility(s==null||s.isEmpty()?View.GONE:View.VISIBLE);
    }
    private TextView actionChip(String s,int w){
        TextView t=text(s,12,Color.WHITE); t.setGravity(Gravity.CENTER);
        t.setBackground(roundRect(Color.rgb(39,44,48),15));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(w),dp(40)); lp.setMargins(dp(3),0,dp(3),0); t.setLayoutParams(lp); return t;
    }
    private TextView toolChip(String s){
        TextView t=text(s,13,Color.WHITE); t.setGravity(Gravity.CENTER); t.setPadding(dp(14),0,dp(14),0);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(45)); lp.setMargins(dp(3),0,dp(3),0); t.setLayoutParams(lp); return t;
    }
    private void styleToolChip(TextView t,boolean active){
        t.setTextColor(active?Color.rgb(15,20,16):Color.WHITE);
        t.setBackground(roundRect(active?Color.rgb(167,224,75):Color.rgb(39,44,48),16));
    }
    private TextView text(String s,int sp,int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); return t;
    }
    private GradientDrawable roundRect(int color,int radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}

    public static class EditorView extends View {
        enum Mode { PREVIEW, VIEW, PERSPECTIVE, CEILING, LINE, TRACK, SPOT, CHANDELIER }

        static class PlaneSegment {
            PointF a,b; int style,kelvin,widthMm; boolean black;
            PlaneSegment(PointF a,PointF b){this.a=copy(a);this.b=copy(b);}
        }
        static class Fixture {
            PointF p; int style,kelvin,rotation; float scale;
            Fixture(PointF p){this.p=copy(p);}
        }
        static class Snapshot {
            ArrayList<PointF> mask,perspective;
            ArrayList<PlaneSegment> lines,tracks;
            ArrayList<Fixture> spots,chandeliers;
            Snapshot(EditorView e){
                mask=copyPts(e.mask); perspective=copyPts(e.perspective);
                lines=copySeg(e.lines); tracks=copySeg(e.tracks);
                spots=copyFixtures(e.spots); chandeliers=copyFixtures(e.chandeliers);
            }
            static ArrayList<PointF> copyPts(List<PointF> src){ArrayList<PointF> r=new ArrayList<>();for(PointF p:src)r.add(copy(p));return r;}
            static ArrayList<PlaneSegment> copySeg(List<PlaneSegment> src){
                ArrayList<PlaneSegment> r=new ArrayList<>();
                for(PlaneSegment s:src){PlaneSegment n=new PlaneSegment(s.a,s.b);n.style=s.style;n.kelvin=s.kelvin;n.widthMm=s.widthMm;n.black=s.black;r.add(n);}return r;
            }
            static ArrayList<Fixture> copyFixtures(List<Fixture> src){
                ArrayList<Fixture> r=new ArrayList<>();for(Fixture f:src){Fixture n=new Fixture(f.p);n.style=f.style;n.kelvin=f.kelvin;n.rotation=f.rotation;n.scale=f.scale;r.add(n);}return r;
            }
        }

        private Bitmap bitmap;
        private Mode mode=Mode.PERSPECTIVE;
        private boolean showAfter=true;
        private boolean hideCeilingPreview=false;

        private final ArrayList<PointF> perspective=new ArrayList<>();
        private final ArrayList<PointF> mask=new ArrayList<>();
        private final ArrayList<PointF> draftMask=new ArrayList<>();
        private final ArrayList<PlaneSegment> lines=new ArrayList<>();
        private final ArrayList<PlaneSegment> tracks=new ArrayList<>();
        private final ArrayList<Fixture> spots=new ArrayList<>();
        private final ArrayList<Fixture> chandeliers=new ArrayList<>();
        private final ArrayDeque<Snapshot> history=new ArrayDeque<>();

        private final Matrix planeToImage=new Matrix();
        private final Matrix imageToPlane=new Matrix();
        private boolean perspectiveReady=false;

        private static final int SEL_NONE=0, SEL_SPOT=1, SEL_CHANDELIER=2, SEL_LINE=3, SEL_TRACK=4;
        private int selectedType=SEL_NONE, selectedIndex=-1;
        private boolean draggingSelection=false;
        private PointF dragLastPlane;
        private Runnable selectionChanged;

        private PointF startPlane,currentPlane;
        private float zoom=1f,panX=0f,panY=0f,lastX,lastY;
        private boolean moving=false,pinching=false,suppressSingleTouch=false,pinchSelection=false;
        private float pinchStartDistance,pinchStartZoom,pinchStartPanX,pinchStartPanY,pinchStartMidX,pinchStartMidY;
        private float pinchSelectedStartScale=1f;

        private int ceilingColor=Color.rgb(248,248,246);
        private int kelvin=3500;
        private int lineMm=30;
        private boolean ortho=false;
        private boolean trackBlack=true;
        private int trackHeadStyle=0;
        private int spotStyle=0;
        private int chandelierStyle=0;
        private float fixtureScale=0.85f;
        private int fixtureRotation=0;

        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private Runnable previewTap;

        private final HashMap<String,Bitmap> remoteAssets=new HashMap<>();
        private final ExecutorService assetPool=Executors.newFixedThreadPool(3);
        private final Handler mainHandler=new Handler(Looper.getMainLooper());

        private static final String ASSET_SHEET_MAIN="https://cdn.openart.ai/watermarked_images/wwKL72mZPzCRR9UDPBp1/thumbnail_8678e3e8_1790620789465.webp";
        private static final String ASSET_SHEET_EXTRA="https://cdn.openart.ai/watermarked_images/wwKL72mZPzCRR9UDPBp1/thumbnail_77a095af_1790621044706.webp";

        EditorView(Activity a){
            super(a);
            density=getResources().getDisplayMetrics().density;
            setBackgroundColor(Color.BLACK);
            preloadAssets();
        }

        void setOnTapInPreview(Runnable r){previewTap=r;}
        void setOnSelectionChanged(Runnable r){selectionChanged=r;}
        void setBitmap(Bitmap b){bitmap=b;clearAllInternal();history.clear();clearPerspective();resetView();invalidate();}
        void setMode(Mode m){
            mode=m;draftMask.clear();startPlane=null;currentPlane=null;
            if(m!=Mode.VIEW) clearSelection(false);
            invalidate();
        }
        String helpText(){
            switch(mode){
                case VIEW:return "Рука: тап по объекту — редактировать; пустое место — двигать фото";
                case PERSPECTIVE:return perspectiveReady?"Перспектива готова":"Коснитесь 4 углов потолочной плоскости";
                case CEILING:return "Потолок: обведите область одним движением";
                case LINE:return "Линия: проведите на потолке; 90° создаёт Г-образную";
                case TRACK:return "Трек: проведите по потолку";
                case SPOT:return "Споты: касайтесь мест установки";
                case CHANDELIER:return "Люстра: коснитесь точки крепления";
                case PREVIEW:return "";
            }return "";
        }

        boolean toggleAfter(){showAfter=!showAfter;invalidate();return showAfter;}
        void push(){if(history.size()>=30)history.removeFirst();history.addLast(new Snapshot(this));}
        void undo(){
            if(history.isEmpty())return;
            restore(history.removeLast());
            clearSelection(true);
            invalidate();
        }
        private void restore(Snapshot s){
            mask.clear();mask.addAll(s.mask);
            perspective.clear();perspective.addAll(s.perspective);rebuildPerspective();
            lines.clear();lines.addAll(s.lines);tracks.clear();tracks.addAll(s.tracks);spots.clear();spots.addAll(s.spots);chandeliers.clear();chandeliers.addAll(s.chandeliers);
        }
        void clearAll(){if(lines.isEmpty()&&tracks.isEmpty()&&spots.isEmpty()&&chandeliers.isEmpty()&&mask.isEmpty())return;push();clearAllInternal();invalidate();}
        private void clearAllInternal(){
            mask.clear();draftMask.clear();lines.clear();tracks.clear();spots.clear();chandeliers.clear();startPlane=null;currentPlane=null;
            clearSelection(false);
        }
        void resetView(){zoom=1f;panX=0f;panY=0f;constrainPan();invalidate();}

        boolean hasSelection(){return selectedType!=SEL_NONE && selectedIndex>=0;}
        boolean selectedIsFixture(){return selectedType==SEL_SPOT || selectedType==SEL_CHANDELIER;}
        String selectionHelpText(){
            if(!hasSelection()) return helpText();
            if(selectedType==SEL_SPOT) return "Выбран спот: тяните пальцем; двумя пальцами меняйте размер";
            if(selectedType==SEL_CHANDELIER) return "Выбрана люстра: тяните пальцем; двумя пальцами меняйте размер";
            if(selectedType==SEL_LINE) return "Выбрана световая линия: перетащите или удалите";
            return "Выбран трек: перетащите или удалите";
        }
        void deselect(){clearSelection(true);invalidate();}
        void scaleSelectedBy(float factor){
            Fixture f=getSelectedFixture(); if(f==null)return;
            push(); f.scale=clamp(f.scale*factor,0.35f,1.60f); invalidate(); notifySelectionChanged();
        }
        void rotateSelected(int degrees){
            Fixture f=getSelectedFixture(); if(f==null)return;
            push(); f.rotation=(f.rotation+degrees)%360; if(f.rotation<0)f.rotation+=360; invalidate(); notifySelectionChanged();
        }
        void deleteSelected(){
            if(!hasSelection())return;
            push();
            if(selectedType==SEL_SPOT && selectedIndex<spots.size()) spots.remove(selectedIndex);
            else if(selectedType==SEL_CHANDELIER && selectedIndex<chandeliers.size()) chandeliers.remove(selectedIndex);
            else if(selectedType==SEL_LINE && selectedIndex<lines.size()) lines.remove(selectedIndex);
            else if(selectedType==SEL_TRACK && selectedIndex<tracks.size()) tracks.remove(selectedIndex);
            clearSelection(true); invalidate();
        }
        private Fixture getSelectedFixture(){
            if(selectedType==SEL_SPOT && selectedIndex>=0 && selectedIndex<spots.size())return spots.get(selectedIndex);
            if(selectedType==SEL_CHANDELIER && selectedIndex>=0 && selectedIndex<chandeliers.size())return chandeliers.get(selectedIndex);
            return null;
        }
        private void clearSelection(boolean notify){selectedType=SEL_NONE;selectedIndex=-1;draggingSelection=false;dragLastPlane=null;if(notify)notifySelectionChanged();}
        private void notifySelectionChanged(){if(selectionChanged!=null)selectionChanged.run();}

        boolean hasPerspective(){return perspectiveReady;}
        void clearPerspective(){perspective.clear();perspectiveReady=false;planeToImage.reset();imageToPlane.reset();invalidate();}
        int getCeilingColor(){return ceilingColor;} void setCeilingColor(int v){ceilingColor=v;invalidate();}
        boolean isCeilingPreviewHidden(){return hideCeilingPreview;} void toggleCeilingPreview(){hideCeilingPreview=!hideCeilingPreview;invalidate();}
        int getKelvin(){return kelvin;} void setKelvin(int v){kelvin=v;invalidate();}
        int getLineMm(){return lineMm;} void setLineMm(int v){lineMm=v;invalidate();}
        boolean isOrtho(){return ortho;} void toggleOrtho(){ortho=!ortho;invalidate();}
        boolean isTrackBlack(){return trackBlack;} void setTrackBlack(boolean v){trackBlack=v;invalidate();}
        int getTrackHeadStyle(){return trackHeadStyle;} void setTrackHeadStyle(int v){trackHeadStyle=v;invalidate();}
        int getSpotStyle(){return spotStyle;} void setSpotStyle(int v){spotStyle=v;invalidate();}
        int getChandelierStyle(){return chandelierStyle;} void setChandelierStyle(int v){chandelierStyle=v;invalidate();}
        float getFixtureScale(){return fixtureScale;} void setFixtureScale(float v){fixtureScale=v;invalidate();}
        int getFixtureRotation(){return fixtureRotation;} void setFixtureRotation(int v){fixtureRotation=v;invalidate();}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            if(bitmap==null){drawEmpty(c);return;}
            c.drawBitmap(bitmap,null,imageRect(),paint);
            if(!showAfter)return;

            if(mask.size()>=3&&!hideCeilingPreview){
                Path p=pathImage(mask,true);
                paint.setStyle(Paint.Style.FILL);paint.setColor(withAlpha(ceilingColor,95));c.drawPath(p,paint);
                if(mode==Mode.CEILING){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dpF(1.4f));paint.setColor(Color.rgb(167,224,75));c.drawPath(p,paint);}
            }

            if(mode==Mode.PERSPECTIVE&&!perspective.isEmpty()){
                Path p=pathImage(perspective,perspective.size()==4);
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dpF(1.6f));paint.setColor(Color.rgb(167,224,75));c.drawPath(p,paint);
                paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dpF(10));
                for(int i=0;i<perspective.size();i++){
                    PointF s=toScreen(perspective.get(i));paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(167,224,75));c.drawCircle(s.x,s.y,dpF(8),paint);
                    paint.setColor(Color.rgb(18,21,23));c.drawText(String.valueOf(i+1),s.x,s.y+dpF(3.5f),paint);
                }
            }

            if(draftMask.size()>1){
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dpF(2));paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(Color.rgb(167,224,75));c.drawPath(pathImage(draftMask,false),paint);
            }

            for(PlaneSegment s:lines)drawLight(c,s);
            for(PlaneSegment s:tracks)drawTrack(c,s);
            for(Fixture f:spots)drawSpot(c,f);
            for(Fixture f:chandeliers)drawChandelier(c,f);

            if(mode==Mode.VIEW && hasSelection()) drawSelectionOverlay(c);

            if(startPlane!=null&&currentPlane!=null){
                PlaneSegment tmp=new PlaneSegment(startPlane,currentPlane);
                tmp.kelvin=kelvin;tmp.widthMm=lineMm;tmp.black=trackBlack;tmp.style=trackHeadStyle;
                if(mode==Mode.LINE)drawLight(c,tmp);
                if(mode==Mode.TRACK)drawTrack(c,tmp);
            }
        }

        private void drawEmpty(Canvas c){
            paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setColor(Color.WHITE);paint.setTextSize(dpF(18));
            c.drawText("Визуализатор потолков",getWidth()/2f,getHeight()/2f-dpF(12),paint);
            paint.setColor(Color.rgb(155,163,169));paint.setTextSize(dpF(13));c.drawText("Выберите фотографию помещения",getWidth()/2f,getHeight()/2f+dpF(18),paint);
        }

        private void drawLight(Canvas c,PlaneSegment s){
            if(ortho&&mode==Mode.LINE&&startPlane!=null){
                PointF elbow=orthoElbow(s.a,s.b);
                PlaneSegment a=new PlaneSegment(s.a,elbow);a.kelvin=s.kelvin;a.widthMm=s.widthMm;
                PlaneSegment b=new PlaneSegment(elbow,s.b);b.kelvin=s.kelvin;b.widthMm=s.widthMm;
                drawLightCore(c,a);drawLightCore(c,b);
            }else drawLightCore(c,s);
        }
        private void drawLightCore(Canvas c,PlaneSegment s){
            float base=s.widthMm<=20?2.2f:s.widthMm>=50?5.0f:3.2f;
            int lc=lightColor(s.kelvin);
            float glow1=base*3.0f, glow2=base*1.7f, core=base, hot=Math.max(1.2f,base*0.42f);
            int glow1c=withAlpha(lc,16), glow2c=withAlpha(lc,42), corec=withAlpha(Color.WHITE,228), hotc=withAlpha(lc,248);
            drawPlaneStrip(c,s.a,s.b,glow1,glow1c);
            drawPlaneCircle(c,s.a,glow1/2f,glow1c); drawPlaneCircle(c,s.b,glow1/2f,glow1c);
            drawPlaneStrip(c,s.a,s.b,glow2,glow2c);
            drawPlaneCircle(c,s.a,glow2/2f,glow2c); drawPlaneCircle(c,s.b,glow2/2f,glow2c);
            drawPlaneStrip(c,s.a,s.b,core,corec);
            drawPlaneCircle(c,s.a,core/2f,corec); drawPlaneCircle(c,s.b,core/2f,corec);
            drawPlaneStrip(c,s.a,s.b,hot,hotc);
            drawPlaneCircle(c,s.a,hot/2f,hotc); drawPlaneCircle(c,s.b,hot/2f,hotc);
        }

        private void drawTrack(Canvas c,PlaneSegment s){
            if(ortho&&mode==Mode.TRACK&&startPlane!=null){
                PointF elbow=orthoElbow(s.a,s.b);
                PlaneSegment a=cloneTrack(s,s.a,elbow), b=cloneTrack(s,elbow,s.b);
                drawTrackCore(c,a);drawTrackCore(c,b);
            }else drawTrackCore(c,s);
        }
        private PlaneSegment cloneTrack(PlaneSegment src,PointF a,PointF b){PlaneSegment n=new PlaneSegment(a,b);n.black=src.black;n.style=src.style;return n;}
        private void drawTrackCore(Canvas c,PlaneSegment s){
            int base=s.black?Color.rgb(22,22,22):Color.rgb(238,238,236);
            int edge=s.black?Color.rgb(80,82,83):Color.rgb(165,168,170);
            int edgeC=withAlpha(edge,220), baseC=withAlpha(base,250);
            drawPlaneStrip(c,s.a,s.b,7f,edgeC);
            drawPlaneCircle(c,s.a,3.5f,edgeC); drawPlaneCircle(c,s.b,3.5f,edgeC);
            drawPlaneStrip(c,s.a,s.b,5f,baseC);
            drawPlaneCircle(c,s.a,2.5f,baseC); drawPlaneCircle(c,s.b,2.5f,baseC);

            float d=planeDistance(s.a,s.b);
            int count=Math.max(1,Math.min(6,(int)(d/190f)));
            for(int i=0;i<count;i++){
                float t=(i+1f)/(count+1f);
                PointF p=lerp(s.a,s.b,t);
                drawTrackHead(c,p,s.a,s.b,s.style,s.black);
            }
        }

        private void drawTrackHead(Canvas c,PointF p,PointF a,PointF b,int style,boolean black){
            PointF s=planeToScreen(p);
            PointF sx=planeToScreen(add(p,100,0));
            PointF sy=planeToScreen(add(p,0,100));
            float local=(distance(s,sx)+distance(s,sy))/200f;
            local=Math.max(0.42f,Math.min(1.35f,local));

            int body=black?Color.rgb(29,30,30):Color.rgb(235,236,233);
            int rim=black?Color.rgb(84,86,87):Color.rgb(175,177,176);
            int lens=lightColor(kelvin);

            float stem=dpF(7f)*local;
            float bw=dpF(10f)*local;
            float bh=dpF(17f)*local;

            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpF(2.2f)*local);
            paint.setColor(body);
            c.drawLine(s.x,s.y,s.x,s.y+stem,paint);

            RectF bodyRect=new RectF(s.x-bw/2f,s.y+stem-bh*0.12f,s.x+bw/2f,s.y+stem+bh*0.88f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(body);
            c.drawRoundRect(bodyRect,bw*0.45f,bw*0.45f,paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpF(1f)*local);
            paint.setColor(rim);
            c.drawOval(new RectF(s.x-bw*0.38f,s.y+stem+bh*0.58f,s.x+bw*0.38f,s.y+stem+bh*0.88f),paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(lens);
            c.drawOval(new RectF(s.x-bw*0.26f,s.y+stem+bh*0.64f,s.x+bw*0.26f,s.y+stem+bh*0.83f),paint);

            paint.setColor(withAlpha(lens,18));
            c.drawCircle(s.x,s.y+stem+bh*1.05f,bw*1.35f,paint);
        }

        private void drawSpot(Canvas c,Fixture f){
            String spotKey;
            switch(f.style){
                case 0: spotKey="spot_recessed_white"; break;
                case 1: spotKey="spot_recessed_black"; break;
                case 2: spotKey="spot_surface_white"; break;
                case 3: spotKey="spot_surface_black"; break;
                case 4: spotKey="spot_gimbal_white"; break;
                default: spotKey="spot_gimbal_black"; break;
            }
            Bitmap real=remoteAssets.get(spotKey);
            if(real!=null){
                float size=43f*f.scale;
                drawBillboardAtPlane(c,real,f.p,size,0f,250,0.50f);
                PointF glowCenter=add(f.p,0,5f*f.scale);
                drawPlaneCircle(c,glowCenter,size*0.42f,withAlpha(lightColor(f.kelvin),9));
                return;
            }

            int white=Color.rgb(240,241,238), black=Color.rgb(35,36,36);
            boolean isBlack=(f.style==1||f.style==3||f.style==5);
            int body=isBlack?black:white;
            int rim=isBlack?Color.rgb(92,94,95):Color.rgb(185,187,185);
            float s=42f*f.scale;
            if(f.style<=1){
                drawPlaneCircle(c,f.p,s*0.75f,withAlpha(Color.BLACK,45));
                drawPlaneCircle(c,f.p,s*0.62f,rim);
                drawPlaneCircle(c,f.p,s*0.52f,body);
                drawPlaneCircle(c,f.p,s*0.28f,lightColor(f.kelvin));
                drawPlaneCircle(c,f.p,s*1.15f,withAlpha(lightColor(f.kelvin),15));
            }else if(f.style<=3){
                PointF sh=add(f.p,8,10);
                drawPlaneCircle(c,sh,s*0.62f,withAlpha(Color.BLACK,35));
                drawPlaneCircle(c,f.p,s*0.62f,rim);
                drawPlaneCircle(c,f.p,s*0.54f,body);
                drawPlaneCircle(c,add(f.p,0,5),s*0.30f,lightColor(f.kelvin));
                drawPlaneCircle(c,add(f.p,0,18),s*1.10f,withAlpha(lightColor(f.kelvin),14));
            }else{
                PointF q=add(f.p,18,18);
                drawPlaneLine(c,f.p,q,7,body);
                drawPlaneCircle(c,q,s*0.58f,rim);
                drawPlaneCircle(c,q,s*0.48f,body);
                drawPlaneCircle(c,add(q,5,7),s*0.25f,lightColor(f.kelvin));
                drawPlaneCircle(c,add(q,20,26),s,withAlpha(lightColor(f.kelvin),14));
            }
        }

        private void drawChandelier(Canvas c,Fixture f){
            String chKey=null;
            switch(f.style){
                case 0: chKey="ch_ring_black"; break;
                case 1: chKey="ch_double_gold"; break;
                case 2: chKey="ch_linear"; break;
                case 3: chKey="ch_three_globes"; break;
                case 4: chKey="ch_five_globes"; break;
                case 5: chKey="ch_cascade"; break;
                case 6: chKey="ch_flush"; break;
                case 7: chKey="ch_cluster"; break;
            }
            Bitmap real=chKey==null?null:remoteAssets.get(chKey);
            if(real!=null){
                float planeWidth;
                if(f.style==0) planeWidth=150f;
                else if(f.style==3) planeWidth=165f;
                else planeWidth=150f;
                drawPendantBitmap(c,real,f.p,planeWidth*f.scale,f.rotation,252);
                return;
            }

            float sc=f.scale;
            float rot=(float)Math.toRadians(f.rotation);
            int metal=Color.rgb(221,222,218), dark=Color.rgb(49,51,51), light=lightColor(3500);
            if(f.style==0||f.style==1){
                int rings=f.style==0?1:2;
                for(int i=0;i<rings;i++){
                    float r=(58f+i*24f)*sc;
                    drawPlaneCircleStroke(c,f.p,r,10f*sc,withAlpha(light,210));
                    drawPlaneCircleStroke(c,f.p,r,3f*sc,metal);
                }
                return;
            }
            if(f.style==2){
                PointF a=rotateAround(add(f.p,-110f*sc,0),f.p,rot), b=rotateAround(add(f.p,110f*sc,0),f.p,rot);
                drawPlaneLine(c,a,b,8f*sc,dark); drawPlaneLine(c,a,b,3f*sc,light);
                for(int i=-2;i<=2;i++){PointF q=rotateAround(add(f.p,i*48f*sc,0),f.p,rot);drawPlaneCircle(c,q,10f*sc,metal);drawPlaneCircle(c,q,5f*sc,light);}
                return;
            }
            if(f.style==3||f.style==4){
                int n=f.style==3?3:5; float r=(f.style==3?62:82)*sc;
                for(int i=0;i<n;i++){
                    double a=rot-Math.PI/2+i*Math.PI*2/n;
                    PointF q=add(f.p,(float)Math.cos(a)*r,(float)Math.sin(a)*r);
                    drawPlaneLine(c,f.p,q,4f*sc,dark);drawPlaneCircle(c,q,16f*sc,metal);drawPlaneCircle(c,q,9f*sc,light);
                    drawPlaneCircle(c,q,30f*sc,withAlpha(light,13));
                } drawPlaneCircle(c,f.p,13f*sc,dark); return;
            }
            if(f.style==5){
                for(int i=0;i<5;i++){
                    float x=(i-2)*32f*sc; float y=Math.abs(i-2)*20f*sc+20f*sc;
                    PointF q=rotateAround(add(f.p,x,y),f.p,rot);
                    drawPlaneLine(c,f.p,q,3f*sc,dark);drawPlaneCircle(c,q,13f*sc,metal);drawPlaneCircle(c,q,7f*sc,light);
                } return;
            }
            if(f.style==6){
                drawPlaneCircle(c,f.p,64f*sc,withAlpha(Color.BLACK,30));
                drawPlaneCircle(c,f.p,58f*sc,metal);
                drawPlaneCircle(c,f.p,46f*sc,Color.rgb(245,245,242));
                drawPlaneCircle(c,f.p,33f*sc,light);
                return;
            }
            for(int i=0;i<7;i++){
                double a=rot+i*Math.PI*2/7.0;
                PointF q=add(f.p,(float)Math.cos(a)*58f*sc,(float)Math.sin(a)*58f*sc);
                drawPlaneCircle(c,q,10f*sc,metal);drawPlaneCircle(c,q,5f*sc,light);
            }
        }

        private void preloadAssets(){
            putLocalAsset("spot_recessed_white", R.drawable.spot_white);
            putLocalAsset("spot_surface_white", R.drawable.spot_white);
            putLocalAsset("spot_gimbal_white", R.drawable.spot_white);

            putLocalAsset("spot_recessed_black", R.drawable.spot_black);
            putLocalAsset("spot_surface_black", R.drawable.spot_black);
            putLocalAsset("spot_gimbal_black", R.drawable.spot_black);

            putLocalAsset("ch_ring_black", R.drawable.ring_black);
            putLocalAsset("ch_three_globes", R.drawable.three_globes);
        }

        private void putLocalAsset(String key,int resId){
            try{
                Bitmap b=BitmapFactory.decodeResource(getResources(),resId);
                if(b!=null) remoteAssets.put(key,b.copy(Bitmap.Config.ARGB_8888,false));
            }catch(Exception ignored){}
        }

        private void loadAssetSheet(String cachePrefix,String url,int cols,int rows,String[] names){
            assetPool.execute(()->{
                try{
                    File sheetCache=new File(getContext().getCacheDir(),"cv_"+cachePrefix+"_sheet.png");
                    Bitmap sheet=null;
                    if(sheetCache.exists()){
                        try(FileInputStream in=new FileInputStream(sheetCache)){sheet=BitmapFactory.decodeStream(in);}
                    }
                    if(sheet==null){
                        HttpURLConnection con=(HttpURLConnection)new URL(url).openConnection();
                        con.setConnectTimeout(15000); con.setReadTimeout(22000);
                        con.setRequestProperty("User-Agent","Mozilla/5.0 CeilingVisualizer");
                        con.setInstanceFollowRedirects(true);
                        try(InputStream in=con.getInputStream()){sheet=BitmapFactory.decodeStream(in);}
                        con.disconnect();
                        if(sheet!=null){
                            try(FileOutputStream out=new FileOutputStream(sheetCache)){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}
                        }
                    }
                    if(sheet==null)return;

                    int cellW=sheet.getWidth()/cols;
                    int cellH=sheet.getHeight()/rows;
                    HashMap<String,Bitmap> prepared=new HashMap<>();
                    int index=0;
                    for(int row=0;row<rows;row++){
                        for(int col=0;col<cols&&index<names.length;col++,index++){
                            int x=col*cellW, y=row*cellH;
                            int w=(col==cols-1?sheet.getWidth()-x:cellW);
                            int h=(row==rows-1?sheet.getHeight()-y:cellH);
                            Bitmap cell=Bitmap.createBitmap(sheet,x,y,w,h).copy(Bitmap.Config.ARGB_8888,true);
                            cell=chromaKeyGreen(cell);
                            cell=cropTransparent(cell);
                            cell=scaleDown(cell,520);
                            prepared.put(names[index],cell);
                        }
                    }
                    mainHandler.post(()->{
                        remoteAssets.putAll(prepared);
                        invalidate();
                    });
                }catch(Exception ignored){}
            });
        }

        private Bitmap chromaKeyGreen(Bitmap src){
            Bitmap b=src.copy(Bitmap.Config.ARGB_8888,true);
            int w=b.getWidth(),h=b.getHeight();
            int[] px=new int[w*h];b.getPixels(px,0,w,0,0,w,h);
            for(int i=0;i<px.length;i++){
                int c=px[i];
                int r=Color.red(c),g=Color.green(c),bl=Color.blue(c);
                int greenDominance=g-Math.max(r,bl);
                if(g>145 && greenDominance>45){
                    int alpha;
                    if(greenDominance>105 && g>190) alpha=0;
                    else alpha=Math.max(0,Math.min(255,255-(greenDominance-45)*4));
                    int nr=Math.min(255,(int)(r*1.05f));
                    int nb=Math.min(255,(int)(bl*1.05f));
                    px[i]=Color.argb(alpha,nr,Math.min(g,Math.max(nr,nb)+18),nb);
                }
            }
            b.setPixels(px,0,w,0,0,w,h);
            return b;
        }

        private Bitmap scaleDown(Bitmap src,int max){
            int w=src.getWidth(),h=src.getHeight();
            if(Math.max(w,h)<=max)return src.copy(Bitmap.Config.ARGB_8888,true);
            float s=max/(float)Math.max(w,h);
            return Bitmap.createScaledBitmap(src,Math.max(1,Math.round(w*s)),Math.max(1,Math.round(h*s)),true).copy(Bitmap.Config.ARGB_8888,true);
        }

        private Bitmap cropTrackHead(Bitmap src){
            int w=src.getWidth(),h=src.getHeight();
            int left=Math.max(0,(int)(w*0.39f));
            int top=Math.max(0,(int)(h*0.15f));
            int right=Math.min(w,(int)(w*0.95f));
            int bottom=Math.min(h,(int)(h*0.68f));
            return Bitmap.createBitmap(src,left,top,Math.max(1,right-left),Math.max(1,bottom-top)).copy(Bitmap.Config.ARGB_8888,true);
        }

        private Bitmap removeEdgeBackground(Bitmap src){
            Bitmap b=src.copy(Bitmap.Config.ARGB_8888,true);
            int w=b.getWidth(),h=b.getHeight(),n=w*h;
            int[] px=new int[n]; b.getPixels(px,0,w,0,0,w,h);
            boolean[] seen=new boolean[n];
            int[] q=new int[Math.max(16,n*4+2*w+2*h)]; int head=0,tail=0;
            int bg=averageCorners(px,w,h);
            for(int x=0;x<w;x++){q[tail++]=x;q[tail++]=(h-1)*w+x;}
            for(int y=1;y<h-1;y++){q[tail++]=y*w;q[tail++]=y*w+w-1;}
            while(head<tail){
                int idx=q[head++]; if(idx<0||idx>=n||seen[idx])continue;
                int color=px[idx];
                if(!nearBackground(color,bg))continue;
                seen[idx]=true; px[idx]=color&0x00FFFFFF;
                int x=idx%w,y=idx/w;
                if(x>0)q[tail++]=idx-1;
                if(x<w-1)q[tail++]=idx+1;
                if(y>0)q[tail++]=idx-w;
                if(y<h-1)q[tail++]=idx+w;
                if(tail>=q.length-4)break;
            }
            b.setPixels(px,0,w,0,0,w,h);
            return b;
        }

        private int averageCorners(int[] px,int w,int h){
            int[] ids={0,w-1,(h-1)*w,h*w-1};
            int r=0,g=0,b=0; for(int id:ids){int c=px[id];r+=Color.red(c);g+=Color.green(c);b+=Color.blue(c);}
            return Color.rgb(r/4,g/4,b/4);
        }

        private boolean nearBackground(int c,int bg){
            int dr=Color.red(c)-Color.red(bg),dg=Color.green(c)-Color.green(bg),db=Color.blue(c)-Color.blue(bg);
            int d=dr*dr+dg*dg+db*db;
            int spread=Math.max(Color.red(c),Math.max(Color.green(c),Color.blue(c)))-Math.min(Color.red(c),Math.min(Color.green(c),Color.blue(c)));
            return d<2800 || (Color.red(c)>235&&Color.green(c)>235&&Color.blue(c)>235&&spread<18);
        }

        private Bitmap removeWhiteMatte(Bitmap src){
            Bitmap b=src.copy(Bitmap.Config.ARGB_8888,true);
            int w=b.getWidth(),h=b.getHeight();
            int[] px=new int[w*h]; b.getPixels(px,0,w,0,0,w,h);
            for(int i=0;i<px.length;i++){
                int c=px[i],a=Color.alpha(c),r=Color.red(c),g=Color.green(c),bl=Color.blue(c);
                int mn=Math.min(r,Math.min(g,bl));
                int mx=Math.max(r,Math.max(g,bl));
                int spread=mx-mn;
                if(mn>=246 && spread<=18){
                    px[i]=c&0x00FFFFFF;
                }else if(mn>=224 && spread<=24){
                    int na=Math.min(a,Math.max(0,(246-mn)*12));
                    px[i]=Color.argb(na,r,g,bl);
                }
            }
            b.setPixels(px,0,w,0,0,w,h);
            return b;
        }

        private boolean hasUsefulTransparency(Bitmap b){
            int w=b.getWidth(),h=b.getHeight(),step=Math.max(1,Math.min(w,h)/80);
            int total=0,transparent=0;
            for(int y=0;y<h;y+=step){
                for(int x=0;x<w;x+=step){
                    total++;
                    if(Color.alpha(b.getPixel(x,y))<32)transparent++;
                }
            }
            return total>0 && transparent>total/20;
        }

        private Bitmap cropTransparent(Bitmap b){
            int w=b.getWidth(),h=b.getHeight(),minX=w,minY=h,maxX=-1,maxY=-1;
            int[] row=new int[w];
            for(int y=0;y<h;y++){b.getPixels(row,0,w,0,y,w,1);for(int x=0;x<w;x++){if(Color.alpha(row[x])>8){if(x<minX)minX=x;if(x>maxX)maxX=x;if(y<minY)minY=y;if(y>maxY)maxY=y;}}}
            if(maxX<minX||maxY<minY)return b;
            int pad=3;minX=Math.max(0,minX-pad);minY=Math.max(0,minY-pad);maxX=Math.min(w-1,maxX+pad);maxY=Math.min(h-1,maxY+pad);
            return Bitmap.createBitmap(b,minX,minY,maxX-minX+1,maxY-minY+1);
        }

        private void drawBitmapOnPlane(Canvas c,Bitmap bmp,PointF center,float planeW,float planeH,float angle,int alpha){
            float ux=(float)Math.cos(angle),uy=(float)Math.sin(angle),nx=-uy,ny=ux;
            float hw=planeW/2f,hh=planeH/2f;
            PointF[] pp={
                    add(center,-ux*hw-nx*hh,-uy*hw-ny*hh),
                    add(center, ux*hw-nx*hh, uy*hw-ny*hh),
                    add(center, ux*hw+nx*hh, uy*hw+ny*hh),
                    add(center,-ux*hw+nx*hh,-uy*hw+ny*hh)
            };
            float[] src={0,0,bmp.getWidth(),0,bmp.getWidth(),bmp.getHeight(),0,bmp.getHeight()};
            float[] dst=new float[8];
            for(int i=0;i<4;i++){PointF s=planeToScreen(pp[i]);dst[i*2]=s.x;dst[i*2+1]=s.y;}
            Matrix m=new Matrix(); if(!m.setPolyToPoly(src,0,dst,0,4))return;
            paint.setAlpha(alpha); c.drawBitmap(bmp,m,paint); paint.setAlpha(255);
        }

        private void drawBillboardAtPlane(Canvas c,Bitmap bmp,PointF anchor,float planeWidth,float rotationDeg,int alpha,float anchorYFraction){
            PointF s=planeToScreen(anchor);
            PointF x2=planeToScreen(add(anchor,100,0));
            PointF y2=planeToScreen(add(anchor,0,100));
            float localScale=(distance(s,x2)+distance(s,y2))/200f;
            localScale=Math.max(0.06f,Math.min(2.8f,localScale));
            float w=planeWidth*localScale;
            float h=w*bmp.getHeight()/Math.max(1f,bmp.getWidth());
            float top=s.y-h*anchorYFraction;
            RectF dst=new RectF(s.x-w/2f,top,s.x+w/2f,top+h);
            c.save();
            c.rotate(rotationDeg,s.x,s.y);
            paint.setAlpha(alpha);
            c.drawBitmap(bmp,null,dst,paint);
            paint.setAlpha(255);
            c.restore();
        }

        private void drawPendantBitmap(Canvas c,Bitmap bmp,PointF anchor,float planeWidth,int rotation,int alpha){
            drawBillboardAtPlane(c,bmp,anchor,planeWidth,rotation,alpha,0.10f);
        }

        private void drawPlaneStrip(Canvas c,PointF a,PointF b,float width,int color){
            float dx=b.x-a.x,dy=b.y-a.y,len=Math.max(1f,(float)Math.hypot(dx,dy));
            float nx=-dy/len,ny=dx/len,h=width/2f;
            PointF[] pts={add(a,nx*h,ny*h),add(b,nx*h,ny*h),add(b,-nx*h,-ny*h),add(a,-nx*h,-ny*h)};
            drawPlanePolygon(c,pts,color);
        }
        private void drawPlaneRect(Canvas c,PointF center,float ux,float uy,float length,float width,int color){
            float nx=-uy,ny=ux,hl=length/2f,hw=width/2f;
            PointF[] pts={
                    add(center,-ux*hl+nx*hw,-uy*hl+ny*hw),
                    add(center, ux*hl+nx*hw, uy*hl+ny*hw),
                    add(center, ux*hl-nx*hw, uy*hl-ny*hw),
                    add(center,-ux*hl-nx*hw,-uy*hl-ny*hw)
            }; drawPlanePolygon(c,pts,color);
        }
        private void drawPlanePolygon(Canvas c,PointF[] plane,int color){
            if(plane.length==0)return; Path p=new Path();PointF s=planeToScreen(plane[0]);p.moveTo(s.x,s.y);
            for(int i=1;i<plane.length;i++){s=planeToScreen(plane[i]);p.lineTo(s.x,s.y);}p.close();
            paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawPath(p,paint);
        }
        private void drawPlaneCircle(Canvas c,PointF center,float r,int color){
            Path p=new Path();int n=28;for(int i=0;i<n;i++){double a=i*Math.PI*2/n;PointF q=planeToScreen(add(center,(float)Math.cos(a)*r,(float)Math.sin(a)*r));if(i==0)p.moveTo(q.x,q.y);else p.lineTo(q.x,q.y);}p.close();
            paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawPath(p,paint);
        }
        private void drawPlaneCircleStroke(Canvas c,PointF center,float r,float width,int color){
            Path outer=new Path();Path inner=new Path();int n=32;
            for(int i=0;i<n;i++){double a=i*Math.PI*2/n;PointF q=planeToScreen(add(center,(float)Math.cos(a)*r,(float)Math.sin(a)*r));if(i==0)outer.moveTo(q.x,q.y);else outer.lineTo(q.x,q.y);}
            for(int i=n-1;i>=0;i--){double a=i*Math.PI*2/n;PointF q=planeToScreen(add(center,(float)Math.cos(a)*(r-width),(float)Math.sin(a)*(r-width)));if(i==n-1)inner.moveTo(q.x,q.y);else inner.lineTo(q.x,q.y);}
            outer.close();inner.close();Path ring=new Path();ring.addPath(outer);ring.addPath(inner);ring.setFillType(Path.FillType.EVEN_ODD);
            paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawPath(ring,paint);
        }
        private void drawPlaneLine(Canvas c,PointF a,PointF b,float width,int color){drawPlaneStrip(c,a,b,width,color);}

        private PointF rotateAround(PointF p,PointF c,float angle){
            float x=p.x-c.x,y=p.y-c.y,co=(float)Math.cos(angle),si=(float)Math.sin(angle);
            return new PointF(c.x+x*co-y*si,c.y+x*si+y*co);
        }

        private int lightColor(int k){
            if(k<=3000)return Color.rgb(255,211,135);
            if(k>=4000)return Color.rgb(246,249,255);
            return Color.rgb(255,230,178);
        }

        private void rebuildPerspective(){
            perspectiveReady=false;
            if(bitmap==null||perspective.size()!=4)return;
            orderPerspective();
            float[] src={0,0,1000,0,1000,1000,0,1000};
            float[] dst=new float[8];
            for(int i=0;i<4;i++){dst[i*2]=perspective.get(i).x;dst[i*2+1]=perspective.get(i).y;}
            planeToImage.reset();
            perspectiveReady=planeToImage.setPolyToPoly(src,0,dst,0,4);
            if(perspectiveReady)planeToImage.invert(imageToPlane);
        }

        private void orderPerspective(){
            if(perspective.size()!=4)return;
            ArrayList<PointF> pts=new ArrayList<>();for(PointF p:perspective)pts.add(copy(p));
            pts.sort(Comparator.comparingDouble(p->p.y));
            PointF t1=pts.get(0),t2=pts.get(1),b1=pts.get(2),b2=pts.get(3);
            PointF tl=t1.x<t2.x?t1:t2, tr=t1.x<t2.x?t2:t1;
            PointF bl=b1.x<b2.x?b1:b2, br=b1.x<b2.x?b2:b1;
            perspective.clear();perspective.add(tl);perspective.add(tr);perspective.add(br);perspective.add(bl);
        }

        private PointF planeToImagePoint(PointF p){
            if(!perspectiveReady){
                if(bitmap==null)return copy(p);
                return new PointF(p.x/1000f*bitmap.getWidth(),p.y/1000f*bitmap.getHeight());
            }
            float[] a={p.x,p.y};planeToImage.mapPoints(a);return new PointF(a[0],a[1]);
        }
        private PointF imageToPlanePoint(PointF p){
            if(!perspectiveReady){
                if(bitmap==null)return copy(p);
                return new PointF(p.x/bitmap.getWidth()*1000f,p.y/bitmap.getHeight()*1000f);
            }
            float[] a={p.x,p.y};imageToPlane.mapPoints(a);return new PointF(a[0],a[1]);
        }
        private PointF planeToScreen(PointF p){return toScreen(planeToImagePoint(p));}

        private Path pathImage(List<PointF> pts,boolean close){
            Path p=new Path();if(pts.isEmpty())return p;PointF s=toScreen(pts.get(0));p.moveTo(s.x,s.y);
            for(int i=1;i<pts.size();i++){s=toScreen(pts.get(i));p.lineTo(s.x,s.y);}if(close)p.close();return p;
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(bitmap==null)return true;
            int action=e.getActionMasked();

            if(mode==Mode.PREVIEW){
                if(action==MotionEvent.ACTION_UP&&previewTap!=null)previewTap.run();
                return true;
            }

            if(action==MotionEvent.ACTION_POINTER_DOWN&&e.getPointerCount()>=2){
                beginPinch(e);draftMask.clear();startPlane=null;currentPlane=null;suppressSingleTouch=true;invalidate();return true;
            }
            if(pinching&&e.getPointerCount()>=2&&action==MotionEvent.ACTION_MOVE){applyPinch(e);return true;}
            if(pinching&&action==MotionEvent.ACTION_POINTER_UP){pinching=false;pinchSelection=false;notifySelectionChanged();return true;}
            if(suppressSingleTouch){if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)suppressSingleTouch=false;return true;}

            float x=e.getX(),y=e.getY();
            if(mode==Mode.VIEW){
                handleViewEdit(e,x,y);
                return true;
            }
            PointF img=toImage(x,y);if(img==null)return true;

            if(mode==Mode.PERSPECTIVE){
                if(action==MotionEvent.ACTION_UP){
                    if(perspective.size()==4)perspective.clear();
                    perspective.add(img);
                    if(perspective.size()==4)rebuildPerspective();
                    invalidate();
                }return true;
            }

            if(mode==Mode.CEILING){
                if(action==MotionEvent.ACTION_DOWN){draftMask.clear();draftMask.add(img);}
                else if(action==MotionEvent.ACTION_MOVE){PointF last=draftMask.get(draftMask.size()-1);if(distance(last,img)>6)draftMask.add(img);invalidate();}
                else if(action==MotionEvent.ACTION_UP&&draftMask.size()>=3){push();mask.clear();mask.addAll(draftMask);draftMask.clear();invalidate();}
                return true;
            }

            PointF plane=imageToPlanePoint(img);
            if(mode==Mode.SPOT||mode==Mode.CHANDELIER){
                if(action==MotionEvent.ACTION_UP){
                    if(!insidePlane(plane))return true;
                    push();Fixture f=new Fixture(plane);f.scale=fixtureScale;f.kelvin=kelvin;f.rotation=fixtureRotation;
                    if(mode==Mode.SPOT){f.style=spotStyle;spots.add(f);}else{f.style=chandelierStyle;chandeliers.add(f);}invalidate();
                }return true;
            }

            if(action==MotionEvent.ACTION_DOWN){startPlane=clampPlane(plane);currentPlane=startPlane;}
            else if(action==MotionEvent.ACTION_MOVE){currentPlane=clampPlane(plane);invalidate();}
            else if(action==MotionEvent.ACTION_UP){
                currentPlane=clampPlane(plane);
                if(startPlane!=null&&planeDistance(startPlane,currentPlane)>20){
                    push();
                    if(ortho){
                        PointF elbow=orthoElbow(startPlane,currentPlane);
                        if(mode==Mode.LINE){addLine(startPlane,elbow);addLine(elbow,currentPlane);}
                        if(mode==Mode.TRACK){addTrack(startPlane,elbow);addTrack(elbow,currentPlane);}
                    }else{
                        if(mode==Mode.LINE)addLine(startPlane,currentPlane);
                        if(mode==Mode.TRACK)addTrack(startPlane,currentPlane);
                    }
                } startPlane=null;currentPlane=null;invalidate();
            }
            return true;
        }

        private void addLine(PointF a,PointF b){
            if(planeDistance(a,b)<8)return;PlaneSegment s=new PlaneSegment(a,b);s.kelvin=kelvin;s.widthMm=lineMm;lines.add(s);
        }
        private void addTrack(PointF a,PointF b){
            if(planeDistance(a,b)<8)return;PlaneSegment s=new PlaneSegment(a,b);s.black=trackBlack;s.style=trackHeadStyle;tracks.add(s);
        }
        private boolean insidePlane(PointF p){return p.x>=0&&p.x<=1000&&p.y>=0&&p.y<=1000;}
        private PointF clampPlane(PointF p){return new PointF(clamp(p.x,0,1000),clamp(p.y,0,1000));}
        private PointF orthoElbow(PointF a,PointF b){return Math.abs(b.x-a.x)>=Math.abs(b.y-a.y)?new PointF(b.x,a.y):new PointF(a.x,b.y);}

        private void beginPinch(MotionEvent e){
            float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1);
            pinchStartDistance=(float)Math.hypot(dx,dy);
            pinchSelection=(mode==Mode.VIEW && selectedIsFixture());
            if(pinchSelection){
                Fixture f=getSelectedFixture();
                pinchSelectedStartScale=f==null?1f:f.scale;
                push();
            }else{
                pinchStartZoom=zoom;pinchStartPanX=panX;pinchStartPanY=panY;
                pinchStartMidX=(e.getX(0)+e.getX(1))/2f;pinchStartMidY=(e.getY(0)+e.getY(1))/2f;
            }
            pinching=true;
        }
        private void applyPinch(MotionEvent e){
            float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy);if(pinchStartDistance<=0)return;
            float ratio=d/pinchStartDistance;
            if(pinchSelection){
                Fixture f=getSelectedFixture();
                if(f!=null){f.scale=clamp(pinchSelectedStartScale*ratio,0.35f,1.60f);invalidate();}
                return;
            }
            float mx=(e.getX(0)+e.getX(1))/2f,my=(e.getY(0)+e.getY(1))/2f;
            zoom=clamp(pinchStartZoom*ratio,1f,6f);panX=pinchStartPanX+(mx-pinchStartMidX);panY=pinchStartPanY+(my-pinchStartMidY);
            constrainPan();invalidate();
        }
        private void handlePan(MotionEvent e,float x,float y){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN){lastX=x;lastY=y;moving=true;}
            else if(a==MotionEvent.ACTION_MOVE&&moving){panX+=x-lastX;panY+=y-lastY;lastX=x;lastY=y;constrainPan();invalidate();}
            else if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)moving=false;
        }

        private void constrainPan(){
            if(bitmap==null||getWidth()<=0||getHeight()<=0)return;
            float iw=bitmap.getWidth(),ih=bitmap.getHeight();
            float base=Math.max(getWidth()/iw,getHeight()/ih);
            float w=iw*base*zoom,h=ih*base*zoom;
            float maxX=Math.max(0f,(w-getWidth())/2f);
            float maxY=Math.max(0f,(h-getHeight())/2f);
            panX=clamp(panX,-maxX,maxX);
            panY=clamp(panY,-maxY,maxY);
        }
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);constrainPan();}
        private void handleViewEdit(MotionEvent e,float x,float y){
            int a=e.getActionMasked();
            PointF img=toImage(x,y);
            PointF plane=img==null?null:imageToPlanePoint(img);

            if(a==MotionEvent.ACTION_DOWN){
                int[] hit=hitTest(x,y);
                if(hit[0]!=SEL_NONE){
                    boolean changed=selectedType!=hit[0]||selectedIndex!=hit[1];
                    selectedType=hit[0];selectedIndex=hit[1];
                    draggingSelection=true;moving=false;dragLastPlane=plane;
                    push();
                    if(changed)notifySelectionChanged();
                    invalidate();
                }else{
                    clearSelection(true);
                    handlePan(e,x,y);
                }
                return;
            }

            if(a==MotionEvent.ACTION_MOVE && draggingSelection && plane!=null && dragLastPlane!=null){
                float dx=plane.x-dragLastPlane.x,dy=plane.y-dragLastPlane.y;
                moveSelectedBy(dx,dy);
                dragLastPlane=plane;
                invalidate();
                return;
            }

            if((a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL) && draggingSelection){
                draggingSelection=false;dragLastPlane=null;invalidate();return;
            }

            handlePan(e,x,y);
        }

        private int[] hitTest(float x,float y){
            float fixtureHit=dpF(30);
            for(int i=chandeliers.size()-1;i>=0;i--){
                PointF s=planeToScreen(chandeliers.get(i).p);
                float r=Math.max(fixtureHit,dpF(42)*chandeliers.get(i).scale);
                if((float)Math.hypot(x-s.x,y-s.y)<=r)return new int[]{SEL_CHANDELIER,i};
            }
            for(int i=spots.size()-1;i>=0;i--){
                PointF s=planeToScreen(spots.get(i).p);
                float r=Math.max(dpF(22),dpF(26)*spots.get(i).scale);
                if((float)Math.hypot(x-s.x,y-s.y)<=r)return new int[]{SEL_SPOT,i};
            }
            float segHit=dpF(18);
            for(int i=tracks.size()-1;i>=0;i--){
                PointF a=planeToScreen(tracks.get(i).a),b=planeToScreen(tracks.get(i).b);
                if(distanceToSegment(x,y,a,b)<=segHit)return new int[]{SEL_TRACK,i};
            }
            for(int i=lines.size()-1;i>=0;i--){
                PointF a=planeToScreen(lines.get(i).a),b=planeToScreen(lines.get(i).b);
                if(distanceToSegment(x,y,a,b)<=segHit)return new int[]{SEL_LINE,i};
            }
            return new int[]{SEL_NONE,-1};
        }

        private float distanceToSegment(float x,float y,PointF a,PointF b){
            float vx=b.x-a.x,vy=b.y-a.y,wx=x-a.x,wy=y-a.y;
            float vv=vx*vx+vy*vy;if(vv<0.001f)return (float)Math.hypot(wx,wy);
            float t=clamp((wx*vx+wy*vy)/vv,0f,1f);
            float px=a.x+t*vx,py=a.y+t*vy;
            return (float)Math.hypot(x-px,y-py);
        }

        private void moveSelectedBy(float dx,float dy){
            if(selectedType==SEL_SPOT && selectedIndex<spots.size()){
                Fixture f=spots.get(selectedIndex);f.p=clampPlane(add(f.p,dx,dy));return;
            }
            if(selectedType==SEL_CHANDELIER && selectedIndex<chandeliers.size()){
                Fixture f=chandeliers.get(selectedIndex);f.p=clampPlane(add(f.p,dx,dy));return;
            }
            PlaneSegment s=null;
            if(selectedType==SEL_LINE && selectedIndex<lines.size())s=lines.get(selectedIndex);
            if(selectedType==SEL_TRACK && selectedIndex<tracks.size())s=tracks.get(selectedIndex);
            if(s!=null){
                float minX=Math.min(s.a.x,s.b.x),maxX=Math.max(s.a.x,s.b.x);
                float minY=Math.min(s.a.y,s.b.y),maxY=Math.max(s.a.y,s.b.y);
                dx=clamp(dx,-minX,1000f-maxX);dy=clamp(dy,-minY,1000f-maxY);
                s.a=add(s.a,dx,dy);s.b=add(s.b,dx,dy);
            }
        }

        private void drawSelectionOverlay(Canvas c){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dpF(1.8f));paint.setColor(Color.rgb(167,224,75));
            if(selectedType==SEL_SPOT && selectedIndex<spots.size()){
                PointF s=planeToScreen(spots.get(selectedIndex).p);float r=dpF(24)*spots.get(selectedIndex).scale;
                c.drawCircle(s.x,s.y,Math.max(dpF(18),r),paint);
                c.drawLine(s.x,s.y-r-dpF(12),s.x,s.y-r,paint);
                c.drawCircle(s.x,s.y-r-dpF(15),dpF(4),paint);
            }else if(selectedType==SEL_CHANDELIER && selectedIndex<chandeliers.size()){
                PointF s=planeToScreen(chandeliers.get(selectedIndex).p);float r=dpF(42)*chandeliers.get(selectedIndex).scale;
                c.drawCircle(s.x,s.y,Math.max(dpF(28),r),paint);
                c.drawLine(s.x,s.y-r-dpF(14),s.x,s.y-r,paint);
                c.drawCircle(s.x,s.y-r-dpF(17),dpF(4),paint);
            }else{
                PlaneSegment seg=null;
                if(selectedType==SEL_LINE && selectedIndex<lines.size())seg=lines.get(selectedIndex);
                if(selectedType==SEL_TRACK && selectedIndex<tracks.size())seg=tracks.get(selectedIndex);
                if(seg!=null){
                    PointF a=planeToScreen(seg.a),b=planeToScreen(seg.b);
                    c.drawLine(a.x,a.y,b.x,b.y,paint);
                    paint.setStyle(Paint.Style.FILL);
                    c.drawCircle(a.x,a.y,dpF(5),paint);c.drawCircle(b.x,b.y,dpF(5),paint);
                }
            }
        }

        private RectF imageRect(){
            float iw=bitmap.getWidth(),ih=bitmap.getHeight(),base=Math.max(getWidth()/iw,getHeight()/ih);
            float w=iw*base*zoom,h=ih*base*zoom,cx=getWidth()/2f+panX,cy=getHeight()/2f+panY;
            return new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2);
        }
        private PointF toImage(float x,float y){
            RectF r=imageRect();if(!r.contains(x,y))return null;
            return new PointF((x-r.left)*bitmap.getWidth()/r.width(),(y-r.top)*bitmap.getHeight()/r.height());
        }
        private PointF toScreen(PointF p){
            RectF r=imageRect();return new PointF(r.left+p.x*r.width()/bitmap.getWidth(),r.top+p.y*r.height()/bitmap.getHeight());
        }

        private static PointF add(PointF p,float x,float y){return new PointF(p.x+x,p.y+y);}
        private static PointF copy(PointF p){return new PointF(p.x,p.y);}
        private static PointF lerp(PointF a,PointF b,float t){return new PointF(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t);}
        private static float distance(PointF a,PointF b){return (float)Math.hypot(a.x-b.x,a.y-b.y);}
        private static float planeDistance(PointF a,PointF b){return distance(a,b);}
        private static float clamp(float v,float mn,float mx){return Math.max(mn,Math.min(mx,v));}
        private float dpF(float v){return v*density;}
        private static int withAlpha(int c,int a){return Color.argb(a,Color.red(c),Color.green(c),Color.blue(c));}
    }
}
