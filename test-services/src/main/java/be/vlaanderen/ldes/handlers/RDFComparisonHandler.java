package be.vlaanderen.ldes.handlers;

import be.vlaanderen.ldes.gitb.TestBedLogger;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.w3c.dom.Document;

/**
 * Handle the comparison of two RDF graphs in a XML format.
 */
@Component
public class RDFComparisonHandler {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(
    RelationTimestampValidationHandler.class
  );

  /**
   * Compare two RDF graphs in a XML format.
   *
   * @param xml1 The first RDF graph in a XML format.
   * @param xml2 The second RDF graph in a XML format.
   * @param logger The logger to log the result of the comparison.
   * @return True if the two RDF graphs are logically equal, false otherwise.
   */

  public boolean compareXMLUris(
    String xml1,
    String xml2,
    TestBedLogger logger
  ) {
    LOG.debug("Compare two RDF graphs in XML format");
    xml1 = removeFirstLine(xml1);
    xml2 = removeFirstLine(xml2);
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc1 = builder.parse(new ByteArrayInputStream(xml1.getBytes()));
      Document doc2 = builder.parse(new ByteArrayInputStream(xml2.getBytes()));
      NodeList uriList1 = doc1.getElementsByTagName("uri");
      NodeList uriList2 = doc2.getElementsByTagName("uri");
      System.out.println(compareNodeLists(uriList1, uriList2));
      return compareNodeLists(uriList1, uriList2);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private boolean compareNodeLists(NodeList nodeList1, NodeList nodeList2) {
    if (nodeList1.getLength() != nodeList2.getLength()) {
      return false;
    }

    // Iterate over the nodes in nodeList1
    for (int i = 0; i < nodeList1.getLength(); i++) {
      Node node1 = nodeList1.item(i);
      String nodeValue1 = node1.getTextContent();

      // Iterate over the nodes in nodeList2
      boolean foundMatch = false;
      for (int j = 0; j < nodeList2.getLength(); j++) {
        Node node2 = nodeList2.item(j);
        String nodeValue2 = node2.getTextContent();

        // Compare node values
        if (nodeValue1.equals(nodeValue2)) {
          foundMatch = true;
          break;
        }
      }

      // If a match is not found, return false
      if (!foundMatch) {
        return false;
      }
    }

    return true;
  }

  private String removeFirstLine(String input) {
    int index = input.indexOf('\n');
    if (index != -1 && index + 1 < input.length()) {
      return input.substring(index + 1);
    } else {
      return "";
    }
  }
}
