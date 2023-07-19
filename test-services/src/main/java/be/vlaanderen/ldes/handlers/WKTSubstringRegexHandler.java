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
public class WKTSubstringRegexHandler {    
private static final Logger LOG = LoggerFactory.getLogger(
            RelationTimestampValidationHandler.class
    );

    /**
     * Check if the input string contains a substring that matches the provided regex.
     *
     * @param inputString The input string to check.
     * @return True if the input string contains a substring that matches the provided regex, false otherwise.
     */

    private boolean containsSubstringWithRegex(String inputString) {
        System.out.println(inputString);
        Pattern pattern = Pattern.compile("POLYGON\\s*\\(\\(.*?\\)\\)|POINT\\s*\\(.*?\\)|LINESTRING\\s*\\(.*?\\)|MULTIPOLYGON\\s*\\(\\(.*?\\)\\)|MULTIPOINT\\s*\\(.*?\\)|MULTILINESTRING\\s*\\(.*?\\)|GEOMETRYCOLLECTION\\s*\\(.*?\\)");
        Matcher matcher = pattern.matcher(inputString);
        return matcher.find();
    }

    /**
     * Validate if the input string contains a substring is a WKT Literal.
     *
     * @param xml The input string to check.
     * @return A list of error messages if the input string doesn't contain a substring that matches the required regex for a WKT Literal.
     */


    public List<String> validateRegList(
                    String xml,
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
                    LOG.debug("Validating if the tree:value contains a WKT Literal"); 
                    logger.log(String.format("Validating if the tree:value contains a WKT Literal"), LogLevel.DEBUG);           
                    for (int i = 0; i < uriList1.getLength(); i++) {
                    if (!containsSubstringWithRegex(uriList1.item(i).getTextContent())) {
                        String message = String.format("Current value [%s] doesn't contains a WKT Literal inside", uriList1.item(i).getTextContent());
                        errorMessages.add(message);
                        logger.log(message, LogLevel.ERROR);
                        } else
                        {
                            String message = String.format("Current value [%s] contains a WKT Literal inside", uriList1.item(i).getTextContent());
                            logger.log(String.format(message), LogLevel.DEBUG);
                            LOG.debug(message);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessages.add(e.getMessage());
                }
                return errorMessages;
            }
        }

