package be.vlaanderen.ldes.gitb;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;

/**
 * Configuration class responsible for creating the Spring beans required by the service.
 */
@Configuration
public class WebServiceConfig {

    /**
     * The processing service endpoint.
     *
     * @return The endpoint.
     */
    @Bean
    public EndpointImpl processingService(Bus cxfBus, ProcessingServiceImpl serviceImplementation) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, serviceImplementation);
        endpoint.setServiceName(new QName("http://www.gitb.com/ps/v1/", "ProcessingServiceService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ps/v1/", "ProcessingServicePort"));
        endpoint.publish("/process");
        return endpoint;
    }

    /**
     * The messaging service endpoint.
     *
     * @return The endpoint.
     */
    @Bean
    public EndpointImpl messagingService(Bus cxfBus, MessagingServiceImpl serviceImplementation) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, serviceImplementation);
        endpoint.setServiceName(new QName("http://www.gitb.com/ms/v1/", "MessagingServiceService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ms/v1/", "MessagingServicePort"));
        endpoint.publish("/messaging");
        return endpoint;
    }

    /**
     * The validation service endpoint.
     *
     * @return The endpoint.
     */
    @Bean
    public EndpointImpl validationService(Bus cxfBus, ValidationServiceImpl serviceImplementation) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, serviceImplementation);
        endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationServicePort"));
        endpoint.publish("/validation");
        return endpoint;
    }

}
