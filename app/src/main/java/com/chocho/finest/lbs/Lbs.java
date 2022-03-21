package com.chocho.finest.lbs;

import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import org.javatuples.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

public class Lbs {
    private static final String TAG = Lbs.class.getSimpleName();

    File lbsDir;
    long nativeContext = 0;

    private long get_native_context(File dir) {
        File lookup_table = new File(dir, "lt.lbs");
        File database = new File(dir, "db.lbs");
        File log = new File(dir, "log.lbs");

        return native_init_context(lookup_table.getAbsolutePath(),
                database.getAbsolutePath(),
                log.getAbsolutePath());
    }

    public Lbs(File dir) {
        lbsDir = dir;
        nativeContext = get_native_context(dir);
//        Log.d(TAG, "nativeContext: " + Long.toHexString(nativeContext));
    }

    public File getLbsDir() {
        return lbsDir;
    }

    public void log(RootLayoutElement root) {
        if (!lbsDir.isDirectory() && lbsDir.mkdirs())
            Log.e(TAG, "Failed to create log directory at " + lbsDir.getAbsolutePath());
        logInPlainText(root);
        logInLbsFormat(root);
    }

    private void logInLbsFormat(RootLayoutElement root) {
        if (nativeContext == 0 && (nativeContext = get_native_context(lbsDir)) == 0)
            return;

        File lt_lbs = new File(lbsDir, "lt.lbs");
        File db_lbs = new File(lbsDir, "db.lbs");
        File log_lbs = new File(lbsDir, "log.lbs");

        if (db_lbs.exists()
                && Integer.parseInt(String.valueOf(db_lbs.length())) > 10*1000*1000 /* 10 MB */) {
//            native_pause(nativeContext);
            native_destroy_context(nativeContext);
            File zip = ArchiveUtil.archive(new File[]{lt_lbs, db_lbs, log_lbs});
            lt_lbs.delete();
            db_lbs.delete();
            log_lbs.delete();
//            native_resume(nativeContext);
            nativeContext = get_native_context(lbsDir);
            FirebaseUploader.getInstance().enqueue(zip);
        }

        for (LayoutElement elm: root) {
            int x, y, w, h;
            if (elm == root) {
                x = elm.mBounds.left;
                y = elm.mBounds.top;
            } else {
                x = elm.mBounds.left - elm.mParent.mBounds.left;
                y = elm.mBounds.top - elm.mParent.mBounds.top;
            }
            w = elm.mBounds.right - elm.mBounds.left;
            h = elm.mBounds.bottom - elm.mBounds.top;

//            if (x < 0 || y < 0 || w < 0 || h < 0)
//                Log.w(TAG, "Out of parent's bounds. relative x: " + x + ", relative y: " + y + ", w: " + w + ", h: " + h);

            elm.nativeObject = native_new_layout_elm(x, y, w, h,
                    elm.mExtra);
        }
        for (LayoutElement elm: root) {
            for (LayoutElement child: elm.mChildren)
                native_add_child(elm.nativeObject, child.nativeObject);
        }
        native_commit_frame(nativeContext, root.nativeObject);
        native_del_layout_recursive(root.nativeObject);
    }

