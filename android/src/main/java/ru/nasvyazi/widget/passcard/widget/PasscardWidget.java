package ru.nasvyazi.widget.passcard.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ru.nasvyazi.widget.passcard.R;
import ru.nasvyazi.widget.passcard.constants.AskingUserActions;
import ru.nasvyazi.widget.passcard.constants.WIDGET_ACTIONS;
import ru.nasvyazi.widget.passcard.constants.WIDGET_HIGHLIGHTS;
import ru.nasvyazi.widget.passcard.constants.WIDGET_STATES;
import ru.nasvyazi.widget.passcard.logger.LogsSender;
import ru.nasvyazi.widget.passcard.service.BackgroundService;
import ru.nasvyazi.widget.passcard.service.BackgroundServiceRunner;
import ru.nasvyazi.widget.passcard.tools.helper.Helper;
import ru.nasvyazi.widget.passcard.widget.entity.WidgetStateData;


public class PasscardWidget extends AppWidgetProvider {


    private final int ALARM_ID = 12312;
    public static String ACTION_AUTO_UPDATE_WIDGET = "ACTION_AUTO_UPDATE_WIDGET";
    public static String TOGGLE_BACKGROUND_SERVICE = "ToggleBackgroundService";


    private int ACTUAL_INIT_VERSION = 2;

