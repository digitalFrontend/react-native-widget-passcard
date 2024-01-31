package ru.nasvyazi.widget.passcard.server;

import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.core.app.ActivityCompat;

import java.util.*;

import kotlin.text.Charsets;
import ru.nasvyazi.widget.passcard.constants.AskingUserActions;
import ru.nasvyazi.widget.passcard.constants.WIDGET_HIGHLIGHTS;
import ru.nasvyazi.widget.passcard.constants.WIDGET_STATES;
import ru.nasvyazi.widget.passcard.interfaces.IAdvertiseCallback;
import ru.nasvyazi.widget.passcard.interfaces.IAdvertiseStateChangeCallback;
import ru.nasvyazi.widget.passcard.interfaces.IEnsureBluetoothCanBeUsed;
import ru.nasvyazi.widget.passcard.interfaces.IHighlightCallback;
import ru.nasvyazi.widget.passcard.logger.LogsSender;
import ru.nasvyazi.widget.passcard.server.entity.GattServerParams;
import ru.nasvyazi.widget.passcard.tools.helper.Helper;
import ru.nasvyazi.widget.passcard.tools.helper.entity.CheckAllProvidersResult;

public class GattServer {

    private Context mContext = null;
    private GattServerParams mParams = null;
    private boolean isAdvertising = false;
    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeAdvertiser bleAdvertiser = null;
    public Set<BluetoothDevice> subscribedDevices = new HashSet<>();
    private AdvertiseSettings advertiseSettings = null;
    private AdvertiseData advertiseData = null;
    private BluetoothGattServer gattServer = null;
    private IAdvertiseStateChangeCallback onStateChange = null;
    private IHighlightCallback onHighlight = null;
    private Helper mHelper = null;
    private LogsSender logsSender = null;


    public GattServer(Context context, IAdvertiseStateChangeCallback callback, IHighlightCallback callback2) {
        mContext = context;
        onStateChange = callback;
        onHighlight = callback2;
        mHelper = new Helper(null, context);
        logsSender = new LogsSender(context);
    }

    public void start(GattServerParams params) {
        mParams = params;
        if (!isAdvertising) {
            prepareAndStartAdvertising();
        }
    }

    public void stop() {
        if (isAdvertising) {
            bleStopAdvertising();
        }
    }

