# shop-rest

A small shop REST API built with **Jakarta REST (JAX-RS) 4.0**, **Jersey 4.0.0** and an embedded **Grizzly** server, with no Spring. It is the project for Lab-04, RESTful Web Services. The data lives in memory, so everything is reset when the server restarts.

What it covers:

| Part | Topic | Status |
|------|-------|--------|
| Task 1 | First resource and server | done |
| Task 2 | Full CRUD, status codes, headers, JSON and XML | done |
| Task 3 | Parameters, Bean Validation, RFC 9457 problem+json | done |
| Task 4 | Filters and interceptors (correlation id, CORS, API key, gzip) | done |
| Task 5 | Pagination, caching, versioning, sub-resource | done |
| Challenge | Orders with HATEOAS, idempotent POST, OpenAPI, JerseyTest suite, JAX-RS vs Spring MVC | done |

## Requirements

- JDK 25
- Maven 3.9 or newer

## How to run

```
mvn -q process-classes exec:java
```

The server prints `shop-rest listening on http://localhost:8080/api/`. Stop it with Ctrl+C.

`process-classes` also generates the OpenAPI document (`target/classes/META-INF/openapi.yaml`), so `GET /api/openapi` works. `mvn -q compile exec:java` starts the same server, but `/api/openapi` then answers 404 because the document is only generated one phase later.

Writes (`POST`, `PUT`, `PATCH`, `DELETE`) need the header `X-Api-Key`. The default key is `demo-123`. To use other keys, pass them at start-up (never commit real keys):

```
mvn -q process-classes exec:java "-Dshop.api.keys=demo-123,ci-777"
```

If port 8080 is busy, change `BASE` in `Main.java`.

A first try (Windows cmd; use `curl` instead of `curl.exe` elsewhere):

```
curl.exe -i http://localhost:8080/api/products
curl.exe -i -X POST http://localhost:8080/api/products -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -d "{\"name\":\"Teapot\",\"sku\":\"TP-01\",\"price\":31.00,\"stock\":5}"
```

## How to test

```
mvn -q verify
```

`ShopApiTest` is a `JerseyTest` suite. It starts the real application (`Main.config()`) on a random free port and has 23 tests. There is one test for every status code the API promises (200, 201, 204, 304, 400, 401, 404, 406, 409, 412, 415, 422, 428). Each test asserts the status, the `Content-Type` (problem+json for errors) and the important header (`Location`, `ETag`, `Link`, `Idempotency-Replayed`). The suite also covers the order flow, idempotent replay, eight concurrent retries of one order, and the OpenAPI document. Every test creates its own data, so the tests do not depend on each other.

Quiet mode (`-q`) prints nothing when all tests pass. Leave `-q` out to see the `Tests run` line.

## Project structure

```
shop-rest/
├── pom.xml
├── README.md
├── .gitignore
├── docs/                       HTTP transcripts, one per task
│   ├── task1-http.md ... task5-http.md
│   └── challenge-http.md
└── src/
    ├── main/java/edu/itc/shop/
    │   ├── Main.java           Grizzly server, ResourceConfig, shared singletons
    │   ├── domain/             Product, Money, Review, Order, OrderService, repositories, exceptions
    │   └── api/
    │       ├── ProductResource, ReviewResource, OrderResource, OpenApiResource
    │       ├── IdempotencyStore, Hashes
    │       ├── dto/            ProductDto, ProductV2, StockChange, ReviewInput, PlaceOrder, OrderDto, Problem
    │       ├── params/         ProductFilter (@BeanParam), MoneyConverterProvider
    │       ├── error/          ValidationMapper, ProblemMapper
    │       └── filter/         CorrelationFilter, CorsFilter, ApiKeyFilter, Compress, GzipWriterInterceptor
    └── test/java/edu/itc/shop/
        └── ShopApiTest.java
```

The resource classes are thin and keep no per-request state. The business rules (duplicate SKU, negative stock, order life cycle) live in the `domain` package.

## API overview

Base URL: `http://localhost:8080/api`

