package ru.nasvyazi.widget.passcard.tools.helper.providers;


import static ru.nasvyazi.widget.passcard.tools.helper.Helper.LOCATION_PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.PermissionAwareActivity;

import java.util.Arrays;

import ru.nasvyazi.widget.passcard.interfaces.IAskActionCallback;
import ru.nasvyazi.widget.passcard.interfaces.IAskingUserActionProvider;
import ru.nasvyazi.widget.passcard.tools.helper.Helper;

public class AskingUserActionFineLocationProvider implements IAskingUserActionProvider {

    private ReactApplicationContext mContext;
    private Context mBaseContext;
    private IAskActionCallback subscriber;
    private Helper mListener;

    public AskingUserActionFineLocationProvider(ReactApplicationContext context, Helper listener, Context baseContext){
        mContext = context;
        mListener = listener;
        mBaseContext = baseContext;
    };

    @Override
    public boolean check() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||  (mContext == null ? mBaseContext : mContext).checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onActivityResult(int requestCode, boolean isSuccess) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE){
            if (subscriber != null){
                (new Handler()).postDelayed((Runnable) () -> {
                    if (subscriber != null){
                        subscriber.callback(isSuccess);

                    }
                }, 100);
                return true;
            }
        }
        return false;
    }

    @Override
    public void ask(IAskActionCallback completion) {
        subscriber = completion;
        PermissionAwareActivity activity = (PermissionAwareActivity) mContext.getCurrentActivity();
        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE, mListener);
    }

    @Override
    public String getErrorMessage() {
        return "Location permission denied";
    }
}
