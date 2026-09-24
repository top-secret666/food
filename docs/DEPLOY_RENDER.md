# Deploy Aroma APIs (24/7)

## Option A — Render (recommended, free web services)

1. Ensure GitHub repo `top-secret666/food` has: `user-service/`, `restaurant-service/`, `order-service/`, `render.yaml`
2. Sign in at https://dashboard.render.com (Login with GitHub)
3. **Blueprints** → **New Blueprint Instance** → select `food` → Apply
4. Wait until `aroma-user`, `aroma-restaurant`, `aroma-order` are Live
5. URLs (usually):
   - https://aroma-user.onrender.com
   - https://aroma-restaurant.onrender.com
   - https://aroma-order.onrender.com
6. In Vercel → Project `aroma-food` → Settings → Environment Variables (Production):

```text
REACT_APP_USER_API=https://aroma-user.onrender.com
REACT_APP_RESTAURANT_API=https://aroma-restaurant.onrender.com
REACT_APP_ORDER_API=https://aroma-order.onrender.com
REACT_APP_GOOGLE_CLIENT_ID=934997964943-dli89dipdsr9fbgbf6804mdghoc9rd3i.apps.googleusercontent.com
```

7. Redeploy Vercel (Deployments → … → Redeploy)
8. Google Cloud → OAuth Web client → Authorized JavaScript origins must include:
   `https://aroma-food.vercel.app` and/or `https://reactfistapp.vercel.app`

Notes:
- Free Render services sleep after ~15 min idle; first request can take 30–60s (cold start).
- H2 in-memory DB resets on each restart (demo OK).

## Option B — Railway

```bash
npx @railway/cli login
npx @railway/cli init
# deploy each service folder with Dockerfile + SPRING_PROFILES_ACTIVE=local
```

Then set the same three `REACT_APP_*` URLs in Vercel.
