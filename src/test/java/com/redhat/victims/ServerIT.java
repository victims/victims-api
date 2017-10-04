package com.redhat.victims;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;
import io.vertx.core.buffer.Buffer;

public class ServerIT {

	private static final String SNAKEYAML = "camel-snakeyaml-2.17.4.jar";

	@BeforeClass
	public static void configureRestAssured() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = Integer.getInteger("http.port", 8082);
	}

	@AfterClass
	public static void unconfigureRestAssured() {
		RestAssured.reset();
	}

    
    @Test
    public void testHealthz() {
        get("healthz").then().assertThat().statusCode(200);
    }
    
    @Test
    public void checkThatWeCanAUpload() {
		String contentType = "application/octet-stream";
		String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
		Buffer buffer = Buffer.buffer();
		String header = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + SNAKEYAML
				+ "\"; filename=\"" + SNAKEYAML + "\"\r\n" + "Content-Type: " + contentType + "\r\n"
				+ "Content-Transfer-Encoding: binary\r\n" + "\r\n";
		buffer.appendString(header);
		Path path = Paths.get(ServerTest.TEST_RESOURCES + SNAKEYAML);
		try {
			buffer.appendBytes(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		String footer = "\r\n--" + boundary + "--\r\n";
		buffer.appendString(footer);
		given().body(buffer.toString()).post("/upload/2017-3159")
		.then().assertThat().statusCode(200);
    }
    
/*    @Test
    public void checkThatWeCanRetrieveIndividualProduct() {

        String hash = "3cfc3c06a141ba3a43c6c0a01567dbb7268839f75e6367ae0346bab90507ea09c9ecd829ecec3f030ed727c0beaa09da8c08835a8ddc27054a03f800fa049a0a";
      // Now get the individual resource and check the content
        get("/api/cves/" + hash).then()
       .assertThat()
       .statusCode(200)
       .body("cves[0]", hasItem("2017-3159"));
    }*/
}
