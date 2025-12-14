#!/bin/bash
set -e

############ INPUT VARS ###################
# Assert vars are ok
if [[ ! -v ${JAVA_HOME} ]] && [[ ! -d ${JAVA_HOME} ]]; then
  echo "[!] Missing JAVA_HOME var or folder not found"
  exit 1
fi

if [[ ! -v ${SMART_THINGS_APK} ]] && [[ ! -f ${SMART_THINGS_APK} ]]; then
  echo "[!] Missing SMART_THINGS_APK var or file not found"
  exit 1
fi

if [[ ! -v ${FIND_MY_MOBILE_LITE_APK} ]] && [[ ! -f ${FIND_MY_MOBILE_LITE_APK} ]]; then
  echo "[!] Missing FIND_MY_MOBILE_LITE_APK var or file not found"
  exit 1
fi

###########################################

############## KEYS #######################
KEYSTORE_KEY_PASSWORD=$(grep -oP '^keyPassword=\K.*' local.properties)
KEYSTORE_PASSWORD=$(grep -oP '^storePassword=\K.*' local.properties)
KEYSTORE_PATH=$(grep -oP '^storeFile=\K.*' local.properties)
KEYSTORE_ALIAS=$(grep -oP '^keyAlias=\K.*' local.properties)
###########################################
######### ANDROID SDK #####################
export ANDROID_HOME=$(grep -oP '^sdk\.dir=\K.*' local.properties)

API_LEVEL=$(grep -oP 'compileSdk\s*=\s*\K\d+' app/build.gradle.kt)
MIN_API_LEVEL=$( grep -oP 'minSdk\s*=\s*\K\d+' app/build.gradle.kt)
BUILD_TOOLS_VERSION=${API_LEVEL}.1.0
###########################################
#################### TOOLS #################
ZIPALIGN=${ANDROID_HOME}/build-tools/${BUILD_TOOLS_VERSION}/zipalign
APKSIGNER=${ANDROID_HOME}/build-tools/${BUILD_TOOLS_VERSION}/apksigner
AAPT2=${ANDROID_HOME}/build-tools/${BUILD_TOOLS_VERSION}/aapt2

APK_EDITOR_VERSION=1.4.5
APK_EDITOR=/tmp/APKEditor-${APK_EDITOR_VERSION}.jar
if [ ! -f ${APK_EDITOR} ]; then
  wget "https://github.com/REAndroid/APKEditor/releases/download/V${APK_EDITOR_VERSION}/APKEditor-${APK_EDITOR_VERSION}.jar" -O ${APK_EDITOR}
fi

###########################################
echo "[+] Build utag xposed-standalone"
chmod +x ./gradlew
./gradlew :xposed-core:clean
./gradlew :xposed-standalone:clean
./gradlew :xposed-standalone:build

XPOSED_STANDALONE_APK=${PWD}/xposed-standalone/build/outputs/apk/release/xposed-standalone-release-unsigned.apk

rm -rf ./build || true
mkdir -p build

pushd ./build
java -jar ${APK_EDITOR} d -dex -t xml -i ${SMART_THINGS_APK} -o ./smartthing

sed -i -e '/<permission[^>]*com.samsung.android.voc.beta.provider.GET_BETA_CONTACT_US_INFO[^>]*>/d' \
    -e 's/android:permission="com.samsung.android.voc.beta.provider.GET_BETA_CONTACT_US_INFO"//g' \
        ./smartthing/AndroidManifest.xml

