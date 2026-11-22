# Challenge: HTTP transcript

Server: `mvn -q process-classes exec:java` (the OpenAPI document is generated at `process-classes`), base URL `http://localhost:8080/api/`. The API key is the default, `demo-123`.
Client: `curl.exe` (Windows cmd). The server was freshly started: product 1 (Kettle) has stock 12 and the first order gets the id `o-1001`.

## 1. Place an order

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "X-Api-Key: demo-123" -H "Idempotency-Key: 7c1e" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":1,\"quantity\":2}]}"
HTTP/1.1 201 Created
Location: http://localhost:8080/api/orders/o-1001
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
Link: <http://localhost:8080/api/orders/o-1001/payment>; method="PUT"; rel="pay"
Link: <http://localhost:8080/api/orders/o-1001>; method="DELETE"; rel="cancel"
Idempotency-Replayed: false
X-Correlation-Id: d767b49d-06e5-4ae5-a694-2e29a1148f3f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 301

{"_links":{"self":"http://localhost:8080/api/orders/o-1001","pay":"http://localhost:8080/api/orders/o-1001/payment","cancel":"http://localhost:8080/api/orders/o-1001","product-1":"http://localhost:8080/api/products/1"},"id":"o-1001","lines":[{"productId":1,"quantity":2}],"status":"NEW","total":49.80}
```

201 Created with a `Location`. The stock of product 1 is reserved. The order carries HATEOAS links twice: as `Link` headers and as a `_links` map. While the order is NEW it offers `pay` and `cancel`, and a `product-1` link for the line. `Idempotency-Replayed: false` says this is the first time the key was seen. The total is 2 x 24.90 = 49.80.

## 2. The same request again (a retry)

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "X-Api-Key: demo-123" -H "Idempotency-Key: 7c1e" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":1,\"quantity\":2}]}"
HTTP/1.1 201 Created
Location: http://localhost:8080/api/orders/o-1001
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
Link: <http://localhost:8080/api/orders/o-1001/payment>; method="PUT"; rel="pay"
Link: <http://localhost:8080/api/orders/o-1001>; method="DELETE"; rel="cancel"
Idempotency-Replayed: true
X-Correlation-Id: 073491bd-12ce-4e49-8ec0-0fb71a1f6815
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 301

{"_links":{"self":"http://localhost:8080/api/orders/o-1001","pay":"http://localhost:8080/api/orders/o-1001/payment","cancel":"http://localhost:8080/api/orders/o-1001","product-1":"http://localhost:8080/api/products/1"},"id":"o-1001","lines":[{"productId":1,"quantity":2}],"status":"NEW","total":49.80}
```

The same key and the same body give the stored response of the first call: the same order `o-1001`, not a second one. `Idempotency-Replayed: true` marks it as a replay. A client that retries after a timeout can never create a duplicate order.

## 3. Same key, different body

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "X-Api-Key: demo-123" -H "Idempotency-Key: 7c1e" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":2,\"quantity\":1}]}"
HTTP/1.1 422 Unprocessable Content
X-Correlation-Id: 10829a80-15a9-4c3f-b971-a3ff7e7a7cc3
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 148

{"detail":"Idempotency-Key reused with a different body","instance":"/api/orders","status":422,"title":"Unprocessable Content","type":"about:blank"}
```

The key is known, but the SHA-256 of this body differs from the stored one. The server refuses, because the client is probably reusing a key by mistake.

## 4. No Idempotency-Key

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "X-Api-Key: demo-123" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":1,\"quantity\":2}]}"
HTTP/1.1 428 Precondition Required
X-Correlation-Id: 91aee702-d7cf-4e36-9303-cf54cfc7ecaf
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 135

{"detail":"Idempotency-Key header required","instance":"/api/orders","status":428,"title":"Precondition Required","type":"about:blank"}
```

Placing an order requires the header. Without it the request is refused.

