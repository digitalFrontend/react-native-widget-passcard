import Foundation
import React

@objc(WidgetPasscardEventEmitter)
//m.b. "open class WidgetPasscardEventEmitter: RCTEventEmitter {"
class WidgetPasscardEventEmitter: RCTEventEmitter {
    
    public static var emitter: RCTEventEmitter!

    override init() {
      super.init()
        WidgetPasscardEventEmitter.emitter = self
    }

    open override func supportedEvents() -> [String] {
      ["LOGS_RECEIVER"]
    }
  }

