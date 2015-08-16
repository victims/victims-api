# victims-api
Future victi.ms microservice

## Why split Web and Api? 
The idea is to increase the scalability and development of the Web service by having a code base that our lyrics deals with service calls. 

- API can be updated / deployed without modifying the Web UI
- API and Web can scale horizontally according to their specific needs
- API Version become releases rather than holding over multiple versions in a single code base
- API and Web can use different technologies
