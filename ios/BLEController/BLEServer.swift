
import CoreBluetooth
import WidgetKit


public class BLEServer: NSObject, CBPeripheralManagerDelegate {

    // BLE related properties
    var isNeedWidgetRefresh: Bool
    var uuidService:CBUUID //= CBUUID(string: "25AE1441-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForRead:CBUUID // = CBUUID(string: "25AE1442-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForWrite:CBUUID // = CBUUID(string: "25AE1443-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForIndicate:CBUUID // = CBUUID(string: "25AE1444-05D3-4C5B-8281-93D4E07420CF")
    var textFieldDataForRead: String!
    
    var charForIndicate: CBMutableCharacteristic?
    
    var subscribedCentrals = [CBCentral]()
    let logsSender = LogsSender()

    var peripheralManager: CBPeripheralManager?
    
    init(isNeedWidgetRefresh: Bool) {
        self.isNeedWidgetRefresh = isNeedWidgetRefresh
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        self.uuidService = CBUUID(string:defaults?.string(forKey: "SERVICE_UUID") ?? "25AE1441-05D3-4C5B-8281-93D4E07420CF")
        self.uuidService = CBUUID(string:defaults?.string(forKey: "SERVICE_UUID") ?? "25AE1441-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForRead = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_READ_UUID") ?? "25AE1442-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForWrite = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_WRITE_UUID") ?? "25AE1443-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForIndicate = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_INDICATE_UUID") ?? "25AE1444-05D3-4C5B-8281-93D4E07420CF")
        self.textFieldDataForRead = defaults?.string(forKey: "USER_UUID") ?? "EMPTY_ID"
        super.init()
        self.peripheralManager = CBPeripheralManager(delegate: self, queue: .main)
    }
    
    func updateManager() {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        let paramsWasUpdate = defaults?.bool(forKey: "paramsWasUpdate") ?? true
        logsSender.appendLog("updateManager ---> paramsWasUpdate = \(paramsWasUpdate)")
        let userId = defaults?.string(forKey: "USER_UUID") ?? "EMPTY_ID"
        logsSender.appendLog("updateManager ---> paramsWasUpdate = \(userId)")
        if(paramsWasUpdate || userId == "EMPTY_ID"){
            self.uuidService = CBUUID(string:defaults?.string(forKey: "SERVICE_UUID") ?? "25AE1441-05D3-4C5B-8281-93D4E07420CF")
            self.uuidService = CBUUID(string:defaults?.string(forKey: "SERVICE_UUID") ?? "25AE1441-05D3-4C5B-8281-93D4E07420CF")
            self.uuidCharForRead = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_READ_UUID") ?? "25AE1442-05D3-4C5B-8281-93D4E07420CF")
            self.uuidCharForWrite = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_WRITE_UUID") ?? "25AE1443-05D3-4C5B-8281-93D4E07420CF")
            self.uuidCharForIndicate = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_INDICATE_UUID") ?? "25AE1444-05D3-4C5B-8281-93D4E07420CF")
            self.textFieldDataForRead = defaults?.string(forKey: "USER_UUID") ?? "EMPTY_ID"
            
            self.peripheralManager = nil
            self.peripheralManager = CBPeripheralManager(delegate: self, queue: .main)
            defaults?.set(false, forKey: "paramsWasUpdate")
        }
    }
    
    func startAdvertising() {
        do {
            let defaults = UserDefaults(suiteName: DATA_GROUP)
            let advertisementData: [String: Any] = [CBAdvertisementDataServiceUUIDsKey: [uuidService],
                                                       CBAdvertisementDataLocalNameKey: ""]
            updateManager()
            logsSender.appendLog("startAdvertising")
            peripheralManager!.startAdvertising(advertisementData)
            if(self.isNeedWidgetRefresh == true) {
                defaults?.set(WidgetStates.RUNNING.rawValue, forKey: "widgetState")
                defaults?.synchronize()
                if #available(iOS 14.0, *) {
                    WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
                }
            } else {
                defaults?.set(AppBleStates.RUNNING.rawValue, forKey: "appBleState")
            }
        } catch {
            print("--- error startAdvertising catch")
            let defaults = UserDefaults(suiteName: DATA_GROUP)
            if(self.isNeedWidgetRefresh == true) {
                defaults?.set(WidgetStates.WAITING_START.rawValue, forKey: "widgetState")
                defaults?.synchronize()
                if #available(iOS 14.0, *) {
                    WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
                }
            } else {
                defaults?.set(AppBleStates.WAITING_START.rawValue, forKey: "appBleState")
            }
        }
        
    }
        
    func stopAdvertising() {
        logsSender.appendLog("stopAdvertising")
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        peripheralManager?.stopAdvertising()
        if(self.isNeedWidgetRefresh == true) {
            defaults?.set(WidgetStates.WAITING_START.rawValue, forKey: "widgetState")
            defaults?.synchronize()
            if #available(iOS 14.0, *) {
                WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
            }
        } else {
            defaults?.set(AppBleStates.WAITING_START.rawValue, forKey: "appBleState")
        }
    }
    
  
    public func start() {
        startAdvertising()
    }
    
    public func stop() {
        stopAdvertising()
    }
    
    private func toggleHighlight(highlight: WidgetHighlight) {
     
        if #available(iOS 14.0, *) {
            logsSender.appendLog("toggleHighlight")
            let backgroundQueue = DispatchQueue(label: "ru.nasvyazi", qos: .background)
            backgroundQueue.async {
                let defaults = UserDefaults(suiteName: DATA_GROUP)
                defaults?.set(highlight.rawValue, forKey: "widgetHighlight")
                WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
                sleep(1)
                defaults?.set(WidgetHighlight.NOTHING.rawValue, forKey: "widgetHighlight")
                WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
            }
        }
    }
    
    private func buildBLEService() -> CBMutableService {
        // create characteristics
        let charForRead = CBMutableCharacteristic(type: uuidCharForRead,
                                                  properties: .read,
                                                  value: nil,
                                                  permissions: .readable)
        let charForWrite = CBMutableCharacteristic(type: uuidCharForWrite,
                                                   properties: .write,
                                                   value: nil,
                                                   permissions: .writeable)
        let charForIndicate = CBMutableCharacteristic(type: uuidCharForIndicate,
                                                      properties: .indicate,
                                                      value: nil,
                                                      permissions: .readable)
        self.charForIndicate = charForIndicate

        // create service
        let service = CBMutableService(type: uuidService, primary: true)
        service.characteristics = [charForRead, charForWrite, charForIndicate]
        return service
    }
    
    
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        logsSender.appendLog("didUpdateState: \(peripheral.state.stringValue) self.isNeedWidgetRefresh: \(self.isNeedWidgetRefresh)")
        logsSender.appendLog("BLESERVER LOG peripheral.state ------> \(peripheral.state.stringValue)")
        if peripheral.state == .poweredOn {
            logsSender.appendLog("BLESERVER self.peripheralManager?.description => \(self.peripheralManager?.description ?? "nil")")
            self.peripheralManager?.removeAllServices()
            logsSender.appendLog("remove all BLE services")
            self.peripheralManager?.add(buildBLEService())
            logsSender.appendLog("adding BLE service")
            
            if(self.isNeedWidgetRefresh){
                
                let currState = defaults?.string(forKey: "widgetState")
                print("----- didUpdate currState \(currState ?? "EMPTY")")
                if(currState == WidgetStates.REQUIRED_ENABLE_BLUETOOTH.rawValue || currState == WidgetStates.REQUIRED_PERMISSION.rawValue ){
                    defaults?.set(WidgetStates.WAITING_START.rawValue, forKey: "widgetState")
                }
                defaults?.synchronize()
                if #available(iOS 14.0, *) {
                    WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
                }
            } else {
//                defaults?.set(AppBleStates.RUNNING.rawValue, forKey: "appBleState")
                defaults?.synchronize()
            }
        } else {
            
            if(self.isNeedWidgetRefresh == true) {
                if peripheral.state == .unauthorized || peripheral.state == .unsupported {
                    print("REQUIRED_PERMISSION didUpdate")
                    defaults?.set(WidgetStates.REQUIRED_PERMISSION.rawValue, forKey: "widgetState")
                }
                if peripheral.state == .poweredOff {
                    print("REQUIRED_ENABLE_BLUETOOTH didUpdate")
                    defaults?.set(WidgetStates.REQUIRED_ENABLE_BLUETOOTH.rawValue, forKey: "widgetState")
                }
                defaults?.synchronize()
                if #available(iOS 14.0, *) {
                    WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
                }
                
            } else {
                defaults?.set(AppBleStates.WAITING_START.rawValue, forKey: "appBleState")
                defaults?.synchronize()
            }
        }
    }
    
    public func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?) {
        if let error = error {
            logsSender.appendLog("didStartAdvertising: error: \(error.localizedDescription)")
        } else {
            logsSender.appendLog("didStartAdvertising: success")
        }
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didAdd service: CBService, error: Error?) {
        if let error = error {
            logsSender.appendLog("didAddService: error: \(error.localizedDescription)")
        } else {
            logsSender.appendLog("didAddService: success: \(service.uuid.uuidString)")
        }
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager,
                           central: CBCentral,
                           didSubscribeTo characteristic: CBCharacteristic) {
        logsSender.appendLog("didSubscribeTo UUID: \(characteristic.uuid.uuidString)")
        if characteristic.uuid == uuidCharForIndicate {
            subscribedCentrals.append(central)
        }
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager,
                           central: CBCentral,
                           didUnsubscribeFrom characteristic: CBCharacteristic) {
        logsSender.appendLog("didUnsubscribeFrom UUID: \(characteristic.uuid.uuidString)")
        if characteristic.uuid == uuidCharForIndicate {
            subscribedCentrals.removeAll { $0.identifier == central.identifier }
        }
        logsSender.appendLog("subscribedCentrals.description ----> \(subscribedCentrals.description)")
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveRead request: CBATTRequest) {
        var log = "didReceiveRead UUID: \(request.characteristic.uuid.uuidString)"
        log += "\noffset: \(request.offset)"

        switch request.characteristic.uuid {
        case uuidCharForRead:
            let textValue = self.textFieldDataForRead ?? ""
            log += "\nresponding with success, value = '\(textValue)'"
            request.value = textValue.data(using: .utf8)
            self.peripheralManager?.respond(to: request, withResult: .success)
        default:
            log += "\nresponding with attributeNotFound"
            self.peripheralManager?.respond(to: request, withResult: .attributeNotFound)
        }
        logsSender.appendLog(log)
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        var log = "didReceiveWrite requests.count = \(requests.count)"
        requests.forEach { (request) in
            log += "\nrequest.offset: \(request.offset)"
            log += "\nrequest.char.UUID: \(request.characteristic.uuid.uuidString)"
            switch request.characteristic.uuid {
            case uuidCharForWrite:
                let data = request.value ?? Data()
                let textValue = String(data: data, encoding: .utf8) ?? ""
                
                log += "\nresponding with success, value = '\(textValue)'"
                self.peripheralManager?.respond(to: request, withResult: .success)
                if(self.isNeedWidgetRefresh == true){
                    toggleHighlight(highlight: WidgetHighlight.SUCCESS)
                }
                
            default:
                log += "\nresponding with attributeNotFound"
                self.peripheralManager?.respond(to: request, withResult: .attributeNotFound)
                if(self.isNeedWidgetRefresh == true){
                    toggleHighlight(highlight: WidgetHighlight.FAIL)
                }
            }
        }
        logsSender.appendLog(log)
    }

    public func peripheralManagerIsReady(toUpdateSubscribers peripheral: CBPeripheralManager) {
        logsSender.appendLog("isReadyToUpdateSubscribers")
    }
    
    public func isAdvertising() -> Bool {
        return self.peripheralManager?.isAdvertising ?? false
    }
}
