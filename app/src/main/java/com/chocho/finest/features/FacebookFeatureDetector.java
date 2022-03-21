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
import java.util.HashMap;

import static com.chocho.finest.util.CryptoUtil.hash;

public class FacebookFeatureDetector extends FeatureDetector {
    enum FacebookFeature {
        UNDEFINED,
        NEWSFEED,
        GROUP,
        WATCH,
        MYPROFILE,
        OTHERPROFILE,
        NOTIFICATION,
        MENU,
        PAGE,
        MESSENGER,
        SEARCH,
        UPLOAD_POST,
        STORY,
        WEB,
        loading,
        main
    }

    FacebookFeature lastFacebookFeature = FacebookFeature.UNDEFINED;
    FacebookFeature facebookFeature = FacebookFeature.UNDEFINED;
    Feature currFeature = null;
    HashMap<Integer, FacebookFeature> windowFeature = new HashMap<>();

    @Override
    public String detect(Context context, Lbs.RootLayoutElement root) {
        extractFacebookFeature(root, context);

//        Log.d("FACEBOOK_FEAT", "Current Feature: " + facebookFeature.toString());
//        Log.d("FACEBOOK_FEAT", "Last Feature: " + lastFacebookFeature.toString());
//        if (facebookFeature != FacebookFeature.UNDEFINED) {
        if (lastFacebookFeature != facebookFeature) {
            // End of last feature
            if (currFeature != null) {
//                Log.d("FACEBOOK_FEAT", "End of a feature");
                currFeature.endFeature(System.currentTimeMillis());
                SharedPreferences sharedPrefs = context.getSharedPreferences("feature-extraction", 0);
                String sessionData = sharedPrefs.getString("sessionData", "");
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("sessionData", sessionData + currFeature.toRow());
                editor.apply();
            }

//            Log.d("FACEBOOK_FEAT", "Start of a feature");
            // Start of a feature
            if (this.packageStart == null) this.packageStart = System.currentTimeMillis();
            currFeature = new Feature(facebookFeature.toString(), this.packageStart,
                    facebookFeature.toString(), System.currentTimeMillis(), ConstKt.getFACEBOOK(), context);
        }
        lastFacebookFeature = facebookFeature;
//        }
        return lastFacebookFeature.toString();
    }

