package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.sparql;

/**
 * Enum class to hold the SPARQL PREFIXES we use for queries.
 */
public enum SparqlPrefix {
    /** Dublic Core HTTP ontology. */
    DCTERM("dcterm", "http://purl.org/dc/terms/"),

    /** Dublic Core HTTPS ontology. */
    DCTERMS("dcterms", "http://purl.org/dc/terms/"),

    /** Permanent URL OBO ontology. */
    OBO("obo", "http://purl.obolibrary.org/obo/"),

    /** NS Prov ontology. */
    PROV("prov", "http://www.w3.org/ns/prov#"),

    /** Standard RDF ontology. */
    RDF("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),

    /** Standard RDF Schema ontology. */
    RDFS("rdfs", "https://www.w3.org/2000/01/rdf-schema#"),

    /** SciData ontology. */
    SDO("sdo", "https://stuchalk.github.io/scidata/ontology/scidata.owl#"),

    /** Schema.org HTTP ontology. */
    URL("url", "http://schema.org/"),

    /** Schema.org HTTPS ontology. */
    URLS("urls", "https://schema.org/"),

    /** XML Schema HTTP ontology. */
    XML("xml", "http://www.w3.org/2001/XMLSchema#"),

    /** XML Schema HTTPS ontology. */
    XMLS("xmls", "https://www.w3.org/2001/XMLSchema#"),

    /** XML Schema HTTPS ontology (as XSD prefix). */
    XSD("xsd", "https://www.w3.org/2001/XMLSchema#");

    /**
     * Label for the prefix name.
     */
    private final String label;

    /**
     * IRI for the prefix name.
     */
    private final String iri;

    /**
     * Constructor for the SparqlPrefix object.
     *
     * @param label Label for the prefix (i.e. 'rdf', 'xml', 'sdo', etc.)
     * @param iri   IRI for the prefix (i.e. URL for the ontology)
     */
    SparqlPrefix(final String label, final String iri) {
        this.label = label;
        this.iri = iri;
    }

    /**
     * Getter for label.
     *
     * @return Label for the prefix
     */
    public String label() {
        return label;
    }

    /**
     * Getter for the IRI.
     *
     * @return IRI for the prefix
     */
    public String iri() {
        return iri;
    }

    /**
     * Getter for the full prefix name for SPARQL queries.
     *
     * @return Full prefix name (i.e. "PREFIX 'label': <'iri'>")
     */
    public String getPrefixName() {
        return "PREFIX " + label + ": " + iri;
    }

    /**
     * Get all the defined full prefix names for SPARQL queries.
     *
     * @return String with all the enum prefix names
     */
    public static String queryPrefixesAll() {
        StringBuilder prefixes = new StringBuilder();
        for (SparqlPrefix sp: SparqlPrefix.values()) {
            prefixes.append(sp.getPrefixName());
        }
        return prefixes.toString();
    }
}
