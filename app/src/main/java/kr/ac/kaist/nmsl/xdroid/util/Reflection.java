package kr.ac.kaist.nmsl.xdroid.util;

//import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dhkim on 5/22/17.
 */

public class Reflection {

    private static final String TAG = "Reflection";

    private Map<MKey, Class<?>> mClassMap = null;
    private Map<MKey, Constructor<?>> mConstructorMap = null;
    private Map<MKey, Method> mMethodMap = null;
    private Map<MKey, Field> mFieldMap = null;

    private static Singleton<Reflection> gDefault = new Singleton<Reflection>() {
        @Override
        protected Reflection create() {
            return new Reflection();
        }
    };

    public static Reflection getDefault() {
        return gDefault.get();
    }

    public Reflection() {
        mClassMap = new HashMap<>();
        mConstructorMap = new HashMap<>();
        mMethodMap = new HashMap<>();
        mFieldMap = new HashMap<>();
    }

    public static Class<?> getClass(String name) {
        return getClass(null, name);
    }
    public static Class<?> getClass(ClassLoader cl, String name) {
        return getClass(cl, name, true);
    }

    public static Class<?> getClass(ClassLoader cl, String name, boolean verbose) {
        MKey k = new MKey(name);
        Class<?> v;

        if ((v = getDefault().mClassMap.get(k)) == null) {
            try {
                if (cl == null)
                    v = Class.forName(name);
                else
                    v = Class.forName(name, true, cl);
            } catch (ClassNotFoundException e) {
                if (verbose)
                    e.printStackTrace();
            }
        } else
            return v;

        if (v == null) {
//            Log.d(TAG, "Failed to find " + name + " by using " + cl);
            return null;
        }

        getDefault().mClassMap.put(k, v);

        return v;
    }

    public static Field getField(String classname, String fieldname) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return null;

