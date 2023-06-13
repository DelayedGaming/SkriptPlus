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

    public static CompletableFuture<HttpResponse<String>> sendGetRequest(URL url) throws URISyntaxException {
        final HttpRequest request = HttpRequest.newBuilder(url.toURI()).build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static JsonObject parseResponse(String response) {
        Gson gson = new Gson();
        return gson.fromJson(response, JsonObject.class);
    }
}
