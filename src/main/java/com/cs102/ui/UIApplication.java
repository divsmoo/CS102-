package com.cs102.ui;

import com.cs102.manager.AuthenticationManager;
import com.cs102.manager.DatabaseManager;
import com.cs102.manager.SessionManager;
import com.cs102.service.FacialRecognitionService;
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
        SessionManager sessionManager = springContext.getBean(SessionManager.class);
        DatabaseManager databaseManager = springContext.getBean(DatabaseManager.class);
        FacialRecognitionService faceService = springContext.getBean(FacialRecognitionService.class);

        // TODO: Build your JavaFX UI here
        // You can now call methods directly on the managers/services
        // Example: authManager.authenticate(username, password);
    }

    @Override
    public void stop() throws Exception {
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
