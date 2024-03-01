import Foundation


@objc public class BackgroundService: NSObject {
    
//    public static var service: BackgroundService! = BackgroundService()
//    
    private let logsSender : LogsSender = LogsSender()
    private var COUNTER: Int = 0
    
    
    override init() {
        super.init()
//        BackgroundService.service = self
    }
    public func start() {
        print("COUNTER ----> \(COUNTER)")
        logsSender.appendLog("COUNTER ----> \(COUNTER)")
        COUNTER+=1
    }
    public func stop() {
        print("COUNTER ----> \(COUNTER)")
        logsSender.appendLog("COUNTER ----> \(COUNTER)")
    }
}

public let BGService = BackgroundService()

public func getBGServiceInstance() -> BackgroundService {
    return BGService
}
