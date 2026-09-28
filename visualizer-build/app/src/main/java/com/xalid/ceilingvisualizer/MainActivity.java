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
                addOption("Сбросить вид",false,()->editor.resetView());
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
                String[] sp={"Встр. белый","Встр. чёрный","Цилиндр белый","Цилиндр чёрный","Поворотный белый","Поворотный чёрный"};
                for(int i=0;i<sp.length;i++){ final int k=i; addOption(sp[i],editor.getSpotStyle()==k,()->{editor.setSpotStyle(k);rebuildOptions();}); }
                addScaleOptions();
                break;
            case CHANDELIER:
                String[] ch={"Кольцо","Два кольца","Линия","3 плафона","5 плафонов","Каскад","Плоская","Точки"};
                for(int i=0;i<ch.length;i++){ final int k=i; addOption(ch[i],editor.getChandelierStyle()==k,()->{editor.setChandelierStyle(k);rebuildOptions();}); }
                addScaleOptions();
                addOption("0°",editor.getFixtureRotation()==0,()->{editor.setFixtureRotation(0);rebuildOptions();});
                addOption("45°",editor.getFixtureRotation()==45,()->{editor.setFixtureRotation(45);rebuildOptions();});
                addOption("90°",editor.getFixtureRotation()==90,()->{editor.setFixtureRotation(90);rebuildOptions();});
                addOption("135°",editor.getFixtureRotation()==135,()->{editor.setFixtureRotation(135);rebuildOptions();});
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
        addOption("80%",editor.getFixtureScale()==0.8f,()->{editor.setFixtureScale(0.8f);rebuildOptions();});
        addOption("100%",editor.getFixtureScale()==1f,()->{editor.setFixtureScale(1f);rebuildOptions();});
        addOption("120%",editor.getFixtureScale()==1.2f,()->{editor.setFixtureScale(1.2f);rebuildOptions();});
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

        private PointF startPlane,currentPlane;
        private float zoom=1f,panX=0f,panY=0f,lastX,lastY;
        private boolean moving=false,pinching=false,suppressSingleTouch=false;
        private float pinchStartDistance,pinchStartZoom,pinchStartPanX,pinchStartPanY,pinchStartMidX,pinchStartMidY;

        private int ceilingColor=Color.rgb(248,248,246);
        private int kelvin=3500;
        private int lineMm=30;
        private boolean ortho=false;
        private boolean trackBlack=true;
        private int trackHeadStyle=0;
        private int spotStyle=0;
        private int chandelierStyle=0;
        private float fixtureScale=1f;
        private int fixtureRotation=0;

        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private Runnable previewTap;

        private final HashMap<String,Bitmap> remoteAssets=new HashMap<>();
        private final ExecutorService assetPool=Executors.newFixedThreadPool(3);
        private final Handler mainHandler=new Handler(Looper.getMainLooper());

        private static final String ASSET_TRACK_BLACK="https://pandaprojects.co.uk/wp-content/uploads/2026/03/TL-TRK90-RAL9005m-Black.jpg";
        private static final String ASSET_TRACK_WHITE="https://pandaprojects.co.uk/wp-content/uploads/2026/03/TL-TRK90-RAL9016m-White.jpg";
        private static final String ASSET_SPOT_WHITE="https://spotwereld.nl/cdn/shop/files/sw-pollux-wit.jpg?v=1786812775&width=600";
        private static final String ASSET_SPOT_BLACK="https://spotwereld.nl/cdn/shop/files/sw-pollux-zwart.jpg?v=1786812775&width=600";
        private static final String ASSET_RING_BLACK="https://images.thdstatic.com/productImages/e4d586721c9a4c3789b50061f252bdd5/svn/black-umeiluce-pendant-lights-mys306-64_1000.jpg";
        private static final String ASSET_RING_GOLD="https://www.lumibox.ca/storage/spec-sheets/luminaire-suspendu-decoratif.png";
        private static final String ASSET_THREE_GLOBES="https://images.thdstatic.com/productImages/9bb1d86b-6d1d-4471-b7b9-fb7727616476/svn/brass-and-frosted-white-pendant-lights-lvn4474brass-c3_600.jpg";

        EditorView(Activity a){
            super(a);
            density=getResources().getDisplayMetrics().density;
            setBackgroundColor(Color.BLACK);
            preloadAssets();
        }

        void setOnTapInPreview(Runnable r){previewTap=r;}
        void setBitmap(Bitmap b){bitmap=b;clearAllInternal();history.clear();clearPerspective();resetView();invalidate();}
        void setMode(Mode m){mode=m;draftMask.clear();startPlane=null;currentPlane=null;invalidate();}
        String helpText(){
            switch(mode){
                case VIEW:return "Рука: перемещение одним пальцем, масштаб двумя";
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
        void undo(){if(history.isEmpty())return;restore(history.removeLast());invalidate();}
        private void restore(Snapshot s){
            mask.clear();mask.addAll(s.mask);
            perspective.clear();perspective.addAll(s.perspective);rebuildPerspective();
            lines.clear();lines.addAll(s.lines);tracks.clear();tracks.addAll(s.tracks);spots.clear();spots.addAll(s.spots);chandeliers.clear();chandeliers.addAll(s.chandeliers);
        }
        void clearAll(){if(lines.isEmpty()&&tracks.isEmpty()&&spots.isEmpty()&&chandeliers.isEmpty()&&mask.isEmpty())return;push();clearAllInternal();invalidate();}
        private void clearAllInternal(){mask.clear();draftMask.clear();lines.clear();tracks.clear();spots.clear();chandeliers.clear();startPlane=null;currentPlane=null;}
        void resetView(){zoom=1f;panX=0f;panY=0f;invalidate();}

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
            float base=s.widthMm<=20?5f:s.widthMm>=50?11f:7f;
            int lc=lightColor(s.kelvin);
            drawPlaneStrip(c,s.a,s.b,base*4.5f,withAlpha(lc,22));
            drawPlaneStrip(c,s.a,s.b,base*2.3f,withAlpha(lc,56));
            drawPlaneStrip(c,s.a,s.b,base,withAlpha(Color.WHITE,238));
            drawPlaneStrip(c,s.a,s.b,Math.max(2f,base*0.35f),withAlpha(lc,250));
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
            drawPlaneStrip(c,s.a,s.b,12f,withAlpha(edge,230));
            drawPlaneStrip(c,s.a,s.b,8f,withAlpha(base,250));

            float d=planeDistance(s.a,s.b);
            int count=Math.max(1,Math.min(6,(int)(d/190f)));
            for(int i=0;i<count;i++){
                float t=(i+1f)/(count+1f);
                PointF p=lerp(s.a,s.b,t);
                drawTrackHead(c,p,s.a,s.b,s.style,s.black);
            }
        }

        private void drawTrackHead(Canvas c,PointF p,PointF a,PointF b,int style,boolean black){
            Bitmap real=remoteAssets.get(black?"track_black":"track_white");
            if(real!=null && style<=1){
                float dx=b.x-a.x,dy=b.y-a.y;
                float angle=(float)Math.atan2(dy,dx);
                drawBitmapOnPlane(c,real,p,120f,160f,angle+(float)Math.PI/2f,255);
                return;
            }

            float dx=b.x-a.x,dy=b.y-a.y,len=Math.max(1f,(float)Math.hypot(dx,dy));
            float nx=-dy/len, ny=dx/len;
            int body=black?Color.rgb(26,27,27):Color.rgb(235,236,233);
            int rim=black?Color.rgb(86,88,89):Color.rgb(170,172,171);
            if(style==2){
                drawPlaneRect(c,p,dx/len,dy/len,46,18,body);
                drawPlaneRect(c,add(p,nx*4,ny*4),dx/len,dy/len,30,8,lightColor(kelvin));
                return;
            }
            if(style==3){
                drawPlaneRect(c,p,dx/len,dy/len,64,16,body);
                drawPlaneRect(c,p,dx/len,dy/len,48,7,lightColor(kelvin));
                return;
            }
            if(style==4){
                PointF q=add(p,nx*42,ny*42);
                drawPlaneLine(c,p,q,5,body);
                drawPlaneCircle(c,q,15,rim);drawPlaneCircle(c,q,11,body);drawPlaneCircle(c,add(q,nx*2,ny*2),6,lightColor(kelvin));
                drawPlaneCircle(c,add(q,nx*22,ny*22),30,withAlpha(lightColor(kelvin),18));
                return;
            }
            int heads=style==5?2:1;
            for(int h=0;h<heads;h++){
                float along=(h-(heads-1)/2f)*36f;
                PointF q=add(p,(dx/len)*along+nx*30,(dy/len)*along+ny*30);
                drawPlaneLine(c,add(p,(dx/len)*along,(dy/len)*along),q,5,body);
                float r=style==1?11:15;
                drawPlaneCircle(c,q,r+3,rim);drawPlaneCircle(c,q,r,body);
                drawPlaneCircle(c,add(q,nx*2,ny*2),r*0.48f,lightColor(kelvin));
                drawPlaneCircle(c,add(q,nx*24,ny*24),r*2.2f,withAlpha(lightColor(kelvin),16));
            }
        }

        private void drawSpot(Canvas c,Fixture f){
            Bitmap real=remoteAssets.get((f.style==1||f.style==3||f.style==5)?"spot_black":"spot_white");
            if(real!=null){
                float size=(f.style<=1?86f:104f)*f.scale;
                drawBitmapOnPlane(c,real,f.p,size,size,0f,245);
                PointF glowCenter=add(f.p,0,10f*f.scale);
                drawPlaneCircle(c,glowCenter,size*0.68f,withAlpha(lightColor(f.kelvin),16));
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
            Bitmap real=null;
            if(f.style==0) real=remoteAssets.get("ring_black");
            else if(f.style==1) real=remoteAssets.get("ring_gold");
            else if(f.style==3) real=remoteAssets.get("three_globes");

            if(real!=null){
                float planeWidth=(f.style==3?300f:280f)*f.scale;
                drawPendantBitmap(c,real,f.p,planeWidth,f.rotation,250);
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
            loadRemoteAsset("track_black",ASSET_TRACK_BLACK,true,true);
            loadRemoteAsset("track_white",ASSET_TRACK_WHITE,true,true);
            loadRemoteAsset("spot_white",ASSET_SPOT_WHITE,true,false);
            loadRemoteAsset("spot_black",ASSET_SPOT_BLACK,true,false);
            loadRemoteAsset("ring_black",ASSET_RING_BLACK,true,false);
            loadRemoteAsset("ring_gold",ASSET_RING_GOLD,true,false);
            loadRemoteAsset("three_globes",ASSET_THREE_GLOBES,true,false);
        }

        private void loadRemoteAsset(String key,String url,boolean removeBackground,boolean trackCrop){
            assetPool.execute(()->{
                try{
                    File cache=new File(getContext().getCacheDir(),"cv_"+key+".png");
                    Bitmap bmp=null;
                    if(cache.exists()){
                        try(FileInputStream in=new FileInputStream(cache)){ bmp=BitmapFactory.decodeStream(in); }
                    }
                    if(bmp==null){
                        HttpURLConnection con=(HttpURLConnection)new URL(url).openConnection();
                        con.setConnectTimeout(12000); con.setReadTimeout(18000);
                        con.setRequestProperty("User-Agent","Mozilla/5.0 CeilingVisualizer");
                        con.setInstanceFollowRedirects(true);
                        try(InputStream in=con.getInputStream()){ bmp=BitmapFactory.decodeStream(in); }
                        con.disconnect();
                        if(bmp!=null){
                            bmp=scaleDown(bmp,720);
                            if(trackCrop) bmp=cropTrackHead(bmp);
                            if(removeBackground) bmp=removeEdgeBackground(bmp);
                            bmp=cropTransparent(bmp);
                            try(FileOutputStream out=new FileOutputStream(cache)){ bmp.compress(Bitmap.CompressFormat.PNG,100,out); }
                        }
                    }
                    if(bmp!=null){
                        Bitmap ready=bmp;
                        mainHandler.post(()->{remoteAssets.put(key,ready);invalidate();});
                    }
                }catch(Exception ignored){}
            });
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

        private void drawPendantBitmap(Canvas c,Bitmap bmp,PointF anchor,float planeWidth,int rotation,int alpha){
            PointF s=planeToScreen(anchor);
            PointF x2=planeToScreen(add(anchor,100,0));
            float pxPerPlane=Math.max(0.08f,distance(s,x2)/100f);
            float w=planeWidth*pxPerPlane;
            float h=w*bmp.getHeight()/Math.max(1f,bmp.getWidth());
            RectF dst=new RectF(s.x-w/2f,s.y-dpF(8),s.x+w/2f,s.y-dpF(8)+h);
            c.save();
            c.rotate(rotation,s.x,s.y);
            paint.setAlpha(alpha); c.drawBitmap(bmp,null,dst,paint); paint.setAlpha(255);
            c.restore();
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
            if(pinching&&action==MotionEvent.ACTION_POINTER_UP){pinching=false;return true;}
            if(suppressSingleTouch){if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)suppressSingleTouch=false;return true;}

            float x=e.getX(),y=e.getY();
            if(mode==Mode.VIEW){handlePan(e,x,y);return true;}
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
            pinchStartDistance=(float)Math.hypot(dx,dy);pinchStartZoom=zoom;pinchStartPanX=panX;pinchStartPanY=panY;
            pinchStartMidX=(e.getX(0)+e.getX(1))/2f;pinchStartMidY=(e.getY(0)+e.getY(1))/2f;pinching=true;
        }
        private void applyPinch(MotionEvent e){
            float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy);if(pinchStartDistance<=0)return;
            float mx=(e.getX(0)+e.getX(1))/2f,my=(e.getY(0)+e.getY(1))/2f;
            zoom=clamp(pinchStartZoom*d/pinchStartDistance,1f,6f);panX=pinchStartPanX+(mx-pinchStartMidX);panY=pinchStartPanY+(my-pinchStartMidY);invalidate();
        }
        private void handlePan(MotionEvent e,float x,float y){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN){lastX=x;lastY=y;moving=true;}
            else if(a==MotionEvent.ACTION_MOVE&&moving){panX+=x-lastX;panY+=y-lastY;lastX=x;lastY=y;invalidate();}
            else if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)moving=false;
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
