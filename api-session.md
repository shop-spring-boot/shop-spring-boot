# API Session

### 1. Create Product (201 Created)
```bash
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"USB-C cable","price":7.90,"stock":25}'

curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"","price":0,"stock":-1}'

curl -i http://localhost:8080/api/products

curl -i -X PATCH http://localhost:8080/api/products/6/stock \
  -H 'Content-Type: application/json' \
  -d '{"delta":-30}'

curl -i http://localhost:8080/api/products/999

Run `./mvnw test` in your project terminal to verify that all the MockMvc assertions pass cleanly.
# API session: Challenge (order management)

Recorded against the app running locally (`mvnw spring-boot:run`, H2 in-memory, default profile).
Seed data: products 1 (Keyboard, 49.99, stock 10), 2 (Mouse, 25.00, stock 5), 3 (Cable, 10.00, stock 20); customers 1 (Alice), 2 (Bob). Tax rate `shop.tax-rate=0.10`.

## 1. Place an order: 201 Created

```
curl.exe -i -X POST localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"customerId\":1,\"lines\":[{\"productId\":1,\"qty\":2},{\"productId\":3,\"qty\":1}]}"
```
```
HTTP/1.1 201
X-Request-Id: 2c5b85d5
Location: /api/orders/1
Content-Type: application/json

{"id":1,"status":"NEW","total":120.98,"lines":[{"productId":1,"qty":2,"unitPrice":49.99},{"productId":3,"qty":1,"unitPrice":10.00}]}
```
Total check: (2 x 49.99 + 10.00) x 1.10 = 120.978, rounded HALF_UP to 120.98.

## 2. Insufficient stock: 409 Conflict

```
curl.exe -i -X POST localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"customerId\":1,\"lines\":[{\"productId\":1,\"qty\":2},{\"productId\":2,\"qty\":999}]}"
```
```
HTTP/1.1 409
X-Request-Id: 9434c260
Content-Type: application/problem+json

{"detail":"Insufficient stock for product 2","instance":"/api/orders","status":409,"title":"Conflict"}
```

## 3. Get one order: 200 OK

```
curl.exe -i localhost:8080/api/orders/1
```
```
HTTP/1.1 200
X-Request-Id: 72eee41e
Content-Type: application/json

{"id":1,"status":"NEW","total":120.98,"lines":[{"productId":1,"qty":2,"unitPrice":49.99},{"productId":3,"qty":1,"unitPrice":10.00}]}
```

## 4. List with filter and paging: 200 OK

```
curl.exe -i "localhost:8080/api/orders?customerId=1&status=NEW&page=0&size=10"
```
```
HTTP/1.1 200
X-Request-Id: d2ac3ad9
Content-Type: application/json

{"content":[{"id":1,"status":"NEW","total":120.98,"lines":[{"productId":1,"qty":2,"unitPrice":49.99},{"productId":3,"qty":1,"unitPrice":10.00}]}],"page":0,"size":10,"totalElements":1}
```

## 5. Cancel an order: 200 OK

```
curl.exe -i -X POST localhost:8080/api/orders/1/cancel
```
```
HTTP/1.1 200
X-Request-Id: 73c1abf8
Content-Type: application/json

{"id":1,"status":"CANCELLED","total":120.98,"lines":[{"productId":1,"qty":2,"unitPrice":49.99},{"productId":3,"qty":1,"unitPrice":10.00}]}
```

## 6. Cancel again: 409 Conflict

```
curl.exe -i -X POST localhost:8080/api/orders/1/cancel
```
```
HTTP/1.1 409
X-Request-Id: b03fa26e
Content-Type: application/problem+json

{"detail":"Order 1 cannot be cancelled (status CANCELLED)","instance":"/api/orders/1/cancel","status":409,"title":"Conflict"}
```

## 7. Unknown order: 404 Not Found

```
curl.exe -i localhost:8080/api/orders/999
```
```
HTTP/1.1 404
X-Request-Id: 2619a6b8
Content-Type: application/problem+json

{"detail":"Order 999 not found","instance":"/api/orders/999","status":404,"title":"Not Found"}
```

## 8. Validation error (empty lines): 400 Bad Request

```
curl.exe -i -X POST localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"customerId\":1,\"lines\":[]}"
```
```
HTTP/1.1 400
X-Request-Id: 33f8174a
Content-Type: application/problem+json

