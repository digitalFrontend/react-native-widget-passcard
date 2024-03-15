import Foundation

public class LogsSender: NSObject {
    let timeFormatter = DateFormatter()
    
    public func appendLog(_ message: String) {
        let logLine = "\(timeFormatter.string(from: Date())) \(message)"
        print("DEBUG: \(logLine)")
        if(WidgetPasscardEventEmitter.canSend) {
            print("---------------> canSend true")
            WidgetPasscardEventEmitter.emitter?.sendEvent(withName: "LOGS_RECEIVER", body: ["log":logLine])
        } else {
            print("---------------> canSend false")
        }
       
    }
    
}
