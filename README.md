# E-Commerce Microservices Platform

A scalable, event-driven microservices architecture for e-commerce built with Spring Boot 3.5.10, Kafka, Redis, and distributed transactions using the Saga pattern.

## Project Structure

```
.
├── discovery-server/          # Eureka Service Registry
├── api-gateway/               # API Gateway with JWT Auth
├── order-service/             # Order Management & Saga Orchestration
├── product-service/           # Product Catalog & Inventory
├── promotion-service/         # Coupons & Flash Sales
├── notification-service/      # Email Notifications
├── loyalty-service/           # Points & Rewards Program
├── auth-service/              # User Authentication & Profiles
├── docker/                    # Database initialization scripts
├── logstash/                  # ELK Configuration
├── docker-compose.yml         # Infrastructure setup
├── REQUIREMENTS.md            # Detailed requirements
└── AGENTS.md                  # AI Agent Guidelines
```

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Framework** | Spring Boot | 3.5.10 |
| **Cloud** | Spring Cloud | 2025.0.1 |
| **Service Discovery** | Eureka | Netflix |
| **API Gateway** | Spring Cloud Gateway | - |
| **Message Queue** | Apache Kafka | 3.x |
| **Cache/Lock** | Redis + Redisson | 3.24.3 |
| **Database** | MySQL | 8.0 |
| **Auth** | Keycloak | 24.0 |
| **Logging** | ELK Stack | 8.x |
| **Java** | Java | 21 |

## Service Overview

### Core Services

| Service | Port | Purpose |
|---------|------|---------|
| **discovery-server** | 8761 | Service registry |
| **api-gateway** | 8282 | API entry point, JWT validation |
| **order-service** | 8080 | Order management, saga orchestration |
| **product-service** | 8888 | Product catalog, inventory management |
| **promotion-service** | 8083 | Coupons, flash sales |
| **notification-service** | 8084 | Email notifications |
| **loyalty-service** | 8085 | Points, tier system, referrals |
| **auth-service** | 8086 | User authentication, profiles |

### Infrastructure Services

| Service | Port | Purpose |
|---------|------|---------|
| MySQL | 3306 | Data persistence |
| Redis | 6379 | Caching, distributed locks |
| Kafka | 9092 | Async messaging |
| Zookeeper | 2181 | Kafka coordination |
| Keycloak | 8080 | Identity provider |
| Elasticsearch | 9200 | Log indexing |
| Kibana | 5601 | Log visualization |

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.8+

### Setup

1. **Clone and navigate to project root**
   ```bash
   cd /path/to/code-practice
   ```

2. **Start infrastructure**
   ```bash
   docker-compose up -d
   ```
   Wait for all services to be healthy:
   ```bash
   docker-compose ps
   ```

3. **Start services in order** (from each service directory)
   ```bash
   # Terminal 1: Discovery Server
   cd discovery-server
   ./mvnw spring-boot:run

   # Terminal 2: Product Service
   cd product-service
   ./mvnw spring-boot:run

   # Terminal 3: Order Service
   cd order-service
   ./mvnw spring-boot:run

   # Terminal 4: Promotion Service
   cd promotion-service
   ./mvnw spring-boot:run

   # Terminal 5: Loyalty Service
   cd loyalty-service
   ./mvnw spring-boot:run

   # Terminal 6: Notification Service
   cd notification-service
   ./mvnw spring-boot:run

   # Terminal 7: Auth Service
   cd auth-service
   ./mvnw spring-boot:run

   # Terminal 8: API Gateway
   cd api-gateway
   ./mvnw spring-boot:run
   ```

4. **Verify services are running**
   ```bash
   curl http://localhost:8761  # Eureka
   curl http://localhost:8282  # API Gateway
   ```

## Key Features

### 1. Order Management with Saga Pattern
- Distributed transaction management across services
- Coupon validation and application
- Inventory reservation with TTL
- Payment verification workflow
- Automatic compensation on failure

### 2. Distributed Stock Locking
- Redis-based distributed locks
- Deadlock prevention via sorted product IDs
- Inventory reservation with 15-minute TTL
- Atomic stock operations

### 3. Flash Sale Management
- Redis-based stock counters
- Rate limiting per user
- Early access for premium members
- Handling 10,000+ concurrent requests

### 4. Coupon System
- Percentage & fixed amount discounts
- Minimum order value constraints
- Per-user & total usage limits
- Auto-expiration

### 5. Loyalty Program
- Points earning with tier multipliers
- Tier progression (BRONZE → SILVER → GOLD → PLATINUM)
- Referral program with bonus points
- Point expiration (12 months)

### 6. Email Notifications
- Order status updates
- Flash sale alerts
- Loyalty rewards
- Batch sending for 1M+ users
- Retry mechanism with DLQ

### 7. Structured Logging
- JSON format via Logstash
- MDC for correlation tracking
- ELK stack integration
- Kibana dashboards

## Event-Driven Architecture

### Kafka Topics

