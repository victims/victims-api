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
    public void checkThatWeCanRetrieveIndividualProduct() {

        String hash = "6532462d68fdce325b6ee0fadb6769511832c6d4524ab6da240add87133ecd1a2811de10892162304228508b4f834a32aeb1d93e1a1e73b2c38c666068cf3395";
      // Now get the individual resource and check the content
        get("/api/cves/" + hash).then()
       .assertThat()
       .statusCode(200)
       .body("cves[0]", hasItem("2017-3159"));
    }
}
