
import CoreBluetooth

public class BLEServer: NSObject, CBPeripheralManagerDelegate {
    
    
    //    private var peripheralQueue: DispatchQueue = DispatchQueue(label: "com.nasvyazi.peripheralQueue", qos: .userInteractive)

    // BLE related properties
    
    var uuidService:CBUUID //= CBUUID(string: "25AE1441-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForRead:CBUUID // = CBUUID(string: "25AE1442-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForWrite:CBUUID // = CBUUID(string: "25AE1443-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForIndicate:CBUUID // = CBUUID(string: "25AE1444-05D3-4C5B-8281-93D4E07420CF")
    var textFieldDataForRead: String?
    
    var charForIndicate: CBMutableCharacteristic?
    
    var subscribedCentrals = [CBCentral]()
    let logsSender = LogsSender()

    var peripheralManager: CBPeripheralManager!
    var service: CBMutableService? = nil
    
    
    override init() {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        self.uuidService = CBUUID(string:defaults?.string(forKey: "SERVICE_UUID") ?? "25AE1441-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForRead = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_READ_UUID") ?? "25AE1442-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForWrite = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_WRITE_UUID") ?? "25AE1443-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForIndicate = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_INDICATE_UUID") ?? "25AE1444-05D3-4C5B-8281-93D4E07420CF")
        self.textFieldDataForRead = defaults?.string(forKey: "USER_UUID") ?? "EMPTY_ID"
        self.peripheralManager = CBPeripheralManager(delegate: nil, queue: .main , options: [CBCentralManagerScanOptionAllowDuplicatesKey:true])
        
        super.init()
        //        DispatchQueue.main.async{
        self.peripheralManager.delegate = self //DispatchQueue.global(qos: .default)
        //        }
    }
    deinit {
        self.stop()
        self.peripheralManager?.removeAllServices()
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        defaults?.set(false, forKey: "paramsWasUpdate_app")
    }
    func updateParams() {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        self.uuidService = CBUUID(string:defaults?.string(forKey: "SERVICE_UUID") ?? "25AE1441-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForRead = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_READ_UUID") ?? "25AE1442-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForWrite = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_WRITE_UUID") ?? "25AE1443-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForIndicate = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_INDICATE_UUID") ?? "25AE1444-05D3-4C5B-8281-93D4E07420CF")
        self.textFieldDataForRead = defaults?.string(forKey: "USER_UUID") ?? ""
    }
    
    func updateService() {
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        let paramsWasUpdate_app = defaults?.bool(forKey: "paramsWasUpdate_app") ?? true
        let paramsWasUpdate_widget = defaults?.bool(forKey: "paramsWasUpdate_widget") ?? true//paramsWasUpdate_widget
        
        self.logsSender.appendLog("updateService: paramsWasUpdate_app - \(paramsWasUpdate_app)")
        self.logsSender.appendLog("updateService: paramsWasUpdate_widget - \(paramsWasUpdate_widget)")
        self.logsSender.appendLog("updateService: peripheralManager.state - \(peripheralManager.state.stringValue)")
        
        if(self.peripheralManager.state == .poweredOn){
//            if(paramsWasUpdate_app){
                self.logsSender.appendLog("updateService a: app service update start ")
                if(self.service != nil){
                    self.logsSender.appendLog("updateService a: service - \(self.service!)")
                    self.peripheralManager.remove(self.service!)
                    self.logsSender.appendLog("updateService a: remove service ")
                }

                self.updateParams()
                self.logsSender.appendLog("updateService a: updateParams ")
                self.service = self.buildBLEService()
                self.logsSender.appendLog("updateService a: buildBLEService ")
                self.peripheralManager.add(self.service!)
                self.logsSender.appendLog("updateService a: add service ")
                defaults?.set(false, forKey: "paramsWasUpdate_app")
                self.logsSender.appendLog("updateService a: end ")
//            }
//            if(paramsWasUpdate_widget && self.isNeedWidgetRefresh){
//                self.logsSender.appendLog("updateService w: widget service update start ")
//                if(self.service != nil){
//                    self.logsSender.appendLog("updateService w: service - \(self.service!)")
//                    self.peripheralManager.remove(self.service!)
//                    self.logsSender.appendLog("updateService w: remove service")
//                }
//                self.updateParams()
//                self.logsSender.appendLog("updateService w: updateParams ")
//                self.service = self.buildBLEService()
//                self.logsSender.appendLog("updateService w: buildBLEService ")
//                self.peripheralManager.add(self.service!)
//                self.logsSender.appendLog("updateService w: add service ")
//                defaults?.set(false, forKey: "paramsWasUpdate_widget")
//                self.logsSender.appendLog("updateService w: end ")
//            }
        }
        
        defaults?.synchronize()
    }
    
    func startAdvertising() {
//        print()
        do {
            let defaults = UserDefaults(suiteName: DATA_GROUP)
            self.updateService()
            let advertisementData: [String: Any] = [CBAdvertisementDataServiceUUIDsKey: [self.uuidService],
                                                       CBAdvertisementDataLocalNameKey: ""]
            
            self.logsSender.appendLog("startAdvertising isRuning\(peripheralManager.isAdvertising)")
            self.peripheralManager!.startAdvertising(advertisementData)
            defaults?.set(AppBleStates.RUNNING.rawValue, forKey: "appBleState")
            
        } catch {
            print("--- error startAdvertising catch")
            let defaults = UserDefaults(suiteName: DATA_GROUP)
            defaults?.set(AppBleStates.WAITING_START.rawValue, forKey: "appBleState")
            
        }
    }
        
    func stopAdvertising() {
        
        self.logsSender.appendLog("stopAdvertising")
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        self.peripheralManager?.stopAdvertising()
        defaults?.set(AppBleStates.WAITING_START.rawValue, forKey: "appBleState")
        
    }
    
  
    public func start() {
        startAdvertising()
    }
    
    public func stop() {
        stopAdvertising()
    }
    
