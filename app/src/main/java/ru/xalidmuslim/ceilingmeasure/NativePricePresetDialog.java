package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

final class NativePricePresetDialog extends Dialog {
    interface Listener {
        void onApplied();
    }

    private static final int TEXT=Color.rgb(35,56,72);
    private static final int MUTED=Color.rgb(116,136,151);
    private static final int BLUE=Color.rgb(47,143,190);
    private static final int LINE=Color.rgb(220,229,235);
    private static final int BG=Color.rgb(246,248,250);
    private static final int DANGER=Color.rgb(190,72,79);

    private final NativeProjectStore store;
    private final NativeProjectStore.ProjectData current;
    private final List<NativeProjectStore.ProjectData> scopeProjects;
    private final boolean client;
    private final Listener listener;
    private boolean manage;
    private LinearLayout content;

    NativePricePresetDialog(
            @NonNull Context context,
            @NonNull NativeProjectStore store,
            @NonNull NativeProjectStore.ProjectData current,
            @NonNull List<NativeProjectStore.ProjectData> scopeProjects,
            boolean client,
            @NonNull Listener listener) {
        super(context);
        this.store=store;
        this.current=current;
        this.scopeProjects=scopeProjects;
        this.client=client;
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
        LinearLayout title=new LinearLayout(getContext());
        title.setOrientation(LinearLayout.VERTICAL);
        title.addView(text(client?"Прайс клиента":"Закупочный прайс",18,TEXT,true));
        title.addView(text(
                client
                        ?"Цены продажи по умолчанию. Они не связаны с закупочным прайсом."
                        :"Твои цены материалов по умолчанию. Новые комнаты получают их автоматически.",
                10,MUTED,false));
        head.addView(title,new LinearLayout.LayoutParams(0,dp(54),1f));
        Button close=button("Закрыть",false);
        close.setOnClickListener(v->dismiss());
        head.addView(close,new LinearLayout.LayoutParams(dp(88),dp(42)));
        content.addView(head);

        HashMap<String,Float> prices=store.loadDefaultPricing(client);
        HashSet<String> hidden=store.loadHiddenDefaultKeys(client);
        NativeEstimateEngine.Meta[] meta=client
                ?NativeEstimateEngine.CLIENT_META
                :NativeEstimateEngine.INTERNAL_META;

        for(NativeEstimateEngine.Meta item:meta){
            boolean isHidden=hidden.contains(item.key);
            if(isHidden&&!manage)continue;
            addStandardRow(item,prices,isHidden);
        }

        for(NativeProjectStore.MaterialData item:store.loadDefaultCustom(client)){
            addCustomRow(item);
        }

        Button manageButton=button(manage?"Готово":"Редактировать материалы",false);
        manageButton.setTag(client?"native_client_price_manage":"native_purchase_price_manage");
        manageButton.setOnClickListener(v->{manage=!manage;render();});
        content.addView(manageButton,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(44)));

