package com.redhat.victims;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Server extends AbstractVerticle {
    private MongoDatabase mongoDB;

    @Override
    public void start(Future<Void> fut) {
     initMongo();
     
     Router router = Router.router(vertx);
     router.get("/api/combined/:hash").handler(this::getByCombined);
     
      vertx
          .createHttpServer()
          .requestHandler(router::accept)
          .listen(8181, result -> {
            if (result.succeeded()) {
              fut.complete();
            } else {
              fut.fail(result.cause());
            }
          });
    }
    
    private void getByCombined(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json; charset=utf-8")
                .setChunked(true);
        
        Block<Document> streamBlock = new Block<Document>() {
            @Override
            public void apply(final Document document) {
                System.out.println(document.toJson());
                response.write(document.toJson());
            }
        };
        
        MongoCollection<Document> hashes= mongoDB.getCollection("hashes");
        String hash = routingContext.request().getParam("hash");
        hashes.find(eq("hashes.sha512.combined", hash))
            .projection(fields(include("cves.id")))
            .forEach(streamBlock);
        response.end();
    }

    private void initMongo() {
        MongoClientURI connectionString = new MongoClientURI(getConnectionString());
        MongoClient mongoClient = new MongoClient(connectionString);
        mongoDB = mongoClient.getDatabase(getDatabaseString());
    }

    private String getDatabaseString() {
        return config().getString("mongoDatabase","victims");
    }

    private String getConnectionString() {
        return "mongodb://" + config().getString("mongoHost", "localhost") + ':' +
                config().getInteger("mongoPort", 27017);
    }
}
