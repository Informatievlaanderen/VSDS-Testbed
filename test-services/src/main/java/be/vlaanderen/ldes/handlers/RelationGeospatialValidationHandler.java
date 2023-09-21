package be.vlaanderen.ldes.handlers;
import static be.vlaanderen.ldes.Utils.*;
import be.vlaanderen.ldes.gitb.TestBedLogger;
import com.gitb.core.LogLevel;
import org.apache.jena.graph.Factory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
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
        var validValues = new ArrayList<String>();
        var membersExist = false;
        try (var reader = new StringReader(content)) {
            inputModel.read(reader, null, RDFLanguages.contentTypeToLang(contentType).getName());
        }
        // Look up the geospatial relations to check.
        var relationsToCheck = getRelationsToCheck(inputModel);
        System.out.println("relationsToCheck: "+relationsToCheck);
        for (var relation: relationsToCheck) {
            LOG.debug("Validating relation {}", relation);
            // Lookup the members of page referred to by a relation.
            var members = getPageMembers(inputModel, relation.relatedPage());              
            for (var member: members) {
                membersExist = true;
                // Look up the value of the member property referred to by the relation.               
                var memberValues = getMemberValue(inputModel, member, relation.relationPath());
                var isValid = false;
                if (memberValues.isPresent()) {
                    switch (relation.relationType()) {                    
                        case GeospatiallyContainsRelation -> {if(!doesContain(extractWKT(relation.relationValue()), memberValues.get()).isEmpty()){isValid = true;validValues.addAll(doesContain(extractWKT(relation.relationValue()), memberValues.get()));}}              
                    };
                    if (isValid) {
                        String message = String.format("Member [%s] passed check [%s] for relation value [%s] with valid value(s):\n%s.", member, relation.relationType(), relation.relationValue(), convertListToString(validValues));
                        logger.log(String.format(message), LogLevel.DEBUG);
                        LOG.debug(message);
                    } else {
                        //errorMessages.add(String.format("Page [%s] has a [%s] relation with page [%s], but member [%s] doesnt contain a valid value for property [%s] considering the relation's value of [%s].", relation.page(), relation.relationType(), relation.relatedPage(), member, relation.relationPath(), relation.relationValue()));
                        errorMessages.add(String.format("Page [%s] has a [%s] relation with page [%s], but member [%s] defines invalid value(s):\n [%s] for property [%s] considering the relation's value of [%s].", relation.page(), relation.relationType(), relation.relatedPage(), member, convertListToString(members), relation.relationPath(), relation.relationValue()));
                    }
                } else {
                    errorMessages.add(String.format("Page [%s] relates to page [%s], but member [%s] does not define the expected relation property [%s].", relation.page(), relation.relatedPage(), member, relation.relationPath()));
                }
            }
        }
        // if no member can be reached by geoSpatial relation, add error message
        if(!membersExist){
            errorMessages.add(String.format("No members found for the provided page(s) with GeoSpatial Semantic relation(s)."));
        }   
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
        System.out.println("page: "+page);
        //page = "http://ldes-server:8080/kbo/by-location?tile=15/16811/10986&pageNumber=1";
        String memberQuery = String.format("""
                    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                    PREFIX tree: <https://w3id.org/tree#>
                    PREFIX crawler: <http://example.org/>
                    PREFIX ldes: <https://w3id.org/ldes#>
                    select DISTINCT ?Page ?PageMember where {
                        ?Page rdf:type crawler:CrawledPage ;
                            crawler:has_contents ?PageContent .
                        ?PageContent rdf:type ldes:EventStream ;
                              tree:member ?PageMember .
                        FILTER (STR(?Page) = "%s")
                    }
                """, page);
        try (var queryExecution = QueryExecutionFactory.create(memberQuery, inputModel)) {
            var resultSet = queryExecution.execSelect();
            resultSet.forEachRemaining(entry -> results.add(entry.get("PageMember").toString()));
            resultSet.close();
        }
        System.out.println("results: "+results);
        return results;
    }

    public static String extractWKT(String input) {
        // Define the regular expression pattern to match th(e WKT part
        Pattern pattern = Pattern.compile("POLYGON\\s*\\(\\(.*?\\)\\)|POINT\\s*\\(.*?\\)|LINESTRING\\s*\\(.*?\\)|MULTIPOLYGON\\s*\\(\\(.*?\\)\\)|MULTIPOINT\\s*\\(.*?\\)|MULTILINESTRING\\s*\\(.*?\\)|GEOMETRYCOLLECTION\\s*\\(.*?\\)");
        // Create a matcher for the input string
        Matcher matcher = pattern.matcher(input);

        // Check if a match is found and extract the WKT part
        if (matcher.find()) {
            String wktPart = matcher.group();
            return wktPart;
        } else {
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
    private Optional<List<String>> getMemberValue(Model inputModel, String memberSubject, String property) {
        //Current query is hardcoded for the structure of the current Crawled dataset.
        var memberValueQuery = String.format("""
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX legal: <http://www.w3.org/ns/legal#>
            PREFIX n1: <http://example.org/>
            select ?MemberValue where {
                ?Member n1:has_contents [rdf:subject <%s>].
                ?Member n1:has_contents [<%s> ?MemberValue].
            }
        """, memberSubject,property);
                  
       try (var queryExecution = QueryExecutionFactory.create(memberValueQuery, inputModel)) {
            var resultSet = queryExecution.execSelect();
            if (resultSet.hasNext()) {
                List<String> returnSet = new ArrayList<>();
                while (resultSet.hasNext()) {
                    // Access individual query solution
                    QuerySolution solution = resultSet.nextSolution();                    
                    returnSet.add(solution.get("MemberValue").toString());    
                }
                return Optional.of(returnSet);
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

    /**
     * Check if the first WKT geometry contains any of the geometries in the second WKT geometry set.
     *
     * @param wkt1 The first WKT geometry.
     * @param wktSet The WKT geometry Set.
     * @return True if the first geometry contains any of the geometries in the second geometry set.
     */


    private List<String> doesContain(String wkt1, List<String> wktSet) {
        WKTReader reader = new WKTReader();      
        List<String> validValues = new ArrayList<>();
        try {
            // Parse the WKT strings into JTS Geometry objects
            Geometry geometry1 = reader.read(wkt1);        
            // Check if the first geometry contains the second geometry
            for(String wkt2 : wktSet){
                Geometry geometry2 = reader.read(wkt2);    
                if(geometry1.contains(geometry2)){
                    validValues.add(wkt2);                
            }}
        } 
        catch (ParseException e) {
            e.printStackTrace();
            // Handle parsing exceptions if needed
        }
        return validValues;
    }

}