## 5. Read the order

```
curl.exe -si http://localhost:8080/api/orders/o-1001
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
Link: <http://localhost:8080/api/orders/o-1001/payment>; method="PUT"; rel="pay"
Link: <http://localhost:8080/api/orders/o-1001>; method="DELETE"; rel="cancel"
X-Correlation-Id: 698afa19-5460-4c3f-a3bc-b63d460fd021
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 301

{"_links":{"self":"http://localhost:8080/api/orders/o-1001","pay":"http://localhost:8080/api/orders/o-1001/payment","cancel":"http://localhost:8080/api/orders/o-1001","product-1":"http://localhost:8080/api/products/1"},"id":"o-1001","lines":[{"productId":1,"quantity":2}],"status":"NEW","total":49.80}
```

Reads are public, so no API key is needed. The order is still NEW, so it still offers `pay` and `cancel`.

## 6. Pay the order (following the `pay` link)

```
curl.exe -si -X PUT http://localhost:8080/api/orders/o-1001/payment -H "X-Api-Key: demo-123"
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
X-Correlation-Id: 1789e13f-0442-442e-8cbe-90bb92b32beb
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 195

{"_links":{"self":"http://localhost:8080/api/orders/o-1001","product-1":"http://localhost:8080/api/products/1"},"id":"o-1001","lines":[{"productId":1,"quantity":2}],"status":"PAID","total":49.80}
```

NEW becomes PAID. The links change with the state: `pay` and `cancel` are gone, because they are no longer possible.

## 7. Pay a second time

```
curl.exe -si -X PUT http://localhost:8080/api/orders/o-1001/payment -H "X-Api-Key: demo-123"
HTTP/1.1 409 Conflict
X-Correlation-Id: 4bbb046c-3ccf-4874-9a11-e94f9479e4f0
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 156

{"detail":"Order o-1001 is PAID; only a NEW order can be paid","instance":"/api/orders/o-1001/payment","status":409,"title":"Conflict","type":"about:blank"}
```

Only a NEW order can be paid. The rule lives in `OrderService`, not in the resource.

## 8. Cancel a paid order

```
curl.exe -si -X DELETE http://localhost:8080/api/orders/o-1001 -H "X-Api-Key: demo-123"
HTTP/1.1 409 Conflict
X-Correlation-Id: e1105426-ad5c-4b77-8f45-0d8689bc1f40
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 153

{"detail":"Order o-1001 is PAID; only a NEW order can be cancelled","instance":"/api/orders/o-1001","status":409,"title":"Conflict","type":"about:blank"}
```

A PAID order cannot be cancelled.

## 9. Place a second order (3 units)

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "X-Api-Key: demo-123" -H "Idempotency-Key: 8d2f" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":1,\"quantity\":3}]}"
HTTP/1.1 201 Created
Location: http://localhost:8080/api/orders/o-1002
Link: <http://localhost:8080/api/orders/o-1002>; rel="self"
Link: <http://localhost:8080/api/orders/o-1002/payment>; method="PUT"; rel="pay"
Link: <http://localhost:8080/api/orders/o-1002>; method="DELETE"; rel="cancel"
Idempotency-Replayed: false
X-Correlation-Id: 7c7d5131-5bbd-4de9-bb7c-9e8f47cb4e5d
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 301

