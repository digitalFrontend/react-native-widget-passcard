import Foundation
import WidgetKit
import React

@objc(WidgetSharePasscardData)
class WidgetSharePasscardData: NSObject {
    
    var BLE: BLEController? = nil
    
    
    let logsSender: LogsSender = LogsSender()
//    let BGService: BackgroundService = getBGServiceInstance()
    @objc
    func sendCustomEvent(
        _ resolve: @escaping RCTPromiseResolveBlock,
        withRejecter reject:  @escaping RCTPromiseRejectBlock
    ) {
        self.BLE = BLEController()
        self.BLE?.createBLE()
    }
    
    @objc
    func start(
        _ resolve: @escaping RCTPromiseResolveBlock,
        withRejecter reject:  @escaping RCTPromiseRejectBlock
    ) {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        defaults?.set(WidgetActions.LET_START.rawValue, forKey: "widgetActions")
        if #available(iOS 14.0, *) {
            WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
        }
//        BackgroundService.shared.start()
        resolve(nil)
    }
    
    @objc
    func stop(
        _ resolve: @escaping RCTPromiseResolveBlock,
        withRejecter reject:  @escaping RCTPromiseRejectBlock
    ) {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        defaults?.set(WidgetActions.LET_STOP.rawValue, forKey: "widgetActions")
        if #available(iOS 14.0, *) {
            WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
        }
//        BackgroundService.shared.stop()
        resolve(nil)
    }
    
    @objc
    func setParams(
        _ params: NSDictionary,
        withResolver resolve: @escaping RCTPromiseResolveBlock,
        withRejecter reject:  @escaping RCTPromiseRejectBlock
    )-> Void {
        
            let defaults = UserDefaults(suiteName: DATA_GROUP)
            defaults?.set(params["USER_UUID"] as? String, forKey: "USER_UUID")
            defaults?.set(params["SERVICE_UUID"] as? String, forKey: "SERVICE_UUID")
            defaults?.set(params["CHAR_FOR_READ_UUID"] as? String, forKey: "CHAR_FOR_READ_UUID")
            defaults?.set(params["CHAR_FOR_WRITE_UUID"] as? String, forKey: "CHAR_FOR_WRITE_UUID")
            defaults?.set(params["CHAR_FOR_INDICATE_UUID"] as? String, forKey: "CHAR_FOR_INDICATE_UUID")
        
//          CCC_DESCRIPTOR_UUID - в ios примере не использовался по какойто причине
//          Но дверь открылась и без него
            defaults?.set(params["CCC_DESCRIPTOR_UUID"] as? String, forKey: "CCC_DESCRIPTOR_UUID")

        if !(defaults?.bool(forKey: "isInited") ?? false) {
            print("--------- setParams true")
            defaults?.set(true, forKey: "isInited")
            defaults?.set(WidgetStates.WAITING_START.rawValue, forKey: "widgetState")
            defaults?.set(WidgetHighlight.NOTHING.rawValue, forKey: "widgetHighlight")
        }
        
        defaults?.set(WidgetActions.LET_INIT.rawValue, forKey: "widgetActions")
        
        defaults?.synchronize()

        if #available(iOS 14.0, *) {
            WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
        }
        resolve("OK")

    }
    @objc
    func getParams(
            _ resolve: @escaping RCTPromiseResolveBlock,
            withRejecter reject:  @escaping RCTPromiseRejectBlock
    )-> Void {
        let defaults = UserDefaults(suiteName: DATA_GROUP)

        var params = [String: Any]()

        params["USER_UUID"] = defaults?.string(forKey: "USER_UUID") ?? ""
        params["SERVICE_UUID"] = defaults?.string(forKey: "SERVICE_UUID") ?? ""
        params["CHAR_FOR_READ_UUID"] = defaults?.string(forKey: "CHAR_FOR_READ_UUID") ?? ""
        params["CHAR_FOR_WRITE_UUID"] = defaults?.string(forKey: "CHAR_FOR_WRITE_UUID") ?? ""
        params["CHAR_FOR_INDICATE_UUID"] = defaults?.string(forKey: "CHAR_FOR_INDICATE_UUID") ?? ""
        params["CCC_DESCRIPTOR_UUID"] = defaults?.string(forKey: "CCC_DESCRIPTOR_UUID") ?? ""

        resolve(params as NSDictionary)
    }
    @objc
    func initData(
        _ resolve: @escaping RCTPromiseResolveBlock,
        withRejecter reject:  @escaping RCTPromiseRejectBlock
    )-> Void {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        if !(defaults?.bool(forKey: "isInited") ?? false) {
//            defaults?.set(true, forKey: "isInited")
//            defaults?.set(WidgetStates.WAITING_START.rawValue, forKey: "widgetState")
            defaults?.set(WidgetHighlight.NOTHING.rawValue, forKey: "widgetHighlight")
            defaults?.set("", forKey: "USER_UUID") //2D:60029
            defaults?.set("25AE1441-05D3-4C5B-8281-93D4E07420CF", forKey: "SERVICE_UUID")
            defaults?.set("25AE1442-05D3-4C5B-8281-93D4E07420CF", forKey: "CHAR_FOR_READ_UUID")
            defaults?.set("25AE1443-05D3-4C5B-8281-93D4E07420CF", forKey: "CHAR_FOR_WRITE_UUID")
            defaults?.set("25AE1444-05D3-4C5B-8281-93D4E07420CF", forKey: "CHAR_FOR_INDICATE_UUID")
            defaults?.set("00002902-0000-1000-8000-00805f9b34fb", forKey: "CCC_DESCRIPTOR_UUID")
            defaults?.synchronize()
        }
        resolve("OK")
       
    }
    
    @objc
    func getWidgetState(
        _ resolve: @escaping RCTPromiseResolveBlock,
        withRejecter reject:  @escaping RCTPromiseRejectBlock
    )-> Void {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        resolve(["widgetState": defaults?.string(forKey: "widgetState")])
    }
}

