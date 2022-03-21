package com.chocho.finest.lbs;

import com.chocho.finest.ZipManager;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class ArchiveUtil {
    private static String getZipFileNameByTime(String basename) {
        Instant time = Instant.now().truncatedTo(ChronoUnit.MICROS);

        String now = Long.toString(time.getLong(ChronoField.INSTANT_SECONDS))
                + '.' + String.format(Locale.getDefault(),
                "%03d", time.getLong(ChronoField.MILLI_OF_SECOND));

        return basename + '.' + now + ".zip";
    }
    public static File archive(File target) {
        return archive(new File[]{target});
    }
    public static File archive(File[] targets) {
        return archive(targets, targets[0].getAbsolutePath());
    }
    public static File archive(File[] targets, String basename) {

        File zip = new File(getZipFileNameByTime(basename));
        // This is unlikely happen but handle this anyway
        while (zip.exists())
            zip = new File(getZipFileNameByTime(basename));

        String[] paths = new String[targets.length];
        for (int i = 0; i < targets.length; i++)
            paths[i] = targets[i].getAbsolutePath();

        ZipManager.zip(paths, zip.getAbsolutePath());

        return zip;
    }
}
