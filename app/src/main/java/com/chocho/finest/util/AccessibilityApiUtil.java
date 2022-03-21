package com.chocho.finest.util;

/* 200430 dhkim:
 * This is a util class providing STATIC methods for inspecting Android accessibility API.
 */

import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.reflect.Method;

import kr.ac.kaist.nmsl.xdroid.util.Reflection;

public class AccessibilityApiUtil {

    public static int getAccessibilityConnectionId(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null)
            return 0;

//        Object nodeInfo_mConnectionId = Reflection.getObject(nodeInfo, AccessibilityNodeInfo.class.getName(), "mConnectionId");
//        if (nodeInfo_mConnectionId == null)
//            return 0;

        int connectionId = Reflection.getInt(nodeInfo,
                AccessibilityNodeInfo.class.getName(), "mConnectionId");
        return connectionId;
    }

    public static long getAccessibilityNodeId(AccessibilityNodeInfo nodeInfo) {
        long nodeInfo_mSourceNodeId = Reflection.getLong(nodeInfo,
                AccessibilityNodeInfo.class.getName(), "mSourceNodeId");
//        if (nodeInfo_mSourceNodeId == Reflection.getLong(nodeInfo,
//                AccessibilityNodeInfo.class.getName(), "UNDEFINED_NODE_ID"))
//            return 0;

        return nodeInfo_mSourceNodeId;
    }
    public static long getChildAccessibilityNodeId(AccessibilityNodeInfo nodeInfo, int childIdx) {
        /* 200430 dhkim:
         * Java reflection enables us to access classes, their methods, and variables by names.
         * When you use the reflection, you must always keep in mind that those classes, methods,
         * and variables may or may not exist at run-time. For example, when this app is compiled,
         * we only know that there is a class named "AccessibilityNodeInfo", but we are not sure
         * anything about how it is implemented. In this method, "getChildAccessibilityNodeId", we
         * assume that "AccessibilityNodeInfo" class has a "android.util.LongArray" type variable
         * "mChildNodeIds". We also assume that "android.util.LongArray" has a method named "get"
         * with an integer parameter slot, and we can get each child's ID by calling the "get"
         * method of "mChildNodeIds". Such assumptions may break when this app is executed on a
         * different variant of OS (e.g., different versions, different device vendors, ...).
         *
         * FYI, we can list underlying assumptions of this method as follows.
         * 1. Android OS has "android.util.LongArray" class.
         * 2. "android.util.LongArray" class has "long get(int)" method.
         * 3. "AccessibilityNodeInfo" class has a "android.util.LongArray" type variable,
         *    "mChildNodeIds".
         */

        if (nodeInfo == null)
            return 0;

        Method LongArray_get = Reflection.getMethod("android.util.LongArray", "get", int.class);
        if (LongArray_get == null)
            return 0;

        Object nodeInfo_mChildNodeIds = Reflection.getObject(nodeInfo, AccessibilityNodeInfo.class.getName(), "mChildNodeIds");
        if (nodeInfo_mChildNodeIds == null)
            return 0;

        long childId = (long)Reflection.invoke(nodeInfo_mChildNodeIds, LongArray_get, childIdx);

        return childId;
    }

    /* 200430 dhkim: FIXME: Please uncomment this method and fix errors.
     * While reading AccessibilityNodeInfo's implementation, I've found that
     * 1. AccessibilityNodeInfo does not maintain the ID of itself.
     * 2. The actual values of AccessibilityNodeId of children (e.g., 0xffffffff00000002) do not seem to be globally unique.
     * 3. We can get mConnectionId and mWindowId from a parent AccessibilityNodeInfo.
     * These may imply
     * 1. Each AccessibilityNodeInfo has globally unique connection ID (and/or) window ID
     * 2. AccessibilityNodeId is yet another relative child index only meaningful to its parent.
     */
//    public static AccessibilityNodeInfo findAccessibilityNodeInfoByChildAccessibilityId(long childId) {
//        final int FLAG_PREFETCH_DESCENDANTS = 0x00000004;
//
//        Method AccessibilityInteractionClient_findAccessibilityNodeInfoByAccessibilityId =
//                Reflection.getMethod("android.view.accessibility.AccessibilityInteractionClient",
//                        "findAccessibilityNodeInfoByAccessibilityId",
//                        int.class /* connectionId */,
//                        int.class /* accessibilityWindowId */,
//                        long.class /* accessibilityNodeId */,
//                        boolean.class /* bypassCache */,
//                        int.class /* prefetchFlags */,
//                        Bundle.class /* arguments */);
//
//        /* AccessibilityInteractionClient */ Object client =
//                Reflection.invoke(null, "android.view.accessibility.AccessibilityInteractionClient", "getInstance");
//
//        return Reflection.invoke(client, AccessibilityInteractionClient_findAccessibilityNodeInfoByAccessibilityId,
//                mConnectionId, mWindowId,
//                childId, false, FLAG_PREFETCH_DESCENDANTS, null);
//    }
}
