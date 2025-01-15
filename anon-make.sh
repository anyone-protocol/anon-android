#!/usr/bin/env bash

set -e

fetch_submodules()
{
    if [ -n "$1" ]; then
        echo "Cleaning repository"
        git reset --hard
        git clean -fdx
        git submodule foreach git reset --hard
        git submodule foreach git clean -fdx
    fi
    echo "Fetching git submodules"
    git submodule sync
    git submodule foreach git submodule sync
    git submodule update --init --recursive
}

# predictable build paths make reproducible builds easier, so this
# tries to find things at likely standard paths
check_android_dependencies()
{
    if [ -d /opt/android-sdk ]; then
        export ANDROID_HOME=/opt/android-sdk
    elif [ ! -e "$ANDROID_HOME" ]; then
        echo "ANDROID_HOME must be set!"
        exit 1
    fi
    export ANDROID_SDK_ROOT="$ANDROID_HOME"

    # openssl wants a var called ANDROID_NDK_HOME
    if [ ! -e "$ANDROID_NDK_HOME" ]; then
        ndkVersion=$(sed -En 's,NDK_REQUIRED_REVISION *:?= *([0-9.]+).*,\1,p' external/Makefile)
        echo $ANDROID_HOME/ndk/$ndkVersion/source.properties
        if [ -n "$ANDROID_NDK_ROOT" ]; then
            export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
        elif [ -e "$ANDROID_HOME/ndk/$ndkVersion/source.properties" ]; then
            export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$ndkVersion"
        elif [ -e "$ANDROID_HOME/ndk-bundle/source.properties" ]; then
            export ANDROID_NDK_HOME="$ANDROID_HOME/ndk-bundle"
        else
            echo "ANDROID_NDK_HOME must be set!"
            exit 1
        fi
        export ANDROID_NDK_ROOT=$ANDROID_NDK_HOME
    fi
    echo "Using Android SDK: $ANDROID_HOME"
    echo "Using Android NDK: $ANDROID_NDK_HOME"
}

build_libs()
{
    echo "Building anon-android native libraries"
    check_android_dependencies
    if [ -f external/bin/termux-elf-cleaner ]; then
        make -C external -f build-tools clean
    fi
    make -C external -f build-tools
    for abi in $abis; do
        default_abis=`echo $default_abis | sed -E "s,(\s?)$abi(\s?),\1\2,"`
        APP_ABI=$abi make -C external clean
        APP_ABI=$abi make -C external
        binary=external/lib/$abi/libanon.so
        test -e $binary || (echo ERROR $abi missing $binary; exit 1)
    done
    for abi in $default_abis; do
        echo remove dangling symlink: $abi
        rm -f anon-android-binary/src/main/jniLibs/$abi
    done
}

show_options()
{
    echo "usage: ./anon-make.sh command arguments"
    echo ""
    echo "Commands:"
    echo "          fetch   Fetch git submodules"
    echo "          build   Build native libraries"
    echo ""
    echo "Options:"
    echo "          -a      ABI(s) to build (default: \"$default_abis\")"
    echo "          -c      Clean the repository (Used together with the fetch command)"
    echo ""
    exit
}

option=$1
default_abis="arm64-v8a armeabi-v7a x86 x86_64"
abis=$default_abis

if [ -z $option ]; then
    show_options
fi
shift

while getopts 'a:b:c' opts; do
    case $opts in
        a) abis=${OPTARG:-$abis} ;;
        c) clean=clean ;;
    esac
done

case "$option" in
    "fetch") fetch_submodules $clean ;;
    "build") build_libs ;;
    *) show_options ;;
esac
