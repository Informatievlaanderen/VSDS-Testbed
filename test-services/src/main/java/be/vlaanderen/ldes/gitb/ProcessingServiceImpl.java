package be.vlaanderen.ldes.gitb;

import be.vlaanderen.ldes.EndpointTypeEnum;
import be.vlaanderen.ldes.Utils;
import be.vlaanderen.ldes.handlers.CrawlHandler;
import be.vlaanderen.ldes.handlers.SparqlQueryHandler;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.ps.Void;
import com.gitb.ps.*;
import com.gitb.tr.TestResultType;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static be.vlaanderen.ldes.Utils.createAnyContentSimple;
import static be.vlaanderen.ldes.Utils.getRequiredString;

/**
 * Implementation of the GITB processing API to handle processing calls.
 */
@Component
public class ProcessingServiceImpl implements ProcessingService {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingServiceImpl.class);

    @Autowired
    private CrawlHandler crawlHandler;
    @Autowired
    private SparqlQueryHandler sparqlQueryHandler;
    @Resource
    private WebServiceContext wsContext;

    /**
     * This method normally returns documentation on how the service is expected to be used. It is meaningful
     * to implement this if this service would be a published utility that other test developers would want to
     * query and reuse. As it is an internal service we can skip this and return an empty implementation.
     *
     * @param aVoid No parameters.
     * @return The result.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void aVoid) {
        return new GetModuleDefinitionResponse();
    }

    /**
     * Called when a "process" step is executed from a test case.
     * <p>
     * This method is expected to read the (optional) requested operation and its expected (optional) inputs and then trigger
     * whatever processing is needed. When completing the method may return data as part of its outputs and a validation report.
     *
     * @param processRequest The request's parameters.
     * @return The results.
     */
    @Override
    public ProcessResponse process(ProcessRequest processRequest) {
        LOG.info("Received processing call for test session [{}].", processRequest.getSessionId());
        Objects.requireNonNull(processRequest.getOperation(), "No processing operation was provided.");
        ProcessResponse response = new ProcessResponse();
        response.setReport(Utils.createReport(TestResultType.SUCCESS));
        // Processing services define an optional "operation" as a convenience to determine what they are expected to do.
        switch (processRequest.getOperation()) {
            case "crawl" -> {
                /*
                 * The "crawl" operation is used to crawl the endpoint and produce a complete graph.
                 */
                // Get the expected inputs.
                var streamData = getRequiredString(processRequest.getInput(), "streamData");
                var contentType = getRequiredString(processRequest.getInput(), "contentType");
                var endpointType = getRequiredString(processRequest.getInput(), "endpointType");
                // Trigger the processing.
                var result = crawlHandler.crawl(streamData, contentType, EndpointTypeEnum.fromType(endpointType));
                // Produce the resulting report.
                response.getOutput().add(createAnyContentSimple("result", result, ValueEmbeddingEnumeration.STRING));
            }
            case "sparqlSelect" -> {
                /*
                 * The "sparqlSelect" operation is used to execute a SPARQL select query against a provided RDF graph.
                 */
                // Get the expected inputs.
                var content = getRequiredString(processRequest.getInput(), "content");
                var contentType = getRequiredString(processRequest.getInput(), "contentType");
                var query = getRequiredString(processRequest.getInput(), "query");
                // Trigger the processing.
                var result = sparqlQueryHandler.select(content, contentType, query);
                // Produce the resulting report.
                response.getOutput().add(createAnyContentSimple("result", result, ValueEmbeddingEnumeration.STRING));
            }
            default -> throw new IllegalArgumentException(String.format("Unexpected operation [%s].", processRequest.getOperation()));
        }
        LOG.info("Completed operation [{}] for test session [{}].", processRequest.getOperation(), processRequest.getSessionId());
        return response;
    }

    /**
     * Called if we use transactional processing operations for stateful operations (you can ignore this).
     *
     * @param beginTransactionRequest The request.
     * @return The response.
     */
    @Override
    public BeginTransactionResponse beginTransaction(BeginTransactionRequest beginTransactionRequest) {
        return new BeginTransactionResponse();
    }

    /**
     * Called if we use transactional processing operations for stateful operations (you can ignore this).
     *
     * @param parameters The request.
     * @return The empty response.
     */
    @Override
    public Void endTransaction(BasicRequest parameters) {
        return new Void();
    }

}
