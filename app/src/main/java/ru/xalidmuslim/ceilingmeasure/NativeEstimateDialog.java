package ru.xalidmuslim.ceilingmeasure;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.util.TypedValue;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

final class NativeEstimateDialog extends Dialog {
    enum Mode { CLIENT, INTERNAL }

    interface Listener {
        void onChanged();
        default void onHomeRequested() {}
        default void onClientPdfRequested(@NonNull List<NativeProjectStore.ProjectData> projects) {}
        default void onClientPdfShareRequested(@NonNull List<NativeProjectStore.ProjectData> projects) {}
    }

    private static final int TEXT=NativeVisualTokens.TEXT;
    private static final int MUTED=NativeVisualTokens.MUTED;
    private static final int BLUE=NativeVisualTokens.ACCENT;
    private static final int LINE=NativeVisualTokens.LINE;
    private static final int BG=NativeVisualTokens.APP_SOFT;
    private static final int DANGER=NativeVisualTokens.DANGER;

    private final NativeProjectStore store;
    private final NativeProjectStore.ProjectData project;
    private final Mode mode;
    private final Listener listener;
    private final NativeUiPrefs uiPrefs;
    private LinearLayout content;
    private ScrollView estimateScroll;

    NativeEstimateDialog(
            @NonNull Context context,
            @NonNull NativeProjectStore store,
            @NonNull NativeProjectStore.ProjectData project,
            @NonNull Mode mode,
            @NonNull Listener listener) {
        super(context);
        this.store=store;
        this.project=project;
        this.mode=mode;
        this.listener=listener;
        this.uiPrefs=NativeUiPrefs.load(context);
    }

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        NativeFullScreenDialogShell.configure(this);
        render();
        if(content!=null)NativeMotion.enter(content);

