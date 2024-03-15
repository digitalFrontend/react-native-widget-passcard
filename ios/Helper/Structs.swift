
import Foundation

let DATA_GROUP = "group.ru.nasvyazi";
//let DATA_KEY = "widgetPasscardKey";

public enum WidgetStates: String {
    
    case NOT_INITIALIZED = "0"
    
    case WAITING_START = "1"
    
    case RUNNING = "2"
    
    case REQUIRED_PERMISSION = "3"
    
    case REQUIRED_ENABLE_BLUETOOTH = "4"
    

    
}
public enum WidgetHighlight: String {
    case NOTHING = "0"
    case SUCCESS = "1"
    case FAIL = "2"
}

public enum AppBleStates: String {
    case WAITING_START = "0"
    case RUNNING = "1"

}

