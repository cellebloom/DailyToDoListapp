package com.example.dailytodolist;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Transport for the optional local AI endpoints of the companion Python server:
 * {@code POST /ai-schedule} (auto-schedule) and {@code POST /ai-chat} (Q&amp;A).
 *
 * <p><b>Scope.</b> AI-assisted scheduling is a 2.0 feature (PRD §5). This class
 * is the transport only: it knows how to turn a list of task names into a
 * proposed schedule and nothing about storage, UI, or when to call it. Nothing
 * in 1.1 depends on it, and it must stay isolated until 1.1 closes.</p>
 *
 * <p><b>Why no Retrofit.</b> The project already ships Gson and needs no
 * annotation processor for one endpoint, so a plain {@link HttpURLConnection}
 * keeps the dependency list unchanged and the build offline-friendly. Retrofit
 * was referenced from this file without being declared in
 * {@code app/build.gradle.kts}, which broke the compile.</p>
 *
 * <p><b>Threading.</b> Every method here performs blocking network I/O and will
 * throw {@link android.os.NetworkOnMainThreadException} if called from the main
 * thread. Call it from a background thread or executor and post the result back.</p>
 */
public final class AIService {

    /**
     * Default server address. {@code 10.0.2.2} is the host machine as seen from
     * the Android emulator; a physical device needs the LAN address of the box
     * running the Python server instead, so callers may override it.
     */
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:8000/";

    /** Endpoint path, relative to the base URL. */
    public static final String SCHEDULE_PATH = "ai-schedule";

    /** Q&A chat endpoint path, relative to the base URL. */
    public static final String CHAT_PATH = "ai-chat";

    /** Longer read timeout for chat: local LLM inference can be slow. */
    private static final int CHAT_TIMEOUT_MS = 60_000;

    private static final int TIMEOUT_MS = 15_000;

    private static final String TAG = "AIService";

    private AIService() {
        // Static transport helper; no instance state.
    }

    /** Schedules against {@link #DEFAULT_BASE_URL}. */
    public static ScheduleResponse getSchedule(ScheduleRequest request) throws IOException {
        return getSchedule(DEFAULT_BASE_URL, request);
    }

