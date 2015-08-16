# victims-api
Future victi.ms microservice. This is a work in progress.

## Why split Web and Api? 
The idea is to increase the scalability and development of the Web service by having a code base that only deals with service calls. 

- API can be updated / deployed without modifying the Web UI
- API and Web can scale horizontally according to their specific needs
- API Version become releases rather than holding over multiple versions in a single code base
- API and Web can use different technologies

## Building
```
$ git clone https://github.com/victims/victims-api
$ go clean
$ go build -a ./...
$ go build -a .
```

## Running

```
$ ./victims-api --BindAddress="127.0.0.1" --BindPort=8080 --MongoDatabase=victims --MongoURI=mongodb://127.0.0.1/
```

### CLI Options
```
Usage of ./victims-api:
      --BindAddress="127.0.0.1": Bind address.
      --BindHttpsPort=0: HTTPS bind port.
      --BindPort=8080: HTTP bind port.
      --CertFile="": Cert file.
      --KeyFile="": Key file.
      --LogFile="": Log file.
      --LogLevel="info": Log level.
      --MaxHeaderBytes=1048576: Max header bytes.
      --MongoDatabase="victims": MongoDB database
      --MongoURI="mongodb://127.0.0.1/": MongoDB server URI
      --ReadTimeout=10s: Read timeout.
      --WriteTimeout=10s: Write timeout.
```

## Proposed Endpoints

### Product

Product view of hashes.

Partially implemented

#### Endpoint
GET ```/service/v3/product/{name}/```

#### Filters
 - **version**: A specific version to query for. If used with **endversion** then it is the starting version.
 - **endversion**: The last version in a version range.

### Group

Shows what groups are available.

#### Endpoint
GET ```/service/v3/groups/```

Implemented

#### Filters
None

### CVE

Provides CVE information.

#### Endpoint
GET ```/service/v3/cve/{id}/```

#### Filters
None

### Hash

Returns a specific Hash instance

Partially implemented

#### Endpoint
GET ```/service/v3/hash/{hash}/```

#### Filters
None

### Sync

Syncing hash data to clients

#### Endpoint
GET ```/service/v3/sync/{since}/```

#### Filters
None

### Inspect

Inspect hashes and return Hash instances if found

#### Endpoint
PUT ```/service/v3/inspect/{group}/```

#### Payload
```json
['hash', 'hash', ...]
```