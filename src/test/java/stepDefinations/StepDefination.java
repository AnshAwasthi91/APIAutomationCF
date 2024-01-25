package stepDefinations;

import static io.restassured.RestAssured.given;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import com.github.javafaker.Faker;

import components.APIResources;
import components.TestDataBuild;
import components.Utils;
import static org.junit.Assert.*;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class StepDefination extends Utils {
	RequestSpecification res;
	Response response;
	TestDataBuild data =new TestDataBuild();
	private Faker faker = new Faker();
	static String place_id;
	static HashMap<String, String> dataMap =  new HashMap<String, String>();


	@Given("Add Place Payload with {string}  {string} {string}")
	public void add_Place_Payload_with(String name, String language, String address) throws IOException {

		if(name.toLowerCase().contains("random")||language.toLowerCase().contains("random")||address.toLowerCase().contains("random")) {
			iHaveARequestWithRandomData();
		}
		else {
			res=given().spec(requestSpecification())
					.body(data.addPlacePayLoad(name,language,address));
		}
	}

	@Given("I have a request with random data")
	public void iHaveARequestWithRandomData() throws IOException {
		String randomName = faker.name().firstName();
		String randomLanguage = faker.lorem().word();
		String randomAddress = faker.address().fullAddress();
		dataMap.put("name", randomName);
		dataMap.put("language", randomLanguage);
		dataMap.put("address", randomAddress);

		res=given().spec(requestSpecification())
				.body(data.addPlacePayLoad(randomName,randomLanguage,randomAddress));
	}

	@When("user calls {string} with {string} http request")
	public void user_calls_with_http_request(String resource, String method) {
		APIResources resourceAPI=APIResources.valueOf(resource);
		System.out.println(resourceAPI.getResource());

		if(method.equalsIgnoreCase("POST"))
			response =res.when().post(resourceAPI.getResource());
		else if(method.equalsIgnoreCase("GET"))
			response =res.when().get(resourceAPI.getResource());
	}

	@Then("the API call got success with status code {int}")
	public void the_API_call_got_success_with_status_code(Integer int1) throws FileNotFoundException {
		response.then().spec(responseSpecification());
	}

	@Then("{string} in response body is {string}")
	public void in_response_body_is(String keyValue, String Expectedvalue) {
		assertEquals(getJsonPath(response,keyValue),Expectedvalue);
	}

	@Then("verify place_Id created maps to {string} using {string}")
	public void verify_place_Id_created_maps_to_using(String expectedName, String resource) throws IOException {
		place_id=getJsonPath(response,"place_id");
		res=given().spec(requestSpecification()).queryParam("place_id",place_id);
		user_calls_with_http_request(resource,"GET");
		String actualName=getJsonPath(response,"name");
		if(expectedName.toLowerCase().contains("random")) {
			expectedName = dataMap.get("name");
		}
		assertEquals(actualName,expectedName);
	}


	@Given("DeletePlace Payload")
	public void deleteplace_Payload() throws IOException {
		res =given().spec(requestSpecification()).body(data.deletePlacePayload(place_id));
	}
	
	@And("verify {string} response schema is as expected")
    public void verifyResponseMatchesSchema(String expectedSchemaKey) {
		String schemaName = getSchemaFileName(expectedSchemaKey);
		Object[] result = verifySchema(response,schemaName);
		if(result.length>1) {
			System.out.println(result[1]);
			assertTrue("Schema validation failed for "+expectedSchemaKey,false);
		}
		else {
			System.out.println(result[0]);
		}
	}

}
