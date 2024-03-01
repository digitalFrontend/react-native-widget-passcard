import { NativeModules, Platform, NativeEventEmitter } from "react-native";

const { WidgetSharePasscardData: Widget } = NativeModules;



let PasscardWidget = {
    init: async () => {},
    setParams: async (params) => {},
    getParams: async () => ({}),
    getWidgetState: async () => ({}),
    start: async () => {},
    stop: async () => {},
    addListener: (callback) => {},
    sendCustomEvent: async () => {}
};

PasscardWidget.setParams = async (params) => {
    if (Platform.OS == "ios") {
        return await Widget.setParams(params);
    } else {
        return await Widget.setParams(params);
    }
};

PasscardWidget.init = async () => {
    if (Platform.OS == "ios") {
        return await Widget.initData();
    } else {
        return await Widget.init();
    }
};

PasscardWidget.getParams = async () => {
    if (Platform.OS == "ios") {
        return await Widget.getParams();
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
        return await Widget.start()
    } else {
        return await Widget.start()
    }
};

PasscardWidget.stop = async () => {
    if (Platform.OS == "ios") {
        return await Widget.stop()
    } else {
        return await Widget.stop()
    }
};

PasscardWidget.addListener = callback => {

    let eventEmitter 
    if (Platform.OS == 'ios') {
        eventEmitter = new NativeEventEmitter(NativeModules.WidgetPasscardEventEmitter);
    } else {
        eventEmitter = new NativeEventEmitter(NativeModules.WidgetSharePasscardData);
    }
    let eventListener = eventEmitter.addListener('LOGS_RECEIVER', event => {
        console.log('LOGS_RECEIVER - ', event);
        callback(event)
    });
    return eventListener; //eventEmitter
}


PasscardWidget.sendCustomEvent = () => {
    if(Platform.OS == 'ios'){
        console.log('send EV');
        return Widget.sendCustomEvent()
    }
}
export default PasscardWidget;
