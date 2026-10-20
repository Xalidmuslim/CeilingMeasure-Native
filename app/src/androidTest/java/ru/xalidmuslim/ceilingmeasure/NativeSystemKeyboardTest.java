package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NativeSystemKeyboardTest {
    @Test public void numericEditorsRequestUsersAndroidKeyboard() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch=new Intent(ctx, NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(launch)) {
            scenario.onActivity(activity->{
                NativeNumberPadDialog price=new NativeNumberPadDialog(
                        activity,"Цена",150f,value->{});
                price.show();
                EditText number=(EditText)price.findViewById(android.R.id.content)
                        .getRootView().findViewWithTag("native_number_pad_display");
                assertNotNull(number);
                assertEquals(InputType.TYPE_CLASS_NUMBER,
                        number.getInputType()&InputType.TYPE_MASK_CLASS);
                price.dismiss();

                NativeTextPadDialog rename=new NativeTextPadDialog(
                        activity,"Название комнаты","Комната 1",value->{});
                rename.show();
                EditText name=(EditText)rename.findViewById(android.R.id.content)
                        .getRootView().findViewWithTag("native_text_pad_display");
                assertNotNull(name);
                assertEquals(InputType.TYPE_CLASS_TEXT,
                        name.getInputType()&InputType.TYPE_MASK_CLASS);
                rename.dismiss();

                NativeWallMeasureDialog wall=new NativeWallMeasureDialog(
                        activity,"Стена",0f,false,new NativeWallMeasureDialog.Listener(){
                            public void onSave(float value){}
                            public void onDelete(){}
                            public void onBack(){}
                            public void onClose(){}
                        });
                wall.show();
                EditText wallInput=(EditText)wall.findViewById(android.R.id.content)
                        .getRootView().findViewWithTag("native_wall_value");
                assertNotNull(wallInput);
                wallInput.setText("0,58");
                assertEquals(58f,wall.currentUnitsOrNull(),.01f);
                wall.dismiss();

                NativeDiagonalMeasureDialog diag=new NativeDiagonalMeasureDialog(
                        activity,"Диагональ",0f,650f,new NativeDiagonalMeasureDialog.Listener(){
                            public void onSave(float value){}
                            public void onUseAuto(float value){}
                            public void onDelete(){}
                            public void onClose(){}
                        });
                diag.show();
                EditText diagInput=(EditText)diag.findViewById(android.R.id.content)
                        .getRootView().findViewWithTag("native_diag_value");
                assertNotNull(diagInput);
                assertEquals(InputType.TYPE_CLASS_NUMBER,
                        diagInput.getInputType()&InputType.TYPE_MASK_CLASS);
                diag.dismiss();
            });
        }
    }

    @Test public void menuIsCompactStyledDialogNotStockAlert() {
        Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch=new Intent(ctx,NativeWorkspaceActivity.class);
        launch.putExtra(NativeWorkspaceActivity.EXTRA_LAUNCH_ACTION,
                NativeWorkspaceActivity.ACTION_NEW_ORDER);
        try(ActivityScenario<NativeWorkspaceActivity> scenario=ActivityScenario.launch(launch)) {
            scenario.onActivity(activity->{
                android.app.Dialog menu=new NativeStyledAlertDialog.Builder(activity)
                        .setTitle("Комната 1")
                        .setItems(new String[]{"Переименовать","Дублировать","Удалить"},
                                (d,w)->{}).show();
                try{
                    assertNotNull(menu.findViewById(android.R.id.content)
                            .getRootView().findViewWithTag("native_styled_alert_shell"));
                }finally { menu.dismiss(); }
            });
        }
    }
}
