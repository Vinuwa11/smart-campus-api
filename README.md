# Smart Campus – Sensor & Room Management API

**Student Name**: Vinuja Sathwara
**Student ID**: w2153007 / 20241189
**Module:** 5COSC022W – Client-Server Architectures  
**Technology Stack:** Java 11 · JAX-RS (Jersey 2) · Grizzly HTTP Server · Jackson JSON  
**Base URL:** `http://localhost:8080/api/v1`

---

## API Overview

This RESTful API manages the physical infrastructure of a Smart Campus. It exposes three primary resource collections:

| Resource | Base Path |
|---|---|
| Discovery | `GET /api/v1` |
| Rooms | `/api/v1/rooms` |
| Sensors | `/api/v1/sensors` |
| Sensor Readings (sub-resource) | `/api/v1/sensors/{sensorId}/readings` |

All request and response bodies use `application/json`. The API never exposes Java stack traces; all errors are returned as structured JSON with a meaningful status code.

### Resource Hierarchy

```
/api/v1
├── /rooms
│   ├── GET    – list all rooms
│   ├── POST   – create a room
│   └── /{roomId}
│       ├── GET    – get room detail
│       └── DELETE – decommission room (blocked if sensors present)
└── /sensors
    ├── GET    – list sensors (optional ?type= filter)
    ├── POST   – register sensor (validates roomId)
    └── /{sensorId}
        ├── GET    – get sensor detail
        ├── PUT    – update sensor
        ├── DELETE – remove sensor
        └── /readings
            ├── GET  – get reading history
            └── POST – append reading (updates currentValue; blocked if MAINTENANCE)
```

---

## Build & Run

### Prerequisites

- Java 11+ (JDK)
- Apache Maven 3.6+

## Sample curl Commands

### 1. Discovery
```bash
curl -s http://localhost:8080/api/v1 | jq
```

### 2. List all rooms
```bash
curl -s http://localhost:8080/api/v1/rooms | jq
```

### 3. Create a new room
```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"CONF-202","name":"Conference Room 202","capacity":20}' | jq
```

### 4. Register a new sensor (validates roomId)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-002","type":"CO2","status":"ACTIVE","currentValue":400.0,"roomId":"CONF-202"}' | jq
```

### 5. Filter sensors by type
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2" | jq
```

### 6. Post a sensor reading (also updates currentValue on parent sensor)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}' | jq
```

### 7. Attempt to delete a room that still has sensors (expect 409)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | jq
```

### 8. Attempt to post a reading to a MAINTENANCE sensor (expect 403)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5.0}' | jq
```

### 9. Attempt to register a sensor with an invalid roomId (expect 422)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"GHOST-999"}' | jq
```

### 10. Get reading history for a sensor
```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-001/readings | jq
```

---

## Conceptual Report (Answers to Specification Questions)

### Part 1.1 – JAX-RS Resource Lifecycle

JAX-RS by default instantiates a new resource class instance for every incoming HTTP request. 
This means the scope of a resource class instance is request, and any instance variables inside a 
resource class are not shared between requests and cannot be used to hold shared state. 
Because each request gets its own resource object, shared mutable data such as the collections of 
rooms and sensors must live outside the resource classes. This project addresses the concern through 
the DataStore singleton, which holds all data in memory for this project. This is very naive, and 
wouldn't work in production. 
Use ConcurrentHashMap instances. ConcurrentHashMap supports simultaneous access by readers 
and atomic updates by writers without manual use of synchronized methods or locks. A regular 
HashMap is not designed for concurrent access and can be easily corrupted or left in an inconsistent 
state by simultaneous puts or removes.  

### Part 1.2 – HATEOAS and Hypermedia

As part of this article on APIs, we’ll look into what HATEOAS (Hypermedia as the Engine of 
Application State) means and how it helps to improve client code. HATEOAS takes the opposite 
approach to that followed in this article and embeds the URLs in the body of the HTTP response, 
rather than using static documentation and manually-hardcoded URLs. In this API, the discovery 
endpoint is /api/v1. You can access it with a GET. The root of the API will return a link to 
/api/v1/rooms under the resource of rooms. From here, the client can follow the links to traverse the 
entire site, much like you would in a browser by clicking on HTML anchors. 
This is fantastic, as it significantly decouples the code for a client from the code for a server. I can 
imagine writing a page with a link, and hardcoding the URL to that page. Now a person following 
that link will be able to do so even after the server code has completely rearranged the URLs. Static 
documentation generated for the server code would go stale immediately, and force all of the clients 
to be updated manually whenever the server's routes changed.  

### Part 2.1 – Returning IDs vs Full Objects

Returning the full object is expensive bandwidth and slow for the API to return, but it means that the 
client has to write N additional requests to get details for each one, which is a classic N+1 problem. 
However, returning more objects per page in this format is more expensive bandwidth per call than 
returning the lightweight form, and decreases both the latency of the call and the complexity of the 
client code. A dropdown might be fine just returning the IDs, but a dashboard might want to show a 
2  
table of all the rooms, and this would be much easier for the client if the API returned the full 
objects for the list endpoint.  

### Part 2.2 – Idempotency of DELETE

This is correct REST behaviour. The DELETE operation is idempotent. I can see with a single call 
that the room has been removed (204 No Content), and that further calls for the same room ID will 
also return 204 No Content. The server no longer knows of the room, not because the subsequent 
calls do anything different to the first call for the room. The server state is the same after the first 
deletion as it would be after any number of retries of the deletion request. This is critical in 
unreliable networks where the client may not even know that the first request for a DELETE was 
sent to the server, and must be able to safely retry the request.  

### Part 3.1 – @Consumes and Media Type Mismatch

