package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ornl.rse.bats.DataSet;

@RestController
@RequestMapping("/datasets")
public class BatsController {
    private static final Logger logger = LoggerFactory.getLogger(BatsController.class);

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private String hostname = "http://rse-nds-dev1.ornl.gov";

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String generateUUID() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest salt = MessageDigest.getInstance("SHA-256");
        salt.update(
            UUID.randomUUID()
                .toString()
                .getBytes("UTF-8"));
        String digest = bytesToHex(salt.digest());
        return digest;
    }

    void write(OutputStream out, DatasetGraph g, RDFFormat f, Context ctx) {
        RDFWriter w =
            RDFWriter.create()
            .format(f)
            .source(g)
            .context(ctx)
            .build();
        w.output(out);
    }

    private void write(DatasetGraph g, RDFFormat f, Context ctx) {
        write(System.out, g, f, ctx);
    }

    private String write2String(DatasetGraph g, RDFFormat f, Context ctx) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            write(out, g, f, ctx);
            out.flush();
            return out.toString("UTF-8");
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    // GET
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public String getDataSetById(@PathVariable("uuid") String uuid) {
        String jsonld = new String();
        try {
            DataSet dataset = new DataSet();
            dataset.setName(uuid);
            dataset.setHost(hostname);
            Model model = dataset.getModel(uuid);

            DatasetGraph g = DatasetFactory.wrap(model).asDatasetGraph();
            JsonLDWriteContext ctx = new JsonLDWriteContext();
            jsonld = write2String(g, RDFFormat.JSONLD_COMPACT_PRETTY, ctx);
        } catch (Exception e) {
            logger.error("Unable to get model on the remote Fuseki server.", e);
        }
        return jsonld;
    }

    // POST
    @RequestMapping(value = "", method = RequestMethod.POST)
    public String createDataset(@RequestBody String jsonPayload) throws Exception {
        // JSON -> Tree
        ObjectMapper mapper = new ObjectMapper();
        JsonNode treeNode = mapper.readTree(jsonPayload);

        // add stuff to tree here
        String uuid = generateUUID();

        // Tree -> JSON -> Jena Model
        StringReader reader = new StringReader(treeNode.toString());
        Model model = ModelFactory.createDefaultModel().read(reader, null, "JSON-LD");

        // Jena Model -> BATS DataSet
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(hostname);
        dataset.create();
        System.out.println(dataset.getFullURI());
        try {
            dataset.updateModel(uuid, model);
            logger.debug("Model uploaded!");
        } catch (Exception e) {
            logger.error("Unable to update model on the remote Fuseki server.", e);
        }
        return uuid;
    }
}
