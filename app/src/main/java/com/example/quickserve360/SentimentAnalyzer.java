package com.example.quickserve360;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;
import java.io.IOException;
import java.util.List;

public class SentimentAnalyzer {
    private static final String TAG = "SentimentAnalyzer-TF";
    private Context context;
    private NLClassifier classifier;

    // IMPORTANT: Use a proper sentiment analysis model
    // Download from: https://www.tensorflow.org/lite/examples/text_classification/overview
    // Recommended: "text_classification.tflite" or "sentiment_analysis.tflite"
    private static final String MODEL_FILE = "text_classification.tflite";

    // Enhanced sentiment word dictionary
    private static final String[] POSITIVE_WORDS = {
            "excellent", "amazing", "fantastic", "wonderful", "delicious",
            "awesome", "perfect", "love", "best", "outstanding", "great",
            "good", "nice", "fresh", "tasty", "enjoyed", "impressed",
            "recommend", "superb", "brilliant", "yummy", "lovely",
            "favorite", "happy", "satisfied", "quality", "friendly",
            "fast", "clean", "comfortable", "beautiful", "pleasant"
    };

    private static final String[] NEGATIVE_WORDS = {
            "terrible", "horrible", "awful", "disgusting", "worst", "bad",
            "poor", "disappointing", "hate", "gross", "tasteless", "rude",
            "slow", "dirty", "cold", "stale", "bland", "overpriced",
            "unhappy", "disappointing", "unsatisfied", "nasty", "mediocre",
            "average", "waste", "never", "avoid", "regret", "unfortunate"
    };

    // Negation words that flip sentiment
    private static final String[] NEGATION_WORDS = {
            "not", "no", "never", "neither", "nobody", "nothing",
            "nowhere", "hardly", "barely", "doesn't", "don't",
            "didn't", "won't", "wouldn't", "can't", "cannot"
    };

    public SentimentAnalyzer(Context context) {
        this.context = context;
        initializeModel();
    }

    /**
     * Initialize TensorFlow Lite Model
     */
    private void initializeModel() {
        // Skip TensorFlow for now - use keyword analysis
        // Uncomment below when you have a proper sentiment model
        /*
        try {
            classifier = NLClassifier.createFromFile(context, MODEL_FILE);
            Log.d(TAG, "TensorFlow Lite model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading TensorFlow model: " + e.getMessage());
            Log.w(TAG, "Falling back to keyword-based analysis");
            classifier = null;
        }
        */
        classifier = null;
        Log.d(TAG, "Using enhanced keyword-based sentiment analysis");
    }

    /**
     * Main sentiment analysis method - async callback
     */
    public void analyzeSentiment(String reviewText, SentimentCallback callback) {
        if (reviewText == null || reviewText.isEmpty()) {
            callback.onSentimentAnalyzed(new SentimentResult(0.5f, "NEUTRAL", 0));
            return;
        }

        // Run on background thread (ML processing can be slow)
        new Thread(() -> {
            try {
                SentimentResult result;

                if (classifier != null) {
                    // Use TensorFlow Lite model
                    result = analyzeSentimentWithTensorFlow(reviewText);
                } else {
                    // Fallback to keyword analysis
                    result = analyzeSentimentWithKeywords(reviewText);
                }

                callback.onSentimentAnalyzed(result);
            } catch (Exception e) {
                Log.e(TAG, "Sentiment analysis error: " + e.getMessage());
                // Use keyword fallback on error
                SentimentResult fallbackResult = analyzeSentimentWithKeywords(reviewText);
                callback.onSentimentAnalyzed(fallbackResult);
            }
        }).start();
    }

    /**
     * TensorFlow Lite Classification
     */
    private SentimentResult analyzeSentimentWithTensorFlow(String reviewText) {
        try {
            String processedText = reviewText.toLowerCase().trim();

            // Use TensorFlow Lite NLClassifier
            List<org.tensorflow.lite.support.label.Category> results = classifier.classify(processedText);

            if (results != null && !results.isEmpty()) {
                // Most sentiment models return categories like "Positive" and "Negative"
                float positiveScore = 0f;
                float negativeScore = 0f;

                for (org.tensorflow.lite.support.label.Category category : results) {
                    String label = category.getLabel().toLowerCase();
                    float score = category.getScore();

                    if (label.contains("positive") || label.equals("1")) {
                        positiveScore = score;
                    } else if (label.contains("negative") || label.equals("0")) {
                        negativeScore = score;
                    }
                }

                // Calculate sentiment score (0 = negative, 1 = positive)
                float sentimentScore = positiveScore;
                String label = classifySentiment(sentimentScore);

                Log.d(TAG, "TensorFlow Analysis - Positive: " + positiveScore +
                        " | Negative: " + negativeScore + " | Final Score: " + sentimentScore);

                return new SentimentResult(sentimentScore, label, 1);
            } else {
                // Fallback if model doesn't return results
                Log.w(TAG, "TensorFlow model returned no results, using keyword fallback");
                return analyzeSentimentWithKeywords(reviewText);
            }
        } catch (Exception e) {
            Log.e(TAG, "TensorFlow classification error: " + e.getMessage());
            return analyzeSentimentWithKeywords(reviewText);
        }
    }

