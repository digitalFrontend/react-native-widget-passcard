package ru.nasvyazi.widget.passcard.tools.helper.providers;

import static ru.nasvyazi.widget.passcard.tools.helper.Helper.BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.PermissionAwareActivity;

import ru.nasvyazi.widget.passcard.interfaces.IAskActionCallback;
import ru.nasvyazi.widget.passcard.interfaces.IAskingUserActionProvider;
import ru.nasvyazi.widget.passcard.tools.helper.Helper;

public class AskingUserActionBluetoothAdvertiseProvider implements IAskingUserActionProvider {

    private ReactApplicationContext mContext;
    private Context mBaseContext;
    private IAskActionCallback subscriber;
    private Helper mListener;

    public AskingUserActionBluetoothAdvertiseProvider(ReactApplicationContext context, Helper listener, Context baseContext){
        mContext = context;
        mListener = listener;
        mBaseContext = baseContext;
    };

    @Override
    public boolean check() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (mContext == null ? mBaseContext : mContext).checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onActivityResult(int requestCode, boolean isSuccess) {
        if (requestCode == BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE){
            (new Handler()).postDelayed((Runnable) () -> {
                if (subscriber != null){
                    subscriber.callback(isSuccess);

                }
            }, 100);
            return true;
        }
        return false;
    }

    @Override
    public void ask(IAskActionCallback completion) {
        subscriber = completion;
        PermissionAwareActivity activity = (PermissionAwareActivity) mContext.getCurrentActivity();
        activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE, mListener);
    }

    @Override
    public String getErrorMessage() {
        return "Bluetooth advertise permission denied";
    }
}
