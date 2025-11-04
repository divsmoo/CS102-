package com.cs102.service;

import com.cs102.model.SecurityEventType;
import com.cs102.model.Severity;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Face Anti-Spoofing Service
 * Detects fake face images from photos, screens, masks, etc.
 */
@Service
public class FaceAntiSpoofingService {

    @Autowired(required = false)
    private IntrusionDetectionService idsService;

    // Configuration thresholds
    private static final double TEXTURE_THRESHOLD = 15.0; // Minimum texture variance
    private static final double QUALITY_THRESHOLD = 25.0; // Minimum image quality
    private static final double BLUR_THRESHOLD = 100.0;   // Maximum blur (lower = more blurred)
    private static final int MIN_FACE_SIZE = 80;          // Minimum face size in pixels
    private static final double BRIGHTNESS_MIN = 40.0;    // Too dark
    private static final double BRIGHTNESS_MAX = 220.0;   // Too bright/washed out

    /**
     * Analyze a face image for spoofing attempts
     * Returns a SpoofingAnalysisResult with score and details
     */
    public SpoofingAnalysisResult analyzeFace(Mat faceImage, String email) {
        if (faceImage == null || faceImage.empty()) {
            return new SpoofingAnalysisResult(false, 0.0, "Empty image");
        }

        List<String> suspiciousIndicators = new ArrayList<>();
        double totalScore = 0.0;
        int checkCount = 0;

        // 1. Size Check - Very small faces might be from printed photos
        double sizeScore = checkFaceSize(faceImage, suspiciousIndicators);
        totalScore += sizeScore;
        checkCount++;

        // 2. Texture Analysis - Printed photos lack natural skin texture
        double textureScore = analyzeTexture(faceImage, suspiciousIndicators);
        totalScore += textureScore;
        checkCount++;

        // 3. Blur Detection - Screen/printed images often have unusual blur
        double blurScore = detectBlur(faceImage, suspiciousIndicators);
        totalScore += blurScore;
        checkCount++;

        // 4. Color Distribution - Fake images have unnatural color patterns
        double colorScore = analyzeColorDistribution(faceImage, suspiciousIndicators);
        totalScore += colorScore;
        checkCount++;

        // 5. Brightness Analysis - Too bright/dark suggests photo of screen
        double brightnessScore = analyzeBrightness(faceImage, suspiciousIndicators);
        totalScore += brightnessScore;
        checkCount++;

        // 6. Edge Analysis - Printed photos have different edge characteristics
        double edgeScore = analyzeEdges(faceImage, suspiciousIndicators);
        totalScore += edgeScore;
        checkCount++;

        // Calculate final score (0-100)
        double finalScore = (totalScore / checkCount) * 100;
        
        // Determine if spoofing detected (below 60% is suspicious)
        boolean isSpoofing = finalScore < 60.0;
        
        String details = String.format("Anti-Spoofing Score: %.1f%% | Checks: %s", 
            finalScore, String.join(", ", suspiciousIndicators));

        // Log to IDS if spoofing detected
        if (isSpoofing && idsService != null) {
            idsService.logSecurityEvent(
                SecurityEventType.FACE_SPOOFING_DETECTED,
                Severity.CRITICAL,
                email != null ? email : "Unknown",
                details
            );
        }

        return new SpoofingAnalysisResult(!isSpoofing, finalScore, details);
    }

    /**
     * Quick liveness check - verify image is from a real person
     */
    public boolean quickLivenessCheck(Mat faceImage) {
        if (faceImage == null || faceImage.empty()) {
            return false;
        }

        // Check basic indicators quickly
        double textureScore = calculateTextureVariance(faceImage);
        double blurScore = calculateBlurScore(faceImage);
        
        // Quick pass/fail based on critical indicators
        return textureScore > TEXTURE_THRESHOLD && blurScore > BLUR_THRESHOLD;
    }

    // ========== Internal Analysis Methods ==========

    private double checkFaceSize(Mat face, List<String> indicators) {
        int size = Math.min(face.width(), face.height());
        
        if (size < MIN_FACE_SIZE) {
            indicators.add("Face too small");
            return 0.3; // Suspicious - might be photo of photo
        } else if (size < MIN_FACE_SIZE * 1.5) {
            return 0.7; // Borderline
        }
        return 1.0; // Good size
    }

