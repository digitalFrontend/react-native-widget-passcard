package ru.nasvyazi.widget.passcard;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Base64;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;


import static android.content.Context.MODE_PRIVATE;
import static android.os.Looper.getMainLooper;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import ru.nasvyazi.widget.passcard.constants.AskingUserActions;
import ru.nasvyazi.widget.passcard.constants.WIDGET_HIGHLIGHTS;
import ru.nasvyazi.widget.passcard.constants.WIDGET_STATES;
import ru.nasvyazi.widget.passcard.interfaces.IAskActionCallback;
import ru.nasvyazi.widget.passcard.interfaces.ILogReceive;
import ru.nasvyazi.widget.passcard.logger.LogsBroadcastReceiver;
import ru.nasvyazi.widget.passcard.service.BackgroundServiceRunner;
import ru.nasvyazi.widget.passcard.tools.helper.Helper;
import ru.nasvyazi.widget.passcard.tools.helper.entity.CheckAllProvidersResult;
import ru.nasvyazi.widget.passcard.widget.PasscardWidget;

public class WidgetSharePasscardDataModule extends ReactContextBaseJavaModule  {

  private final ReactApplicationContext reactContext;
  private final String sharedPreferencesName = "PASSCARD_storage";

  private Helper mHelper;
  private LogsBroadcastReceiver logsReceiver;


