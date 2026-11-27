# Task 3: Roles and Method Security

## 1. Alice creates an order (201 Created)
```bash
$ curl -i -u alice:Alice-pass-123 -X POST http://localhost:8080/api/orders \ -H "Content-Type: application/json" -d '{"productId":2,"quantity":1}'

HTTP/1.1 201 
Location: /api/orders/2
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 27 Nov 2026 06:32:24 GMT

{"customer":"alice","productId":2,"quantity":1,"id":2,"status":"NEW"}

$ curl -i -u bob:Correct-Horse-9 http://localhost:8080/api/orders/1

HTTP/1.1 403 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/problem+json
Transfer-Encoding: chunked
Date: Fri, 27 Nov 2026 06:33:43 GMT

{"detail":"Access denied","instance":"/api/orders/1","status":403,"title":"Forbidden"}

$ curl -i -u admin:Admin-pass-123 http://localhost:8080/api/orders/1

HTTP/1.1 200 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Content-Length: 69
Date: Fri, 27 Nov 2026 06:34:35 GMT

{"customer":"alice","productId":2,"quantity":1,"id":1,"status":"NEW"}

$ .\mvnw -q test -Dtest='Order*SecurityTest'
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0