| Method and path | What it does | Main status codes |
|-----------------|--------------|-------------------|
| `GET /products` | List with `q`, `minPrice`, `maxPrice`, `inStock`, `sort`, `page`, `size`; `X-Total-Count` and `Link` headers; gzip if accepted | 200, 400, 406 |
| `GET /products/{id}` | One product (v1 JSON or XML) with `ETag` and `Cache-Control`; v2 with `Accept: application/vnd.shop.v2+json` | 200, 304, 404, 406 |
| `POST /products` | Create; `Location` header | 201, 400, 401, 409, 415 |
| `PUT /products/{id}` | Replace; needs `If-Match` | 200, 400, 401, 404, 412, 428 |
| `PATCH /products/{id}/stock` | Body `{"delta": -5}` | 200, 400, 401, 404, 409 |
| `DELETE /products/{id}` | Delete | 204, 401, 404 |
| `GET /products/{id}/reviews` | Reviews of a product | 200, 404 |
| `POST /products/{id}/reviews` | Add a review (`stars` 1 to 5, `author`, `text`) | 201, 400, 401, 404 |
| `POST /orders` | Place an order; needs `Idempotency-Key` | 201, 400, 401, 404, 409, 422, 428 |
| `GET /orders/{id}` | Read an order, with links | 200, 404 |
| `PUT /orders/{id}/payment` | NEW to PAID | 200, 401, 404, 409 |
| `DELETE /orders/{id}` | NEW to CANCELLED, stock restored | 200, 401, 404, 409 |
| `GET /openapi` | Generated OpenAPI 3.1 document (YAML) | 200 |

How the API behaves:

- **Errors** are always `application/problem+json` (RFC 9457) with `type`, `title`, `status`, `detail` and `instance`. Validation errors add an `errors` list with one `field`/`message` pair per violated constraint. A 500 only shows a reference id; the stack trace stays in the server log.
- **Filters** run in this order for a write: `ApiKeyFilter` (before matching), `CorsFilter`, `CorrelationFilter`. Every response carries `X-Correlation-Id` (echoed or generated) and one access-log line is printed per request. Preflight `OPTIONS` requests answer 200 and never reach a resource.
- **Caching:** the `ETag` of a product is its version. A matching `If-None-Match` answers 304; `PUT` needs a matching `If-Match`.
- **Idempotency:** the first response for an `Idempotency-Key` is stored. The same key and body replay it with `Idempotency-Replayed: true`; the same key with another body gives 422. `computeIfAbsent` makes concurrent retries create one order.
- **HATEOAS:** an order carries `Link` headers and a `_links` map. `pay` and `cancel` are only offered while the order is NEW, so a client can drive the whole flow by following links.

The transcripts in `docs/` show real requests and responses for every task and for the challenge.

## JAX-RS vs Spring MVC

The same two endpoints in both frameworks. The JAX-RS code is from this project. The Spring MVC code is from Lab-02 (`ProductController` in `shop-spring-boot`).

### `GET /products/{id}`

JAX-RS (this lab):

```java
@GET
@Path("/{id: \\d+}")
@Produces({"application/json; qs=1.0", "application/vnd.shop.v1+json", MediaType.APPLICATION_XML})
public Response one(@PathParam("id") long id, @Context Request request) {
    Product p = find(id);                       // throws NotFoundException
    EntityTag tag = tagOf(p);
    Response.ResponseBuilder cond = request.evaluatePreconditions(tag);
    if (cond != null) {
        return cond.tag(tag).build();           // 304
    }
    CacheControl cc = new CacheControl();
    cc.setPrivate(true);
    cc.setMaxAge(60);
    cc.setNoTransform(true);
    return Response.ok(ProductDto.from(p)).tag(tag).cacheControl(cc).build();
}
```

Spring MVC (Lab-02):

```java
@GetMapping("/{id}")
public ResponseEntity<ProductDto> getById(@PathVariable long id) {
    return ResponseEntity.ok(service.findById(id));
}
```

### `POST /products`

JAX-RS (this lab):

