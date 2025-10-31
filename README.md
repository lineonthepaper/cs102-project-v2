# Smart Attendance System

Face recognition-based attendance system with Java Spring Boot backend and React frontend.

## Prerequisites

- Java 21
- Node.js 18+
- Supabase account
- [OpenCV 4.12.0](https://opencv.org/releases/)
- Internet connection (for automatic model download)

## Pre Setup

### 1. OpenCV Native Library
Download the OpenCV native library based on your OS:
- **macOS/Linux**: Extract `libopencv_java4xx.dylib` (or `.so` on Linux) and place it under `backend/src/main/resources/native/`
- **Windows**: Extract `opencv_java4xx.dll` and place it under `backend/src/main/resources/native/`

Download from: https://opencv.org/releases/

### 2. Face Recognition Model (Automatic)
The face recognition model will be **automatically downloaded** when you first start the backend. The setup script checks for the model and downloads it if missing (~249 MB).

**Manual setup (optional):**
If you prefer to download manually:
- Run the setup script:
  ```bash
  # macOS/Linux
  ./scripts/setup-models.sh
  
  # Windows
  scripts\setup-models.bat
  ```

**Note:** The model file (`facenet.onnx`) is ~249 MB and is intentionally not committed to Git. It will be automatically downloaded on first run. 

## Setup

### 1. Configure Environment

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

**Face recognition model not found:**
If you see an error about `facenet.onnx` missing:
1. Run the setup script:
   ```bash
   # macOS/Linux
   ./scripts/setup-models.sh
   
   # Windows
   scripts\setup-models.bat
   ```
2. The script will automatically download the ~249 MB ArcFace model
3. Verify it completed successfully (should show ~249 MB file size)
4. Restart the backend server

**Model download fails:**
- Check your internet connection
- Verify you have ~300 MB free disk space
- Try running the setup script manually (see above)
- If still failing, download manually from:
  ```
  https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx
  ```
  Save to: `backend/src/main/resources/models/facenet.onnx`
