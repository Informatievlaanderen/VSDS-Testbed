package be.vlaanderen.ldes.handlers;

import be.vlaanderen.ldes.CRAWL;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Handle data crawling.
 */



public class Crawler {
    private final Queue<String> urlQueue = new LinkedList<>();;
    private final List<String> visitedURLs = new ArrayList<>();

    private final Model crawledGraph = ModelFactory.createDefaultModel() ;
    public Crawler(String starting_url) {
        urlQueue.add(starting_url);
        visitedURLs.add(starting_url);
    }
    public Crawler run() {
        while(!urlQueue.isEmpty()) {
            String pageUrl = urlQueue.remove();
            try {
                Model page = crawlPage(pageUrl);
                crawledGraph.add(page);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public Model getGraph() {
        return this.crawledGraph;
    }

    public List<String> getVisitedURLs() {
        return this.visitedURLs;
    }

    public Model crawlPage(String url) throws URISyntaxException, IOException, InterruptedException {
        // Download page and turn into RDF model.
        HttpResponse<String> page = fetchPage(url);
        Model retrievedGraph = pageToModel(page, url);

        // Find all tree:relations and add them to the download queue.
        extractRelations(retrievedGraph).forEach((String relation_url) -> {
            // Skip visited pages.
            if (visitedURLs.contains(relation_url))
                return;
            urlQueue.add(relation_url);
        });

        // Create a new bNode for each entity (named subject).
        Map<String, Resource> subjectMap = getSubjectMap(retrievedGraph);

        // Anonymize these entities.
        Model processedGraph = anonymizeSubjects(retrievedGraph, subjectMap);
        // Add original subject as property.
        trackOriginalSubject(subjectMap, processedGraph);

        // Make all entities of downloaded page children (nested bNodes) of page entity.
        Resource pageId = createPageEntity(url, subjectMap, processedGraph);
        // Add header info to the page entity.
        addHeaders(processedGraph, page, pageId);
        return processedGraph;
    }

    private void addHeaders(Model processedGraph, HttpResponse<String> page, Resource pageId) {
        HttpHeaders headers = page.headers();
        headers.map().forEach((String headerName, List<String> headerValueList) -> {
            Resource headerNode = processedGraph.createResource();
            Statement hasHeader = processedGraph.createStatement(
                    pageId,
                    CRAWL.hasHeader,
                    headerNode
            );
            processedGraph.add(hasHeader);
            Statement headerType = processedGraph.createStatement(
                    headerNode,
                    RDF.type,
                    CRAWL.Header
            );
            processedGraph.add(headerType);
            Statement hasHeaderName = processedGraph.createStatement(
                    headerNode,
                    CRAWL.headerName,
                    processedGraph.createLiteral(headerName)
            );
            processedGraph.add(hasHeaderName);
            headerValueList.forEach((String headerValue) -> {
                Statement hasHeaderValue = processedGraph.createStatement(
                        headerNode,
                        CRAWL.headerValue,
                        processedGraph.createLiteral(headerValue)
                );
                processedGraph.add(hasHeaderValue);
            });
        });
    }

    private Model pageToModel(HttpResponse<String> page, String url) {
        return ModelFactory
                .createDefaultModel()
                // @todo Use content type header of http response?
                .read(IOUtils.toInputStream(page.body(), "UTF-8"), url, "TURTLE");
    }

    private static Resource createPageEntity(String url, Map<String, Resource> subjectMap, Model processedGraph) {
        Resource pageNode = processedGraph.createResource(url);
        subjectMap.forEach((String subject, Resource bNode)-> {
            Statement hasEntity = processedGraph.createStatement(
                    pageNode,
                    CRAWL.hasContents,
                    bNode
                    );
            processedGraph.add(hasEntity);
        });
        Statement hasEntity = processedGraph.createStatement(
                pageNode,
                RDF.type,
                CRAWL.CrawledPage
        );
        processedGraph.add(hasEntity);
        return pageNode;
    }

    private static void trackOriginalSubject(Map<String, Resource> subjectMap, Model processedGraph) {
        subjectMap.forEach((String subject, Resource bnode) -> {
            processedGraph.add(
                    processedGraph.createStatement(
                            bnode,
                            CRAWL.hasSubject,
                            processedGraph.createResource(subject)
                    ));
        });
    }

    private static Model anonymizeSubjects(Model retrievedGraph, Map<String, Resource> subjectMap) {
        Model processedGraph = ModelFactory.createDefaultModel();
        retrievedGraph.listStatements().forEach((Statement statement) -> {
            Resource subject;
            if (subjectMap.containsKey(statement.getSubject().getURI()))
                subject = subjectMap.get(statement.getSubject().getURI());
            else
                subject = statement.getSubject();
            Statement st = processedGraph.createStatement(
                    subject,
                    statement.getPredicate(),
                    statement.getObject()
            );
            processedGraph.add(st);
        });
        return processedGraph;
    }

    /**
     * Build a subject - bnode mapping.
     */
    private static Map<String, Resource> getSubjectMap(Model retrievedGraph) {
        Map<String, Resource> subjectMap = new HashMap<>();

        retrievedGraph.listSubjects().forEach((Resource subject) -> {
            if (subject.isAnon())
                return;
            Resource bnode = retrievedGraph.createResource();
            subjectMap.put(subject.getURI(), bnode);
        });
        return subjectMap;
    }

    private static HttpResponse<String> fetchPage(String url) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Accept", "text/turtle")
                .timeout(Duration.of(10, SECONDS))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    private List<String> extractRelations(Model model) {
        List<String> relations = new ArrayList<>();
        String queryString = """
            PREFIX tree: <https://w3id.org/tree#>
            SELECT DISTINCT ?relation
            WHERE {
                ?node a tree:Node .
                ?node tree:relation/tree:node ?relation.
            }
            """ ;
        Query query = QueryFactory.create(queryString) ;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect() ;
            while (results.hasNext()) {
                Resource relation = results.nextSolution().getResource("relation") ;
                relations.add(relation.getURI());
            }
        }
        return relations;
    }

}
