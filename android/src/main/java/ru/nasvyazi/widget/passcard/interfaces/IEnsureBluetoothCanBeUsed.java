package ru.nasvyazi.widget.passcard.interfaces;

import ru.nasvyazi.widget.passcard.constants.AskingUserActions;

public interface IEnsureBluetoothCanBeUsed {
    public void callback(boolean isSuccess, String text, AskingUserActions providerType);
}
