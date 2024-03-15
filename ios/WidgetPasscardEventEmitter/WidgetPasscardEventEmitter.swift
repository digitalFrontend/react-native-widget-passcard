import Foundation
import React

@objc(WidgetPasscardEventEmitter)
//m.b. "open class WidgetPasscardEventEmitter: RCTEventEmitter {"
class WidgetPasscardEventEmitter: RCTEventEmitter {
    
    public static var emitter: RCTEventEmitter!
    public static var canSend: Bool = false
    
    override init() {
      super.init()
        WidgetPasscardEventEmitter.emitter = self
    }
    
    @objc override func startObserving(){
        WidgetPasscardEventEmitter.canSend = true
    };
    
    @objc override func stopObserving(){
        WidgetPasscardEventEmitter.canSend = false
    };


    @objc override public static func requiresMainQueueSetup() -> Bool {
        return false
    }

    open override func supportedEvents() -> [String] {
      ["LOGS_RECEIVER"]
    }
  }

