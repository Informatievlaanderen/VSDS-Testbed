package be.vlaanderen.ldes.handlers;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Handle the requesting of datasets.
 */
@Component
public class DatasetRequestHandler {

    /**
     * Request a dataset. This is actually a generic implementation that could be used for any kind of HTTP GET.
     *
     * @param endpoint The endpoint to call.
     * @param contentType The content type to request from the LDES server.
     * @return A pair of returned content and HTTP status code.
     */
    public Pair<String, Integer> request(String endpoint, String contentType) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(endpoint))
                    .GET()
                    .header("Accept", contentType)
                    .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(String.format("Provided URI was invalid [%s].", endpoint), e);
        }
        try {
            var response = HttpClient.newBuilder()
                    .build().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return Pair.of(response.body(), response.statusCode());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(String.format("Error while posting dataset to endpoint [%s]", endpoint), e);
        }

    }

}
