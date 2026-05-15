package org.musicStore.gui;

import org.musicStore.util.DBUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.musicStore.dao.*;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(loader.load(), 900, 620);
        scene.getStylesheets().add(MainApp.class.getResource("/dark-theme.css").toExternalForm());

        primaryStage.setTitle("🎵 ASH Music Store");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
        primaryStage.setMaximized(true);
    }

    @Override
    public void stop() {
        DBUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}