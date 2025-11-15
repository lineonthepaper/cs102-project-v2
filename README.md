# Smart Attendance System

Face recognition-based attendance system with Java Spring Boot backend and React frontend.

## Libraries used

- [OpenCV](https://opencv.org/)
- [DJL (Deep Java Library)](https://github.com/deepjavalibrary/djl)
- [Lombok](https://projectlombok.org/)
- [SLF4J](https://www.slf4j.org/)
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/)
- [Apache POI](https://poi.apache.org/)

## Prerequisites

- Java 21
- Node.js 18+

## Setup

### 1. Configure Environment

**If you are Prof Zhang and have received the code through our submission, there is no configuration needed. :)** Skip to [section 2](#2-run-application)!

Ensure that the environment files are already in their respective folders. 

There are **3 configuration files** to set up:

#### a) Root Environment File (`/env`)
Used by backend startup scripts to set database connection variables.

**macOS/Linux:**
```bash
cp env.example env
```

**Windows:**
```cmd
copy env.example env
```

Edit `env` with your Supabase credentials:
- API URL and keys: Supabase Dashboard → Project Settings → API
- Database credentials: Project Settings → Database → Connection String (use **Session Pooler**)

#### b) Frontend Environment File (`/frontend/.env`)
Already configured with default values. Update if using a different Supabase project:
```env
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
```

#### c) Backend Application Properties (`/backend/src/main/resources/application.properties`)
Already configured to use environment variables from `/env` file. No changes needed.

⚠️ **Never commit the `/env` or `/frontend/.env` files!**

### 2. Run Application

**macOS/Linux:**
```bash
# Both services
./scripts/run-all.sh

# Individual services
./scripts/run-backend.sh
./scripts/run-frontend.sh
```

**Windows:**
```cmd
# Both services
scripts\run-all.bat

# Individual services
scripts\run-backend.bat
scripts\run-frontend.bat
```

**Access:** http://localhost:5173

## Ports

- Backend: `8080`
- Frontend: `5173`

## Project Structure

```
CS102 V2/
├── backend/              # Spring Boot API
├── frontend/             # React UI
├── database/             # SQL schemas
└── scripts/              # Run scripts (.sh for macOS/Linux, .bat for Windows)
```