        return getField(c, fieldname);
    }

    public static Field getField(Class<?> c, String name) {
        MKey k = new MKey(c, name);
        Field v;

        if ((v = getDefault().mFieldMap.get(k)) == null) {
            try {
                v = c.getDeclaredField(name);
                v.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        } else
            return v;

        if (v == null)
            return null;

        getDefault().mFieldMap.put(k, v);

        return v;
    }

    public static boolean hasField(String classname, String fieldname) {
        Class<?> c;
        if ((c = getClass(classname)) == null)
            return false;
        return hasField(c, fieldname);
    }

    public static boolean hasField(Class<?> c, String name) {
        MKey k = new MKey(c, name);
        Field v;

        if ((v = getDefault().mFieldMap.get(k)) == null) {
            try {
                v = c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                return false;
            }
        } else
            return true;

        if (v == null)
            return false;

        getDefault().mFieldMap.put(k, v);

        return true;
    }

    public static Object getObject(Object t, String classname, String fieldname) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return null;

        return getObject(t, c, fieldname);
    }

    public static Object getObject(Object t, Class<?> c, String fieldname) {
        Field f = getField(c, fieldname);
        Object o = null;

        try {
            o = f.get(t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return o;
    }

    public static void setObject(Object t, Class<?> c, String fieldname, Object value) {
        Field f = getField(c, fieldname);

        try {
            f.setAccessible(true);
            f.set(t, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getInt(Object t, String classname, String fieldname) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return 0;

        return getInt(t, c, fieldname);
    }

    public static int getInt(Object t, Class<?> c, String fieldname) {
        Field f = getField(c, fieldname);
        int i = 0;

        try {
            i = f.getInt(t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return i;
    }

    public static boolean getBoolean(Object t, String classname, String fieldname) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return false;

        return getBoolean(t, c, fieldname);
    }

    public static boolean getBoolean(Object t, Class<?> c, String fieldname) {
        Field f = getField(c, fieldname);
        boolean i = false;

        try {
            i = f.getBoolean(t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return i;
    }

    public static float getFloat(Object t, String classname, String fieldname) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return 0;

        return getFloat(t, c, fieldname);
    }

    public static float getFloat(Object t, Class<?> c, String fieldname) {
        Field f = getField(c, fieldname);
        float i = 0;

        try {
            i = f.getFloat(t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return i;
    }

    public static long getLong(Object t, String classname, String fieldname) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return 0;

        return getLong(t, c, fieldname);
    }

    public static long getLong(Object t, Class<?> c, String fieldname) {
        Field f = getField(c, fieldname);
        long i = 0;

        try {
            i = f.getLong(t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return i;
    }

    public static String getString(Object t, String classname, String fieldname) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return null;

        return getString(t, c, fieldname);
    }

    public static String getString(Object t, Class<?> c, String fieldname) {
        Field f = getField(c, fieldname);
        String s = null;

        try {
            s = (String)f.get(t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return s;
    }

    public static void setInt(Object t, Class<?> c, String fieldname, int value) {
        Field f = getField(c, fieldname);

        try {
            f.setInt(t, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setBoolean(Object t, Class<?> c, String fieldname, boolean value) {
        Field f = getField(c, fieldname);

        try {
            f.setBoolean(t, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setFloat(Object t, Class<?> c, String fieldname, float value) {
        Field f = getField(c, fieldname);

        try {
            f.setFloat(t, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setLong(Object t, Class<?> c, String fieldname, long value) {
        Field f = getField(c, fieldname);

        try {
            f.setLong(t, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Constructor<?> getConstructor(String name, Class<?>... args) {
        Class<?> c;

        if ((c = getClass(name)) == null)
            return null;

        return getConstructor(c, args);
    }

    public static Constructor<?> getConstructor(Class<?> c, Class<?>... args) {
        MKey k = new MKey(c, args);
        Constructor<?> v;

        if ((v = getDefault().mConstructorMap.get(k)) == null) {
            try {
                v = c.getDeclaredConstructor(args);
                v.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else
            return v;

        if (v == null)
            return null;

        getDefault().mConstructorMap.put(k, v);

        return v;
    }

    public static Method getMethod(String classname, String methodname, Class<?>... args) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return null;

        return getMethod(c, methodname, args);
    }

    public static Method getMethod(Class<?> c, String name, Class<?>... args) {
        MKey k = new MKey(c, name, args);
        Method v;

        if ((v = getDefault().mMethodMap.get(k)) == null) {
            try {
                v = c.getDeclaredMethod(name, args);
                v.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else
            return v;

        if (v == null)
            return null;

        getDefault().mMethodMap.put(k, v);

        return v;
    }

    public static Object instantiate(String name) {
        Class<?> c;

        if ((c = getClass(name)) == null)
            return null;

        return instantiate(c);
    }

    public static Object instantiate(Class<?> c) {
        Object o = null;

        try {
            o = c.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (o == null)
            return null;

        return o;
    }

    public static Object instantiate(Constructor<?> c, Object... args) {
        Object o = null;

        try {
            o = c.newInstance(args);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        if (o == null)
            return null;

        return o;
    }

    @Deprecated
    public static Object invoke(Object t, String classname, String methodname, Object... args) {
        Class<?> c;

        if ((c = getClass(classname)) == null)
            return null;

        return invoke(t, c, methodname, args);
    }

    @Deprecated
    public static Object invoke(Object t, Class<?> c, String methodname, Object... args) {
        Method m;
        Class<?>[] classes = (Class<?>[])args.clone();

        for (int i = 0; i < classes.length; i++)
            classes[i] = classes[i].getClass();

        if ((m = getMethod(c, methodname, classes)) == null)
            return null;

        return invoke(t, m, args);
    }

    public static Object invoke(Object t, String classname, String methodname) {
        Method m;

        if ((m = getMethod(classname, methodname)) == null)
            return null;

        return invoke(t, m);
    }

    public static Object invoke(Object t, Class<?> c, String methodname) {
        Method m;

        if ((m = getMethod(c, methodname)) == null)
            return null;

        return invoke(t, m);
    }

    public static Object invoke(Object t, Method m, Object... args) {
        Object o = null;

        try {
            o = m.invoke(t, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return o;
    }

    private static class MKey implements Comparable<MKey> {

        private int iKey;

        public MKey(Object... args) {
            iKey = 0;
            for (Object o : args) {
                iKey ^= o.hashCode();
            }
        }

        public int hashcode() {
            return iKey;
        }

        @Override
        public boolean equals(Object o) {
            if(o == this)
                return true;

            else if(o == null || !(o instanceof MKey))
                return false;

            return this.compareTo((MKey)o) == 0;
        }

        /**
         * Compares this object to the specified object to determine their relative
         * order.
         *
         * @param another the object to compare to this instance.
         * @return a negative integer if this instance is less than {@code another};
         * a positive integer if this instance is greater than
         * {@code another}; 0 if this instance has the same order as
         * {@code another}.
         * @throws ClassCastException if {@code another} cannot be converted into something
         *                            comparable to {@code this} instance.
         */
        @Override
        public int compareTo(/*@NonNull*/ MKey another) {
            return iKey - another.iKey;
        }
    }
}
