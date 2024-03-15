
import CoreBluetooth
import WidgetKit


public class BLEController: NSObject {

    // BLE related properties
    var isNeedWidgetRefresh: Bool
    var uuidService:CBUUID //= CBUUID(string: "25AE1441-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForRead:CBUUID // = CBUUID(string: "25AE1442-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForWrite:CBUUID // = CBUUID(string: "25AE1443-05D3-4C5B-8281-93D4E07420CF")
    var uuidCharForIndicate:CBUUID // = CBUUID(string: "25AE1444-05D3-4C5B-8281-93D4E07420CF")

    var blePeripheral: CBPeripheralManager!
    var charForIndicate: CBMutableCharacteristic?
    var subscribedCentrals = [CBCentral]()
    var counter: Int = 0
    var textFieldDataForRead: String!
    var textFieldDataForWrite: String!
    let logsSender = LogsSender()

    public init(isNeedWidgetRefresh: Bool) {
        self.isNeedWidgetRefresh = isNeedWidgetRefresh
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        self.uuidService = CBUUID(string:defaults?.string(forKey: "SERVICE_UUID") ?? "25AE1441-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForRead = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_READ_UUID") ?? "25AE1442-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForWrite = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_WRITE_UUID") ?? "25AE1443-05D3-4C5B-8281-93D4E07420CF")
        self.uuidCharForIndicate = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_INDICATE_UUID") ?? "25AE1444-05D3-4C5B-8281-93D4E07420CF")
        self.textFieldDataForRead = defaults?.string(forKey: "USER_UUID") ?? "EMPTY_ID"
    }
    
    public func createBLE() {
        
        print("ASD HERE")
        
        initBLE()
    }
    public func start() {
        print("------ START -----> \(self.isNeedWidgetRefresh)" )
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        uuidService = CBUUID(string:defaults?.string(forKey: "SERVICE_UUID") ?? "25AE1441-05D3-4C5B-8281-93D4E07420CF")
        uuidCharForRead = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_READ_UUID") ?? "25AE1442-05D3-4C5B-8281-93D4E07420CF")
        uuidCharForWrite = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_WRITE_UUID") ?? "25AE1443-05D3-4C5B-8281-93D4E07420CF")
        uuidCharForIndicate = CBUUID(string: defaults?.string(forKey: "CHAR_FOR_INDICATE_UUID") ?? "25AE1444-05D3-4C5B-8281-93D4E07420CF")
        textFieldDataForRead = defaults?.string(forKey: "USER_UUID") ?? "EMPTY_ID"
        print(defaults?.string(forKey: "USER_UUID") ?? "EMPTY_ID");
        if(isAdvertising()){
            bleStopAdvertising()
        }
        blePeripheral = CBPeripheralManager(delegate: self, queue: DispatchQueue.main)
        print("--------- initBLE --- \(String(describing: blePeripheral))")
        print("blePeripheral.state = \(blePeripheral.state.stringValue)")
   
    }
    public func stop() {
        bleStopAdvertising()
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        if(self.isNeedWidgetRefresh == true) {
            defaults?.set(WidgetStates.WAITING_START.rawValue, forKey: "widgetState")
            if #available(iOS 14.0, *) {
                WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
            }
        } else {
            defaults?.set(AppBleStates.WAITING_START.rawValue, forKey: "appBleState")
        }
    }
}

// MARK: - BLE related methods
extension BLEController {

