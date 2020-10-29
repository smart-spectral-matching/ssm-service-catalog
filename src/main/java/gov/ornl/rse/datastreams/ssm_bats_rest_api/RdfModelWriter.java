package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;

public class RdfModelWriter {
    private static void write(OutputStream out, DatasetGraph g, RDFFormat f, Context ctx) {
        RDFWriter w =
            RDFWriter.create()
            .format(f)
            .source(g)
            .context(ctx)
            .build();
        w.output(out);
    }

    private static void write(DatasetGraph g, RDFFormat f, Context ctx) {
        write(System.out, g, f, ctx);
    }

    private static String write2String(DatasetGraph g, RDFFormat f, Context ctx) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            write(out, g, f, ctx);
            out.flush();
            return out.toString("UTF-8");
        } catch (IOException e) { throw new RuntimeException(e); }
    }


    public static String model2jsonld(Model model) {
        DatasetGraph g = DatasetFactory.wrap(model).asDatasetGraph();
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        String jsonld = write2String(g, RDFFormat.JSONLD_COMPACT_PRETTY, ctx);
        return jsonld;
    }
}