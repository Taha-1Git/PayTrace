package com.paytrace.utils;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * Lightweight logger — writes to console + a daily log file.
 * Used by DAOs and services instead of System.err.
 */
public class Logger {

    private static final String LOG_DIR = "D:/paytrace/logs/";

    public static void info(String origin, String msg)  { write("INFO",  origin, msg); }
    public static void warn(String origin, String msg)  { write("WARN",  origin, msg); }
    public static void error(String origin, String msg) { write("ERROR", origin, msg); }

    public static void error(String origin, Throwable t) {
        write("ERROR", origin, t.getClass().getSimpleName() + ": " + t.getMessage());
    }

    private static void write(String level, String origin, String msg) {
        String line = "[" + LocalDateTime.now() + "] [" + level + "] ["
                + origin + "] " + msg;
        System.out.println(line);
        try {
            java.io.File dir = new java.io.File(LOG_DIR);
            if (!dir.exists()) dir.mkdirs();
            String filename = LOG_DIR + "paytrace-"
                    + LocalDateTime.now().toLocalDate() + ".log";
            try (PrintWriter pw = new PrintWriter(new FileWriter(filename, true))) {
                pw.println(line);
            }
        } catch (Exception ignored) {}
    }
}