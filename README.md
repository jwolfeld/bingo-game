# Bingo Game — Full Stack Application

A web-based Bingo game with an admin-driven **Setup Mode** and **Play Mode**.
Built with **Java 17 + Spring Boot 3** (server) and **React 18** (client),
communicating via REST.

---

## Project Structure

```
bingo-game/
├── server/          ← Java Maven Spring Boot application
│   ├── pom.xml      ← includes frontend-maven-plugin (builds React automatically)
│   └── src/main/java/com/bingo/
│       ├── BingoApplication.java
│       ├── controller/
│       │   ├── GameController.java   ← REST endpoints (/api/games/...)
│       │   └── SpaController.java    ← Catch-all → serves React index.html
│       ├── service/
│       │   ├── GameService.java      ← Game logic
│       │   └── PdfService.java       ← PDF generation (Apache PDFBox)
│       ├── storage/
│       │   ├── GameStorage.java      ← Interface (swap for DB impl)
│       │   └── InMemoryGameStorage.java
│       ├── model/
│       │   ├── GameInstance.java
│       │   └── BingoGrid.java
│       └── dto/BingoDtos.java
│
└── client/          ← React application (built by Maven, not run separately)
    ├── package.json
    └── src/
        ├── App.jsx               ← Phase router (Setup → Play → Over)
        ├── App.css               ← All styles
        ├── index.js
        ├── api/bingoApi.js       ← REST client (relative /api paths)
        └── components/
            ├── SetupMode.jsx     ← Word entry + grid generation
            ├── PlayMode.jsx      ← Word calling + history
            └── GameOver.jsx      ← End screen
```

---

## Prerequisites

You only need Java and Maven installed. Node.js and npm are downloaded
automatically by Maven during the build via `frontend-maven-plugin`.

| Tool     | Minimum Version |
|----------|----------------|
| Java JDK | 17             |
| Maven    | 3.8+           |

---

## Running the Application

Everything runs from a single command:

```bash
cd server
mvn spring-boot:run
```

Maven will automatically:
1. Download Node.js and npm into `server/target/node/` (first run only)
2. Run `npm install` in the `client/` directory
3. Run `npm run build` to produce `client/build/`
4. Copy the React build into the Spring Boot static resources
5. Start the Spring Boot server

Then open **http://localhost:8080/bingo** in your browser. No separate npm process needed.

> **First run** downloads Node.js (~40 MB) and npm dependencies. Subsequent runs
> are fast because Node is cached in `target/node/` and `node_modules/` is reused.

### Build a deployable JAR

```bash
cd server
mvn package -DskipTests
java -jar target/bingo-server-1.0.0.jar
```

The fat JAR contains both the Spring Boot server and the compiled React app.
Deploy it anywhere Java 17 is available — no Node.js required on the server.

### Development workflow (hot-reload)

When actively changing the React frontend, you can still run the two processes
separately to get instant hot-reload:

```bash
# Terminal 1 — backend
cd server
mvn spring-boot:run -Dfrontend.skip=true   # skip the React build step

# Terminal 2 — frontend (proxies /api calls to :8080 via package.json proxy)
cd client
npm start
```

Use **http://localhost:3000** while developing, and the integrated
**http://localhost:8080** for testing or deployment.

---

## How to Play

### Setup Mode
1. Enter **at least 24 unique words** in the text area (comma- or newline-separated).
2. Set the **number of players** (each gets a unique 5×5 grid).
3. Click **Generate Grids** — the server creates N randomized grids.
4. Click **Download All Grids (ZIP)** to get a zip of N PDFs (one per player).
   Print and distribute them before the game.
5. Click **Start Game →** to enter Play Mode.

### Play Mode
- Click **Next Word** to draw a random word from the list.
- Call the word out loud to your players — they mark it on their own grids.
- All previously called words appear below in alphabetical order.
- When a player wins (you decide!), click **Game Over**.

---

## REST API Reference

| Method | Path                           | Description                          |
|--------|--------------------------------|--------------------------------------|
| POST   | `/api/games`                   | Create game, generate N grids         |
| GET    | `/api/games/{id}/state`        | Get full game state                  |
| POST   | `/api/games/{id}/start`        | Transition SETUP → PLAYING           |
| POST   | `/api/games/{id}/next-word`    | Draw next random word                |
| POST   | `/api/games/{id}/end`          | Mark game FINISHED                   |
| GET    | `/api/games/{id}/download-grids` | Download ZIP of player grid PDFs    |

### POST `/api/games` — Create Game

**Request:**
```json
{
  "words": ["Apple", "Banana", "Cherry", "..."],
  "playerCount": 6
}
```

**Response:**
```json
{
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "gridCount": 6
}
```

### POST `/api/games/{id}/next-word` — Draw Word

**Response:**
```json
{
  "word": "Dragon",
  "calledWords": ["Apple", "Dragon", "Forest"],
  "remainingCount": 21
}
```

---

## Persistence / Database

Game state is held **in memory** by default and resets on server restart.

To add database persistence:
1. Create a class that implements `com.bingo.storage.GameStorage`.
2. Annotate it with `@Component` (and optionally `@Primary` if keeping both).
3. Inject your preferred data source (JPA, MongoDB, Redis, etc.).
4. Remove (or de-prioritize) `InMemoryGameStorage`.

