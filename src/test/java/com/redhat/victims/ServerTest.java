package com.redhat.victims;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class ServerTest {

	  private Vertx vertx;
	  private Integer port;
	  private static MongodProcess MONGO;
	  private static int MONGO_PORT = 12345;

	  @BeforeClass
	  public static void initialize() throws IOException {
	    MongodStarter starter = MongodStarter.getDefaultInstance();

	    IMongodConfig mongodConfig = new MongodConfigBuilder()
	        .version(Version.Main.PRODUCTION)
	        .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
	        .build();

	    MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
	    MONGO = mongodExecutable.start();
	  }

	  @AfterClass
	  public static void shutdown() {
	    MONGO.stop();
	  }

	  /**
	   * Before executing our test, let's deploy our verticle.
	   * <p/>
	   * This method instantiates a new Vertx and deploy the verticle. Then, it waits in the verticle has successfully
	   * completed its start sequence (thanks to `context.asyncAssertSuccess`).
	   *
	   * @param context the test context.
	   */
	  @Before
	  public void setUp(TestContext context) throws IOException {
	    vertx = Vertx.vertx();

	    // Let's configure the verticle to listen on the 'test' port (randomly picked).
	    // We create deployment options and set the _configuration_ json object:
	    ServerSocket socket = new ServerSocket(0);
	    port = socket.getLocalPort();
	    socket.close();

	    DeploymentOptions options = new DeploymentOptions()
	        .setConfig(new JsonObject()
	            .put("http.port", port)
	            .put("db_name", "victims-v2")
	            .put("connection_string", "mongodb://localhost:" + MONGO_PORT)
	        );

	    // We pass the options as the second parameter of the deployVerticle method.
	    vertx.deployVerticle(Server.class.getName(), options, context.asyncAssertSuccess());
	  }

	  /**
	   * This method, called after our test, just cleanup everything by closing the vert.x instance
	   *
	   * @param context the test context
	   */
	  @After
	  public void tearDown(TestContext context) {
	    vertx.close(context.asyncAssertSuccess());
	}

	  @Test
	  public void testMyApplication(TestContext context) {
	    // This test is asynchronous, so get an async handler to inform the test when we are done.
	    final Async async = context.async();

	    // We create a HTTP client and query our application. When we get the response we check it contains the 'Hello'
	    // message. Then, we call the `complete` method on the async handler to declare this async (and here the test) done.
	    // Notice that the assertions are made on the 'context' object and are not Junit assert. This ways it manage the
	    // async aspect of the test the right way.
	    vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
	      response.handler(body -> {
	        context.assertTrue(body.toString().contains("Hello"));
	        async.complete();
	      });
	    });
	
	  
/*  @Test
  public void testMyApplication(TestContext context) {
    final Async async = context.async();

    vertx.createHttpClient().getNow(8181, "localhost", "/api/cves/6532462d68fdce325b6ee0fadb6769511832c6d4524ab6da240add87133ecd1a2811de10892162304228508b4f834a32aeb1d93e1a1e73b2c38c666068cf3395",
     response -> {
      response.handler(body -> {
        context.assertTrue(body.toString().contains("2017-3159"));
        async.complete();
      });
    });*/
  }
}