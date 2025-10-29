package com.cs102;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Simple switch: if you start the JVM with -Dno.ui=true
        // the app will run backend-only (no JavaFX). This is useful while
        // you're developing REST endpoints and don't want to load the UI.
        boolean noUi = "true".equals(System.getProperty("no.ui"));

        // Always start Spring Boot (the backend). If you ever want to
        // conditionally launch JavaFX in the same JVM, you can add code here.
        SpringApplication.run(Application.class, args);

        // NOTE: We purposely do NOT launch JavaFX here. Launch the UI separately
        // with the javafx plugin: `mvn javafx:run -Djavafx.platform=mac-aarch64 ...`
        // That keeps backend and UI development separate and simpler.
    }
}
