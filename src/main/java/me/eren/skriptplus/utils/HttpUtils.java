package me.eren.skriptplus.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpUtils {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    public static CompletableFuture<HttpResponse<String>> sendGetRequest(URL url) {
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error while sending a get request.", e);
        }
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<String>> sendPostRequest(URL url, String data) {
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error while sending a post request.", e);
        }
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static JsonObject parseResponse(String response) {
        return GSON.fromJson(response, JsonObject.class);
    }
}
