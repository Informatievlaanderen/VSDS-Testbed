package be.vlaanderen.ldes.gitb;
import static be.vlaanderen.ldes.Utils.createReport;
import static be.vlaanderen.ldes.Utils.getReplyToAddressFromHeaders;
import be.vlaanderen.ldes.Utils;
import be.vlaanderen.ldes.handlers.RDFComparisonHandler;
import be.vlaanderen.ldes.handlers.RelationTimestampValidationHandler;
import be.vlaanderen.ldes.handlers.WKTSubstringRegexHandler;
import be.vlaanderen.ldes.handlers.RelationGeospatialValidationHandler;
import be.vlaanderen.ldes.handlers.RelationStringValidationHandler;
import com.gitb.core.LogLevel;
import com.gitb.tr.BAR;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.vs.*;
import com.gitb.vs.Void;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import java.math.BigInteger;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.ArrayList;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of the GITB validation API to handle custom validations.
 */
@Component
public class ValidationServiceImpl implements ValidationService {

  private static final Logger LOG = LoggerFactory.getLogger(
    ValidationServiceImpl.class
  );
  private final ObjectFactory objectFactory = new ObjectFactory();

  @Autowired
  private RelationTimestampValidationHandler relationTimestampValidationHandler;
  @Autowired
  private RDFComparisonHandler rdfComparisonHandler;
  @Autowired
  private WKTSubstringRegexHandler wktSubstringRegexHandler;
  @Autowired
  private RelationGeospatialValidationHandler relationGeospatialValidationHandler;
  @Autowired
  private RelationStringValidationHandler relationStringValidationHandler;

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
   * Called when a "verify" step is executed in a test case.
   * <p>
   * This method is expected to retrieve any necessary inputs, perform the validation and produce a validation report.
   * The report may also include context data that will be recorded in the test session's context for subsequent use.
   *
   * @param validateRequest The request's inputs.
   * @return The validation outcome.
   */
  @Override
  public ValidationResponse validate(ValidateRequest validateRequest) {
    /*
     * This service is only used to validate time-based relations. If you want to add additional types of custom validations
     * you can add additional services, or as a simpler alternative, just use the same implementation by passing a
     * special purpose input (e.g. "type"). The service can read this input to determine what other inputs to expect and
     * what validation to do.
     */
    var response = new ValidationResponse();
    List<String> errorMessages = new ArrayList<>();

    LOG.info(Utils.getRequiredString(validateRequest.getInput(), "type"));
    LOG.info(
          "Requested validation from test session [{}]",
          validateRequest.getSessionId()
        );

    // Determine what type of validation is requested.
    switch (Utils.getRequiredString(validateRequest.getInput(), "type")) {
      case "time-based relations" -> {
       
        // Get the expected inputs.
        var content = Utils.getRequiredString(
          validateRequest.getInput(),
          "content"
        );
        var contentType = Utils.getRequiredString(
          validateRequest.getInput(),
          "contentType"
        );
        // To illustrate the logging capabilities we will use this class to add log statements to the test session's log.
        var logger = new ValidationServiceLogger(
          validateRequest.getSessionId(),
          getReplyToAddressFromHeaders(wsContext).orElse(null)
        );
        // Carry out the validation.
        errorMessages = relationTimestampValidationHandler.validate(
          content,
          contentType,
          logger
        );       
      }

       case "geospatial relations" -> {
       
        // Get the expected inputs.
        var content = Utils.getRequiredString(
          validateRequest.getInput(),
          "content"
        );
        var contentType = Utils.getRequiredString(
          validateRequest.getInput(),
          "contentType"
        );
         String filePath = "crawled.ttl";
         try {
           String content_a = content.toString();
           // Create a FileWriter object to write to the file
           FileWriter fileWriter = new FileWriter(filePath);

           // Wrap the FileWriter in a BufferedWriter for better performance
           BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
           // Write the string to the file
           bufferedWriter.write(content_a);

           // Close the BufferedWriter to flush and close the file
           bufferedWriter.close();

           System.out.println("String has been written to the file successfully.");
         } catch (IOException e) {
           e.printStackTrace();
         }
        // To illustrate the logging capabilities we will use this class to add log statements to the test session's log.
        var logger = new ValidationServiceLogger(
          validateRequest.getSessionId(),
          getReplyToAddressFromHeaders(wsContext).orElse(null)
        );
        // Carry out the validation.
        errorMessages = relationGeospatialValidationHandler.validate(
          content,
          contentType,
          logger
        );       
      }

      case "string relations" -> {
       
        // Get the expected inputs.
        var content = Utils.getRequiredString(
          validateRequest.getInput(),
          "content"
        );
        var contentType = Utils.getRequiredString(
          validateRequest.getInput(),
          "contentType"
        );
        // To illustrate the logging capabilities we will use this class to add log statements to the test session's log.
        var logger = new ValidationServiceLogger(
          validateRequest.getSessionId(),
          getReplyToAddressFromHeaders(wsContext).orElse(null)
        );
        // Carry out the validation.
        errorMessages = relationStringValidationHandler.validate(
          content,
          contentType,
          logger
        );       
      }


      case "RDF Comparison" -> {
        LOG.info(
          "Requested validation from test session [{}]",
          validateRequest.getSessionId()
        );
        // Get the expected inputs.
        var model1 = Utils.getRequiredString(
          validateRequest.getInput(),
          "model1"
        );
        var model2 = Utils.getRequiredString(
          validateRequest.getInput(),
          "model2"
        );
        // To illustrate the logging capabilities we will use this class to add log statements to the test session's log.
        var logger = new ValidationServiceLogger(
          validateRequest.getSessionId(),
          getReplyToAddressFromHeaders(wsContext).orElse(null)
        );
        // Carry out the validation.
        errorMessages =  rdfComparisonHandler.compareXMLUris(
                model1,
                model2,
                logger
              );           
      }
      case "WktSubstringRegexHandler" -> {
        LOG.info(
          "Requested validation from test session [{}]",
          validateRequest.getSessionId()
        );
        // Get the expected inputs.
        var xmlResult = Utils.getRequiredString(
          validateRequest.getInput(),
          "xmlResult"
        );        
        // To illustrate the logging capabilities we will use this class to add log statements to the test session's log.
        System.out.println(errorMessages);   
        var logger = new ValidationServiceLogger(
          validateRequest.getSessionId(),
          getReplyToAddressFromHeaders(wsContext).orElse(null)
        );
        // Carry out the validation.
        errorMessages = wktSubstringRegexHandler.validateRegList(
                xmlResult,
                logger
              );   
        System.out.println(errorMessages);        
      }
    }
       // Create the validation report.
              var report = createReport(TestResultType.SUCCESS);
              if (!errorMessages.isEmpty()) {
                report.setResult(TestResultType.FAILURE);
                // Create the report's items. Set the "counters" and add the individual report items.
                report
                  .getCounters()
                  .setNrOfErrors(BigInteger.valueOf(errorMessages.size()));
                report.setReports(new TestAssertionGroupReportsType());
                for (var errorMessage : errorMessages) {
                  var itemContent = new BAR();
                  itemContent.setDescription(errorMessage);
                  // Add as an error. you can also add warnings and information messages.
                  report
                    .getReports()
                    .getInfoOrWarningOrError()
                    .add(
                      objectFactory.createTestAssertionGroupReportsTypeError(
                        itemContent
                      )
                    );
                }
             
              }
    response.setReport(report);
    return response;
  }

