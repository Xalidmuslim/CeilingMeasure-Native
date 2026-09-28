package com.xalid.ceilingvisualizer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.InputStream;
import java.util.*;

public class MainActivity extends Activity {
    private EditorView editor;
    private TextView status;
    private final int PICK_IMAGE = 1001;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(17,19,21));
        getWindow().setNavigationBarColor(Color.rgb(17,19,21));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17,19,21));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(10), dp(6), dp(8), dp(6));
        top.setBackgroundColor(Color.rgb(26,30,33));

        TextView title = label("Визуализатор", 18, Color.WHITE);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button photo = button("Фото");
        Button undo = button("↶");
        Button compare = button("До/После");
        Button clear = button("Очистить");
        top.addView(photo); top.addView(undo); top.addView(compare); top.addView(clear);

        editor = new EditorView(this);
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(60)));
        root.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1));

        status = label("Выберите фото помещения", 13, Color.rgb(190,198,203));
        status.setGravity(Gravity.CENTER);
        status.setBackgroundColor(Color.rgb(26,30,33));
        status.setPadding(dp(8), dp(4), dp(8), dp(4));
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(34)));

        HorizontalScrollView sc = new HorizontalScrollView(this);
        sc.setHorizontalScrollBarEnabled(false);
        sc.setBackgroundColor(Color.rgb(26,30,33));
        LinearLayout tools = new LinearLayout(this);
        tools.setPadding(dp(8), dp(7), dp(8), dp(9));
        String[] names={"Рука","Потолок","Линия","Трек","Свет","Люстра"};
        EditorView.Mode[] modes={EditorView.Mode.VIEW,EditorView.Mode.CEILING,EditorView.Mode.LINE,EditorView.Mode.TRACK,EditorView.Mode.SPOT,EditorView.Mode.CHANDELIER};
        for(int i=0;i<names.length;i++){
            final int k=i;
            Button x=button(names[i]);
            x.setOnClickListener(v->{ editor.setMode(modes[k]); status.setText(editor.helpText()); });
            tools.addView(x);
        }
        sc.addView(tools);
        root.addView(sc, new LinearLayout.LayoutParams(-1, dp(64)));

        photo.setOnClickListener(v->pickPhoto());
        undo.setOnClickListener(v->editor.undo());
        compare.setOnClickListener(v->editor.toggleAfter());
        clear.setOnClickListener(v->editor.clearAll());
        setContentView(root);
    }

    private void pickPhoto(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i,PICK_IMAGE);
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==PICK_IMAGE && res==RESULT_OK && data!=null && data.getData()!=null){
            try{
                Uri u=data.getData();
                getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try(InputStream in=getContentResolver().openInputStream(u)){
                    Bitmap b=BitmapFactory.decodeStream(in);
                    editor.setBitmap(b);
                    status.setText("Потолок: проведите пальцем по границе нужной области");
                }
            }catch(Exception e){
                Toast.makeText(this,"Не удалось открыть фото",Toast.LENGTH_LONG).show();
            }
        }
    }

    private Button button(String s){
        Button b=new Button(this);
        b.setText(s); b.setTextSize(12); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setPadding(dp(10),0,dp(10),0);
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(38,43,47)); g.setCornerRadius(dp(14));
        b.setBackground(g);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(42));
        p.setMargins(dp(3),dp(3),dp(3),dp(3)); b.setLayoutParams(p);
        return b;
    }

    private TextView label(String s,int sp,int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    public static class EditorView extends View {
        enum Mode { VIEW, CEILING, LINE, TRACK, SPOT, CHANDELIER }
        static class Segment { PointF a,b; Segment(PointF a,PointF b){this.a=a;this.b=b;} }
        static class Snapshot {
            ArrayList<PointF> mask; ArrayList<Segment> lines,tracks; ArrayList<PointF> spots,chandeliers;
            Snapshot(EditorView v){
                mask=copyPts(v.mask); lines=copySeg(v.lines); tracks=copySeg(v.tracks);
                spots=copyPts(v.spots); chandeliers=copyPts(v.chandeliers);
            }
            static ArrayList<PointF> copyPts(List<PointF> x){ ArrayList<PointF> r=new ArrayList<>(); for(PointF p:x)r.add(new PointF(p.x,p.y)); return r; }
            static ArrayList<Segment> copySeg(List<Segment> x){ ArrayList<Segment> r=new ArrayList<>(); for(Segment s:x)r.add(new Segment(new PointF(s.a.x,s.a.y),new PointF(s.b.x,s.b.y))); return r; }
        }

        private Bitmap bitmap;
        private Mode mode=Mode.CEILING;
        private boolean showAfter=true;
        private final ArrayList<PointF> mask=new ArrayList<>(),spots=new ArrayList<>(),chandeliers=new ArrayList<>(),draft=new ArrayList<>();
        private final ArrayList<Segment> lines=new ArrayList<>(),tracks=new ArrayList<>();
        private final ArrayDeque<Snapshot> history=new ArrayDeque<>();
        private PointF start,current;
        private float zoom=1f, panX=0f, panY=0f, lastX,lastY, pinchStartDist=0f, pinchStartZoom=1f;
        private boolean moving=false,pinching=false;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);

        EditorView(Activity c){ super(c); setBackgroundColor(Color.BLACK); }

        void setBitmap(Bitmap b){ bitmap=b; clearState(false); invalidate(); }
        void setMode(Mode m){ mode=m; draft.clear(); start=current=null; invalidate(); }
        String helpText(){
            switch(mode){
                case VIEW:return "Рука: двигайте фото одним пальцем, масштабируйте двумя";
                case CEILING:return "Потолок: обведите пальцем нужную область";
                case LINE:return "Линия: проведите от начала до конца";
                case TRACK:return "Трек: проведите от начала до конца";
                case SPOT:return "Свет: коснитесь места установки";
                case CHANDELIER:return "Люстра: коснитесь центра";
            } return "";
        }
        void push(){ if(history.size()>30) history.removeFirst(); history.addLast(new Snapshot(this)); }
        void undo(){
            if(history.isEmpty()) return;
            Snapshot s=history.removeLast(); restore(s); invalidate();
        }
        void restore(Snapshot s){
            mask.clear();mask.addAll(s.mask); lines.clear();lines.addAll(s.lines); tracks.clear();tracks.addAll(s.tracks);
            spots.clear();spots.addAll(s.spots); chandeliers.clear();chandeliers.addAll(s.chandeliers);
        }
        void toggleAfter(){ showAfter=!showAfter; invalidate(); }
        void clearAll(){ if(mask.isEmpty()&&lines.isEmpty()&&tracks.isEmpty()&&spots.isEmpty()&&chandeliers.isEmpty())return; push(); clearState(false); invalidate(); }
        void clearState(boolean clearBmp){ mask.clear(); lines.clear(); tracks.clear(); spots.clear(); chandeliers.clear(); draft.clear(); history.clear(); if(clearBmp)bitmap=null; }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            if(bitmap==null){ drawEmpty(c); return; }
            RectF dst=imageRect();
            c.drawBitmap(bitmap,null,dst,p);
            if(!showAfter)return;

            if(mask.size()>=3){
                Path path=pathFor(mask,true);
                p.setStyle(Paint.Style.FILL); p.setColor(Color.argb(70,255,255,255)); c.drawPath(path,p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(Color.argb(210,255,255,255)); c.drawPath(path,p);
            }
            if(draft.size()>1){
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(167,224,75));
                c.drawPath(pathFor(draft,false),p);
            }
            for(Segment s:lines) drawLight(c,toScreen(s.a),toScreen(s.b));
            for(Segment s:tracks) drawTrack(c,toScreen(s.a),toScreen(s.b));
            for(PointF q:spots) drawSpot(c,toScreen(q));
            for(PointF q:chandeliers) drawChandelier(c,toScreen(q));
            if(start!=null&&current!=null){
                if(mode==Mode.LINE)drawLight(c,toScreen(start),toScreen(current));
                if(mode==Mode.TRACK)drawTrack(c,toScreen(start),toScreen(current));
            }
        }

        private void drawEmpty(Canvas c){
            p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.WHITE); p.setTextSize(44);
            c.drawText("Визуализатор потолков",getWidth()/2f,getHeight()/2f-20,p);
            p.setColor(Color.rgb(160,168,174)); p.setTextSize(30);
            c.drawText("Нажмите «Фото» сверху",getWidth()/2f,getHeight()/2f+32,p);
        }
        private Path pathFor(List<PointF> pts,boolean close){
            Path path=new Path(); if(pts.isEmpty())return path; PointF a=toScreen(pts.get(0)); path.moveTo(a.x,a.y);
            for(int i=1;i<pts.size();i++){ PointF q=toScreen(pts.get(i)); path.lineTo(q.x,q.y); } if(close)path.close(); return path;
        }
        private void drawLight(Canvas c,PointF a,PointF b){
            p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.argb(55,255,210,120));p.setStrokeWidth(26);c.drawLine(a.x,a.y,b.x,b.y,p);
            p.setColor(Color.argb(125,255,226,163));p.setStrokeWidth(14);c.drawLine(a.x,a.y,b.x,b.y,p);
            p.setColor(Color.rgb(255,248,225));p.setStrokeWidth(5);c.drawLine(a.x,a.y,b.x,b.y,p);
        }
        private void drawTrack(Canvas c,PointF a,PointF b){
            p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.argb(230,15,15,15));p.setStrokeWidth(10);c.drawLine(a.x,a.y,b.x,b.y,p);
            float len=(float)Math.hypot(b.x-a.x,b.y-a.y); int n=Math.max(1,(int)(len/90f));
            p.setStyle(Paint.Style.FILL);
            for(int i=0;i<=n;i++){float t=i/(float)n,x=a.x+(b.x-a.x)*t,y=a.y+(b.y-a.y)*t;p.setColor(Color.LTGRAY);c.drawCircle(x,y,7,p);p.setColor(Color.BLACK);c.drawCircle(x,y,4,p);}
        }
        private void drawSpot(Canvas c,PointF q){
            p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(55,255,225,150));c.drawCircle(q.x,q.y,38,p);p.setColor(Color.rgb(255,248,225));c.drawCircle(q.x,q.y,11,p);p.setColor(Color.DKGRAY);c.drawCircle(q.x,q.y,5,p);
        }
        private void drawChandelier(Canvas c,PointF q){
            p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(45,255,255,255));c.drawCircle(q.x,q.y,50,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(Color.LTGRAY);c.drawCircle(q.x,q.y,25,p);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(255,241,199));c.drawCircle(q.x,q.y,8,p);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            if(bitmap==null)return true;
            int n=e.getPointerCount();
            if(mode==Mode.VIEW && n>=2){
                float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy);
                if(e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){ pinchStartDist=d;pinchStartZoom=zoom;pinching=true; }
                else if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&pinching&&pinchStartDist>0){ zoom=Math.max(1f,Math.min(6f,pinchStartZoom*d/pinchStartDist)); invalidate(); }
                else if(e.getActionMasked()==MotionEvent.ACTION_POINTER_UP){pinching=false;}
                return true;
            }
            float x=e.getX(),y=e.getY();
            if(mode==Mode.VIEW){
                if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=x;lastY=y;moving=true;}
                else if(e.getAction()==MotionEvent.ACTION_MOVE&&moving){panX+=x-lastX;panY+=y-lastY;lastX=x;lastY=y;invalidate();}
                else if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)moving=false;
                return true;
            }
            PointF img=toImage(x,y); if(img==null)return true;
            if(mode==Mode.SPOT||mode==Mode.CHANDELIER){
                if(e.getAction()==MotionEvent.ACTION_UP){push(); if(mode==Mode.SPOT)spots.add(img);else chandeliers.add(img);invalidate();} return true;
            }
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                start=img;current=img;
                if(mode==Mode.CEILING){draft.clear();draft.add(img);}
            }else if(e.getAction()==MotionEvent.ACTION_MOVE){
                current=img;
                if(mode==Mode.CEILING){PointF last=draft.get(draft.size()-1); if(dist(last,img)>8)draft.add(img);}
                invalidate();
            }else if(e.getAction()==MotionEvent.ACTION_UP){
                current=img;
                if(mode==Mode.CEILING&&draft.size()>=3){push();mask.clear();mask.addAll(draft);draft.clear();}
                else if(mode==Mode.LINE&&dist(start,current)>10){push();lines.add(new Segment(start,current));}
                else if(mode==Mode.TRACK&&dist(start,current)>10){push();tracks.add(new Segment(start,current));}
                start=current=null;invalidate();
            }else if(e.getAction()==MotionEvent.ACTION_CANCEL){draft.clear();start=current=null;invalidate();}
            return true;
        }

        private RectF imageRect(){
            float iw=bitmap.getWidth(),ih=bitmap.getHeight(),s=Math.min(getWidth()/iw,getHeight()/ih);
            float w=iw*s*zoom,h=ih*s*zoom,cx=getWidth()/2f+panX,cy=getHeight()/2f+panY;
            return new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2);
        }
        private PointF toImage(float x,float y){
            RectF r=imageRect(); if(!r.contains(x,y))return null;
            return new PointF((x-r.left)*bitmap.getWidth()/r.width(),(y-r.top)*bitmap.getHeight()/r.height());
        }
        private PointF toScreen(PointF q){
            RectF r=imageRect(); return new PointF(r.left+q.x*r.width()/bitmap.getWidth(),r.top+q.y*r.height()/bitmap.getHeight());
        }
        private static float dist(PointF a,PointF b){return (float)Math.hypot(a.x-b.x,a.y-b.y);}
    }
}
