# Lab 4 - Task 2: Full CRUD REST API

## Overview

This task implements a RESTful API for managing products using Jakarta REST (JAX-RS) and Jersey.

The API supports full CRUD operations (Create, Read, Update, and Delete), uses appropriate HTTP status codes and headers, and supports JSON and XML representations.

## Technologies

- Java 25
- Maven 3.9
- Jakarta REST 4.0
- Jersey 4
- Grizzly HTTP Server
- JSON
- XML (JAXB)

## API Base URL

```text
http://localhost:8080/api/products
```

## API Endpoints

| Method | Endpoint | Description | Expected Status |
|---|---|---|---|
| GET | `/api/products` | Get all products | 200 OK |
| GET | `/api/products/{id}` | Get a product by ID | 200 OK |
| POST | `/api/products` | Create a new product | 201 Created |
| PUT | `/api/products/{id}` | Update a product | 200 OK |
| PATCH | `/api/products/{id}/stock` | Change product stock | 200 OK |
| DELETE | `/api/products/{id}` | Delete a product | 204 No Content |

## Supported Representations

The API supports:
- `application/json`
- `application/xml`

Use the `Content-Type` header to specify the request body format and the `Accept` header to request the response format.

## Example Requests

### 1. Get All Products

```bash
curl.exe -i http://localhost:8080/api/products
```

### 2. Get a Product by ID

```bash
curl.exe -i http://localhost:8080/api/products/1
```

### 3. Create a Product (JSON)

```bash
curl.exe -i -X POST http://localhost:8080/api/products ^
-H "Content-Type: application/json" ^
-d "{\"name\":\"Teapot\",\"sku\":\"TP-99\",\"price\":31.00,\"stock\":5}"
```

### 4. Update a Product

```bash
curl.exe -i -X PUT http://localhost:8080/api/products/1 ^
-H "Content-Type: application/json" ^
-d "{\"name\":\"Electric Kettle\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":15}"
```

### 5. Change Product Stock

```bash
curl.exe -i -X PATCH http://localhost:8080/api/products/1/stock ^
-H "Content-Type: application/json" ^
-d "{\"delta\":5}"
```

### 6. Delete a Product

```bash
curl.exe -i -X DELETE http://localhost:8080/api/products/1
```

## Run the Application

Start the application using Maven:

```bash
mvn clean compile
mvn exec:java
```

If the project uses the Maven Wrapper, you can run:

```bash
.\mvnw.cmd clean compile
.\mvnw.cmd exec:java
```

## HTTP Status Codes

| Status Code | Meaning |
|---|---|
| 200 OK | Request completed successfully |
| 201 Created | Product created successfully |
| 204 No Content | Product deleted successfully |
| 400 Bad Request | Invalid request data |
| 404 Not Found | Product does not exist |
| 409 Conflict | Business rule conflict, such as duplicate SKU |
| 415 Unsupported Media Type | Unsupported request content type |

## Task Objectives

- Implement full CRUD operations for products.
- Use appropriate HTTP status codes.
- Return a `Location` header when creating a product.
- Support JSON and XML representations.
- Validate product operations through HTTP requests.
- Keep business logic in the service layer.

## Author

**Name:** LY SENGKIM

**Student ID:** e20231094

**Course:** Lab 4 - RESTful Web Services

**Task:** Task 2 - Full CRUD with Status Codes, Headers, JSON and XML
## Testing Results

All the following API tests were completed successfully:

| Test | Expected Status | Result |
|---|---|---|
| GET all products (JSON) | 200 OK | Passed |
| POST product (JSON) | 201 Created | Passed |
| POST duplicate SKU | 409 Conflict | Passed |
| PUT update product | 200 OK | Passed |
| PATCH change stock | 200 OK | Passed |
| DELETE product | 204 No Content | Passed |
| GET deleted product | 404 Not Found | Passed |
| GET all products (XML) | 200 OK | Passed |
| POST product (XML) | 201 Created | Passed |
| GET product by ID (XML) | 200 OK | Passed |

