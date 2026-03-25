package com.mindscribe.service;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import org.springframework.stereotype.Service;

import java.nio.LongBuffer;
import java.util.Map;

@Service
public class SentimentAnalysisService {

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;
    private final boolean modelLoaded;
    private final String loadingError;

    public SentimentAnalysisService() {
        OrtEnvironment env = null;
        OrtSession sess = null;
        HuggingFaceTokenizer tok = null;
        boolean loaded = false;
        String error = "";
        
        try {
            System.out.println("🔍 Loading ONNX AI Model...");
            
            // Get absolute path to model files
            java.io.File modelFile = new java.io.File("model.onnx");
            java.io.File tokenizerFile = new java.io.File("tokenizer.json");
            String modelPath = modelFile.getAbsolutePath();
            
            System.out.println("📂 Model path: " + modelPath);
            System.out.println("📊 Model file size: " + modelFile.length() + " bytes");
            System.out.println("📊 Tokenizer file size: " + tokenizerFile.length() + " bytes");
            
            // Step 1: Load ONNX Runtime Environment
            env = OrtEnvironment.getEnvironment();
            System.out.println("✅ OrtEnvironment created");
            
            // Step 2: Create ONNX Session with the model
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            sess = env.createSession(modelPath, options);
            System.out.println("✅ ONNX Session created");
            
            // Step 3: Load HuggingFace Tokenizer from local file
            try {
                System.out.println(" Loading HuggingFace Tokenizer from local file...");
                
                // Get the path to your local tokenizer.json
                java.nio.file.Path tokenizerPath = java.nio.file.Paths.get("tokenizer.json");
                System.out.println(" Tokenizer path: " + tokenizerPath.toAbsolutePath());
                
                // Load the tokenizer from the local file instead of the Hub
                tok = HuggingFaceTokenizer.newInstance(tokenizerPath);
                System.out.println(" HuggingFace Tokenizer loaded successfully from local file");
                
            } catch (Exception tokenizerError) {
                System.err.println("❌ HuggingFace tokenizer failed: " + tokenizerError.getMessage());
                System.out.println("🔄 Using simple tokenization as backup");
                tok = null;
            }
            
            // Step 4: Verify model input/output names
            System.out.println("📋 Model Input Names: " + sess.getInputNames());
            System.out.println("📋 Model Output Names: " + sess.getOutputNames());
            
            loaded = true;
            System.out.println("🎉 ONNX AI Model loaded successfully!");
            
        } catch (Exception e) {
            loaded = false;
            error = "ONNX Model loading failed: " + e.getMessage();
            System.err.println("❌ " + error);
            e.printStackTrace();
        }
        
        this.environment = env;
        this.session = sess;
        this.tokenizer = tok;
        this.modelLoaded = loaded;
        this.loadingError = error;
    }
    
    // Helper method to create tokenizer from JSON content
    private HuggingFaceTokenizer createTokenizerFromJson(String jsonContent) throws Exception {
        // Try to use the tokenizer library's internal methods
        try {
            // Method 1: Try using the tokenizer's JSON parsing capability
            java.lang.reflect.Method method = HuggingFaceTokenizer.class.getDeclaredMethod("fromJson", String.class);
            method.setAccessible(true);
            return (HuggingFaceTokenizer) method.invoke(null, jsonContent);
        } catch (Exception e1) {
            try {
                // Method 2: Try creating from byte array
                java.lang.reflect.Method method = HuggingFaceTokenizer.class.getDeclaredMethod("fromJson", byte[].class);
                method.setAccessible(true);
                return (HuggingFaceTokenizer) method.invoke(null, jsonContent.getBytes());
            } catch (Exception e2) {
                // Method 3: All methods failed, throw exception
                throw new Exception("All tokenizer loading methods failed", e2);
            }
        }
    }

    public boolean isModelAvailable() {
        return modelLoaded;
    }

    public String getLoadingError() {
        return loadingError;
    }

