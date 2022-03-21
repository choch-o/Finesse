package com.chocho.finest.features;

import android.content.Context;

import com.chocho.finest.lbs.Lbs;

public abstract class FeatureDetector {
    /* package */ Long packageStart = null;
    /* package */ abstract String detect(Context context, Lbs.RootLayoutElement root);
    /* package */ void onSessionStart() {
        packageStart = System.currentTimeMillis();
    }
    /* package */ void onSessionEnd(Context context, Long lastTrackingPackageTime) {

    }
}
