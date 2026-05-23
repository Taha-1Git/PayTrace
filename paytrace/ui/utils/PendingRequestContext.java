package com.paytrace.ui.utils;

import com.paytrace.models.enums.TargetType;

/**
 * Carries the target (bank/vendor) the user picked on the Login screen
 * so the Choose-Access screen can pre-select it in its form.
 */
public class PendingRequestContext {

    private static TargetType targetType;
    private static String     targetId;
    private static String     targetName;

    public static void set(TargetType type, String id, String name) {
        targetType = type; targetId = id; targetName = name;
    }

    public static TargetType getTargetType() { return targetType; }
    public static String     getTargetId()   { return targetId; }
    public static String     getTargetName() { return targetName; }
    public static boolean    isSet()         { return targetType != null; }

    public static void clear() {
        targetType = null; targetId = null; targetName = null;
    }
}