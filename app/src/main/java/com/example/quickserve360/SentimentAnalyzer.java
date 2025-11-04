package com.example.quickserve360;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;

import java.io.IOException;
import java.util.List;

public class SentimentAnalyzer {
    private static final String TAG = "SentimentAnalyzer";
    private Context context;
    private NLClassifier classifier;

    private static final String MODEL_FILE = "text_classification.tflite";

    public SentimentAnalyzer(Context context) {
        this.context = context;
        initializeModel();
    }

    private void initializeModel() {
        try {
            Log.d(TAG, "Initializing TensorFlow Lite model...");

            // Simple direct initialization
            classifier = NLClassifier.createFromFile(context, MODEL_FILE);
            Log.d(TAG, "✅ TensorFlow Lite model loaded SUCCESSFULLY!");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to load TensorFlow model: " + e.getMessage());
            // Don't throw exception - we'll handle it gracefully
        }
    }

    public boolean isModelLoaded() {
        return classifier != null;
    }

    public void analyzeSentiment(String reviewText, SentimentCallback callback) {
        if (reviewText == null || reviewText.isEmpty()) {
            callback.onSentimentAnalyzed(new SentimentResult(0.5f, "NEUTRAL", 0));
            return;
        }

        // If model didn't load, use keyword analysis
        if (classifier == null) {
            Log.w(TAG, "Model not loaded, using keyword analysis");
            SentimentResult result = analyzeWithKeywords(reviewText);
            callback.onSentimentAnalyzed(result);
            return;
        }

        new Thread(() -> {
            try {
                SentimentResult result = analyzeWithTensorFlow(reviewText);
                callback.onSentimentAnalyzed(result);
            } catch (Exception e) {
                Log.e(TAG, "TensorFlow analysis failed, using keywords: " + e.getMessage());
                SentimentResult result = analyzeWithKeywords(reviewText);
                callback.onSentimentAnalyzed(result);
            }
        }).start();
    }

    private SentimentResult analyzeWithTensorFlow(String reviewText) {
        try {
            Log.d(TAG, "Analyzing with TensorFlow: " + reviewText);

            List<Category> results = classifier.classify(reviewText);

            if (results == null || results.isEmpty()) {
                throw new RuntimeException("No results from model");
            }

            // Debug: log all results
            for (Category category : results) {
                Log.d(TAG, "Model output - " + category.getLabel() + ": " + category.getScore());
            }

            float positiveScore = 0f;
            float negativeScore = 0f;

            for (Category category : results) {
                String label = category.getLabel().toLowerCase();
                float score = category.getScore();

                if (label.contains("positive") || label.equals("1") || label.contains("pos")) {
                    positiveScore = score;
                } else if (label.contains("negative") || label.equals("0") || label.contains("neg")) {
                    negativeScore = score;
                }
            }

            float sentimentScore;
            if (positiveScore > 0) {
                sentimentScore = positiveScore;
            } else if (negativeScore > 0) {
                sentimentScore = 1 - negativeScore;
            } else {
                sentimentScore = results.get(0).getScore();
            }

            String label = classifySentiment(sentimentScore);
            Log.d(TAG, "✅ TensorFlow Result: " + label + " (" + sentimentScore + ")");

            return new SentimentResult(sentimentScore, label, 1);

        } catch (Exception e) {
            throw new RuntimeException("TensorFlow analysis error: " + e.getMessage());
        }
    }

    // Enhanced keyword-based fallback
    private SentimentResult analyzeWithKeywords(String text) {
        String lowerText = text.toLowerCase();
        int positive = 0, negative = 0;

        String[] positiveWords = {"good", "great", "excellent", "amazing", "awesome", "delicious",
                "love", "best", "perfect", "wonderful", "nice", "friendly", "fast"};
        String[] negativeWords = {"bad", "terrible", "horrible", "awful", "disgusting", "worst",
                "hate", "poor", "slow", "dirty", "rude", "overpriced"};

        for (String word : positiveWords) {
            if (lowerText.contains(word)) positive++;
        }
        for (String word : negativeWords) {
            if (lowerText.contains(word)) negative++;
        }

        float score;
        if (positive + negative == 0) {
            score = 0.5f; // Neutral if no keywords
        } else {
            score = (float) positive / (positive + negative);
        }

        String label = classifySentiment(score);
        Log.d(TAG, "🔑 Keyword Analysis: " + label + " (" + score + ") - Positive: " + positive + ", Negative: " + negative);

        return new SentimentResult(score, label, 1);
    }

    private String classifySentiment(float score) {
        if (score >= 0.7f) return "POSITIVE";
        else if (score <= 0.3f) return "NEGATIVE";
        else return "NEUTRAL";
    }

    public void close() {
        if (classifier != null) {
            classifier.close();
        }
    }

    public interface SentimentCallback {
        void onSentimentAnalyzed(SentimentResult result);
    }

    public static class SentimentResult {
        public float score;
        public String label;
        public int status;

        public SentimentResult(float score, String label, int status) {
            this.score = score;
            this.label = label;
            this.status = status;
        }
    }
}