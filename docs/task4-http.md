# Task 4: HTTP transcript

Server: `mvn -q compile exec:java`, base URL `http://localhost:8080/api/`. The API key list is the default, `demo-123` (`-Dshop.api.keys`).
Client: `curl.exe` (Windows cmd). The server was freshly started, so it held only the three seeded products (ids 1 to 3).

## 1. POST without an API key

```
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{\"name\":\"Teapot\",\"sku\":\"TP-01\",\"price\":31.00,\"stock\":5}"
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 7ad3173f-3c33-4b7e-9410-a3132e4e5737
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 125

{"detail":"Missing or invalid X-Api-Key","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

`ApiKeyFilter` runs before resource matching and rejects the write with `abortWith`. Exception mappers are skipped for an aborted request, so the filter builds the problem+json body itself. The response filters still run, so the 401 carries the CORS headers and an `X-Correlation-Id`. Because the request was aborted before `CorrelationFilter` saw it, the response side generated a new id.

## 2. POST with a wrong API key

```
curl.exe -i -X POST http://localhost:8080/api/products -H "X-Api-Key: wrong" -H "Content-Type: application/json" -d "{\"name\":\"Teapot\",\"sku\":\"TP-01\",\"price\":31.00,\"stock\":5}"
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 6e79a62a-f450-40d5-b498-41610ba5edad
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 125

{"detail":"Missing or invalid X-Api-Key","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

A key that is not in `shop.api.keys` is rejected the same way as a missing one.

## 3. POST with the right API key

```
curl.exe -i -X POST http://localhost:8080/api/products -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -d "{\"name\":\"Teapot\",\"sku\":\"TP-01\",\"price\":31.00,\"stock\":5}"
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/4
X-Correlation-Id: 064c04eb-65df-4573-94ef-0d6776d35f9f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 62

{"id":4,"name":"Teapot","price":31.00,"sku":"TP-01","stock":5}
```

With a valid key the request passes the filter and reaches the resource: 201 Created, as in Task 2.

## 4. CORS preflight

```
curl.exe -i -X OPTIONS http://localhost:8080/api/products -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: POST"
HTTP/1.1 200 OK
X-Correlation-Id: 966140aa-3e72-45dc-a52b-e279358a1ad2
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Length: 0
```

The pre-matching part of `CorsFilter` answers `OPTIONS` with 200, so the preflight never reaches a resource. `OPTIONS` is also on the safe list of `ApiKeyFilter`, so no key is needed.

## 5. Correlation id echoed back

```
curl.exe -i http://localhost:8080/api/products/1 -H "X-Correlation-Id: order-check-42"
HTTP/1.1 200 OK
X-Correlation-Id: order-check-42
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

The id sent by the client is stored with `setProperty` in the request filter and returned in the response. Without a header (calls 1 to 4 and 7) the filter generates a UUID.

## 6. gzip when the client accepts it

```
curl.exe -sv --compressed http://localhost:8080/api/products -o NUL 2>&1 | findstr /i "encoding vary"
> Accept-Encoding: deflate, gzip
< Content-Encoding: gzip
< Vary: Accept-Encoding
```

`--compressed` makes curl send `Accept-Encoding: deflate, gzip`. `GzipWriterInterceptor` is name-bound with `@Compress` and sees `gzip`, so it wraps the output stream and sets `Content-Encoding: gzip` and `Vary: Accept-Encoding`.

## 7. No compression without Accept-Encoding

```
curl.exe -i http://localhost:8080/api/products
HTTP/1.1 200 OK
X-Correlation-Id: 14f42ade-5899-401d-9c92-1326d0d7d7a9
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 247

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0},{"id":4,"name":"Teapot","price":31.00,"sku":"TP-01","stock":5}]
```

The same GET without `Accept-Encoding` is sent as plain JSON, with no `Content-Encoding` header. The list now has four products, including the Teapot created in call 3.
