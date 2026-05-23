package com.paytrace.patterns.observer;

import com.paytrace.models.Alert;

/** Observer Pattern — dashboard listens for new alerts */
public interface AlertObserver {
    void onAlertTriggered(Alert alert);
    void notifyDashboard(int count);
}