        Window window=getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            // Fade-only dialog transition. Never slide or scale the window.
            window.setWindowAnimations(R.style.CeilingFadeWindowAnimation);
            NativeFullScreenDialogShell.configure(this);
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width=WindowManager.LayoutParams.MATCH_PARENT;
            lp.height=WindowManager.LayoutParams.MATCH_PARENT;
            lp.gravity=Gravity.CENTER;
            window.setAttributes(lp);
        }
    }

    private void render(){
        int previousScrollY=estimateScroll==null?0:estimateScroll.getScrollY();
        ScrollView scroll=new ScrollView(getContext());
        estimateScroll=scroll;
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        content=new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(6),dp(3),dp(6),dp(5));
        scroll.addView(content,new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head=new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBox=new LinearLayout(getContext());
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(0,0,0,0);
        titleBox.addView(text(mode==Mode.CLIENT?"Смета для клиента":"Расчёт по всем комнатам",18,TEXT,true));
        titleBox.addView(text(
                mode==Mode.CLIENT
                        ?"Цены продажи по помещениям"
                        :"Площадь, периметр и полный закупочный расчёт",
                10,MUTED,false));
        LinearLayout.LayoutParams titleBoxParams=new LinearLayout.LayoutParams(
                0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        titleBoxParams.rightMargin=dp(6);
        head.addView(titleBox,titleBoxParams);
        Button close=button("Закрыть",false);
        close.setTag("native_estimate_close");
        close.setOnClickListener(v->dismiss());
        close.setSingleLine(true);
        LinearLayout.LayoutParams closeParams=new LinearLayout.LayoutParams(
                dp(82),adaptiveActionHeight(38,false));
        closeParams.gravity=Gravity.TOP;
        head.addView(close,closeParams);
        content.addView(head);

        List<NativeProjectStore.ProjectData> projects=estimateProjects();

        LinearLayout toolbar=new LinearLayout(getContext());
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        if(mode==Mode.CLIENT){
            Button prices=button("Прайс клиента",false);
            prices.setTag("native_client_price_preset");
            prices.setOnClickListener(v->openPricePreset(true,projects));
            Button merge=button(
                    projects.size()>1?"Объединено заказов: "+projects.size():"Объединить заказы",
                    false);
            merge.setTag("native_merge_orders");
            merge.setOnClickListener(v->openMergeOrders());
            prices.setMaxLines(2);
            merge.setMaxLines(2);
            int toolbarHeight=adaptiveActionHeight(40,true);
            toolbar.addView(prices,new LinearLayout.LayoutParams(0,toolbarHeight,1f));
            LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,toolbarHeight,1f);
            mp.leftMargin=dp(6);
            toolbar.addView(merge,mp);
        }else{
            Button prices=button("Закупочный прайс",false);
            prices.setTag("native_purchase_price_preset");
            prices.setOnClickListener(v->openPricePreset(false,projects));
            prices.setMaxLines(2);
            toolbar.addView(prices,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,adaptiveActionHeight(40,true)));
        }
        content.addView(toolbar,top(3));

        NativeEstimateEngine.ProjectCost total=combinedCost(projects);
        int rooms=0;
        for(NativeProjectStore.ProjectData p:projects)rooms+=p.rooms.size();

        LinearLayout summary=card();
        summary.setTag(mode==Mode.CLIENT?"native_client_summary":"native_internal_summary");
        summary.addView(text(
                mode==Mode.CLIENT?"Итого к оплате":"Итого за все комнаты",
                11,MUTED,false));
        TextView grandTotal=text(money(total.total)+" ₽",25,TEXT,true);
        grandTotal.setSingleLine(true);
        grandTotal.setAutoSizeTextTypeUniformWithConfiguration(
                18,Math.max(24,Math.round(24f*uiPrefs.scale)),1,
                TypedValue.COMPLEX_UNIT_SP);
        grandTotal.setTag("native_estimate_grand_total");
        summary.addView(grandTotal,top(1));
        summary.addView(text(
                rooms+" помещ. · "+qty(total.areaM2)+" м² · "+qty(total.perimeterM)+" м",
                11,MUTED,false),
                top(1));
        content.addView(summary,top(4));

        boolean multi=mode==Mode.CLIENT&&projects.size()>1;
        for(NativeProjectStore.ProjectData owner:projects){
            for(NativeProjectStore.RoomData room:owner.rooms){
                addRoom(owner,room,multi);
            }
        }

        if(mode==Mode.CLIENT){
            LinearLayout bottom=new LinearLayout(getContext());
            bottom.setOrientation(LinearLayout.VERTICAL);
            bottom.setClipChildren(false);
            bottom.setClipToPadding(false);

            Button add=button("+ Дополнительный материал",false);
            add.setTag("native_client_add_material");
            add.setOnClickListener(v->openGlobalMaterialPicker(projects));
            add.setMaxLines(2);
            bottom.addView(add,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    adaptiveActionHeight(42,true)));

            LinearLayout pdfActions=new LinearLayout(getContext());
            pdfActions.setOrientation(LinearLayout.HORIZONTAL);
            pdfActions.setBaselineAligned(false);
            pdfActions.setClipChildren(false);
            pdfActions.setClipToPadding(false);

            Button pdf=button("Скачать PDF",false);
            pdf.setTag("native_client_pdf");
            pdf.setSingleLine(true);
            pdf.setOnClickListener(v->listener.onClientPdfRequested(
                    new ArrayList<>(estimateProjects())));

            Button share=button("Отправить PDF",true);
            share.setTag("native_client_pdf_share");
            share.setSingleLine(true);
            share.setOnClickListener(v->listener.onClientPdfShareRequested(
                    new ArrayList<>(estimateProjects())));

            int pdfHeight=adaptiveActionHeight(40,false);
            LinearLayout.LayoutParams saveParams=
                    new LinearLayout.LayoutParams(0,pdfHeight,1f);
            LinearLayout.LayoutParams shareParams=
                    new LinearLayout.LayoutParams(0,pdfHeight,1f);
            shareParams.leftMargin=dp(6);
            pdfActions.addView(pdf,saveParams);
            pdfActions.addView(share,shareParams);

            LinearLayout.LayoutParams pdfRowParams=new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            pdfRowParams.topMargin=dp(5);
            bottom.addView(pdfActions,pdfRowParams);

            content.addView(bottom,top(7));
            content.addView(text(
                    "PDF можно сохранить на устройство или сразу отправить через WhatsApp, Telegram, почту и другие приложения.",
                    9.5f,MUTED,false),top(5));
        }

        setContentView(NativeFullScreenDialogShell.create(this,scroll,
                new NativeFullScreenDialogShell.Action(R.drawable.ic_shell_home,
                        "Домой","native_estimate_nav_home",()->{
                    dismiss();
                    listener.onHomeRequested();
                }),
                new NativeFullScreenDialogShell.Action(R.drawable.ic_shell_undo,
                        "Чертёж","native_estimate_nav_drawing",this::dismiss),
                new NativeFullScreenDialogShell.Action(R.drawable.ic_shell_list,
                        "Прайс","native_estimate_nav_price",
                        ()->openPricePreset(mode==Mode.CLIENT,estimateProjects())),
                new NativeFullScreenDialogShell.Action(R.drawable.ic_shell_more,
                        "Ещё","native_estimate_nav_more",this::showNavigationActions)));
        if(previousScrollY>0)scroll.post(()->scroll.scrollTo(0,previousScrollY));
    }

    private void showNavigationActions(){
        if(mode==Mode.CLIENT){
            new NativeStyledAlertDialog.Builder(getContext())
                    .setTitle("Действия со сметой")
                    .setItems(new String[]{"Скачать PDF","Отправить PDF",
                                    "Добавить материал","Закрыть смету"},
                            (d,which)->{
                                if(which==0)listener.onClientPdfRequested(
                                        new ArrayList<>(estimateProjects()));
                                else if(which==1)listener.onClientPdfShareRequested(
                                        new ArrayList<>(estimateProjects()));
                                else if(which==2)openGlobalMaterialPicker(estimateProjects());
                                else dismiss();
                            }).show();
        }else{
            new NativeStyledAlertDialog.Builder(getContext())
                    .setTitle("Закупочный расчёт")
                    .setItems(new String[]{"Закупочный прайс","Закрыть расчёт"},
                            (d,which)->{
                                if(which==0)openPricePreset(false,estimateProjects());
                                else dismiss();
                            }).show();
        }
    }

    private void addRoom(
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room,
            boolean multi){
        NativeEstimateEngine.RoomCost cost=mode==Mode.CLIENT
                ?NativeEstimateEngine.clientCost(room)
                :NativeEstimateEngine.internalCost(room);

        LinearLayout card=card();
        card.setTag((mode==Mode.CLIENT?"native_client_room_":"native_internal_room_")+room.id);
        content.addView(card,top(2));

        LinearLayout head=new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left=new LinearLayout(getContext());
        left.setPadding(0,0,dp(4),0);
        String roomName=multi
                ?projectLabel(owner)+" · "+room.name
                :room.name;
        String measurements=
                (cost.metrics.areaM2>0?qty(cost.metrics.areaM2)+" м²":"Площадь —")
                        +" · "
                        +(cost.metrics.perimeterM>0?qty(cost.metrics.perimeterM)+" м":"Периметр —");
        // Small and medium room names fit on a single compact header line.
        // Long names, combined orders and large accessibility text retain
        // two lines instead of risking a cropped total.
        boolean inlineRoomMetrics=!multi && roomName.length()<=13
                && getContext().getResources().getConfiguration().screenWidthDp>=360
                && getContext().getResources().getConfiguration().fontScale<=1.10f
                && uiPrefs.scale<=1.10f*NativeUiPrefs.defaults().scale;
        if(inlineRoomMetrics){
            left.setOrientation(LinearLayout.HORIZONTAL);
            left.setGravity(Gravity.CENTER_VERTICAL);
            TextView titleView=text(roomName,14.5f,TEXT,true);
            titleView.setSingleLine(true);
            titleView.setMaxWidth(dp(145));
            left.addView(titleView,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView inlineMetrics=text("  ·  "+measurements,10,MUTED,false);
            inlineMetrics.setTag("native_estimate_room_metrics_"+room.id);
            inlineMetrics.setSingleLine(true);
            inlineMetrics.setEllipsize(android.text.TextUtils.TruncateAt.END);
            left.addView(inlineMetrics,new LinearLayout.LayoutParams(
                    0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        }else{
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(text(roomName,15,TEXT,true));
            TextView wrappedMetrics=text(measurements,10,MUTED,false);
            wrappedMetrics.setTag("native_estimate_room_metrics_"+room.id);
            left.addView(wrappedMetrics);
        }
        head.addView(left,new LinearLayout.LayoutParams(
                0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        TextView roomTotal=text(money(cost.total)+" ₽",14,TEXT,true);
        roomTotal.setSingleLine(true);
        roomTotal.setAutoSizeTextTypeUniformWithConfiguration(
                10,Math.max(14,Math.round(14f*uiPrefs.scale)),1,
                TypedValue.COMPLEX_UNIT_SP);
        roomTotal.setTag("native_estimate_room_total_"+room.id);
        head.addView(roomTotal,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(head);

        // Repeated column captions consumed a full extra row in every room.
        // The estimates are self-explanatory: labels and totals identify each column.
        View roomDivider=new View(getContext());
        roomDivider.setBackgroundColor(NativeVisualTokens.DIVIDER);
        LinearLayout.LayoutParams headingDivider=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(1));
        headingDivider.topMargin=dp(2);
        card.addView(roomDivider,headingDivider);

        for(NativeEstimateEngine.Line line:cost.lines)addLine(card,owner,room,line);

        if(mode==Mode.CLIENT){
            Button add=button("+ Добавить материал",false);
            add.setTag("native_client_add_material_"+room.id);
            add.setTextColor(BLUE);
            add.setBackground(NativeVisualTokens.rippleOutlined(
                    NativeVisualTokens.SURFACE_SOFT,Color.TRANSPARENT,
                    dp(9),0,Color.argb(25,33,110,147)));
            add.setPadding(dp(7),0,dp(7),0);
            add.setOnClickListener(v->openMaterialPicker(owner,room));
            LinearLayout.LayoutParams addParams=new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,adaptiveActionHeight(32,false));
            addParams.topMargin=dp(1);
            card.addView(add,addParams);
        }else{
            Button add=button("+ Другой материал",false);
            add.setTag("native_internal_add_material_"+room.id);
            add.setTextColor(BLUE);
            add.setBackground(NativeVisualTokens.rippleOutlined(
                    NativeVisualTokens.SURFACE_SOFT,Color.TRANSPARENT,
                    dp(9),0,Color.argb(25,33,110,147)));
            add.setPadding(dp(7),0,dp(7),0);
            add.setOnClickListener(v->openAdhocMaterial(owner,room,false));
            card.addView(add,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,adaptiveActionHeight(32,false)));
        }

        // Repeating "Расчёт готов" in every completed room consumed valuable
        // vertical space without adding information. Keep incomplete warnings.
        if(!cost.complete){
            TextView incomplete=text("Не хватает обязательных размеров",10.5f,
                    NativeVisualTokens.DANGER,false);
            incomplete.setTag("native_estimate_incomplete_"+room.id);
            card.addView(incomplete,top(3));
        }

        if(room.notes!=null&&!room.notes.trim().isEmpty()){
            card.addView(text("Примечание: "+room.notes.trim(),10,MUTED,false),top(2));
        }
    }

    /**
     * Professional two-tier estimate row. Every formula uses the same columns
     * and a dedicated trailing action column; no visual padding between
     * quantity and price. Inset control backgrounds keep a 40dp visible input
     * inside a full 48dp touch target.
     */
    private void addLine(
            LinearLayout parent,
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        if(estimateFitsSingleRow(line)){
            addSingleRowLine(parent,owner,room,line);
            return;
        }
        String key=line.key+"_"+room.id;
        NativeEstimateTableColumns columns=estimateColumns();

        LinearLayout item=new LinearLayout(getContext());
        item.setTag("native_estimate_line_"+key);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0,dp(1),0,0);

        LinearLayout heading=new LinearLayout(getContext());
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setMinimumHeight(dp(17));

        TextView title=text(line.name,12.5f,TEXT,true);
        title.setTag("native_estimate_title_"+key);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(
                0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        titleParams.rightMargin=dp(6);
        heading.addView(title,titleParams);

        TextView total=text(NativeMoney.format(line.total)+" ₽",13,TEXT,true);
        total.setTag("native_estimate_total_"+key);
        total.setSingleLine(true);
        total.setMaxWidth(dp(145));
        total.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,
                TypedValue.COMPLEX_UNIT_SP);
        total.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        heading.addView(total,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        item.addView(heading);

        LinearLayout formula=new LinearLayout(getContext());
        formula.setTag("native_estimate_formula_"+key);
        formula.setOrientation(LinearLayout.HORIZONTAL);
        formula.setGravity(Gravity.CENTER_VERTICAL);
        int controlHeight=estimateControlHeight();
        item.addView(formula,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(controlHeight)));

        boolean canEditQty=line.custom||line.qtyEditable
                ||(mode==Mode.INTERNAL&&isInternalExtra(line.key));
        TextView quantity;
        if(canEditQty){
            Button edit=button(qty(line.qty),false);
            edit.setTextSize(12);
            edit.setSingleLine(true);
            edit.setGravity(Gravity.CENTER);
            edit.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,
                    TypedValue.COMPLEX_UNIT_SP);
            edit.setPadding(dp(2),0,dp(2),0);
            edit.setContentDescription("Изменить количество: "+line.name);
            edit.setBackground(estimateFieldBackground(
                    NativeVisualTokens.SURFACE,LINE));
            edit.setOnClickListener(v->editQty(owner,room,line));
            NativeFastLongPress.attach(edit,()->editQty(owner,room,line));
            quantity=edit;
        }else{
            quantity=text(qty(line.qty),12,MUTED,true);
            quantity.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
            quantity.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,
                    TypedValue.COMPLEX_UNIT_SP);
            quantity.setPadding(0,0,dp(8),0);
            quantity.setSingleLine(true);
        }
        quantity.setTag("native_estimate_qty_"+key);
        formula.addView(quantity,new LinearLayout.LayoutParams(
                dp(columns.quantityDp),dp(controlHeight)));

        TextView unit=text(line.unit,11,MUTED,false);
        unit.setGravity(Gravity.CENTER);
        unit.setMaxLines(2);
        unit.setAutoSizeTextTypeUniformWithConfiguration(8,11,1,
                TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams unitParams=new LinearLayout.LayoutParams(
                dp(columns.unitDp),dp(controlHeight));
        unitParams.leftMargin=dp(columns.operandGapDp);
        formula.addView(unit,unitParams);

        TextView times=text("×",13,MUTED,false);
        times.setGravity(Gravity.CENTER);
        formula.addView(times,new LinearLayout.LayoutParams(
                dp(columns.multiplyDp),dp(controlHeight)));

        Button price=button(NativeMoney.format(line.price)+" ₽",false);
        price.setTag("native_estimate_price_"+key);
        price.setSingleLine(true);
        price.setTextSize(12);
        price.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,
                TypedValue.COMPLEX_UNIT_SP);
        price.setGravity(Gravity.CENTER);
        price.setPadding(dp(2),0,dp(2),0);
        price.setContentDescription("Изменить цену: "+line.name);
        price.setBackground(estimateFieldBackground(
                NativeVisualTokens.SURFACE_SOFT,LINE));
        price.setOnClickListener(v->editPrice(owner,room,line));
        LinearLayout.LayoutParams priceParams=new LinearLayout.LayoutParams(
                dp(columns.priceDp),dp(controlHeight));
        priceParams.leftMargin=dp(columns.operandGapDp);
        formula.addView(price,priceParams);

        // The flexible space starts AFTER the price, not inside the formula:
        // the menu stays at the far right edge for every material.
        formula.addView(new View(getContext()),new LinearLayout.LayoutParams(
                0,dp(1),1f));

        if(line.removable){
            Button more=button("⋯",false);
            more.setTag("native_estimate_actions_"+key);
            more.setTextSize(18);
            more.setContentDescription("Действия с позицией "+line.name);
            more.setBackground(estimateFieldBackground(
                    NativeVisualTokens.SURFACE_SOFT,Color.TRANSPARENT));
            more.setOnClickListener(v->
                    new NativeStyledAlertDialog.Builder(getContext())
                            .setTitle(line.name)
                            .setItems(new String[]{"Удалить позицию"},
                                    (dialog,which)->removeLine(owner,room,line))
                            .show());
            formula.addView(more,new LinearLayout.LayoutParams(
                    dp(columns.actionsDp),dp(controlHeight)));
        }else{
            // Blank but fixed-width cell: prices and columns do not shift
            // between removable and automatically calculated positions.
            View placeholder=new View(getContext());
            placeholder.setTag("native_estimate_actions_placeholder_"+key);
            formula.addView(placeholder,new LinearLayout.LayoutParams(
                    dp(columns.actionsDp),dp(controlHeight)));
        }

        parent.addView(item);
        View divider=new View(getContext());
        divider.setBackgroundColor(NativeVisualTokens.DIVIDER);
        parent.addView(divider,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(1)));
    }

    /**
     * A true one-row material entry on regular-width phones. Its 42dp visual
     * height replaces the former 17dp heading + 42dp formula + padding;
     * long descriptions and accessibility text safely use the two-tier row.
     *
     * The same tags and edit callbacks are used in both representations.
     */
    private void addSingleRowLine(
            LinearLayout parent,
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        String key=line.key+"_"+room.id;

        LinearLayout item=new LinearLayout(getContext());
        item.setTag("native_estimate_line_"+key);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(0,0,0,0);

        TextView title=text(line.name,12,TEXT,true);
        title.setTag("native_estimate_title_"+key);
        title.setMaxLines(2);
        title.setSingleLine(true);
        title.setAutoSizeTextTypeUniformWithConfiguration(10,12,1,
                TypedValue.COMPLEX_UNIT_SP);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(
                dp(82),dp(42));
        titleParams.rightMargin=dp(3);
        title.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(title,titleParams);

        LinearLayout formula=new LinearLayout(getContext());
        formula.setTag("native_estimate_formula_"+key);
        formula.setOrientation(LinearLayout.HORIZONTAL);
        formula.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(formula,new LinearLayout.LayoutParams(
                0,dp(42),1f));

        boolean canEditQty=line.custom||line.qtyEditable
                ||(mode==Mode.INTERNAL&&isInternalExtra(line.key));
        TextView quantity;
        if(canEditQty){
            Button edit=button(qty(line.qty),false);
            edit.setTextSize(11.5f);
            edit.setSingleLine(true);
            edit.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,
                    TypedValue.COMPLEX_UNIT_SP);
            edit.setGravity(Gravity.CENTER);
            edit.setPadding(dp(2),0,dp(2),0);
            edit.setBackground(estimateFieldBackground(
                    NativeVisualTokens.SURFACE,LINE));
            edit.setContentDescription("Изменить количество: "+line.name);
            edit.setOnClickListener(v->editQty(owner,room,line));
            NativeFastLongPress.attach(edit,()->editQty(owner,room,line));
            quantity=edit;
        }else{
            quantity=text(qty(line.qty),11,MUTED,true);
            quantity.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
            quantity.setSingleLine(true);
            quantity.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,
                    TypedValue.COMPLEX_UNIT_SP);
            quantity.setPadding(0,0,dp(3),0);
        }
        quantity.setTag("native_estimate_qty_"+key);
        formula.addView(quantity,new LinearLayout.LayoutParams(dp(46),dp(42)));

        TextView unit=text(line.unit,10.5f,MUTED,false);
        unit.setGravity(Gravity.CENTER);
        unit.setSingleLine(true);
        unit.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,
                TypedValue.COMPLEX_UNIT_SP);
        formula.addView(unit,new LinearLayout.LayoutParams(dp(20),dp(42)));

        TextView times=text("×",11,MUTED,false);
        times.setGravity(Gravity.CENTER);
        formula.addView(times,new LinearLayout.LayoutParams(dp(10),dp(42)));

        Button price=button(NativeMoney.format(line.price)+" ₽",false);
        price.setTag("native_estimate_price_"+key);
        price.setSingleLine(true);
        price.setTextSize(11);
        price.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,
                TypedValue.COMPLEX_UNIT_SP);
        price.setGravity(Gravity.CENTER);
        price.setPadding(dp(2),0,dp(2),0);
        price.setBackground(estimateFieldBackground(
                NativeVisualTokens.SURFACE_SOFT,LINE));
        price.setContentDescription("Изменить цену: "+line.name);
        price.setOnClickListener(v->editPrice(owner,room,line));
        LinearLayout.LayoutParams priceParams=new LinearLayout.LayoutParams(
                dp(76),dp(42));
        priceParams.leftMargin=dp(3);
        formula.addView(price,priceParams);

        TextView total=text(NativeMoney.format(line.total)+" ₽",11.5f,TEXT,true);
        total.setTag("native_estimate_total_"+key);
        total.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        total.setSingleLine(true);
        total.setPadding(dp(2),0,dp(3),0);
        total.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,
                TypedValue.COMPLEX_UNIT_SP);
        formula.addView(total,new LinearLayout.LayoutParams(
                0,dp(42),1f));

        if(line.removable){
            Button more=button("⋯",false);
            more.setTag("native_estimate_actions_"+key);
            more.setTextSize(16);
            more.setContentDescription("Действия с позицией "+line.name);
            more.setBackground(estimateFieldBackground(
                    NativeVisualTokens.SURFACE_SOFT,Color.TRANSPARENT));
            more.setOnClickListener(v->
                    new NativeStyledAlertDialog.Builder(getContext())
                            .setTitle(line.name)
                            .setItems(new String[]{"Удалить позицию"},
                                    (dialog,which)->removeLine(owner,room,line))
                            .show());
            formula.addView(more,new LinearLayout.LayoutParams(
                    dp(44),dp(42)));
        }else{
            View empty=new View(getContext());
            empty.setTag("native_estimate_actions_placeholder_"+key);
            formula.addView(empty,new LinearLayout.LayoutParams(
                    dp(44),dp(42)));
        }

        parent.addView(item);
        View divider=new View(getContext());
        divider.setBackgroundColor(NativeVisualTokens.DIVIDER);
        parent.addView(divider,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(1)));
    }

    private boolean estimateFitsSingleRow(NativeEstimateEngine.Line line){
        int width=getContext().getResources().getConfiguration().screenWidthDp;
        float system=getContext().getResources().getConfiguration().fontScale;
        float custom=uiPrefs.scale/NativeUiPrefs.defaults().scale;
        return NativeEstimateDensePolicy.oneRow(width,system,custom,
                line.name,line.unit,NativeMoney.format(line.price),
                NativeMoney.format(line.total),qty(line.qty));
    }

    private int estimateControlHeight(){
        // Default 42dp instead of 48dp. Larger system/app font scales retain
        // more room automatically, without returning to oversized normal rows.
        float system=getContext().getResources().getConfiguration().fontScale;
        float custom=uiPrefs.scale/NativeUiPrefs.defaults().scale;
        if(Math.max(system,custom)>=1.25f)return 48;
        if(Math.max(system,custom)>=1.10f)return 44;
        return 42;
    }

    private NativeEstimateTableColumns estimateColumns(){
        int width=getContext().getResources().getConfiguration().screenWidthDp;
        if(width<=0){
            float density=getContext().getResources().getDisplayMetrics().density;
            width=Math.round(getContext().getResources().getDisplayMetrics().widthPixels
                    /Math.max(1f,density));
        }
        return NativeEstimateTableColumns.forScreenWidthDp(width);
    }

    private InsetDrawable estimateFieldBackground(int fill,int stroke){
        return new InsetDrawable(NativeVisualTokens.rippleOutlined(
                fill,stroke,dp(7),stroke==Color.TRANSPARENT?0:dp(1),
                Color.argb(24,33,110,147)),
                0,dp(5),0,dp(5));
    }

    private void editPrice(
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        float current=(float)line.price;
        new NativeNumberPadDialog(
                getContext(),
                line.name+" · цена",
                current,
                value->{
                    if(line.custom){
                        NativeProjectStore.MaterialData item=findCustom(room,line.customId,mode==Mode.CLIENT);
                        if(item!=null)item.price=Math.max(0f,value);
                    }else if(mode==Mode.CLIENT){
                        room.clientPricing.put(line.key,Math.max(0f,value));
                    }else{
                        room.pricing.put(line.key,Math.max(0f,value));
                    }
                    changedValue(owner,room);
                }).show();
    }

    private void editQty(
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        new NativeNumberPadDialog(
                getContext(),
                line.name+" · количество",
                (float)line.qty,
                value->{
                    if(line.custom){
                        NativeProjectStore.MaterialData item=findCustom(room,line.customId,mode==Mode.CLIENT);
                        if(item!=null)item.qty=Math.max(0f,value);
                    }else if(mode==Mode.CLIENT){
                        room.clientEstimateQty.put(line.key,Math.max(0f,value));
                        room.clientEstimateOverride.add(line.key);
                    }else{
                        room.extraQty.put(line.key,Math.max(0f,value));
                    }
                    changedValue(owner,room);
                }).show();
    }

    private void removeLine(
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room,
            NativeEstimateEngine.Line line){
        if(line.custom){
            (mode==Mode.CLIENT?room.clientCustomItems:room.customItems)
                    .removeIf(item->line.customId.equals(item.id));
        }else if(mode==Mode.CLIENT){
            if("canvas".equals(line.key))return;
            room.clientEstimateQty.put(line.key,0f);
            room.clientEstimateOverride.add(line.key);
        }else{
            if("canvas".equals(line.key)||"baguette".equals(line.key))return;
            room.hiddenInternalKeys.add(line.key);
        }
        changed(owner);
    }

    private void openGlobalMaterialPicker(
            List<NativeProjectStore.ProjectData> projects){
        ArrayList<NativeProjectStore.ProjectData> owners=new ArrayList<>();
        ArrayList<NativeProjectStore.RoomData> rooms=new ArrayList<>();
        ArrayList<String> labels=new ArrayList<>();
        boolean multi=projects.size()>1;

        for(NativeProjectStore.ProjectData owner:projects){
            for(NativeProjectStore.RoomData room:owner.rooms){
                owners.add(owner);
                rooms.add(room);
                NativeEstimateEngine.Metrics metrics=NativeEstimateEngine.metrics(room);
                String prefix=multi?projectLabel(owner)+" · ":"";
                labels.add(
                        prefix+room.name+" · "
                                +qty(metrics.areaM2)+" м² · "
                                +qty(metrics.perimeterM)+" м");
            }
        }
        if(rooms.isEmpty())return;
        if(rooms.size()==1){
            openMaterialPicker(owners.get(0),rooms.get(0));
            return;
        }

        new NativeStyledAlertDialog.Builder(getContext())
                .setTitle("В какую комнату добавить?")
                .setItems(labels.toArray(new String[0]),(d,which)->
                        openMaterialPicker(owners.get(which),rooms.get(which)))
                .show();
    }

    private void openMaterialPicker(
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room){
        ArrayList<String> labels=new ArrayList<>();
        ArrayList<String> keys=new ArrayList<>();
        for(NativeProjectStore.MaterialData preset:store.loadDefaultCustom(true)){
            labels.add(preset.name+" · "+preset.unit+" · "+money(preset.price)+" ₽");
            keys.add("preset:"+preset.id);
        }
        for(NativeEstimateEngine.Meta meta:NativeEstimateEngine.CLIENT_META){
            if("canvas".equals(meta.key))continue;
            labels.add(meta.name+" · "+meta.unit);
            keys.add(meta.key);
        }
        labels.add("Другое · своё название и единица");
        keys.add("other");

        new NativeStyledAlertDialog.Builder(getContext())
                .setTitle("Дополнительный материал")
                .setItems(labels.toArray(new String[0]),(d,which)->{
                    String key=keys.get(which);
                    if("other".equals(key)){
                        openAdhocMaterial(owner,room,true);
                        return;
                    }
                    if(key.startsWith("preset:")){
                        String presetId=key.substring("preset:".length());
                        NativeProjectStore.MaterialData preset=null;
                        for(NativeProjectStore.MaterialData p:store.loadDefaultCustom(true)){
                            if(p.id.equals(presetId)){preset=p;break;}
                        }
                        if(preset==null)return;
                        NativeProjectStore.MaterialData item=null;
                        for(NativeProjectStore.MaterialData old:room.clientCustomItems){
                            if(presetId.equals(old.sourceId)){item=old;break;}
                        }
                        if(item==null){
                            item=preset.copy();
                            item.id=NativeProjectStore.newId("mat");
                            item.sourceId=preset.id;
                            item.qty=1f;
                            room.clientCustomItems.add(item);
                        }else if(item.qty<=0f)item.qty=1f;
                        changed(owner);
                        editCustomQty(owner,room,item,true);
                        return;
                    }

                    double amount="insert".equals(key)
                            ?NativeEstimateEngine.metrics(room).perimeterM
                            :NativeEstimateEngine.clientDefaultQty(room,key);
                    room.clientEstimateQty.put(key,(float)Math.max(0d,amount));
                    room.clientEstimateOverride.add(key);
                    changed(owner);
                    if(!"insert".equals(key)){
                        NativeEstimateEngine.Meta meta=NativeEstimateEngine.clientMeta(key);
                        new NativeNumberPadDialog(
                                getContext(),
                                meta.name+" · количество",
                                (float)amount,
                                qty->{
                                    room.clientEstimateQty.put(key,Math.max(0f,qty));
                                    room.clientEstimateOverride.add(key);
                                    changed(owner);
                                }).show();
                    }
                }).show();
    }

    private void openAdhocMaterial(
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room,
            boolean clientMaterial){
        NativeProjectStore.MaterialData item=new NativeProjectStore.MaterialData();
        item.id=NativeProjectStore.newId("mat");
        item.name="Материал";
        item.unit="шт";
        item.qty=1f;
        item.price=0f;
        new NativeMaterialEditorDialog(
                getContext(),
                clientMaterial?"Другой материал":"Другой закупочный материал",
                item,
                true,
                material->{
                    material.sourceId=null;
                    (clientMaterial?room.clientCustomItems:room.customItems).add(material);
                    changed(owner);
                }).show();
    }

    private void editCustomQty(
            NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room,
            NativeProjectStore.MaterialData item,
            boolean clientMaterial){
        new NativeNumberPadDialog(
                getContext(),
                item.name+" · количество",
                item.qty,
                value->{
                    item.qty=Math.max(0f,value);
                    changedValue(owner,room);
                }).show();
    }

    private NativeProjectStore.MaterialData findCustom(
            NativeProjectStore.RoomData room,
            String id,
            boolean clientMaterial){
        if(id==null)return null;
        for(NativeProjectStore.MaterialData item:
                (clientMaterial?room.clientCustomItems:room.customItems)){
            if(id.equals(item.id))return item;
        }
        return null;
    }

    private void openPricePreset(
            boolean client,
            List<NativeProjectStore.ProjectData> projects){
        NativePricePresetDialog prices=new NativePricePresetDialog(
                getContext(),
                store,
                project,
                projects,
                client,
                ()->{
                    listener.onChanged();
                    render();
                });
        prices.setOnHomeRequested(()->{
            dismiss();
            listener.onHomeRequested();
        });
        prices.show();
    }

    private void openMergeOrders(){
        List<NativeProjectStore.ProjectData> saved=store.loadSaved();
        ArrayList<NativeProjectStore.ProjectData> choices=new ArrayList<>();
        for(NativeProjectStore.ProjectData p:saved){
            if(!p.id.equals(project.id))choices.add(p);
        }
        if(choices.isEmpty()){
            new NativeStyledAlertDialog.Builder(getContext())
                    .setTitle("Объединить заказы")
                    .setMessage("Других сохранённых заказов пока нет.")
                    .setPositiveButton("Готово",null)
                    .show();
            return;
        }

        String[] labels=new String[choices.size()];
        boolean[] checked=new boolean[choices.size()];
        HashSet<String> selected=new HashSet<>(project.combinedEstimateOrderIds);
        for(int i=0;i<choices.size();i++){
            NativeProjectStore.ProjectData p=choices.get(i);
            labels[i]=projectLabel(p)+" · "+p.rooms.size()+" помещ.";
            checked[i]=selected.contains(p.id);
        }

        new NativeStyledAlertDialog.Builder(getContext())
                .setTitle("Объединить заказы")
                .setMultiChoiceItems(labels,checked,(d,which,isChecked)->{
                    String id=choices.get(which).id;
                    if(isChecked)selected.add(id);else selected.remove(id);
                })
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Объединить",(d,w)->{
                    project.combinedEstimateOrderIds.clear();
                    project.combinedEstimateOrderIds.addAll(selected);
                    store.saveCurrent(project);
                    listener.onChanged();
                    render();
                }).show();
    }

    private List<NativeProjectStore.ProjectData> estimateProjects(){
        ArrayList<NativeProjectStore.ProjectData> out=new ArrayList<>();
        out.add(project);
        if(mode!=Mode.CLIENT)return out;
        if(project.combinedEstimateOrderIds.isEmpty())return out;
        List<NativeProjectStore.ProjectData> saved=store.loadSaved();
        for(String id:project.combinedEstimateOrderIds){
            if(id.equals(project.id))continue;
            for(NativeProjectStore.ProjectData candidate:saved){
                if(id.equals(candidate.id)){
                    out.add(candidate);
                    break;
                }
            }
        }
        return out;
    }

    private NativeEstimateEngine.ProjectCost combinedCost(
            List<NativeProjectStore.ProjectData> projects){
        double area=0d,perimeter=0d,total=0d;
        boolean complete=true;
        for(NativeProjectStore.ProjectData p:projects){
            NativeEstimateEngine.ProjectCost pc=
                    NativeEstimateEngine.projectCost(p,mode==Mode.CLIENT);
            area+=pc.areaM2;
            perimeter+=pc.perimeterM;
            total+=pc.total;
            if(!pc.complete)complete=false;
        }
        return new NativeEstimateEngine.ProjectCost(area,perimeter,total,complete);
    }

    private void changed(NativeProjectStore.ProjectData owner){
        // Row insertion/removal or price-list changes alter table structure.
        if(!owner.id.equals(project.id))store.updateSavedProject(owner);
        listener.onChanged();
        render();
    }

    private void changedValue(NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room){
        // A price/quantity edit normally only changes numeric cells.
        // Keep the existing shell, navigation, scroll and Material buttons.
        if(!owner.id.equals(project.id))store.updateSavedProject(owner);
        listener.onChanged();
        if(!refreshVisibleValues(owner,room))render();
    }

    /**
     * Updates the affected room and the grand total in place. A line may
     * disappear when its quantity reaches zero: in that case the caller
     * rebuilds the table once to keep the line ordering and actions correct.
     */
    boolean refreshVisibleValues(NativeProjectStore.ProjectData owner,
            NativeProjectStore.RoomData room){
        if(content==null)return false;
        LinearLayout card=content.findViewWithTag(
                (mode==Mode.CLIENT?"native_client_room_":"native_internal_room_")
                        +room.id);
        TextView roomTotal=content.findViewWithTag(
                "native_estimate_room_total_"+room.id);
        TextView total=content.findViewWithTag("native_estimate_grand_total");
        if(card==null||roomTotal==null||total==null)return false;

        NativeEstimateEngine.RoomCost roomCost=mode==Mode.CLIENT
                ?NativeEstimateEngine.clientCost(room)
                :NativeEstimateEngine.internalCost(room);

        int existingRows=0;
        for(int i=0;i<card.getChildCount();i++){
            Object tag=card.getChildAt(i).getTag();
            if(tag instanceof String&&((String)tag).startsWith("native_estimate_line_"))
                existingRows++;
        }
        if(existingRows!=roomCost.lines.size())return false;

        // First ensure all view cells still exist; never partially update
        // before deciding that a structural rebuild is required.
        for(NativeEstimateEngine.Line line:roomCost.lines){
            String key=line.key+"_"+room.id;
            if(content.findViewWithTag("native_estimate_qty_"+key)==null
                    ||content.findViewWithTag("native_estimate_price_"+key)==null
                    ||content.findViewWithTag("native_estimate_total_"+key)==null)
                return false;
        }

        for(NativeEstimateEngine.Line line:roomCost.lines){
            String key=line.key+"_"+room.id;
            TextView qtyValue=content.findViewWithTag("native_estimate_qty_"+key);
            TextView priceValue=content.findViewWithTag("native_estimate_price_"+key);
            TextView sumValue=content.findViewWithTag("native_estimate_total_"+key);
            qtyValue.setText(qty(line.qty));
            priceValue.setText(NativeMoney.format(line.price)+" ₽");
            sumValue.setText(NativeMoney.format(line.total)+" ₽");
        }
        roomTotal.setText(NativeMoney.format(roomCost.total)+" ₽");
        total.setText(NativeMoney.format(combinedCost(estimateProjects()).total)+" ₽");
        return true;
    }

    private String projectLabel(NativeProjectStore.ProjectData p){
        String name=p.name==null?"":p.name.trim();
        if(!name.isEmpty())return name;
        String address=p.address==null?"":p.address.trim();
        return address.isEmpty()?"Без имени":address;
    }

    private boolean isInternalExtra(String key){
        return "light".equals(key)
                ||"corner".equals(key)
                ||"cornice".equals(key)
                ||"lightline".equals(key)
                ||"pipe".equals(key);
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(getContext());
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(7),dp(3),dp(7),dp(3));
        c.setBackground(NativeVisualTokens.outlined(
                NativeVisualTokens.SURFACE,NativeVisualTokens.LINE,
                dp(NativeVisualTokens.CARD_RADIUS_DP),dp(1)));
        return c;
    }

    private Button button(String value,boolean primary){
        Button b=new Button(getContext());
        b.setAllCaps(false);
        b.setText(value);
        NativeTypography.register(b,11.5f,true,uiPrefs);
        b.setTextColor(primary?Color.WHITE:TEXT);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(4),0,dp(4),0);
        b.setBackground(NativeVisualTokens.controlButton(primary,dp(10)));
        // Remove framework Button elevation/shadow: flat small controls.
        b.setStateListAnimator(null);
        b.setElevation(0f);
        return b;
    }

    private TextView text(String value,float sp,int color,boolean bold){
        TextView t=new TextView(getContext());
        t.setText(value);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setLineSpacing(0f,1.04f);
        return NativeTypography.register(t,sp,bold,uiPrefs);
    }

    private LinearLayout.LayoutParams top(int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(margin);
        return p;
    }

    private int adaptiveActionHeight(int baseDp,boolean mayWrap){
        float systemScale=Math.max(
                1f,
                getContext().getResources().getConfiguration().fontScale);
        float appScale=Math.max(
                1f,
                uiPrefs.scale/NativeUiPrefs.defaults().scale);
        float effective=Math.max(systemScale,appScale);
        float multiplier=mayWrap?1.75f:1.15f;
        float growth=1f+(effective-1f)*multiplier;
        growth=Math.min(mayWrap?1.50f:1.28f,Math.max(1f,growth));
        return dp(Math.round(baseDp*growth));
    }

    private String qty(double value){
        return String.format(Locale.getDefault(),"%.2f",value)
                .replaceAll("0+$","")
                .replaceAll("[,.]$","");
    }

    private String money(double value){
        return NativeMoney.format(value);
    }

    private GradientDrawable rounded(int fill,int stroke,int radius,int width){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if(width>0)d.setStroke(dp(width),stroke);
        return d;
    }

    private int dp(int v){
        return Math.round(v*getContext().getResources().getDisplayMetrics().density);
    }
}
