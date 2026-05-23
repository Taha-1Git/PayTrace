package com.paytrace.ui.observer;

import com.paytrace.models.Alert;
import com.paytrace.patterns.observer.AlertObserver;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Concrete Observer for UC-09 (Monitor and Respond to System Alerts).
 *
 * Listens to alerts raised through {@link com.paytrace.services.AlertService}
 * and exposes JavaFX-observable properties so any controller can bind a
 * notification badge or status label to the live alert state — no polling.
 *
 * Singleton — keeps a single shared count per JVM so every controller sees
 * the same number even when several dashboards are open.
 */
public final class DashboardAlertObserver implements AlertObserver {

    private static final DashboardAlertObserver INSTANCE = new DashboardAlertObserver();
    public  static DashboardAlertObserver getInstance() { return INSTANCE; }

    /** Live count of ACTIVE alerts; bind a Label.text to this for free updates. */
    private final IntegerProperty activeCount = new SimpleIntegerProperty(0);

    /** Most recently triggered alert — controllers can react via change-listener. */
    private final SimpleObjectProperty<Alert> lastAlert = new SimpleObjectProperty<>(null);

    private DashboardAlertObserver() {}

    public IntegerProperty            activeCountProperty() { return activeCount; }
    public SimpleObjectProperty<Alert> lastAlertProperty()  { return lastAlert;   }
    public int  getActiveCount() { return activeCount.get(); }

    @Override
    public void onAlertTriggered(Alert alert) {
        runOnFx(() -> lastAlert.set(alert));
    }

    @Override
    public void notifyDashboard(int count) {
        runOnFx(() -> activeCount.set(count));
    }

    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