    /**
     * Enhanced keyword-based sentiment analysis with negation handling
     */
    private SentimentResult analyzeSentimentWithKeywords(String reviewText) {
        String text = reviewText.toLowerCase();
        String[] words = text.split("\\s+");

        int positiveCount = 0;
        int negativeCount = 0;

        // Track negation
        boolean negationActive = false;
        int negationWindow = 0;

        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll("[^a-z]", "");

            // Check for negation words
            if (isNegationWord(word)) {
                negationActive = true;
                negationWindow = 3; // Negation affects next 3 words
                continue;
            }

            // Check sentiment
            boolean isPositive = isPositiveWord(word);
            boolean isNegative = isNegativeWord(word);

            if (isPositive || isNegative) {
                if (negationActive) {
                    // Flip sentiment due to negation
                    if (isPositive) {
                        negativeCount++;
                    } else {
                        positiveCount++;
                    }
                } else {
                    // Normal sentiment
                    if (isPositive) {
                        positiveCount++;
                    } else {
                        negativeCount++;
                    }
                }
            }

            // Decrease negation window
            if (negationActive) {
                negationWindow--;
                if (negationWindow <= 0) {
                    negationActive = false;
                }
            }
        }

        // Calculate sentiment score with improved formula
        float totalWords = positiveCount + negativeCount;
        float sentimentScore;

        if (totalWords == 0) {
            // No sentiment words found - analyze overall tone
            sentimentScore = 0.5f; // Neutral
        } else {
            // Score based on ratio of positive to total sentiment words
            sentimentScore = (float) positiveCount / totalWords;
        }

        // Apply intensity multiplier based on total sentiment words
        if (totalWords > 0) {
            float intensity = Math.min(totalWords / 5f, 1f); // Max intensity at 5+ words
            // Push score away from neutral based on intensity
            if (sentimentScore > 0.5f) {
                sentimentScore = 0.5f + (sentimentScore - 0.5f) * (0.5f + intensity * 0.5f);
            } else {
                sentimentScore = 0.5f - (0.5f - sentimentScore) * (0.5f + intensity * 0.5f);
            }
        }

        String label = classifySentiment(sentimentScore);

        Log.d(TAG, "Keyword Analysis - Text: \"" +
                reviewText.substring(0, Math.min(50, reviewText.length())) + "\"");
        Log.d(TAG, "Positive: " + positiveCount + " | Negative: " + negativeCount +
                " | Score: " + sentimentScore + " | Label: " + label);

        return new SentimentResult(sentimentScore, label, 1);
    }

    /**
     * Check if word is a negation word
     */
    private boolean isNegationWord(String word) {
        for (String negation : NEGATION_WORDS) {
            if (word.equals(negation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if word is positive
     */
    private boolean isPositiveWord(String word) {
        for (String positive : POSITIVE_WORDS) {
            if (word.equals(positive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if word is negative
     */
    private boolean isNegativeWord(String word) {
        for (String negative : NEGATIVE_WORDS) {
            if (word.equals(negative)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Classify sentiment based on score with adjusted thresholds
     */
    private String classifySentiment(float score) {
        if (score >= 0.6f) {
            return "POSITIVE";
        } else if (score <= 0.4f) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * Calculate adjusted restaurant rating
     */
    public float calculateAdjustedRating(float userRating, float sentimentScore) {
        float adjustment = (sentimentScore - 0.5f) * 0.5f;
        float adjusted = userRating + adjustment;
        adjusted = Math.max(1f, Math.min(5f, adjusted));

        Log.d(TAG, "Rating Adjustment - User: " + userRating +
                " | Sentiment: " + sentimentScore +
                " | Adjusted: " + adjusted);

        return adjusted;
    }

    /**
     * Callback interface for async sentiment analysis
     */
    public interface SentimentCallback {
        void onSentimentAnalyzed(SentimentResult result);
    }

    /**
     * Result class containing ML prediction
     */
    public static class SentimentResult {
        public float score;      // 0-1 (0=negative, 1=positive)
        public String label;     // POSITIVE, NEGATIVE, NEUTRAL
        public int status;       // 1 = success, 0 = failed

        public SentimentResult(float score, String label, int status) {
            this.score = score;
            this.label = label;
            this.status = status;
        }
    }
}