    private double analyzeTexture(Mat face, List<String> indicators) {
        double variance = calculateTextureVariance(face);
        
        if (variance < TEXTURE_THRESHOLD * 0.5) {
            indicators.add("Low texture (printed photo?)");
            return 0.2;
        } else if (variance < TEXTURE_THRESHOLD) {
            indicators.add("Below-normal texture");
            return 0.6;
        }
        return 1.0; // Good natural texture
    }

    private double calculateTextureVariance(Mat face) {
        // Convert to grayscale for texture analysis
        Mat gray = new Mat();
        if (face.channels() > 1) {
            Imgproc.cvtColor(face, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = face.clone();
        }

        // Calculate Laplacian variance (texture measure)
        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
        
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stddev);
        
        double variance = stddev.get(0, 0)[0];
        
        gray.release();
        laplacian.release();
        
        return variance;
    }

    private double detectBlur(Mat face, List<String> indicators) {
        double blurScore = calculateBlurScore(face);
        
        if (blurScore < BLUR_THRESHOLD * 0.5) {
            indicators.add("Excessive blur detected");
            return 0.3;
        } else if (blurScore < BLUR_THRESHOLD) {
            indicators.add("Unusual blur pattern");
            return 0.7;
        }
        return 1.0; // Sharp image
    }

    private double calculateBlurScore(Mat face) {
        Mat gray = new Mat();
        if (face.channels() > 1) {
            Imgproc.cvtColor(face, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = face.clone();
        }

        // Variance of Laplacian (blur measure)
        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
        
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stddev);
        
        double variance = Math.pow(stddev.get(0, 0)[0], 2);
        
        gray.release();
        laplacian.release();
        
        return variance;
    }

    private double analyzeColorDistribution(Mat face, List<String> indicators) {
        if (face.channels() < 3) {
            return 1.0; // Grayscale, can't analyze color
        }

        // Calculate standard deviation across channels
        List<Mat> channels = new ArrayList<>();
        Core.split(face, channels);
        
        double totalStdDev = 0;
        for (Mat channel : channels) {
            MatOfDouble mean = new MatOfDouble();
            MatOfDouble stddev = new MatOfDouble();
            Core.meanStdDev(channel, mean, stddev);
            totalStdDev += stddev.get(0, 0)[0];
            channel.release();
        }
        
        double avgStdDev = totalStdDev / 3.0;
        
        // Printed photos often have very uniform colors
        if (avgStdDev < 20.0) {
            indicators.add("Flat color distribution");
            return 0.4;
        } else if (avgStdDev < 30.0) {
            return 0.7;
        }
        return 1.0; // Good color variance
    }

    private double analyzeBrightness(Mat face, List<String> indicators) {
        Mat gray = new Mat();
        if (face.channels() > 1) {
            Imgproc.cvtColor(face, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = face.clone();
        }

        Scalar meanBrightness = Core.mean(gray);
        double brightness = meanBrightness.val[0];
        gray.release();

        if (brightness < BRIGHTNESS_MIN) {
            indicators.add("Too dark");
            return 0.5;
        } else if (brightness > BRIGHTNESS_MAX) {
            indicators.add("Over-exposed/washed out");
            return 0.5;
        }
        
        return 1.0; // Good brightness
    }

    private double analyzeEdges(Mat face, List<String> indicators) {
        Mat gray = new Mat();
        if (face.channels() > 1) {
            Imgproc.cvtColor(face, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = face.clone();
        }

        // Detect edges using Canny
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);
        
        // Count edge pixels
        int totalPixels = edges.rows() * edges.cols();
        int edgePixels = Core.countNonZero(edges);
        double edgeRatio = (double) edgePixels / totalPixels;
        
        gray.release();
        edges.release();

        // Printed photos often have too few or too many edges
        if (edgeRatio < 0.02) {
            indicators.add("Too smooth (printed?)");
            return 0.4;
        } else if (edgeRatio > 0.25) {
            indicators.add("Too many edges (screen glare?)");
            return 0.6;
        }
        
        return 1.0; // Normal edge distribution
    }

    /**
     * Result class for spoofing analysis
     */
    public static class SpoofingAnalysisResult {
        private final boolean isLive;
        private final double confidenceScore;
        private final String details;

        public SpoofingAnalysisResult(boolean isLive, double confidenceScore, String details) {
            this.isLive = isLive;
            this.confidenceScore = confidenceScore;
            this.details = details;
        }

        public boolean isLive() {
            return isLive;
        }

        public double getConfidenceScore() {
            return confidenceScore;
        }

        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return String.format("Live: %s, Score: %.1f%%, Details: %s", 
                isLive ? "YES" : "NO", confidenceScore, details);
        }
    }
}