    public String analyzeSentiment(String text) {
        if (!modelLoaded) {
            throw new RuntimeException("AI Model not available: " + loadingError);
        }

        try {
            System.out.println("🧠 Running ONNX inference for: " + text);
            
            // Tokenize input text
            long[] inputIds;
            long[] attentionMask;
            
            if (tokenizer != null) {
                // Use HuggingFace tokenizer
                var encoding = tokenizer.encode(text);
                inputIds = encoding.getIds();
                attentionMask = encoding.getAttentionMask();
                System.out.println("🔤 Using HuggingFace tokenizer: " + inputIds.length + " tokens");
                System.out.println("🔤 Token IDs: " + java.util.Arrays.toString(inputIds));
            } else {
                throw new RuntimeException("Tokenizer not available - cannot perform AI analysis");
            }
            
            System.out.println("🔤 Tokenized to " + inputIds.length + " tokens");
            
            // Create input tensors for ONNX (TinyBert needs both input_ids and attention_mask)
            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(inputIds), new long[]{1, inputIds.length});
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), new long[]{1, attentionMask.length});
            
            // Run ONNX inference with both inputs
            OrtSession.Result result = session.run(
                Map.of(
                    "input_ids", inputIdsTensor,
                    "attention_mask", attentionMaskTensor
                )
            );
            
            // Get output logits
            float[][] logits = (float[][]) result.get(0).getValue();
            System.out.println("📊 Model output logits: " + java.util.Arrays.toString(logits[0]));
            
            // Convert logits to sentiment
            String sentiment = logitsToSentiment(logits[0]);
            System.out.println("💭 Predicted sentiment: " + sentiment);
            
            return sentiment;
            
        } catch (Exception e) {
            System.err.println("❌ ONNX inference failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("AI analysis failed: " + e.getMessage());
        }
    }
    
    // Simple tokenization as backup
    private long[] tokenizeText(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        long[] tokens = new long[Math.min(words.length + 2, 128)];
        
        tokens[0] = 101; // [CLS] token ID
        for (int i = 0; i < Math.min(words.length, 126); i++) {
            tokens[i + 1] = Math.abs(words[i].hashCode()) % 30000 + 1000;
        }
        tokens[Math.min(words.length + 1, 127)] = 102; // [SEP] token ID
        
        return tokens;
    }

    public double getSentimentScore(String text) {
        if (!modelLoaded) {
            throw new RuntimeException("AI Model not available: " + loadingError);
        }
        
        try {
            // Tokenize input text
            long[] inputIds;
            long[] attentionMask;
            
            if (tokenizer != null) {
                var encoding = tokenizer.encode(text);
                inputIds = encoding.getIds();
                attentionMask = encoding.getAttentionMask();
            } else {
                throw new RuntimeException("Tokenizer not available - cannot calculate sentiment score");
            }
            
            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(inputIds), new long[]{1, inputIds.length});
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), new long[]{1, attentionMask.length});
            
            OrtSession.Result result = session.run(
                Map.of(
                    "input_ids", inputIdsTensor,
                    "attention_mask", attentionMaskTensor
                )
            );
            
            float[][] logits = (float[][]) result.get(0).getValue();
            
            // Convert logits to probability using softmax
            float[] probabilities = softmax(logits[0]);
            
            // Return probability of the predicted class
            int predictedIndex = argmax(logits[0]);
            return probabilities[predictedIndex];
            
        } catch (Exception e) {
            System.err.println("❌ Score calculation failed: " + e.getMessage());
            throw new RuntimeException("Sentiment score calculation failed: " + e.getMessage());
        }
    }
    
    private String logitsToSentiment(float[] logits) {
        int maxIndex = argmax(logits);
        return switch (maxIndex) {
            case 0 -> "negative";
            case 1 -> "neutral"; 
            case 2 -> "positive";
            default -> "neutral";
        };
    }
    
    private int argmax(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }
    
    private float[] softmax(float[] logits) {
        float[] exp = new float[logits.length];
        float sum = 0.0f;
        
        // Find max for numerical stability
        float max = logits[0];
        for (float logit : logits) {
            if (logit > max) max = logit;
        }
        
        // Compute exp and sum
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        
        // Normalize
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }
        
        return exp;
    }
}
