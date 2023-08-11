package me.eren.skriptplus.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpUtils {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final String HASTEBIN_SERVER = "https://ptero.co/";

    public static CompletableFuture<HttpResponse<String>> sendGetRequest(URL url) {
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder(url.toURI()).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error while sending a get request.", e);
        }
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<String>> sendPostRequest(URL url, String body) {
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder(url.toURI()).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error while sending a get request.", e);
        }
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static JsonObject parseResponse(String response) {
        return GSON.fromJson(response, JsonObject.class);
    }

    public static void uploadLogs(Player player, String body) {
        URL url;

        try {
            url = new URL(HASTEBIN_SERVER + "documents");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error while uploading logs.", e);
        }

        CompletableFuture<HttpResponse<String>> future = HttpUtils.sendPostRequest(url, body);

        future.thenAccept(response -> {
            String responseBody = response.body();
            JsonObject jsonBody = parseResponse(responseBody);

            if (jsonBody.has("key")) {
                player.sendMessage(HASTEBIN_SERVER + jsonBody.get("key").getAsString());
            } else {
                throw new RuntimeException("Incorrect response from the Hastebin server");
            }
        });
    }
}
