# Finest
This repository contains the code for *Finesse* app in Android.

## Finesse Android App
Code for *Finesse* app in Android. The app tracks the feature-level usage of Instagram, Facebook, YouTube, and KakaoTalk. You can find the list of features available for tracking in the paper's appendix. 

### Before Installation
The app was deployed in September 2020. It is thus optimized for app versions at the time. For the best experience, please uninstall existing apps in your testing device and install the proper versions available [here](https://drive.google.com/drive/folders/1APL7Vs_Z77qmlHH1WFmf4lBkUW6lB_vl?usp=sharing).
Also, don't forget to set the device language to **Korean**.

### How to Install and Run
You can download the [apk file](https://drive.google.com/file/d/1qypsWPDp9RtsroMp5uWTq_M2XChOjQ8O/view) of the version used in the user study.
Or you can open the root directory in Android Studio and run.

For installation, follow the instructions [here](https://docs.google.com/presentation/d/1UrVYaGMdICW7NzQL_A4jBaZQ1s__N3LKTH3x5hVFex4/edit?usp=sharing).


# Key Components
## UIComponentService.kt
Path=`app/java/com.chocho.finest/UIComponentService`

Main service (`AccessibilityService()`) that receives the accessibility events and layout information in the background.

The service starts running when the accessibility setting is enabled.

- `onCreate()`: Set up Firebase uploader (raw data in zip files) 

- `onServiceConnected()`: Set up screen off receiver and alarm manager (to reset ESM conditions every 3 hours)

- `onAccessibilityEvent(...)`: Listener for every accessibility event (scroll, focus, click, etc.). Executes `handleLayoutUpdate(...)` in `LayoutLogger.kt` with the current package and class names.


## LayoutLogger.kt

- `handleLayoutUpdate(...)`: Called in `UIComponentService.kt`. Saves the tree-structured UI layout in the `lbs` format (defined by Donghwi). Calls `FeatureDetectorManager.instance.handleLayoutUpdate(...)` using the formatted element.

Other functions are supporter functions to translate the `AccessibilityNodeInfo` to our `lbs` structure for ease of use.

## features/FeatureDetectorManager.kt
- `handleLayoutUpdate(...)`: Detects the progress (start, end, during) of a session for target apps and handles feature tracking and ESM accordingly. Feature detection is handled a `FeatureDetector` for each target app. In `features/`, there are four `<target_app>FeatureDetector.kt`.

- `checkSessionEnd(...)`: Checks if a session ended, saves the last feature, and show the session summary (ESM) by calling `summarizeSession(...)`. 

- `summarizeSession(...)`: Shows the AlertDialog overlay containing the ESM webView (in file `<root>/app/assets/highchart.html`).


## features/<target_app>FeatureDetector.kt
The feature detector for each target app analyzes the layout information and determines which feature is currently in use.




