package be.vlaanderen.ldes.handlers;

import be.vlaanderen.ldes.EndpointTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

/**
 * Handle data crawling.
 */
@Component
public class CrawlHandler {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(CrawlHandler.class);

    /**
     * Carry out a crawl.
     *
     * @param streamData The data of the stream's definition.
     * @param contentType The content type of the stream's definition.
     * @param endpointType The type of endpoint to consider.
     * @return The resulting crawled graph.
     */
    public String crawl(String streamData, String contentType, EndpointTypeEnum endpointType) {
        Objects.requireNonNull(endpointType);
        LOG.info("Requested to crawl for endpoint [{}]", endpointType.getType());
        /*
         * This is currently a dummy implementation that just returns the expected datasets that would be produced by the
         * crawling. This is where one would expect to add a Java implementation of the Scrappy crawling logic that was
         * present in the initial python project.
         */
        var resourcePath = switch (endpointType) {
            case BY_TIME -> "data/items_timebased.ttl";
            case BY_LOCATION -> "data/items_geospatial.ttl";
            default -> throw new IllegalArgumentException(String.format("Mock crawling of endpoint type [%s] not supported yet.", endpointType.getType()));
        };
        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            return new String(Objects.requireNonNull(stream).readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading mocked crawl response.", e);
        }
    }

}
