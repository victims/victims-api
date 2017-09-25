package com.redhat.victims;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.redhat.victims.domain.File;
import com.redhat.victims.domain.Hash;
import com.redhat.victims.fingerprint.JarFile;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.mongo.MongoClient;

public class Server extends AbstractVerticle {
    private static final String COLLECTION = "hashes";
    // mongo = MongoClient.createShared(vertx, config());
    private MongoClient mongo;

    @Override
    public void start(Future<Void> fut) {
        mongo = MongoClient.createShared(vertx, config());

        createSomeData((nothing) -> startWebApp((http) -> completeStartup(http, fut)), fut);
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        // Create a router object.
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));

        router.get("/healthz").handler(this::healthz);

        router.get("/api/cves/:hash").handler(this::getByCombined);

        // handle the form
        router.post("/upload").handler(this::upload);

        System.out.println("Starting server at:" + config().getInteger("http.port", 8082));

        // Create the HTTP server and pass the "accept" method to the request
        // handler.
        vertx.createHttpServer().requestHandler(router::accept).listen(
                // Retrieve the port from the configuration,
                // default to 8080.
                config().getInteger("http.port", 8082), next::handle);
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
        } else {
            fut.fail(http.cause());
        }
    }

    @Override
    public void stop() throws Exception {
        mongo.close();
    }

    private void healthz(RoutingContext routingContext) {
        mongo.getCollections(results -> {
            HttpServerResponse response = routingContext.response();
            if (results.failed())
                response.setStatusCode(500).end();
            else
                response.setStatusCode(200).end();
        });
    }

    private void getByCombined(RoutingContext routingContext) {
        String hash = routingContext.request().getParam("hash");
        mongo.find(COLLECTION, new JsonObject("{\"hash\":\"" + hash + "\"}"), results -> {
            List<JsonObject> objects = results.result();
            String result = Json.encodePrettily(objects);
            System.out.println(result);
            routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
                    .end(result);
        });
    }

    private void upload(RoutingContext ctx) {
        ctx.response().putHeader("Content-Type", "text/plain");

        ctx.response().setChunked(true);

        for (FileUpload f : ctx.fileUploads()) {
            System.out.println("uploaded file name: " + f.uploadedFileName());
            Path uploadedFile = Paths.get(f.uploadedFileName());
            System.out.println("absolute path" + uploadedFile.toAbsolutePath());
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(Files.readAllBytes(uploadedFile), f.fileName());
            } catch (IOException e) {
                e.printStackTrace();
                ctx.response().setStatusCode(500);
            }
            System.out.println(Json.encodePrettily(jarFile));
        }

        ctx.response().end();

    }

    private void createSomeData(Handler<AsyncResult<Void>> next, Future<Void> fut) {
        List<String> cves = new ArrayList<String>();
        cves.add("2017-3159");
        List<File> files = new ArrayList<File>();
        files.add(new File("7c3728d2149df93f39c4d55cf5e3f835fd87aaab",
                "org/apache/camel/component/snakeyaml/SnakeYAMLDataFormat.class"));
        Hash camelSnake = new Hash(
                "6532462d68fdce325b6ee0fadb6769511832c6d4524ab6da240add87133ecd1a2811de10892162304228508b4f834a32aeb1d93e1a1e73b2c38c666068cf3395",
                "camel-snakeyaml-2.17.3", "jar", cves, "adminuser", files);

        System.out.println(Json.encode(camelSnake));

        // Do we have data in the collection ?
        mongo.count(COLLECTION, new JsonObject(), count -> {
            if (count.succeeded()) {
                if (count.result() == 0) {
                    // no hashes, insert data
                    mongo.insert(COLLECTION, new JsonObject(Json.encode(camelSnake)), ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                        } else {
                            next.handle(Future.<Void> succeededFuture());
                        }
                    });
                }
            } else {
                // report the error
                fut.fail(count.cause());
            }
        });
    }

}
