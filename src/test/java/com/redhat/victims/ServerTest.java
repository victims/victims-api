package com.redhat.victims;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
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

import org.bson.Document;
import org.junit.*;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.Base64;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.Block;
import com.mongodb.async.AsyncBatchCursor;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.redhat.victims.domain.Hash;
import com.redhat.victims.fingerprint.JarFile;

@RunWith(VertxUnitRunner.class)
public class ServerTest {

	protected static final String TEST_RESOURCES = "src/test/resources/";
    private static final String TEST_DB = "victims-it";
    private static final String SNAKEYAML = "camel-snakeyaml-2.17.4.jar";;
    private Vertx vertx;
    private Integer port;
    private HttpClient client;
    private static MongoClient mongo;
    private static MongodProcess MONGO;
    private static int MONGO_PORT = 12345;

    @BeforeClass
    public static void initialize() throws IOException {
        MongodStarter starter = MongodStarter.getDefaultInstance();

        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Versions.withFeatures(new GenericVersion("3.2.4"), Feature.SYNC_DELAY))
                .net(new Net(MONGO_PORT, Network.localhostIsIPv6())).build();

        MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
        MONGO = mongodExecutable.start();

        mongo = MongoClients.create("mongodb://localhost:" + MONGO_PORT);
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
                .put("db_name", TEST_DB).put("connection_string", "mongodb://localhost:" + MONGO_PORT)
        		.put("testing", true));

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

    @Test
    public void testHealthz(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(port, "localhost", "/healthz", response -> {
            context.assertEquals(response.statusCode(), 200);
            async.complete();
        });

    }

    @Test
    public void sendCamelSnakeUploadUpsertRequest(TestContext context) throws Exception {
        final Async async = context.async();
        //insert a hash first
        JarFile jarFile = new JarFile(TEST_RESOURCES + SNAKEYAML);
        Hash hash = new Hash(jarFile, "2017-9999", "");
        MongoDatabase testDB = mongo.getDatabase(TEST_DB);
        MongoCollection<Document> hashes = testDB.getCollection(Server.HASHES_COLLECTION);
        hashes.insertOne(hash.asDocument(), new SingleResultCallback<Void>() {

            @Override
            public void onResult(Void result, Throwable arg1) {
                
                try {
                    //Then send a request to persist the same hash with a different CVE
                    sendFile("camel-snakeyaml-2.17.4.jar", "2017-3159", 200, "OK");
                } catch (Exception e) {
                    context.fail(e);
                }
                
                hashes.find(eq("hash",
                        "3cfc3c06a141ba3a43c6c0a01567dbb7268839f75e6367ae0346bab90507ea09c9ecd829ecec3f030ed727c0beaa09da8c08835a8ddc27054a03f800fa049a0a"))
                        .batchCursor(new SingleResultCallback<AsyncBatchCursor<Document>>() {

                            @Override
                            public void onResult(AsyncBatchCursor<Document> result, Throwable t) {
                                result.next(new SingleResultCallback<List<Document>>() {

                                    @Override
                                    public void onResult(List<Document> result, Throwable t) {
                                        for (Document doc : result) {
                                            //verify the hash was updated
                                            List<String> cveList = (List<String>) doc.get("cves");
                                            context.assertTrue(cveList.contains("2017-3159"));
                                            async.complete();
                                        }
                                    }

                                });

                            }
                        });
            }

        });

    }

    @Test
    public void send2Struts2UploadRequest(TestContext context) throws Exception {
        final Async async = context.async();
        sendFile("struts2-core-2.5.12.jar", "2017-9805", 200, "OK");
        async.complete();

    }

    @Test
    public void sendUnsupportedType(TestContext context) throws Exception {
        final Async async = context.async();
        sendFile("freckles-0.2.1.tar.gz", "2017-1111", 501, "Not Implemented");
        async.complete();
    }

    private void sendFile(final String fileName, final String cve, int expectedStatusCode, String expectedBody)
            throws Exception {
        testRequest(HttpMethod.POST, "/upload/" + cve, req -> {
            fileUploadRequest(fileName, req);
        }, expectedStatusCode, expectedBody, null);
    }

    private void fileUploadRequest(final String fileName, HttpClientRequest req) {
        Buffer fileData = vertx.fileSystem().readFileBlocking(TEST_RESOURCES + fileName);
        String contentType = "application/octet-stream";
        String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
        Buffer buffer = Buffer.buffer();
        String header = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + fileName
                + "\"; filename=\"" + fileName + "\"\r\n" + "Content-Type: " + contentType + "\r\n"
                + "Content-Transfer-Encoding: binary\r\n" + "\r\n";
        buffer.appendString(header);
        buffer.appendBuffer(fileData);

        String footer = "\r\n--" + boundary + "--\r\n";
        buffer.appendString(footer);
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "multipart/form-data; boundary=" + boundary);
        String encodedCredentials = Base64.getEncoder().encodeToString("testuser:testpass".getBytes());
        req.headers().set("Authorization", "Basic " + encodedCredentials);
        req.write(buffer);
    }

    protected void testRequest(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction,
            int statusCode, String statusMessage, String responseBody) throws Exception {
        testRequestBuffer(method, path, requestAction, statusCode, statusMessage,
                responseBody != null ? Buffer.buffer(responseBody) : null);
    }

    protected void testRequestBuffer(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction,
            int statusCode, String statusMessage, Buffer responseBodyBuffer) throws Exception {
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
