package be.vlaanderen.ldes.handlers;
import be.vlaanderen.ldes.gitb.TestBedLogger;

import com.gitb.core.LogLevel;

import org.apache.jena.base.Sys;
import org.apache.jena.graph.Factory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Handle validations of Geospatial relations.
 */
@Component
public class RelationGeospatialValidationHandler {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(RelationTimestampValidationHandler.class);

    /**
     * Validate the provided (crawled) content.
     *
     * @param content The content to validate (to be progressively queried multiple times).
     * @param contentType The content's type.
     * @param logger The logger to use to post log messages to the Test Bed.
     * @return The list of error messages to report.
     */
    public List<String> validate(String content, String contentType, TestBedLogger logger) {
        var errorMessages = new ArrayList<String>();
        var inputModel = ModelFactory.createModelForGraph(Factory.createDefaultGraph());
        try (var reader = new StringReader(content)) {
            inputModel.read(reader, null, RDFLanguages.contentTypeToLang(contentType).getName());
        }
        // Look up the geospatial relations to check.
        var relationsToCheck = getRelationsToCheck(inputModel);
        for (var relation: relationsToCheck) {
            LOG.debug("Validating relation {}", relation);
            // Lookup the members of page referred to by a relation.
            var members = getPageMembers(inputModel, relation.relatedPage());
            for (var member: members) {
                // Look up the value of the member property referred to by the relation.
                System.out.println("member: " + member);
                var memberValue = getMemberValue(inputModel, member, relation.relationPath());
                System.out.println("memberValue: " + memberValue);
                if (memberValue.isPresent()) {
                    var memberWKT = extractWKT(memberValue.get());
                    var isValid = switch (relation.relationType()) {                    
                        case GeospatiallyContainsRelation -> doesContain(extractWKT(relation.relationValue()), memberWKT);                     
                    };
                    if (isValid) {
                        String message = String.format("Member [%s] value [%s] passed check [%s] for relation value [%s].", member, memberWKT, relation.relationType(), relation.relationValue());
                        logger.log(String.format(message), LogLevel.DEBUG);
                        LOG.debug(message);
                    } else {
                        errorMessages.add(String.format("Page [%s] has a [%s] relation with page [%s], but member [%s] defines an invalid value [%s] for property [%s] considering the relation's value of [%s].", relation.page(), relation.relationType(), relation.relatedPage(), member, memberWKT, relation.relationPath(), relation.relationValue()));
                        // errorMessages.add(String.format("Page [%s] has a [%s] relation with page [%s], but member [%s] defines an invalid value [%s] for property [%s] considering the relation's value of [%s].", relation.page(), relation.relationType(), relation.relatedPage(), memberWKT, relation.relationPath(), relation.relationValue()));
                    }
                } else {
                    errorMessages.add(String.format("Page [%s] relates to page [%s], but member [%s] does not define the expected relation property [%s].", relation.page(), relation.relatedPage(), member, relation.relationPath()));
                }
            }
        }
        //System.out.println("errorMessages: " + errorMessages);
        return errorMessages;
    }

    /**
     * Detect the time-based relations to validate.
     *
     * @param inputModel The input model.
     * @return The relations.
     */
    private List<RelationData> getRelationsToCheck(Model inputModel) {
        var results = new ArrayList<RelationData>();
        String query = """
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX tree: <https://w3id.org/tree#>
            select DISTINCT ?Subject ?RelationType ?RelatedPage ?PropertyPath ?RelationValue  where {
                    ?Node rdf:type tree:Node ;
                        rdf:subject ?Subject ;
                        tree:relation ?Relation .
                    ?Relation rdf:type ?RelationType ;
                        tree:node ?RelatedPage ;
                        tree:value ?RelationValue ;
                        tree:path ?PropertyPath .
            }
        """;
        try (var queryExecution = QueryExecutionFactory.create(query, inputModel)) {
            var resultSet = queryExecution.execSelect();
            while (resultSet.hasNext()) {
                var entry = resultSet.next();
                results.add(new RelationData(
                        entry.get("Subject").toString(),
                        RelationType.fromProperty(entry.get("RelationType").toString()),
                        entry.get("RelationValue").toString(),
                        entry.get("PropertyPath").toString(),
                        entry.get("RelatedPage").toString()
                ));
            }
            resultSet.close();
        }
        return results;
    }

