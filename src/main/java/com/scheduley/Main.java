package com.scheduley;

import com.scheduley.db.Migrations;
import com.scheduley.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        Migrations.init();
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 1180, 760);
        stage.setTitle("Scheduley");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
