package com.finance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class FinanceApplication extends Application {
    @Getter
    private static ApplicationContext applicationContext;
    private static final CountDownLatch springInitialized = new CountDownLatch(1);

    @Getter
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        try {
            if (!springInitialized.await(30, TimeUnit.SECONDS)) {
                System.err.println("ERROR: Spring Boot initialization timeout");
                System.exit(1);
            }
            if (applicationContext == null) {
                System.err.println("ERROR: Spring application context is null");
                System.exit(1);
            }
            showLoginScreen();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to start application");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void showLoginScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(FinanceApplication.class.getResource("/fxml/Login.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root, 500, 400);
        applyCSS(scene);
        primaryStage.setTitle("Daily Finance Management System");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showDashboard() throws Exception {
        FXMLLoader loader = new FXMLLoader(FinanceApplication.class.getResource("/fxml/Dashboard.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1024, 768);
        applyCSS(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
    }

    private static void applyCSS(Scene scene) {
        String css = FinanceApplication.class.getResource("/css/main.css") != null
                ? FinanceApplication.class.getResource("/css/main.css").toExternalForm()
                : null;
        if (css != null) scene.getStylesheets().add(css);
    }

    public static void main(String[] args) {
        Thread springThread = new Thread(() -> {
            try {
                applicationContext = SpringApplication.run(FinanceApplication.class, args);
                springInitialized.countDown();
            } catch (Exception e) {
                System.err.println("ERROR: Spring Boot failed to initialize");
                e.printStackTrace();
                System.exit(1);
            }
        });
        springThread.setDaemon(true);
        springThread.start();
        launch(args);
    }
}