    private void extractFacebookFeature(Lbs.RootLayoutElement root, Context context) {
        printAllChild(root);

        Pair<Integer, Lbs.LayoutElement> selected = new Pair<Integer, Lbs.LayoutElement>(0, null);
        for (Lbs.LayoutElement elm : root) {
            if (elm.getExtra().contains("isSelected:: true")) {
//                Log.d("FACEBOOK_FEAT", "first sibling: " + elm.getParent().getChildren().get(0).getExtraContent("text") + "  |  " + elm.getParent().getChildren().get(0).getExtraContent("contentDescription"));
                /*Page header*/
                if (elm.getParent().getChildren().get(0).getExtraContent("text").equals(hash("홈"))) {
                    selected = selected.setAt0(7);
                    break;
                }
                /*Search header*/
                if (elm.getParent().getChildren().get(0).getExtraContent("text").equals(hash("모두"))) {
                    selected = selected.setAt0(8);
                    break;
                }
                /*Tab*/
                if (elm.getParent().getChildren().get(0).getExtraContent("contentDescription").equals(hash("뉴스피드"))) {
                    String hashedDesc = elm.getExtraContent("contentDescription");
                    if (hashedDesc.equals(hash("뉴스피드"))) {
//                        Log.d("FACEBOOK_FEAT", "selected tab: 뉴스피드");
                        selected = selected.setAt0(1);
                        selected = selected.setAt1(elm);
                    } else if (hashedDesc.equals(hash("그룹"))) {
//                        Log.d("FACEBOOK_FEAT", "selected tab: 그룹");
                        selected = selected.setAt0(2);
                        selected = selected.setAt1(elm);
                    } else if (hashedDesc.equals(hash("Watch"))) {
//                        Log.d("FACEBOOK_FEAT", "selected tab: Watch");
                        selected = selected.setAt0(3);
                        selected = selected.setAt1(elm);
                    } else if (hashedDesc.equals(hash("프로필"))) {
//                        Log.d("FACEBOOK_FEAT", "selected tab: 프로필");
                        selected = selected.setAt0(4);
                        selected = selected.setAt1(elm);
                    } else if (hashedDesc.equals(hash("알림"))) {
//                        Log.d("FACEBOOK_FEAT", "selected tab: 알림");
                        selected = selected.setAt0(5);
                        selected = selected.setAt1(elm);
                    } else if (hashedDesc.equals(hash("메뉴"))) {
//                        Log.d("FACEBOOK_FEAT", "selected tab: 메뉴");
                        selected = selected.setAt0(6);
                        selected = selected.setAt1(elm);
                    } else {
//                        Log.d("FACEBOOK_FEAT", "selected element: " + hashedDesc);
                        selected = selected.setAt1(elm);
                    }
                }
            }
        }

        switch (selected.getValue0()) {
            case 0:
                facebookFeature = analyzeLayout(root, context);
                break;
            case 1:
                facebookFeature = FacebookFeature.NEWSFEED;
                break;
            case 2:
                facebookFeature = FacebookFeature.GROUP;
                break;
            case 3:
                facebookFeature = FacebookFeature.WATCH;
                break;
            case 4:
                facebookFeature = FacebookFeature.MYPROFILE;
                break;
            case 5:
                facebookFeature = FacebookFeature.NOTIFICATION;
                break;
            case 6:
                facebookFeature = FacebookFeature.MENU;
                break;
            case 7:
                facebookFeature = FacebookFeature.PAGE;
                break;
            case 8:
                facebookFeature = FacebookFeature.SEARCH;
                break;
        }

        int windowId = Integer.parseInt(root.getExtraContent("windowId"));
        if (facebookFeature == FacebookFeature.loading || facebookFeature == FacebookFeature.UNDEFINED) {
//            Log.d("FACEBOOK_FEAT", "Undefined or loading");
            facebookFeature = lastFacebookFeature;
        }
        else if ((selected.getValue0() == 0 || selected.getValue0() > 6) && windowFeature.get(windowId) == null) {
//            Log.d("FACEBOOK_FEAT", "Put window "+ windowId + " as " + facebookFeature.toString());
            windowFeature.put(windowId, facebookFeature);
        }
    }

    private FacebookFeature analyzeLayout(Lbs.RootLayoutElement root, Context context) {
        /*Check size*/
        if (root.getBounds().top != 0 || root.getBounds().left != 0)
            return lastFacebookFeature;

        /*Check loading page */
        for (Lbs.LayoutElement elm : root) {
            if (elm.getExtraContent("nodeClassName").equals("android.widget.ProgressBar"))
                return FacebookFeature.loading;
        }

        int windowId = Integer.parseInt(root.getExtraContent("windowId"));
        if (windowFeature.get(windowId) != null)
            return windowFeature.get(windowId);

        /*Check class name*/
        String className = root.getExtraContent("className");
        switch (className) {
            case "com.facebook.messaginginblue.inbox.activities.InboxActivity":
                return FacebookFeature.MESSENGER;
            case "android.webkit.WebView":
            case "com.facebook.browser.lite.BrowserLiteInMainProcess2Activity":
                return FacebookFeature.WEB;
            case "com.facebook.composer.activity.ComposerActivity":
                return FacebookFeature.UPLOAD_POST;
            case "com.facebook.stories.viewer.activity.StoryViewerActivity":
                return FacebookFeature.STORY;
        }

        String[] targetTabContents = {"돌아가기", "채팅", "새 메시지"};
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);

        for (Lbs.LayoutElement elm : root) {
            /*Check messenger header*/
            if (elm.getExtraContent("contentDescription").equals(hash("돌아가기")))
                if (elm.getParent().getChildren().size() == 3) {
                    ArrayList<Lbs.LayoutElement> upperTabContents = elm.getParent().getChildren();
                    int correct = 0;
                    for (int i = upperTabContents.size() - 1; i >= 0; i--) {
                        Lbs.LayoutElement child = upperTabContents.get(i);
                        if (child.getExtraContent("contentDescription").equals(hash(targetTabContents[i]))) {
                            correct += 1;
                        }
                    }
                    if (correct == upperTabContents.size())
                        return FacebookFeature.MESSENGER;
                }

            /*Check search bar*/
            if (elm.getBounds().top == statusBarHeight && elm.getExtra().contains("isEditable:: true")) {
                for (Lbs.LayoutElement profile : root) {
                    /*Check other's profile*/
                    if ((profile.getExtraContent("contentDescription").equals(hash("프로필 사진")) || profile.getExtraContent("contentDescription").equals(hash("프로필"))) && profile.getBounds().width() > root.getBounds().width() * 0.4)
                        return FacebookFeature.OTHERPROFILE;
                }
//                return FacebookFeature.SEARCH;
            }

            /*Check hashtag search*/
            if (elm.getExtraContent("contentDescription").equals(hash("둘러보기"))) {
                return FacebookFeature.SEARCH;
            }
        }

//        Log.d("FACEBOOK_FEAT", "Header Text: "+getHeaderText(root));
        switch (getHeaderText(root)) {
            case "둘러보기":
            case "그룹 검색":
                return FacebookFeature.GROUP;
        }