```java
@POST
public Response create(@Valid @NotNull ProductDto dto, @Context UriInfo uriInfo) {
    repo.findBySku(dto.sku).ifPresent(p -> { throw new DuplicateSkuException(dto.sku); });
    Product saved = repo.save(dto.toProduct(0, 0));
    URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(saved.id())).build();
    return Response.created(location).entity(ProductDto.from(saved)).build();
}
```

Spring MVC (Lab-02):

```java
@PostMapping
public ResponseEntity<ProductDto> create(@RequestBody @Valid CreateProduct cmd) {
    ProductDto created = service.create(cmd);
    return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
}
```

### Eight differences

| # | Topic | JAX-RS / Jersey (this lab) | Spring MVC (Lab-02) |
|---|-------|----------------------------|---------------------|
| 1 | Annotations | A standard (Jakarta) specification: `@Path`, `@GET`, `@POST`, `@Produces`, `@Consumes`. The class path and method path are combined. The path of an id can carry a regex: `{id: \d+}`. | Spring's own annotations: `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`. Content types go in attributes (`produces`, `consumes`). |
| 2 | Parameter and body binding | `@PathParam`, `@QueryParam`, `@HeaderParam`, `@BeanParam` to group query parameters. A parameter without an annotation is the request body. Custom types use a `ParamConverter`. | `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@ModelAttribute`. The body is marked explicitly with `@RequestBody`. Custom types use a `Converter` or `Formatter`. |
| 3 | Response building | The method returns an object, or a `Response` built with `Response.created(uri).entity(...).tag(...).cacheControl(...)`. `UriInfo` builds absolute URIs. Preconditions are checked with `Request.evaluatePreconditions`. | The method returns an object, or a `ResponseEntity` built with `ResponseEntity.created(uri).body(...)`. In Lab-02 the `Location` URI is written as a string; `ServletUriComponentsBuilder` would build it from the request. |
| 4 | Errors | Throw a `WebApplicationException` (for example `NotFoundException`, `ClientErrorException`). `ExceptionMapper<T>` classes turn exceptions into responses; the most specific mapper wins. problem+json is built by hand with a `Problem` record. | `@RestControllerAdvice` with `@ExceptionHandler` methods. Spring has `ProblemDetail` for RFC 9457 built in, so Lab-02 returns it directly. |
| 5 | Filters and interceptors | `ContainerRequestFilter` and `ContainerResponseFilter` (`@PreMatching` runs before a resource is chosen), `ReaderInterceptor` and `WriterInterceptor` for bodies, `@NameBinding` to apply a provider to selected methods only. Priorities come from `@Priority`. | Servlet `Filter` or `OncePerRequestFilter` (Lab-02 has a request-id filter), `HandlerInterceptor` around controller calls, `@ControllerAdvice` for body advice. There is no name-binding; filters are matched by URL pattern or order. |
| 6 | JSON library | JSON-B (Yasson) and JAXB for XML. A record is serialised without annotations, properties in alphabetical order. A class with no-arg constructor is needed for JAXB. | Jackson. Property order follows the declaration, and annotations such as `@JsonProperty` or `@JsonIgnore` shape the output. XML needs an extra dependency. |
| 7 | Dependency injection | HK2 (Jersey). Singletons are bound by hand in an `AbstractBinder` in `Main`. Resources are created per request, so they must not keep mutable state. Use `@Inject`. | The Spring IoC container. Classes are found by component scan and wired automatically by constructor injection. Controllers are singletons. |
| 8 | Testing | `JerseyTest` starts the real application on a random port in Grizzly and a JAX-RS `Client` calls it. The tests exercise the real HTTP stack, including filters and mappers. | `MockMvc` (`@WebMvcTest`) calls controllers without a real server, or `@SpringBootTest` with `TestRestTemplate` or `WebTestClient` for a running server. Beans can be replaced with `@MockitoBean`. |

In short: JAX-RS is a small specification with an explicit `Response` object, and you assemble the server, the injection and the error handling yourself. Spring MVC does more by convention and wires everything together for you, but it ties the code to Spring.