    public PasscardWidget() {
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), getClass().getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        if (appWidgetIds.length == 0) {
            // stop alarm
            AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
            appWidgetAlarm.stopAlarm(ALARM_ID);
        }
    }



    private BluetoothAdapter getBluetoothAdapter(Context context) {
        return ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }

    private void switchToPermissionError(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("widgetState", WIDGET_STATES.REQUIRED_PERMISSION);
        editor.commit();
    }

    private void switchToBluetoothError(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("widgetState", WIDGET_STATES.REQUIRED_ENABLE_BLUETOOTH);
        editor.commit();
    }

    private void switchToBluetoothNoError(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("widgetState", WIDGET_STATES.WAITING_START);
        editor.commit();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            SharedPreferences sharedPref = context.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);
            int currentWidgetState = sharedPref.getInt("widgetState", WIDGET_STATES.NOT_INITIALIZED);
            if (currentWidgetState == WIDGET_STATES.REQUIRED_ENABLE_BLUETOOTH || currentWidgetState == WIDGET_STATES.WAITING_START|| currentWidgetState == WIDGET_STATES.RUNNING){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        switchToBluetoothError(context);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        switchToBluetoothNoError(context);
                        break;
                }
            }

        }

        if(intent.getAction().equals(TOGGLE_BACKGROUND_SERVICE)) {

            if(BackgroundServiceRunner.isMyServiceRunning(context, BackgroundService.class)) {
                BackgroundServiceRunner.StopService(context);
            } else {
                Helper mHelper = new Helper(null, context);

                if (mHelper.getAskingUserActionProvider(AskingUserActions.BLUETOOTH_CONNECT).check()){
                    if (mHelper.getAskingUserActionProvider(AskingUserActions.BLUETOOTH_ADVERTISE).check()){
                        if (mHelper.getAskingUserActionProvider(AskingUserActions.LOCATION).check()){
                            if (mHelper.getAskingUserActionProvider(AskingUserActions.ENABLE_BLUETOOTH).check()){
                                try {
                                    BackgroundServiceRunner.StartService(context);
                                } catch (Exception e) {
                                    Toast.makeText(context, e.getLocalizedMessage(),
                                            Toast.LENGTH_LONG).show();
                                    Log.i("PASSCARD_WIDGET", e.getLocalizedMessage());
                                }
                            } else {
                                Toast.makeText(context, "ENABLE_BLUETOOTH error",
                                        Toast.LENGTH_LONG).show();
                                switchToBluetoothError(context);
                            }
                        } else {
                            Toast.makeText(context, "LOCATION error",
                                    Toast.LENGTH_LONG).show();
                            switchToPermissionError(context);
                        }
                    } else {
                        Toast.makeText(context, "BLUETOOTH_ADVERTISE error",
                                Toast.LENGTH_LONG).show();
                        switchToPermissionError(context);
                    }
                } else {
                    Toast.makeText(context, "BLUETOOTH_CONNECT error",
                            Toast.LENGTH_LONG).show();
                    switchToPermissionError(context);
                }
            }
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), getClass().getName());

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);

        onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);      

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // start alarm
        AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
        appWidgetAlarm.startAlarm(ALARM_ID);

        // update widgets
        for (int i : appWidgetIds) {
            updateWidget(context, appWidgetManager, i);
        }
    }

    WidgetStateData getWidgetStateData(Context context) {
        WidgetStateData result = new WidgetStateData();
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);
            int state = sharedPref.getInt("widgetState", WIDGET_STATES.NOT_INITIALIZED);
            boolean isInited = sharedPref.getInt("initVersion", 0) == ACTUAL_INIT_VERSION;

            if (!isInited) {
                result.drawableId = R.drawable.passcard_widget_not_inited;
                result.action = WIDGET_ACTIONS.NOTHING;
            } else {
                int hightlight = sharedPref.getInt("widgetHighlight", WIDGET_HIGHLIGHTS.NOTHING);
                if (hightlight == WIDGET_HIGHLIGHTS.NOTHING) {
                    switch (state) {
                        case WIDGET_STATES.NOT_INITIALIZED: {
                            result.drawableId = R.drawable.passcard_widget_not_inited;
                            result.action = WIDGET_ACTIONS.NOTHING;
                            break;
                        }
                        case WIDGET_STATES.WAITING_START: {
                            result.drawableId = R.drawable.passcard_widget_inactive;
                            result.action = WIDGET_ACTIONS.TOGGLE_SERVICE;
                            break;
                        }
                        case WIDGET_STATES.RUNNING: {
                            result.drawableId = R.drawable.passcard_widget_active;
                            result.action = WIDGET_ACTIONS.TOGGLE_SERVICE;
                            break;
                        }
                        case WIDGET_STATES.REQUIRED_ENABLE_BLUETOOTH: {

                            result.drawableId = R.drawable.passcard_widget_bluetooth;
                            result.action = WIDGET_ACTIONS.ENABLE_BLUETOOTH;
                            break;
                        }
                        case WIDGET_STATES.REQUIRED_PERMISSION: {
                            result.drawableId = R.drawable.passcard_widget_permission;
                            result.action = WIDGET_ACTIONS.TOGGLE_SERVICE;
                            break;
                        }
                    }
                } else {
                    result.action = WIDGET_ACTIONS.NOTHING;
                    switch (hightlight) {
                        case WIDGET_HIGHLIGHTS.SUCCESS: {
                            result.drawableId = R.drawable.passcard_widget_success;
                            break;
                        }
                        case WIDGET_HIGHLIGHTS.FAIL: {
                            result.drawableId = R.drawable.passcard_widget_unsuccess;
                            break;
                        }
                    }
                }
            }

        } catch (Exception err) {
        } finally {
            return result;
        }

    }

    PendingIntent getEnableBluetoothPendingIntent(Context context){
        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, enableBluetoothIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return pendingIntent;
    }

    PendingIntent getToggleServicePendingIntent(Context context){
        Intent newIntent = new Intent(context, PasscardWidget.class);
        newIntent.setAction(TOGGLE_BACKGROUND_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_IMMUTABLE);
        return pendingIntent;
    }

    void updateWidget(Context context, AppWidgetManager appWidgetManager,
                      int appWidgetId) {

        try {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.passcard_widget);
            WidgetStateData stateData = getWidgetStateData(context);

            rv.setImageViewResource(R.id.toggle_button, stateData.drawableId);

            if (stateData.action == WIDGET_ACTIONS.ENABLE_BLUETOOTH){
                rv.setOnClickPendingIntent(R.id.toggle_button, getEnableBluetoothPendingIntent(context));
            } else if (stateData.action == WIDGET_ACTIONS.TOGGLE_SERVICE) {
                rv.setOnClickPendingIntent(R.id.toggle_button, getToggleServicePendingIntent(context));
            }

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        } catch (ActivityNotFoundException e) {
            Log.e("PASSCARD_WIDGET", e.getLocalizedMessage());
        }
    }
}