import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://ihesechgwrelxkwizcke.supabase.co'
const supabaseKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImloZXNlY2hnd3JlbHhrd2l6Y2tlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5MDE0MjYsImV4cCI6MjA3NjQ3NzQyNn0.sw5As-HTAQsj87GG940QDU_QIi0gimGuRife8bgvqQ0'

export const supabase = createClient(supabaseUrl, supabaseKey)