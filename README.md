[![uTag Banner](https://i.imgur.com/g1jJpKOl.png)](https://i.imgur.com/g1jJpKO.png)

uTag is an Android app and mod for Samsung SmartThings which allows the use of Samsung SmartTags on non-Samsung Android devices running Android 11 or above. No root, no Shizuku needed.

[![Crowdin](https://badges.crowdin.net/utag/localized.svg)](https://crowdin.com/project/utag)

## Features

uTag allows the use of *almost* all SmartTag features. This includes setting up, connecting to and tracking Tags, ringing Tags, location history, precise finding with UWB (requires a UWB-capable device), left behind alerts and much more. In addition, uTag includes some features which Samsung's app does not have, such as widgets, safe areas for leaving Tags behind based on Wi-Fi networks and exporting location history. You can find a larger list of available features in the [Frequently Asked Questions](https://github.com/KieronQuinn/uTag/blob/main/app/src/main/res/raw/faq.md), as well as a longer introduction to uTag on [Medium](https://medium.com/@KieronQuinn/utag-use-samsung-smarttags-on-any-android-device-01bd71d2a12b).

## Why Samsung's Tags?

Great question, this is answered both in the [Medium article](https://medium.com/@KieronQuinn/utag-use-samsung-smarttags-on-any-android-device-01bd71d2a12b) about uTag, and with a [longer piece](https://medium.com/@KieronQuinn/android-item-tracking-a-tale-of-two-networks-954eb42daf30) about Samsung's network vs Google's.

## Download & Installation

Installing uTag is very simple. Just download the latest release APK from the 
[releases](https://github.com/KieronQuinn/uTag/releases) page, install it and follow the instructions for setup. uTag will download the latest SmartThings mod, install it and guide you through setup. You will be required to sign into your Samsung Account, which is required to access
Samsung APIs.

## Frequently Asked Questions
FAQs can be found [here](https://github.com/KieronQuinn/uTag/blob/main/app/src/main/res/raw/faq.md). They are also available in the app, from the uTag settings.

## Content Creator Mode

If you're a content creator, and you want to showcase uTag with images or video, you should enable Content Creator Mode in the Advanced settings after setup. This option uses a pre-defined set of fake locations (shown in the screenshots below), while allowing otherwise normal use of the Tags. Locations sent to the network are not impacted.

## Screenshots

![Screenshots of uTag showing tracking a Tag, location history and finding a Tag](https://i.imgur.com/RnNH4pI.png)

## Discord

Join the uTag Discord server for any further queries

[![uTag Discord](https://i.imgur.com/eeklYon.png)](https://discord.gg/acp5aM7FpA)

## Building

> **Note:** This section is only for people who wish to compile uTag from source for themselves. Most people should follow the instructions above to download and install the APK.

If you want to compile uTag yourself, some additional setup is required:

1. Clone the repository as normal.
2. Download [this APK](https://www.apkmirror.com/apk/samsung-electronics-co-ltd/find-my-mobile/find-my-mobile-7-3-18-2-release/samsung-find-my-mobile-lite-7-3-18-2-android-apk-download/) of Samsung Find my Mobile Lite.

> **Note:** this specific version is required since it's the last version to support both armv7 and v8. This library has remained unchanged since the beginning so there's no reason to "update" it.

4. Extract the `libfmm_ct.so` shared libraries and place them in their respective folders in `app/src/main/jniLibs`. This is required to support sending locations to the Samsung Find my Everything network.
5. Open the auto-generated `local.properties` file and set it up as follows:

```properties
storeFile=<path to keystore>
storePassword=<keystore password>
keyAlias=<key alias>
keyPassword=<key password>
```

6. Create a Google Maps API key on the [Google Cloud console](https://console.cloud.google.com),
   then create a `secrets.properties` file and set it up as follows:

```properties
MAPS_API_KEY=<Maps API key>
```

> **Note:** Your API key should have certificate configurations for both uTag (`com.kieronquinn.app.utag`) **and** SmartThings (`com.samsung.android.oneconnect`), both using your signature fingerprint.

7. Run the `app` configuration. **If your device is rooted and using LSPosed, this is all you need to do, install SmartThings from Google Play, enable uTag in LSPosed and finish setup as normal.**
8. **If your device is not rooted**, create an APK of the `xposed-standalone` configuration, and push it to your device.
9. Install the latest version of [LSPatch](https://github.com/JingMatrix/LSPatch).
10. Download the latest APK of [SmartThings](https://www.apkmirror.com/apk/samsung-electronics-co-ltd/samsung-smartthings-samsung-connect/).

> **Note:** It is vitally important that you download the APK and do not use the version installed from Google Play, since Google Play uses split APKs which are not supported by LSPatch.

11. Use a hex editor to replace `AIzaSyBw69qJybnXaPq-md6-4gAmBfX5XdKuhd8` in the SmartThings APK with your Maps API key (this will allow Google Maps to work in SmartThings).
12. Open LSPatch, go to the settings and set up your custom keystore. If you need to convert it to BKS format, you can use [KeyStore Explorer](https://keystore-explorer.org/).
13. On the "Manage" tab of LSPatch, select the + button and then pick the SmartThings APK from storage.
14. Select the "Integrated" patch mode, and then "Embed modules". 
15. Pick the `xposed-standalone` APK you pushed earlier, and then "Start Patch".
16. Once patching is complete, find the patched SmartThings APK in `/sdcard/LSPatch` and install it.

> **Note:** If you were previously running an unpatched version of SmartThings, you will need to uninstall it first.

17. Open the patched SmartThings. It may take a short while to setup, once completed you can sign in, then continue to use uTag as normal, it will detect the patched SmartThings automatically and skip trying to download the precompiled version.

> **Note:** When SmartThings or uTag is updated, you will need to repeat steps 8 to 17. 

## Documentation

If you're a developer wanting to interact with the SmartThings and Find APIs in the same way that uTag does, or interested in how **some** of the Bluetooth interactions work, check out the [uTag Wiki](https://github.com/KieronQuinn/uTag/wiki).
