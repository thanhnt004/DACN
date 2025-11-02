
# Example for a Vite/React app (adjust to Next.js if used)
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN npm install -g pnpm && pnpm install --frozen-lockfile

FROM node:22-alpine AS build
WORKDIR /app
COPY . ./
COPY --from=deps /app/node_modules ./node_modules
RUN npm install -g pnpm && pnpm build

# Production image: serve static with a small server (or use nginx)
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/dist ./dist
RUN npm install -g serve
EXPOSE 3000
CMD ["serve", "-s", "dist", "-l", "3000"]