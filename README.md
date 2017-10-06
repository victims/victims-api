## Victims API

Victims is a service designed to track vulnerabilities in libraries. We currently support Java libraries, but support may be added for other libraries in future.

### How does it work?

the `upload` endpoint is called passing a CVE, and file. This service uses the victims-lib-java project to generate a hash of the file and files contained within it. Those hashes are then stored in a MongoDB database along with the relevant CVE identifier.

### API Documentation

#### POST /upload/{cve} 

```
curl -X POST -u jasinner:<github personal access token> -F "library=@src/test/resources/struts2-core-2.5.12.jar" http://localhost:8080/upload/2017-9805
```

*User must be a member of `victims` Github organisation*

#### GET /cves/hash

Use a SHA512 checksum of a library to check if it's got any linked CVE references

```
$ curl http://localhost:8080/cves/4787f28235b320d7e9e0945a9e0303fae55e49a2f0e938594fd522939bdab65842cd377a2bb051519e2f5de80a6317297056d427a315c02a3bb6e923de9efa78
{
  "cves" : [ "2017-9805" ]
}
```

#### GET /healthz

Checks the service is up and can access the database

```
[jshepher@localhost victims-api]$ curl -v http://localhost:8080/healthz

> GET /healthz HTTP/1.1
> Host: localhost:8080
> Accept: */*
> 
< HTTP/1.1 200 OK
< 

```

### Building this service

Use maven to build the service:
`mvn clean package`

Run the Integration Tests:
`mvn clean verify`

Run a local service:
`java -jar java -jar target/victims-v2-1.0-SNAPSHOT-fat.jar -conf src/main/resources/config.js`

### Running with Docker

Package the service as a Docker image using S2I:
`s2i build . redhat-openjdk-18/openjdk18-openshift victims-api`

Run the image passing MongoDB environment variables:
`docker run -d -p 8080:8080 -e MONGODB_HOST=172.17.0.1 -e MONGODB_DATABASE=victims-it -e MONGODB_PORT=27017 victims-api`
