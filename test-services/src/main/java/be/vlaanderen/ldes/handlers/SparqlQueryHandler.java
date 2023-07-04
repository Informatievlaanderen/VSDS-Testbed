package be.vlaanderen.ldes.handlers;

import org.apache.jena.graph.Factory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.stereotype.Component;

import java.io.StringReader;

/**
 * Handle SPARQL select queries.
 */
@Component
public class SparqlQueryHandler {

    /**
     * Execute a SPARQL select query on the provided input.
     *
     * @param inputContent The input to query.
     * @param inputContentType The content type of the input.
     * @param query The SPARQL query to execute.
     * @return The query's result set as an XML string.
     */
    public String select(String inputContent, String inputContentType, String query) {
        var inputModel = ModelFactory.createModelForGraph(Factory.createDefaultGraph());
        try (var reader = new StringReader(inputContent)) {
            inputModel.read(reader, null, RDFLanguages.contentTypeToLang(inputContentType).getName());
        }
        String queryResultAsXml;
        try (var queryExecution = QueryExecutionFactory.create(query, inputModel)) {
            var resultSet = queryExecution.execSelect();
            queryResultAsXml = ResultSetFormatter.asXMLString(resultSet);
            resultSet.close();
        }
        return queryResultAsXml;
    }

}
