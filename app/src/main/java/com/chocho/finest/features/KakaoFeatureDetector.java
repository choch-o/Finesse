package com.chocho.finest.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.chocho.finest.ConstKt;
import com.chocho.finest.Feature;
import com.chocho.finest.lbs.Lbs;
import com.google.common.base.Strings;

import org.javatuples.Pair;

import static com.chocho.finest.util.CryptoUtil.hash;

public class KakaoFeatureDetector extends FeatureDetector {
    enum KakaoFeature {
        UNDEFINED,
        PROFILE,
        CHAT,
        NEWS,
        ETC,
        SHARPSEARCH
    }

    KakaoFeature lastKakaoFeature = KakaoFeature.UNDEFINED;
    KakaoFeature kakaoFeature = KakaoFeature.UNDEFINED;
    Feature currFeature = null;

    @Override
    public String detect(Context context, Lbs.RootLayoutElement root) {
        extractKakaoFeature(root, context);

//        Log.d("KAKAO_FEAT", "Current Feature: " + kakaoFeature.toString());
//        Log.d("KAKAO_FEAT", "Last Feature: " + lastKakaoFeature.toString());
//        if (kakaoFeature != KakaoFeature.UNDEFINED) {
            if (lastKakaoFeature != kakaoFeature) {
                // End of last feature
                if (currFeature != null) {
//                    Log.d("KAKAO_FEAT", "End of a feature");
                    currFeature.endFeature(System.currentTimeMillis());
                    SharedPreferences sharedPrefs = context.getSharedPreferences("feature-extraction", 0);
                    String sessionData = sharedPrefs.getString("sessionData", "");
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString("sessionData", sessionData + currFeature.toRow());
                    editor.apply();
                }

//                Log.d("KAKAO_FEAT", "Start of a feature");
                // Start of a feature
                if (this.packageStart == null) this.packageStart = System.currentTimeMillis();
                currFeature = new Feature(kakaoFeature.toString(), this.packageStart,
                        lastKakaoFeature.toString(), System.currentTimeMillis(),
                        ConstKt.getKAKAOTALK(), context);
            }
            lastKakaoFeature = kakaoFeature;
//        }
        return lastKakaoFeature.toString();
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

    public void extractKakaoFeature(Lbs.RootLayoutElement root, Context context) {
        printAllChild(root);

//        Log.d("FEAT_EXT", "extract kakaotalk feature");
        Pair<Integer, Lbs.LayoutElement> selected = new Pair<Integer, Lbs.LayoutElement>(0, null);
        for (Lbs.LayoutElement elm: root) {
            if (elm.getExtra().contains("isSelected:: true")) {
                String hashedDesc = elm.getExtraContent("contentDescription");
                if (hashedDesc.equals(hash("친구 탭"))) {
//                    Log.d("FEAT_EXT", "selected tab: 친구 ");
                    selected = selected.setAt0(1);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("채팅 탭"))) {
                    // TODO: how to detect dynamic content description
//                    Log.d("FEAT_EXT", "selected tab: 채팅 ");
                    selected = selected.setAt0(2);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("# tab"))) {
//                    Log.d("FEAT_EXT", "selected tab: # tab");
                    selected = selected.setAt0(3);
                    selected = selected.setAt1(elm);
                } else if (hashedDesc.equals(hash("더보기 탭"))) {
//                    Log.d("FEAT_EXT", "selected tab: 더보기 탭");
                    selected = selected.setAt0(4);
                    selected = selected.setAt1(elm);
                }
            }
        }

        switch(selected.getValue0()) {
            case 1:
                kakaoFeature = KakaoFeature.PROFILE;
                break;
            case 2:
                kakaoFeature = KakaoFeature.CHAT;
                break;
            case 3:
                kakaoFeature = KakaoFeature.NEWS;
                break;
            case 4:
                kakaoFeature = KakaoFeature.ETC;
                break;
            case 0:
                if (analyzeLayout(root) != KakaoFeature.UNDEFINED)
                    kakaoFeature = analyzeLayout(root);
        }

        if (kakaoFeature == KakaoFeature.UNDEFINED)
            kakaoFeature = lastKakaoFeature;
    }

    private KakaoFeature analyzeLayout(Lbs.RootLayoutElement root) {
        if (root.getExtraContent("className").equals("com.kakao.talk.profile.ProfileActivity") || root.getExtraContent("className").equals("com.kakao.talk.activity.friend.board.ProfileItemDetailActivity"))
            return KakaoFeature.PROFILE;
        if (root.getExtraContent("className").equals("com.kakao.talk.activity.shop.ShopActivity"))
            return KakaoFeature.ETC;
        if (root.getExtraContent("className").equals("com.kakao.talk.activity.search.card.SharpCardActivity"))
            return KakaoFeature.SHARPSEARCH;

        for (Lbs.LayoutElement tab1 : root) {
            /*check chatroom layout*/
            if (tab1.getExtraContent("viewIdResourceName").equals("com.kakao.talk:id/bubble_linearlayout"))
                return KakaoFeature.CHAT;
            /*check send money layout*/
            if (tab1.getExtraContent("viewIdResourceName").equals("com.kakao.talk:id/toolbar_default_title_text"))
                if (tab1.getExtraContent("contentDescription").equals(hash("친구송금")))
                    return KakaoFeature.ETC;
            /*check sharp search icon*/
            if (tab1.getExtraContent("viewIdResourceName").equals("com.kakao.talk:id/search_card_sharp"))
                return KakaoFeature.SHARPSEARCH;
        }
        return KakaoFeature.UNDEFINED;
    }

    public static void _printAllChild(Lbs.LayoutElement elm, int depth) {
        String hashedContent = elm.getExtraContent("contentDescription");
        String hashedContent2 = elm.getExtraContent("text");
//        Log.d("Print", Strings.repeat("- ", depth) + elm.getBounds().toString() + "\t" + hashedContent + "\t\t" + hashedContent2 + "\t" + elm.getExtraContent("nodeClassName") + "\t" + elm.getExtraContent("viewIdResourceName"));
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
//            Log.d("Print", Strings.repeat("- ", 1) + elm.getBounds().toString() + "\t" + hashedContent + "\t" + hashedContent2 + "\t" + elm.getExtraContent("nodeClassName") + "\t" + elm.getExtraContent("viewIdResourceName"));
            _printAllChild(elm, 1);
        }
//        Log.d("Print", Strings.repeat("-", 100));
    }

    public void resetLastFeature() {
        lastKakaoFeature = KakaoFeature.UNDEFINED;
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