    private void logInPlainText(RootLayoutElement root) {
        Instant time = Instant.now().truncatedTo(ChronoUnit.MICROS);

        String output = "FRAME;" + time.getLong(ChronoField.INSTANT_SECONDS) +  ";"
                + time.getLong(ChronoField.MICRO_OF_SECOND) + System.lineSeparator();

        for (LayoutElement elm: root) {
            int x, y, w, h;
            if (elm == root) {
                x = elm.mBounds.left;
                y = elm.mBounds.top;
            } else {
                x = elm.mBounds.left - elm.mParent.mBounds.left;
                y = elm.mBounds.top - elm.mParent.mBounds.top;
            }
            w = elm.mBounds.right - elm.mBounds.left;
            h = elm.mBounds.bottom - elm.mBounds.top;

//            if (x < 0 || y < 0 || w < 0 || h < 0)
//                Log.w(TAG, "Out of parent's bounds. relative x: " + x + ", relative y: " + y + ", w: " + w + ", h: " + h);

            output += "BLOCK;" + x + ";" + y + ";" + w + ";" + h + ";" + elm.mExtra + System.lineSeparator();
        }

        File plain_txt = new File(lbsDir, "plain.txt");

        if (plain_txt.exists()
                && Integer.parseInt(String.valueOf(plain_txt.length())) > 10*1000*1000 /* 10 MB */) {
            File zip = ArchiveUtil.archive(plain_txt);
            plain_txt.delete();
            FirebaseUploader.getInstance().enqueue(zip);
        }

        try {
            FileOutputStream fOut = new FileOutputStream(plain_txt, true);
            fOut.write(output.getBytes(StandardCharsets.US_ASCII));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fixBounds(Rect bounds) {
        int temp;

        if (bounds.right < bounds.left) {
//            Log.w(TAG, "Weird. left-right flipped. l: " + bounds.left + ", r: " + bounds.right + ", t: " + bounds.top + ", b: " + bounds.bottom);
            temp = bounds.right;
            bounds.right = bounds.left;
            bounds.left = temp;
        }

        if (bounds.bottom < bounds.top) {
//            Log.w(TAG, "Weird. top-bottom flipped. l: " + bounds.left + ", r: " + bounds.right + ", t: " + bounds.top + ", b: " + bounds.bottom);
            temp = bounds.bottom;
            bounds.bottom = bounds.top;
            bounds.top = temp;
        }

    }

    public static class LayoutElement {
        /* package */ Rect mBounds;
        /* package */ LayoutElement mParent;
        /* package */ ArrayList<LayoutElement> mChildren;
        /* package */ String mExtra;
        /* package */ long nativeObject;

        public LayoutElement(@NonNull LayoutElement parent, Rect bounds) {
            mBounds = bounds;
            fixBounds(mBounds);
            mParent = parent;
            mChildren = new ArrayList<>();

            mParent.mChildren.add(this);
        }

        /* package */ LayoutElement() {
        }

        public Rect getBounds() {
            return mBounds;
        }

        public LayoutElement getParent() {
            return mParent;
        }

        public ArrayList<LayoutElement> getChildren() {
            return mChildren;
        }

        public void setExtra(String extra) {
            mExtra = extra;
        }

        public String getExtra() {
            return mExtra;
        }

        public String getExtraContent(String contentName) {
            int keyword_index = mExtra.indexOf(contentName+"::");
            if (keyword_index == -1)    return "";
            int start_index = mExtra.indexOf("::", keyword_index) + 3;
            if (contentName.equals("viewIdResourceName"))
                return mExtra.substring(start_index);
            int end_index = mExtra.indexOf("||", start_index);
            return mExtra.substring(start_index, end_index);
        }
    }

    public static class RootLayoutElement extends LayoutElement implements Iterable<LayoutElement> {
        public RootLayoutElement(Rect bounds) {
            mBounds = bounds;
            fixBounds(mBounds);
            mParent = null;
            mChildren = new ArrayList<>();
        }

        @NonNull
        @Override
        public Iterator<LayoutElement> iterator() {
            return new DfsIterator(this);
        }

        private static class DfsIterator implements Iterator<LayoutElement> {

            LayoutElement next;
            int nextChild;

            Stack<Pair<LayoutElement, Integer>> stack = new Stack<>();

            DfsIterator(RootLayoutElement root) {
//                stack.push(new Pair<LayoutElement, Integer>(root, 0));
                next = root;
                nextChild = 0;
                setNext();
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            private void setNext() {
                while (true) {
                    if (nextChild < next.mChildren.size()) {
                        /* 1. If I have a child then go for it first */
                        stack.push(new Pair<>(next, nextChild + 1));
                        next = next.mChildren.get(nextChild);
                        nextChild = 0;
                        continue;
                    } else if (nextChild == next.mChildren.size()) {
                        /* 2. This is my turn */
                        nextChild++;
                        return;
                    }

                    /* 3. Me and my children are done. move to my parent if exists */

                    if (stack.empty()) {
                        next = null;
                        break;
                    } else {
                        Pair<LayoutElement, Integer> pair = stack.pop();
                        next = pair.getValue0();
                        nextChild = pair.getValue1();

//                        if (nextChild >= next.mChildren.size())
//                            break;
                    }
                }
            }

            @Override
            public LayoutElement next() {
                LayoutElement elm = next;

                setNext();

                return elm;
            }
        }
    }

    static {
        System.loadLibrary("lbs_android");
    }

    private static native long native_init_context(String ltpath, String dbpath, String logpath);

    private static native int native_destroy_context(long nativeContext);

    private static native long native_new_layout_elm(int x, int y, int width, int height, String extra);

    private static native int native_add_child(long nativeParent, long nativeChild);

    private static native void native_del_layout_recursive(long nativeRoot);

    private static native int native_commit_frame(long nativeContext, long nativeRoot);

    private static native int native_pause(long nativeContext);

    private static native int native_resume(long nativeContext);
}
