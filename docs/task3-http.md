# Task 3: HTTP transcript

Server: `mvn -q compile exec:java`, base URL `http://localhost:8080/api/`.
Client: `curl.exe -i` (Windows cmd). The server was freshly started, so it held only the three seeded products (ids 1 to 3).

## 1. Filtered and sorted list

```
curl.exe -s "http://localhost:8080/api/products?minPrice=5%20EUR&inStock=true&sort=price,desc"
[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40}]
```

`minPrice=5 EUR` is converted to a `Money` by the `ParamConverter`, so Tea (4.20 EUR) is out. `inStock=true` keeps only products with stock above 0. `sort=price,desc` puts the most expensive first.

## 2. Money that cannot be parsed

```
curl.exe -i "http://localhost:8080/api/products?minPrice=abc"
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
Connection: close
Content-Length: 125

{"detail":"Cannot parse Money from 'abc'","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

The converter throws `BadRequestException`, and `ProblemMapper` turns it into a problem+json body.

## 3. Unknown sort field

```
curl.exe -i "http://localhost:8080/api/products?sort=colour,asc"
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
Connection: close
Content-Length: 121

{"detail":"Unknown sort field colour","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

`colour,asc` has the right format, so the `@Pattern` on `sort` passes. The field name is not on the whitelist (`name`, `price`, `stock`), so the filter rejects it with 400.

## 4. Invalid body with three or more field errors

```
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{\"name\":\"\",\"sku\":\"kettle\",\"price\":0,\"stock\":-1}"
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
Connection: close
Content-Length: 367

{"detail":"Validation failed","errors":[{"field":"sku","message":"must match \"[A-Z]{2}-\\d{2}\""},{"field":"price","message":"must be greater than or equal to 0.01"},{"field":"name","message":"must not be blank"},{"field":"stock","message":"must be greater than or equal to 0"}],"instance":"/api/products","status":400,"title":"Invalid request","type":"about:blank"}
```

Bean Validation finds four violated constraints on `ProductDto`. `ValidationMapper` reports one entry per violation in the `errors` list. The order of the entries is not guaranteed.

## 5. Unknown id as problem+json

```
curl.exe -i http://localhost:8080/api/products/99
HTTP/1.1 404 Not Found
Content-Type: application/problem+json
Content-Length: 117

{"detail":"Product 99 not found","instance":"/api/products/99","status":404,"title":"Not Found","type":"about:blank"}
```

The resource code did not change. The `NotFoundException` from Task 1 is now answered by `ProblemMapper`, so the plain 404 became a problem body.

## 6. The 500 experiment

For this call only, `get(...)` in `ProductResource` temporarily contained `throw new IllegalStateException("boom");`. The line was removed afterwards.

```
curl.exe -i http://localhost:8080/api/products/1
HTTP/1.1 500 Internal Server Error
Content-Type: application/problem+json
Connection: close
Content-Length: 154

{"detail":"reference f4a67037-8085-4129-bdfd-9ce117f2a145","instance":"/api/products/1","status":500,"title":"Internal Server Error","type":"about:blank"}
```

The client only sees a reference id. There is no stack trace and no exception message in the body. The full stack trace is logged on the server under the same reference.

## 7. Extra: malformed JSON

```
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{bad"
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
Connection: close
Content-Length: 118

{"detail":"Malformed request body","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

A body that cannot be parsed is the client's mistake. Without a special case it came out as a 500, so `ProblemMapper` now answers it with 400.