        Button add=button("+ Другой материал",false);
        add.setTag(client?"native_client_price_add":"native_purchase_price_add");
        add.setOnClickListener(v->{
            NativeProjectStore.MaterialData item=new NativeProjectStore.MaterialData();
            item.id=NativeProjectStore.newId("mat");
            item.name="Материал";
            item.unit="шт";
            item.price=0f;
            new NativeMaterialEditorDialog(
                    getContext(),
                    client?"Другой материал клиента":"Другой закупочный материал",
                    item,
                    false,
                    material->{
                        store.upsertDefaultCustom(client,material);
                        render();
                    }).show();
        });
        LinearLayout.LayoutParams addp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(44));
        addp.topMargin=dp(6);
        content.addView(add,addp);

        TextView note=text(
                client
                        ?"Новые комнаты получают этот прайс автоматически. В каждой клиентской смете цену можно изменить вручную."
                        :"Изменения прайса сохраняются сразу. Цена в конкретной комнате после этого всё равно редактируется отдельно.",
                10,MUTED,false);
        content.addView(note,top(8));

        LinearLayout actions=new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button room=button("К текущей комнате",false);
        room.setTag("native_price_apply_room");
        room.setOnClickListener(v->apply(false));
        Button all=button(
                client?"Ко всем комнатам сметы":"Ко всем комнатам заказа",
                true);
        all.setTag("native_price_apply_all");
        all.setOnClickListener(v->apply(true));
        actions.addView(room,new LinearLayout.LayoutParams(0,dp(46),1f));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(46),1f);
        ap.leftMargin=dp(6);
        actions.addView(all,ap);
        content.addView(actions,top(10));

        setContentView(scroll);
    }

    private void addStandardRow(
            NativeEstimateEngine.Meta meta,
            HashMap<String,Float> prices,
            boolean hidden){
        LinearLayout row=card();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout label=new LinearLayout(getContext());
        label.setOrientation(LinearLayout.VERTICAL);
        label.addView(text(meta.name,12,TEXT,true));
        label.addView(text(meta.unit,10,MUTED,false));
        row.addView(label,new LinearLayout.LayoutParams(0,dp(46),1f));

        if(hidden){
            Button restore=button("Вернуть",false);
            restore.setOnClickListener(v->{
                store.setDefaultKeyHidden(client,meta.key,false);
                render();
            });
            row.addView(restore,new LinearLayout.LayoutParams(dp(86),dp(40)));
        }else{
            Button price=button(format(NativeEstimateEngine.value(prices,meta.key)),false);
            price.setTag("native_default_price_"+(client?"client_":"purchase_")+meta.key);
            price.setOnClickListener(v->new NativeNumberPadDialog(
                    getContext(),
                    meta.name+" · цена",
                    NativeEstimateEngine.value(prices,meta.key),
                    value->{
                        store.saveDefaultPrice(client,meta.key,value);
                        render();
                    }).show());
            row.addView(price,new LinearLayout.LayoutParams(dp(82),dp(40)));

            boolean removable=client
                    ?!"canvas".equals(meta.key)
                    :!("canvas".equals(meta.key)||"baguette".equals(meta.key));
            if(manage&&removable){
                Button remove=button("×",false);
                remove.setTextColor(DANGER);
                remove.setOnClickListener(v->{
                    store.saveDefaultPrice(client,meta.key,0f);
                    store.setDefaultKeyHidden(client,meta.key,true);
                    render();
                });
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(40),dp(40));
                rp.leftMargin=dp(4);
                row.addView(remove,rp);
            }
        }
        content.addView(row,top(5));
    }

    private void addCustomRow(NativeProjectStore.MaterialData item){
        LinearLayout row=card();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout label=new LinearLayout(getContext());
        label.setOrientation(LinearLayout.VERTICAL);
        label.addView(text(item.name,12,TEXT,true));
        label.addView(text(item.unit,10,MUTED,false));
        row.addView(label,new LinearLayout.LayoutParams(0,dp(46),1f));

        Button price=button(format(item.price),false);
        price.setOnClickListener(v->new NativeNumberPadDialog(
                getContext(),
                item.name+" · цена",
                item.price,
                value->{
                    item.price=Math.max(0f,value);
                    store.upsertDefaultCustom(client,item);
                    render();
                }).show());
        row.addView(price,new LinearLayout.LayoutParams(dp(78),dp(40)));

        Button edit=button("✎",false);
        edit.setOnClickListener(v->new NativeMaterialEditorDialog(
                getContext(),
                "Редактировать материал",
                item,
                false,
                material->{
                    store.upsertDefaultCustom(client,material);
                    render();
                }).show());
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(dp(40),dp(40));
        ep.leftMargin=dp(3);
        row.addView(edit,ep);

        Button remove=button("×",false);
        remove.setTextColor(DANGER);
        remove.setOnClickListener(v->{
            store.deleteDefaultCustom(client,item.id);
            render();
        });
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(40),dp(40));
        rp.leftMargin=dp(3);
        row.addView(remove,rp);

        content.addView(row,top(5));
    }

    private void apply(boolean all){
        if(all){
            List<NativeProjectStore.ProjectData> targets=client?scopeProjects:java.util.Collections.singletonList(current);
            for(NativeProjectStore.ProjectData project:targets){
                for(NativeProjectStore.RoomData room:project.rooms){
                    store.applyDefaultsToRoom(room,client);
                }
                persist(project);
            }
        }else{
            int index=Math.max(0,Math.min(current.activeRoom,current.rooms.size()-1));
            if(!current.rooms.isEmpty()){
                store.applyDefaultsToRoom(current.rooms.get(index),client);
                persist(current);
            }
        }
        listener.onApplied();
        dismiss();
    }

    private void persist(NativeProjectStore.ProjectData p){
        if(p.id.equals(current.id))store.saveCurrent(p);
        else store.updateSavedProject(p);
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(getContext());
        c.setPadding(dp(10),dp(6),dp(10),dp(6));
        c.setBackground(rounded(Color.WHITE,LINE,12,1));
        return c;
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setAllCaps(false);b.setText(value);b.setTextSize(12);
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(primary?BLUE:Color.WHITE,primary?BLUE:LINE,10,1));
        return b;
    }

    private TextView text(String value,float sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);t.setTextSize(sp);t.setTextColor(color);
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

    private String format(float value){
        return String.format(Locale.getDefault(),"%.2f",value)
                .replaceAll("0+$","")
                .replaceAll("[,.]$","");
    }

    private GradientDrawable rounded(int fill,int stroke,int radius,int width){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(dp(radius));
        if(width>0)d.setStroke(dp(width),stroke);
        return d;
    }

    private int dp(int v){
        return Math.round(v*getContext().getResources().getDisplayMetrics().density);
    }
}
