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

        Label instructions = new Label("Please look at the camera.\nWe'll automatically capture 15 images of your face.\nTurn your head slightly for different angles.");
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

                // Capture 15 images with intervals
                for (int i = 0; i < 15; i++) {
                    final int captureNum = i + 1;

                    Platform.runLater(() -> {
                        statusLabel.setText("Capturing image " + captureNum + "/15...");
                        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    });

                    Thread.sleep(300); // Short delay before capture

                    // Capture the face image
                    captureFaceImage();

                    Platform.runLater(() -> {
                        statusLabel.setText("Captured " + capturedFaces.size() + "/15 images");
                    });

                    // Wait before next capture (except for the last one)
                    if (i < 14) {
                        Thread.sleep(700); // 700ms between captures for variety
                    }
                }

                // All captures complete
                Thread.sleep(500);
                Platform.runLater(() -> {
                    stopCamera();
                    if (capturedFaces.size() >= 15) {
                        statusLabel.setText("Capture complete! Processing images...");
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

        // Configure camera for maximum FPS and performance
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 60.0); // Request 60 FPS (camera will use max available)
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH, 640);
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT, 480);
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_BUFFERSIZE, 1); // Minimize buffer latency

        // Get actual FPS the camera can provide
        double actualFps = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FPS);
        System.out.println("Camera configured - Requested: 60 FPS, Actual: " + actualFps + " FPS");

        isCapturing = true;
        statusLabel.setText("Camera active - Position your face");
        statusLabel.setStyle("-fx-text-fill: green;");

        captureThread = new Thread(() -> {
            Mat frame = new Mat();
            int frameCount = 0;
            long startTime = System.currentTimeMillis();
            long lastFpsReport = startTime;

            while (isCapturing) {
                if (camera.read(frame) && !frame.empty()) {
                    frameCount++;

                    // Report FPS every second
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastFpsReport >= 1000) {
                        long elapsed = currentTime - startTime;
                        double fps = frameCount / (elapsed / 1000.0);
                        System.out.println("Camera FPS: " + String.format("%.2f", fps) + " (Frame #" + frameCount + ")");
                        lastFpsReport = currentTime;
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

                // NO SLEEP - Run at maximum FPS
                // The camera.read() call is the bottleneck, so removing sleep allows max FPS
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

        Mat frame = new Mat();
        if (!camera.read(frame) || frame.empty()) {
            System.err.println("Failed to read frame from camera");
            return;
        }

        Mat processedFace = null;

        if (faceDetector != null && !faceDetector.empty()) {
            // Detect face
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(frame, faceDetections);

            if (faceDetections.toArray().length > 0) {
                if (faceDetections.toArray().length > 1) {
                    System.out.println("Multiple faces detected, using first face");
                }

                // Extract face region
                Rect faceRect = faceDetections.toArray()[0];

                // Add padding around face (15% on each side)
                int padding = (int)(faceRect.width * 0.15);
                faceRect.x = Math.max(0, faceRect.x - padding);
                faceRect.y = Math.max(0, faceRect.y - padding);
                faceRect.width = Math.min(frame.width() - faceRect.x, faceRect.width + 2 * padding);
                faceRect.height = Math.min(frame.height() - faceRect.y, faceRect.height + 2 * padding);

                processedFace = frame.submat(faceRect);
            } else {
                System.out.println("No face detected, using full frame");
                processedFace = frame.clone();
            }
        } else {
            System.out.println("Face detector not available, using full frame");
            processedFace = frame.clone();
        }

        // PREPROCESSING PIPELINE
        if (processedFace != null) {
            // 1. Convert to grayscale
            Mat grayFace = new Mat();
            Imgproc.cvtColor(processedFace, grayFace, Imgproc.COLOR_BGR2GRAY);

            // 2. Normalize lighting using histogram equalization
            Mat equalizedFace = new Mat();
            Imgproc.equalizeHist(grayFace, equalizedFace);

            // 3. Resize to standardized size (224x224 - common for face recognition)
            Mat resizedFace = new Mat();
            Imgproc.resize(equalizedFace, resizedFace, new Size(224, 224), 0, 0, Imgproc.INTER_LANCZOS4);

            // 4. Apply Gaussian blur to reduce noise
            Mat blurredFace = new Mat();
            Imgproc.GaussianBlur(resizedFace, blurredFace, new Size(3, 3), 0);

            // 5. Normalize pixel values to standardized range
            Mat normalizedFace = new Mat();
            Core.normalize(blurredFace, normalizedFace, 0, 255, Core.NORM_MINMAX);

            // Convert to byte array (JPEG format with high quality for database storage)
            MatOfByte buffer = new MatOfByte();
            MatOfInt compressionParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 95);
            Imgcodecs.imencode(".jpg", normalizedFace, buffer, compressionParams);
            byte[] faceData = buffer.toArray();

            capturedFaces.add(faceData);

            System.out.println("Preprocessed face captured: " + faceData.length + " bytes (224x224 grayscale, equalized, normalized)");

            // Clean up memory
            grayFace.release();
            equalizedFace.release();
            resizedFace.release();
            blurredFace.release();
            normalizedFace.release();
            processedFace.release();
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