//    private func toggleHighlight(highlight: WidgetHighlight) {
//     
//        if #available(iOS 14.0, *) {
//            logsSender.appendLog("toggleHighlight")
//            let backgroundQueue = DispatchQueue(label: "ru.nasvyazi.widgetHilight", qos: .background)
//            backgroundQueue.async {
//                let defaults = UserDefaults(suiteName: DATA_GROUP)
//                defaults?.set(highlight.rawValue, forKey: "widgetHighlight")
//                WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
//                sleep(2)
//                defaults?.set(WidgetHighlight.NOTHING.rawValue, forKey: "widgetHighlight")
//                WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
//            }
//        }
//    }
    
    private func buildBLEService() -> CBMutableService {
        // create characteristics
        
        let charForRead = CBMutableCharacteristic(type: self.uuidCharForRead,
                                                  properties: .read,
                                                  value: nil,
                                                  permissions: .readable)
        let charForWrite = CBMutableCharacteristic(type: self.uuidCharForWrite,
                                                   properties: .write,
                                                   value: nil,
                                                   permissions: .writeable)
        let charForIndicate = CBMutableCharacteristic(type: self.uuidCharForIndicate,
                                                      properties: .indicate,
                                                      value: nil,
                                                      permissions: .readable)
        self.charForIndicate = charForIndicate
        
        // create service
        let service = CBMutableService(type: self.uuidService, primary: true)
        service.characteristics = [charForRead, charForWrite, charForIndicate]
        return service
        
    }
    
    
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        self.logsSender.appendLog("didUpdateState: \(peripheral.state.stringValue)")
        self.logsSender.appendLog("BLESERVER LOG peripheral.state ------> \(peripheral.state.stringValue)")
        if peripheral.state == .poweredOn {
            self.logsSender.appendLog("BLESERVER self.peripheralManager?.description => \(self.peripheralManager?.description ?? "nil")")
        } else {
            defaults?.set(AppBleStates.WAITING_START.rawValue, forKey: "appBleState")
            defaults?.synchronize()
        }
        
    }
    
    public func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?) {
        if let error = error {
            self.logsSender.appendLog("didStartAdvertising: error: \(error.localizedDescription)")
        } else {
            self.logsSender.appendLog("didStartAdvertising: success")
        }
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didAdd service: CBService, error: Error?) {
        
        if let error = error {
            self.logsSender.appendLog("didAddService: error: \(error.localizedDescription)")
        } else {
            self.logsSender.appendLog("didAddService: success: \(service.uuid.uuidString)")
        }
        
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager,
                           central: CBCentral,
                                  didSubscribeTo characteristic: CBCharacteristic) {
        self.logsSender.appendLog("didSubscribeTo UUID: \(characteristic.uuid.uuidString)")
        if characteristic.uuid == self.uuidCharForIndicate {
            self.subscribedCentrals.append(central)
        }
        
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager,
                           central: CBCentral,
                                  didUnsubscribeFrom characteristic: CBCharacteristic) {
        self.logsSender.appendLog("didUnsubscribeFrom UUID: \(characteristic.uuid.uuidString)")
        if characteristic.uuid == self.uuidCharForIndicate {
            self.subscribedCentrals.removeAll { $0.identifier == central.identifier }
        }
        self.logsSender.appendLog("subscribedCentrals.description ----> \(self.subscribedCentrals.description)")
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveRead request: CBATTRequest) {
        
        var log = "didReceiveRead UUID: \(request.characteristic.uuid.uuidString)"
        log += "\noffset: \(request.offset)"
        
        switch request.characteristic.uuid {
        case self.uuidCharForRead:
            let textValue = self.textFieldDataForRead ?? ""
            log += "\nresponding with success, value = '\(textValue)'"
            request.value = textValue.data(using: .utf8)
            self.peripheralManager?.respond(to: request, withResult: .success)
        default:
            log += "\nresponding with attributeNotFound"
            self.peripheralManager?.respond(to: request, withResult: .attributeNotFound)
        }
        self.logsSender.appendLog(log)
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        
        
        var log = "didReceiveWrite requests.count = \(requests.count)"
        requests.forEach { (request) in
            log += "\nrequest.offset: \(request.offset)"
            log += "\nrequest.char.UUID: \(request.characteristic.uuid.uuidString)"
            switch request.characteristic.uuid {
            case self.uuidCharForWrite:
                let data = request.value ?? Data()
                let textValue = String(data: data, encoding: .utf8) ?? ""
                
                log += "\nresponding with success, value = '\(textValue)'"
                self.peripheralManager?.respond(to: request, withResult: .success)
                
                
            default:
                log += "\nresponding with attributeNotFound"
                self.peripheralManager?.respond(to: request, withResult: .attributeNotFound)
                
            }
        }
        self.logsSender.appendLog(log)
        
    }

    public func peripheralManagerIsReady(toUpdateSubscribers peripheral: CBPeripheralManager) {
        
        self.logsSender.appendLog("isReadyToUpdateSubscribers")
        
    }
    
    public func isAdvertising() -> Bool {
        return self.peripheralManager?.isAdvertising ?? false
    }
}


extension CBManagerState {
    var stringValue: String {
        switch self {
        case .unknown: return "unknown"
        case .resetting: return "resetting"
        case .unsupported: return "unsupported"
        case .unauthorized: return "unauthorized"
        case .poweredOff: return "poweredOff"
        case .poweredOn: return "poweredOn"
        @unknown default: return "\(rawValue)"
        }
    }
}
