# Lab 3: Hibernate framework and Spring Data JPA

Spring Boot 4.1.1 · Java 25 · Maven · JdbcClient · H2 (default) / PostgreSQL (`prod`)

## Project structure

The code is organised **by feature**: each business area has its own package, plus a few cross-cutting packages.

```
shop-spring-boot
├── pom.xml
├── mvnw, mvnw.cmd
├── docker-compose.yml          PostgreSQL for the prod profile
├── .env.example                template for DB_PASSWORD
├── api-session.md              recorded API session
├── screenshots/
└── src
    ├── main
    │   ├── java/edu/itc/shop
    │   │   ├── ShopApplication.java
    │   │   ├── product/        products: entity, service, repositories, REST + JSP controllers, DTOs
    │   │   ├── order/          orders: entity, service, repository, REST controller, DTOs
    │   │   ├── notification/   Notifier and its Email / SMS / Console implementations
    │   │   ├── audit/          @Audited, AuditAspect, TimingAspect, RequestContext
    │   │   ├── config/         properties, health indicator, start-up runners
    │   │   └── common/         exception handler, request-id filter
    │   ├── resources           application*.yml, schema.sql, data*.sql, static/
    │   └── webapp/WEB-INF/jsp  JSP views
    └── test/java/edu/itc/shop  product/, order/, notification/ (mirrors main)
```

## Quick start

```powershell
.\mvnw.cmd spring-boot:run                                      # default profile: in-memory H2
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"     # dev: H2 console + DEBUG logging
.\mvnw.cmd clean verify                                         # run all tests (JDK 25 only)

## Contents

1. 
2. 
## Task 3
##Step 1: Typo in a derived query

**What I changed:** in `OrderRepository`, I renamed
`findByCustomerEmailAndStatusOrderByPlacedAtDesc` to
`findByCustomerEmaliAndStatusOrderByPlacedAtDesc` (typo: `Emali`) and started the app.

**Boot error:**

​```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'orderRepository' defined in edu.itc.shop.order.OrderRepository defined in @EnableJpaRepositories declared on DataJpaRepositoriesRegistrar.EnableJpaRepositoriesConfiguration: No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer

Caused by: org.springframework.data.repository.query.QueryCreationException: Cannot create query for method [OrderRepository.findByCustomerEmaliAndStatusOrderByPlacedAtDesc(java.lang.String,edu.itc.shop.domain.OrderStatus)]; No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer

Caused by: org.springframework.data.core.PropertyReferenceException: No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer
​```

**Explanation:** Spring Data parses every repository method name when the application starts. It splits `findByCustomerEmaliAndStatus...` into properties (`customer.emali`, `status`) and checks them against the entities. `Customer` has no `emali` property, so the `orderRepository` bean fails and the whole application stops before it starts.

**Reverted:** yes, the method is back to `findByCustomerEmail...`.

## Step 2: JPQL and native queries

- `findWithLinesByCustomerEmail`: JPQL with `join fetch` loads an order, its lines and each line's product in one query. This avoids the N+1 problem (one extra query per order).
- `countLinesByStatus`: JPQL query that counts order lines by the order's status.
- Both queries are checked when the app starts, and the app starts without errors.

## Step 3: Projections

- `OrderSummary` is an interface projection, so Hibernate selects only the listed columns.
- `ProductView` is a record projection, built with a constructor expression in `@Query`.
- Derived `Page<ProductView> findByCategory` would fail at boot, because Spring Data can't map `price.amount` to `amount`. That is why I used `@Query`.
- SQL log check: *(write what you saw here after you call `/api/orders?status=PAID`)*

##Step 4: Specifications

- `ProductRepository` also extends `JpaSpecificationExecutor<Product>`.
- `ProductSpecs` has `nameContains`, `inCategory`, `cheaperThan` and `inStock`. Each returns a small `Specification` that builds one `WHERE` condition.
- `ProductSpecs.filter(q, category, maxPrice, inStock)` combines the filters with `Specification.allOf`. Each filter returns `null` when its parameter is absent, and `allOf` skips `null`, so with no filter there is no `WHERE` clause.
- `root.get("price").get("amount")` walks into the embedded `Money`.
- Specifications only run when they are called, so they are tested through the `/api/products` endpoint (Step 6) and in the tests (Step 7).

## Step 5: DataSeeder

- `DataSeeder` is a `@Component @Profile("dev")` `ApplicationRunner`.
- It inserts 3 customers, 12 products with tags and 30 orders (2 to 4 lines each, mixed statuses) through the repositories, inside one `@Transactional` method.
- `Order.addLine(product, quantity)` creates the `OrderLine` with the product's price, and `cascade = ALL` saves the lines with the order.
- Run it with the `dev` profile: `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"`. The SQL log shows the inserts, and the app starts normally afterwards.
4. 
5. 
6.

---
