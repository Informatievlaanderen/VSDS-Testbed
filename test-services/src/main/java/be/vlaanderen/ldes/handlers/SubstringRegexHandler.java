package be.vlaanderen.ldes.handlers;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import be.vlaanderen.ldes.gitb.TestBedLogger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.gitb.core.LogLevel;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.slf4j.Logger;

@Component
public class SubstringRegexHandler {    
private static final Logger LOG = LoggerFactory.getLogger(
            RelationTimestampValidationHandler.class
    );

    /**
     * Check if the input string contains a substring that matches the provided regex.
     *
     * @param inputString The input string to check.
     * @param regex The regex to match.
     * @return True if the input string contains a substring that matches the provided regex, false otherwise.
     */

    private boolean containsSubstringWithRegex(String inputString, String regex) {
        // System.out.println(inputString);
        // System.out.println(regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        return matcher.find();
    }

    /**
     * Validate if the object of tree:node of a tree:Relation also has a type tree:Node
     *
     * @param xml The XML document, sparql select output.
     * @param regEXPRString The regular expression to match.
     * @param logger The logger to use.
     * @return A list of error messages.
     */

    public List<String> validateRegList(
                    String xml,
                    String regEXPRString,
                    TestBedLogger logger
            ) {

                var errorMessages = new ArrayList<String>();
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    // Parse the XML documents
                    Document doc1 = builder.parse(new ByteArrayInputStream(xml.getBytes()));
                    // Get the list of URI's in the two documents
                    NodeList uriList1 = doc1.getElementsByTagName("literal");    
                    LOG.debug("Validating if the object of tree:node of a tree:Relation also has a type tree:Node "); 
                    logger.log(String.format("Validating if the object of tree:node of a tree:Relation also has a type tree:Node "), LogLevel.DEBUG);           
                    // System.out.println("Node: " + uriList1.item(0).getTextContent());
                    // Compare the two lists of URI's
                    for (int i = 0; i < uriList1.getLength(); i++) {
                    if (!containsSubstringWithRegex(uriList1.item(i).getTextContent(), regEXPRString)) {
                        String message = String.format("Current value [%s] does not match the regular expression [%s]", uriList1.item(i).getTextContent(), regEXPRString);
                        errorMessages.add(message);
                        logger.log(message, LogLevel.ERROR);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessages.add(e.getMessage());
                }
                return errorMessages;
            }
        }

