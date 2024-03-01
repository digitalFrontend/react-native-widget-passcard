
import Foundation

let DATA_GROUP = "group.ru.nasvyazi";
//let DATA_KEY = "widgetPasscardKey";

let NOT_INITIALIZED = "0";
let WAITING_START = "1";
let RUNNING = "2";
let REQUIRED_PERMISSION = "3";
let REQUIRED_ENABLE_BLUETOOTH = "4";
let NOTHING = "0";
let SUCCESS = "1";
let FAIL = "2";


struct TransferParams: Decodable, Encodable {
    let data: [String]
//    let updateDate: String
//    let hasTeleopti: Bool
}
struct BLEParams: Decodable, Encodable {
    let textFieldAdvertisingData: String
    let SERVICE_UUID: String
    let CHAR_FOR_WRITE_UUID: String
    let CHAR_FOR_INDICATE_UUID: String
}




