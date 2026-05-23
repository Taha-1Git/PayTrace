package com.paytrace.models;

import com.paytrace.models.enums.AccountType;

/**
 * Vendor owner — manages exactly one vendor's invoices, payments, users.
 * Has access ONLY to their own vendor.
 */
public class AdministratorUser extends User {

    private String managedVendorId;

    public AdministratorUser() {
        super(AccountType.ADMINISTRATOR);
    }

    public String getManagedVendorId()             { return managedVendorId; }
    public void   setManagedVendorId(String v)     { this.managedVendorId = v; }

    @Override
    public String getDashboardSummary() {
        return "Your vendor's payments, invoices, and connected users";
    }

    @Override
    public boolean canAccessVendor(String vendorId) {
        return managedVendorId != null && managedVendorId.equals(vendorId);
    }

    @Override
    public String getRoleBadge() {
        return "VENDOR ADMIN";
    }
}