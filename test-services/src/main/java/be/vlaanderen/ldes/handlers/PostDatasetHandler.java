package be.vlaanderen.ldes.handlers;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * Handle the posting of datasets.
 */
@Component
public class PostDatasetHandler {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PostDatasetHandler.class);

    /**
     * Post several dataset files to the LDES server defined in the provided ZIP archive.
     *
     * @param zipArchive The ZIP archive containing the dataset files to post.
     * @param contentType The content type of all dataset files.
     * @param endpoint The endpoint to post to.
     * @return The list of result pairs (dataset file name and HTTP status code).
     */
    public List<Pair<String, PostResult>> postDatasets(byte[] zipArchive, String contentType, String endpoint) {
        var results = new ArrayList<Pair<String, PostResult>>();
        try (var zis = new ZipInputStream(new ByteArrayInputStream(zipArchive))) {
            var zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                var entryBytes = zis.readAllBytes();
                var result = post(entryBytes, contentType, endpoint);
                LOG.info("Posted dataset [{}] to [{}] as [{}] with response code [{}].", zipEntry.getName(), endpoint, contentType, result.status);
                results.add(Pair.of(zipEntry.getName(), result));
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Error while processing dataset ZIP Archive", e);
        }
        return results;
    }

    /**
     * Post a single dataset file.
     *
     * @param dataset The dataset to post.
     * @param contentType The dataset content type for the request.
     * @param endpoint The endpoint to post to.
     * @return The result of the post.
     */
    public PostResult post(byte[] dataset, String contentType, String endpoint) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(endpoint))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(dataset))
                    .header("Content-Type", contentType)
                    .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(String.format("Provided URI was invalid [%s].", endpoint), e);
        }
        try {
            var response = HttpClient.newBuilder()
                    .build().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new PostResult(response.statusCode(), response.body());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(String.format("Error while posting dataset to endpoint [%s]", endpoint), e);
        }
    }

    /**
     * Record reflecting a POST result.
     *
     * @param status The HTTP status code.
     * @param body The response's body.
     */
    public record PostResult(int status, String body) {}

}
