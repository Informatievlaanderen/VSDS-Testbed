package be.vlaanderen.ldes.gitb;

import be.vlaanderen.ldes.handlers.DatasetRequestHandler;
import be.vlaanderen.ldes.handlers.PostDatasetHandler;
import com.apicatalog.jsonld.StringUtils;
import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.ms.Void;
import com.gitb.ms.*;
import com.gitb.tr.TestResultType;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static be.vlaanderen.ldes.Utils.*;

/**
 * Implementation of the GITB messaging API to handle messaging calls.
 */
@Component
public class MessagingServiceImpl implements MessagingService {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(MessagingServiceImpl.class);

    @Resource
    private WebServiceContext wsContext;
    @Autowired
    private PostDatasetHandler postDatasetHandler;
    @Autowired
    private DatasetRequestHandler datasetRequestHandler;

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
     * Called when a test session is being initialised and before it executes.
     * <p>
     * One of the main things this method does is to generate a unique identifier that will be used by the Test Bed
     * in subsequent calls to help the service understand that the calls link to the same test session. If you don't
     * set a specific session ID in the response the Test Bed will use by default the test session ID.
     *
     * @param initiateRequest The call's parameters.
     * @return The response.
     */
    @Override
    public InitiateResponse initiate(InitiateRequest initiateRequest) {
        LOG.info("Initiating new test session [{}].", getTestSessionIdFromHeaders(wsContext).orElse(""));
        return new InitiateResponse();
    }

    /**
     * Called when a "send" step is executed.
     * <p>
     * What we typically do here is to retrieve input parameters provided by the test case and trigger whatever needs
     * to happen to carry out a "send". As part of its response, this method also returns a report (TAR) that includes
     * optional report items (if e.g. errors were encountered) and data in the report's "context". This context data
     * is displayed on the UI as part of the step's report and is recorded in the test session's context for use in
     * subsequent steps.
     *
     * @param sendRequest The request's parameters.
     * @return The response.
     */
    @Override
    public SendResponse send(SendRequest sendRequest) {
        LOG.info("Called 'send' from test session [{}].", sendRequest.getSessionId());
        SendResponse response = new SendResponse();
        response.setReport(createReport(TestResultType.SUCCESS));
        // We use an input named "type" to tell us what kind of processing we are expected to do.
        var type = getOptionalString(sendRequest.getInput(), "type");
        if (type.isPresent()) {
            if ("ingest".equals(type.get())) {
                /*
                 * The "ingest" case is called when we provide the test dataset for the target LDES server to ingest.
                 */
                // Get the expected inputs.
                var datasetZipArchive = getRequiredBinary(sendRequest.getInput(), "datasets");
                var endpoint = getRequiredString(sendRequest.getInput(), "endpoint");
                var contentType = getRequiredString(sendRequest.getInput(), "contentType");
                // Trigger the related action.
                var results = postDatasetHandler.postDatasets(datasetZipArchive, contentType, endpoint);
                // Produce the resulting report.
                var resultsItem = new AnyContent();
                resultsItem.setName("datasets");
                resultsItem.setType("list");
                for (var result: results) {
                    if (result.getRight().status() >= 400) {
                        response.getReport().setResult(TestResultType.FAILURE);
                    }
                    var resultItem = new AnyContent();
                    resultItem.setType("map");
                    resultItem.getItem().add(createAnyContentSimple("file", result.getLeft(), ValueEmbeddingEnumeration.STRING));
                    resultItem.getItem().add(createAnyContentSimple("status", String.valueOf(result.getRight().status()), ValueEmbeddingEnumeration.STRING));
                    if (StringUtils.isNotBlank(result.getRight().body())) {
                        resultItem.getItem().add(createAnyContentSimple("body", result.getRight().body(), ValueEmbeddingEnumeration.STRING));
                    }
                    // Do not record this information in the test session context (it is only for display purposes).
                    resultItem.setForContext(false);
                    resultsItem.getItem().add(resultItem);
                }
                response.getReport().getContext().getItem().add(resultsItem);
            } else if ("post".equals(type.get())) {
                /*
                 * The "post" case is used to make a generic HTTP post to the LDES server.
                 */
                // Get the expected inputs.
                var endpoint = getRequiredString(sendRequest.getInput(), "endpoint");
                var content = getRequiredString(sendRequest.getInput(), "content");
                var contentName = getRequiredString(sendRequest.getInput(), "contentName");
                var contentType = getRequiredString(sendRequest.getInput(), "contentType");
                // Trigger the related action.
                var result = postDatasetHandler.post(content.getBytes(StandardCharsets.UTF_8), contentType, endpoint);
                // Produce the resulting report.
                if (result.status() == 400) {
                    /*
                     * The sample LDES server returns a 400 code when you try to re-create the stream and views. I assume
                     * this is implementation-specific, so I would expect this to be eventually changed.
                     */
                    response.getReport().setResult(TestResultType.WARNING);
                } else if (result.status() > 400) {
                    response.getReport().setResult(TestResultType.FAILURE);
                }
                var resultItem = new AnyContent();
                resultItem.setType("map");
                resultItem.getItem().add(createAnyContentSimple("file", contentName, ValueEmbeddingEnumeration.STRING));
                resultItem.getItem().add(createAnyContentSimple("status", String.valueOf(result.status()), ValueEmbeddingEnumeration.STRING));
                if (StringUtils.isNotBlank(result.body())) {
                    resultItem.getItem().add(createAnyContentSimple("body", result.body(), ValueEmbeddingEnumeration.STRING));
                }
                // Do not record this information in the test session context (it is only for display purposes).
                resultItem.setForContext(false);
                response.getReport().getContext().getItem().add(resultItem);
            }
        } else {
            /*
             * The default case we assume is a GET access on the LDES server's access endpoint.
             */
            // Get the expected inputs.
            var endpoint = getRequiredString(sendRequest.getInput(), "endpoint");
            var contentType = getRequiredString(sendRequest.getInput(), "contentType");
            // Trigger the related action.
            var result = datasetRequestHandler.request(endpoint, contentType);
            // Produce the resulting report.
            if (result.getRight() >= 400) {
                throw new IllegalStateException(String.format("The LDES server responded with an error code [%s]", result.getRight()));
            } else {
                response.getReport().getContext().getItem().add(createAnyContentSimple("response", result.getLeft(), ValueEmbeddingEnumeration.STRING));
            }
        }
        return response;
    }

