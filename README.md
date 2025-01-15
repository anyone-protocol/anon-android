## Anon Android

This is native Android `AnonService` built on the Anon shared library built for
Android.  The included _libanon.so_ binaries can also be used directly as an anon
daemon.

First, make sure the Maven Central repo is added to your top level `build.gradle` project:
```gradle
allprojects {
    repositories {
        // ...
        mavenCentral()
    }
}
```

Then add the `anon-android` dependency to your project:
```gradle
dependencies {
    implementation 'io.anyone:anon-android:0.4.9.10'
}
```

Apps using anon-android need to declare the `INTERNET` permission in their Android Manifest file:

```xml
    <uses-permission android:name="android.permission.INTERNET"/>
```

Anon protects your privacy on the internet by hiding the connection 
between your Internet address and the services you use. We believe Anon
is reasonably secure, but please ensure you read the instructions and
configure it properly. Learn more at https://anyone.io/

## Minimum Requirements 

In order to use anon-android you need to target Android **API 21** or higher. 

It runs on the following hardware architectures:
- arm64-v8a 
- armeabi-v7a
- x86
- x86_64

## Anon Frequently Asked Questions:
        
- https://2019.www.torproject.org/docs/faq
- https://support.torproject.org/faq/


## How to Build

Please see: https://github.com/anyone-protocol/anon-android/blob/main/BUILD.md

This can be built using the included Docker or Vagrant VM setup. Vagrant will
run with VirtualBox.
