package com.cs102.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FaceCaptureView {

    private VideoCapture camera;
    private CascadeClassifier faceDetector;
    private ImageView imageView;
    private volatile boolean isCapturing = false;
    private Thread captureThread;
    private List<byte[]> capturedFaces = new ArrayList<>();
    private Consumer<List<byte[]>> onComplete;
    private Runnable onCancel;
    private Label statusLabel;

    static {
        // Load OpenCV native library
        OpenCV.loadLocally();
    }

    public FaceCaptureView(Stage stage, Consumer<List<byte[]>> onComplete, Runnable onCancel) {
        this.onComplete = onComplete;
        this.onCancel = onCancel;
        initializeFaceDetector();
    }

    private void initializeFaceDetector() {
        try {
            // Load Haar Cascade classifier for face detection
            // Extract resource to temporary file to handle spaces in path
            java.io.InputStream is = getClass().getClassLoader()
                .getResourceAsStream("haarcascade_frontalface_default.xml");

            if (is == null) {
                System.err.println("Could not find haarcascade_frontalface_default.xml in resources");
                return;
            }

            // Create temp file
            java.io.File tempFile = java.io.File.createTempFile("haarcascade", ".xml");
            tempFile.deleteOnExit();

            // Copy resource to temp file
            java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();

            // Load cascade from temp file
            faceDetector = new CascadeClassifier(tempFile.getAbsolutePath());

            if (faceDetector.empty()) {
                System.err.println("Failed to load cascade classifier from: " + tempFile.getAbsolutePath());
            } else {
                System.out.println("Successfully loaded Haar Cascade classifier");
            }
        } catch (Exception e) {
            System.err.println("Error loading Haar Cascade: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Scene createScene() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        // Top Section - Header
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Text title = new Text("Face Registration");
        title.setFont(Font.font("Tahoma", FontWeight.BOLD, 24));

        Label instructions = new Label("Please look at the camera.\nWe'll automatically capture 5 images of your face.");
        instructions.setFont(Font.font(14));
        instructions.setStyle("-fx-text-fill: gray;");
        instructions.setWrapText(true);
        instructions.setAlignment(Pos.CENTER);

        statusLabel = new Label("Initializing camera...");
        statusLabel.setFont(Font.font(16));
        statusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");

        header.getChildren().addAll(title, instructions, statusLabel);

        // Center Section - Camera View
        VBox centerContent = new VBox(15);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(20));

        imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: black; -fx-border-width: 2;");

        centerContent.getChildren().add(imageView);

        // Bottom Section - Cancel button only
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefWidth(150);
        cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
        cancelBtn.setOnAction(e -> {
            stopCamera();
            onCancel.run();
        });

        controls.getChildren().add(cancelBtn);

        // Assemble layout
        mainLayout.setTop(header);
        mainLayout.setCenter(centerContent);
        mainLayout.setBottom(controls);

        Scene scene = new Scene(mainLayout, 800, 700);

        // Automatically start camera and capture process
        Platform.runLater(() -> startAutomaticCapture());

        return scene;
    }

    private void startAutomaticCapture() {
        // Start camera first
        startCamera();

        // Wait a moment for camera to initialize, then start automatic capture
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Wait 1 second for camera to stabilize

                // Capture 5 images with 1.5 second intervals
                for (int i = 0; i < 5; i++) {
                    final int captureNum = i + 1;

                    Platform.runLater(() -> {
                        statusLabel.setText("Capturing image " + captureNum + "/5...");
                        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    });

                    Thread.sleep(500); // Short delay before capture

                    // Capture the face image
                    captureFaceImage();

                    Platform.runLater(() -> {
                        statusLabel.setText("Captured " + capturedFaces.size() + "/5 images");
                    });

                    // Wait before next capture (except for the last one)
                    if (i < 4) {
                        Thread.sleep(1000);
                    }
                }

                // All captures complete
                Thread.sleep(500);
                Platform.runLater(() -> {
                    stopCamera();
                    if (capturedFaces.size() >= 5) {
                        statusLabel.setText("Capture complete! Proceeding to registration...");
                        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        // Automatically proceed to registration
                        onComplete.accept(capturedFaces);
                    } else {
                        statusLabel.setText("Failed to capture enough images. Please try again.");
                        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        showAlert(AlertType.ERROR, "Capture Failed",
                            "Only captured " + capturedFaces.size() + " images.\nPlease ensure your face is clearly visible.");
                    }
                });

            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Capture interrupted");
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
            }
        }).start();
    }

    private void startCamera() {
        camera = new VideoCapture(0); // 0 for default camera

        if (!camera.isOpened()) {
            showAlert(AlertType.ERROR, "Camera Error", "Failed to open camera");
            return;
        }

        isCapturing = true;
        statusLabel.setText("Camera active - Position your face");
        statusLabel.setStyle("-fx-text-fill: green;");

        captureThread = new Thread(() -> {
            Mat frame = new Mat();
            int frameCount = 0;
            while (isCapturing) {
                if (camera.read(frame) && !frame.empty()) {
                    frameCount++;
                    if (frameCount % 30 == 0) {
                        System.out.println("Camera frame " + frameCount + " - Size: " + frame.size());
                    }

                    // Detect faces only if detector is loaded
                    if (faceDetector != null && !faceDetector.empty()) {
                        MatOfRect faceDetections = new MatOfRect();
                        faceDetector.detectMultiScale(frame, faceDetections);

                        // Draw rectangles around faces
                        for (Rect rect : faceDetections.toArray()) {
                            Imgproc.rectangle(frame, new Point(rect.x, rect.y),
                                new Point(rect.x + rect.width, rect.y + rect.height),
                                new Scalar(0, 255, 0), 3);
                        }
                    }

                    // Convert to JavaFX Image and display FULL FRAME
                    Image image = mat2Image(frame);
                    Platform.runLater(() -> {
                        imageView.setImage(image);
                        imageView.setFitWidth(640);
                        imageView.setFitHeight(480);
                    });
                }

                try {
                    Thread.sleep(33); // ~30 FPS
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void captureFaceImage() {
        if (camera == null || !camera.isOpened()) {
            System.err.println("Camera is not active");
            return;
        }

        if (faceDetector == null || faceDetector.empty()) {
            System.err.println("Face detector not loaded");
            // Still capture the full frame if detector isn't available
            Mat frame = new Mat();
            if (camera.read(frame)) {
                MatOfByte buffer = new MatOfByte();
                Imgcodecs.imencode(".jpg", frame, buffer);
                byte[] frameData = buffer.toArray();
                capturedFaces.add(frameData);
                System.out.println("Frame captured (no face detection): " + frameData.length + " bytes");
            }
            return;
        }

        Mat frame = new Mat();
        if (camera.read(frame)) {
            // Detect face
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(frame, faceDetections);

            if (faceDetections.toArray().length == 0) {
                // No face detected, capture full frame anyway
                System.out.println("No face detected, capturing full frame");
                MatOfByte buffer = new MatOfByte();
                Imgcodecs.imencode(".jpg", frame, buffer);
                byte[] frameData = buffer.toArray();
                capturedFaces.add(frameData);
                return;
            }

            if (faceDetections.toArray().length > 1) {
                System.out.println("Multiple faces detected, using first face");
            }

            // Extract face region
            Rect faceRect = faceDetections.toArray()[0];
            Mat faceROI = frame.submat(faceRect);

            // Convert to byte array (JPEG format)
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".jpg", faceROI, buffer);
            byte[] faceData = buffer.toArray();

            capturedFaces.add(faceData);
            statusLabel.setText("Captured " + capturedFaces.size() + "/5 images");

            System.out.println("Face captured: " + faceData.length + " bytes");
        }
    }

    private void stopCamera() {
        isCapturing = false;
        if (captureThread != null) {
            captureThread.interrupt();
        }
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
        statusLabel.setText("Camera stopped");
        statusLabel.setStyle("-fx-text-fill: gray;");
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