        Lbs.LayoutElement header = getHeader(root, statusBarHeight);
        if (header != null) {
            if (header.getChildren().get(header.getChildren().size()-1).getChildren().size() == 2)
                if (header.getChildren().get(header.getChildren().size()-1).getChildren().get(1).getExtraContent("contentDescription").equals(hash("멤버 도구")))
                    return FacebookFeature.GROUP;
            if (header.getChildren().get(header.getChildren().size()-1).getChildren().size() == 0) {
                for (Lbs.LayoutElement elm : _getAllChild(header)) {
                    if (elm.getExtra().contains("isEditable:: true"))
                        return FacebookFeature.SEARCH;
                }
            }
        }

        /*Check video view*/
        for (Lbs.LayoutElement elm : root) {
            if (elm.getExtraContent("contentDescription").equals(hash("현재 동영상 재생")) || elm.getExtraContent("contentDescription").equals(hash("현재 동영상 일시 중지"))) {
                return FacebookFeature.WATCH;
            }
        }

        return FacebookFeature.UNDEFINED;
    }

    private Lbs.LayoutElement getHeader(Lbs.RootLayoutElement root, int statusBarHeight) {
        for (Lbs.LayoutElement elm : root) {
            if (elm.getExtraContent("nodeClassName").equals("android.widget.LinearLayout") && elm.getBounds().left == 0 && elm.getBounds().top == statusBarHeight && elm.getBounds().width() == root.getBounds().width()) {
                if (elm.getChildren().size() > 0 && elm.getChildren().get(0).getExtraContent("contentDescription").equals(hash("돌아가기"))) {
                    return elm;
                }
            }
        }
        return null;
    }

    private String getHeaderText(Lbs.RootLayoutElement root) {
        for (Lbs.LayoutElement elm : root) {
            if (elm.getExtraContent("contentDescription").equals(hash("돌아가기"))) {
                for (Lbs.LayoutElement uppertabtext : _getAllChild(elm.getParent())) {
                    String text = uppertabtext.getExtraContent("text");
                    if (text != null && ! text.equals("")) {
                        if (text.equals(hash("공감한 사람")))
                            return "공감한 사람";
                        if (text.equals(hash("둘러보기")))
                            return "둘러보기";
                        if (text.equals(hash("그룹 검색")))
                            return "그룹 검색";
                    }
                }
            }
        }
        return "";
    }

    public static void _printAllChild(Lbs.LayoutElement elm, int depth) {
        String hashedContent = elm.getExtraContent("contentDescription");
        String hashedContent2 = elm.getExtraContent("text");
//        Log.d("Print", Strings.repeat("- ", depth) + elm.getBounds().toString() + "  |  " + hashedContent + "  |  " + hashedContent2 + "  |  " + elm.getExtraContent("nodeClassName") + "  |  " + elm.getExtraContent("isSelected"));
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
//            Log.d("Print", Strings.repeat("- ", 1) + elm.getBounds().toString() + "  |  " + hashedContent + "  |  " + hashedContent2 + "  |  " + elm.getExtraContent("nodeClassName") + "  |  " + elm.getExtraContent("isSelected"));
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

    public void resetLastFeature() {
        lastFacebookFeature = FacebookFeature.UNDEFINED;
        windowFeature = new HashMap<>();
    }

    public void saveEndFeature(Context context, Long lastTrackingPackageTime) {
//        Log.d("FACEBOOK_FEAT", "save end feature");
        if (currFeature != null) {
//            Log.d("FACEBOOK_FEAT", "currfeature is not null");
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
