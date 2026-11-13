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

1. [Task 1: Entities and mappings, validated by Flyway](#task-1-entities-and-mappings-validated-by-flyway)
2. [Task 2: Associations, cascading and entity identity](#task-2-associations-cascading-and-entity-identity)

## Task 1: Entities and mappings, validated by Flyway

### What I built

- **Setup:** added `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `flyway-database-postgresql` (runtime) and `spring-boot-starter-data-jpa-test` (test) to `pom.xml`. In `application.yml` Hibernate only validates (`ddl-auto: validate`), `open-in-view` is off, `generate_statistics` is on, and the SQL log is on (`org.hibernate.SQL: DEBUG`, `org.hibernate.orm.jdbc.bind: TRACE`).
- **Migration:** `src/main/resources/db/migration/V1__init.sql` creates five sequences (`INCREMENT BY 50`) and the tables `customer`, `product`, `tag`, `product_tag`, `orders` and `order_line`, with primary keys, foreign keys, `NOT NULL`, `UNIQUE` and `CHECK` constraints (`stock >= 0`, `quantity > 0`, category and status values). The old `schema.sql`, `data*.sql` and `reset.sql` were removed, so Flyway now owns the schema.
- **Value objects** (package `domain`): `Money(amount, currency)` and `Address(street, city, zip)` are `@Embeddable` records.
- **Enums:** `Category { BOOKS, HARDWARE, TOYS, FOOD }` and `OrderStatus { NEW, PAID, SHIPPED, CANCELLED }`, both stored with `@Enumerated(EnumType.STRING)`.
- **Entities:** `Customer` (unique email, `billing` and `shipping` addresses, the shipping columns renamed to `ship_*` with `@AttributeOverride`), `Product` (unique `sku`, `Money price`, `Category`, `stock`), `Tag` (unique name), `Order` (`@Table(name = "orders")`, lazy `@ManyToOne customer`, `status`, `Instant placedAt`, `@Version version`) and `OrderLine` (lazy `@ManyToOne` to `Order` and `Product`, `quantity`, `Money unitPrice`).
- **Ids:** every id is a `Long` with `GenerationType.SEQUENCE`, a named generator and `allocationSize = 50`, which matches `INCREMENT BY 50` in `V1__init.sql`. Each entity has a protected no-argument constructor and no `setId`.

### Start-up check

Flyway runs first and Hibernate validates the result:

```
org.flywaydb.core.FlywayExecutor : Database: jdbc:h2:mem:shopdb (H2 2.4)
o.f.core.internal.command.DbValidate : Successfully validated 1 migration
o.f.core.internal.command.DbMigrate : Migrating schema "PUBLIC" to version "1 - init"
o.f.core.internal.command.DbMigrate : Successfully applied 1 migration to schema "PUBLIC", now at version v1
org.hibernate.orm.core : HHH000001: Hibernate ORM core version 7.4.5.Final
o.s.o.j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
```

Hibernate started without a `SchemaManagementException`.

### Error 1: `CHAR(3)` against `VARCHAR`

My first start failed on the `currency` column, which `V1__init.sql` declares as `CHAR(3)`:

```
SchemaManagementException: Schema validation: wrong column type encountered in column [currency]
in table [order_line]; found [character (Types#CHAR)], but expecting [varchar(3) (Types#VARCHAR)]
```

Hibernate maps a Java `String` to `VARCHAR` by default, and its validator treats `CHAR` and `VARCHAR` as different types. **Fix:** I added `@JdbcTypeCode(SqlTypes.CHAR)` on `Money.currency`, which keeps `CHAR(3)` in the migration.

### Error 2: wrong column name (on purpose)

**What I changed:** in `V1__init.sql` only, I renamed `sku` to `code` in the `product` table and started the app.

```
PASTE THE EXACT SchemaManagementException LINE HERE (it should say a column [sku] is missing in table [product])
```

**Explanation:** Hibernate compares every mapped column with the real table at start-up. `Product.sku` has no matching column, so validation fails and the application does not start. **Reverted:** yes, the column is `sku` again.

### Error 3: wrong sequence step (on purpose)

**What I changed:** in `V1__init.sql` only, I declared `product_seq` with `INCREMENT BY 1`.

```
PASTE THE EXACT ERROR LINE HERE (it should mention the sequence increment size and the allocationSize of 50)
```

**Explanation:** `@SequenceGenerator(allocationSize = 50)` makes Hibernate reserve 50 ids per call to the sequence. That only works if the database sequence also steps by 50, so Hibernate refuses to start when the two disagree. **Reverted:** yes, it is `INCREMENT BY 50` again.

### ProductMappingTest

`ProductMappingTest` (`@DataJpaTest` with `@AutoConfigureTestDatabase(replace = NONE)`, so Flyway and `validate` run in the test too) persists a `Product` with `TestEntityManager`, calls `flush()` and `clear()`, then reloads it by id and asserts that `Money` and `Category` survive the round trip. A native query then reads the raw `category` column and asserts it contains the text `HARDWARE`, not a number.

```
Tests run: 1, Failures: 0, Errors: 0   (paste your real line)
```

Run it with:

```powershell
.\mvnw.cmd test -Dtest=ProductMappingTest
```

### Checklist

- [x] Entities mapped, `V1__init.sql` written, Flyway applies it
- [x] Hibernate starts without `SchemaManagementException`
- [x] `Money` and `Address` are embeddables; enums use `EnumType.STRING`
- [ ] Error 2 and Error 3 pasted above
- [ ] `ProductMappingTest` result pasted above
- [ ] Tag `task-1`

---

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
- SQL log check: _(write what you saw here after you call `/api/orders?status=PAID`)_

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

## Step 7: OrderRepositoryTest

`OrderRepositoryTest` (`@DataJpaTest`, Flyway schema) has 5 tests: derived query, fetch join, plain `findById`, native query and projection.

- Fetch join: one `select distinct ... from orders join order_line join product join customer where c1_0.email=?` loads orders, lines and products together. `PersistenceUnitUtil.isLoaded(order, "lines")` is true.
- Plain `findById`: `select ... from orders where id=?` only, so `lines` is not loaded (`isLoaded` is false). I used `isLoaded` and not `Hibernate.isInitialized(order.getLines())`, because `getLines()` returns `Collections.unmodifiableList(...)`, which always reports initialized.
- Native query: `SELECT p.* FROM product p WHERE p.stock < ? ORDER BY p.stock`.
- Result: `Tests run: 5, Failures: 0, Errors: 0` (paste your real line).
- The old JDBC tests (`ProductServiceTest`, `RepositoryTest`, etc.) described the old schema (`price`, `orders.total`) and failed against Flyway, so I moved them to `legacy-jdbc/tests`.

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

## Task 4: states, flush order, optimistic locking

- **Managed vs detached**: `markPaid` has no `save()` and runs exactly one `update orders`.
  A detached `Product` with `setStock(99)` stays unchanged until `products.save(p)`, which is a merge:
  1 select + 1 update. (paste the SQL log lines)
- **Read-only trap**: `wronglyModifyInReadOnly` changes the status but nothing is flushed (FlushMode.MANUAL).
- **Flush order**: `delete` then `save` of tag "sale" fails with a unique violation, because Hibernate
  runs INSERT before DELETE. Fix: `tags.flush()` between them. (paste the exception message)
- **Optimistic locking**: two threads load the same order version; one UPDATE ... where version=? gets 0 rows
  and throws ObjectOptimisticLockingFailureException. The version rises by exactly 1.
  (paste the lines: "Getting transaction for [...OrderService.changeStatus]", the update with version=?, the rollback)
- **Retry**: `OrderFacade.changeStatusWithRetry` lives in a separate bean (the exception is raised at commit),
  retries up to 3 times; both writers succeed and the version rises by 2.

## Task 5: N+1 performance problem

- **The Problem:** The naive `linesPerPaidOrder()` implementation fetches orders with a single query, but then triggers an additional `SELECT` query for each individual order when `o.getLines().size()` is called inside the loop. For 50 orders, this executes exactly 51 queries (the N+1 problem).
- **The Solution:** Adding `@EntityGraph(attributePaths = "lines")` to a new `findAllByStatusWithLines` repository query forces Hibernate to fetch the orders and their associated lines together using a single `JOIN` query.

| variant                 | statements | median ms |
|-------------------------|-----------:|----------:|
| naive (lazy loop)       |         51 |       6.7 |
| join fetch              |          1 |       4.1 |
| @EntityGraph            |          1 |       4.4 |
| @BatchSize(16)          |          5 |       7.9 |
