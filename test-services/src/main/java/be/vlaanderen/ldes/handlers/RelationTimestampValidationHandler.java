package be.vlaanderen.ldes.handlers;

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
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static be.vlaanderen.ldes.Utils.convertListToString;

/**
 * Handle validations of time-based relations.
 */
@Component
public class RelationTimestampValidationHandler {

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
        // Look up the time-based relations to check.
        var relationsToCheck = getRelationsToCheck(inputModel);
        for (var relation: relationsToCheck) {
            LOG.debug("Validating relation {}", relation);
            // Lookup the members of page referred to by a relation.
            var members = getPageMembers(inputModel, relation.relatedPage());
            for (var member: members) {
                // Look up the value of the member property referred to by the relation.
                var memberValues = getMemberValue(inputModel, member, relation.relationPath());
                var validValues = new ArrayList<String>();
                if (memberValues.isPresent()) {
                    Boolean isValid = false;
                    switch (relation.relationType()) {                        
                        case EqualToRelation -> {for (String memberValue: memberValues.get()) {if(relation.relationValue().isEqual(toDate(memberValue))){isValid = true; validValues.add(memberValue);}}}
                        case GreaterThanRelation -> {for (String memberValue: memberValues.get()) {if(relation.relationValue().isAfter(toDate(memberValue))){isValid = true; validValues.add(memberValue);}}}
                        case GreaterThanOrEqualToRelation ->{for (String memberValue: memberValues.get()) {if( relation.relationValue().isAfter(toDate(memberValue)) || relation.relationValue().isEqual(toDate(memberValue))){isValid = true; validValues.add(memberValue);}}}
                        case LessThanRelation ->{for (String memberValue: memberValues.get()) {if(relation.relationValue().isBefore(toDate(memberValue))){isValid = true;validValues.add(memberValue);}}}
                        case LessThanOrEqualToRelation -> {for (String memberValue: memberValues.get()) {if(relation.relationValue().isBefore(toDate(memberValue)) || relation.relationValue().isEqual(toDate(memberValue))){isValid = true;validValues.add(memberValue);}}}                    };
                    if (isValid) {
                        String message = String.format("Member [%s] passed check [%s] for relation value [%s] with valid value(s):\n%s.", member, relation.relationType(), relation.relationValue(), convertListToString(validValues));
                        logger.log(String.format(message), LogLevel.DEBUG);
                        LOG.debug(message);
                    } else {
                        errorMessages.add(String.format("Page [%s] has a [%s] relation with page [%s], but member [%s] defines invalid value(s):\n [%s] for property [%s] considering the relation's value of [%s].", relation.page(), relation.relationType(), relation.relatedPage(), member, convertListToString(memberValues.get()), relation.relationPath(), relation.relationValue()));
                    }
                } else {
                    errorMessages.add(String.format("Page [%s] relates to page [%s], but member [%s] does not define the expected relation property [%s].", relation.page(), relation.relatedPage(), member, relation.relationPath()));
                }
            }
        }
        return errorMessages;
    }

    /**
     * Convert the property value to a date instant.
     *
     * @param propertyValue The property value to convert.
     * @return The date instant.
     */
    private OffsetDateTime toDate(String propertyValue) {
        OffsetDateTime result = null;
        if (propertyValue != null) {
            var parts = StringUtils.split(propertyValue, "^^");
            if (parts != null && parts.length == 2) {
                String datePattern = switch (parts[1]) {
                    case "http://www.w3.org/2001/XMLSchema#time" -> "HH:mm:ss.SSSSSSXXXXX";
                    case "http://www.w3.org/2001/XMLSchema#date" -> "yyyy-MM-dd";
                    case "http://www.w3.org/2001/XMLSchema#dateTime" -> "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXXXX";
                    case "http://www.w3.org/2001/XMLSchema#dateTimeStamp" -> "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXXXX";
                    default -> throw new IllegalStateException(String.format("Unexpected type [%s] for date-based property.", parts[1]));
                };
                result = OffsetDateTime.parse(parts[0], DateTimeFormatter.ofPattern(datePattern));
            }
        }
        return result;
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
                        toDate(entry.get("RelationValue").toString()),
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

    /**
     * Get the property value for a given member to compare against the relation's value.
     *
     * @param inputModel The input model.
     * @param memberSubject The member's subject.
     * @param property The property name to lookup.
     * @return The property value (if found).
     */
    private Optional<List<String>> getMemberValue(Model inputModel, String memberSubject, String property) {
        var memberValueQuery = String.format("""
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            select ?MemberValue where {
                ?Member rdf:subject ?MemberSubject ;
                    <%s> ?MemberValue .
                FILTER(?MemberSubject = <%s>)
            }
        """, property, memberSubject);
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
    record RelationData(String page, RelationType relationType, OffsetDateTime relationValue, String relationPath, String relatedPage) {}

    /**
     * The types of relations to consider.
     */
    enum RelationType {

        GreaterThanRelation, GreaterThanOrEqualToRelation, LessThanRelation, LessThanOrEqualToRelation, EqualToRelation;

        /**
         * Parse a relation type from the relation type value.
         *
         * @param property The value to parse.
         * @return The relation type.
         */
        static RelationType fromProperty(String property) {
            return switch (property) {
                case "https://w3id.org/tree#GreaterThanRelation" -> GreaterThanRelation;
                case "https://w3id.org/tree#GreaterThanOrEqualToRelation" -> GreaterThanOrEqualToRelation;
                case "https://w3id.org/tree#LessThanRelation" -> LessThanRelation;
                case "https://w3id.org/tree#LessThanOrEqualToRelation" -> LessThanOrEqualToRelation;
                case "https://w3id.org/tree#EqualToRelation" -> EqualToRelation;
                default -> throw new IllegalArgumentException(String.format("Unknown relation type [%s]", property));
            };
        }

    }

}
