package com.redhat.victims;

import static io.restassured.RestAssured.get;
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

    /*
     * [ {
  "_id" : "59c8671b3b319269ba2694cb",
  "id" : "",
  "hash" : "6532462d68fdce325b6ee0fadb6769511832c6d4524ab6da240add87133ecd1a2811de10892162304228508b4f834a32aeb1d93e1a1e73b2c38c666068cf3395",
  "name" : "camel-snakeyaml-2.17.3",
  "format" : "jar",
  "cves" : [ "2017-3159" ],
  "submitter" : "adminuser",
  "files" : [ {
    "name" : "7c3728d2149df93f39c4d55cf5e3f835fd87aaab",
    "hash" : "org/apache/camel/component/snakeyaml/SnakeYAMLDataFormat.class"
  } ]
} ]
     */
    
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
