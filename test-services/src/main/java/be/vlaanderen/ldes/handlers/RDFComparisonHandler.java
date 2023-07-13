package be.vlaanderen.ldes.handlers;

import be.vlaanderen.ldes.gitb.TestBedLogger;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

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

  public List<String> compareXMLUris(
    String xml1,
    String xml2,
    TestBedLogger logger
  ) {
    var errorMessages = new ArrayList<String>();
    LOG.debug("Compare two RDF graphs in XML format");
    LOG.debug("First string {}", xml1);
    LOG.debug("First string {}", xml2);
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc1 = builder.parse(new ByteArrayInputStream(xml1.getBytes()));
      Document doc2 = builder.parse(new ByteArrayInputStream(xml2.getBytes()));
      NodeList uriList1 = doc1.getElementsByTagName("uri");
      NodeList uriList2 = doc2.getElementsByTagName("uri");
      if(!compareNodeLists(uriList1, uriList2)){
    	  NodeList difference = getNodeListDifference(uriList1, uriList2);
          // Print the nodes in the difference list
          for (int i = 0; i < difference.getLength(); i++) {
              Node node = difference.item(i);
              errorMessages.add(String.format("%s\n",node));
          }}
    } catch (Exception e) {
      e.printStackTrace();
      errorMessages.add(String.format("%s\n", xml1));
      errorMessages.add(String.format("%s\n", xml1));
      errorMessages.add(String.format("These two SPARQL resultS are not logically same, which means not each relation's tree:node is pointing to a datatype tree:Node"));
    }
    return errorMessages;
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
  private NodeList getNodeListDifference(NodeList list1, NodeList list2) {
      // Create a new NodeList to store the difference
      NodeList difference = ((Node) list1).getOwnerDocument().createElement("Difference").getChildNodes();

      // Iterate over nodes in list1 and add them to the difference list if they are not present in list2
      for (int i = 0; i < list1.getLength(); i++) {
          Node node = list1.item(i);
          if (!containsNode(list2, node)) {
              ((Node) difference).appendChild(node.cloneNode(true));
          }
      }

      return difference;
  }

  private boolean containsNode(NodeList nodeList, Node node) {
      // Iterate over nodes in the NodeList and check if the given node is present
      for (int i = 0; i < nodeList.getLength(); i++) {
          Node currentNode = nodeList.item(i);
          if (currentNode.isEqualNode(node)) {
              return true;
          }
      }
      return false;
  }
}
