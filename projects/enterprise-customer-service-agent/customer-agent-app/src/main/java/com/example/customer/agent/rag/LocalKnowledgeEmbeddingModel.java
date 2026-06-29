package com.example.customer.agent.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * 本地确定性知识 embedding 模型。
 * <p>
 * 仅用于 Day 17 本地 RAG 闭环验证，不调用外部模型服务；后续可替换为真实 EmbeddingModel。
 *
 * @author jiangzhibin
 * @since 2026-06-29 20:15:00
 */
public class LocalKnowledgeEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 128;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        var embeddings = new ArrayList<Embedding>();
        var index = 0;
        for (var instruction : request.getInstructions()) {
            embeddings.add(new Embedding(vectorize(instruction), index++));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return vectorize(getEmbeddingContent(document));
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private float[] vectorize(String value) {
        var vector = new float[DIMENSIONS];
        var text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        addAsciiTokens(vector, text);
        addCjkTokens(vector, text);
        normalize(vector);
        return vector;
    }

    private void addAsciiTokens(float[] vector, String text) {
        var token = new StringBuilder();
        for (var index = 0; index < text.length(); index++) {
            var ch = text.charAt(index);
            if (Character.isLetterOrDigit(ch)) {
                token.append(ch);
            } else if (!token.isEmpty()) {
                addToken(vector, token.toString(), 1.0f);
                token.setLength(0);
            }
        }
        if (!token.isEmpty()) {
            addToken(vector, token.toString(), 1.0f);
        }
    }

    private void addCjkTokens(float[] vector, String text) {
        var cjkChars = new ArrayList<String>();
        for (var index = 0; index < text.length(); index++) {
            var ch = text.charAt(index);
            if (isCjk(ch)) {
                var token = String.valueOf(ch);
                cjkChars.add(token);
                addToken(vector, token, 0.35f);
            }
        }
        addCjkBigrams(vector, cjkChars);
    }

    private void addCjkBigrams(float[] vector, List<String> cjkChars) {
        for (var index = 0; index + 1 < cjkChars.size(); index++) {
            addToken(vector, cjkChars.get(index) + cjkChars.get(index + 1), 1.0f);
        }
    }

    private boolean isCjk(char value) {
        return Character.UnicodeScript.of(value) == Character.UnicodeScript.HAN;
    }

    private void addToken(float[] vector, String token, float weight) {
        var bucket = Math.floorMod(token.hashCode(), vector.length);
        vector[bucket] += weight;
    }

    private void normalize(float[] vector) {
        var sum = 0.0d;
        for (var value : vector) {
            sum += value * value;
        }
        if (sum == 0.0d) {
            return;
        }
        var norm = Math.sqrt(sum);
        for (var index = 0; index < vector.length; index++) {
            vector[index] = (float) (vector[index] / norm);
        }
    }
}
