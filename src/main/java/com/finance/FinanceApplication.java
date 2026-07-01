package com.finance;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
        applyAppIcon(primaryStage);
        Stage splashStage = new Stage(StageStyle.TRANSPARENT);
        applyAppIcon(splashStage);
        showSplashScreen(splashStage);

        Thread springWaitThread = new Thread(() -> {
            try {
                if (!springInitialized.await(30, TimeUnit.SECONDS)) {
                    System.err.println("ERROR: Spring Boot initialization timeout");
                    Platform.exit();
                    System.exit(1);
                    return;
                }
                if (applicationContext == null) {
                    System.err.println("ERROR: Spring application context is null");
                    Platform.exit();
                    System.exit(1);
                    return;
                }
                Platform.runLater(() -> {
                    try {
                        splashStage.close();
                        showLoginScreen();
                    } catch (Exception e) {
                        System.err.println("ERROR: Failed to start application");
                        e.printStackTrace();
                        System.exit(1);
                    }
                });
            } catch (Exception e) {
                System.err.println("ERROR: Failed to start application");
                e.printStackTrace();
                System.exit(1);
            }
        });
        springWaitThread.setDaemon(true);
        springWaitThread.start();
    }

    private static void applyAppIcon(Stage stage) {
        if (FinanceApplication.class.getResource("/images/logo.png") != null) {
            stage.getIcons().add(new Image(FinanceApplication.class.getResourceAsStream("/images/logo.png")));
        }
    }

    private static void showSplashScreen(Stage splashStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(FinanceApplication.class.getResource("/fxml/Splash.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 500, 350);
        scene.setFill(Color.TRANSPARENT);
        applyCSS(scene);
        splashStage.setScene(scene);
        splashStage.setResizable(false);
        splashStage.centerOnScreen();
        splashStage.show();
    }

    public static void showLoginScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(FinanceApplication.class.getResource("/fxml/Login.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * 0.24;
        double height = screenBounds.getHeight() * 0.32;

        Scene scene = new Scene(root, width, height);
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

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * 0.75;
        double height = screenBounds.getHeight() * 0.82;

        Scene scene = new Scene(root, width, height);
        applyCSS(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(screenBounds.getWidth() * 0.45);
        primaryStage.setMinHeight(screenBounds.getHeight() * 0.45);
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