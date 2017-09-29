package com.redhat.victims;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.put;
import static org.hamcrest.Matchers.hasItem;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


import io.restassured.RestAssured;

public class ServerIT {
    @BeforeClass
    public static void configureRestAssured() {
      RestAssured.baseURI = "http://localhost";
      RestAssured.port = Integer.getInteger("http.port", 8082);
      //TODO Craft a put here so we can test get by hash
    }

    @AfterClass
    public static void unconfigureRestAssured() {
      RestAssured.reset();
    }

/*    @Test
    public void checkUpsert() {
    	put("upload/2017-3159").then()
    	.assertThat()
    	.statusCode(200);
    }*/
    
    @Test
    public void testHealthz() {
        get("healthz").then().assertThat().statusCode(200);
    }
    
    @Test
    public void checkThatWeCanRetrieveIndividualProduct() {

        String hash = "3cfc3c06a141ba3a43c6c0a01567dbb7268839f75e6367ae0346bab90507ea09c9ecd829ecec3f030ed727c0beaa09da8c08835a8ddc27054a03f800fa049a0a";
      // Now get the individual resource and check the content
        get("/api/cves/" + hash).then()
       .assertThat()
       .statusCode(200)
       .body("cves[0]", hasItem("2017-3159"));
    }
}
