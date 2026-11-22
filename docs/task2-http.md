# Task 2: HTTP transcript

Server: `mvn -q compile exec:java`, base URL `http://localhost:8080/api/`.
Client: `curl.exe -i` (Windows cmd). The server was freshly started, so it held only the three seeded products (ids 1 to 3).

## 1. Create a product (JSON)

```
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{\"name\":\"Teapot\",\"sku\":\"TP-01\",\"price\":31.00,\"stock\":5}"
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/4
Content-Type: application/json
Content-Length: 62

{"id":4,"name":"Teapot","price":31.00,"sku":"TP-01","stock":5}
```

201 Created. The `Location` header is built from `UriInfo` and points to the new product. The body is a `ProductDto`, so there is no `version` field.

## 2. Duplicate SKU

```
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{\"name\":\"Teapot\",\"sku\":\"TP-01\",\"price\":31.00,\"stock\":5}"
HTTP/1.1 409 Conflict
Content-Length: 0
```

`findBySku` finds `TP-01`, so `DuplicateSkuException` (status 409) is thrown. The body is empty for now; Task 3 turns it into problem+json.

## 3. Read a product as XML

```
curl.exe -i http://localhost:8080/api/products/4 -H "Accept: application/xml"
HTTP/1.1 200 OK
Content-Type: application/xml
Content-Length: 152

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="4"><name>Teapot</name><sku>TP-01</sku><price>31.00</price><stock>5</stock></product>
```

Content negotiation: the same resource method and the same `ProductDto` answer in XML because of the `Accept` header. `id` is an XML attribute.

## 4. XML round trip, part 1: POST XML

```
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/xml" -H "Accept: application/xml" -d "<product><name>Bowl</name><sku>BW-01</sku><price>9.90</price><stock>3</stock></product>"
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/5
Content-Type: application/xml
Content-Length: 149

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="5"><name>Bowl</name><sku>BW-01</sku><price>9.90</price><stock>3</stock></product>
```

XML goes in (JAXB reads the request) and XML comes out.

## 5. XML round trip, part 2: GET XML

```
curl.exe -i http://localhost:8080/api/products/5 -H "Accept: application/xml"
HTTP/1.1 200 OK
Content-Type: application/xml
Content-Length: 149

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="5"><name>Bowl</name><sku>BW-01</sku><price>9.90</price><stock>3</stock></product>
```

The product created from XML can be read back as XML.

## 6. Replace a product (PUT)

```
curl.exe -i -X PUT http://localhost:8080/api/products/4 -H "Content-Type: application/json" -d "{\"name\":\"Big Teapot\",\"sku\":\"TP-01\",\"price\":35.00,\"stock\":8}"
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 66

{"id":4,"name":"Big Teapot","price":35.00,"sku":"TP-01","stock":8}
```

200 OK with the replaced representation.

Repeating the identical request:

```
curl.exe -i -X PUT http://localhost:8080/api/products/4 -H "Content-Type: application/json" -d "{\"name\":\"Big Teapot\",\"sku\":\"TP-01\",\"price\":35.00,\"stock\":8}"
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 66

{"id":4,"name":"Big Teapot","price":35.00,"sku":"TP-01","stock":8}
```

The same result both times: PUT is idempotent.

## 7. Change stock (PATCH)

```
curl.exe -i -X PATCH http://localhost:8080/api/products/1/stock -H "Content-Type: application/json" -d "{\"delta\":-5}"
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 62

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":7}
```

Stock went from 12 to 7.

Repeating the identical request:

```
curl.exe -i -X PATCH http://localhost:8080/api/products/1/stock -H "Content-Type: application/json" -d "{\"delta\":-5}"
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 62

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":2}
```

Stock went from 7 to 2. The same request gave a different result, so this PATCH (a relative change) is not idempotent, unlike PUT.

## 8. Stock would go negative

```
curl.exe -i -X PATCH http://localhost:8080/api/products/3/stock -H "Content-Type: application/json" -d "{\"delta\":-1}"
HTTP/1.1 409 Conflict
Content-Length: 0
```

Tea has stock 0, so 0 + (-1) < 0 and `InsufficientStockException` (409) is thrown. The rule lives in the repository, not in the resource class.

## 9. Delete a product

```
curl.exe -i -X DELETE http://localhost:8080/api/products/4
HTTP/1.1 204 No Content
```

204 No Content on success.

```
curl.exe -i -X DELETE http://localhost:8080/api/products/4
HTTP/1.1 404 Not Found
Content-Length: 0
```

The product is already gone, so the second delete gives 404.

## 10. Wrong media type

```
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: text/plain" -d "Teapot"
HTTP/1.1 415 Unsupported Media Type
Content-Length: 0
```

The resource only consumes JSON and XML, so Jersey rejects `text/plain` with 415 before any of our code runs.
