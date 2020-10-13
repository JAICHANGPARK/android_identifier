#import "AndroidMultipleIdentifierPlugin.h"
#if __has_include(<android_multiple_identifier/android_multiple_identifier-Swift.h>)
#import <android_multiple_identifier/android_multiple_identifier-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "android_multiple_identifier-Swift.h"
#endif

@implementation AndroidMultipleIdentifierPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftAndroidMultipleIdentifierPlugin registerWithRegistrar:registrar];
}
@end
