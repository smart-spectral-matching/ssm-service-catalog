package gov.ornl.rse.datastreams.nuclear_datastreams.bats_service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;


/**
 * Hello world!
 *
 */
public class BatsServiceApp 
{
    public static void main( String[] args )
    {
    	Model model = ModelFactory.createDefaultModel() ;
    	model.read("data.jsonld", "JSON-LD") ;
    	model.write(System.out, "TTL");
    }
}
