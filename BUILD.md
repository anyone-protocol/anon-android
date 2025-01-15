# On Linux

These instructions are for building anon-android on a Debian based system.

First install the prerequisite packages:

```bash
sudo apt install autotools-dev
sudo apt install automake
sudo apt install autogen autoconf libtool gettext-base autopoint
sudo apt install git make g++ po4a pkg-config openjdk-17-jdk openjdk-17-jre
```

Then obtain the Android SDK and NDK. The Android SDK is installed by default with Android Studio, and the NDK can be downloaded from within Android Studio's SDK manager.

for now, anon-android is built with NDK toolchain 27.2.12479018

Then set these environment variables for the SDK and NDK:

```bash
export ANDROID_HOME=~/Android/Sdk
export ANDROID_NDK_HOME=~/Android/Sdk/ndk/27.2.12479018
```

Be sure that you have all of the git submodules up-to-date:
```bash
./anon-make.sh fetch
```

To build, run:
```bash
# make a universal anon-android library for every supported architecture
./anon-make.sh build 
# make an anon-android library for particular architectures from:
# arm64-v8a armeabi-v7a x86 x86_64, e.g.:
./anon-make.sh build -a arm64-v8a
```

This will produce an unsigned anon-android AAR

# On other OSes with an Intel processor using Vagrant

```bash
brew install vagrant
vagrant up --provision
```

# With Docker

There's also a Dockerfile if you like Docker more than Vagrant.