package com.chocho.finest.util;

import com.google.common.hash.Hashing;

import java.nio.charset.Charset;

public class CryptoUtil {
    public static String hash(String str) {
//        return str;
        return Hashing.sha1()
                .hashString(str, Charset.defaultCharset()).toString();
    }
}