  @SuppressLint("RestrictedApi")
  public WidgetSharePasscardDataModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.mHelper = new Helper(reactContext, null);
    logsReceiver = new LogsBroadcastReceiver(reactContext, handleNewLog);
    IntentFilter filter = new IntentFilter();
    filter.addAction(LogsBroadcastReceiver.ON_LOGS_INTENT);
    reactContext.registerReceiver(logsReceiver, filter);
  }

  private ILogReceive handleNewLog = new ILogReceive() {
    @Override
    public void callback(String log) {
      if (reactContext != null){
        WritableMap params = Arguments.createMap();
        params.putString("log", log);
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("LOGS_RECEIVER", params);
      }
    }
  };

  @Override
  public String getName() {
    return "WidgetSharePasscardData";
  }

  @ReactMethod
  public void setParams(final ReadableMap params, final Promise promise) {
    SharedPreferences sharedPreferences =  getReactApplicationContext().getSharedPreferences(sharedPreferencesName,MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString("USER_UUID", params.getString("USER_UUID"));
    editor.putString("SERVICE_UUID", params.getString("SERVICE_UUID"));
    editor.putString("CHAR_FOR_READ_UUID", params.getString("CHAR_FOR_READ_UUID"));
    editor.putString("CHAR_FOR_WRITE_UUID", params.getString("CHAR_FOR_WRITE_UUID"));
    editor.putString("CHAR_FOR_INDICATE_UUID", params.getString("CHAR_FOR_INDICATE_UUID"));
    editor.putString("CCC_DESCRIPTOR_UUID", params.getString("CCC_DESCRIPTOR_UUID"));

    if (!sharedPreferences.getBoolean("isInited", false)){
      editor.putBoolean("isInited", true);
      editor.putInt("widgetState", WIDGET_STATES.WAITING_START);
    }
    editor.commit();
    promise.resolve(null);
  }

  @ReactMethod
  public void getParams(final Promise promise) {

    SharedPreferences sharedPreferences =  getReactApplicationContext().getSharedPreferences(sharedPreferencesName,MODE_PRIVATE);
    WritableMap params = new WritableNativeMap();
    params.putString("USER_UUID", sharedPreferences.getString("USER_UUID", null));
    params.putString("SERVICE_UUID", sharedPreferences.getString("SERVICE_UUID", null));
    params.putString("CHAR_FOR_READ_UUID", sharedPreferences.getString("CHAR_FOR_READ_UUID", null));
    params.putString("CHAR_FOR_WRITE_UUID", sharedPreferences.getString("CHAR_FOR_WRITE_UUID", null));
    params.putString("CHAR_FOR_INDICATE_UUID", sharedPreferences.getString("CHAR_FOR_INDICATE_UUID", null));
    params.putString("CCC_DESCRIPTOR_UUID", sharedPreferences.getString("CCC_DESCRIPTOR_UUID", null));

    promise.resolve(params);
  }

  @ReactMethod
  public void getWidgetState(final Promise promise) {

    SharedPreferences sharedPreferences =  getReactApplicationContext().getSharedPreferences(sharedPreferencesName,MODE_PRIVATE);
    WritableMap params = new WritableNativeMap();
    params.putInt("widgetState", sharedPreferences.getInt("widgetState", WIDGET_STATES.NOT_INITIALIZED));
    params.putInt("widgetHighlight", sharedPreferences.getInt("widgetHighlight", WIDGET_HIGHLIGHTS.NOTHING));

    promise.resolve(params);
  }

  @ReactMethod
  public void start(final Promise promise) {
    try {
      BackgroundServiceRunner.StartService(getReactApplicationContext());
      promise.resolve(null);
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void init(final Promise promise) {
    try {
      initData();
    } catch (Exception e) {
      promise.reject(e);
    }


    initPermissions(new IAskActionCallback() {
      @Override
      public void callback(boolean isSuccess) {
        promise.resolve(isSuccess);
      }
    });
  }

  @ReactMethod
  public void checkPermission(final Promise promise) {
    try {
      CheckAllProvidersResult result = mHelper.checkAllProviders();
      WritableMap params = new WritableNativeMap();
      params.putBoolean("result", result.result);
      params.putString("errorMessage", result.errorMessage);

      promise.resolve(params);
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void stop(final Promise promise) {
    BackgroundServiceRunner.StopService(getReactApplicationContext());
    promise.resolve(null);
  }

  private void initData() {
    SharedPreferences sharedPreferences = getReactApplicationContext().getSharedPreferences(sharedPreferencesName,MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    if (!sharedPreferences.getBoolean("isInited", false)){
      editor.putBoolean("isInited", true);
      editor.putInt("widgetState", WIDGET_STATES.WAITING_START);
      editor.putInt("widget_highlight", WIDGET_HIGHLIGHTS.NOTHING);
      editor.putString("USER_UUID", "4F0001001310BC01");
      editor.putString("SERVICE_UUID", "25AE1441-05D3-4C5B-8281-93D4E07420CF");
      editor.putString("CHAR_FOR_READ_UUID", "25AE1442-05D3-4C5B-8281-93D4E07420CF");
      editor.putString("CHAR_FOR_WRITE_UUID", "25AE1443-05D3-4C5B-8281-93D4E07420CF");
      editor.putString("CHAR_FOR_INDICATE_UUID", "25AE1444-05D3-4C5B-8281-93D4E07420CF");
      editor.putString("CCC_DESCRIPTOR_UUID", "00002902-0000-1000-8000-00805f9b34fb");
      editor.commit();
    }

    Intent intent = new Intent(getReactApplicationContext(), PasscardWidget.class);
    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

    int[] ids = AppWidgetManager.getInstance(getReactApplicationContext())
            .getAppWidgetIds(new ComponentName(getReactApplicationContext(), PasscardWidget.class));
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
    getReactApplicationContext().sendBroadcast(intent);
  }

  private void initPermissions(IAskActionCallback completion) {
    mHelper.ensureAction(AskingUserActions.BLUETOOTH_CONNECT, new IAskActionCallback() {
      @Override
      public void callback(boolean isSuccess) {
        if (isSuccess){
          mHelper.ensureAction(AskingUserActions.BLUETOOTH_ADVERTISE, new IAskActionCallback() {
            @Override
            public void callback(boolean isSuccess) {
              if (isSuccess){
                mHelper.ensureAction(AskingUserActions.LOCATION, new IAskActionCallback() {
                  @Override
                  public void callback(boolean isSuccess) {
                    if (isSuccess){
                      mHelper.ensureAction(AskingUserActions.ENABLE_BLUETOOTH, new IAskActionCallback() {
                        @Override
                        public void callback(boolean isSuccess) {
                          if (isSuccess){
                            completion.callback(true);
                          } else {
                            completion.callback(false);
                          }
                        }
                      });
                    } else {
                      completion.callback(false);
                    }
                  }
                });
              } else {
                completion.callback(false);
              }
            }
          });
        } else {
          completion.callback(false);
        }
      }
    });
  }
}

