import Foundation
import WidgetKit
import OSLog

@objc public class BackgroundService: NSObject {
    
//    public static var service: BackgroundService! = BackgroundService()
//    
    static let logsSender : LogsSender = LogsSender()
    var BLEC: BLEController? = nil
    var startTime: Date? = nil
//    public static let shared = BackgroundService()
    
   public override init() {}
    
    public func initBLE() {
        
        print("BackgroundService ----> initBLE")
        self.BLEC = BLEController()
        self.BLEC?.createBLE()
        BackgroundService.logsSender.appendLog("BackgroundService ----> initBLE")
    }
    
    public func start() {
        
        if (self.BLEC == nil){
            self.initBLE()
        }
        
        self.BLEC?.start()
        let defaults = UserDefaults(suiteName: DATA_GROUP)
        
        defaults?.set(WidgetStates.RUNNING.rawValue, forKey: "widgetState")
        
        if #available(iOS 14.0, *) {
            
            WidgetCenter.shared.reloadTimelines(ofKind: "WidgetTeleopti")
            
        }
        
        let workTime = defaults?.integer(forKey: "WORK_TIME") ?? 0
        
        startTime = Date()
        
        Task {
            await startTimer(workTime: workTime)
        }
    }
    
    public func startTimer(workTime: Int) async {
        do {
            try await Task.sleep(nanoseconds: UInt64(5 * Double(NSEC_PER_SEC)))
            
            
        }catch{
            print(error.localizedDescription)
        }
        
        let currentTime = Date()
        
        let log = "Time: "+currentTime.description
        BackgroundService.logsSender.appendLog(log);
        
        if (workTime != 0){
            let diff = currentTime - startTime!
            
            if (Int(diff.rounded()) > workTime){
                let log2 = "Time suspended, close"
                BackgroundService.logsSender.appendLog(log2);
                
                stop()
            }
        }
        
        if (self.BLEC != nil &&  self.BLEC!.isAdvertising()){
            await startTimer(workTime: workTime)
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

extension Date {

    static func - (lhs: Date, rhs: Date) -> TimeInterval {
        return lhs.timeIntervalSinceReferenceDate - rhs.timeIntervalSinceReferenceDate
    }

}

//public let BGServiceInstace = BackgroundService()
//
//public func getBGServiceInstance() -> BackgroundService {
//    return BGServiceInstace
//}

