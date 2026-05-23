package com.paytrace.ui.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * Two navigation modes:
 *   1) navigateTo(path)         — full scene swap (used for Login/Register/Dashboard root)
 *   2) loadIntoShell(path)      — load FXML content INTO the dashboard's center pane
 *                                 (used for inner screens like Reports, Reconciliation, etc.)
 */
public class SceneNavigator {

    private static Stage primaryStage;
    private static Pane  shellContentPane;   // the center pane of the active dashboard

    // ── Scene constants ──
    public static final String LOGIN            = "/fxml/Login.fxml";
    public static final String REGISTER         = "/fxml/Register.fxml";
    public static final String DASHBOARD        = "/fxml/Dashboard.fxml";

    // ── Inner screens (loaded INTO the shell, not as full scenes) ──
    public static final String DASHBOARD_HOME   = "/fxml/DashboardHome.fxml";
    public static final String CHOOSE_ACCESS    = "/fxml/ChooseAccess.fxml";
    public static final String IMPORT           = "/fxml/Import.fxml";
    public static final String RECON            = "/fxml/Reconciliation.fxml";
    public static final String MANUAL_RECON     = "/fxml/ManualReconciliation.fxml";
    public static final String ALERTS           = "/fxml/Alerts.fxml";
    public static final String REPORTS          = "/fxml/Reports.fxml";
    public static final String CONN_REQUESTS    = "/fxml/ConnectionRequests.fxml";
    public static final String CREATE_INVOICE   = "/fxml/CreateInvoice.fxml";
    public static final String MY_INVOICES      = "/fxml/MyInvoices.fxml";

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void setShellContentPane(Pane pane) {
        shellContentPane = pane;
    }

    /** Full scene swap — used for Login → Dashboard, Logout → Login, etc. */
    public static void navigateTo(String fxmlPath) throws Exception {
        navigateTo(fxmlPath, 1280, 800);
    }

    public static void navigateTo(String fxmlPath, double width, double height) throws Exception {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root, width, height);

        // ADD THIS LINE ↓
        scene.getStylesheets().add(
                SceneNavigator.class.getResource("/css/paytrace.css").toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    /** Loads inner screen FXML INTO the dashboard's content pane (no scene swap). */
    /** Loads inner screen FXML INTO the dashboard's content pane with a fade-in transition. */
    public static void loadIntoShell(String fxmlPath) throws Exception {
        if (shellContentPane == null) {
            throw new IllegalStateException(
                    "Shell content pane not set. Are you on the Dashboard?");
        }
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
        Parent content = loader.load();
        shellContentPane.getChildren().setAll(content);

        if (content instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region r = (javafx.scene.layout.Region) content;
            r.prefWidthProperty().bind(shellContentPane.widthProperty());
            r.prefHeightProperty().bind(shellContentPane.heightProperty());
        }

        // Fade-in animation (~250ms)
        content.setOpacity(0);
        javafx.animation.FadeTransition fade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}