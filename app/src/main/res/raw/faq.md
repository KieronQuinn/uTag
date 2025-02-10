# What is uTag?
uTag is a modification for SmartThings and companion app to allow the use of Samsung Galaxy 
SmartTags on non-Samsung Android devices.

# Does uTag require root?
No, uTag can work without root by replacing the version of SmartThings on your device with a patched
version.

**Note:** The only exception to this is on Samsung devices running OneUI, where if you absolutely
need to use uTag instead of the official app, root is required due to the SmartThings Mod being 
incompatible with OneUI.

# Can I use uTag on a rooted device?
Yes, on a rooted device with LSPosed you can apply the required modifications directly to 
SmartThings without needing to install a patched version. The uTag modifications will also 
automatically disable SmartThings' root checks, so it will need to be removed from any denylists.

# How does uTag work?
uTag's modifications to SmartThings disable a number of software restrictions imposed by Samsung to
prevent the use of SmartTags on non-Samsung devices, and provides a companion app to replace the
SmartThings Find app which only works on Samsung OneUI.

# Which features work with uTag?
Most features supported by the official SmartThings Find app work with uTag. Below is a table 
detailing what is and isn't available in uTag.

| Feature                                    | Available | Notes                             |
|--------------------------------------------|-----------|-----------------------------------|
| Setting up a Tag out of the box            | Yes       | Unlocked in SmartThings           |
| Viewing Tag locations                      | Yes       |                                   |
| Viewing Tag details                        | Yes       |                                   |
| Updating nearby Tag locations              | Yes       |                                   |
| Encrypted locations                        | Yes       |                                   |
| Location history                           | Yes       |                                   |
| Ringing a Tag                              | Yes       |                                   |
| Setting a Tag's ringtone & volume          | Yes       | Unlocked in SmartThings           |
| Setting a Tag's button sound & volume      | Yes       | Unlocked in SmartThings           |
| Setting Power Save Mode                    | Yes       | Unlocked in SmartThings           |
| Lost Mode                                  | Yes       |                                   |
| Searching for nearby Tag                   | Yes       |                                   |
| Notifications when a Tag is left behind    | Yes       | Replaced with uTag implementation |           
| Safe Areas for left behind Tags            | Yes       | Replaced with uTag implementation |
| Notifications when a Tag is found          | Yes       | Unlocked in SmartThings           |
| Find my Device (ringing this device)       | Yes       | Replaced with uTag implementation |
| Sharing a Tag's location with home members | Yes       |                                   |
| Setting & changing encryption PIN          | Yes       |                                   | 
| Enabling/disabling location encryption     | Yes       |                                   |
| Contributing to Find my Everything network | Yes       |                                   |
| Background Scanning for Unknown Tags       | Yes       |                                   |
| Pet Walking Mode                           | No        | May be supported in the future    |

# What extra features does uTag provide?
uTag provides some extra features not found in the official SmartThings Find app:

| Feature                 | Description                                                                                 |
|-------------------------|---------------------------------------------------------------------------------------------|
| Widgets                 | Display Tag location and location history on a map in a widget                              |
| Custom Automations      | Open apps, shortcuts, or trigger Tasker tasks from pressing or long pressing a Tag's button |
| Offline Location        | View your Tag's last location & use Search Nearby when offline but near your Tag            |
| Wi-Fi Safe Areas        | Use a connected Wi-Fi network to mark an area as safe to leave Tags at                      |
| Passive Mode            | Disconnect Tags that you only need locations for and get more location updates              |
| Biometric Entry         | Lock access to the app behind a Biometric Prompt                                            |
| Custom Lost Mode URL    | Use your Tag like an NFC Tag and open any website when tapped                               |
| Export Location History | Save location history for a Tag to CSV                                                      |
| Smartspacer Support     | Display Tag information in At a Glance (requires Smartspacer)                               |

# How much battery does uTag use?
Very little. uTag keeps Bluetooth Low Energy scans at low power, and generally uses a negligible
(< 1%) amount of battery. Please note that if you have location refreshes set to a high frequency,
this may use more battery.

# A Tag won't connect even though it's nearby
There's a few things that could cause this:
- Make sure Bluetooth is enabled
- Check the Tag has sufficient battery, you can view it in Find to see what its last known level was
- Check both uTag and SmartThings are signed into the same Samsung account
- Try rebooting toggling Bluetooth off/on or rebooting your device

# Notifications when a Tag is found are not working
This sometimes happens if you're using SmartThings with Xposed (rooted). Clear the data of 
SmartThings, open it (the mod may take a moment to re-initialise), sign in and then reboot your 
device. This resets the notification token.