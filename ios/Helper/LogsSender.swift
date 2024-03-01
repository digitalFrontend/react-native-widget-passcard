import Foundation

 public class LogsSender: NSObject {
    let timeFormatter = DateFormatter()
    
    public func appendLog(_ message: String) {
        let logLine = "\(timeFormatter.string(from: Date())) \(message)"
        print("DEBUG: \(logLine)")
        WidgetPasscardEventEmitter.emitter.sendEvent(withName: "LOGS_RECEIVER", body: ["log":logLine])
    }
    
}
