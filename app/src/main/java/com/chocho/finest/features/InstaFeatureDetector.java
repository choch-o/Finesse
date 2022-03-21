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

public class InstaFeatureDetector extends FeatureDetector {
    enum InstaFeature {
        UNDEFINED,
        FOLLOWING_FEED,
        RANDOM_FEED,
        SEARCH,
        NOTIFICATION,
        MY_POST,
        OTHER_POST,
        DM,
        STORY,
        UPLOAD_POST,
        UPLOAD_STORY,
        WEB,
        HASHTAG,
        VIDEO
    }

    enum InstaView {
        NONE,
        FEED,
        PROFILE,
        HASHTAG,
        NOTIFICATION,
        POST,
        COMMENT,
        RANDOMFEEDHOME,
        SEARCH,
        STORY,
        DM,
        UPLOAD_STORY,
        WEB,
        UPLOAD_POST,
        VIDEO
    }

    enum InstaContent {
        NONE,
        FOLLOWING,
        RANDOM,
        HASHTAG,
        NOTIFICATION,
        MY,
        OTHER,
        STORY,
        SEARCH
    }

    private class InstaState {
        InstaView view;
        InstaContent content;

        public InstaState() {
            this.view = InstaView.NONE;
            this.content = InstaContent.NONE;
        }

        public InstaState(InstaView view, InstaContent content) {
            this.view = view;
            this.content = content;
        }

        public void set(InstaView view, InstaContent content) {
            this.view = InstaView.values()[view.ordinal()];
            this.content = InstaContent.values()[content.ordinal()];
        }

        public void set(InstaState instaState) {
            this.view = InstaView.values()[instaState.view.ordinal()];
            this.content = InstaContent.values()[instaState.content.ordinal()];
        }

        public boolean isNone() {
            return this.view == InstaView.NONE && this.content == InstaContent.NONE;
        }

        @Override
        public String toString() {
            return "{" +
                    "view=" + view +
                    ", content=" + content +
                    '}';
        }
    }

    private class TabStates {
        InstaState homeTab, searchTab, notificationTab, profileTab;

        public TabStates() {
            this.homeTab = new InstaState();
            this.searchTab = new InstaState();
            this.notificationTab = new InstaState();
            this.profileTab = new InstaState();
        }

        public InstaState getIndex(int index) {
            switch (index) {
                case 1:
                    return this.homeTab;
                case 2:
                    return this.searchTab;
                case 3:
                    return this.notificationTab;
                case 4:
                    return this.profileTab;
                default:
                    throw new IllegalStateException("Unexpected value: " + index);
            }
        }

        public void setDefault(int index) {
            switch (index) {
                case 1:
                    this.homeTab.set(InstaView.FEED, InstaContent.FOLLOWING);
                    break;
                case 2:
                    this.searchTab.set(InstaView.RANDOMFEEDHOME, InstaContent.RANDOM);
                    break;
                case 3:
                    this.notificationTab.set(InstaView.NOTIFICATION, InstaContent.NOTIFICATION);
                    break;
                case 4:
                    this.profileTab.set(InstaView.PROFILE, InstaContent.MY);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + index);
            }
        }

