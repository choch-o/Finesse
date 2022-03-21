package com.chocho.finest

import android.content.Context
import com.google.gson.GsonBuilder
import java.util.*

class Feature(currFeature: String, var packageStart: Long,
              var prevFeature: String, var startTime: Long, packageName: String,
              context: Context) {
    var id: String = currFeature
    var label: String = ""
    var endTime: Long = 0
    var duration: Long = 0

    init {
        when (packageName) {
            INSTAGRAM -> this.label = getInstaLabel(context, currFeature)
            KAKAOTALK -> this.label = getKakaoLabel(context, currFeature)
            FACEBOOK -> this.label = getFacebookLabel(context, currFeature)
            YOUTUBE -> this.label = getYoutubeLabel(context, currFeature)
        }
    }

    fun toRow() : String{
        var pFeature: String? = "\"" + prevFeature + "\""
        if (prevFeature.equals("UNDEFINED")) pFeature = null
        // Google Charts - Gantt
        /*
        val row = "{\"c\":[" +
                    "{\"v\": \"" + id + "\"}," +
                    "{\"v\": \"" + label+ "\"}, " +
                    "{\"v\": \"Date(" + startTime + ")\"}, " +
                    "{\"v\": \"Date(" + endTime + ")\"}, " +
                    "{\"v\": " + duration + "}, " +
                    "{\"v\": 100}, " +
                    "{\"v\": " + pFeature + "}]},"

         */

        // Google Charts - Timeline
        /*
        val row = "{\"c\":[" +
                "{\"v\": \"" + label + "\"}," +
                "{\"v\": \"" + label+ "\"}, " +
                "{\"v\": \"Date(" + (startTime - packageStart) + ")\"}, " +
                "{\"v\": \"Date(" + (endTime - packageStart) + ")\"}]},"

         */

        // Highcharts - Gantt
//        println("ROW")
        val row = "{\"id\": \"" + id + "\"," +
                "\"name\": \"" + label + "\"," +
                "\"start\": " + (startTime - packageStart) + "," +
                "\"end\": " + (endTime - packageStart) + "},"
//                "\"dependency\": " + pFeature + "},"
//        println(row)

        return row
    }

    fun equals(feature: Feature) : Boolean{
        return this.label.equals(feature.label)
    }

    private fun getInstaLabel(context: Context, id: String) : String{
        when (id) {
            "UNDEFINED" -> return "Undefined"
            "FOLLOWING_FEED" -> return context.resources.getString(R.string.following_feed) // "팔로잉 피드 보기"
            "RANDOM_FEED" -> return context.resources.getString(R.string.recommended_feed) // "랜덤 피드 보기"
            "SEARCH" -> return context.resources.getString(R.string.search) // "검색"
            "NOTIFICATION" -> return context.resources.getString(R.string.notification) // "알림"
            "MY_POST" -> return context.resources.getString(R.string.my_post) //"내 포스트"
            "OTHER_POST" -> return context.resources.getString(R.string.other_post) // "다른 사람 포스트"
            "DM" -> return context.resources.getString(R.string.dm) // "메시지"
            "STORY" -> return context.resources.getString(R.string.story) // "스토리 보기"
            "UPLOAD_POST" -> return context.resources.getString(R.string.upload_post) // "포스트 올리기"
            "UPLOAD_STORY" -> return context.resources.getString(R.string.upload_story) // "스토리 올리기"
            "HASHTAG" -> return context.resources.getString(R.string.hashtag) // "해쉬태그별로 보기"
            "WEB" -> return context.resources.getString(R.string.web) // "웹 서핑"
            "VIDEO" -> return context.resources.getString(R.string.ig_video) // "영상 보기"
        }
        return ""
    }

    private fun getKakaoLabel(context: Context, id: String) : String{
        when (id) {
            "UNDEFINED" -> return "Undefined"
            "PROFILE" -> return context.resources.getString(R.string.profile) // "프로필"
            "CHAT" -> return context.resources.getString(R.string.chat) // "채팅"
            "NEWS" -> return context.resources.getString(R.string.news) // "뉴스"
            "ETC" -> return context.resources.getString(R.string.etc) // "기타"
            "SHARPSEARCH" -> return "#검색"
        }
        return ""
    }

    private fun getFacebookLabel(context: Context, id: String) : String{
        when (id) {
            "UNDEFINED" -> return "Undefined"
            "NEWSFEED" -> return context.resources.getString(R.string.newsfeed) // "뉴스피드"
            "GROUP" -> return context.resources.getString(R.string.group) // "그룹"
            "WATCH" -> return context.resources.getString(R.string.fb_video) // "동영상"
            "MYPROFILE" -> return context.resources.getString(R.string.my_profile) // "내 프로필"
            "OTHERPROFILE" -> return context.resources.getString(R.string.other_profile) // "다른 사람 프로필"
            "NOTIFICATION" -> return context.resources.getString(R.string.notification) // "알림"
            "MENU" -> return context.resources.getString(R.string.menu) // "메뉴"
            "PAGE" -> return context.resources.getString(R.string.page) // "페이지"
            "MESSENGER" -> return context.resources.getString(R.string.messenger) // "메신저"
            "SEARCH" -> return context.resources.getString(R.string.fb_search) // "검색/해시태그"
            "WEB" -> return context.resources.getString(R.string.web) // "웹 서핑"
            "POST" -> return context.resources.getString(R.string.post) // "포스트 보기"
            "COMMENT" -> return context.resources.getString(R.string.comment) // "댓글 보기"
            "STORY" -> return context.resources.getString(R.string.story) // "스토리 보기"
            "UPLOAD_POST" -> return context.resources.getString(R.string.upload_post) // "포스트 올리기"
        }
        return ""
    }

    private fun getYoutubeLabel(context: Context, id: String) : String{
        when (id) {
            "UNDEFINED" -> return "Undefined"
            "HOME" -> return context.resources.getString(R.string.home) // "홈"
            "EXPLORE" -> return context.resources.getString(R.string.explore) // "탐색"
            "SUBSCRIPTIONS" -> return context.resources.getString(R.string.subscriptions) // "구독"
            "INBOX" -> return context.resources.getString(R.string.inbox) // "알림"
            "LIBRARY" -> return context.resources.getString(R.string.library) // "보관함/내 계정"
            "SEARCH" -> return context.resources.getString(R.string.search) // "검색"
            "CHANNEL" -> return context.resources.getString(R.string.channel) // "채널"
            "VIDEO" -> return context.resources.getString(R.string.yt_video) // "영상 시청"
            "COMMENT" -> return context.resources.getString(R.string.yt_comment) // "영상 댓글 보기"
            "PLAYLIST" -> return context.resources.getString(R.string.playlist) // "플레이리스트"
        }
        return ""
    }

    fun endFeature(endTime: Long) {
        this.endTime = endTime
        duration = this.endTime - this.startTime
    }
}