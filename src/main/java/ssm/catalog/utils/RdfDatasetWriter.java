package ssm.catalog.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;

/**
 * Represents a writer for RDF Datasets.
*/
public final class RdfDatasetWriter {
    /**
     * Constructor set to private since this is a utility class.
    */
    private RdfDatasetWriter() { }

    /**
     * Writes RDF dataset w/ provided format to the output stream given.
     *
     * @param out  Output stream to write RDF dataset to
     * @param g    Apache Jena DatasetGraph that represents the RDF dataset
     * @param f    RDF format for the dataset that we will write out
     * @param ctx  Context for the RDF dataset
    */
    private static void write(
        final OutputStream out,
        final DatasetGraph g,
        final RDFFormat f,
        final Context ctx) {

        RDFWriter w =
            RDFWriter.create()
            .format(f)
            .source(g)
            .context(ctx)
            .build();
        w.output(out);
    }

    /**
     * Returns RDF dataset as a String.
     *
     * @param g    Apache Jena DatasetGraph that represents the RDF dataset
     * @param f    RDF format for the dataset to write to string
     * @param ctx  Context for the RDF dataset
     * @return     RDF Dataset as a string
    */
    public static String write2String(
        final DatasetGraph g,
        final RDFFormat f,
        final Context ctx) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            write(out, g, f, ctx);
            out.flush();
            return out.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Returns RDF dataset as JSON-LD.
     *
     * @param dataset Dataset to return as JSON-LD
     * @return      JSON-LD for the Dataset provided
    */

    public static String getJsonldForDataset(final Model dataset) {
        DatasetGraph g = DatasetFactory.wrap(dataset).asDatasetGraph();
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        String jsonld = write2String(g, RDFFormat.JSONLD_COMPACT_PRETTY, ctx);
        return jsonld;
    }
}

