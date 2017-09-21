package com.redhat.victims;

import java.util.ArrayList;
import java.util.List;

import com.redhat.victims.domain.File;
import com.redhat.victims.domain.Hash;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
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

		// Bind "/" to our hello message.
		router.route("/").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html").end("<h1>Hello from my first Vert.x 3 application</h1>");
		});

		router.get("/api/whiskies").handler(this::getByCombined);

		// Create the HTTP server and pass the "accept" method to the request handler.
		vertx.createHttpServer().requestHandler(router::accept).listen(
				// Retrieve the port from the configuration,
				// default to 8080.
				config().getInteger("http.port", 8181), next::handle);
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

	private void getByCombined(RoutingContext routingContext) {
		System.out.println("in getByCombined");
		String hash = routingContext.request().getParam("hash");
		mongo.find(COLLECTION, new JsonObject("{\"hash\":\"" + hash + "\"}"), results -> {
			List<JsonObject> objects = results.result();
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
					.end(Json.encodePrettily(objects));
		});
		/*
		 * Block<Document> streamBlock = new Block<Document>() {
		 * 
		 * @Override public void apply(final Document document) {
		 * System.out.println(document.toJson()); response.write(document.toJson()); }
		 * };
		 */

		/*
		 * MongoCollection<Document> hashes= mongoDB.getCollection("hashes"); String
		 * hash = routingContext.request().getParam("hash");
		 * hashes.find(eq("hashes.sha512.combined", hash))
		 * .projection(fields(include("cves.id"))) .forEach(streamBlock);
		 * response.end();
		 */
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
			                  next.handle(Future.<Void>succeededFuture());
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
