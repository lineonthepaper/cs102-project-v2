-- Migration Script: Add CASCADE DELETE to Foreign Keys
-- This allows deleting users (students/instructors/TAs) and their related records

-- 1. Drop existing foreign key constraints
ALTER TABLE public.attendance_records 
  DROP CONSTRAINT IF EXISTS attendance_records_user_id_fkey;

ALTER TABLE public.section_enrollments 
  DROP CONSTRAINT IF EXISTS section_enrollments_user_id_fkey;

ALTER TABLE public.section_assignments 
  DROP CONSTRAINT IF EXISTS section_assignments_user_id_fkey;

ALTER TABLE public.ta_assignments 
  DROP CONSTRAINT IF EXISTS ta_assignments_user_id_fkey;

-- 2. Add foreign key constraints WITH CASCADE DELETE
ALTER TABLE public.attendance_records 
  ADD CONSTRAINT attendance_records_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES public.users(id) 
  ON DELETE CASCADE;

ALTER TABLE public.section_enrollments 
  ADD CONSTRAINT section_enrollments_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES public.users(id) 
  ON DELETE CASCADE;

ALTER TABLE public.section_assignments 
  ADD CONSTRAINT section_assignments_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES public.users(id) 
  ON DELETE CASCADE;

ALTER TABLE public.ta_assignments 
  ADD CONSTRAINT ta_assignments_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES public.users(id) 
  ON DELETE CASCADE;

-- Verification query (optional - run to check)
SELECT 
  tc.table_name, 
  tc.constraint_name, 
  rc.delete_rule
FROM information_schema.table_constraints tc
JOIN information_schema.referential_constraints rc 
  ON tc.constraint_name = rc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema = 'public'
  AND rc.delete_rule = 'CASCADE'
ORDER BY tc.table_name;

