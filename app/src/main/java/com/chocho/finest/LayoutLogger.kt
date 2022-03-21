package com.chocho.finest

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Environment
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.chocho.finest.features.FeatureDetector
import com.chocho.finest.features.FeatureDetectorManager
import com.chocho.finest.lbs.Lbs
import com.chocho.finest.util.CryptoUtil.hash
import com.google.gson.Gson
import kr.ac.kaist.nmsl.xdroid.util.Singleton
import java.io.File

var windowManager: WindowManager? = null
var screen_width = -1
var screen_height = -1

var sInstance: Singleton<Lbs> =
    object : Singleton<Lbs>() {
        override fun create(): Lbs {
            val sdMain = File(Environment.getExternalStorageDirectory(), "/Finest/lbs")
            if (!sdMain.exists()) sdMain.mkdirs()
            return Lbs(sdMain)
        }
    }

fun getLbsContext(): Lbs {
    return sInstance.get()
}

fun handleLayoutUpdate(context: Context, rootNode: AccessibilityNodeInfo, sourceNode: AccessibilityNodeInfo?, packageName: String, className: String, event: AccessibilityEvent) {
    fun _updateWindowSize(context: Context) {
        if (windowManager == null)
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val point = Point()
        windowManager!!.defaultDisplay.getRealSize(point)

        screen_width = point.x
        screen_height = point.y
    }

    // Save the tree layout recursively
    fun _saveLayoutToLbs(node: AccessibilityNodeInfo, elm: Lbs.LayoutElement, packageName: String) {
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i) ?: continue

            val childElm = createLayoutElementFromAccessibilityNodeInfo(childNode, packageName,
                className, elm, false, node == sourceNode, event.eventType) ?: continue

            _saveLayoutToLbs(childNode, childElm, packageName)
        }
    }

    _updateWindowSize(context)

    // Save the tree-structured UI layout in Lbs format (defined by Donghwi).
    val elm = createLayoutElementFromAccessibilityNodeInfo(rootNode, packageName,
        className, null, true, rootNode == sourceNode, event.eventType) as Lbs.RootLayoutElement?
        ?: return
    _saveLayoutToLbs(rootNode, elm, packageName)

    // Call handleLayoutUpdate using the root element
    FeatureDetectorManager.instance.handleLayoutUpdate(context, packageName, elm, sourceNode, event)

    // Log the Lbs-formatted layout
    getLbsContext().log(elm)
}

fun handleScreenOff(context: Context) {
    val elm = Lbs.RootLayoutElement(Rect(0,0,0,0))
    elm.extra += "isScreenOff: true"
    FeatureDetectorManager.instance.handleLayoutUpdate(context, "screenoff", elm, null, null)
    getLbsContext().log(elm)
}