  /**
   * Convenience class to facilitate test session logging.
   */
  static class ValidationServiceLogger implements TestBedLogger {

    private final String sessionId;
    private final String callbackAddress;
    private ValidationClient client;

    /**
     * Constructor that received the basic information needed to make log entries on the test session log (the session ID and the callback address).
     *
     * @param sessionId The test session ID
     * @param callbackAddress The Test Bed's callback address to post log messages to.
     */
    private ValidationServiceLogger(String sessionId, String callbackAddress) {
      this.sessionId = sessionId;
      this.callbackAddress = callbackAddress;
    }

    /**
     * {@inheritDoc}
     */
    public void log(String message, LogLevel level) {
      if (sessionId != null && callbackAddress != null) {
        var logRequest = new LogRequest();
        logRequest.setSessionId(sessionId);
        logRequest.setMessage(message);
        logRequest.setLevel(level);
        getClient().log(logRequest);
      }
    }

    /**
     * Create (and cache) a service client to post log messages to the Test Bed.
     * <p>
     * Realistically speaking we could just create a single global client and reuse it across
     * all calls. However, for the theoretical case that the same service is used by different Test Bed
     * instances at the same time we can create it per request.
     *
     * @return The client.
     */
    private ValidationClient getClient() {
      if (client == null) {
        var proxyFactoryBean = new JaxWsProxyFactoryBean();
        proxyFactoryBean.setServiceClass(ValidationClient.class);
        proxyFactoryBean.setAddress(callbackAddress);
        client = (ValidationClient) proxyFactoryBean.create();
      }
      return client;
    }
  }
}