        public void setIndex(int index, InstaState state) {
            switch (index) {
                case 1:
                    this.homeTab.set(state.view, state.content);
                    break;
                case 2:
                    this.searchTab.set(state.view, state.content);
                    break;
                case 3:
                    this.notificationTab.set(state.view, state.content);
                    break;
                case 4:
                    this.profileTab.set(state.view, state.content);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + index);
            }
        }

        @Override
        public String toString() {
            return "TabStates{" +
                    "homeTab=" + homeTab +
                    "\t searchTab=" + searchTab +
                    "\t notificationTab=" + notificationTab +
                    "\t profileTab=" + profileTab +
                    '}';
        }
    }

    InstaFeature lastFeatureName = InstaFeature.UNDEFINED;
    Feature currFeature = null;

    @Override
    public String detect(Context context, Lbs.RootLayoutElement root) {
        extractInstagramFeature(root, context);
        InstaFeature currFeatureName = stateToFeature(currentState);
//        Log.d("INSTA_FEAT", "Current Feature: " + currFeatureName.toString());
//        Log.d("INSTA_FEAT", "Last Feature: " + lastFeatureName.toString());
//        if (currFeatureName != InstaFeature.UNDEFINED) {
        if (lastFeatureName != currFeatureName) {

            // End of last feature
            if (currFeature != null) {
//                Log.d("INSTA_FEAT", "End of a feature");
                currFeature.endFeature(System.currentTimeMillis());
                SharedPreferences sharedPrefs = context.getSharedPreferences("feature-extraction", 0);
                String sessionData = sharedPrefs.getString("sessionData", "");
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("sessionData", sessionData + currFeature.toRow());
                editor.apply();
                // TODO: handle the sessionData or End the last feature when different package is detected
            }

//            Log.d("INSTA_FEAT", "Start of a feature");
            // Start of a feature
            if (this.packageStart == null) this.packageStart = System.currentTimeMillis();
            currFeature = new Feature(stateToFeature(currentState).toString(),
                    this.packageStart,
                    stateToFeature(lastState).toString(), System.currentTimeMillis(),
                    ConstKt.getINSTAGRAM(), context);
        }
        lastFeatureName = currFeatureName;
//        }
        return stateToFeature(currentState).toString();
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

    TabStates tabStates = new TabStates();
    InstaState lastState = new InstaState();
    InstaState currentState = new InstaState();

    public void extractInstagramFeature(Lbs.RootLayoutElement root, Context context) {
//        Log.d("FEAT_EXT", "extract instagram feature");
        Pair<Integer, Lbs.LayoutElement> selected = new Pair<Integer, Lbs.LayoutElement>(0, null);
        for (Lbs.LayoutElement elm : root) {
            // 200726 dhkim: XXX: Is this necessary to visit children while DFS?
//            for (Lbs.LayoutElement child: elm.getChildren()) {
            if (elm.getExtra().contains("isSelected:: true")) {
                String hashedDesc = elm.getExtraContent("contentDescription");
                if (hashedDesc.equals(hash("홈"))) {
//                    Log.d("FEAT_EXT", "selected tab: 홈");
                    selected = selected.setAt0(1);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("검색 및 탐색하기"))) {
//                    Log.d("FEAT_EXT", "selected tab: 검색 및 탐색하기");
                    selected = selected.setAt0(2);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("활동"))) {
//                    Log.d("FEAT_EXT", "selected tab: 활동");
                    selected = selected.setAt0(3);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("프로필"))) {
//                    Log.d("FEAT_EXT", "selected tab: 프로필");
                    selected = selected.setAt0(4);
                    selected = selected.setAt1(elm);
                }
            }
//            }
        }

        printAllChild(root);

        int selectedTab = selected.getValue0();
        if (selectedTab > 0) {
            /*Upload Story*/
            if (selected.getValue0() == 1 && selected.getValue1().getBounds().left > root.getBounds().width() * 0.5) {
                currentState.set(InstaView.UPLOAD_STORY, InstaContent.NONE);
            } else {
                lastState = tabStates.getIndex(selectedTab);
                if (lastState.isNone()) {
                    tabStates.setDefault(selectedTab);
                    lastState = tabStates.getIndex(selectedTab);
                }
                currentState.set(getInstaState(root, lastState));
                lastState.set(currentState);
                tabStates.setIndex(selectedTab, currentState);
            }
        } else {
//            Log.d("FEAT_EXT", "No Tab");
//            Log.d("FEAT_EXT", "last state: " + lastState.toString());
            if (lastState.isNone())     // Start of the application without tab --> notification
                currentState.set(analyze_layout(root), InstaContent.NOTIFICATION);
            else
                currentState.set(getInstaState(root, lastState));
            lastState.set(currentState.view, currentState.content);
        }
