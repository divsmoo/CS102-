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
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.videoio.VideoCapture;
import com.cs102.service.FaceAntiSpoofingService;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FaceCaptureView {

    private VideoCapture camera;
    private FaceDetectorYN faceDetector;
    private ImageView imageView;
    private volatile boolean isCapturing = false;
    private Thread captureThread;
    private Thread detectionThread;
    private List<byte[]> capturedFaces = new ArrayList<>();
    private Consumer<List<byte[]>> onComplete;
    private Runnable onCancel;
    private Label statusLabel;
    private Label faceDetectionLabel;
    private volatile Mat latestFrame = null;
    private volatile Mat latestFaceDetections = null;
    private final Object frameLock = new Object();
    private FaceAntiSpoofingService antiSpoofingService;

    static {
        // Load OpenCV native library
        OpenCV.loadLocally();
    }

    public FaceCaptureView(Stage stage, Consumer<List<byte[]>> onComplete, Runnable onCancel) {
        this.onComplete = onComplete;
        this.onCancel = onCancel;
        // Face detector will be initialized after camera starts (needs resolution)
        this.antiSpoofingService = new FaceAntiSpoofingService();
    }

    private void initializeFaceDetector(int width, int height) {
        try {
            String modelPath = downloadYuNetModel();

            if (modelPath == null) {
                System.err.println("Failed to get YuNet model");
                return;
            }

            // Create YuNet face detector
            faceDetector = FaceDetectorYN.create(
                modelPath,
                "",  // config (empty for ONNX)
                new Size(width, height),  // input size
                0.6f,  // score threshold
                0.3f,  // nms threshold
                5000   // top_k
            );

            System.out.println("YuNet face detector initialized successfully for registration");
        } catch (Exception e) {
            System.err.println("Error loading YuNet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String downloadYuNetModel() {
        try {
            // Try to load from resources first
            java.io.InputStream is = getClass().getClassLoader()
                .getResourceAsStream("face_detection_yunet_2023mar.onnx");

            if (is != null) {
                System.out.println("Loading YuNet model from resources...");
                java.io.File tempFile = java.io.File.createTempFile("yunet", ".onnx");
                tempFile.deleteOnExit();

                java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                is.close();

                System.out.println("YuNet model loaded from resources: " + tempFile.getAbsolutePath());
                return tempFile.getAbsolutePath();
            }

            // If not in resources, download from GitHub
            System.out.println("YuNet model not found in resources, downloading from GitHub...");
            String modelUrl = "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx";

            java.io.File tempFile = java.io.File.createTempFile("yunet", ".onnx");
            tempFile.deleteOnExit();

            // Download the model
            java.net.URL url = new java.net.URL(modelUrl);
            java.io.InputStream in = url.openStream();
            java.nio.file.Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            in.close();

            System.out.println("YuNet model downloaded successfully: " + tempFile.getAbsolutePath());
            return tempFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("Error loading YuNet model: " + e.getMessage());
            e.printStackTrace();
            return null;
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

        faceDetectionLabel = new Label("");
        faceDetectionLabel.setFont(Font.font(14));
        faceDetectionLabel.setStyle("-fx-text-fill: #555; -fx-font-weight: normal;");

        header.getChildren().addAll(title, instructions, statusLabel, faceDetectionLabel);

        // Center Section - Camera View
        VBox centerContent = new VBox(15);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(20));

        imageView = new ImageView();
        imageView.setFitWidth(900); // Appropriate size for window
        imageView.setFitHeight(675); // 900 * 3/4 aspect ratio
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

        Scene scene = new Scene(mainLayout, 1000, 850); // Optimized window size

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

        // Configure camera for maximum resolution and FPS
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 60.0); // Request 60 FPS (camera will use max available)
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH, 1920); // Full HD width
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT, 1080); // Full HD height
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_BUFFERSIZE, 1); // Minimize buffer latency

        // Try common resolutions in order of preference for registration
        // Lower resolution = higher FPS and less lag
        int[][] resolutions = {
            {1280, 720},  // HD 720p - best balance of quality and performance
            {960, 540},   // qHD - good performance
            {640, 480},   // VGA - fallback
        };

        int selectedWidth = 640;
        int selectedHeight = 480;

        for (int[] res : resolutions) {
            camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH, res[0]);
            camera.set(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT, res[1]);

            double actualWidth = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH);
            double actualHeight = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT);

            // Check if camera accepted this resolution (within 10% tolerance)
            if (Math.abs(actualWidth - res[0]) < res[0] * 0.1 &&
                Math.abs(actualHeight - res[1]) < res[1] * 0.1) {
                selectedWidth = (int) actualWidth;
                selectedHeight = (int) actualHeight;
                System.out.println("  ✓ Camera supports " + selectedWidth + "x" + selectedHeight);
                break;
            }
        }

        // Try to maximize FPS
        camera.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 60.0);
        double actualFps = camera.get(org.opencv.videoio.Videoio.CAP_PROP_FPS);
        System.out.println("Camera configured - Resolution: " + selectedWidth + "x" + selectedHeight +
                         ", Requested: 60 FPS, Actual: " + actualFps + " FPS");

        // Initialize YuNet face detector with camera resolution
        initializeFaceDetector(selectedWidth, selectedHeight);

        isCapturing = true;
        statusLabel.setText("Camera active - Position your face");
        statusLabel.setStyle("-fx-text-fill: green;");

        // Separate thread for continuous face detection (doesn't block rendering)
        detectionThread = new Thread(() -> {
            while (isCapturing) {
                Mat frameToDetect = null;
                synchronized (frameLock) {
                    if (latestFrame != null && !latestFrame.empty()) {
                        frameToDetect = latestFrame.clone();
                    }
                }

                if (frameToDetect != null && faceDetector != null) {
                    Mat faces = new Mat();
                    faceDetector.detect(frameToDetect, faces);

                    int numFaces = faces.rows();

                    // Update face detection label
                    if (numFaces == 0) {
                        Platform.runLater(() -> {
                            faceDetectionLabel.setText("⚠ No face detected");
                            faceDetectionLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        });
                    } else if (numFaces == 1) {
                        Platform.runLater(() -> {
                            faceDetectionLabel.setText("✓ Face detected");
                            faceDetectionLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        });
                    } else {
                        Platform.runLater(() -> {
                            faceDetectionLabel.setText("⚠ Multiple faces detected - showing largest");
                            faceDetectionLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        });
                    }

                    // Store face detections for rendering
                    synchronized (frameLock) {
                        if (latestFaceDetections != null) {
                            latestFaceDetections.release();
                        }
                        latestFaceDetections = faces.clone();
                    }

                    faces.release();
                    frameToDetect.release();
                }

                // Small sleep to avoid maxing out CPU
                try {
                    Thread.sleep(50); // Run detection ~20 times per second
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "FaceDetection-Thread");
        detectionThread.setDaemon(true);
        detectionThread.start();

        // Main rendering thread - display ALL frames without dropping
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

                    // Update latest frame for detection thread
                    synchronized (frameLock) {
                        if (latestFrame != null) {
                            latestFrame.release();
                        }
                        latestFrame = frame.clone();
                    }

                    // Draw ONLY the largest face rectangle from latest detections
                    Mat displayFrame = frame.clone();
                    synchronized (frameLock) {
                        if (latestFaceDetections != null && latestFaceDetections.rows() > 0) {
                            // Find the largest face by area
                            int largestIndex = 0;
                            float largestArea = 0;

                            for (int i = 0; i < latestFaceDetections.rows(); i++) {
                                float w = (float) latestFaceDetections.get(i, 2)[0];
                                float h = (float) latestFaceDetections.get(i, 3)[0];
                                float area = w * h;
                                if (area > largestArea) {
                                    largestArea = area;
                                    largestIndex = i;
                                }
                            }

                            // Draw only the largest face
                            float x = (float) latestFaceDetections.get(largestIndex, 0)[0];
                            float y = (float) latestFaceDetections.get(largestIndex, 1)[0];
                            float w = (float) latestFaceDetections.get(largestIndex, 2)[0];
                            float h = (float) latestFaceDetections.get(largestIndex, 3)[0];

                            Imgproc.rectangle(displayFrame,
                                new Point((int)x, (int)y),
                                new Point((int)(x + w), (int)(y + h)),
                                new Scalar(0, 255, 0), 3);
                        }
                    }

                    // Display EVERY frame - no dropping
                    Image image = mat2Image(displayFrame);
                    Platform.runLater(() -> {
                        imageView.setImage(image);
                    });

                    displayFrame.release();
                }

                // Small delay to prevent overwhelming the system
                try {
                    Thread.sleep(16); // ~60 FPS maximum
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "CameraRender-Thread");
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

        if (faceDetector != null) {
            // Detect face using YuNet
            Mat faces = new Mat();
            faceDetector.detect(frame, faces);

            if (faces.rows() > 0) {
                // Find the largest face (by area)
                int largestFaceIndex = 0;
                float largestArea = 0;

                if (faces.rows() > 1) {
                    System.out.println("Multiple faces detected, selecting largest face");
                    for (int i = 0; i < faces.rows(); i++) {
                        float w = (float) faces.get(i, 2)[0];
                        float h = (float) faces.get(i, 3)[0];
                        float area = w * h;
                        if (area > largestArea) {
                            largestArea = area;
                            largestFaceIndex = i;
                        }
                    }
                }

                // Extract face region from largest detection
                // YuNet returns: x, y, w, h, x_re, y_re, x_le, y_le, x_nt, y_nt, x_rcm, y_rcm, x_lcm, y_lcm, confidence
                float x = (float) faces.get(largestFaceIndex, 0)[0];
                float y = (float) faces.get(largestFaceIndex, 1)[0];
                float w = (float) faces.get(largestFaceIndex, 2)[0];
                float h = (float) faces.get(largestFaceIndex, 3)[0];

                // Create face rectangle
                Rect faceRect = new Rect((int)x, (int)y, (int)w, (int)h);

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
            faces.release();
        } else {
            System.out.println("Face detector not available, using full frame");
            processedFace = frame.clone();
        }

        // Store face images for ArcFace (112x112 required)
        if (processedFace != null) {
            // ===== ANTI-SPOOFING CHECK =====
            // Analyze face for spoofing before accepting it
            FaceAntiSpoofingService.SpoofingAnalysisResult spoofingResult = 
                antiSpoofingService.analyzeFace(processedFace, "registration");
            
            System.out.println("🔒 Anti-Spoofing: " + spoofingResult);
            
            if (!spoofingResult.isLive()) {
                // SPOOFING DETECTED - Alert user and reject capture
                System.err.println("⚠️ SPOOFING DETECTED: " + spoofingResult.getDetails());
                
                Platform.runLater(() -> {
                    statusLabel.setText("⚠️ Fake face detected! Use a real camera, not a photo.");
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("Spoofing Detected");
                    alert.setHeaderText("Fake Face Image Detected");
                    alert.setContentText(String.format(
                        "The system detected a fake face (score: %.1f%%).\n\n" +
                        "Possible causes:\n" +
                        "• Using a photo instead of live camera\n" +
                        "• Showing a screen/monitor to the camera\n" +
                        "• Poor lighting or image quality\n" +
                        "• Wearing a mask\n\n" +
                        "Please ensure you're using a real camera with good lighting.",
                        spoofingResult.getConfidenceScore()
                    ));
                    alert.showAndWait();
                });
                
                processedFace.release();
                frame.release();
                return; // Don't capture this face
            }
            
            // Spoofing check passed - proceed with capture
            System.out.println("✓ Liveness verified (Score: " + 
                String.format("%.1f%%", spoofingResult.getConfidenceScore()) + ")");
            
            // Resize to ArcFace standard size (112x112)
            Mat resizedFace = new Mat();
            Imgproc.resize(processedFace, resizedFace, new Size(112, 112));

            // Ensure 3 channels (BGR format - will be converted to RGB during preprocessing)
            Mat bgrFace = new Mat();
            if (resizedFace.channels() == 1) {
                // If grayscale, convert to BGR
                Imgproc.cvtColor(resizedFace, bgrFace, Imgproc.COLOR_GRAY2BGR);
            } else {
                bgrFace = resizedFace.clone();
            }

            // Encode as JPEG in BGR format (OpenCV's native format)
            // The preprocessing step will handle BGR->RGB conversion during recognition
            MatOfByte buffer = new MatOfByte();
            MatOfInt compressionParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 95);
            Imgcodecs.imencode(".jpg", bgrFace, buffer, compressionParams);
            byte[] faceData = buffer.toArray();

            capturedFaces.add(faceData);

            System.out.println("Face captured for ArcFace: " + faceData.length + " bytes (112x112 BGR)");

            // Clean up memory
            resizedFace.release();
            bgrFace.release();
            processedFace.release();
        }
    }

    private void stopCamera() {
        isCapturing = false;
        if (captureThread != null) {
            captureThread.interrupt();
        }
        if (detectionThread != null) {
            detectionThread.interrupt();
        }
        if (camera != null && camera.isOpened()) {
            camera.release();
        }

        // Clean up frames
        synchronized (frameLock) {
            if (latestFrame != null) {
                latestFrame.release();
                latestFrame = null;
            }
            if (latestFaceDetections != null) {
                latestFaceDetections.release();
                latestFaceDetections = null;
            }
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