{"detail":"Invalid request content.","instance":"/api/orders","status":400,"title":"Bad Request"}
```

## Audit log excerpt (same request ids as the X-Request-Id headers above)

```
12:38:24.313 INFO  [2c5b85d5] edu.itc.shop.audit.AuditAspect : AUDIT OrderService.place requestId=2c5b85d5 args=[PlaceOrder[customerId=1, lines=[Line[productId=1, qty=2], Line[productId=3, qty=1]]]] result=OrderDto[id=1, status=NEW, total=120.98, ...] (3 ms)
12:38:43.582 WARN  [9434c260] edu.itc.shop.audit.AuditAspect : AUDIT OrderService.place requestId=9434c260 args=[PlaceOrder[customerId=1, lines=[Line[productId=1, qty=2], Line[productId=2, qty=999]]]] failed=edu.itc.shop.order.InsufficientStockException: Insufficient stock for product 2 (0 ms)
12:39:12.420 INFO  [73c1abf8] edu.itc.shop.notify.LogNotifier : NOTIFY: Order 1 cancelled
12:39:12.421 INFO  [73c1abf8] edu.itc.shop.audit.AuditAspect : AUDIT OrderService.cancel requestId=73c1abf8 args=[1] result=OrderDto[id=1, status=CANCELLED, ...] (3 ms)
12:39:20.904 WARN  [b03fa26e] edu.itc.shop.audit.AuditAspect : AUDIT OrderService.cancel requestId=b03fa26e args=[1] failed=edu.itc.shop.order.OrderNotCancellableException: Order 1 cannot be cancelled (status CANCELLED) (1 ms)
```

## TODO (not recorded yet)

- Duplicate product in one order -> expect 400 "Duplicate product 1".
- Rollback proof: after the 409 in step 2, product 1 stock must still be 8, so ordering product 1 with qty 8 returns 201 and then qty 1 returns 409.



---

# Lab 3: Spring Data JPA endpoints

Recorded with the `dev` profile (`.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"`). `DataSeeder` loaded 3 customers, 12 products and 30 orders.

## 9. GET /api/orders (paged and sorted)

````
curl.exe -s "localhost:8080/api/orders?status=PAID&page=0&size=2&sort=placedAt,desc"
````
````
{"content":[{"customer":{"name":"Ada Lovelace"},"id":1,"placedAt":"2026-11-01T09:25:17.558046Z","status":"PAID"},{"customer":{"name":"Alan Turing"},"id":30,"placedAt":"2026-10-28T09:25:17.592047Z","status":"PAID"}],"page":{"size":2,"number":0,"totalElements":9,"totalPages":5}}
````
2 orders, all `PAID`, newest first. 9 `PAID` orders in total, 5 pages of size 2.

## 10. GET /api/products (Specification filters)

````
curl.exe -s "localhost:8080/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true"
````
````
{"content":[{"id":1,"sku":"KB-1","name":"Mechanical Keyboard","amount":49.90,"stock":15},{"id":2,"sku":"KB-7","name":"Compact Keyboard","amount":39.00,"stock":8}],"page":{"size":20,"number":0,"totalElements":2,"totalPages":1}}
````
Name contains `key`, category `HARDWARE`, price at most 60, in stock. Default page size is 20.

## 11. GET /api/customers/{email}/orders (full orders with lines)

````
curl.exe -s "localhost:8080/api/customers/ada@example.com/orders"
````
````
[{"id":16,"status":"SHIPPED","placedAt":"2026-11-13T09:25:17.582046Z","total":147.80,"lines":[{"sku":"KB-7","productName":"Compact Keyboard","quantity":1,"unitPrice":39.00},{"sku":"HB-1","productName":"USB-C Hub","quantity":1,"unitPrice":34.90},{"sku":"BK-2","productName":"SQL Cookbook","quantity":2,"unitPrice":32.00},{"sku":"FD-1","productName":"Coffee Beans","quantity":1,"unitPrice":9.90}]},{"id":13,"status":"NEW","placedAt":"2026-11-05T09:25:17.581048Z","total":57.10,"lines":[{"sku":"TY-2","productName":"Puzzle 1000","quantity":3,"unitPrice":14.50},{"sku":"FD-3","productName":"Green Tea","quantity":2,"unitPrice":6.80}]},{"id":1,"status":"PAID","placedAt":"2026-11-01T09:25:17.558046Z","total":122.00,"lines":[{"sku":"BK-2","productName":"SQL Cookbook","quantity":2,"unitPrice":32.00},{"sku":"BK-1","productName":"Java Handbook","quantity":2,"unitPrice":29.00}]}]
````
Shortened here to the first 3 of 10 orders. The SQL log shows one `select distinct ... join order_line ... join product` query, so there is no N+1.
# Lab 3: Spring Data JPA endpoints

Recorded with the dev profile (DataSeeder: 3 customers, 12 products, 30 orders).

