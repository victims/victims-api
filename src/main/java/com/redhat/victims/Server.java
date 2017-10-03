package com.redhat.victims;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.redhat.victims.domain.File;
import com.redhat.victims.domain.Hash;
import com.redhat.victims.fingerprint.Algorithms;
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
import io.vertx.ext.mongo.UpdateOptions;

public class Server extends AbstractVerticle {
	protected static final String HASHES_COLLECTION = "hashes";
	// mongo = MongoClient.createShared(vertx, config());
	private MongoClient mongo;

	@Override
	public void start(Future<Void> fut) {
		mongo = MongoClient.createShared(vertx, config());

		startWebApp((http) -> completeStartup(http, fut));
	}

	private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
		// Create a router object.
		Router router = Router.router(vertx);

		router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));

		router.get("/healthz").handler(this::healthz);

		router.get("/api/cves/:hash").handler(this::getByCombined);

		// handle the form
		router.post("/upload/:cve").handler(this::upload);

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
		JsonObject projection = new JsonObject("{\"_id\":0, \"cves\":1}");
		mongo.findOne(HASHES_COLLECTION, new JsonObject("{\"hash\":\"" + hash + "\"}"), projection, results -> {
			JsonObject result = results.result();
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
					.end(Json.encodePrettily(result));
		});
	}

	/*{
	 * 	"id":"",
	 * 	"hash":"4787f28235b320d7e9e0945a9e0303fae55e49a2f0e938594fd522939bdab65842cd377a2bb051519e2f5de80a6317297056d427a315c02a3bb6e923de9efa78",
	 *  "name":"struts2-core-2.5.12.jar",
	 *  "format":"SHA512",
	 *  "cves":["2017-9805"],
	 *  "submitter":"",
	 *  "files":[{"hash":"c19224b32c115c98e52e5290a6da10cd135fac6097e3bbf7f0e22c303146ac3f7dba70b10b67b6e80d12a6ddf29e803b9c7b79286e5ef2b56b520fa70aa55283","name":"SHA512"}]
	 */
	
	private void upload(RoutingContext ctx) {
		String cve = ctx.request().getParam("cve");
		ctx.response().putHeader("Content-Type", "text/plain");
		ctx.response().setChunked(true);

		for (FileUpload f : ctx.fileUploads()) {
			Path uploadedFile = Paths.get(f.uploadedFileName());
			// System.out.println("absolute path" + uploadedFile.toAbsolutePath());
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(Files.readAllBytes(uploadedFile), f.fileName());
			    if(!jarFile.getRecord().filetype().equals(".jar")) {
			    	ctx.response().setStatusCode(501)
			    		.setStatusMessage("Not Implemented");
			    	break;
			    }
			} catch (IOException e) {
				ctx.response().setStatusCode(500)
				.setStatusMessage(e.getMessage());
				break;
			}
			//TODO add submitter
			Hash hash = new Hash(jarFile, cve, "");
			//query for existing hash
			JsonObject query = new JsonObject();
			query.put("hash", hash.getHash());
			
			JsonObject update = new JsonObject();
			
			//only add to cve list if hash is found
			JsonObject newCve = new JsonObject();
			newCve.put("cves", cve);
			update.put("$addToSet", newCve);
			
			//if not found insert other values as well
			update.put("$setOnInsert", hash.asDocument(false));
			
			System.out.println("-----doing update query");
			mongo.updateCollectionWithOptions(HASHES_COLLECTION, query, update, new UpdateOptions(true), updateResult ->{
				if(updateResult.failed()) {
					ctx.response().setStatusCode(500)
						.setStatusMessage("Failed to add hash");
				}else {
					ctx.response().setStatusCode(200);
				}
			});
		}
		ctx.response().end();

	}

}