    private func initBLE() {
        // using DispatchQueue.main means we can update UI directly from delegate methods
       
       
        // BLE service must be created AFTER CBPeripheralManager receives .poweredOn state
        // see peripheralManagerDidUpdateState
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

    private func bleStartAdvertising(_ advertisementData: String) {
        let dictionary: [String: Any] = [CBAdvertisementDataServiceUUIDsKey: [uuidService],
                                         CBAdvertisementDataLocalNameKey: advertisementData]
        logsSender.appendLog("startAdvertising")
        blePeripheral.startAdvertising(dictionary)
    }

    private func bleStopAdvertising() {
        logsSender.appendLog("stopAdvertising")
        blePeripheral?.stopAdvertising()
    }

    private func bleSendIndication(_ valueString: String) {
        guard let charForIndicate = charForIndicate else {
            logsSender.appendLog("cannot indicate, characteristic is nil")
            return
        }
        let data = valueString.data(using: .utf8) ?? Data()
        let result = blePeripheral.updateValue(data, for: charForIndicate, onSubscribedCentrals: nil)
        let resultStr = result ? "true" : "false"
        logsSender.appendLog("updateValue result = '\(resultStr)' value = '\(valueString)'")
    }

    private func bleGetStatusString() -> String {
        guard let blePeripheral = blePeripheral else { return "not initialized" }
        switch blePeripheral.state {
        case .unauthorized:
            return blePeripheral.state.stringValue + " (allow in Settings)"
        case .poweredOff:
            return "Bluetooth OFF"
        case .poweredOn:
            let advertising = blePeripheral.isAdvertising ? "advertising" : "not advertising"
            return "ON, \(advertising)"
        default:
            return blePeripheral.state.stringValue
        }
    }
    
    private func toggleHighlight(highlight: WidgetHighlight) {
        if #available(iOS 14.0, *) {
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
}

// MARK: - CBPeripheralManagerDelegate
extension BLEController: CBPeripheralManagerDelegate {
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        print("----- didUpdate")
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        logsSender.appendLog("didUpdateState: \(peripheral.state.stringValue) self.isNeedWidgetRefresh: \(self.isNeedWidgetRefresh)")
      
        if (peripheral.state == .poweredOn){
            let currState = defaults?.string(forKey: "widgetState")
            print("----- didUpdate currState \(currState)")
            if(currState == WidgetStates.REQUIRED_ENABLE_BLUETOOTH.rawValue || currState == WidgetStates.REQUIRED_PERMISSION.rawValue ){
                defaults?.set(WidgetStates.WAITING_START.rawValue, forKey: "widgetState")
            } else {
                print("--------- blePeripheral.state == .poweredOn --- ")
                print("--------- removeAllServices --- ")
                blePeripheral.removeAllServices()
                print("--------- add BuildBLE --- ")
                blePeripheral.add(buildBLEService())
                logsSender.appendLog("adding BLE service")
                bleStartAdvertising("")
                if(self.isNeedWidgetRefresh == true) {
                    defaults?.set(WidgetStates.RUNNING.rawValue, forKey: "widgetState")
                } else {
                    defaults?.set(AppBleStates.RUNNING.rawValue, forKey: "appBleState")
                }
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
                
            } else {
                defaults?.set(AppBleStates.WAITING_START.rawValue, forKey: "appBleState")
            }
        }
        
        defaults?.synchronize()
        if #available(iOS 14.0, *) {
            print("reload didUpdate")
            WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
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
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveRead request: CBATTRequest) {
        var log = "didReceiveRead UUID: \(request.characteristic.uuid.uuidString)"
        log += "\noffset: \(request.offset)"

        switch request.characteristic.uuid {
        case uuidCharForRead:
            let textValue = self.textFieldDataForRead ?? ""
            log += "\nresponding with success, value = '\(textValue)'"
            request.value = textValue.data(using: .utf8)
            blePeripheral.respond(to: request, withResult: .success)
        default:
            log += "\nresponding with attributeNotFound"
            blePeripheral.respond(to: request, withResult: .attributeNotFound)
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
                textFieldDataForWrite = textValue
                log += "\nresponding with success, value = '\(textValue)'"
                blePeripheral.respond(to: request, withResult: .success)
                if(self.isNeedWidgetRefresh == true){
                    toggleHighlight(highlight: WidgetHighlight.SUCCESS)
                }
                
            default:
                log += "\nresponding with attributeNotFound"
                blePeripheral.respond(to: request, withResult: .attributeNotFound)
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
        return blePeripheral?.isAdvertising ?? false
    }
}

// MARK: - Other extensions
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
