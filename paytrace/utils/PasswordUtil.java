package com.paytrace.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {
    /** Hash a plain password with BCrypt cost 12 */
    public static String hash(String plainPassword) {
        return BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
    }

    /** Verify a plain password against a stored BCrypt hash */
    public static boolean verify(String plainPassword, String hash) {
        return BCrypt.verifyer()
                     .verify(plainPassword.toCharArray(), hash)
                     .verified;
    }
}
