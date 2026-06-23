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

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Wait for Spring Boot to initialize (max 30 seconds)
            if (!springInitialized.await(30, TimeUnit.SECONDS)) {
                System.err.println("ERROR: Spring Boot initialization timeout");
                System.exit(1);
            }

            // Check if Spring context is available
            if (applicationContext == null) {
                System.err.println("ERROR: Spring application context is null");
                System.exit(1);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();
            primaryStage.setTitle("Daily Finance Management System");
            primaryStage.setScene(new Scene(root, 1024, 768));
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to start application");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        Thread springThread = new Thread(() -> {
            try {
                applicationContext = SpringApplication.run(FinanceApplication.class, args);
                springInitialized.countDown(); // Signal that Spring is ready
            } catch (Exception e) {
                System.err.println("ERROR: Spring Boot failed to initialize");
                e.printStackTrace();
                System.exit(1);
            }
        });
        springThread.setDaemon(true);
        springThread.start();

        // Launch JavaFX application
        launch(args);
    }
}