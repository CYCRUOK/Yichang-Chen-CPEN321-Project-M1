# CPEN 321 – M1 (Yichang Chen)

Android (Kotlin / Jetpack Compose) app with a Node.js / TypeScript backend for the M1 skeleton:

| Button | What it does |
|---|---|
| **Login + Server** | Google sign-in (Credential Manager), then over HTTPS shows server public IP, client IP, server & client local time (`hh:mm:ss GMT+hh:mm`), the developer's name (from `GET /api/name`) and the signed-in user's name. |
| **Live Updates** | A 16×16 pixel-art image assembles live. The backend keeps one connection to the course server `wss://8.229.22.124` and relays every pixel verbatim to the app over its own WebSocket (`wss://<host>/ws`). |
| **Timer** | Minutes + seconds countdown. When it fires: vibration, notification, confetti and a **"While You Waited"** summary – how far the ISS flew (live `wheretheiss.at` API + great-circle math, on a mini world map), how far Earth and light travelled, heartbeats, births, etc. |

**Deployed backend:** `https://8-229-19-28.sslip.io` (public IP `8.229.19.28`, Google Cloud `us-west1`, Let's Encrypt certificate).

---

## Requirements

- [git](https://git-scm.com/install/)
- Backend: [Node.js](https://nodejs.org/en/download/) 22+ and npm 10+ (local run) **or** Docker + Docker Compose v2.24+
- Frontend: [Android Studio](https://developer.android.com/studio) (Narwhal 3 / 2025.1.3 or newer – required by AGP 8.13), Java 17, Android SDK 35 + 36

---

## Backend

### Environment configuration

```bash
cp backend/.env.example backend/.env
```

| Variable | Purpose |
|---|---|
| `PORT` | Listen port (default `3000`). |
| `OWNER_FIRST_NAME`, `OWNER_LAST_NAME` | Developer name returned by `GET /api/name`. Set to `Yichang` / `Chen`. |
| `UPSTREAM_WS_URL` | Course pixel stream to relay (default `wss://8.229.22.124`). |
| `UPSTREAM_WS_INSECURE` | `true` only if the upstream ever uses a self-signed certificate (it currently has a valid Let's Encrypt one). |
| `SERVER_PUBLIC_IP` | Optional. If set, `GET /api/server-ip` returns it instead of asking `api64.ipify.org`. |
| `JWT_SECRET`, `MONGODB_URI`, `GOOGLE_CLIENT_ID` | Not used by M1 (kept from the template for later milestones). |

### Option 1: run locally

```bash
cd backend
npm install
npm run dev        # TypeScript with auto-reload on :3000
# or: npm run build && npm start
```

### Option 2: Docker Compose

```bash
./scripts/run-backend.sh      # or: docker compose up --build -d
docker compose down
```

M1 has no database, so `docker-compose.yml` runs only the backend container (multi-stage
Dockerfile, `node dist/index.js`, `restart: unless-stopped`).

### HTTP API and WebSocket

| Endpoint | Response |
|---|---|
| `GET /health` | `{"status":"ok"}` |
| `GET /api/server-ip` | `{"ip":"8.229.19.28"}` (IPv4 or IPv6, via ipify or `SERVER_PUBLIC_IP`) |
| `GET /api/server-time` | `{"time":"12:18:22 GMT+01:00"}` – server local time at call time, 24-hour |
| `GET /api/name` | `{"first":"Yichang","last":"Chen"}` |
| `GET /api/ws-status` | `{"upstreamUrl","upstreamConnected","clients","relayed"}` – relay diagnostics |
| `WS /ws` | Relayed pixel stream: `{"x":<0-15>,"y":<0-15>,"color":"#rrggbb"}` per message, unchanged from the course server |

Quick check of a running instance (local or deployed):

```bash
./scripts/smoke-test-backend.sh https://8-229-19-28.sslip.io   # or http://localhost:3000
```

### Tests

```bash
./scripts/run-backend-interface-tests.sh    # tests/mock + tests/no-mock with coverage
cd backend && npm test                       # everything incl. tests/unit
```

`tests/no-mock/pixelRelay.test.ts` spins up a fake upstream WebSocket server, so no network is needed.

### Cloud deployment (what is running now)

Fresh Ubuntu 22.04/24.04 VM with TCP 22, 80, 443 open:

```bash
# on the VM
curl -fsSL https://raw.githubusercontent.com/CYCRUOK/Yichang-Chen-CPEN321-Project-M1/main/scripts/cloud-setup.sh -o cloud-setup.sh
OWNER_FIRST_NAME=Yichang OWNER_LAST_NAME=Chen bash cloud-setup.sh https://github.com/CYCRUOK/Yichang-Chen-CPEN321-Project-M1.git
# installs Docker, clones the repo to ~/cpen321, writes backend/.env, docker compose up -d

DOMAIN=8-229-19-28.sslip.io EMAIL=<you@example.com> bash ~/cpen321/scripts/cloud-https.sh
# nginx + Let's Encrypt: https://DOMAIN -> :3000, wss://DOMAIN/ws -> ws://:3000/ws, HTTP redirects to HTTPS
```

`<ip-with-dashes>.sslip.io` resolves to the IP encoded in the name, so no DNS setup is required.
To update: `cd ~/cpen321 && git pull && sudo docker compose up --build -d`.

---

## Frontend

### Setup

1. Open `frontend/` in Android Studio and let Gradle sync (or `cd frontend && ./gradlew build`).
2. SDK Manager: make sure **Android SDK Platform 35** (compileSdk, auto-installed by AGP if licences are accepted) and **36** (emulator image) are installed.
3. Device: a **Pixel 9 AVD with Android Baklava (API 36, Google Play image)**, or a physical device with USB debugging.
4. Copy the config and fill it in:
   ```bash
   cp frontend/local.properties.example frontend/local.properties
   ```

   | Property | Value |
   |---|---|
   | `sdk.dir` | Path to the Android SDK (forward slashes on Windows, e.g. `C:/Users/me/AppData/Local/Android/Sdk`). |
   | `API_BASE_URL` | Backend URL baked into the APK. **Deployed:** `https://8-229-19-28.sslip.io`. Local backend from the emulator: `http://10.0.2.2:3000`; from a USB phone: `http://localhost:3000` after `adb reverse tcp:3000 tcp:3000`. The WebSocket URL is derived from it (`/ws`, `http→ws`, `https→wss`). |
   | `GOOGLE_CLIENT_ID` | The **Web application** OAuth client ID (see below). |
   | `RELEASE_STORE_FILE` … `RELEASE_KEY_PASSWORD` | Optional release signing; without them `assembleRelease` signs with the debug key. |

### Build and run

- **Debug:** green Run button in Android Studio, or `./scripts/run-frontend.sh` (starts the `Pixel_9` AVD, builds, installs, launches).
- **Release APK:** `cd frontend && ./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`. Google sign-in only works if the SHA-1 of the signing key is registered (below).

### Google sign-in (Button 1)

Google Cloud Console → APIs & Services → Credentials:

1. **Android** OAuth client(s): package `com.example.cpen321application` + the SHA-1 of every key that signs an APK you want to run. Debug key:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
   ```
   The submitted `M1_*.apk` is signed with the developer's release key, whose SHA-1 is already registered.
2. **Web application** OAuth client → its client ID goes into `local.properties` as `GOOGLE_CLIENT_ID`.
3. OAuth consent screen: the app must be **published** (External audience) so that any Google account can sign in; in *Testing* mode only listed test users can.

The name shown after sign-in is the Google account's profile name (given + family name).

### Tests

- Host unit tests (view models, time/IP formatting, ISS maths, pixel parsing): `cd frontend && ./gradlew testDebugUnitTest`
- Instrumented E2E tests (`app/src/androidTest/.../e2e`): `./scripts/run-frontend-e2e-tests.sh` (needs Docker for the backend and the `Pixel_9` AVD), or directly against any connected device: `cd frontend && ./gradlew connectedDebugAndroidTest`.

  The E2E tests drive every screen with fakes (Google authenticator, backend API, pixel stream, ISS tracker), so they pass without network. The real Google account picker is system UI and is checked manually.

---

## Notes for graders

- Backend and relay are live at `https://8-229-19-28.sslip.io` (`/health`, `/api/ws-status` for a quick look).
- The server runs in UTC, so "Server local time" shows `GMT+00:00`; the client row shows the phone's zone.
- Timer: Android 13+ asks for notification permission on the first **Start**; the ISS card needs internet (`api.wheretheiss.at`), everything else is computed on-device.
- Developed and tested on a physical FCNT arrows phone (model M08, Android 15) and verified to build for API 36.
