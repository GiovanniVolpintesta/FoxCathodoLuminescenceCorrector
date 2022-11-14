package com.volpintesta.IBBIC;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.opencv.core.Core;

import java.io.IOException;

public class ConverterApplication extends Application {
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ConverterApplication.class.getResource("converter-window-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Image Converter");
        stage.setScene(scene);
        stage.show();
        fxmlLoader.<ConverterWindowController>getController().init();
    }

    public static void main(String[] args) {
        launch();
    }
}