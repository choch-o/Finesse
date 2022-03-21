package com.chocho.finest.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.chocho.finest.ConstKt;
import com.chocho.finest.Feature;
import com.chocho.finest.lbs.Lbs;
import com.google.common.base.Strings;

import org.javatuples.Pair;

import java.util.ArrayList;

import static com.chocho.finest.util.CryptoUtil.hash;

public class YoutubeFeatureDetector extends FeatureDetector {
    enum YoutubeFeature {
        UNDEFINED,
        HOME,
        EXPLORE,
        SUBSCRIPTIONS,
        INBOX,
        LIBRARY,
        SEARCH,
        CHANNEL,
        VIDEO,
        COMMENT,
        PLAYLIST
    }

    private class TabStates {
        YoutubeFeature homeTab, exploreTab, subscribeTab, notificationTab, libraryTab;

        public TabStates() {
            this.homeTab = YoutubeFeature.UNDEFINED;
            this.exploreTab = YoutubeFeature.UNDEFINED;
            this.subscribeTab = YoutubeFeature.UNDEFINED;
            this.notificationTab = YoutubeFeature.UNDEFINED;
            this.libraryTab = YoutubeFeature.UNDEFINED;
        }

        public YoutubeFeature getIndex(int index) {
            switch (index) {
                case 1:
                    return this.homeTab;
                case 2:
                    return this.exploreTab;
                case 3:
                    return this.subscribeTab;
                case 4:
                    return this.notificationTab;
                case 5:
                    return this.libraryTab;
                default:
                    throw new IllegalStateException("Unexpected value: " + index);
            }
        }

        public void setDefault(int index) {
            switch (index) {
                case 1:
                    this.homeTab = YoutubeFeature.HOME;
                    break;
                case 2:
                    this.exploreTab = YoutubeFeature.EXPLORE;
                    break;
                case 3:
                    this.subscribeTab = YoutubeFeature.SUBSCRIPTIONS;
                    break;
                case 4:
                    this.notificationTab = YoutubeFeature.INBOX;
                    break;
                case 5:
                    this.libraryTab = YoutubeFeature.LIBRARY;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + index);
            }
        }