    /**
     * Get the members of a given page.
     *
     * @param inputModel The input model.
     * @param page The page's subject.
     * @return The list of member subjects.
     */
    private List<String> getPageMembers(Model inputModel, String page) {
        var results = new ArrayList<String>();
        String memberQuery = String.format("""
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX tree: <https://w3id.org/tree#>
                PREFIX crawler: <http://example.org/>
                PREFIX ldes: <https://w3id.org/ldes#>
                select DISTINCT ?PageMember where {
                    ?Page rdf:type crawler:CrawledPage ;
                        crawler:hasPageSource ?PageSource ;
                        crawler:has_contents ?PageContent .
                    ?PageContent rdf:type ldes:EventStream ;
                         tree:member ?PageMember .
                    FILTER(?PageSource = "%s")
                }
            """, page);
        try (var queryExecution = QueryExecutionFactory.create(memberQuery, inputModel)) {
            var resultSet = queryExecution.execSelect();
            resultSet.forEachRemaining(entry -> results.add(entry.get("PageMember").toString()));
            resultSet.close();
        }
        return results;
    }

    public static String extractWKT(String input) {
        // Define the regular expression pattern to match th(e WKT part
        Pattern pattern = Pattern.compile("POLYGON\\s*\\(\\(.*?\\)\\)|POINT\\s*\\(.*?\\)");
        //Pattern pattern = Pattern.compile("(POINT\\s*\\([^)]+\\))\\^\\^http://www\\.opengis\\.net/ont/geosparql#wktLiteral");

        Matcher matcher = pattern.matcher(input);

        // Check if a match is found and extract the WKT part
        if (matcher.find()) {
            String wktPart = matcher.group();
            System.out.println("Extracted WKT: " + wktPart);
            return wktPart;
        } else {
            System.out.println("No WKT part found in the input string.");
            return null;
        }
    }

    /**
     * Get the property value for a given member to compare against the relation's value.
     *
     * @param inputModel The input model.
     * @param memberSubject The member's subject.
     * @param property The property name to lookup.
     * @return The property value (if found).
     */
    private Optional<String> getMemberValue(Model inputModel, String memberSubject, String property) {
        System.out.println("memberSubject: " + memberSubject);
        System.out.println("property: " + property);
        // System.out.println("inputModel: " + inputModel);
        var memberValueQuery = String.format("""
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX legal: <http://www.w3.org/ns/legal#>
            PREFIX n1: <http://example.org/>
            select ?MemberValue where {
                ?Member n1:has_contents [rdf:subject <%s>].
                ?Member n1:has_contents [<%s> ?MemberValue].
            }
        """, memberSubject,property);
       // System.out.println("memberVlaue: " + memberValueQuery);
        try (var queryExecution = QueryExecutionFactory.create(memberValueQuery, inputModel)) {
            var resultSet = queryExecution.execSelect();
            if (resultSet.hasNext()) {
                return Optional.of(resultSet.next().get("MemberValue").toString());
            }
        }
        return Optional.empty();
    }

    /**
     * Record to capture the information of a relation.
     *
     * @param page The page's subject.
     * @param relationType The type of relation.
     * @param relationValue The value of the relation.
     * @param relationPath The property path of the relation (to be looked-up in members).
     * @param relatedPage The related page's subject.
     */
    record RelationData(String page, RelationType relationType, String relationValue, String relationPath, String relatedPage) {}

    /**
     * The types of relations to consider.
     */
    enum RelationType {

        GeospatiallyContainsRelation;

        /**
         * Parse a relation type from the relation type value.
         *
         * @param property The value to parse.
         * @return The relation type.
         */
        static RelationType fromProperty(String property) {
            return switch (property) {
                case "https://w3id.org/tree#GeospatiallyContainsRelation" -> GeospatiallyContainsRelation;
                default -> throw new IllegalArgumentException(String.format("Unknown relation type [%s] for Geospatial Fragmentation", property));
            };
        }

    }

    public static boolean doesContain(String wkt1, String wkt2) {
        System.out.println("wkt1: " + wkt1);
        System.out.println("wkt2: " + wkt2);
        // wkt1 = "POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))";
        // wkt2 = "POINT(5 5)";
        wkt1 = "POLYGON ((4.339599609375 51.17245530329899, 4.339599609375 51.16556659836183, 4.32861328125 51.16556659836183, 4.32861328125 51.17245530329899, 4.339599609375 51.17245530329899))";
        wkt2 = "POINT (4.3583312 50.850838)";
        WKTReader reader = new WKTReader();
        try {
            // Parse the WKT strings into JTS Geometry objects
            Geometry geometry1 = reader.read(wkt1);
            Geometry geometry2 = reader.read(wkt2);
            // System.out.println("geometry1: " + geometry1);
            // System.out.println("geometry2: " + geometry2);    
            // Check if the first geometry contains the second geometry\
            System.out.println("geometry1.contains(geometry2): " + geometry1.contains(geometry2));
            return geometry1.contains(geometry2);
        } catch (ParseException e) {
            e.printStackTrace();
            // Handle parsing exceptions if needed
        }

        return false;
    }


}
