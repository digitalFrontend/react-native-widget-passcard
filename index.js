import { NativeModules, Platform, NativeEventEmitter } from "react-native";

const { WidgetSharePasscardData: Widget } = NativeModules;



let PasscardWidget = {
    init: async () => {},
    setParams: async (params) => {},
    getParams: async () => ({}),
    getWidgetState: async () => ({}),
    start: async () => {},
    stop: async () => {},
    addListener: (callback) => {}
};

PasscardWidget.setParams = async (params) => {
    if (Platform.OS == "ios") {
        return;
    } else {
        return await Widget.setParams(params);
    }
};

PasscardWidget.init = async () => {
    if (Platform.OS == "ios") {
        return;
    } else {
        return await Widget.init();
    }
};

PasscardWidget.getParams = async () => {
    if (Platform.OS == "ios") {
        return;
    } else {
        return await Widget.getParams();
    }
};

PasscardWidget.getWidgetState = async () => {
    if (Platform.OS == "ios") {
        return {};
    } else {
        return await Widget.getWidgetState();
    }
};

PasscardWidget.start = async () => {
    if (Platform.OS == "ios") {
        return null
    } else {
        return await Widget.start()
    }
};

PasscardWidget.stop = async () => {
    if (Platform.OS == "ios") {
        return null
    } else {
        return await Widget.stop()
    }
};

PasscardWidget.addListener = callback => {
    const eventEmitter = new NativeEventEmitter(NativeModules.WidgetSharePasscardData);
    let eventListener = eventEmitter.addListener('LOGS_RECEIVER', event => {
        callback(event)
    });
    return eventEmitter;
}

export default PasscardWidget;
