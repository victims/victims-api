package com.redhat.victims;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;

import org.bson.Document;

import com.redhat.victims.domain.File;
import com.redhat.victims.domain.Hash;
import com.redhat.victims.fingerprint.Algorithms;
import com.redhat.victims.fingerprint.JarFile;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;

public class Server extends AbstractVerticle {
	protected static final String HASHES_COLLECTION = "hashes";
	private static boolean isTestingEnv = false;
	private MongoClient mongo;
	private WebClient client;
	

	@Override
	public void start(Future<Void> fut) {
		mongo = MongoClient.createShared(vertx, config());

		startWebApp((http) -> completeStartup(http, fut));
		
		client = WebClient.create(vertx);
		
		Boolean testing = config().getBoolean("testing");
		if(testing != null)
			isTestingEnv = testing;
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
	
	//TODO Require Basic Auth and check credentials on Github :https://developer.github.com/v3/auth/#basic-authentication
	private void upload(RoutingContext ctx) {
		HttpServerRequest request = ctx.request();
		String authorizationHeader = request.getHeader("Authorization");
		validateCredentials(authorizationHeader, ctx);
		ctx.response().end();

	}

	private Optional<String> memberOfVictims(String authorizationHeader) {
		Optional<String> result = Optional.empty();
		Base64.getDecoder().decode(authorizationHeader.getBytes());
		return result;
	}

	private void validateCredentials(String authorizationHeader, RoutingContext ctx) {
		if(isTestingEnv) {
			ctx.response().setStatusCode(200);
			processFiles(ctx, "");
			return;
		}
		client
		  .get(443, "api.github.com", "/user").ssl(true).putHeader("Authorization", authorizationHeader)
		  .send(ar -> {
		    if (ar.succeeded()) {
		    	if(ar.result().statusCode() == 200) {
		    		checkVictimsMembership(authorizationHeader, ctx);
		    	} else {
		    		ctx.response().setStatusCode(401);
		    	}
		    } else {
		      ctx.response().setStatusCode(401);
		    }
		  });	
	}

	private void checkVictimsMembership(String authorizationHeader, RoutingContext ctx) {
		client
			.get(443, "api.github.com", "/orgs/victims/members").ssl(true)
			.send(ra -> {
				if(ra.succeeded()) {
					String submitter = getUsername(authorizationHeader);
					
					//check for user
				    HttpResponse<Buffer> response = ra.result();
				    JsonArray body = response.bodyAsJsonArray();
				    Iterator<Object> it = body.iterator();
					while(it.hasNext()) {
						JsonObject result = (JsonObject) it.next();
						String login = result.getString("login");
						if(login.equals(submitter)) {
							processFiles(ctx, submitter);
							ctx.response().setStatusCode(200);
							break;
						}
					}
					
					
				}
			
			});
	}

	private String getUsername(String authorizationHeader) {
		String stripped = ":";
		if(authorizationHeader.startsWith("Basic "))
			stripped = authorizationHeader.substring("Basic ".length());
		String decodedHeader = new String(Base64.getDecoder().decode(stripped.getBytes()));
		String submitter = decodedHeader.substring(0, decodedHeader.indexOf(':'));
		return submitter;
	}

	private void processFiles(RoutingContext ctx, String submitter) {
		String cve = ctx.request().getParam("cve");
		for (FileUpload f : ctx.fileUploads()) {
			Path uploadedFile = Paths.get(f.uploadedFileName());
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
			updateDatabase(jarFile, cve, ctx, submitter);
		}
	}

	private void updateDatabase(JarFile jarFile, String cve, RoutingContext ctx, String submitter) {
		//TODO add submitter
		Hash hash = new Hash(jarFile, cve, submitter);
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
		
		mongo.updateCollectionWithOptions(HASHES_COLLECTION, query, update, new UpdateOptions(true), updateResult ->{
			if(updateResult.failed()) {
				ctx.response().setStatusCode(500)
					.setStatusMessage("Failed to add hash");
			}else {
				ctx.response().setStatusCode(200);
			}
		});
	}
	
}
