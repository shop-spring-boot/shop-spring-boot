# Task 1: HTTP transcript

Server: `mvn -q compile exec:java`, base URL `http://localhost:8080/api/`.
Client: `curl.exe -i` (Windows).

## 1. List all products

```
curl.exe -i http://localhost:8080/api/products
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 220

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12,"version":1},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40,"version":1},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0,"version":1}]
```

200 OK with the three seeded products as a JSON array.

## 2. Get one product

```
curl.exe -i http://localhost:8080/api/products/1
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 75

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12,"version":1}
```

200 OK with the single product as a JSON object.

## 3. Unknown id

```
curl.exe -i http://localhost:8080/api/products/99
HTTP/1.1 404 Not Found
Content-Length: 0
```

The id matches the path template, but the repository has no product 99, so the resource throws `NotFoundException` and Jersey answers 404.

## 4. Id that is not a number

```
curl.exe -i http://localhost:8080/api/products/abc
HTTP/1.1 404 Not Found
Content-Length: 0
```

`abc` does not match the template `/{id: \d+}`, so no resource method matches the URL and Jersey answers 404.