echo "[+] Rebuild smartthing apk"
java -jar ${APK_EDITOR} b -t xml -i ./smartthing -o ./smartthing_out.apk
GENERATED_APK_SMARTTHING=${PWD}/$(ls ./*.apk)
echo "[+] Smartthing generated at: ${GENERATED_APK_SMARTTHING}"

${AAPT2} d xmltree  ${GENERATED_APK_SMARTTHING} --file AndroidManifest.xml

GENERATED_APK_SMARTTHING_ZIP=${GENERATED_APK_SMARTTHING}.aligned
GENERATED_APK_SMARTTHING_SIGN=${GENERATED_APK_SMARTTHING_ZIP}.signed.apk
# Zipalign
${ZIPALIGN} -v -p 4 ${GENERATED_APK_SMARTTHING} ${GENERATED_APK_SMARTTHING_ZIP}
  #--min-sdk-version ${MIN_API_LEVEL} \
${APKSIGNER} sign \
  --ks ${KEYSTORE_PATH} \
  --ks-key-alias ${KEYSTORE_ALIAS} \
  --ks-pass "pass:${KEYSTORE_PASSWORD}" \
  --key-pass "pass:${KEYSTORE_KEY_PASSWORD}" \
  --out ${GENERATED_APK_SMARTTHING_SIGN} \
  ${GENERATED_APK_SMARTTHING_ZIP}

LIBXPOSED_API=/tmp/api
if [ ! -d ${LIBXPOSED_API} ]; then
echo "[+] Build libxposed-api"
  # Change to correct version to use with LSPatch
  LIBXPOSED_API_COMMIT=50619e84ec879ed566bc9c5e09fbd0c9e8042781
  git clone https://github.com/libxposed/api ${LIBXPOSED_API}
  pushd ${LIBXPOSED_API}
  git reset --hard ${LIBXPOSED_API_COMMIT}
  chmod +x ./gradlew
  ./gradlew :api:publishToMavenLocal
  popd
fi

LIBXPOSED_SERVICE=/tmp/service
if [ ! -d ${LIBXPOSED_SERVICE} ]; then
  echo "[+] Build libxposed-service"
  git clone https://github.com/libxposed/service ${LIBXPOSED_SERVICE}
  pushd ${LIBXPOSED_SERVICE}
  chmod +x ./gradlew
  ./gradlew :service:publishToMavenLocal
  popd
fi

LSPATCH_REPO=/tmp/LSPatch
if [ ! -d ${LSPATCH_REPO} ]; then
  echo "[+] Build LSPATCH"
  git clone --recurse-submodules -j8 https://github.com/JingMatrix/LSPatch ${LSPATCH_REPO}
  pushd ${LSPATCH_REPO}
  chmod +x ./gradlew
  # Fix OOM
  echo "org.gradle.jvmargs=-Xms512M -Xmx4G" >> gradle.properties
  ./gradlew :buildRelease
  popd
fi

LSPATCH_JAR=$(ls ${LSPATCH_REPO}/out/release/jar-*-release.jar)
# -d debug
java -jar ${LSPATCH_JAR} -v --force -l 0 -k ${KEYSTORE_PATH} ${KEYSTORE_PASSWORD} mykey ${KEYSTORE_KEY_PASSWORD} -m ${XPOSED_STANDALONE_APK} ${GENERATED_APK_SMARTTHING_ZIP} ${GENERATED_APK_SMARTTHING_SIGN}
popd

SMART_THINGS_APK_MODDED=$(ls ./build/smartthing_out.apk.aligned.signed-*-lspatched.apk)

echo "[+] Generate UTAG app"

echo "[+] Extract fmm lib"
FMM_LIB_PATH_ARM64=app/src/main/jniLibs/arm64-v8a
FMM_LIB_PATH_ARM32=app/src/main/jniLibs/armeabi-v7a

unzip -jo "${FIND_MY_MOBILE_LITE_APK}" lib/armeabi-v7a/libfmm_ct.so -d "${FMM_LIB_PATH_ARM32}"
unzip -jo "${FIND_MY_MOBILE_LITE_APK}" lib/arm64-v8a/libfmm_ct.so -d "${FMM_LIB_PATH_ARM64}"

echo "[+] Build uTag"
./gradlew assembleDebug

# Quick easy test without rebuilding utag and custom google-api
#UTAG_APK=./inputapk/uTag-v1.0.11.apk
#UTAG_APK_RESIGN=./build/uTag-v1.0.11.resign.apk
#wget https://github.com/KieronQuinn/uTag/releases/download/1.0.11/uTag-v1.0.11.apk -O ./inputapk/uTag-v1.0.11.apk
#
#${APKSIGNER} sign \
#  --ks ${KEYSTORE_PATH} \
#  --ks-key-alias ${KEYSTORE_ALIAS} \
#  --ks-pass "pass:${KEYSTORE_PASSWORD}" \
#  --key-pass "pass:${KEYSTORE_KEY_PASSWORD}" \
#  --out ${UTAG_APK_RESIGN} \
#  ${UTAG_APK}

echo "[+] APKs generated uTag: ${UTAG_APK_RESIGN} and Smart Things modded: ${SMART_THINGS_APK_MODDED} "