fun createLayoutElementFromAccessibilityNodeInfo(node: AccessibilityNodeInfo,
                                                 packageName: String,
                                                 className: String,
                                                 parentElm: Lbs.LayoutElement?,
                                                 isRoot: Boolean,
                                                 isSource: Boolean,
                                                 eventType: Int): Lbs.LayoutElement? {
    fun _validateBounds(bounds: Rect): Boolean {
        return bounds.left >= 0 && bounds.right >= 0 && bounds.top >= 0 && bounds.bottom >= 0
                && bounds.left <= screen_width && bounds.right <= screen_width
                && bounds.top <= screen_height && bounds.bottom <= screen_height
    }

    val bounds = Rect()
    node.getBoundsInScreen(bounds)

// 200825 dhkim: We disable boundary validation as out-of-screen UI elements are still useful for us
//    if (!_validateBounds(bounds)) {
//        return null
//    }

    val elm: Lbs.LayoutElement

    if (isRoot)
        elm = Lbs.RootLayoutElement(bounds)
    else if (parentElm == null)
        return null
    else
        elm = Lbs.LayoutElement(parentElm, bounds)

    elm.extra = ""
    elm.extra += "isSource:: " + isSource + "|| "
    elm.extra += "eventType:: " + eventType + "|| "
    elm.extra += "isFocused:: " + node.isFocused + "|| "
    elm.extra += "labelFor:: " + node.labelFor + "|| "
    elm.extra += "labeledBy:: " + node.labeledBy + "|| "
    if (node.text != null)
        elm.extra += "text:: " + hash(node.text.toString()) + "|| "
    if (node.contentDescription != null) {
        val conDesc = node.contentDescription.toString()
        if (packageName.equals(KAKAOTALK)) {
            if (node.isSelected) {
                if (conDesc.contains("채팅 탭")) {
                    elm.extra += "contentDescription:: " + hash("채팅 탭") + "|| "
                } else if (conDesc.contains("더보기 탭")) {
                    elm.extra += "contentDescription:: " + hash("더보기 탭") + "|| "
                } else {
                    elm.extra += "contentDescription:: " + hash(conDesc) + "|| "
                }
            }
        }
        if (packageName.equals(FACEBOOK)) {
            if (conDesc.contains("뉴스피드")) {
                elm.extra += "contentDescription:: " + hash("뉴스피드") + "|| "
            } else if (conDesc.contains("그룹")) {
                elm.extra += "contentDescription:: " + hash("그룹") + "|| "
            } else if (conDesc.contains("Watch")) {
                elm.extra += "contentDescription:: " + hash("Watch") + "|| "
            } else if (conDesc.contains("프로필")) {
                elm.extra += "contentDescription:: " + hash("프로필") + "|| "
            } else if (conDesc.contains("알림")) {
                elm.extra += "contentDescription:: " + hash("알림") + "|| "
            } else if (conDesc.contains("메뉴")) {
                elm.extra += "contentDescription:: " + hash("메뉴") + "|| "
            } else if (conDesc.contains("홈")) {
                elm.extra += "contentDescription:: " + hash("홈") + "|| "
            } else {
                elm.extra += "contentDescription:: " + hash(conDesc) + "|| "
            }
        } else if (packageName.equals(YOUTUBE)) {
            if (conDesc.contains("홈")) {
                elm.extra += "contentDescription:: " + hash("홈") + "|| "
            } else if (conDesc.contains("탐색")) {
                elm.extra += "contentDescription:: " + hash("탐색") + "|| "
            } else if (conDesc.contains("구독")) {
                elm.extra += "contentDescription:: " + hash("구독") + "|| "
            } else if (conDesc.contains("알림")) {
                elm.extra += "contentDescription:: " + hash("알림") + "|| "
            } else if (conDesc.contains("보관함")) {
                elm.extra += "contentDescription:: " + hash("보관함") + "|| "
            } else {
                elm.extra += "contentDescription:: " + hash(conDesc) + "|| "
            }
        } else {
            elm.extra += "contentDescription:: " + hash(conDesc) + "|| "
        }
    }
    elm.extra += "windowId:: " + node.windowId + "|| "
    elm.extra += "actionList:: " + node.actionList + "|| "
    elm.extra += "isScrollable:: " + node.isScrollable + "|| "
    elm.extra += "isClickabble:: " + node.isClickable + "|| "
    elm.extra += "isEditable:: " + node.isEditable + "|| "
    elm.extra += "isPassword:: " + node.isPassword + "|| "
    elm.extra += "isSelected:: " + node.isSelected + "|| "
    elm.extra += "drawingOrder:: " + node.drawingOrder + "|| "
    elm.extra += "paneTitle:: " + node.paneTitle + "|| "
    elm.extra += "packageName:: " + packageName + "|| "
    elm.extra += "className:: " + className + "|| "
    elm.extra += "nodeClassName:: " + node.className + "|| "
    elm.extra += "viewIdResourceName:: " + node.viewIdResourceName

    return elm
}
