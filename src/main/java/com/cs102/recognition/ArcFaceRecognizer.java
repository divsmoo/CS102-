package com.cs102.recognition;

import ai.onnxruntime.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * ArcFace-based face recognition using ONNX Runtime
 * Provides high-accuracy face embeddings for recognition
 */
public class ArcFaceRecognizer {

    private OrtEnvironment env;
    private OrtSession session;
    private static final int INPUT_SIZE = 112; // ArcFace standard input size
    private static final int EMBEDDING_SIZE = 512; // ArcFace embedding dimension

    /**
     * Initialize ArcFace model from resources
     */
    public ArcFaceRecognizer() throws Exception {
        // Try to load from resources
        java.io.InputStream is = getClass().getClassLoader()
            .getResourceAsStream("arc.onnx");

        if (is == null) {
            throw new Exception(
                "ArcFace model not found in resources!\n\n" +
                "Please ensure the model file exists:\n" +
                "1. File should be at: src/main/resources/arc.onnx\n" +
                "2. Run: mvn clean install\n" +
                "3. Restart application"
            );
        }

        // Extract to temp file
        java.io.File tempFile = java.io.File.createTempFile("arcface", ".onnx");
        tempFile.deleteOnExit();

        java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        is.close();

        String modelPath = tempFile.getAbsolutePath();

        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();

        // Optimize for inference
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        opts.setIntraOpNumThreads(4); // Use 4 threads for faster processing

        session = env.createSession(modelPath, opts);

        System.out.println("ArcFace model loaded successfully from resources");
        System.out.println("  Input names: " + session.getInputNames());
        System.out.println("  Output names: " + session.getOutputNames());
        System.out.println("  Input shape: [1, 3, " + INPUT_SIZE + ", " + INPUT_SIZE + "]");
        System.out.println("  Output embedding size: " + EMBEDDING_SIZE);
    }

    /**
     * Initialize ArcFace model with custom model path
     * @param modelPath Path to the ArcFace ONNX model file
     */
    public ArcFaceRecognizer(String modelPath) throws OrtException {
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();

        // Optimize for inference
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        opts.setIntraOpNumThreads(4); // Use 4 threads for faster processing

        session = env.createSession(modelPath, opts);

        System.out.println("ArcFace model loaded successfully");
        System.out.println("  Input shape: [1, 3, " + INPUT_SIZE + ", " + INPUT_SIZE + "]");
        System.out.println("  Output embedding size: " + EMBEDDING_SIZE);
    }

    /**
     * Preprocess face image for ArcFace
     * Converts BGR OpenCV Mat to normalized RGB float array
     */
    public Mat preprocessFace(Mat face) {
        // Resize to 112x112 (ArcFace standard)
        Mat resized = new Mat();
        Imgproc.resize(face, resized, new Size(INPUT_SIZE, INPUT_SIZE));

        // Convert to RGB (ArcFace expects RGB)
        Mat rgb = new Mat();
        if (resized.channels() == 1) {
            // Grayscale to RGB
            Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_GRAY2RGB);
        } else if (resized.channels() == 3) {
            // BGR to RGB
            Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_BGR2RGB);
        } else {
            rgb = resized;
        }

        return rgb;
    }

    /**
     * Extract face embedding from preprocessed face image
     * Thread-safe: synchronized to prevent concurrent ONNX session access
     * @param face Preprocessed face Mat (112x112 RGB)
     * @return 512-dimensional embedding vector
     */
    public synchronized float[] extractEmbedding(Mat face) throws OrtException {
        // Convert Mat to float array [1, 112, 112, 3] in HWC format
        float[] inputArray = matToFloatArray(face);

        // Create ONNX tensor with HWC shape (Height, Width, Channels)
        long[] shape = {1, INPUT_SIZE, INPUT_SIZE, 3};
        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputArray), shape);

        // Run inference
        Map<String, OnnxTensor> inputs = new HashMap<>();
        // Use the actual input name from the model (get first input name)
        String inputName = session.getInputNames().iterator().next();
        inputs.put(inputName, tensor);

        OrtSession.Result results = session.run(inputs);

        // Extract embedding
        float[][] output = (float[][]) results.get(0).getValue();
        float[] embedding = output[0];

        // Normalize embedding (L2 normalization)
        embedding = normalizeEmbedding(embedding);

        // Clean up
        tensor.close();
        results.close();

        return embedding;
    }

    /**
     * Convert OpenCV Mat to float array in HWC (Height, Width, Channel) format
     * Applies normalization: (pixel / 255.0 - 0.5) / 0.5
     */
    private float[] matToFloatArray(Mat mat) {
        int channels = mat.channels();
        int height = mat.rows();
        int width = mat.cols();

        float[] array = new float[height * width * channels];
        byte[] data = new byte[(int) mat.total() * channels];
        mat.get(0, 0, data);

        // Convert to HWC format with normalization
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                for (int c = 0; c < channels; c++) {
                    int pixelIndex = (h * width + w) * channels + c;
                    int arrayIndex = h * width * channels + w * channels + c;

                    // Normalize: (pixel / 255.0 - 0.5) / 0.5 = (pixel - 127.5) / 127.5
                    float pixelValue = (data[pixelIndex] & 0xFF); // Convert to unsigned
                    array[arrayIndex] = (pixelValue - 127.5f) / 127.5f;
                }
            }
        }

        return array;
    }

    /**
     * Normalize embedding using L2 normalization
     */
    private float[] normalizeEmbedding(float[] embedding) {
        float sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        float norm = (float) Math.sqrt(sum);

        float[] normalized = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / norm;
        }

        return normalized;
    }

    /**
     * Compute cosine similarity between two embeddings
     * Returns value between -1 and 1 (higher = more similar)
     * Typical threshold: 0.5 for same person
     */
    public static double cosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have same length");
        }

        double dotProduct = 0;
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
        }

        // Since embeddings are already normalized, dot product = cosine similarity
        return dotProduct;
    }

    /**
     * Find best match from a list of stored embeddings
     * @param queryEmbedding Embedding to match
     * @param storedEmbeddings Map of userId to list of embeddings
     * @param threshold Minimum similarity threshold (default 0.5)
     * @return userId of best match, or null if no match above threshold
     */
    public MatchResult findBestMatch(float[] queryEmbedding,
                                     Map<String, float[][]> storedEmbeddings,
                                     double threshold) {
        String bestUserId = null;
        double bestSimilarity = threshold;

        for (Map.Entry<String, float[][]> entry : storedEmbeddings.entrySet()) {
            String userId = entry.getKey();
            float[][] userEmbeddings = entry.getValue();

            // Compare with all stored embeddings for this user
            for (float[] storedEmbedding : userEmbeddings) {
                double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);

                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestUserId = userId;
                }
            }
        }

        if (bestUserId != null) {
            return new MatchResult(bestUserId, bestSimilarity);
        }

        return null;
    }

    /**
     * Close ONNX session and release resources
     */
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (OrtException e) {
                System.err.println("Error closing ONNX session: " + e.getMessage());
            }
        }
    }

    /**
     * Result of a face matching operation
     */
    public static class MatchResult {
        public final String userId;
        public final double similarity;

        public MatchResult(String userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }

        /**
         * Convert similarity (0-1) to confidence percentage (0-100)
         */
        public double getConfidencePercentage() {
            return similarity * 100.0;
        }
    }
}
