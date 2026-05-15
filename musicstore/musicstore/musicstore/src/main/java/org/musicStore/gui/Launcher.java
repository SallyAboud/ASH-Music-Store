package org.musicStore.gui;

import org.musicStore.util.DBUtil;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {

        try {
            DBUtil.getConnection(); // test connection
        } catch (Exception e) {
            e.printStackTrace();
        }

        Application.launch(MainApp.class, args);
    }
}