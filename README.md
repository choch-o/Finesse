# Finesse: Social Media App Feature Tracking Tool
This repository contains the code for *Finesse* app in Android. The app tracks the feature-level usage of Instagram, Facebook, YouTube, and KakaoTalk. This app was first used and introduced in the paper, [*Reflect, Not Regret: Understanding Regretful Smartphone Use with App Feature-Level Analysis*](https://dl.acm.org/doi/abs/10.1145/3479600) (CSCW 2021).

You can find the list of features available for tracking in the paper's appendix. 

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


# How to Cite (BibTex)
```
@article{Cho2021Reflect,
author = {Cho, Hyunsung and Choi, DaEun and Kim, Donghwi and Kang, Wan Ju and Choe, Eun Kyoung and Lee, Sung-Ju},
title = {Reflect, Not Regret: Understanding Regretful Smartphone Use with App Feature-Level Analysis},
year = {2021},
issue_date = {October 2021},
publisher = {Association for Computing Machinery},
address = {New York, NY, USA},
volume = {5},
number = {CSCW2},
url = {https://doi.org/10.1145/3479600},
doi = {10.1145/3479600},
abstract = {Digital intervention tools against problematic smartphone usage help users control their consumption on smartphones, for example, by setting a time limit on an app. However, today's social media apps offer a mix of quasiessential and addictive features in an app (e.g., Instagram has following feeds, recommended feeds, stories, and direct messaging features), which makes it hard to apply a uniform logic for all uses of an app without a nuanced understanding of feature-level usage behaviors. We study when and why people regret using different features of social media apps on smartphones. We examine regretful feature uses in four smartphone social media apps (Facebook, Instagram, YouTube, and KakaoTalk) by utilizing feature usage logs, ESM surveys on regretful use collected for a week, and retrospective interviews from 29 Android users. In determining whether a feature use is regretful, users considered different types of rewards they obtained from using a certain feature (i.e., social, informational, personal interests, and entertainment) as well as alternative rewards they could have gained had they not used the smartphone (e.g., productivity). Depending on the types of rewards and the way rewards are presented to users, probabilities to regret vary across features of the same app. We highlight three patterns of features with different characteristics that lead to regretful use. First, "following"-based features (e.g., Facebook's News Feed and Instagram's Following Posts and Stories) induce habitual checking and quickly deplete rewards from app use. Second, recommendation-based features situated close to actively used features (e.g., Instagram's Suggested Posts adjacent to Search) cause habitual feature tour and sidetracking from the original intention of app use. Third, recommendation-based features with bite-sized contents (e.g., Facebook's Watch Videos) induce using "just a bit more," making people fall into prolonged use. We discuss implications of our findings for how social media apps and intervention tools can be designed to reduce regretful use and how feature-level usage information can strengthen self-reflection and behavior changes.},
journal = {Proc. ACM Hum.-Comput. Interact.},
month = {oct},
articleno = {456},
numpages = {36},
keywords = {regret, smartphone use, digital wellbeing, app feature, social media}
}
```

