# syntax=docker/dockerfile:1

# Build stage: compile TypeScript to dist/
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Runtime stage: production dependencies only, run the compiled server
FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist

# sets process.env.PORT
ENV PORT=3000

# documents that the container listens on port 3000
EXPOSE 3000

CMD ["node", "dist/index.js"]
