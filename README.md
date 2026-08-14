## Getting Started

Two ways to run this: Docker (no local Java/Maven needed) or directly with Maven. Pick either.

### Option 1: Docker

Prerequisites: 
- Docker installed and running

Build container image

`docker build -t library-svc .`

Create volume to persist between container runs (This does not actually matter in our case, but does not hurt anything to have if the database is changed to persist to a file)

`docker volume create library-data`

Run container

`docker run -d --name library-svc -p 8080:8080 -v library-data:/app/data library-svc:latest`

### Option 2: Maven

Prerequisites:
- JDK 21 or newer installed (a full JDK, not just a JRE — Maven needs to compile the code). Check with `java -version`.
- No local Maven install required — the repo includes the Maven wrapper (`./mvnw` on macOS/Linux, `mvnw.cmd` on Windows), which downloads the correct Maven version for you on first run.
- Port 8080 free locally.

Run the tests

`./mvnw test`

Start the app

`./mvnw spring-boot:run`

The app starts on `http://localhost:8080` either way.

## Using the API

Go here for swagger docs defining the required endpoints:
http://localhost:8080/swagger-ui/index.html

By default there is data seeded by [./src/main/java/com.weatherspoon.library.DataSeed.java] It creates 
2 users and 4 books 

Submit requests there and validate the backend and enjoy your new book :)