```
order.created              → ProductService, NotificationService
product.locked            → OrderService
payment.verify            → PaymentService (stub)
payment.verified          → OrderService
order.completed           → LoyaltyService, NotificationService
order.failed              → NotificationService
flashsale.created         → NotificationService, ProductService
flashsale.starting.soon   → NotificationService
loyalty.points.earned     → NotificationService
loyalty.tier.upgraded     → NotificationService
```

### Retry & DLQ Strategy

- **Max Retries**: 3 attempts (configurable)
- **Backoff**: Exponential (initial 1s, multiplier 2.0)
- **DLQ**: Auto-created with `.dlq` suffix
- **Monitoring**: Check DLQ topics in Kafka UI for failures

## API Examples

### Create Order
```bash
curl -X POST http://localhost:8282/v1/orders \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user_001",
    "couponCode": "SAVE20",
    "idempotencyKey": "unique_key_123",
    "orderItems": [
      {"productId": "PROD_001", "quantity": 2},
      {"productId": "PROD_002", "quantity": 1}
    ]
  }'
```

### Validate Coupon
```bash
curl -X GET "http://localhost:8282/api/promotions/coupons/SAVE20/validate?customerId=user_001" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Get Loyalty Points
```bash
curl -X GET http://localhost:8282/api/loyalty/user_001/points \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## Monitoring & Observability

### Dashboards

| Tool | URL | Purpose |
|------|-----|---------|
| Eureka | http://localhost:8761 | Service registration |
| Kafka UI | http://localhost:8081 | Topic/broker monitoring |
| Kibana | http://localhost:5601 | Log analysis |
| RedisInsight | http://localhost:8001 | Redis data inspection |

### Key Logs to Monitor

1. **Order Creation Flow**
   - `action=ORDER_CREATE` with `correlationId`
   - `action=COUPON_VALIDATION` with coupon code
   - `action=PRODUCT_LOCK` with product IDs
   - `action=PAYMENT_VERIFY` with payment ID

2. **Error Cases**
   - `level=WARN` for retry attempts
   - `level=ERROR` for final failures
   - Check `failureReason` field

3. **Flash Sale Performance**
   - `action=FLASH_SALE_STOCK_DECREMENT`
   - Monitor `remaining_stock` field
   - Track `sold_quantity` rate

## Database Schemas

### Order Service
- `orders` table (order metadata, coupon code, final amount)
- `order_items` table (line items with pricing)

### Product Service
- `products` table (catalog)
- `inventories` table (stock levels)

### Promotion Service
- `coupons` table (coupon definitions)
- `flash_sales` table (flash sale configs)

### Loyalty Service
- `loyalty_accounts` table (user points & tier)
- `point_transactions` table (earning/redemption history)
- `referrals` table (referral tracking)

### Notification Service
- `email_messages` table (sent/pending emails)

## Configuration

### Environment Variables

```bash
# Email Configuration
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=app-password

# Keycloak
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin

# MySQL
MYSQL_ROOT_PASSWORD=root
MYSQL_USER=ecommerce
MYSQL_PASSWORD=password
```

### Application Properties

Each service has `application.yaml` with:
- Server port
- Database connection
- Kafka brokers
- Redis connection
- Eureka registration
- Keycloak issuer URI

## Testing

### Unit Tests
```bash
cd order-service
./mvnw test
```

### Integration Tests
```bash
./mvnw verify
```

### Load Testing (Flash Sales)
```bash
# Using Apache JMeter or similar
# Simulate 1000+ concurrent requests to /api/promotions/flash-sales/{id}/participate
```

## Troubleshooting

### Services Not Connecting

1. **Check Eureka Registration**
   ```bash
   curl http://localhost:8761/eureka/apps
   ```

2. **Verify Kafka Broker**
   ```bash
   docker exec kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092
   ```

3. **Check Redis Connection**
   ```bash
   docker exec redis redis-cli ping
   ```

### Order Processing Issues

1. **Check Order Status**
   ```bash
   curl http://localhost:8282/v1/orders/{orderId}
   ```

2. **View Order Saga Progress**
   - Check logs in Kibana with `orderId` filter
   - Look for `action=PRODUCT_LOCK` and `action=PAYMENT_VERIFIED`

3. **Check Kafka Topics**
   - Visit Kafka UI: http://localhost:8081
   - Look for `order.created`, `product.locked`, `order.failed` messages

### Email Not Sending

1. **Enable Less Secure Apps** (Gmail)
   - https://myaccount.google.com/apppasswords

2. **Check Email Queue**
   ```bash
   curl http://localhost:8084/api/notifications/emails?status=FAILED
   ```

3. **Review Email Logs**
   - Search Kibana for `service=notification-service` and `action=EMAIL_SEND`

## Development Guidelines

See [AGENTS.md](./AGENTS.md) for:
- Code authoring rules
- Service patterns
- Exception handling
- Logging conventions
- Event design

## License

Proprietary - Internal Use Only

## Support

For issues or questions, please refer to:
- REQUIREMENTS.md - Detailed feature specs
- AGENTS.md - Technical architecture & patterns
- Service README files - Service-specific setup
