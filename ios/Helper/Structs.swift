
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

public enum WidgetActions: String {
    case NONE = "0"
    case LET_INIT = "1"
    case LET_START = "2"
    case LET_STOP = "3"
    
}


