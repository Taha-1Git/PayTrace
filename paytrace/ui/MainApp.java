package com.paytrace.ui;

import com.paytrace.ui.utils.SceneNavigator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;




public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneNavigator.setPrimaryStage(primaryStage);
        primaryStage.initStyle(StageStyle.DECORATED);

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));

        primaryStage.setTitle("PayTrace — Multivendor Payment Reconciliation");
        Scene scene = new Scene(root, 1000, 720);

        // ADD THIS LINE ↓
        scene.getStylesheets().add(getClass().getResource("/css/paytrace.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(680);
        primaryStage.setMinHeight(560);
        primaryStage.centerOnScreen();
        primaryStage.setMaximized(true);
        primaryStage.show();
    }


    @Override
    public void stop() {
        // Gracefully close DB connections on app exit
        try {
            com.paytrace.config.DatabaseConnection.getInstance().shutdown();
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}