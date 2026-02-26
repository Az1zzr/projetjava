package utils;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Service de détection d'images sensibles / inappropriées.
 * Utilise l'API Claude Vision pour analyser les images avant upload.
 *
 * Usage :
 *   SensitiveImageDetector.Result result = SensitiveImageDetector.analyze(filePath);
 *   if (result.isSensitive()) { ... }
 */
public class SensitiveImageDetector {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-opus-4-5";
    // Stocker la clé dans la variable d'environnement ANTHROPIC_API_KEY
    private static final String API_KEY = System.getenv("ANTHROPIC_API_KEY");

    // ==============================================================
    // Résultat d'analyse
    // ==============================================================
    public static class Result {
        public enum Level { SAFE, WARNING, BLOCKED }

        private final boolean sensitive;
        private final Level   level;
        private final String  reason;
        private final String  rawResponse;

        public Result(boolean sensitive, Level level, String reason, String rawResponse) {
            this.sensitive   = sensitive;
            this.level       = level;
            this.reason      = reason;
            this.rawResponse = rawResponse;
        }

        public boolean isSensitive()    { return sensitive; }
        public Level   getLevel()       { return level; }
        public String  getReason()      { return reason; }
        public String  getRawResponse() { return rawResponse; }

        @Override
        public String toString() {
            return "[" + level + "] sensitive=" + sensitive + " | " + reason;
        }
    }

    // ==============================================================
    // Analyse synchrone
    // ==============================================================
    public static Result analyze(String imagePath) throws Exception {
        return analyze(new File(imagePath));
    }

    public static Result analyze(File imageFile) throws Exception {
        if (!imageFile.exists())
            throw new FileNotFoundException("Image introuvable : " + imageFile.getAbsolutePath());

        String mediaType = detectMediaType(imageFile.getName());
        byte[] bytes     = Files.readAllBytes(imageFile.toPath());
        String base64    = Base64.getEncoder().encodeToString(bytes);

        return callApi(base64, mediaType);
    }

    // ==============================================================
    // Analyse asynchrone (non-bloquant, idéal dans un JavaFX Task)
    // ==============================================================
    public static CompletableFuture<Result> analyzeAsync(File imageFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyze(imageFile);
            } catch (Exception e) {
                return new Result(false, Result.Level.WARNING,
                        "Analyse indisponible : " + e.getMessage(), "");
            }
        });
    }

    // ==============================================================
    // Appel API Claude Vision
    // ==============================================================
    private static Result callApi(String base64, String mediaType) throws Exception {
        if (API_KEY == null || API_KEY.isBlank())
            throw new IllegalStateException(
                    "Variable d'environnement ANTHROPIC_API_KEY manquante.");

        String body = buildRequestBody(base64, mediaType);

        HttpClient  client  = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type",     "application/json")
                .header("x-api-key",         API_KEY)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200)
            throw new IOException("API error " + resp.statusCode() + " : " + resp.body());

        return parseResponse(resp.body());
    }

    private static String buildRequestBody(String base64, String mediaType) {
        // Prompt demandant une réponse JSON stricte
        String text = "Tu es un moderateur de contenu pour une plateforme e-commerce. "
                    + "Analyse cette image et reponds UNIQUEMENT en JSON (aucun texte autour) : "
                    + "{\\\"sensitive\\\": true/false, "
                    + "\\\"level\\\": \\\"SAFE\\\" ou \\\"WARNING\\\" ou \\\"BLOCKED\\\", "
                    + "\\\"reason\\\": \\\"explication courte\\\"} "
                    + "SAFE=image normale. "
                    + "WARNING=contenu ambigu ou possiblement inappropriate. "
                    + "BLOCKED=nudite, pornographie, violence extreme, symboles haineux.";

        return "{"
             + "\"model\":\"" + MODEL + "\","
             + "\"max_tokens\":256,"
             + "\"messages\":[{"
             + "  \"role\":\"user\","
             + "  \"content\":["
             + "    {\"type\":\"image\","
             + "     \"source\":{\"type\":\"base64\","
             + "               \"media_type\":\"" + mediaType + "\","
             + "               \"data\":\"" + base64 + "\"}},"
             + "    {\"type\":\"text\",\"text\":\"" + text + "\"}"
             + "  ]"
             + "}]}";
    }

    // ==============================================================
    // Parsing de la réponse JSON sans librairie externe
    // ==============================================================
    private static Result parseResponse(String apiResponse) {
        try {
            // Extraire le champ "text" retourné par Claude
            int s = apiResponse.indexOf("\"text\":\"");
            if (s == -1) throw new Exception("Champ 'text' absent dans la reponse API");
            s += 8;
            int e = apiResponse.indexOf("\"}", s);
            String json = apiResponse.substring(s, e)
                    .replace("\\n", " ").replace("\\\"", "\"").trim();

            boolean sensitive = json.contains("\"sensitive\":true")
                             || json.contains("\"sensitive\": true");

            String level = "SAFE";
            if      (json.contains("BLOCKED")) level = "BLOCKED";
            else if (json.contains("WARNING")) level = "WARNING";

            String reason = extractJsonString(json, "reason");
            if (reason.isEmpty()) reason = "Aucune raison fournie";

            return new Result(sensitive, Result.Level.valueOf(level), reason, apiResponse);

        } catch (Exception ex) {
            return new Result(false, Result.Level.WARNING,
                    "Erreur parsing : " + ex.getMessage(), apiResponse);
        }
    }

    /** Extrait la valeur d'une clé string dans un JSON simple. */
    private static String extractJsonString(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i == -1) return "";
        i = json.indexOf("\"", i + key.length() + 2);
        if (i == -1) return "";
        i++;
        int end = json.indexOf("\"", i);
        return end > i ? json.substring(i, end) : "";
    }

    // ==============================================================
    // Utilitaire — type MIME
    // ==============================================================
    private static String detectMediaType(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(".png"))  return "image/png";
        if (f.endsWith(".gif"))  return "image/gif";
        if (f.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