This resource only accepts JSON formatted requests, indicated by 
@Consumes(MediaType.APPLICATION_JSON). Therefore, any requests for this resource with a 
Content-Type of text/plain (or application/xml) will instead receive a 415 status with the message 
“Unsupported Media Type” because the JAX-RS runtime will silently prevent such requests from 
reaching the resource method body and never execute the code inside the method. The production of 
different media types was handled declaratively on the server side of the web service, and the same 
declarative approach is used to handle different media types on the client side of web services, 
according to the JAX-RS specification.  

### Part 3.2 – @QueryParam vs. Path-Based Filtering

This parameter filters/scoes the result to only records that contain type information of type CO2. The 
base URI of /api/v1/sensors specifies the collection of all sensors of this endpoint. The query string 
then filters the result so that only members of the collection of sensors that are relevant are included 
in the result. According to RFC 3986, query parameters are used to filter the result of a resource, but 
are not providing new resources themselves. 
Remove the path segment /api/v1/sensors/type/CO2; type is a sub-resource of sensors and the 
current path segment clashes with the alternative path segment /api/v1/sensors/{sensorId} which is 
used to retrieve a particular sensor. Mechanisms such as ordering can be used to distinguish between 
sensor ID and the literal string 'type'. Path-based multi-filtering is very unnatural and harder to use, 
query parameters are composable so ?type=CO2&status=ACTIVE is natural and correct. 

### Part 4.1 – Sub-Resource Locator Patten

The URLLocation sub-resource locus pattern continues. One of the nested URL segments is turned 
into a separate class. In this project, the SensorResource is responsible for the location to the 
SensorReadingResource. This SensorResource uses a method locator to determine the object that 
3  
will be handled by the JAX-RS runtime. So the runtime will then determine the HTTP method (e.g. 
GET, POST) for this object. 
This approach provides several architectural benefits: 
We have followed the single responsibility principle. Each class should do one job. For example, 
SensorResource does one job (sensors) and SensorReadingResource does one job (historical 
readings from a particular sensor). 

Single Responsibility Principle 

The Single Responsibility Principle (SRP) states that each class should have a single responsibility. 
That way your code is easier to modify should you need to add new functionality because a single 
purpose class is much easier to extend than a multi purpose class. Second, it makes your code much 
easier to understand because each class has one role or responsibility instead of several. 
Thirdly, use of Locators helps to avoid the God controller anti-pattern whereby one large class 
attempts to control dozens of independent workflows and becomes impossible to maintain as the 
API grows. Using Locators allows the URL hierarchy of your application to reflect the hierarchy of 
your domain model.  

### Part 5.2 – HTTP 422 vs. 404 for Missing Referenced Resource

Although the request body is valid JSON and the path /api/v1/sensors exists, attempting to POST a 
new sensor specifying an unknown roomId results in a 404 Not Found error. Typically a 404 error 
code indicates that the resource addressed by a URI could not be found. In this case the sensors 
collection has been found so the error is misleading. 
I've changed the status code from 400 to HTTP 422 Unprocessable Entity, which semantically is a 
better choice because it means “The server understands the content format specified for the request, 
but after interpretation of the contained instructions, it was unable to proceed with processing.” My 
semantic error in this case is that the request body contains an invalid roomId, but this status code 
helps developers debug integration errors faster because the error message is now indicating that the 
problem is with the request body (as opposed to the roomId portion of the URL). 

### Part 5.4 – Security Risk of Exposing Stack Traces

Java stack traces are notorious for leaking sensitive data, which can be exploited by attackers in 
several different ways.  
Java stack traces are also known to provide insight into the underlying code structure and the 
runtime configuration, making them a highly prized target for attackers engaged in code injection 
attacks.  
New Analysis:  
Technology fingerprinting of NPM modules provides Class names, package structures and third
party library names that expose the specific software stack including versions.  
Utilizing these “fingerprints” when searching for relevant CVEs results in more accurate and 
relevant search results.  
Internal code structure,  Method names and line numbers give away internal application structure. 
You can probably figure out a couple of code paths that you would want to instrument.  
File system paths: Most stack traces contain the absolute file path for an internal framework 
method and thus reveal details of the server's configuration and file hierarchy.  
Exposed Data : Exception messages may contain portions of database query code as well as 
parameter values, revealing details about data schemas as well as user supplied data.  
The API returns a generic error of 500 Internal Server Error with a JSON body to the client but logs 
the full trace on the server only. This meets the principle of least privilege by ensuring the client is 
informed that something has gone wrong but is not provided with enough detail to aid an attacker.  

### Part 5.5 – Filters vs. Inline Logging

I've seen resources where every single method has a Logger.info() statement (e.g. GET, POST, PUT, 
DELETE, etc.). This is a violation of the DRY (Don't Repeat Yourself) principle, and tightly couples 
a cross-cutting concern (observability) directly to your application's business logic. Now, every time 
the format of a log changes, every place that a logger statement is found has to be updated. The poor 
new developer assigned the task of adding a new endpoint to the resource may even forget to add 
the logger statement. Now, you have a blind spot in the observability of the resource.  
JAX-RS filters are implemented as filters following the interceptor pattern. This means I can write 
the logging code once in a filter, the framework will then guarantee that this filter runs for every 
incoming request and every outgoing response - now and in the future. The JAX-RS resource 
methods remain as simple and straight forward as they should be, following their primary function. 

---

## Error Response Format

All error responses follow this consistent JSON structure:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Cannot delete room 'LIB-301': it still has active sensors assigned.",
  "timestamp": 1713700000000
}
```

| Scenario | Status Code |
|---|---|
| Resource not found | 404 |
| Room still has sensors | 409 |
| Referenced roomId missing | 422 |
| Sensor in MAINTENANCE | 403 |
| Unexpected server error | 500 |
| Wrong Content-Type | 415 (JAX-RS automatic) |