//        Log.d("FEAT_EXT", "Current State: " + currentState.toString());
//        Log.d("FEAT_EXT", tabStates.toString());
    }

    private InstaState getInstaState(Lbs.RootLayoutElement root, InstaState lastState) {
        InstaView currentView = analyze_layout(root);
        InstaContent currentContent = InstaContent.NONE;
        switch (currentView) {
            case NONE:
                currentView = InstaView.NONE;
                currentContent = lastState.content;
                break;
            case FEED:
                String headertext = getHeaderText(getHeader(root));
                currentContent = lastState.content;
                if (headertext.equals("탐색 탭"))
                    currentContent = InstaContent.RANDOM;
                if (headertext.equals("이전 게시물"))
                    currentContent = InstaContent.FOLLOWING;
                if (headertext.equals("MainHeader")) {
                    currentContent = InstaContent.NONE;
                    for (Lbs.LayoutElement elm: root){
                        if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/icon_wrapper"))
                            if (elm.getChildren().size() > 0) {
                                if (elm.getChildren().get(0).getExtraContent("text").equals(hash("추천 게시물")) && elm.getBounds().top < root.getBounds().height() * 0.5) {
                                    currentContent = InstaContent.RANDOM;
                                    break;
                                }
                            }
                    }
                    if (currentContent == InstaContent.NONE)
                        currentContent = InstaContent.FOLLOWING;
                }
                if (headertext.equals("Hashtag"))
                    currentContent = InstaContent.HASHTAG;
                break;
            case PROFILE:
                currentContent = InstaContent.OTHER;
                for (Lbs.LayoutElement child : root)
                    if (child.getExtraContent("text").equals(hash("프로필 수정")) || child.getExtraContent("text").equals(hash("프로필 편집")))
                        currentContent = InstaContent.MY;
                break;
            case HASHTAG:
                currentContent = InstaContent.HASHTAG;
                break;
            case NOTIFICATION:
                currentContent = InstaContent.NOTIFICATION;
                break;
            case POST:
            case COMMENT:
                currentContent = lastState.content;
                if (lastState.content == InstaContent.STORY)
                    currentContent = InstaContent.OTHER;
                break;
            case RANDOMFEEDHOME:
                currentContent = InstaContent.RANDOM;
                break;
            case SEARCH:
                currentContent = InstaContent.SEARCH;
                break;
            case STORY:
                currentContent = InstaContent.STORY;
                break;
        }
        return new InstaState(currentView, currentContent);
    }

    private InstaFeature stateToFeature(InstaState instaState) {
        if (instaState == null)
            return InstaFeature.UNDEFINED;
        if (instaState.view == InstaView.NONE && instaState.content == InstaContent.HASHTAG)
            return InstaFeature.HASHTAG;
        else if (instaState.view == InstaView.NONE && instaState.content == InstaContent.OTHER)
            return InstaFeature.OTHER_POST;
        if (instaState.view == InstaView.NONE)
            return InstaFeature.UNDEFINED;
        if (instaState.view == InstaView.VIDEO)
            return InstaFeature.VIDEO;
        else {
            switch (instaState.content) {
                case NONE:
                    if (instaState.view == InstaView.DM)
                        return InstaFeature.DM;
                    else if (instaState.view == InstaView.STORY)
                        return InstaFeature.STORY;
                    else if (instaState.view == InstaView.UPLOAD_STORY)
                        return InstaFeature.UPLOAD_STORY;
                    else if (instaState.view == InstaView.UPLOAD_POST)
                        return InstaFeature.UPLOAD_POST;
                    else if (instaState.view == InstaView.WEB)
                        return InstaFeature.WEB;
                    break;
                case FOLLOWING:
                    return InstaFeature.FOLLOWING_FEED;
                case RANDOM:
                    return InstaFeature.RANDOM_FEED;
                case HASHTAG:
                    return InstaFeature.HASHTAG;
                case NOTIFICATION:
                    return InstaFeature.NOTIFICATION;
                case MY:
                    return InstaFeature.MY_POST;
                case OTHER:
                    return InstaFeature.OTHER_POST;
                case SEARCH:
                    return InstaFeature.SEARCH;
                case STORY:
                    return InstaFeature.STORY;
            }
        }
        return InstaFeature.UNDEFINED;
    }

    private InstaView analyze_layout(Lbs.RootLayoutElement root) {
        /*check web*/
        String className = root.getExtraContent("className");
        if (className.equals("android.webkit.WebView") || className.equals("com.facebook.browser.lite.BrowserLiteInMainProcessBottomSheetActivity")) {
            return InstaView.WEB;
        }

        /*check profile and hashtag layout*/
        for (Lbs.LayoutElement tab1 : root) {
            if (tab1.getExtraContent("contentDescription").equals(hash("그리드 보기")))
                for (Lbs.LayoutElement tab2 : _getAllChild(tab1.getParent().getParent().getParent()))
                    if (tab2.getExtraContent("contentDescription").equals(hash("회원님이 나온 사진")))
                        return InstaView.PROFILE;
            if (tab1.getExtraContent("text").equals(hash("인기 게시물")) || tab1.getExtraContent("text").equals(hash("인기")))
                for (Lbs.LayoutElement tab2 : _getAllChild(tab1.getParent().getParent().getParent()))
                    if (tab2.getExtraContent("text").equals(hash("최근 게시물")))
                        return InstaView.HASHTAG;
        }

        /*check DM layout*/
        for (Lbs.LayoutElement elm : root) {
            if (elm.getBounds().left < root.getBounds().right) {
                if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/row_inbox_container") || elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/message_thread_container"))
                    return InstaView.DM;
                if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/view_pager"))
                    return InstaView.STORY;
                if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/profile_header_container")) {
                    if (elm.getChildren().size() >= 1) {
                        return InstaView.PROFILE;
                    }
                }
                if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/media_tab_bar"))
                    return InstaView.UPLOAD_POST;
                if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/quick_capture_root_container"))
                    return InstaView.UPLOAD_STORY;
                if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/video_container") || elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/video_states"))
                    return InstaView.VIDEO;
                if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/browser_lite_root_container"))
                    return InstaView.WEB;
            }
        }

        /*check randomfeedhome layout*/
        ArrayList<Lbs.LayoutElement> allChild = getAllChild(root);
        String[] targetTabContents = {"IGTV", "Shop", "장식", "여행", "건축"};
        for (int i = allChild.size() - 1; i >= 0; i--) {
            Lbs.LayoutElement child = allChild.get(i);
            if (child.getExtraContent("contentDescription").equals(hash("네임 태그"))) {
                ArrayList<Lbs.LayoutElement> allTabChild = _getAllChild(allChild.get(i + 1));
                ArrayList<String> tabContents = new ArrayList<>();
                for (Lbs.LayoutElement tabChild : allTabChild) {
                    tabContents.add(tabChild.getExtraContent("text"));
                }
                int result = 0;
                for (int j = 0; j < targetTabContents.length; j++) {
                    if (tabContents.contains(hash(targetTabContents[j])))
                        result += 1;
                }
                if (result > 3) return InstaView.RANDOMFEEDHOME;
                break;
            }
        }

        /*check search layout*/
        targetTabContents = new String[]{"인기", "계정", "태그", "장소"};
        for (int i = 0; i < allChild.size() - 3; i++) {
            boolean result = true;
            for (int j = 0; j < targetTabContents.length; j++) {
                if (!allChild.get(i + j * 2).getExtraContent("text").equals(hash(targetTabContents[j]))) {
                    result = false;
                    break;
                }
            }
            if (result) return InstaView.SEARCH;
        }

        /*check notification, comment layout*/
        Lbs.LayoutElement header = getHeader(root);
        if (header != null) {
            String headerText = getHeaderText(header);
//            Log.d("FEAT_EXT", "detected header text: " + headerText);
            switch (headerText) {
                case "notification":
                    return InstaView.NOTIFICATION;
                case "댓글":
                    return InstaView.COMMENT;
                case "RandomFeedHeader":
                    return InstaView.RANDOMFEEDHOME;
                case "게시물":
                    return InstaView.POST;
                case "UploadPost":
                    return InstaView.UPLOAD_POST;
                case "Hashtag":
                case "탐색 탭":
                case "이전 게시물":
                case "MainHeader":
                    return InstaView.FEED;
                case "IGTV":
                    return InstaView.VIDEO;
                case "받는 사람":
                    return InstaView.DM;
                default:
            }
        }

        return InstaView.NONE;
    }

    private Lbs.LayoutElement getHeader(Lbs.RootLayoutElement root) {
        for (Lbs.LayoutElement elm : root) {
            if (elm.getBounds().left < root.getBounds().right && (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/action_bar_container") || elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/media_edit_action_bar_wrapper")))
                return elm;
            if (elm.getExtraContent("viewIdResourceName").equals("com.instagram.android:id/action_bar_shadow"))
                return elm.getParent();
        }
        return null;
    }

    private String getHeaderText(Lbs.LayoutElement header) {
//        _printAllChild(header, 1);
        ArrayList<Lbs.LayoutElement> allElem = _getAllChild(header);
        ArrayList<String> texts = new ArrayList<>();
        for (Lbs.LayoutElement elem : allElem) {
            if (!elem.getExtraContent("text").equals("") && !elem.getExtraContent("text").equals(hash("")))
                texts.add(elem.getExtraContent("text"));
            if (!elem.getExtraContent("contentDescription").equals("") && !elem.getExtraContent("contentDescription").equals(hash("")))
                texts.add(elem.getExtraContent("contentDescription"));
        }
        if (texts.contains(hash("활동")) || texts.contains(hash("팔로우 요청")))
            return "notification";
        if (texts.contains(hash("탐색 탭")))
            return "탐색 탭";
        if (texts.contains(hash("댓글")))
            return "댓글";
        if (texts.contains(hash("검색")))
            return "RandomFeedHeader";
        if (texts.contains(hash("게시물")) || texts.contains(hash("사진")) || texts.contains(hash("동영상")) || texts.contains(hash("태그됨")))
            return "게시물";
        if (texts.contains(hash("인기 게시물")) || texts.contains(hash("최신글")))
            return "Hashtag";
        if (texts.contains(hash("새 게시물")))
            return "UploadPost";
        if (texts.contains(hash("IGTV")))
            return "IGTV";
        if (texts.contains(hash("이전 게시물")))
            return "이전 게시물";
        if (texts.contains(hash("받는 사람")))
            return "DM";
        if (texts.contains(hash("카메라")) && texts.contains(hash("위쪽으로 스크롤")))
            return "MainHeader";
        return "";
    }

    public static void _printAllChild(Lbs.LayoutElement elm, int depth) {
        String hashedContent = elm.getExtraContent("contentDescription");
        String hashedContent2 = elm.getExtraContent("text");
//        Log.d("Print", Strings.repeat("- ", depth) + elm.getBounds().toString() + "\t" + hashedContent + "\t\t" + hashedContent2 + "\t" + elm.getExtraContent("className") + "\t" + elm.getExtraContent("viewIdResourceName"));
        if (elm.getChildren().size() > 0) {
            for (Lbs.LayoutElement child : elm.getChildren()) {
                _printAllChild(child, depth + 1);
            }
        }
    }

    public static void printAllChild(Lbs.RootLayoutElement root) {
        for (Lbs.LayoutElement elm : root.getChildren()) {
            String hashedContent = elm.getExtraContent("contentDescription");
            String hashedContent2 = elm.getExtraContent("text");
//            Log.d("Print", Strings.repeat("- ", 1) + elm.getBounds().toString() + "\t/" + hashedContent + "\t/" + hashedContent2 + "\t" + elm.getExtraContent("className") + "\t" + elm.getExtraContent("viewIdResourceName"));
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

    public ArrayList<Lbs.LayoutElement> getAllChild(Lbs.RootLayoutElement root) {
        ArrayList<Lbs.LayoutElement> allChild = new ArrayList<>();
        for (Lbs.LayoutElement elm : root) {
            allChild.add(elm);
        }
        return allChild;
    }

    public void resetLastFeature() {
        lastFeatureName = InstaFeature.UNDEFINED;
        tabStates = new TabStates();
        lastState = new InstaState();
    }

    public void saveEndFeature(Context context, Long lastTrackingPackageTime) {
//        Log.d("INSTA_FEAT", "save end feature");
        if (currFeature != null) {
//            Log.d("INSTA_FEAT", "currfeature is not null");
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
