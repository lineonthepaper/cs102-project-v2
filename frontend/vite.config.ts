import { defineConfig, loadEnv } from 'vite'
import path from 'node:path'
import react from '@vitejs/plugin-react'
import { createClient } from '@supabase/supabase-js'

export default defineConfig(({ mode }) => {
  const projectRootEnv = loadEnv(mode, process.cwd(), '')
  const workspaceRootEnv = loadEnv(mode, path.resolve(process.cwd(), '..'), '')
  const env = { ...workspaceRootEnv, ...projectRootEnv }

  const supabaseUrl =
    env.VITE_SUPABASE_URL ||
    env.APP_SUPABASE_URL ||
    env.SUPABASE_URL ||
    'https://ihesechgwrelxkwizcke.supabase.co'
  const serviceRoleKey = 
    env.SUPABASE_SERVICE_ROLE_KEY || 
    env.APP_SUPABASE_SERVICE_KEY ||
    ''

  const supabaseAdmin = serviceRoleKey
    ? createClient(supabaseUrl, serviceRoleKey, {
        auth: {
          autoRefreshToken: false,
          persistSession: false
        }
      })
    : null

  return {
    plugins: [
      react(),
      {
        name: 'ta-auth-backend',
        configureServer(server) {
          server.middlewares.use(async (req, res, next) => {
            if (!req.url) {
              return next()
            }

            const url = new URL(req.url, 'http://localhost')
            const match = url.pathname.match(/^\/api\/admin\/teaching-assistants\/([^/]+)\/?$/)

            if (!match) {
              return next()
            }

            if (req.method !== 'DELETE') {
              res.statusCode = 405
              res.setHeader('Content-Type', 'application/json')
              res.end(JSON.stringify({ error: 'Method not allowed' }))
              return
            }

            if (!supabaseAdmin) {
              res.statusCode = 500
              res.setHeader('Content-Type', 'application/json')
              res.end(JSON.stringify({ error: 'Supabase service role key is not configured. Set SUPABASE_SERVICE_ROLE_KEY in your .env.local file.' }))
              return
            }

            const authId = decodeURIComponent(match[1])

            try {
              const { error } = await supabaseAdmin.auth.admin.deleteUser(authId)

              if (error) {
                res.statusCode = 502
                res.setHeader('Content-Type', 'application/json')
                res.end(JSON.stringify({ error: 'Failed to delete user from Supabase Auth', details: error.message }))
                return
              }

              res.statusCode = 200
              res.setHeader('Content-Type', 'application/json')
              res.end(
                JSON.stringify({
                  success: true,
                  authId,
                  message: 'Teaching Assistant removed from Supabase Auth'
                })
              )
            } catch (err) {
              res.statusCode = 502
              res.setHeader('Content-Type', 'application/json')
              res.end(
                JSON.stringify({
                  error: 'Unexpected error while contacting Supabase Auth',
                  details: err instanceof Error ? err.message : String(err)
                })
              )
            }
          })
        }
      }
    ]
  }
})