    /**
     * Called when a "receive" step is executed.
     * <p>
     * This would typically record what kind of message is expected to be received and later on call the Test Bed's callback API
     * when a matching message comes in. In our case we have no need to receive messages from the LDES server, so we can just give
     * an empty implementation.
     *
     * @param receiveRequest The request's parameters.
     * @return An empty output ("receive" outputs are returned asynchronously).
     */
    @Override
    public Void receive(ReceiveRequest receiveRequest) {
        return new Void();
    }

    /**
     * Called when a transaction starts (if we use transactions in our test cases).
     * <p>
     * As we don't use transactions we can keep this empty.
     *
     * @param beginTransactionRequest The request.
     * @return An empty response.
     */
    @Override
    public Void beginTransaction(BeginTransactionRequest beginTransactionRequest) {
        return new Void();
    }

    /**
     * Called when a transaction ends (if we use transactions in our test cases).
     * <p>
     * As we don't use transactions we can keep this empty.
     *
     * @param basicRequest The request.
     * @return An empty response.
     */
    @Override
    public Void endTransaction(BasicRequest basicRequest) {
        return new Void();
    }

    /**
     * Called when a test session completes.
     * <p>
     * This method is useful is you need to maintain any in-memory state for each test session. We don't need to
     * in our case, so we just add a log statement that the test session has completed.
     *
     * @param finalizeRequest The request.
     * @return An empty response.
     */
    @Override
    public Void finalize(FinalizeRequest finalizeRequest) {
        LOG.info("Finalising test session [{}].", finalizeRequest.getSessionId());
        return new Void();
    }

}
