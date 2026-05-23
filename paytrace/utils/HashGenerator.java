package com.paytrace.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class HashGenerator {
    public static void main(String[] args) {
        String adminHash = BCrypt.withDefaults().hashToString(10, "12345678".toCharArray());
        String userHash  = BCrypt.withDefaults().hashToString(10, "12345678".toCharArray());

        System.out.println("════════════════════════════════════════════");
        System.out.println("  COPY THESE INTO V2__seed_data.sql");
        System.out.println("════════════════════════════════════════════");
        System.out.println();
        System.out.println("12345678 hash (use for all administrator accounts):");
        System.out.println(adminHash);
        System.out.println();
        System.out.println("12345678 hash (use for all regular user accounts):");
        System.out.println(userHash);
        System.out.println();
        System.out.println("════════════════════════════════════════════");
    }
}