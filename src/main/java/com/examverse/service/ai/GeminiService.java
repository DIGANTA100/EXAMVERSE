package com.examverse.service.ai;

import com.examverse.config.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * GeminiService — Calls Google Gemini 1.5 Flash REST API.
 *
 * Maintains a conversation history so that multi-turn chat works properly.
 * Each instance represents one chat session. Create a new instance when the
 * user clicks "New Chat" or navigates away and back.
 *
 * Placement: src/main/java/com/examverse/service/ai/GeminiService.java
 */
public class GeminiService {

    // ── Gemini endpoint (Flash model — free tier, fast) ──────────────────────
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    // System context injected as the very first user message (Gemini doesn't
    // have a dedicated "system" role in the REST API the same way OpenAI does,
    // so we prepend a context message instead).
    private static final String SYSTEM_CONTEXT =
            "You are ExamVerse AI, a friendly and knowledgeable academic assistant " +
                    "built into the ExamVerse student portal. You help students with exam " +
                    "preparation, subject questions, study strategies, and understanding " +
                    "concepts. Keep answers clear, concise, and encouraging. " +
                    "If a student asks something completely unrelated to academics or studying, " +
                    "gently redirect them.";

    private final HttpClient httpClient;

    /**
     * Conversation history as a JSON-ready list of {role, text} pairs.
     * We build the full history JSON on every request (Gemini is stateless).
     */
    private final List<Turn> history = new ArrayList<>();

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        // Seed history with system context as a user+model exchange so Gemini
        // understands its persona from the very first message.
        history.add(new Turn("user",  SYSTEM_CONTEXT));
        history.add(new Turn("model", "Understood! I'm ExamVerse AI, ready to help you study and succeed. What would you like to know?"));
    }

    /**
     * Send a user message and return the assistant's reply.
     * This method is BLOCKING — call it from a background thread.
     *
     * @param userMessage The student's message
     * @return The AI reply text, or an error string starting with "ERROR:"
     */
    public String sendMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return "";

        // Add the user turn to history
        history.add(new Turn("user", userMessage));

        String requestBody = buildRequestJson();

        try {
            String apiKey = AppConfig.GEMINI_API_KEY;
            if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_API_KEY_HERE")) {
                return "ERROR: Gemini API key is not configured. Please add your key to AppConfig.GEMINI_API_KEY.";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String reply = extractText(response.body());
                // Add model turn to history so conversation context is maintained
                history.add(new Turn("model", reply));
                return reply;
            } else {
                // Remove the user turn we just added since it didn't succeed
                history.remove(history.size() - 1);
                return "ERROR: API returned status " + response.statusCode() +
                        ". Check your API key and quota.";
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            history.remove(history.size() - 1);
            return "ERROR: Request was interrupted.";
        } catch (Exception e) {
            history.remove(history.size() - 1);
            return "ERROR: " + e.getMessage();
        }
    }

    /** Clear conversation history (keep the system context seed). */
    public void clearHistory() {
        history.clear();
        history.add(new Turn("user",  SYSTEM_CONTEXT));
        history.add(new Turn("model", "Understood! I'm ExamVerse AI, ready to help you study and succeed. What would you like to know?"));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build the Gemini REST API JSON body with full conversation history.
     * Gemini uses the "contents" array with role + parts[].text structure.
     */
    private String buildRequestJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"contents\":[");

        for (int i = 0; i < history.size(); i++) {
            Turn t = history.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"role\":\"").append(t.role).append("\",");
            sb.append("\"parts\":[{\"text\":\"").append(escapeJson(t.text)).append("\"}]}");
        }

        sb.append("],");
        sb.append("\"generationConfig\":{");
        sb.append("\"temperature\":0.7,");
        sb.append("\"maxOutputTokens\":1024,");
        sb.append("\"topP\":0.9");
        sb.append("}}");

        return sb.toString();
    }

    /**
     * Extract the text content from Gemini's JSON response.
     * Gemini returns: {"candidates":[{"content":{"parts":[{"text":"..."}],...},...}],...}
     *
     * We do a simple manual parse to avoid pulling in a JSON library dependency.
     */
    private String extractText(String json) {
        try {
            // Find "text":" and extract the value
            int textIdx = json.indexOf("\"text\":");
            if (textIdx == -1) return "I couldn't generate a response. Please try again.";

            int start = json.indexOf("\"", textIdx + 7) + 1;
            int end   = findJsonStringEnd(json, start);

            if (start <= 0 || end <= start) return "Unexpected response format from AI.";

            return unescapeJson(json.substring(start, end));
        } catch (Exception e) {
            return "Error parsing AI response: " + e.getMessage();
        }
    }

    /** Find the end index of a JSON string value starting at {@code start}. */
    private int findJsonStringEnd(String json, int start) {
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') {
                i += 2; // skip escaped character
            } else if (c == '"') {
                return i;
            } else {
                i++;
            }
        }
        return -1;
    }

    /** Escape a Java string for embedding in JSON. */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Unescape JSON string escape sequences back to readable text. */
    private String unescapeJson(String text) {
        return text
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    // ── Inner record ─────────────────────────────────────────────────────────

    private static class Turn {
        final String role; // "user" or "model"
        final String text;
        Turn(String role, String text) { this.role = role; this.text = text; }
    }
}