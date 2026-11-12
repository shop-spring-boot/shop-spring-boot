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
3. 
4. 
5. 
6.

---
## Task 2

### Persist cascade

Only `em.persist(order)` was called. The lines were saved by the cascade:

```
insert into orders (customer_id,placed_at,status,version,id) values (?,?,?,?,?)
insert into order_line (order_id,product_id,quantity,amount,currency,id) values (?,?,?,?,?,?)
insert into order_line (order_id,product_id,quantity,amount,currency,id) values (?,?,?,?,?,?)
```

1 insert for the order + 2 for its lines, and no `update` statements.

### Orphan removal

`order.removeLine(...)` + `flush()` produced one delete:

```
delete from order_line where id=?
```

### Cascade REMOVE

`em.remove(order)` + `flush()` deleted the remaining line first, then the order:

```
delete from order_line where id=?
delete from orders where id=? and version=?
```

### toString pitfall

I temporarily put `lines` in `Order.toString()` and `order` in `OrderLine.toString()`. Printing an order then failed in two ways:

```
java.lang.StackOverflowError
org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role 'edu.itc.shop.domain.Order.lines' with key '2' (no session)
```

`StackOverflowError`: the order prints its lines and each line prints its order, forever.
`LazyInitializationException`: the order was detached, so `lines` could not be loaded without a session.

Fix: `toString()` prints scalar fields only (never `lines`, `order`, `tags`, `products`). I reverted the change.

### Result

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

![Tests passed](screenshots/task2-tests-passed.png)
![toString pitfall](screenshots/task2-tostring-pitfall.png)
