package com.redhat.victims;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.logging.Logger;

import com.redhat.victims.domain.Hash;
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
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;

public class Server extends AbstractVerticle {
    
    private static final int DEFAULT_PORT = 8080;
    private static final Logger LOG = Logger.getLogger(Server.class.getName());
	protected static final String HASHES_COLLECTION = "hashes";
	private static boolean isTestingEnv = false;
	private MongoClient mongo;
	private WebClient client;
	
	@Override
	public void start(Future<Void> fut) {
		JsonObject config = config();
		//config keys can be found at http://vertx.io/docs/vertx-mongo-client/java/
		setFromEnv(config, "host", "MONGODB_HOST");
		setFromEnv(config, "db_name", "MONGODB_DATABASE");
		setFromEnv(config, "port", "MONGODB_PORT");
		mongo = MongoClient.createShared(vertx, config);

		startWebApp((http) -> completeStartup(http, fut));
		
		client = WebClient.create(vertx);
		
		Boolean testing = config.getBoolean("testing");
		if(testing != null)
			isTestingEnv = testing;
	}

	private void setFromEnv(JsonObject config, String configKey, String envVarName) {
		String mongoDBHost = System.getenv(envVarName);
		if(mongoDBHost != null) {
			config.put(configKey, mongoDBHost);
		}
	}

	private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
		Router router = Router.router(vertx);

		router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));

		router.get("/healthz").handler(this::healthz);

		router.get("/cves/:hash").handler(this::getByCombined);

		// handle the form
		router.post("/upload/:cve").handler(this::upload);

		LOG.info("Starting server at:" + config().getInteger("http.port", DEFAULT_PORT));

		vertx.createHttpServer().requestHandler(router::accept).listen(
				config().getInteger("http.port", DEFAULT_PORT), next::handle);
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
			if (results.failed()) {
				response.setStatusCode(500).end();
			    LOG.severe("Healthz check failed: " + results.cause().getMessage());
			}
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

	private void validateCredentials(String authorizationHeader, RoutingContext ctx) {
		if(isTestingEnv) {
		    LOG.info("Testing flag enabled skipping credentials check");
			ctx.response().setStatusCode(200);
			processFiles(ctx, "");
			return;
		}
		String username = getUsername(authorizationHeader);
        LOG.fine("Checking credentials of user: " + username);
		client
		  .get(443, "api.github.com", "/user").ssl(true).putHeader("Authorization", authorizationHeader)
		  .send(ar -> {
		    if (ar.succeeded()) {
		    	if(ar.result().statusCode() == 200) {
		    		checkVictimsMembership(authorizationHeader, ctx);
		    	} else {
		    		ctx.response().setStatusCode(401);
		    		LOG.warning("Invalid credentials for user: " + username);
		    	}
		    } else {
		      ctx.response().setStatusCode(500);
		      LOG.warning("Credentials check failed");
		    }
		  });	
	}

	private void checkVictimsMembership(String authorizationHeader, RoutingContext ctx) {
		client
			.get(443, "api.github.com", "/orgs/victims/members").ssl(true)
			//Authorization is not required but helps prevent rate limiting
			.putHeader("Authorization", authorizationHeader)
			.send(ra -> {
				if(ra.succeeded()) {
					String submitter = getUsername(authorizationHeader);
					boolean isMember = false;
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
							isMember = true;
							break;
						}
					}
					if(!isMember)
					    LOG.severe(submitter + " not a member of Github organization 'victims'");
					
				}
				else {
				    LOG.severe("Failed to get members in 'victims' organisation: " + ra.result().bodyAsString());
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
		    LOG.info("Processing file: " + f.fileName());
			Path uploadedFile = Paths.get(f.uploadedFileName());
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(Files.readAllBytes(uploadedFile), f.fileName());
			    String filetype = jarFile.getRecord().filetype();
                if(!filetype.equals(".jar")) {
			    	ctx.response().setStatusCode(501)
			    		.setStatusMessage("Not Implemented");
			    	LOG.warning("Invalid file type: " + filetype);
			    	break;
			    }
			} catch (IOException e) {
				ctx.response().setStatusCode(500)
				.setStatusMessage(e.getMessage());
				LOG.severe("Error reading from file: " + f.uploadedFileName());
				break;
			}
			updateDatabase(jarFile, cve, ctx, submitter, uploadedFile);
		}
	}

	private void updateDatabase(JarFile jarFile, String cve, RoutingContext ctx, String submitter, Path uploadedFile) {
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
		        cleanup(uploadedFile);
			}else {
				ctx.response().setStatusCode(200);
                LOG.fine("Persisted hash to database");
                cleanup(uploadedFile);
			}
		});
	}

    private void cleanup(Path uploadedFile) {
        try {
            Files.delete(uploadedFile);
        } catch (IOException e) {
            LOG.severe("Error deleting file" + uploadedFile.getFileName());
        }
    }
	
}
