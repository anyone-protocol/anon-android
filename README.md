## Tor Android

This is native Android `AnonService` built on the Anon shared library built for
Android.  The included _libanon.so_ binaries can also be used directly as an anon
daemon.

First add the repo to your top level `build.gradle` project:
```gradle
allprojects {
    repositories {
        // ...
        maven { url "https://raw.githubusercontent.com/anyone-protocol/gpmaven/master" }
    }
}
```

Then add the `anon-android` dependency to your project:
```gradle
dependencies {
    implementation 'io.anyone:anon-android:0.4.9.7'
}
```

Apps using tor-android need to declare the `INTERNET` permission in their Android Manifest file:

```xml
    <uses-permission android:name="android.permission.INTERNET"/>
```

Tor protects your privacy on the internet by hiding the connection 
between your Internet address and the services you use. We believe Tor
is reasonably secure, but please ensure you read the instructions and
configure it properly. Learn more at https://anyone.io/

## Minimum Requirements 

In order to use tor-android you need to target Android **API 21** or higher. 

It runs on the following hardware architectures:
- arm64-v8a 
- armeabi-v7a
- x86
- x86_64

## Tor Frequently Asked Questions:
        
- https://2019.www.torproject.org/docs/faq
- https://support.torproject.org/faq/


## How to Build

Please see: https://github.com/anyone-protocol/anon-android/blob/main/BUILD.md

This can be built using the included Docker or Vagrant VM setup. Vagrant will
run with VirtualBox.
