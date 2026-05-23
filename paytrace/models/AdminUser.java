package com.paytrace.models;

import com.paytrace.models.enums.AccountType;

/**
 * The system CEO — full read access to everything across all vendors.
 * No vendor scoping; no service operations.
 */
public class AdminUser extends User {

    public AdminUser() {
        super(AccountType.ADMIN);
    }

    @Override
    public String getDashboardSummary() {
        return "System overview — all vendors, all users, all activity";
    }

    @Override
    public boolean canAccessVendor(String vendorId) {
        // Admin sees every vendor's data
        return true;
    }

    @Override
    public String getRoleBadge() {
        return "SYSTEM ADMIN";
    }
}