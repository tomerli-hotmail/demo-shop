# DemoShop — Microservices Demo

A simplified e-commerce microservices demo inspired by [GCP Online Boutique](https://github.com/GoogleCloudPlatform/microservices-demo).

**Live demo:** https://demoshop-frontend.onrender.com
> Built with Vite + React, Node.js/Express, and Java/Spring Boot. Backed by Supabase (PostgreSQL). Deployed on Render.

**Demo credentials:** `demo / demo123` · `admin / admin456`

---

## Architecture

```
Browser
  └─→ demoshop-frontend          (Vite + React SPA + Express proxy)
        ├─→ demoshop-catalog      (Node/Express — product listings)
        ├─→ demoshop-cart         (Node/Express — shopping cart)
        └─→ demoshop-checkout     (Java/Spring Boot — orders & auth)
                ├─→ demoshop-cart     (fetch cart items)
                └─→ demoshop-payment  (Node/Express — mock payments)
```

## Services

| Service | Tech | Responsibility |
|---|---|---|
| `frontend` | Vite + React + Express | SPA + API proxy |
| `product-catalog-svc` | Node.js/Express | Product listings & search |
| `cart-svc` | Node.js/Express | Shopping cart (Supabase) |
| `checkout-svc` | Java 17 / Spring Boot | Order orchestration & auth |
| `payment-svc` | Node.js/Express | Mock payment processing |

---

## Local Development

### Prerequisites

- Node.js 18+
- Java 17+
- Maven (`brew install maven`)
- A [Supabase](https://supabase.com) project (free tier works)

### 1. Database setup

1. Create a project at [supabase.com](https://supabase.com)
2. In **SQL Editor**, run `db/schema.sql` then `db/seed.sql`
3. Copy your project URL and anon key from **Settings → API**

### 2. Environment variables

Copy `.env.example` → `.env` in each Node.js service and the frontend directory. Fill in your Supabase URL and anon key.

For `checkout-svc`, copy `src/main/resources/application.properties.example` → `application.properties`.

### 3. Start everything

```bash
# Each service in a separate terminal:
cd product-catalog-svc && npm install && npm run dev   # :3001
cd cart-svc            && npm install && npm run dev   # :3002
cd payment-svc         && npm install && npm run dev   # :3003
cd checkout-svc        && mvn spring-boot:run          # :8080
cd frontend            && npm install && npm run dev   # :3000 + :4000
```

The frontend `dev` script starts two processes: the Vite dev server on `:3000` (SPA) and the Express API proxy on `:4000`. Vite automatically proxies `/api/*` requests to Express.

Visit [http://localhost:3000](http://localhost:3000)

---

## Deployment (Render)

The repo includes a `render.yaml` Blueprint that defines all 5 services. To deploy your own instance:

1. Fork this repo
2. Sign up at [render.com](https://render.com) → **New → Blueprint** → connect your fork
3. In the Render dashboard, set these environment variables for each service:

   | Service | Variable | Value |
   |---|---|---|
   | catalog, cart, payment | `SUPABASE_URL` | your Supabase project URL |
   | catalog, cart, payment | `SUPABASE_ANON_KEY` | your Supabase anon key |
   | checkout | `SUPABASE_URL` | your Supabase project URL |
   | checkout | `SUPABASE_KEY` | your Supabase anon key |

4. Trigger a deploy — all 5 services go live automatically

> **Note:** Free tier services sleep after 15 min of inactivity (50s cold start). Use [UptimeRobot](https://uptimerobot.com) to ping each `/health` endpoint every 5 minutes to keep them warm.

---

## User Flow

1. Browse products on the home page
2. Click a product → Add to Cart
3. View cart → adjust quantities
4. Sign in (or use guest checkout)
5. Enter email + shipping address → Place Order
6. See order confirmation with transaction ID
7. View past orders under **My Orders**
