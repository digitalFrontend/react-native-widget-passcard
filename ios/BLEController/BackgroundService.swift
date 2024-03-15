import Foundation
import WidgetKit
import OSLog

@objc public class BackgroundService: NSObject {
    
//    public static var service: BackgroundService! = BackgroundService()
//    
    static let logsSender : LogsSender = LogsSender()
    var BLEC: BLEServer
    var startTime: Date? = nil
    var isNeedWidgetRefresh: Bool?
//    public static let shared = BackgroundService()
    
   public init(isNeedWidgetRefresh: Bool) {
        self.isNeedWidgetRefresh = isNeedWidgetRefresh
        self.BLEC = BLEServer(isNeedWidgetRefresh: isNeedWidgetRefresh)
    }
    
//    public func initBLE() {
//        print("BackgroundService ----> initBLE")
//        self.BLEC?.createBLE()
//        BackgroundService.logsSender.appendLog("BackgroundService ----> initBLE")
//    }
//    
    public func start() {
//        if(self.BLEC.isAdvertising()){
//            self.BLEC.stop()
//        }
        
        self.BLEC.start()
        let defaults = UserDefaults(suiteName: DATA_GROUP)
                
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
        
        if (self.BLEC != nil &&  self.BLEC.isAdvertising()){
            await startTimer(workTime: workTime)
        }
        
    }

    public func stop() {
        self.BLEC.stop()
        let defaults = UserDefaults(suiteName: DATA_GROUP)
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