```java
@Component
@Primary
public class DatabaseGameStorage implements GameStorage {
    // ... JPA / JDBC / MongoDB implementation
}
```

---

## Building for Production

```bash
cd server
mvn package -DskipTests
java -jar target/bingo-server-1.0.0.jar
```

The single fat JAR includes the compiled React app. Open **http://localhost:8080/bingo**.

---

## Grid Design

- Each grid is **5×5** (25 cells).
- The **center cell (row 3, col 3)** is always a solid black **FREE** square.
- The remaining **24 cells** are filled with a random, non-repeating selection
  from the administrator's word pool.
- Each player's grid is independently randomized — the same word can appear on
  multiple players' grids, but never twice on the same grid.

---

## Deploying to a Cloud Server (Ubuntu 22.04 LTS + Apache2)

This section covers deploying the app to a Linux server alongside WordPress,
with Apache acting as a reverse proxy in front of the Spring Boot server.

### Architecture

```
Internet
    │
    ▼
Apache2 (port 80/443)
    ├── yourdomain.com/        → WordPress (PHP)
    └── yourdomain.com/bingo/  → Spring Boot (localhost:8080)
```

### Step 1 — Build the JAR locally

```bash
cd server
mvn package -DskipTests
```

This produces `server/target/bingo-server-1.0.0.jar` — a single fat JAR
containing the Spring Boot server and the compiled React frontend.

### Step 2 — Install Java on the server

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless
java -version   # should show 17.x
```

### Step 3 — Copy the JAR to the server

```bash
scp server/target/bingo-server-1.0.0.jar \
    jwolfeld@yourdomain.com:/home/jwolfeld/webs/bingo-server/bingo-server.jar
```

### Step 4 — Create the systemd service

Create `/etc/systemd/system/bingo-server.service`:

```ini
[Unit]
Description=Bingo Game Server
After=network.target

[Service]
User=jwolfeld
Group=jwolfeld
WorkingDirectory=/home/jwolfeld/webs/bingo-server
ExecStart=/usr/bin/java -jar /home/jwolfeld/webs/bingo-server/bingo-server.jar
StandardOutput=journal
StandardError=journal
SyslogIdentifier=bingo-server
Restart=on-failure
RestartSec=10
TimeoutStopSec=60

[Install]
WantedBy=multi-user.target
```

Then enable and start it:

```bash
sudo systemctl daemon-reload
sudo systemctl enable bingo-server    # start automatically on boot
sudo systemctl start bingo-server
sudo systemctl status bingo-server    # should show: active (running)
```

To tail the logs:

```bash
journalctl -u bingo-server -f
```

### Step 5 — Enable Apache proxy modules

```bash
sudo a2enmod proxy proxy_http
sudo systemctl reload apache2
```

### Step 6 — Configure the Apache reverse proxy

If you already have an existing VirtualHost config (e.g. for WordPress),
add these lines inside that `<VirtualHost>` block:

```apache
ProxyPreserveHost On
ProxyPass        /bingo/ http://localhost:8080/bingo/
ProxyPassReverse /bingo/ http://localhost:8080/bingo/
```

If you don't yet have a VirtualHost config, create
`/etc/apache2/sites-available/bingo.conf`:

```apache
<VirtualHost *:80>
    ServerName yourdomain.com

    ProxyPreserveHost On
    ProxyPass        /bingo/ http://localhost:8080/bingo/
    ProxyPassReverse /bingo/ http://localhost:8080/bingo/

    ErrorLog  ${APACHE_LOG_DIR}/bingo-error.log
    CustomLog ${APACHE_LOG_DIR}/bingo-access.log combined
</VirtualHost>
```

Then enable it:

```bash
sudo a2ensite bingo.conf
sudo apache2ctl configtest     # should say: Syntax OK
sudo systemctl reload apache2
```

### Step 7 — Verify

Open `http://yourdomain.com/bingo/` in your browser. The React app should
load. WordPress at `http://yourdomain.com/` is unaffected.

### Updating the app

```bash
# Build locally
cd server && mvn package -DskipTests

# Copy to server
scp server/target/bingo-server-1.0.0.jar \
    jwolfeld@yourdomain.com:/home/jwolfeld/webs/bingo-server/bingo-server.jar

# Restart the service on the server
ssh jwolfeld@yourdomain.com "sudo systemctl restart bingo-server"
```

### Firewall note

Spring Boot binds to port 8080 on localhost only. That port does **not** need
to be open in your firewall — Apache proxies to it internally. Only ports
80 (HTTP) and 443 (HTTPS) need to be publicly accessible.

### HTTPS (recommended)

```bash
sudo apt install certbot python3-certbot-apache
sudo certbot --apache -d yourdomain.com
```

Certbot updates your Apache config automatically. The reverse proxy to the
bingo app works over HTTPS without any changes to Spring Boot.

### macOS Gatekeeper note (local development)

On a new Mac, macOS may quarantine the Node binaries downloaded by Maven.
If you see a Gatekeeper warning about `fsevents.node`, click Done (not
Move to Trash), then run once:

```bash
xattr -dr com.apple.quarantine client/node_modules/
```

The warning will not reappear.