    /**
     * Asks the server for a proposed schedule.
     *
     * @param baseUrl server root, with or without a trailing slash
     * @param request task names plus past reviews, never null
     * @return the parsed response; never null, but its fields may be empty
     * @throws IOException on any transport failure, non-2xx status, or a body
     *                     that is not the JSON the endpoint promises
     */
    public static ScheduleResponse getSchedule(String baseUrl, ScheduleRequest request)
            throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("request == null");
        }
        String endpoint = normalizeBaseUrl(baseUrl) + SCHEDULE_PATH;
        String payload = new Gson().toJson(request);
        // Diagnostic: proves on-device how many names actually left the app and
        // in what order. If the server stacks everything at one hour, comparing
        // this line with the response line below says which side is at fault.
        Log.d(TAG, "ai-schedule request (" + request.tasks.size()
                + " tasks): " + payload);
        String text = postJson(endpoint, payload, TIMEOUT_MS);
        Log.d(TAG, "ai-schedule response: " + text);

        ScheduleResponse response;
        try {
            response = new Gson().fromJson(text, ScheduleResponse.class);
        } catch (JsonSyntaxException e) {
            // e.g. the model returned {"schedule":{...}} instead of an array:
            // Gson throws "Expected BEGIN_ARRAY". Log the raw body before it
            // is turned into a user-facing failure, then fail through the
            // normal IOException path the caller already handles.
            Log.e(TAG, "Failed to parse AI schedule response. Raw JSON: " + text, e);
            throw new IOException("AI scheduler returned malformed JSON", e);
        }
        return response == null ? new ScheduleResponse() : response;
    }

    /** Asks the Q&A endpoint against {@link #DEFAULT_BASE_URL}. */
    public static ChatResponse ask(String question, String contextData) throws IOException {
        return ask(DEFAULT_BASE_URL, question, contextData, null);
    }

    /** Convenience overload that sends conversation history. */
    public static ChatResponse ask(String question, String contextData,
                                   List<ChatMessage> history) throws IOException {
        return ask(DEFAULT_BASE_URL, question, contextData, history);
    }

    /**
     * Asks the "DailyToDo-AI" a question with the user's tasks as context.
     *
     * <p>Blocking network I/O — call from a background thread. The endpoint
     * expects {@code {"question": ..., "context_data": ..., "history": [...]}}
     * and returns an {@code answer} plus, optionally, an {@code action}
     * describing a task to create. Both shapes are accepted: a nested
     * {@code "action": {...}} object or action fields at the top level (with
     * {@code "action"} as a string discriminator). An action needs a non-empty
     * {@code task_name} to count.</p>
     *
     * @param history prior turns, oldest first; may be null on a first message
     * @return the parsed reply; never null, but its fields may be empty
     * @throws IOException on transport failure, non-2xx status, or bad JSON
     */
    public static ChatResponse ask(String baseUrl, String question, String contextData,
                                   List<ChatMessage> history) throws IOException {
        String endpoint = normalizeBaseUrl(baseUrl) + CHAT_PATH;
        ChatRequest request = new ChatRequest(question, contextData, history);
        String text = postJson(endpoint, new Gson().toJson(request), CHAT_TIMEOUT_MS);

        try {
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            ChatResponse reply = new ChatResponse();
            reply.answer = jsonString(root, "answer");

            JsonObject actionObject = null;
            JsonElement actionElement = root.get("action");
            if (actionElement != null && actionElement.isJsonObject()) {
                actionObject = actionElement.getAsJsonObject();
            } else if (actionElement != null && actionElement.isJsonPrimitive()
                    && actionElement.getAsJsonPrimitive().isString()) {
                // "action" as a string holding the object (some models do this).
                actionObject = parseJsonObject(actionElement.getAsString());
            }
            if (actionObject == null && root.has("task_name")) {
                // Flat shape: start_hour / end_hour / task_name live at the root.
                actionObject = root;
            }
            if (actionObject == null) {
                // Last resort: a local model sometimes answers with the action as
                // prose/fenced JSON instead of filling the action field. Parse the
                // answer text itself so the task still gets created rather than
                // the user being told about a task that was never saved.
                actionObject = parseJsonObject(reply.answer);
            }
            if (actionObject != null) {
                ChatAction parsed = new Gson().fromJson(actionObject, ChatAction.class);
                if (parsed != null && !parsed.getTaskName().isEmpty()) {
                    reply.action = parsed;
                }
            }
            return reply;
        } catch (JsonSyntaxException | IllegalStateException e) {
            Log.e(TAG, "Failed to parse AI chat response. Raw JSON: " + text, e);
            throw new IOException("AI chat returned malformed JSON", e);
        }
    }

    /**
     * Best-effort extraction of a JSON object from model text.
     *
     * <p>Handles a bare object, one wrapped in a ``` fence, and one surrounded
     * by prose ("Sure! {...}"). Returns null when no complete object is found, so
     * a genuinely plain-text reply still behaves as plain text.</p>
     */
    private static JsonObject parseJsonObject(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String text = raw.trim();
        // Only look for a task payload; without this, any JSON-looking prose
        // would be handed to ChatAction.
        if (!text.contains("task_name")) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(text.substring(start, end + 1));
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Reads a primitive string field, or "" when absent / not a primitive. */
    private static String jsonString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return "";
        }
        return element.getAsString();
    }

    /**
     * Shared POST of a JSON body, returning the response body as text. Non-2xx
     * responses read the error stream so the server's detail reaches the caller.
     */
    private static String postJson(String endpoint, String json, int readTimeoutMs)
            throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = null;
        try {
            // URI first: new URL(String) is deprecated from Java 20 onward.
            connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setFixedLengthStreamingMode(body.length);

            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }

            int status = connection.getResponseCode();
            String text = readFully(status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            if (status < 200 || status >= 300) {
                String detail = text.isEmpty() ? "" : ": " + text;
                throw new IOException("AI server returned HTTP " + status + detail);
            }
            return text;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Adds the trailing slash so {@code http://host:8000} and {@code .../} behave alike. */
    private static String normalizeBaseUrl(String baseUrl) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.isEmpty()) {
            throw new IllegalArgumentException("baseUrl is empty");
        }
        return base.endsWith("/") ? base : base + "/";
    }

    private static String readFully(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}

