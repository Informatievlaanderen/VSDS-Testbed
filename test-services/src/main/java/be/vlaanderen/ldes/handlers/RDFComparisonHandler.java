package be.vlaanderen.ldes.handlers;

import be.vlaanderen.ldes.gitb.TestBedLogger;
import com.gitb.core.LogLevel;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.jena.graph.Factory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Handle SPARQL select queries.
 */
@Component
public class RDFComparisonHandler {

  /**
   * Compare if two RDF graphs are equal.
   *
   * @param inputFirstRDF The first input RDF graph.
   * @param inputSecondRDF The second input RDF graph.
   * @return boolean result for the comparison.
   */
  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(RDFComparisonHandler.class);

  public Boolean compare(
    String inputFirstRDF,
    String inputSecondRDF,
    String ContentType,
    TestBedLogger logger
  ) {
    var model1 = ModelFactory.createModelForGraph(Factory.createDefaultGraph());
    var model2 =  ModelFactory.createModelForGraph(Factory.createDefaultGraph());
    try (var reader = new StringReader(inputFirstRDF)) {
      model1.read(
        reader,
        null,
        RDFLanguages.contentTypeToLang(ContentType).getName()
      );
    }
    try (var reader = new StringReader(inputSecondRDF)) {
      model2.read(
        reader,
        null,
        RDFLanguages.contentTypeToLang(ContentType).getName()
      );
    }
    boolean isSame = true;
    StmtIterator iter1 = model1.listStatements();
    while (iter1.hasNext()) {
      if (!model2.contains(iter1.nextStatement())) {
        isSame = false;
        break;
      }
    }

    StmtIterator iter2 = model2.listStatements();
    while (iter2.hasNext()) {
      if (!model1.contains(iter2.nextStatement())) {
        isSame = false;
        break;
      }
    }

    return isSame;
  }
}