        public void setIndex(int index, YoutubeFeature feature) {
            switch (index) {
                case 1:
                    this.homeTab = feature;
                    break;
                case 2:
                    this.exploreTab = feature;
                    break;
                case 3:
                    this.subscribeTab = feature;
                    break;
                case 4:
                    this.notificationTab = feature;
                    break;
                case 5:
                    this.libraryTab = feature;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + index);
            }
        }

        @Override
        public String toString() {
            return "TabStates{" +
                    "homeTab=" + homeTab +
                    ", exploreTab=" + exploreTab +
                    ", subscribeTab=" + subscribeTab +
                    ", notificationTab=" + notificationTab +
                    ", libraryTab=" + libraryTab +
                    '}';
        }
    }

    YoutubeFeature lastFeatureName = YoutubeFeature.UNDEFINED;
    YoutubeFeature lastYoutubeFeature = YoutubeFeature.UNDEFINED;
    YoutubeFeature youtubeFeature = YoutubeFeature.UNDEFINED;
    Feature currFeature = null;
    TabStates tabStates = new TabStates();

    @Override
    public String detect(Context context, Lbs.RootLayoutElement root) {
        extractYoutubeFeature(root, context);

//        Log.d("YOUTUBE_FEAT", "Current Feature: " + youtubeFeature.toString());
//        Log.d("YOUTUBE_FEAT", "Last Feature: " + lastFeatureName.toString());
        if (lastFeatureName != youtubeFeature) {
            // End of last feature
            if (currFeature != null) {
//                Log.d("YOUTUBE_FEAT", "End of a feature");
                currFeature.endFeature(System.currentTimeMillis());
                SharedPreferences sharedPrefs = context.getSharedPreferences("feature-extraction", 0);
                String sessionData = sharedPrefs.getString("sessionData", "");
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("sessionData", sessionData + currFeature.toRow());
                editor.apply();
            }

//            Log.d("YOUTUBE_FEAT", "Start of a feature");
            // Start of a feature
            if (this.packageStart == null) this.packageStart = System.currentTimeMillis();
            currFeature = new Feature(youtubeFeature.toString(),
                    this.packageStart,
                    lastFeatureName.toString(), System.currentTimeMillis(),
                    ConstKt.getYOUTUBE(), context);
        }
        lastFeatureName = youtubeFeature;
        return youtubeFeature.toString();
    }

    @Override
    void onSessionStart() {
        super.onSessionStart();
        resetLastFeature();
    }

    @Override
    void onSessionEnd(Context context, Long lastTrackingPackageTime) {
        super.onSessionEnd(context, lastTrackingPackageTime);
        saveEndFeature(context, lastTrackingPackageTime);
    }

    public void extractYoutubeFeature(Lbs.RootLayoutElement root, Context context) {
        printAllChild(root);
//        Log.d("FEAT_EXT", "extract youtube feature");
//        Log.d("FEAT_EXT", "header text: "+getHeaderText(root));
        Pair<Integer, Lbs.LayoutElement> selected = new Pair<Integer, Lbs.LayoutElement>(0, null);
        for (Lbs.LayoutElement elm: root) {
            if (elm.getExtra().contains("isSelected:: true")) {
                String hashedDesc = elm.getExtraContent("contentDescription");
                if (hashedDesc.equals(hash("홈"))) {
//                    Log.d("FEAT_EXT", "selected tab: 홈");
                    selected = selected.setAt0(1);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("탐색"))) {
                    // TODO: how to detect dynamic content description
//                    Log.d("FEAT_EXT", "selected tab: 탐색");
                    selected = selected.setAt0(2);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("구독"))) {
//                    Log.d("FEAT_EXT", "selected tab: 구독");
                    selected = selected.setAt0(3);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("알림"))) {
//                    Log.d("FEAT_EXT", "selected tab: 알림");
                    selected = selected.setAt0(4);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("보관함"))) {
//                    Log.d("FEAT_EXT", "selected tab: 보관함");
                    selected = selected.setAt0(5);
                    selected = selected.setAt1(elm);
                }
            }
        }

        int selectedTab = selected.getValue0();
        if (selectedTab > 0) {
            lastYoutubeFeature = tabStates.getIndex(selectedTab);
            if (lastYoutubeFeature == YoutubeFeature.UNDEFINED) {
                tabStates.setDefault(selectedTab);
                lastYoutubeFeature = tabStates.getIndex(selectedTab);
            }
            if (getHeaderText(root).equals("MainHeader")) {
                tabStates.setDefault(selectedTab);
                youtubeFeature = tabStates.getIndex(selectedTab);
            }
            else
                youtubeFeature = analyzeLayout(root, context);
            lastYoutubeFeature = youtubeFeature;
            tabStates.setIndex(selectedTab, youtubeFeature);
        } else {
//            Log.d("FEAT_EXT", "No Tab");
//            Log.d("FEAT_EXT", "last state: " + lastYoutubeFeature.toString());
            youtubeFeature = analyzeLayout(root, context);
            lastYoutubeFeature = youtubeFeature;
        }
//        Log.d("FEAT_EXT", "Current State: " + youtubeFeature.toString());
//        Log.d("FEAT_EXT", tabStates.toString());

