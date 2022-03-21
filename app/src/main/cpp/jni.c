#include <jni.h>
#include "lbs.h"
#include <string.h>
#include <android/log.h>

JNIEXPORT jlong JNICALL
Java_com_chocho_finest_lbs_Lbs_native_1new_1layout_1elm(JNIEnv *env, jclass clazz, jint x, jint y,
                                                        jint width, jint height, jstring extra) {
    const char *n_extra = (*env)->GetStringUTFChars(env, extra, 0);
    int n_extra_len = strlen(n_extra);

    layout_elm_t *elm = lbs_new_layout_elm(x, y, width, height, n_extra, n_extra_len);

    (*env)->ReleaseStringUTFChars(env, extra, n_extra);
//    __android_log_print(ANDROID_LOG_DEBUG, "LbsNative", "nativeElm: %p", elm);

    return (jlong)elm;
}

JNIEXPORT jlong JNICALL
Java_com_chocho_finest_lbs_Lbs_native_1init_1context(JNIEnv *env, jclass clazz, jstring ltpath,
                                                     jstring dbpath, jstring logpath) {
    static lbs_ctx_t ctx;
    int ret;

    const char *n_ltpath = (*env)->GetStringUTFChars(env, ltpath, 0);
    const char *n_dbpath = (*env)->GetStringUTFChars(env, dbpath, 0);
    const char *n_logpath = (*env)->GetStringUTFChars(env, logpath, 0);

    ret = lbs_init_ctx(&ctx, n_ltpath, n_dbpath, n_logpath, false);

    (*env)->ReleaseStringUTFChars(env, ltpath, n_ltpath);
    (*env)->ReleaseStringUTFChars(env, dbpath, n_dbpath);
    (*env)->ReleaseStringUTFChars(env, logpath, n_logpath);

//    __android_log_print(ANDROID_LOG_DEBUG, "LbsNative", "nativeContext: %p", &ctx);

    if (ret < 0)
        return 0;

    return (jlong)&ctx;
}

JNIEXPORT jint JNICALL
Java_com_chocho_finest_lbs_Lbs_native_1destroy_1context(JNIEnv *env, jclass clazz,
                                                        jlong native_context) {
    lbs_ctx_t *ctx = (lbs_ctx_t *)native_context;
    return (jint)lbs_destroy_ctx(ctx);
}

JNIEXPORT jint JNICALL
Java_com_chocho_finest_lbs_Lbs_native_1add_1child(JNIEnv *env, jclass clazz, jlong native_parent,
                                                  jlong native_child) {
    layout_elm_t *parent = (layout_elm_t *)native_parent;
    layout_elm_t *child = (layout_elm_t *)native_child;

    return (jint)lbs_add_child(parent, child);
}

JNIEXPORT void JNICALL
Java_com_chocho_finest_lbs_Lbs_native_1del_1layout_1recursive(JNIEnv *env, jclass clazz,
                                                              jlong native_root) {
    layout_elm_t *root = (layout_elm_t *)native_root;

    lbs_del_layout_recursive(root);
}

JNIEXPORT jint JNICALL
Java_com_chocho_finest_lbs_Lbs_native_1commit_1frame(JNIEnv *env, jclass clazz,
                                                     jlong native_context, jlong native_root) {
    lbs_ctx_t *ctx = (lbs_ctx_t *)native_context;
    layout_elm_t *root = (layout_elm_t *)native_root;

    return (jint)lbs_commit_frame(ctx, root, NULL);
}

JNIEXPORT jint JNICALL
Java_com_chocho_finest_lbs_Lbs_native_1pause(JNIEnv *env, jclass clazz, jlong native_context) {
    lbs_ctx_t *ctx = (lbs_ctx_t *)native_context;

    return (jint)lbs_pause(ctx);
}

JNIEXPORT jint JNICALL
Java_com_chocho_finest_lbs_Lbs_native_1resume(JNIEnv *env, jclass clazz, jlong native_context) {
    lbs_ctx_t *ctx = (lbs_ctx_t *)native_context;

    return (jint)lbs_resume(ctx);
}