{"_links":{"self":"http://localhost:8080/api/orders/o-1002","pay":"http://localhost:8080/api/orders/o-1002/payment","cancel":"http://localhost:8080/api/orders/o-1002","product-1":"http://localhost:8080/api/products/1"},"id":"o-1002","lines":[{"productId":1,"quantity":3}],"status":"NEW","total":74.70}
```

A new key gives a new order, `o-1002`.

## 10. Stock after both orders

```
curl.exe -s http://localhost:8080/api/products/1
{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":7}
```

12 - 2 (`o-1001`) - 3 (`o-1002`) = 7. Orders reserve stock in the same repository that the products API reads.

## 11. Cancel the NEW order (following the `cancel` link)

```
curl.exe -si -X DELETE http://localhost:8080/api/orders/o-1002 -H "X-Api-Key: demo-123"
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/orders/o-1002>; rel="self"
X-Correlation-Id: 7d6e2f17-18d8-4c44-aee5-6c072f15d6f8
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 200

{"_links":{"self":"http://localhost:8080/api/orders/o-1002","product-1":"http://localhost:8080/api/products/1"},"id":"o-1002","lines":[{"productId":1,"quantity":3}],"status":"CANCELLED","total":74.70}
```

NEW becomes CANCELLED. The response returns the cancelled order, so the client sees the new state.

## 12. Stock after the cancellation

```
curl.exe -s http://localhost:8080/api/products/1
{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":10}
```

The 3 reserved units went back: 7 + 3 = 10. Only the paid order `o-1001` still holds 2 units.

## 13. Not enough stock

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "X-Api-Key: demo-123" -H "Idempotency-Key: 9a3b" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":1,\"quantity\":999}]}"
HTTP/1.1 409 Conflict
X-Correlation-Id: 3a278b26-72dd-41fc-bdef-423580efb1e3
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 117

{"detail":"Not enough stock for KT-01","instance":"/api/orders","status":409,"title":"Conflict","type":"about:blank"}
```

## 14. Unknown product

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "X-Api-Key: demo-123" -H "Idempotency-Key: 4c5d" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":999,\"quantity\":1}]}"
HTTP/1.1 404 Not Found
X-Correlation-Id: 6e3a4b97-2c27-4d97-8b25-fa132263c993
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 113

{"detail":"Product 999 not found","instance":"/api/orders","status":404,"title":"Not Found","type":"about:blank"}
```

## 15. Invalid quantity

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "X-Api-Key: demo-123" -H "Idempotency-Key: 6e7f" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":1,\"quantity\":0}]}"
HTTP/1.1 400 Bad Request
X-Correlation-Id: e4d0702b-5193-400c-adaa-b8c66815057c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Connection: close
Content-Length: 194

{"detail":"Validation failed","errors":[{"message":"must be greater than or equal to 1","field":"quantity"}],"instance":"/api/orders","status":400,"title":"Invalid request","type":"about:blank"}
```

`@Min(1)` on the line quantity is checked by Bean Validation before the order code runs.

## 16. No API key

```
curl.exe -si -X POST http://localhost:8080/api/orders -H "Idempotency-Key: 1a2b" -H "Content-Type: application/json" -d "{\"lines\":[{\"productId\":1,\"quantity\":1}]}"
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 359d430e-3747-42c9-8ced-1044ddba8692
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 123

{"detail":"Missing or invalid X-Api-Key","instance":"/api/orders","status":401,"title":"Unauthorized","type":"about:blank"}
```

## 17. Unknown order

```
curl.exe -si http://localhost:8080/api/orders/o-9999
HTTP/1.1 404 Not Found
X-Correlation-Id: ab8dc9b6-6d89-4ab5-bbe6-4f47c694d68b
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 121

{"detail":"Order o-9999 not found","instance":"/api/orders/o-9999","status":404,"title":"Not Found","type":"about:blank"}
```

## 18. The generated OpenAPI document

```
curl.exe -s http://localhost:8080/api/openapi | findstr /i /c:"openapi:" /c:"title" /c:"/orders:" /c:"/products:"
openapi: 3.1.0
  /openapi:
  /orders:
  /products:
  title: shop-rest API
```

`GET /api/openapi` serves the document that the `smallrye-open-api-maven-plugin` generated from the `@Tag`, `@Operation` and `@APIResponse` annotations. Only the matching lines are shown here. It describes OpenAPI 3.1.0, the title `shop-rest API`, and the paths `/orders` and `/products`.
