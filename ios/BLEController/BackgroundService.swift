import Foundation
import WidgetKit
import OSLog

@objc public class BackgroundService: NSObject {
    
//    public static var service: BackgroundService! = BackgroundService()
//    
    static let logsSender : LogsSender = LogsSender()
    var BLEC: BLEController? = nil
//    public static let shared = BackgroundService()
    
   public override init() {}
    
    public func initBLE() {
        
        print("BackgroundService ----> initBLE")
        self.BLEC = BLEController()
        self.BLEC?.createBLE()
        BackgroundService.logsSender.appendLog("BackgroundService ----> initBLE")
    }
    
    public func start() {
        print("start1")
        print(self.BLEC != nil ? "YES BLEC" : "NO BLEC")
        
        if (self.BLEC == nil){
            self.initBLE()
        }
        
        print("start12")
        self.BLEC?.start()
        print("start2")
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        
            print("start3")
        defaults?.set(WidgetStates.RUNNING.rawValue, forKey: "widgetState")
        if #available(iOS 14.0, *) {
            
                print("start4")
            WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
            
                print("start5")
        }
    }

    public func stop() {
        
        self.BLEC?.stop()
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        defaults?.set(WidgetStates.WAITING_START.rawValue, forKey: "widgetState")
        if #available(iOS 14.0, *) {
            WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
        }
    }
    
    public func openSettings() {
        let settingsUrl =  URL(string: UIApplication.openSettingsURLString)!
        UIApplication.shared.open(settingsUrl, options: [:], completionHandler: nil)
    }
}

//public let BGServiceInstace = BackgroundService()
//
//public func getBGServiceInstance() -> BackgroundService {
//    return BGServiceInstace
//}

