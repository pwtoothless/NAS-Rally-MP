# Stage 1: Build the Kotlin web application
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Install OS tools required by Kotlin's Node/Yarn setup
RUN apt-get update && apt-get install -y git python3 make g++ curl && rm -rf /var/lib/apt/lists/*

# Copy build configuration files first for caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Copy the rest of the source code
COPY . .

# Compile the production web distribution
# Note: Depending on your Kotlin/JS or Compose Web setup, this task might be 
# :webApp:wasmJsBrowserDistribution or :webApp:build
RUN ./gradlew :webApp:wasmJsBrowserDistribution --no-daemon

# Stage 2: Serve the static files with Nginx
FROM nginx:alpine

# Reconfigure Nginx to listen on port 8089 instead of the default port 80
RUN sed -i 's/listen  *80;/listen 8089;/g' /etc/nginx/conf.d/default.conf

# Copy the generated static files into the Nginx HTML directory
COPY --from=builder /app/webApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html

# Expose port 8089
EXPOSE 8089

CMD ["nginx", "-g", "daemon off;"]
