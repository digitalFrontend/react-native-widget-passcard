#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(WidgetSharePasscardData, NSObject)

RCT_EXTERN_METHOD(getParams: (RCTPromiseResolveBlock*) resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(initData: (RCTPromiseResolveBlock*) resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(setParams: (NSDictionary*) params
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(sendCustomEvent: (RCTPromiseResolveBlock*) resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(start: (RCTPromiseResolveBlock*) resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(stop: (RCTPromiseResolveBlock*) resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getWidgetState: (RCTPromiseResolveBlock*) resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getAppBLEState: (RCTPromiseResolveBlock*) resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)


@end
