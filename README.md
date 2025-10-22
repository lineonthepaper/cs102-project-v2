# Smart Attendance System

Face recognition-based attendance system with Java Spring Boot backend and React frontend.

## Prerequisites

- Java 21
- Node.js 18+
- Supabase account

## Setup

### 1. Configure Environment

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
- Database credentials: Project Settings → Database → Connection String (use Session Pooler)

⚠️ **Never commit the `env` file!**

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
├── scripts/              # Run scripts (.sh for macOS/Linux, .bat for Windows)
└── env                   # Config (DO NOT COMMIT)
```

## Development

### Backend
```bash
cd backend
./gradlew build      # macOS/Linux
gradlew.bat build    # Windows
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Troubleshooting

**Port in use:**
- macOS/Linux: `lsof -ti:8080 | xargs kill -9`
- Windows: `netstat -ano | findstr :8080` then `taskkill /PID <PID> /F`

**Database connection failed:**
- Check credentials in `env`
- Verify Supabase project is active
- Use Session Pooler connection string

**Java version mismatch:**
```bash
java -version  # Should show 21.x
```
