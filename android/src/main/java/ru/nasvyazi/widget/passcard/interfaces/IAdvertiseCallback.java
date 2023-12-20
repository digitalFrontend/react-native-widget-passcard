package ru.nasvyazi.widget.passcard.interfaces;

import android.bluetooth.le.AdvertiseSettings;

public interface IAdvertiseCallback {
    public void onStartSuccess(AdvertiseSettings settingsInEffect);
    public void onStartFailure(Integer errorCode);
}
