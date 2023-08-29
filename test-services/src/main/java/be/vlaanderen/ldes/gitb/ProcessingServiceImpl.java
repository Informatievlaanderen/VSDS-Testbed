package be.vlaanderen.ldes.gitb;

import be.vlaanderen.ldes.Utils;
import be.vlaanderen.ldes.handlers.CrawlHandler;
import be.vlaanderen.ldes.handlers.Crawler;
import be.vlaanderen.ldes.handlers.SparqlQueryHandler;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.ps.Void;
import com.gitb.ps.*;
import com.gitb.tr.TestResultType;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

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

                Model stream = ModelFactory
                        .createDefaultModel()
                        // @todo Use content type header of http response?
                        .read(IOUtils.toInputStream(streamData, "UTF-8"), null, "TURTLE");
                List<String> relations = new ArrayList<>();
                // @todo Select correct view when multiple views are available.
                String queryString = """
                    PREFIX tree: <https://w3id.org/tree#>
                    SELECT DISTINCT ?relation
                    WHERE {
                        ?node a tree:Node .
                        ?node tree:relation/tree:node ?relation.
                    }
                    """ ;
                Query query = QueryFactory.create(queryString) ;
                try (QueryExecution qexec = QueryExecutionFactory.create(query, stream)) {
                    ResultSet results = qexec.execSelect() ;
                    while (results.hasNext()) {
                        org.apache.jena.rdf.model.Resource relation = results.nextSolution().getResource("relation") ;
                        relations.add(relation.getURI());
                    }
                }
                AtomicReference<Boolean> hasCrawled = new AtomicReference<>(false);
                relations.forEach((String ViewURI) -> {
                    LOG.info("Received processing call for test session [{}].", processRequest.getSessionId());
                    if (ViewURI.contains(endpointType)) {
                        hasCrawled.set(true);
                        Crawler crawler = new Crawler(ViewURI);
                        LOG.info("Now crawling the View [{}]", ViewURI);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        RDFDataMgr.write(outputStream, crawler.run().getGraph(), Lang.TURTLE);
                        // Produce the resulting report.
                        response.getOutput().add(createAnyContentSimple("result", outputStream.toString(), ValueEmbeddingEnumeration.STRING));
                    }
                });
                if (!hasCrawled.get()) {
                    response.setReport(Utils.createReport(TestResultType.FAILURE));
                    LOG.error( "No matching view found in eventstream for [{}]", endpointType);
                }
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