//        if (youtubeFeature == YoutubeFeature.UNDEFINED)
//            youtubeFeature = lastYoutubeFeature;
    }

    private String getHeaderText(Lbs.RootLayoutElement root) {
        for (Lbs.LayoutElement elm: root)
            if (elm.getExtraContent("viewIdResourceName").equals("com.google.android.youtube:id/toolbar")) {
                if (!elm.getChildren().get(0).getExtraContent("contentDescription").equals("위로 이동"))
                    return "MainHeader";
                for (Lbs.LayoutElement content: _getAllChild(elm)) {
                    if (content.getExtraContent("text") != null && !content.getExtraContent("text").equals("") && !content.getExtraContent("text").equals(hash(""))) {
                        String headertext = content.getExtraContent("text");
                        if (headertext.equals(hash("게시물")))
                            return "게시물";
                        if (headertext.equals(hash("계정")) || headertext.equals(hash("시청 시간")) || headertext.equals(hash("기록")) || headertext.equals(hash("구입한 동영상")) || headertext.equals(hash("나중에 볼 동영상")))
                            return "profile";
                    }
                }
                return "Undefined";
            }
        return "";
    }

    private YoutubeFeature analyzeLayout(Lbs.RootLayoutElement root, Context context) {
        /*Check size*/
        if (root.getBounds().top != 0 || root.getBounds().left != 0)
            return lastYoutubeFeature;

        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);

        for (Lbs.LayoutElement elm: root) {
            /*Check video*/
            if (elm.getExtraContent("viewIdResourceName").equals("com.google.android.youtube:id/watch_player")) {
                for (Lbs.LayoutElement comment: root)
                    if (comment.getExtraContent("viewIdResourceName").equals("com.google.android.youtube:id/engagement_panel_below_the_player"))
                        return YoutubeFeature.COMMENT;
                return YoutubeFeature.VIDEO;
            }
            /*Check channel*/
            if (elm.getExtraContent("viewIdResourceName").equals("com.google.android.youtube:id/tabs_bar"))
                return YoutubeFeature.CHANNEL;
            /*Check search bar*/
            if (elm.getBounds().top == statusBarHeight && elm.getExtraContent("viewIdResourceName").equals("com.google.android.youtube:id/search_box"))
                return YoutubeFeature.SEARCH;
            /*Check playlist*/
            if (elm.getExtraContent("viewIdResourceName").equals("com.google.android.youtube:id/playlist_video_item"))
                return YoutubeFeature.PLAYLIST;
//            if (elm.getExtraContent("viewIdResourceName").equals("com.google.android.youtube:id/results"))
//                return YoutubeFeature.SEARCH;
        }

        switch (getHeaderText(root)){
            case "게시물":
                return YoutubeFeature.CHANNEL;
            case "profile":
                return YoutubeFeature.LIBRARY;
        }

        return YoutubeFeature.UNDEFINED;
    }

    public static void _printAllChild(Lbs.LayoutElement elm, int depth) {
        String hashedContent =  elm.getExtraContent("contentDescription");
        String hashedContent2 =  elm.getExtraContent("text");
//        Log.d("Print", Strings.repeat("- ", depth) + elm.getBounds().toString() + "\t" + hashedContent + "\t\t" + hashedContent2 + "\t" + elm.getExtraContent("viewIdResourceName"));
        if (elm.getChildren().size() > 0){
            for (Lbs.LayoutElement child: elm.getChildren()) {
                _printAllChild(child, depth + 1);
            }
        }
    }

    public static void printAllChild(Lbs.RootLayoutElement root) {
        for (Lbs.LayoutElement elm: root.getChildren()) {
            String hashedContent =  elm.getExtraContent("contentDescription");
            String hashedContent2 =  elm.getExtraContent("text");
//            Log.d("Print", Strings.repeat("- ", 1) + elm.getBounds().toString() + "\t" + hashedContent + "\t" + hashedContent2 + "\t" + elm.getExtraContent("viewIdResourceName"));
            _printAllChild(elm, 1);
        }
//        Log.d("Print", Strings.repeat("-", 100));
    }

    public ArrayList<Lbs.LayoutElement> _getAllChild(Lbs.LayoutElement elm) {
        ArrayList<Lbs.LayoutElement> allChild = new ArrayList<>();
        allChild.add(elm);
        if (elm.getChildren().size() != 0) {
            for (Lbs.LayoutElement child : elm.getChildren()) {
                allChild.addAll(_getAllChild(child));
            }
        }
        return allChild;
    }

    public void resetLastFeature() {
        lastYoutubeFeature = YoutubeFeature.UNDEFINED;
        tabStates = new TabStates();
        lastFeatureName = YoutubeFeature.UNDEFINED;
    }

    public void saveEndFeature(Context context, Long lastTrackingPackageTime) {
        if (currFeature != null) {
            currFeature.endFeature(lastTrackingPackageTime);
            SharedPreferences sharedPrefs = context.getSharedPreferences("feature-extraction", 0);
            String sessionData = sharedPrefs.getString("sessionData", "");
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString("sessionData", sessionData + currFeature.toRow());
            editor.apply();

            currFeature = null;
        }
    }

}
