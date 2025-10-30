package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class UIApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        springContext = SpringApplication.run(com.cs102.Application.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Get Spring-managed beans (services/managers)
        AuthenticationManager authManager = springContext.getBean(AuthenticationManager.class);

        // Create and show auth screen with tabs
        AuthView authView = new AuthView(primaryStage, authManager);
        primaryStage.setScene(authView.createScene());
        primaryStage.setTitle("Student Attendance System");
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        springContext.close();
    }

    public static void main(String[] args) {
        // Set default timezone to Singapore for the entire JVM
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Singapore"));
        System.out.println("Default timezone set to: " + java.util.TimeZone.getDefault().getID());

        launch(args);
    }
}
