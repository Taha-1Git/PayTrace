package com.paytrace.models;

import com.paytrace.models.enums.AccountType;

/**
 * Customer using PayTrace to receive invoices and pay vendors.
 * Can access only the vendor they're currently logged into via code.
 */
public class RegularUser extends User {

    private String currentVendorContext;   // set at login time from the access code

    public RegularUser() {
        super(AccountType.USER);
    }

    public String getCurrentVendorContext()         { return currentVendorContext; }
    public void   setCurrentVendorContext(String v) { this.currentVendorContext = v; }

    @Override
    public String getDashboardSummary() {
        return "Your invoices, quotes, and service requests";
    }

    @Override
    public boolean canAccessVendor(String vendorId) {
        return currentVendorContext != null && currentVendorContext.equals(vendorId);
    }

    @Override
    public String getRoleBadge() {
        return "USER";
    }
}