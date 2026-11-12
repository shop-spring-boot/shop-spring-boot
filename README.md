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
```

## Contents

1. 
2. 
3. 
4. 
5. 
6.

---

## Task 2: Associations, cascading and entity identity

### What was added

- `Order.lines`: `@OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true)` with the helpers `addLine`, `removeLine` and `total()`.
- `Product` ↔ `Tag` many-to-many through `product_tag` (`Product` is the owning side, `Tag` is the inverse side), with `Product.tag(Tag)` updating both sides.
- Hand-written `equals`, `hashCode` and `toString` on `Customer`, `Product`, `Tag`, `Order` and `OrderLine`.
- `OrderCascadeTest` (2 tests) and `EntityIdentityTest` (3 tests).

### How to run the tests

```bash
mvnw test -Dtest=OrderCascadeTest,EntityIdentityTest
```

Expected: `Tests run: 5, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

To see only the SQL lines and the summary:

```bash
mvnw test -Dtest=OrderCascadeTest,EntityIdentityTest | findstr /C:"Hibernate: insert into orders" /C:"Hibernate: insert into order_line" /C:"Hibernate: delete from" /C:"Hibernate: update" /C:"Tests run" /C:"BUILD"
```

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

### Entity identity

- `equals` is true when both objects have the same class and the same non-null id.
- `hashCode` returns a constant per class, so a new `Product` put in a `HashSet` is still found after `persist` gives it an