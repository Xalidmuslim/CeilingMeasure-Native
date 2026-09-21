package ru.xalidmuslim.ceilingmeasure;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;

final class NativeEstimateDialog extends Dialog {
    enum Mode { CLIENT, INTERNAL }

    interface Listener {
        void onChanged();
    }

    private static final int TEXT=Color.rgb(35,56,72);
    private static final int MUTED=Color.rgb(116,136,151);
    private static final int BLUE=Color.rgb(47,143,190);
    private static final int LINE=Color.rgb(220,229,235);
    private static final int BG=Color.rgb(246,248,250);
    private static final int DANGER=Color.rgb(190,72,79);

    private final NativeProjectStore.ProjectData project;
    private final Mode mode;
    private final Listener listener;
    private LinearLayout content;

    NativeEstimateDialog(
            @NonNull Context context,
            @NonNull NativeProjectStore.ProjectData project,
            @NonNull Mode mode,
            @NonNull Listener listener) {
        super(context);
        this.project=project;
        this.mode=mode;
        this.listener=listener;
    }

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        render();

        Window window=getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width=WindowManager.LayoutParams.MATCH_PARENT;
            lp.height=WindowManager.LayoutParams.MATCH_PARENT;
            lp.gravity=Gravity.CENTER;
            window.setAttributes(lp);
        }
    }

    private void render(){
        ScrollView scroll=new ScrollView(getContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        content=new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12),dp(14),dp(12),dp(28));
        scroll.addView(content,new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head=new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBox=new LinearLayout(getContext());
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text(mode==Mode.CLIENT?"Смета для клиента":"Расчёт по всем комнатам",18,TEXT,true));
        titleBox.addView(text(
                mode==Mode.CLIENT
                        ?"Цены продажи по помещениям"
                        :"Площадь, периметр и полный закупочный расчёт",
                10,MUTED,false));
        head.addView(titleBox,new LinearLayout.LayoutParams(0,dp(52),1f));
        Button close=button("Закрыть",false);
        close.setTag("native_estimate_close");
        close.setOnClickListener(v->dismiss());
        head.addView(close,new LinearLayout.LayoutParams(dp(88),dp(42)));
        content.addView(head);

        NativeEstimateEngine.ProjectCost pc =
                NativeEstimateEngine.projectCost(project,mode==Mode.CLIENT);
        LinearLayout summary=card();
        summary.setTag(mode==Mode.CLIENT?"native_client_summary":"native_internal_summary");
        summary.addView(text(
                mode==Mode.CLIENT?"Итого к оплате":"Итого за все комнаты",
                11,MUTED,false));
        summary.addView(text(money(pc.total)+" ₽",24,TEXT,true),top(3));
        summary.addView(text(
                project.rooms.size()+" помещ. · "+qty(pc.areaM2)+" м² · "+qty(pc.perimeterM)+" м",
                11,MUTED,false),
                top(3));
        content.addView(summary,top(8));

        for(NativeProjectStore.RoomData room:project.rooms){
            addRoom(room);
        }

        setContentView(scroll);
    }

    private void addRoom(NativeProjectStore.RoomData room){
        NativeEstimateEngine.RoomCost cost=mode==Mode.CLIENT
                ?NativeEstimateEngine.clientCost(room)
                :NativeEstimateEngine.internalCost(room);

        LinearLayout card=card();
        card.setTag((mode==Mode.CLIENT?"native_client_room_":"native_internal_room_")+room.id);
        content.addView(card,top(8));

        LinearLayout head=new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left=new LinearLayout(getContext());
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(room.name,15,TEXT,true));
        left.addView(text(
                (cost.metrics.areaM2>0?qty(cost.metrics.areaM2)+" м²":"Площадь —")
                        +" · "
                        +(cost.metrics.perimeterM>0?qty(cost.metrics.perimeterM)+" м":"Периметр —"),
                10,MUTED,false));
        head.addView(left,new LinearLayout.LayoutParams(0,dp(45),1f));
        head.addView(text(money(cost.total)+" ₽",14,TEXT,true));
        card.addView(head);

        for(NativeEstimateEngine.Line line:cost.lines)addLine(card,room,line);

        if(mode==Mode.CLIENT){
            Button add=button("+ Дополнительный материал",false);
            add.setTag("native_client_add_material_"+room.id);
            add.setOnClickListener(v->openMaterialPicker(room));
            card.addView(add,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,dp(42)));
        }

        TextView state=text(
                cost.complete?"Расчёт готов.":"Расчёт неполный: не все обязательные размеры введены.",
                10,
                cost.complete?Color.rgb(58,132,94):MUTED,
                false);
        card.addView(state,top(6));
    }

    private void addLine(
            LinearLayout parent,
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        LinearLayout row=new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,dp(5),0,dp(5));

        LinearLayout nameBox=new LinearLayout(getContext());
        nameBox.setOrientation(LinearLayout.VERTICAL);
        nameBox.addView(text(line.name,12,TEXT,true));
        nameBox.addView(text(qty(line.qty)+" "+line.unit,10,MUTED,false));
        row.addView(nameBox,new LinearLayout.LayoutParams(0,dp(42),1.2f));

        Button qtyButton=button(qty(line.qty),false);
        qtyButton.setTextSize(11);
        qtyButton.setEnabled(line.qtyEditable || (mode==Mode.INTERNAL && isInternalExtra(line.key)));
        qtyButton.setAlpha(qtyButton.isEnabled()?1f:.55f);
        if(qtyButton.isEnabled()){
            qtyButton.setOnClickListener(v->editQty(room,line));
        }
        row.addView(qtyButton,new LinearLayout.LayoutParams(dp(68),dp(38)));

        Button price=button(money(line.price),false);
        price.setTextSize(11);
        price.setTag("native_estimate_price_"+line.key+"_"+room.id);
        price.setOnClickListener(v->editPrice(room,line));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(76),dp(38));
        pp.leftMargin=dp(4);
        row.addView(price,pp);

        TextView sum=text(money(line.total)+" ₽",11,TEXT,true);
        sum.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(90),dp(38));
        sp.leftMargin=dp(4);
        row.addView(sum,sp);

        if(line.removable){
            Button remove=button("×",false);
            remove.setTextColor(DANGER);
            remove.setOnClickListener(v->removeLine(room,line));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(38),dp(38));
            rp.leftMargin=dp(3);
            row.addView(remove,rp);
        }

        parent.addView(row);
    }

    private void editPrice(
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        float current=mode==Mode.CLIENT
                ?NativeEstimateEngine.value(room.clientPricing,line.key)
                :NativeEstimateEngine.value(room.pricing,line.key);
        new NativeNumberPadDialog(
                getContext(),
                line.name+" · цена",
                current,
                value->{
                    if(mode==Mode.CLIENT)room.clientPricing.put(line.key,Math.max(0f,value));
                    else room.pricing.put(line.key,Math.max(0f,value));
                    changed();
                }).show();
    }

    private void editQty(
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        float current=(float)line.qty;
        new NativeNumberPadDialog(
                getContext(),
                line.name+" · количество",
                current,
                value->{
                    if(mode==Mode.CLIENT){
                        room.clientEstimateQty.put(line.key,Math.max(0f,value));
                        room.clientEstimateOverride.add(line.key);
                    }else{
                        room.extraQty.put(line.key,Math.max(0f,value));
                    }
                    changed();
                }).show();
    }

    private void removeLine(
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        if(mode==Mode.CLIENT){
            if("canvas".equals(line.key))return;
            room.clientEstimateQty.put(line.key,0f);
            room.clientEstimateOverride.add(line.key);
        }else{
            if("canvas".equals(line.key)||"baguette".equals(line.key))return;
            room.hiddenInternalKeys.add(line.key);
        }
        changed();
    }

    private void openMaterialPicker(NativeProjectStore.RoomData room){
        String[] names=new String[]{
                "Вставка","Светильники","Карниз","Углы","Трубы",
                "Световые линии","Теневой профиль","Керамогранит"
        };
        String[] keys=new String[]{
                "insert","light","cornice","corner","pipe",
                "lightline","shadow","porcelain"
        };
        new AlertDialog.Builder(getContext())
                .setTitle("Дополнительный материал")
                .setItems(names,(d,which)->{
                    String key=keys[which];
                    double value="insert".equals(key)
                            ?NativeEstimateEngine.metrics(room).perimeterM
                            :NativeEstimateEngine.clientDefaultQty(room,key);
                    room.clientEstimateQty.put(key,(float)Math.max(0d,value));
                    room.clientEstimateOverride.add(key);
                    changed();
                    if(!"insert".equals(key)){
                        NativeEstimateEngine.Meta meta=NativeEstimateEngine.clientMeta(key);
                        new NativeNumberPadDialog(
                                getContext(),
                                meta.name+" · количество",
                                (float)value,
                                qty->{
                                    room.clientEstimateQty.put(key,Math.max(0f,qty));
                                    room.clientEstimateOverride.add(key);
                                    changed();
                                }).show();
                    }
                }).show();
    }

    private boolean isInternalExtra(String key){
        return "light".equals(key)
                ||"corner".equals(key)
                ||"cornice".equals(key)
                ||"lightline".equals(key)
                ||"pipe".equals(key);
    }

    private void changed(){
        listener.onChanged();
        render();
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(getContext());
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(12),dp(10),dp(12),dp(10));
        c.setBackground(rounded(Color.WHITE,LINE,14,1));
        return c;
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        b.setTextSize(12);
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(5),0,dp(5),0);
        b.setBackground(rounded(primary?BLUE:Color.WHITE,primary?BLUE:LINE,10,1));
        return b;
    }

    private TextView text(String value,float sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    private LinearLayout.LayoutParams top(int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(margin);
        return p;
    }

    private GradientDrawable rounded(int fill,int stroke,int radius,int width){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if(width>0)d.setStroke(dp(width),stroke);
        return d;
    }

    private String qty(double value){
        return String.format(Locale.getDefault(),"%.2f",value)
                .replaceAll("0+$","")
                .replaceAll("[,.]$","");
    }

    private String money(double value){
        return String.format(Locale.getDefault(),"%.2f",value)
                .replaceAll("0+$","")
                .replaceAll("[,.]$","");
    }

    private int dp(int v){
        return Math.round(v*getContext().getResources().getDisplayMetrics().density);
    }
}
