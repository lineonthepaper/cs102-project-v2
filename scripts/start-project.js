#!/usr/bin/env node

/**
 * Cross-platform helper that installs dependencies for the frontend/backend
 * and then boots both dev servers. Works on macOS/Linux and Windows.
 */
const { spawn } = require("child_process");
const path = require("path");
const fs = require("fs");
const net = require("net");

const projectRoot = path.resolve(__dirname, "..");
const backendDir = path.join(projectRoot, "backend");
const frontendDir = path.join(projectRoot, "frontend");

const isWindows = process.platform === "win32";
const npmCmd = isWindows ? "npm.cmd" : "npm";
const gradleCmd = isWindows ? "gradlew.bat" : "./gradlew";

function runCommand(command, args, cwd) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd,
      stdio: "inherit",
      shell: false,
    });

    child.on("error", reject);
    child.on("exit", (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`${command} ${args.join(" ")} exited with code ${code}`));
      }
    });
  });
}

function parseArgs() {
  const args = new Set(process.argv.slice(2));
  let runFrontend = !args.has("--backend-only");
  let runBackend = !args.has("--frontend-only");

  if (!runFrontend && !runBackend) {
    console.warn("Both --frontend-only and --backend-only were supplied; starting both services.");
    runFrontend = true;
    runBackend = true;
  }

  return {
    runFrontend,
    runBackend,
    skipDbCheck: args.has("--no-db-check"),
  };
}

function loadEnvFiles() {
  const candidateFiles = [
    path.join(projectRoot, ".env"),
    path.join(projectRoot, ".env.local"),
    path.join(projectRoot, "env"),
    path.join(projectRoot, "env.local"),
    path.join(backendDir, ".env"),
    path.join(backendDir, ".env.local"),
  ];

  const loaded = [];

  candidateFiles.forEach((filePath) => {
    if (!fs.existsSync(filePath)) {
      return;
    }

    const content = fs.readFileSync(filePath, "utf8");
    content
      .split(/\r?\n/)
      .map((line) => line.trim())
      .forEach((line) => {
        if (!line || line.startsWith("#")) {
          return;
        }

        const separatorIndex = line.indexOf("=");
        if (separatorIndex === -1) {
          return;
        }

        const key = line.slice(0, separatorIndex).trim();
        let value = line.slice(separatorIndex + 1).trim();
        if (!value.startsWith('"') && !value.startsWith("'")) {
          const commentIndex = value.indexOf(" #");
          if (commentIndex !== -1) {
            value = value.slice(0, commentIndex).trim();
          }
        }

        // Remove optional surrounding quotes
        if (
          (value.startsWith('"') && value.endsWith('"')) ||
          (value.startsWith("'") && value.endsWith("'"))
        ) {
          value = value.slice(1, -1);
        }

        if (!(key in process.env)) {
          process.env[key] = value;
        }
      });

    loaded.push(path.relative(projectRoot, filePath) || ".");
  });

  if (loaded.length > 0) {
    console.log(`• Loaded environment variables from ${loaded.join(", ")}`);
  }
}

function applyDatabaseEnvDefaults() {
  if (!process.env.DATABASE_URL) {
    const host = process.env.SUPABASE_DB_HOST;
    const port = process.env.SUPABASE_DB_PORT || "5432";
    const db = process.env.SUPABASE_DB_NAME || "postgres";
    if (host) {
      process.env.DATABASE_URL = `jdbc:postgresql://${host}:${port}/${db}`;
    }
  }

  if (!process.env.DATABASE_USERNAME && process.env.SUPABASE_DB_USERNAME) {
    process.env.DATABASE_USERNAME = process.env.SUPABASE_DB_USERNAME;
  }

  if (!process.env.DATABASE_PASSWORD && process.env.SUPABASE_DB_PASSWORD) {
    process.env.DATABASE_PASSWORD = process.env.SUPABASE_DB_PASSWORD;
  }
}

async function ensureFrontendDeps() {
  const nodeModulesPath = path.join(frontendDir, "node_modules");
  if (fs.existsSync(nodeModulesPath)) {
    console.log("• Frontend dependencies already installed (node_modules found).");
    return;
  }

  console.log("• Installing frontend dependencies (npm install)...");
  await runCommand(npmCmd, ["install"], frontendDir);
}

async function ensureBackendDeps() {
  const wrapperExists = fs.existsSync(path.join(backendDir, isWindows ? "gradlew.bat" : "gradlew"));
  if (!wrapperExists) {
    throw new Error("Gradle wrapper not found in backend/. Please regenerate it before running this script.");
  }

  const buildDir = path.join(backendDir, "build");
  if (fs.existsSync(buildDir)) {
    console.log("• Backend dependencies already prepared (build directory present).");
    return;
  }

  console.log("• Preparing backend dependencies (Gradle build)...");
  await runCommand(gradleCmd, ["build"], backendDir);
}