/** Request body: the tasks to schedule plus how recent days were reviewed. */
class ScheduleRequest {

    final List<String> tasks;

    @SerializedName("past_reviews")
    final List<String> pastReviews;

    ScheduleRequest(List<String> tasks, List<String> pastReviews) {
        this.tasks = tasks == null ? new ArrayList<String>() : new ArrayList<>(tasks);
        this.pastReviews = pastReviews == null
                ? new ArrayList<String>()
                : new ArrayList<>(pastReviews);
    }

    List<String> getTasks() { return tasks; }

    List<String> getPastReviews() { return pastReviews; }
}

/** Q&A request body. Field names are snake_case to match the Python model. */
class ChatRequest {

    final String question;

    @SerializedName("context_data")
    final String contextData;

    /**
     * Prior turns, oldest first. The server needs these so a follow-up like
     * "and at 10?" resolves against what was just said. Never null: an empty
     * list is what a first message sends.
     */
    final List<ChatMessage> history;

    ChatRequest(String question, String contextData, List<ChatMessage> history) {
        this.question = question == null ? "" : question;
        this.contextData = contextData == null ? "" : contextData;
        this.history = history == null ? new ArrayList<ChatMessage>() : new ArrayList<>(history);
    }
}

/**
 * One conversation turn lives in {@link ChatMessage} (its own file, because it
 * is public and Java requires a public type to match its file name).
 */

/** Q&A response body: an answer plus an optional add-task action. */
class ChatResponse {

    String answer;

    ChatAction action;

    String getAnswer() {
        return answer == null ? "" : answer;
    }
}

/**
 * An "add task" action from the chat endpoint.
 *
 * <p>{@code start_hour == -1} means the user gave no time: an Anytime task.
 * Gson leaves absent numeric fields at 0, so the sentinel is defaulted in the
 * constructor — a missing hour must never become a real 00:00 task.</p>
 */
class ChatAction {

    @SerializedName("task_name")
    String taskName;

    @SerializedName("start_hour")
    int startHour = TaskWithDate.NO_TIME;

    @SerializedName("end_hour")
    int endHour = TaskWithDate.NO_TIME;

    @SerializedName("category")
    String category;

    @SerializedName("top_priority")
    boolean topPriority;

    @SerializedName("date")
    String date;

    String getTaskName() {
        return taskName == null ? "" : taskName.trim();
    }

    int getStartHour() { return startHour; }

    int getEndHour() { return endHour; }
}

/** Response body. Exactly one of {@link #getSchedule()} and the error is set. */
class ScheduleResponse {

    List<ScheduleItem> schedule;

    String error;

    /** Proposed blocks, empty rather than null when the server sent none. */
    List<ScheduleItem> getSchedule() {
        return schedule == null ? new ArrayList<ScheduleItem>() : schedule;
    }

    /** Server-side explanation when {@link #getSchedule()} is empty; never null. */
    String getError() {
        return error == null ? "" : error;
    }

    boolean isSuccess() {
        return error == null && schedule != null && !schedule.isEmpty();
    }
}

/** One proposed block: which task, and the hour range to place it in. */
class ScheduleItem {

    @SerializedName("task_name")
    String taskName;

    @SerializedName("start_hour")
    int startHour;

    @SerializedName("end_hour")
    int endHour;

    String getTaskName() { return taskName == null ? "" : taskName; }

    int getStartHour() { return startHour; }

    int getEndHour() { return endHour; }
}
