# Smart Attendance System

Face recognition-based attendance system built with Java Spring Boot and React.

## 🚀 Quick Start

### Prerequisites
- Java 21
- Node.js & npm
- PostgreSQL (via Supabase)

### 1. Setup Environment

**Important:** Copy the example environment file and add your credentials:

```bash
cp env.example env
```

Then edit `env` with your Supabase credentials:
- Get API keys from: Supabase Dashboard → Project Settings → API
- Get database password from: Project Settings → Database → Connection String
- Use **direct connection** (db.xxx.supabase.co), NOT pooler

**⚠️ Security:** The `env` file contains sensitive credentials and is excluded from Git!

### 2. Run the Application

From the project root:

```bash
# Run both backend and frontend
./run-all.sh

# Or run separately:
./run-backend.sh    # Backend on http://localhost:8080
./run-frontend.sh   # Frontend on http://localhost:5173
```

The application will be available at **http://localhost:5173**

## 📁 Project Structure

```
CS102 V2/
├── README.md              # This file
├── env.example            # Environment template (safe to commit)
├── env                    # Your credentials (DO NOT COMMIT!)
│
├── run-all.sh            # Run both services
├── run-backend.sh        # Run backend only
├── run-frontend.sh       # Run frontend only
│
├── backend/              # Java Spring Boot backend
├── frontend/             # React + TypeScript frontend
│
├── documents/            # Documentation
│   ├── techstack.md     # Technology stack
│   └── features.md      # Feature specifications
│
├── scripts/              # Build and run scripts
└── dbschema.sql         # Database schema
```

## 🔧 Configuration

### Database Connection
- **Type**: Supabase PostgreSQL (direct connection)
- **Port**: 5432
- **Credentials**: Set in `env` file

### Application Ports
- **Backend**: 8080
- **Frontend**: 5173

## 🛠️ Development

### Backend (Java + Spring Boot)
```bash
cd backend
./gradlew build        # Build
./gradlew bootRun      # Run
```

### Frontend (React + Vite)
```bash
cd frontend
npm install            # Install dependencies
npm run build          # Build for production
npm run dev            # Development server
```

## 📚 Documentation

- `documents/techstack.md` - Technology stack details
- `documents/features.md` - Feature specifications
- `dbschema.sql` - Database schema

## 🆘 Troubleshooting

**Backend fails to start:**
1. Check Java version: `java -version` (need Java 21)
2. Verify `env` file exists and has correct credentials
3. Ensure you're using direct connection (db.xxx.supabase.co)

**Frontend fails to build:**
1. Run `npm install` in frontend directory
2. Check Node.js version: `node --version`

**Database connection issues:**
1. Verify password in `env` file is correct
2. Make sure Supabase project is not paused
3. Check you're using direct connection, not pooler

**Port already in use:**
```bash
lsof -ti:8080 | xargs kill -9  # Backend
lsof -ti:5173 | xargs kill -9  # Frontend
```

## 🔒 Security

- `env` file contains sensitive credentials - **never commit it!**
- Use `env.example` as a template
- The `.gitignore` ensures `env` is excluded from version control
- Different credentials should be used for development and production

## 📝 License

[Add your license here]

## 👥 Contributors

[Add contributors here]
