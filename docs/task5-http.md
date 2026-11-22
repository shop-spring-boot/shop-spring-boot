# Task 5: HTTP transcript

Server: `mvn -q compile exec:java`, base URL `http://localhost:8080/api/`. The API key is the default, `demo-123`.
Client: `curl.exe` (Windows cmd). The server was freshly started: 33 products (3 seeded plus 30 generated for paging), product 1 at version 1. In calls 1 and 2 the output is filtered with `findstr` to show only the interesting headers.

## 1. Page 1 of size 10, with Link headers and total count

```
curl.exe -si "http://localhost:8080/api/products?page=1&size=10" | findstr /i /c:"link" /c:"total" /c:"HTTP/"
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="first"
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="prev"
Link: <http://localhost:8080/api/products?size=10&page=2>; rel="next"
Link: <http://localhost:8080/api/products?size=10&page=3>; rel="last"
X-Total-Count: 33
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
```

33 products with size 10 make 4 pages (0 to 3). Page 1 has all four relations: `prev` is page 0 and `next` is page 2. The links are built from the request URI with `replaceQueryParam("page", n)`, so they keep `size=10`. On page 0 there would be no `prev`, and on page 3 no `next`.

## 2. ETag and Cache-Control on one product

```
curl.exe -si http://localhost:8080/api/products/1 | findstr /i /c:"etag" /c:"cache" /c:"HTTP/"
HTTP/1.1 200 OK
ETag: "1"
Cache-Control: private, no-transform, max-age=60
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
```

The `ETag` is the product `version` (hex) as a quoted string. `Cache-Control` allows the client to reuse the response for 60 seconds.

## 3. Conditional GET: 304

```
curl.exe -si http://localhost:8080/api/products/1 -H "If-None-Match: \"1\""
HTTP/1.1 304 Not Modified
ETag: "1"
X-Correlation-Id: f9e09928-78fd-4a6d-ae78-f2952c010ef6
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
```

`evaluatePreconditions` sees that the client already has version `"1"`, so the server answers 304 with no body.

## 4. PUT without If-Match: 428

```
curl.exe -si -X PUT http://localhost:8080/api/products/1 -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -d "{\"name\":\"Kettle XL\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":12}"
HTTP/1.1 428 Precondition Required
X-Correlation-Id: 2c42f4b7-32d5-42d5-b2e9-7b2a35850eef
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 135

{"detail":"If-Match header is required","instance":"/api/products/1","status":428,"title":"Precondition Required","type":"about:blank"}
```

A PUT must say which version it is replacing. Without `If-Match` the server refuses, so a client cannot overwrite changes it never saw.

## 5. PUT with a stale tag: 412

```
curl.exe -si -X PUT http://localhost:8080/api/products/1 -H "X-Api-Key: demo-123" -H "If-Match: \"0\"" -H "Content-Type: application/json" -d "{\"name\":\"Kettle XL\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":12}"
HTTP/1.1 412 Precondition Failed
X-Correlation-Id: 48816bc0-7284-449f-99f3-17e44fc0a207
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 146

{"detail":"The product changed; reload it and retry","instance":"/api/products/1","status":412,"title":"Precondition Failed","type":"about:blank"}
```

The current tag is `"1"`, not `"0"`, so the precondition fails and nothing is changed.

## 6. PUT with the right tag: 200 and a new ETag

```
curl.exe -si -X PUT http://localhost:8080/api/products/1 -H "X-Api-Key: demo-123" -H "If-Match: \"1\"" -H "Content-Type: application/json" -d "{\"name\":\"Kettle XL\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":12}"
HTTP/1.1 200 OK
ETag: "2"
X-Correlation-Id: 801e13f5-ce47-4f7a-82a4-bfd8e631e70c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 66

{"id":1,"name":"Kettle XL","price":29.90,"sku":"KT-01","stock":12}
```

The tag matches, so the update is applied. The product version went from 1 to 2, and the response carries the new `ETag: "2"`.

## 7. Version 2 of the same product

```
curl.exe -s http://localhost:8080/api/products/1 -H "Accept: application/vnd.shop.v2+json"
{"available":true,"id":1,"name":"Kettle XL","price":{"amount":29.90,"currency":"EUR"},"sku":"KT-01","stock":12}
```

The media type selects the representation. v2 has `price` as an object with a currency, and a new `available` field. Plain `application/json` clients still get v1 (see call 2).

## 8. Create a review

```
curl.exe -si -X POST http://localhost:8080/api/products/1/reviews -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -d "{\"stars\":5,\"author\":\"Ana\",\"text\":\"Boils fast.\"}"
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/1/reviews/1
X-Correlation-Id: 9de884d5-6a81-4387-8005-20e59d9a943b
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 68

{"author":"Ana","id":1,"productId":1,"stars":5,"text":"Boils fast."}
```

201 Created with a `Location` header. The request is handled by `ReviewResource`, which the locator method in `ProductResource` returns for `/products/{id}/reviews`.

## 9. List the reviews of a product

```
curl.exe -s http://localhost:8080/api/products/1/reviews
[{"author":"Ana","id":1,"productId":1,"stars":5,"text":"Boils fast."}]
```

## 10. Reviews of an unknown product

```
curl.exe -si http://localhost:8080/api/products/999/reviews
HTTP/1.1 404 Not Found
X-Correlation-Id: c6883fb0-04e7-497f-a69c-9eaae9fab0fb
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 127

{"detail":"Product 999 not found","instance":"/api/products/999/reviews","status":404,"title":"Not Found","type":"about:blank"}
```

The locator method looks the product up before it returns the `ReviewResource`, so an unknown product fails with 404 before any review method is chosen.
