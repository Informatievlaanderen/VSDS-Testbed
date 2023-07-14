package be.vlaanderen.ldes.handlers;

import be.vlaanderen.ldes.gitb.TestBedLogger;

import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gitb.core.LogLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle the comparison of two RDF graphs in a XML format.
 * Return a list of object which is a tree:node of a tree:Relation, but not typed as tree:Node.
 * The comparison is done by comparing the URI's of the nodes in the two RDF graphs.
 * 
 */

@Component
public class RDFComparisonHandler {

    /*** Logger.****/ 
    private static final Logger LOG = LoggerFactory.getLogger(
            RelationTimestampValidationHandler.class
    );

    /**
     * Compare two RDF graphs in a XML format.
     *
     * @param xml1   The first RDF graph in a XML format.
     * @param xml2   The second RDF graph in a XML format.
     * @param logger The logger to log the result of the comparison.
     * @return True if the two RDF graphs are logically equal, false otherwise.
     */

    public List<String> compareXMLUris(
            String xml1,
            String xml2,
            TestBedLogger logger
    ) {

        var errorMessages = new ArrayList<String>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Parse the XML documents
            Document doc1 = builder.parse(new ByteArrayInputStream(xml1.getBytes()));
            Document doc2 = builder.parse(new ByteArrayInputStream(xml2.getBytes()));
            // Get the list of URI's in the two documents
            NodeList uriList1 = doc1.getElementsByTagName("uri");
            NodeList uriList2 = doc2.getElementsByTagName("uri");
            LOG.debug("Validating if the object of tree:node of a tree:Relation also has a type tree:Node "); 
            logger.log(String.format("Validating if the object of tree:node of a tree:Relation also has a type tree:Node "), LogLevel.DEBUG);           
            // System.out.println("Node: " + uriList1.item(0).getTextContent());
            // Compare the two lists of URI's
            if (!compareNodeLists(uriList1, uriList2)) {
                NodeList difference = getNodeListDifference(uriList1, uriList2);
                // Print the nodes in the difference list
                for (int i = 0; i < difference.getLength(); i++) {
                    Node node = difference.item(i);
                    // System.out.println("Node: " +  difference.item(i));
                    String message = String.format("[%s] is tree:node of a tree:Relation, but not typed as tree:Node", node.getTextContent());
                    logger.log(String.format(message), LogLevel.DEBUG);
                    LOG.debug(message);
                    errorMessages.add(String.format("[%s] is tree:node of a tree:Relation, but not typed as tree:Node", node.getTextContent()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessages.add(e.getMessage());
        }
        return errorMessages;
    }

    /**
     * Compare if two NodeList objects are logically equal.
     *
     * @param nodeList1   The first NodeList object.
     * @param nodeList2   The second NodeList object.
     * @return true if the two NodeList objects are logically equal, false otherwise.
     */

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

    
    /**
     * Found a list of difference between two NodeList objects.
     *
     * @param nodeList1   The first NodeList object.
     * @param nodeList2   The second NodeList object.
     * @return A NodeList object containing the difference between the two NodeList objects.
     */

    private  NodeList getNodeListDifference(NodeList list1, NodeList list2) {
        List<Node> difference = new ArrayList<>();

        // Iterate over nodes in list1 and add them to the difference list if they are not present in list2
        for (int i = 0; i < list1.getLength(); i++) {
            Node node = list1.item(i);
            if (!containsNode(list2, node)) {
                difference.add(node.cloneNode(true));
            }
        }

        Node[] differenceArray = difference.toArray(new Node[0]);
        return new NodeList() {
            @Override
            public Node item(int index) {
                return differenceArray[index];
            }

            @Override
            public int getLength() {
                return differenceArray.length;
            }
        };
    }

    /**
     * Check if a NodeList contains a given Node.
     *
     * @param nodeList   The NodeList object.
     * @param node   The Node object.
     * @return true if the NodeList contains the Node, false otherwise.
     */

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