## GET localhost:8080/api/orders?status=PAID&page=0&size=2&sort=placedAt,desc
```json
{"content":[{"customer":{"name":"Ada Lovelace"},"id":1,"placedAt":"2026-11-01T09:43:08.864662Z","status":"PAID"},{"customer":{"name":"Alan Turing"},"id":30,"placedAt":"2026-10-28T09:43:08.935415Z","status":"PAID"}],"page":{"size":2,"number":0,"totalElements":9,"totalPages":5}}
```

## GET localhost:8080/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true
```json
{"content":[{"id":1,"sku":"KB-1","name":"Mechanical Keyboard","amount":49.90,"stock":15},{"id":2,"sku":"KB-7","name":"Compact Keyboard","amount":39.00,"stock":8}],"page":{"size":20,"number":0,"totalElements":2,"totalPages":1}}
```

## GET localhost:8080/api/customers/ada@example.com/orders
```json
[{"id":16,"status":"SHIPPED","placedAt":"2026-11-13T09:43:08.914412Z","total":147.80,"lines":[{"sku":"KB-7","productName":"Compact Keyboard","quantity":1,"unitPrice":39.00},{"sku":"HB-1","productName":"USB-C Hub","quantity":1,"unitPrice":34.90},{"sku":"BK-2","productName":"SQL Cookbook","quantity":2,"unitPrice":32.00},{"sku":"FD-1","productName":"Coffee Beans","quantity":1,"unitPrice":9.90}]},{"id":13,"status":"NEW","placedAt":"2026-11-05T09:43:08.906749Z","total":57.10,"lines":[{"sku":"TY-2","productName":"Puzzle 1000","quantity":3,"unitPrice":14.50},{"sku":"FD-3","productName":"Green Tea","quantity":2,"unitPrice":6.80}]},{"id":1,"status":"PAID","placedAt":"2026-11-01T09:43:08.864662Z","total":122.00,"lines":[{"sku":"BK-2","productName":"SQL Cookbook","quantity":2,"unitPrice":32.00},{"sku":"BK-1","productName":"Java Handbook","quantity":2,"unitPrice":29.00}]},{"id":19,"status":"NEW","placedAt":"2026-10-30T09:43:08.920342Z","total":202.20,"lines":[{"sku":"HB-1","productName":"USB-C Hub","quantity":1,"unitPrice":34.90},{"sku":"FD-3","productName":"Green Tea","quantity":1,"unitPrice":6.80},{"sku":"TY-2","productName":"Puzzle 1000","quantity":3,"unitPrice":14.50},{"sku":"KB-7","productName":"Compact Keyboard","quantity":3,"unitPrice":39.00}]},{"id":7,"status":"CANCELLED","placedAt":"2026-10-13T09:43:08.894743Z","total":23.30,"lines":[{"sku":"FD-1","productName":"Coffee Beans","quantity":2,"unitPrice":9.90},{"sku":"FD-2","productName":"Dark Chocolate","quantity":1,"unitPrice":3.50}]},{"id":4,"status":"CANCELLED","placedAt":"2026-10-12T09:43:08.891745Z","total":159.80,"lines":[{"sku":"MS-1","productName":"Gaming Mouse","quantity":3,"unitPrice":25.50},{"sku":"TY-2","productName":"Puzzle 1000","quantity":3,"unitPrice":14.50},{"sku":"TY-1","productName":"Wooden Train","quantity":2,"unitPrice":19.90}]},{"id":25,"status":"SHIPPED","placedAt":"2026-09-27T09:43:08.930407Z","total":168.70,"lines":[{"sku":"HB-1","productName":"USB-C Hub","quantity":3,"unitPrice":34.90},{"sku":"BK-2","productName":"SQL Cookbook","quantity":2,"unitPrice":32.00}]},{"id":10,"status":"SHIPPED","placedAt":"2026-09-24T09:43:08.902758Z","total":38.80,"lines":[{"sku":"FD-3","productName":"Green Tea","quantity":1,"unitPrice":6.80},{"sku":"BK-2","productName":"SQL Cookbook","quantity":1,"unitPrice":32.00}]},{"id":22,"status":"PAID","placedAt":"2026-09-18T09:43:08.926409Z","total":193.70,"lines":[{"sku":"KB-1","productName":"Mechanical Keyboard","quantity":3,"unitPrice":49.90},{"sku":"BK-3","productName":"Hibernate in Depth","quantity":1,"unitPrice":44.00}]},{"id":28,"status":"PAID","placedAt":"2026-09-17T09:43:08.933407Z","total":61.50,"lines":[{"sku":"FD-2","productName":"Dark Chocolate","quantity":1,"unitPrice":3.50},{"sku":"BK-1","productName":"Java Handbook","quantity":2,"unitPrice":29.00}]}]
```
