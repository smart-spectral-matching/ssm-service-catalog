package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

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
 * Represents a writer for RDF Models.
*/
public final class RdfModelWriter {
    /**
     * Constructor set to private since this is a utility class.
    */
    private RdfModelWriter() { }

    /**
     * Writes RDF model w/ provided format to the output stream given.
     *
     * @param out  Output stream to write RDF model to
     * @param g    Apache Jena DatasetGraph that represents the RDF model
     * @param f    RDF format for the model that we will write out
     * @param ctx  Context for the RDF model
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
     * Returns RDF model as a String.
     *
     * @param g    Apache Jena DatasetGraph that represents the RDF model
     * @param f    RDF format for the model to write to string
     * @param ctx  Context for the RDF model
     * @return     RDF Model as a string
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
     * Returns RDF model as JSON-LD.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      JSON-LD for the Model provided
    */

    public static String getJsonldForModel(final Model model) {
        DatasetGraph g = DatasetFactory.wrap(model).asDatasetGraph();
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        String jsonld = write2String(g, RDFFormat.JSONLD_COMPACT_PRETTY, ctx);
        return jsonld;
    }
}

