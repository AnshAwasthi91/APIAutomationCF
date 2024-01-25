package components;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public class Utils {

	public static RequestSpecification req;
	public static ResponseSpecification res;
	
	public RequestSpecification requestSpecification() throws IOException
	{
		if(req==null)
		{
		PrintStream log =new PrintStream(new FileOutputStream("logging.txt"));
		 req=new RequestSpecBuilder().setBaseUri(getGlobalValue("baseUrl")).addQueryParam("key", "qaclick123")
				 .addFilter(RequestLoggingFilter.logRequestTo(log))
				 .addFilter(ResponseLoggingFilter.logResponseTo(log))
		.setContentType(ContentType.JSON).build();
		 return req;
		}
		return req;
		
	}
	
	public ResponseSpecification responseSpecification() throws FileNotFoundException {
		if(res == null) {
			res=new ResponseSpecBuilder()
					.expectStatusCode(200)
					.expectContentType(ContentType.JSON)
					.build();
			return res;
		}
		return res;
	}
	
	
	
	public static String getGlobalValue(String key) throws IOException
	{
		Properties prop =new Properties();
		FileInputStream fis =new FileInputStream(System.getProperty("user.dir")+"/src/test/resources/global.properties");
		prop.load(fis);
		return prop.getProperty(key);	
	}
	
	
	public String getJsonPath(Response response,String key)
	{
		  String resp=response.asString();
		JsonPath   js = new JsonPath(resp);
		return js.get(key).toString();
	}
	
	public static String getReportConfigPath() throws IOException{
		
		String reportConfigPath = getGlobalValue("reportConfigPath");
		if(reportConfigPath!= null) return reportConfigPath;
		else throw new RuntimeException("Report Config Path not specified in the Configuration.properties file for the Key:reportConfigPath");		
	}
	
	public String getSchemaFileName(String apiName) {
		switch(apiName.toUpperCase()) {
		case "ADDPLACE":
			return "AddPlaceSchema.json";
		default :
				return "";
		}
	}
	
	public Object[] verifySchema(Response apiResponse, String schemaFileName) {
		JsonSchema schema = null;
		String expectedSchema = null;
        JsonNode actualJson = null;
        JsonNode schemaNode = null;
        ProcessingReport report = null;
        URI schemaFilePath = null;
        String actualResponse = null;
		ObjectMapper objectMapper = new ObjectMapper();
		
        try {
        	actualResponse = apiResponse.body().asString();
        	schemaFilePath = new URI("file://"+System.getProperty("user.dir")+getGlobalValue("jsonSchemaPaths")+schemaFileName);
        	System.out.println(schemaFilePath);
            expectedSchema = new String(Files.readAllBytes(Paths.get(schemaFilePath)));
            actualJson = objectMapper.readTree(actualResponse);
            schemaNode = objectMapper.readTree(expectedSchema);
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            schema = factory.getJsonSchema(schemaNode);
            report = schema.validate(actualJson);
            if (report.isSuccess()) {
                return new String[]{"Validation succeeded"};
            } else {
            	return new Object[] {"Validation failed",report};
            }
        } catch (JsonParseException e) {
        	throw new RuntimeException("Error parsing JSON response: " + e.getMessage(), e);
        } catch (IOException e) {
        	throw new RuntimeException("Error reading schema file: " + e.getMessage(), e);
		} catch (ProcessingException e) {
			throw new RuntimeException("Error processing schema file: " + e.getMessage(), e);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error in URI Syntax schema file: " + e.getMessage(), e);
		}
    }
}
