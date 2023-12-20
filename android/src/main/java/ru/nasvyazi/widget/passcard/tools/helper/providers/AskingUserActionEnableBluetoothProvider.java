package ru.nasvyazi.widget.passcard.tools.helper.providers;

import static ru.nasvyazi.widget.passcard.tools.helper.Helper.ENABLE_BLUETOOTH_REQUEST_CODE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.facebook.react.bridge.ReactApplicationContext;

import ru.nasvyazi.widget.passcard.interfaces.IAskActionCallback;
import ru.nasvyazi.widget.passcard.interfaces.IAskingUserActionProvider;
import ru.nasvyazi.widget.passcard.tools.helper.Helper;

public class AskingUserActionEnableBluetoothProvider implements IAskingUserActionProvider {

    private ReactApplicationContext mContext;
    private Context mBaseContext;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager = null;
    private IAskActionCallback subscriber;
    private Helper mListener;

    public AskingUserActionEnableBluetoothProvider(ReactApplicationContext context, Helper listener, Context baseContext){
        mContext = context;
        mListener = listener;
        mBaseContext = baseContext;
    };

    private BluetoothManager getBluetoothManager() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) ((mContext == null ? mBaseContext : mContext).getSystemService(Context.BLUETOOTH_SERVICE));
        }
        return bluetoothManager;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = getBluetoothManager().getAdapter();
        }

        return bluetoothAdapter;
    }

    @Override
    public boolean check() {
        return getBluetoothAdapter().isEnabled();
    }

    @Override
    public boolean onActivityResult(int requestCode, boolean isSuccess) {
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE){
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

    @SuppressLint("MissingPermission")
    @Override
    public void ask(IAskActionCallback completion) {
        subscriber = completion;
        String intentString = BluetoothAdapter.ACTION_REQUEST_ENABLE;
        int requestCode = ENABLE_BLUETOOTH_REQUEST_CODE;

        mContext.startActivityForResult(new Intent(intentString), requestCode, null);
    }

    @Override
    public String getErrorMessage() {
        return "Bluetooth disabled";
    }
}
