FROM debian:bookworm

RUN apt-get -y update
RUN apt-get -y install python3 python3-requests
RUN apt-get -y install autotools-dev
RUN apt-get -y install automake
RUN apt-get -y install autogen autoconf libtool gettext-base autopoint
RUN apt-get -y install git make g++ po4a pkg-config openjdk-17-jdk openjdk-17-jre
RUN apt-get -y install android-sdk

COPY . .

ENV ANDROID_HOME=/usr/lib/android-sdk

RUN rm -rf sdkmanager
RUN git clone --depth 1 --branch "0.6.10" https://gitlab.com/fdroid/sdkmanager.git
RUN ./sdkmanager/sdkmanager.py "ndk;27.2.12479018"

RUN ./anon-make.sh fetch
RUN ./anon-make.sh build
