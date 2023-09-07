package be.vlaanderen.ldes;

import org.apache.jena.datatypes.RDFDatatype ;
import org.apache.jena.datatypes.xsd.impl.RDFLangString ;
import org.apache.jena.datatypes.xsd.impl.RDFhtml ;
import org.apache.jena.datatypes.xsd.impl.RDFjson;
import org.apache.jena.datatypes.xsd.impl.XMLLiteralType ;
import org.apache.jena.graph.Node ;
import org.apache.jena.rdf.model.Property ;
import org.apache.jena.rdf.model.Resource ;
import org.apache.jena.rdf.model.ResourceFactory ;
import org.apache.jena.vocabulary.RDF;

/**
 The Crawler vocabulary.
 */

public class CRAWL {

    /**
     * The namespace of the vocabulary as a string
     */
    public static final String uri = "http://example.org/";

    /** returns the URI for this schema
     @return the URI for this schema
     */
    public static String getURI()
    { return uri; }

    protected static final Resource resource(String local )
    { return ResourceFactory.createResource( uri + local ); }

    protected static final org.apache.jena.rdf.model.Property property(String local )
    { return ResourceFactory.createProperty( uri, local ); }

    public static final Resource    CrawledPage          = be.vlaanderen.ldes.CRAWL.Init.CrawledPage();
    public static final Resource    Header          = be.vlaanderen.ldes.CRAWL.Init.Header();

    public static final Property    hasPageSource        = be.vlaanderen.ldes.CRAWL.Init.hasPageSource();
    public static final Property    hasContents         = be.vlaanderen.ldes.CRAWL.Init.hasContents();
    public static final Property    hasSubject      = be.vlaanderen.ldes.CRAWL.Init.hasSubject();
    public static final Property    hasHeader    = be.vlaanderen.ldes.CRAWL.Init.hasHeader();
    public static final Property    headerName       = be.vlaanderen.ldes.CRAWL.Init.headerName();
    public static final Property    headerValue         = be.vlaanderen.ldes.CRAWL.Init.headerValue();

    public static class Init {
        public static Resource CrawledPage()              { return resource( "CrawledPage" ); }
        public static Resource Header()              { return resource( "Header" ); }

        public static Property hasPageSource()            { return property( "hasPageSource" ); }
        public static Property hasContents()             { return property( "has_contents" ); }
        public static Property hasSubject()          { return RDF.subject; }
        public static Property hasHeader()        { return property( "has_headers" ); }
        public static Property headerName()           { return property( "headerName" ); }
        public static Property headerValue()             { return property( "headerValue" ); }
    }
}