    private BluetoothManager getBluetoothManager() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        return bluetoothManager;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = getBluetoothManager().getAdapter();
        }

        return bluetoothAdapter;
    }

    private BluetoothLeAdvertiser getBleAdvertiser() {
        if (bleAdvertiser == null) {
            bleAdvertiser = getBluetoothAdapter().getBluetoothLeAdvertiser();
        }

        return bleAdvertiser;
    }

    private void ensureBluetoothCanBeUsed(IEnsureBluetoothCanBeUsed completion) {
        CheckAllProvidersResult result = mHelper.checkAllProviders();
        logsSender.appendLog("CheckAllProvidersResult result="+result.result +" message="+ result.errorMessage);
        completion.callback(result.result, (result.errorMessage == null ? "BLE ready for use" : result.errorMessage), result.providerType);
    }

    private void prepareAndStartAdvertising() {
        ensureBluetoothCanBeUsed(new IEnsureBluetoothCanBeUsed() {
            @Override
            public void callback(boolean isSuccess, String text, AskingUserActions providerType) {
                logsSender.appendLog(text);
                if (isSuccess) {
                    bleStartAdvertising();
                } else {
                    isAdvertising = false;
                    stop();
                    if (onStateChange != null){
                        onStateChange.callback(providerType == AskingUserActions.ENABLE_BLUETOOTH ? WIDGET_STATES.REQUIRED_ENABLE_BLUETOOTH : WIDGET_STATES.REQUIRED_PERMISSION);
                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void sendGattServerResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
        gattServer.sendResponse(device, requestId, status, offset, value);
    }

    private BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                logsSender.appendLog("Central did connect"+" USER_UUID = " + mParams.USER_UUID);
            } else {
                subscribedDevices.remove(device);
                logsSender.appendLog("Central did disconnect");
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            logsSender.appendLog("onNotificationSent status="+status);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            String log = "onCharacteristicRead offset="+offset;

            if (characteristic.getUuid().equals(UUID.fromString(mParams.CHAR_FOR_READ_UUID))) {
                String strValue = mParams.USER_UUID;
                sendGattServerResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, strValue.getBytes(Charsets.UTF_8));

                log += "\nresponse=success, value=\""+strValue+"\"";
                logsSender.appendLog(log);
            } else {

                sendGattServerResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                log += "\nresponse=failure, unknown UUID\n"+characteristic.getUuid();
                logsSender.appendLog(log);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            String log = "onCharacteristicWrite offset="+offset+" responseNeeded="+responseNeeded+" preparedWrite="+preparedWrite;
            if (characteristic.getUuid().equals(UUID.fromString(mParams.CHAR_FOR_WRITE_UUID))) {
                String strValue = value == null || value.length == 0 ? "" : new String(value, Charsets.UTF_8);
                if (responseNeeded) {
                    sendGattServerResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, strValue.getBytes(Charsets.UTF_8));
                    log += "\nresponse=success, value=\""+strValue+"\"";
                    if (onHighlight != null){
                        logsSender.appendLog("WIDGET_HIGHLIGHTS.SUCCESS - " + mParams.USER_UUID);
                        onHighlight.callback(WIDGET_HIGHLIGHTS.SUCCESS);
                    }
                } else {
                    log += "\nresponse=notNeeded, value=\""+strValue+"\"";
                }
            } else {
                if (responseNeeded) {
                    sendGattServerResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                    log += "\nresponse=failure, unknown UUID\n"+characteristic.getUuid();
                    if (onHighlight != null){
                        logsSender.appendLog("WIDGET_HIGHLIGHTS.FAIL - " + mParams.USER_UUID);
                        onHighlight.callback(WIDGET_HIGHLIGHTS.FAIL);
                    }
                } else {
                    log += "\nresponse=notNeeded, unknown UUID\n"+characteristic.getUuid();
                }
            }
            logsSender.appendLog(log);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            String strLog = "onDescriptorWriteRequest";
            if (descriptor.getUuid().equals(UUID.fromString(mParams.CCC_DESCRIPTOR_UUID))) {
                int status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                if (descriptor.getCharacteristic().getUuid().equals(UUID.fromString(mParams.CHAR_FOR_INDICATE_UUID))) {
                    if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                        subscribedDevices.add(device);
                        status = BluetoothGatt.GATT_SUCCESS;
                        strLog += ", subscribed";
                    } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                        subscribedDevices.remove(device);
                        status = BluetoothGatt.GATT_SUCCESS;
                        strLog += ", unsubscribed";
                    }
                }
                if (responseNeeded) {
                    sendGattServerResponse(device, requestId, status, 0, null);
                }
            } else {
                strLog += " unknown uuid="+descriptor.getUuid();
                if (responseNeeded) {
                    sendGattServerResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            }
            logsSender.appendLog(strLog);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            String log = "onDescriptorReadRequest";
            if (descriptor.getUuid().equals(UUID.fromString(mParams.CCC_DESCRIPTOR_UUID))) {
                byte[] returnValue = null;
                if (subscribedDevices.contains(device)) {
                    log += " CCCD response=ENABLE_NOTIFICATION";
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    log += " CCCD response=DISABLE_NOTIFICATION";
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                sendGattServerResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, returnValue);
            } else {
                log += " unknown uuid="+descriptor.getUuid();
                sendGattServerResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
            logsSender.appendLog(log);
        }
    };

    @SuppressLint("MissingPermission")
    private void bleStartGattServer() {
        BluetoothGattServer _gattServer = getBluetoothManager().openGattServer(mContext, serverCallback);
        BluetoothGattService service = new BluetoothGattService(UUID.fromString(mParams.SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic charForRead = new BluetoothGattCharacteristic(UUID.fromString(mParams.CHAR_FOR_READ_UUID),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattCharacteristic charForWrite = new BluetoothGattCharacteristic(UUID.fromString(mParams.CHAR_FOR_WRITE_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic charForIndicate = new BluetoothGattCharacteristic(UUID.fromString(mParams.CHAR_FOR_INDICATE_UUID),
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattDescriptor charConfigDescriptor = new BluetoothGattDescriptor(UUID.fromString(mParams.CCC_DESCRIPTOR_UUID),
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        charForIndicate.addDescriptor(charConfigDescriptor);

        service.addCharacteristic(charForRead);
        service.addCharacteristic(charForWrite);
        service.addCharacteristic(charForIndicate);

        boolean result = _gattServer.addService(service);

        gattServer = _gattServer;
        logsSender.appendLog("addService "+(result ? "OK" : "FAIL"));
    }

    private AdvertiseSettings getAdvertiseSettings(){
        if (advertiseSettings == null){
            advertiseSettings = new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED).setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM).setConnectable(true).build();
        }

        return advertiseSettings;
    }

    private AdvertiseData getAdvertiseData(){
        if (advertiseData == null){
            advertiseData = new AdvertiseData.Builder()
                    .setIncludeDeviceName(false) // don't include name, because if name size > 8 bytes, ADVERTISE_FAILED_DATA_TOO_LARGE
                    .addServiceUuid(new ParcelUuid(UUID.fromString(mParams.SERVICE_UUID)))
                    .build();
        }

        return advertiseData;
    }

    private IAdvertiseCallback advertiseCallback = new IAdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            logsSender.appendLog("Advertise start success\n"+mParams.SERVICE_UUID);
            logsSender.appendLog("For USER - " + mParams.USER_UUID);
        }

        @Override
        public void onStartFailure(Integer errorCode) {
            String desc = "";
            switch (errorCode) {
                case  (ADVERTISE_FAILED_DATA_TOO_LARGE):
                    desc = "\nADVERTISE_FAILED_DATA_TOO_LARGE";
                    break;
                case (ADVERTISE_FAILED_TOO_MANY_ADVERTISERS):
                    desc = "\nADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                    break;
                case (ADVERTISE_FAILED_ALREADY_STARTED):
                    desc = "\nADVERTISE_FAILED_ALREADY_STARTED";
                    break;
                case (ADVERTISE_FAILED_INTERNAL_ERROR):
                    desc = "\nADVERTISE_FAILED_INTERNAL_ERROR";
                    break;
                case (ADVERTISE_FAILED_FEATURE_UNSUPPORTED):
                    desc = "\nADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                    break;
                default:
                    desc = "";
                    break;
            }

//            isAdvertising = false;
            stop();
            logsSender.appendLog("Advertise start failed: errorCode="+errorCode+" "+desc);
        }
    };

    @SuppressLint("MissingPermission")
    private void bleStartAdvertising() {
        isAdvertising= true;
        if (onStateChange != null){
            onStateChange.callback(WIDGET_STATES.RUNNING);
        }
        bleStartGattServer();
        getBleAdvertiser().startAdvertising(getAdvertiseSettings(), getAdvertiseData(), new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                advertiseCallback.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                advertiseCallback.onStartFailure(errorCode);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void bleStopGattServer() {
        gattServer.close();
        gattServer = null;
        logsSender.appendLog("gattServer closed");
    }

    @SuppressLint("MissingPermission")
    private void bleStopAdvertising() {
        isAdvertising = false;
        if (onStateChange != null){
            onStateChange.callback(WIDGET_STATES.WAITING_START);
        }
        bleStopGattServer();
        getBleAdvertiser().stopAdvertising(new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                advertiseCallback.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                advertiseCallback.onStartFailure(errorCode);
            }
        });
    }
}
