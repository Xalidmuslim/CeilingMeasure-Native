package ru.xalidmuslim.ceilingmeasure;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.ClipData;
import android.view.DragEvent;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class NativePricePresetDialog extends Dialog {
    interface Listener {
        void onApplied();
    }

    private static final int TEXT=NativeVisualTokens.TEXT;
    private static final int MUTED=NativeVisualTokens.MUTED;
    private static final int BLUE=NativeVisualTokens.ACCENT;
    private static final int LINE=NativeVisualTokens.LINE;
    private static final int BG=NativeVisualTokens.APP_SOFT;
    private static final int DANGER=NativeVisualTokens.DANGER;

    private final NativeProjectStore store;
    private final NativeProjectStore.ProjectData current;
    private final List<NativeProjectStore.ProjectData> scopeProjects;
    private final boolean client;
    private final Listener listener;
    private final NativeUiPrefs uiPrefs;
    private boolean manage;
    private boolean reorderMode;
    private LinearLayout content;
    private LinearLayout priceRows;
    private ScrollView priceScroll;
    private ArrayList<String> priceOrder=new ArrayList<>();

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
        this.uiPrefs=NativeUiPrefs.load(context);
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
        int previousY=priceScroll==null?0:priceScroll.getScrollY();
        ScrollView scroll=new ScrollView(getContext());
        priceScroll=scroll;
        final long[] lastScrollTime={0L};
        scroll.setOnDragListener((view,event)->{
            if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)
                return event.getLocalState() instanceof String;
            if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){
                long now=android.os.SystemClock.uptimeMillis();
                if(now-lastScrollTime[0]>=16L){
                    lastScrollTime[0]=now;
                    float edge=dp(64);
                    if(event.getY()<edge)scroll.scrollBy(0,-dp(20));
                    else if(event.getY()>scroll.getHeight()-edge)
                        scroll.scrollBy(0,dp(20));
                }
            }
            return true;
        });
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setVerticalScrollBarEnabled(false);
        content=new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8),dp(10),dp(8),dp(16));
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
        head.addView(title,new LinearLayout.LayoutParams(0,dp(40),1f));
        Button close=button("Закрыть",false);
        close.setOnClickListener(v->dismiss());
        head.addView(close,new LinearLayout.LayoutParams(dp(82),dp(38)));
        content.addView(head);

        NativeProjectStore.PricePresetSnapshot catalog=store.loadPricePresetSnapshot(client);
        HashMap<String,Float> prices=catalog.prices;
        HashSet<String> hidden=catalog.hidden;
        NativeEstimateEngine.Meta[] meta=client
                ?NativeEstimateEngine.CLIENT_META
                :NativeEstimateEngine.INTERNAL_META;

        ArrayList<NativeProjectStore.MaterialData> customs=catalog.custom;
        LinkedHashMap<String,NativeEstimateEngine.Meta> standards=new LinkedHashMap<>();
        LinkedHashMap<String,NativeProjectStore.MaterialData> materials=new LinkedHashMap<>();
        ArrayList<String> available=new ArrayList<>();
        for(NativeEstimateEngine.Meta item:meta){
            String key=NativePriceOrder.standard(item.key);
            standards.put(key,item);
            available.add(key);
        }
        for(NativeProjectStore.MaterialData item:customs){
            String key=NativePriceOrder.custom(item.id);
            materials.put(key,item);
            available.add(key);
        }
        priceOrder=NativePriceOrder.normalized(catalog.order,available);

        LinearLayout toolbar=new LinearLayout(getContext());
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams toolbarParams=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(48));
        toolbarParams.topMargin=dp(8);
        content.addView(toolbar,toolbarParams);
        Button reorder=button(reorderMode?"✓ Готово":"↕ Изменить порядок",false);
        reorder.setTag("native_price_toggle_reorder");
        reorder.setOnClickListener(v->{reorderMode=!reorderMode;render();});
        toolbar.addView(reorder,new LinearLayout.LayoutParams(0,dp(46),1f));

        Button manageButton=button(manage?"✓ Готово":"Редактировать",false);
        manageButton.setTag(client?"native_client_price_manage":"native_purchase_price_manage");
        manageButton.setOnClickListener(v->{manage=!manage;render();});
        LinearLayout.LayoutParams manageParams=new LinearLayout.LayoutParams(0,dp(46),1f);
        manageParams.leftMargin=dp(6);
        toolbar.addView(manageButton,manageParams);

        TextView reorderHint=text(reorderMode
                ?"Зажми маркер ≡ и перетащи позицию"
                :"Для перестановки нажми «Изменить порядок»",
                10.5f,MUTED,false);
        content.addView(reorderHint,top(6));

        priceRows=new LinearLayout(getContext());
        priceRows.setTag(client?"native_client_price_drag_list":"native_purchase_price_drag_list");
        priceRows.setOrientation(LinearLayout.VERTICAL);
        content.addView(priceRows,top(5));

        for(String key:priceOrder){
            NativeEstimateEngine.Meta standard=standards.get(key);
            if(standard!=null){
                boolean isHidden=hidden.contains(standard.key);
                if(isHidden&&!manage)continue;
                addStandardRow(standard,prices,isHidden);
                continue;
            }
            NativeProjectStore.MaterialData material=materials.get(key);
            if(material!=null)addCustomRow(material);
        }

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
                ViewGroup.LayoutParams.MATCH_PARENT,dp(38));
        addp.topMargin=dp(4);
        content.addView(add,addp);

        TextView note=text(
                client
                        ?"Новые комнаты получают этот прайс автоматически. В каждой клиентской смете цену можно изменить вручную."
                        :"Изменения прайса сохраняются сразу. Цена в конкретной комнате после этого всё равно редактируется отдельно.",
                10,MUTED,false);
        content.addView(note,top(5));

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
        actions.addView(room,new LinearLayout.LayoutParams(0,dp(40),1f));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(40),1f);
        ap.leftMargin=dp(6);
        actions.addView(all,ap);
        content.addView(actions,top(6));

        setContentView(scroll);
        if(previousY>0)scroll.post(()->scroll.scrollTo(0,previousY));
    }

    private void addStandardRow(
            NativeEstimateEngine.Meta meta,
            HashMap<String,Float> prices,
            boolean hidden){
        LinearLayout row=card();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setMinimumHeight(dp(56));
        attachReorderHandle(row,NativePriceOrder.standard(meta.key));

        LinearLayout label=new LinearLayout(getContext());
        label.setOrientation(LinearLayout.VERTICAL);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.addView(text(meta.name,12.5f,TEXT,true));
        label.addView(text(meta.unit,10.5f,MUTED,false));
        row.addView(label,new LinearLayout.LayoutParams(
                0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        if(hidden){
            Button restore=button("Вернуть",false);
            restore.setOnClickListener(v->{
                store.setDefaultKeyHidden(client,meta.key,false);
                render();
            });
            row.addView(restore,new LinearLayout.LayoutParams(dp(86),dp(44)));
        }else{
            Runnable editPrice=()->new NativeNumberPadDialog(
                    getContext(),
                    meta.name+" · цена",
                    NativeEstimateEngine.value(prices,meta.key),
                    value->{
                        store.saveDefaultPrice(client,meta.key,value);
                        render();
                    }).show();

            Button price=button(format(NativeEstimateEngine.value(prices,meta.key)),false);
            price.setTag("native_default_price_"+(client?"client_":"purchase_")+meta.key);
            price.setOnClickListener(v->editPrice.run());
            row.addView(price,new LinearLayout.LayoutParams(dp(88),dp(44)));

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
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(44),dp(44));
                rp.leftMargin=dp(4);
                row.addView(remove,rp);
            }
        }
        priceRows.addView(row,top(3));
    }

    private void addCustomRow(NativeProjectStore.MaterialData item){
        LinearLayout row=card();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setMinimumHeight(dp(56));
        attachReorderHandle(row,NativePriceOrder.custom(item.id));

        LinearLayout label=new LinearLayout(getContext());
        label.setOrientation(LinearLayout.VERTICAL);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.addView(text(item.name,12.5f,TEXT,true));
        label.addView(text(item.unit,10.5f,MUTED,false));
        row.addView(label,new LinearLayout.LayoutParams(
                0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

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
        row.addView(price,new LinearLayout.LayoutParams(dp(88),dp(44)));

        Button menu=button("⋯",false);
        menu.setContentDescription("Действия с материалом "+item.name);
        menu.setTag("native_price_custom_menu_"+item.id);
        menu.setTextSize(18);
        menu.setOnClickListener(v->new NativeStyledAlertDialog.Builder(getContext())
                .setTitle(item.name)
                .setItems(new String[]{"Редактировать материал","Удалить материал"},
                        (dialog,which)->{
                            if(which==0){
                                new NativeMaterialEditorDialog(
                                        getContext(),"Редактировать материал",item,false,
                                        material->{store.upsertDefaultCustom(client,material);render();})
                                        .show();
                            }else{
                                new NativeStyledAlertDialog.Builder(getContext())
                                        .setTitle("Удалить материал?")
                                        .setMessage(item.name+" будет удалён из прайса.")
                                        .setNegativeButton("Отмена",null)
                                        .setPositiveButton("Удалить",(d,w)->{
                                            store.deleteDefaultCustom(client,item.id);
                                            render();
                                        }).show();
                            }
                        }).show());
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(44),dp(44));
        mp.leftMargin=dp(4);
        row.addView(menu,mp);

        priceRows.addView(row,top(3));
    }

    private void attachReorderHandle(LinearLayout row,String key){
        row.setTag("native_price_row_"+key);
        Runnable startDrag=()->{
            if(!row.isAttachedToWindow())return;
            ClipData clip=ClipData.newPlainText("priceRow",key);
            row.startDragAndDrop(clip,new View.DragShadowBuilder(row),key,0);
        };
        if(reorderMode){
            Button grip=button("≡",false);
            grip.setTextSize(22);
            grip.setTag("native_price_grip_"+key);
            grip.setContentDescription("Перетащить позицию "+key);
            grip.setPadding(0,0,0,0);
            grip.setBackground(NativeVisualTokens.solid(
                    NativeVisualTokens.ACCENT_SOFT,dp(10)));
            LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(48),dp(48));
            gp.rightMargin=dp(6);
            row.addView(grip,gp);
            NativeFastLongPress.attach(grip,startDrag);
        }
        // Dragging is enabled only in the explicit reorder mode.
        if(reorderMode)NativeFastLongPress.attach(row,startDrag);
        row.setOnDragListener((view,event)->{
            switch(event.getAction()){
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getLocalState() instanceof String;
                case DragEvent.ACTION_DRAG_ENTERED:
                    row.setBackground(NativeVisualTokens.outlined(
                            NativeVisualTokens.ACCENT_SOFT,NativeVisualTokens.ACCENT,
                            dp(16),dp(1)));
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    row.setBackground(NativeVisualTokens.outlined(
                            NativeVisualTokens.SURFACE,NativeVisualTokens.LINE,
                            dp(16),dp(1)));
                    return true;
                case DragEvent.ACTION_DROP:
                    row.setBackground(NativeVisualTokens.outlined(
                            NativeVisualTokens.SURFACE,NativeVisualTokens.LINE,
                            dp(16),dp(1)));
                    String moving=(String)event.getLocalState();
                    ArrayList<String> ordered=NativePriceOrder.move(priceOrder,moving,key);
                    if(!ordered.equals(priceOrder)){
                        priceOrder=ordered;
                        store.saveDefaultPriceOrder(client,priceOrder);
                        content.post(this::render);
                    }
                    return true;
                default: return true;
            }
        });
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
        c.setPadding(dp(8),dp(4),dp(8),dp(4));
        c.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,NativeVisualTokens.LINE,dp(16),dp(1)));
        return c;
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setAllCaps(false);b.setText(value);
        NativeTypography.register(b,11.5f,true,uiPrefs);
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(4),0,dp(4),0);
        b.setBackground(NativeVisualTokens.controlButton(primary,dp(10)));
        return b;
    }

    private TextView text(String value,float sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return NativeTypography.register(t,sp,bold,uiPrefs);
    }

    private LinearLayout.LayoutParams top(int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(margin);
        return p;
    }

    private String format(float value){
        return NativeMoney.format(value);
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
