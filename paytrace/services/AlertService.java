package com.paytrace.services;

import com.paytrace.dao.AlertDAO;
import com.paytrace.dao.AlertDAO;
import com.paytrace.models.Alert;
import com.paytrace.models.enums.AlertStatus;
import com.paytrace.patterns.observer.AlertObserver;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject in the Observer pattern (UC-09).
 *
 * Persists new alerts via {@link AlertDAO} and broadcasts them to every
 * registered {@link AlertObserver} so dashboards, notification badges, and
 * downstream services react in real time without polling the database.
 *
 * Singleton — observers register against the same instance from anywhere
 * in the application (controllers, services, schedulers).
 */
public final class AlertService {

    private static final AlertService INSTANCE = new AlertService();
    public  static AlertService getInstance() { return INSTANCE; }

    private final AlertDAO alertDAO = new AlertDAO();

    /** Thread-safe list — observers may be added/removed from any thread. */
    private final List<AlertObserver> observers = new CopyOnWriteArrayList<>();

    private AlertService() {}

    /* ─── Observer registration ─────────────────────────────────────── */

    public void addObserver(AlertObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(AlertObserver observer) {
        observers.remove(observer);
    }

    public int observerCount() { return observers.size(); }

    /* ─── Subject behaviour: raise & broadcast ──────────────────────── */

    /**
     * Persist the alert, then notify every observer with the saved alert
     * and the new active-alert count. Each observer is isolated: a single
     * observer throwing does not stop the others.
     */
    public void raiseAlert(Alert alert) throws SQLException {
        alertDAO.save(alert);
        int active = alertDAO.findByStatus(AlertStatus.ACTIVE).size();
        for (AlertObserver o : observers) {
            try {
                o.onAlertTriggered(alert);
                o.notifyDashboard(active);
            } catch (Exception ex) {
                System.err.println("AlertObserver " + o.getClass().getSimpleName()
                        + " failed: " + ex.getMessage());
            }
        }
    }

    /** Re-broadcast the current active count to all observers (e.g. after a refresh). */
    public void refreshAll() throws SQLException {
        int active = alertDAO.findByStatus(AlertStatus.ACTIVE).size();
        for (AlertObserver o : observers) {
            try { o.notifyDashboard(active); }
            catch (Exception ex) {
                System.err.println("AlertObserver refresh failed: " + ex.getMessage());
            }
        }
    }

    /* ─── Read-through to DAO (so callers don't bypass the service) ── */

    public List<Alert> findActive() throws SQLException {
        return alertDAO.findByStatus(AlertStatus.ACTIVE);
    }

    public List<Alert> findByStatus(AlertStatus status) throws SQLException {
        return alertDAO.findByStatus(status);
    }

    public List<Alert> findAll() throws SQLException {
        return alertDAO.findAll();
    }

    public void markReviewed(String alertId, String userId) throws SQLException {
        alertDAO.updateStatus(alertId, AlertStatus.REVIEWED, userId);
        refreshAll();
    }

    /** Dismiss an info-level alert (UC-09 extension). */
    public void dismiss(String alertId, String userId) throws SQLException {
        alertDAO.updateStatus(alertId, AlertStatus.AUTO_RESOLVED, userId);
        refreshAll();
    }
}
