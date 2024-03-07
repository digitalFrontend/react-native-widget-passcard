import Foundation
import OSLog


public class LogsSender: NSObject {
    let timeFormatter = DateFormatter()
    
    public func appendLog(_ message: String) {
        let logLine = "\(timeFormatter.string(from: Date())) \(message)"
        
        if #available(iOS 14.0, *) {
            let logger = Logger()
            logger.info("\(logLine)")
        }
        
        print("DEBUG: \(logLine)")
        WidgetPasscardEventEmitter.emitter?.sendEvent(withName: "LOGS_RECEIVER", body: ["log":logLine])
    }
    
}
