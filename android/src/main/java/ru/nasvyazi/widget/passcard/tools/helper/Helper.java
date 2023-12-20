package ru.nasvyazi.widget.passcard.tools.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.PermissionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.nasvyazi.widget.passcard.constants.AskingUserActions;
import ru.nasvyazi.widget.passcard.interfaces.IAskActionCallback;
import ru.nasvyazi.widget.passcard.interfaces.IAskingUserActionProvider;
import ru.nasvyazi.widget.passcard.tools.helper.entity.CheckAllProvidersResult;
import ru.nasvyazi.widget.passcard.tools.helper.providers.AskingUserActionBluetoothAdvertiseProvider;
import ru.nasvyazi.widget.passcard.tools.helper.providers.AskingUserActionBluetoothConnectProvider;
import ru.nasvyazi.widget.passcard.tools.helper.providers.AskingUserActionEnableBluetoothProvider;
import ru.nasvyazi.widget.passcard.tools.helper.providers.AskingUserActionFineLocationProvider;

public class Helper implements ActivityEventListener, PermissionListener {
    public static Integer ENABLE_BLUETOOTH_REQUEST_CODE = 1;
    public static Integer LOCATION_PERMISSION_REQUEST_CODE = 2;
    public static Integer BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE = 3;
    public static Integer BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE = 4;


    private ReactApplicationContext mContext = null;
    private Context mBaseContext = null;
    private Map<AskingUserActions, IAskingUserActionProvider> providers = new HashMap<>();
    public Helper(ReactApplicationContext context, Context baseContext){
        mContext = context;
        mBaseContext = baseContext;
        providers.put(AskingUserActions.ENABLE_BLUETOOTH, new AskingUserActionEnableBluetoothProvider(context, this, baseContext));
        providers.put(AskingUserActions.BLUETOOTH_CONNECT, new AskingUserActionBluetoothConnectProvider(context, this, baseContext));
        providers.put(AskingUserActions.BLUETOOTH_ADVERTISE, new AskingUserActionBluetoothAdvertiseProvider(context, this, baseContext));
        providers.put(AskingUserActions.LOCATION, new AskingUserActionFineLocationProvider(context, this, baseContext));
        if (mContext != null){
            mContext.addActivityEventListener(this);
        }
    }

    public IAskingUserActionProvider getAskingUserActionProvider(AskingUserActions action){
        return providers.get(action);
    }

    public CheckAllProvidersResult checkAllProviders(){
        boolean result = true;
        String message = null;
        AskingUserActions providerType = null;
        for (Map.Entry<AskingUserActions, IAskingUserActionProvider> entry : providers.entrySet()) {
            boolean itemResult = entry.getValue().check();
            if (!itemResult){
                result = false;
                message = entry.getValue().getErrorMessage();
                providerType = entry.getKey();
                break;
            }
        }

        CheckAllProvidersResult returnValue = new CheckAllProvidersResult();
        returnValue.errorMessage = message;
        returnValue.providerType = providerType;
        returnValue.result = result;

        return returnValue;
    }

    public boolean onAskResult(int requestCode, boolean isSuccess) {
        boolean result = false;
        for (Map.Entry<AskingUserActions, IAskingUserActionProvider> entry : providers.entrySet()) {
            boolean itemResult = entry.getValue().onActivityResult(requestCode, isSuccess);
            if (itemResult){
                result = true;
            }
        }

        return result;
    }

    public void ensureAction(AskingUserActions action, IAskActionCallback completion){
        IAskingUserActionProvider provider = providers.get(action);
        if (!provider.check()){
            provider.ask(completion);
        } else {
            completion.callback(true);
        }
    }


    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent intent) {
        boolean isSuccess = resultCode == Activity.RESULT_OK;
        onAskResult(requestCode, isSuccess);
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0){
            boolean isSuccess = grantResults[0] != PackageManager.PERMISSION_DENIED;

            return onAskResult(requestCode, isSuccess);
        } else {
            return false;
        }
    }
}
