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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RunWith(VertxUnitRunner.class)
public class ServerTest{

    private Vertx vertx;
    private Integer port;
    private HttpClient client;
    private static MongodProcess MONGO;
    private static int MONGO_PORT = 12345;

    @BeforeClass
    public static void initialize() throws IOException {
        MongodStarter starter = MongodStarter.getDefaultInstance();

        IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION)
                .net(new Net(MONGO_PORT, Network.localhostIsIPv6())).build();

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
     * This method instantiates a new Vertx and deploy the verticle. Then, it
     * waits in the verticle has successfully completed its start sequence
     * (thanks to `context.asyncAssertSuccess`).
     *
     * @param context
     *            the test context.
     */
    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();

        // Let's configure the verticle to listen on the 'test' port (randomly
        // picked).
        // We create deployment options and set the _configuration_ json object:
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)
                .put("db_name", "victims-v2").put("connection_string", "mongodb://localhost:" + MONGO_PORT));

        // We pass the options as the second parameter of the deployVerticle
        // method.
        vertx.deployVerticle(Server.class.getName(), options, context.asyncAssertSuccess());
        
        client = vertx.createHttpClient(getHttpClientOptions());

    }
    
    protected HttpServerOptions getHttpServerOptions() {
        return new HttpServerOptions().setPort(port).setHost("localhost");
    }

    protected HttpClientOptions getHttpClientOptions() {
        return new HttpClientOptions().setDefaultPort(port);
    }

    /**
     * This method, called after our test, just cleanup everything by closing
     * the vert.x instance
     *
     * @param context
     *            the test context
     */
    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

/*    @Test
    public void testHealthz(TestContext context) {
        // This test is asynchronous, so get an async handler to inform the test
        // when we are done.
        final Async async = context.async();

        // We create a HTTP client and query our application. When we get the
        // response we check it contains the 'Hello'
        // message. Then, we call the `complete` method on the async handler to
        // declare this async (and here the test) done.
        // Notice that the assertions are made on the 'context' object and are
        // not Junit assert. This ways it manage the
        // async aspect of the test the right way.
        vertx.createHttpClient().getNow(port, "localhost", "/healthz", response -> {
            context.assertEquals(response.statusCode(), 200);
            async.complete();
        });

    }*/
    
    @Test 
    public void sendFileUploadRequest(TestContext context) throws Exception {
        final Async async = context.async();
        
        Buffer fileData = vertx.fileSystem().readFileBlocking("src/test/resources/camel-snakeyaml-2.17.4.jar");
        String name = "somename";
        String fileName = "camel-snakeyaml";
        String contentType = "application/octet-stream";
        testRequest(HttpMethod.POST, "/upload", req -> {
            String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
            Buffer buffer = Buffer.buffer();
            String header = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + name
                    + "\"; filename=\"" + fileName + "\"\r\n" + "Content-Type: " + contentType + "\r\n"
                    + "Content-Transfer-Encoding: binary\r\n" + "\r\n";
            buffer.appendString(header);
            buffer.appendBuffer(fileData);
            String footer = "\r\n--" + boundary + "--\r\n";
            buffer.appendString(footer);
            req.headers().set("content-length", String.valueOf(buffer.length()));
            req.headers().set("content-type", "multipart/form-data; boundary=" + boundary);
            req.write(buffer);
        }, 200, "OK", null);
        async.complete();
    }

    protected void testRequest(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction,
            int statusCode, String statusMessage,
            String responseBody) throws Exception {
        testRequestBuffer(method, path, requestAction, statusCode, statusMessage, responseBody != null ? Buffer.buffer(responseBody) : null);
}

    protected void testRequestBuffer(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction, int statusCode,
            String statusMessage, Buffer responseBodyBuffer) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClientRequest req = client.request(method, port, "localhost", path, resp -> {
            assertEquals(statusCode, resp.statusCode());
            assertEquals(statusMessage, resp.statusMessage());
            if (responseBodyBuffer == null) {
                latch.countDown();
            } else {
                resp.bodyHandler(buff -> {
                    assertEquals(responseBodyBuffer, buff);
                    latch.countDown();
                });
            }
        });
        if (requestAction != null) {
            requestAction.accept(req);
        }
        req.end();
        awaitLatch(latch);
    }
    
    protected void awaitLatch(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
}