function parseDatabaseTarget() {
  const supabaseUrl =
    process.env.SUPABASE_DB_URL ||
    process.env.SUPABASE_POSTGRES_URL ||
    process.env.SUPABASE_CONNECTION_STRING;
  if (supabaseUrl) {
    try {
      const parsed = new URL(supabaseUrl);
      return {
        host: parsed.hostname || "localhost",
        port: parsed.port ? Number.parseInt(parsed.port, 10) : 5432,
      };
    } catch {
      // fall through to other checks
    }
  }

  const supabaseHost = process.env.SUPABASE_DB_HOST;
  const supabasePort = process.env.SUPABASE_DB_PORT;
  if (supabaseHost || supabasePort) {
    return {
      host: supabaseHost || "localhost",
      port: supabasePort ? Number.parseInt(supabasePort, 10) || 5432 : 5432,
    };
  }

  const directHost = process.env.DATABASE_HOST;
  const directPort = process.env.DATABASE_PORT;
  if (directHost || directPort) {
    return {
      host: directHost || "localhost",
      port: Number.parseInt(directPort ?? "5432", 10) || 5432,
    };
  }

  const rawUrl =
    process.env.DATABASE_URL ||
    process.env.POSTGRES_URL ||
    process.env.PG_URL ||
    process.env.SUPABASE_URL ||
    "jdbc:postgresql://localhost:5432/postgres";

  const sanitizedUrl = rawUrl.startsWith("jdbc:") ? rawUrl.slice(5) : rawUrl;

  try {
    const parsed = new URL(sanitizedUrl);
    return {
      host: parsed.hostname || "localhost",
      port: parsed.port ? Number.parseInt(parsed.port, 10) : 5432,
    };
  } catch {
    return { host: "localhost", port: 5432 };
  }
}

function checkDatabaseReachability({ host, port }, timeoutMs = 2000) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host, port }, () => {
      socket.end();
      resolve(true);
    });

    const onError = () => {
      socket.destroy();
      resolve(false);
    };

    socket.setTimeout(timeoutMs, onError);
    socket.on("error", onError);
  });
}

function startDevServers({ runBackend, runFrontend }) {
  const targets = [];
  console.log("\nStarting dev servers (Ctrl+C to stop all)...");

  if (runBackend) {
    targets.push({
      name: "Backend",
      proc: spawn(gradleCmd, ["bootRun"], {
        cwd: backendDir,
        stdio: "inherit",
        shell: false,
      }),
    });
  }

  if (runFrontend) {
    targets.push({
      name: "Frontend",
      proc: spawn(npmCmd, ["run", "dev"], {
        cwd: frontendDir,
        stdio: "inherit",
        shell: false,
      }),
    });
  }

  if (targets.length === 0) {
    console.log("Nothing to start. Check your flags.");
    return;
  }

  let shuttingDown = false;

  const cleanup = (exitCode = 0) => {
    if (shuttingDown) {
      return;
    }
    shuttingDown = true;

    const killProc = (proc) => {
      if (!proc || proc.killed) {
        return;
      }

      try {
        proc.kill("SIGINT");
      } catch {
        // Fallback for Windows if SIGINT is not supported
        try {
          proc.kill();
        } catch {
          /* ignore */
        }
      }
    };

    targets.forEach(({ proc }) => killProc(proc));

    setTimeout(() => {
      process.exit(exitCode);
    }, 250);
  };

  const handleChildExit =
    (name) =>
    (code = 0) => {
      console.log(`\n${name} exited with code ${code}.`);
      cleanup(code);
    };

  targets.forEach(({ name, proc }) => proc.on("exit", handleChildExit(name)));

  const signals = ["SIGINT", "SIGTERM", "SIGHUP"];
  signals.forEach((sig) => {
    process.on(sig, () => {
      console.log(`\nReceived ${sig}, shutting down...`);
      cleanup(0);
    });
  });
}

async function main() {
  console.log("Smart Attendance starter\n=========================");
  console.log("This script will ensure dependencies are installed and start both servers.\n");

  loadEnvFiles();
  applyDatabaseEnvDefaults();
  const { runFrontend, runBackend, skipDbCheck } = parseArgs();

  try {
    if (runFrontend) {
      await ensureFrontendDeps();
    }

    if (runBackend) {
      await ensureBackendDeps();
    }

    if (runBackend && !skipDbCheck) {
      const target = parseDatabaseTarget();
      console.log(`• Checking database availability at ${target.host}:${target.port}...`);
      const reachable = await checkDatabaseReachability(target);
      if (!reachable) {
        throw new Error(
          `Unable to reach Postgres at ${target.host}:${target.port}. Start your database or re-run with --frontend-only (or --no-db-check).`,
        );
      }
      console.log("• Database connection check passed.");
    }

    startDevServers({ runBackend, runFrontend });
  } catch (error) {
    console.error(`\n❌ Setup failed: ${error.message}`);
    process.exit(1);
  }
}

main();
