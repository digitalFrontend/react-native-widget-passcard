package ru.nasvyazi.widget.passcard.interfaces;

public interface IAskingUserActionProvider {
    public boolean check();
    public boolean onActivityResult(int requestCode, boolean isSuccess);
    public void ask(IAskActionCallback completion);
    public String getErrorMessage();
}
