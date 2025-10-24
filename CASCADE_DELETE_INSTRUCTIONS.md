# CASCADE DELETE Migration Instructions

## Problem

Currently, you **cannot delete students** if they have:
- Attendance records
- Section enrollments
- Section assignments (if they're instructors/TAs)
- TA assignments

This is because the database foreign keys don't have `ON DELETE CASCADE` configured.

---

## Solution

Run the migration script to add CASCADE DELETE to all foreign keys referencing the `users` table.

---

## How to Apply (Supabase SQL Editor)

### Step 1: Open Supabase SQL Editor

1. Go to your Supabase project: https://supabase.com/dashboard/project/ihesechgwrelxkwizcke
2. Click on **SQL Editor** in the left sidebar
3. Click **New Query**

### Step 2: Copy and Paste the SQL

Copy the entire contents of `database/add-cascade-delete.sql` and paste it into the SQL editor.

### Step 3: Run the Migration

Click **Run** or press `Ctrl+Enter` (Windows/Linux) or `Cmd+Enter` (Mac)

### Step 4: Verify

You should see a success message. The migration will:
- Drop the old foreign key constraints
- Add new foreign key constraints WITH CASCADE DELETE

---

## SQL Script Location

```
database/add-cascade-delete.sql
```

---

## What This Does

After applying this migration, when you delete a user (student/instructor/TA):

✅ **Automatically deletes:**
- All their attendance records
- All their section enrollments
- All their section assignments (instructor/TA assignments)
- All their TA assignments

✅ **Result:**
- No more "foreign key constraint violation" errors
- Clean deletion of users and their related data
- Better data integrity

---

## Testing After Migration

### Test 1: Delete a Student
```sql
-- Create test student
INSERT INTO users (id, email, first_name, last_name, is_student, enabled)
VALUES ('TEST001', 'test@delete.com', 'Test', 'Delete', true, true);

-- Add enrollment
INSERT INTO section_enrollments (section_id, user_id, is_active)
VALUES (20, 'TEST001', true);

-- Now delete the student (should work!)
DELETE FROM users WHERE id = 'TEST001';

-- Verify enrollment is also deleted
SELECT * FROM section_enrollments WHERE user_id = 'TEST001';
-- Should return 0 rows
```

### Test 2: Delete from Frontend
1. Go to Students page
2. Click delete on any student
3. Should delete successfully without errors

---

## Rollback (If Needed)

If you need to rollback (remove CASCADE DELETE):

```sql
-- Drop CASCADE constraints
ALTER TABLE public.attendance_records 
  DROP CONSTRAINT attendance_records_user_id_fkey;
ALTER TABLE public.section_enrollments 
  DROP CONSTRAINT section_enrollments_user_id_fkey;
ALTER TABLE public.section_assignments 
  DROP CONSTRAINT section_assignments_user_id_fkey;
ALTER TABLE public.ta_assignments 
  DROP CONSTRAINT ta_assignments_user_id_fkey;

-- Add back without CASCADE (original)
ALTER TABLE public.attendance_records 
  ADD CONSTRAINT attendance_records_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE public.section_enrollments 
  ADD CONSTRAINT section_enrollments_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE public.section_assignments 
  ADD CONSTRAINT section_assignments_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE public.ta_assignments 
  ADD CONSTRAINT ta_assignments_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES public.users(id);
```

---

## Verification Query

After running the migration, verify CASCADE is enabled:

```sql
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
```

Expected output:
```
table_name           | constraint_name                      | delete_rule
---------------------|--------------------------------------|-------------
attendance_records   | attendance_records_user_id_fkey      | CASCADE
section_assignments  | section_assignments_user_id_fkey     | CASCADE
section_enrollments  | section_enrollments_user_id_fkey     | CASCADE
ta_assignments       | ta_assignments_user_id_fkey          | CASCADE
```

---

## Notes

- ✅ Safe to run (drops and recreates constraints)
- ✅ No data loss (only changes constraint behavior)
- ✅ Can be rolled back if needed
- ✅ Production-ready
- ⚠️ Make sure to backup your database before running (Supabase auto-backups, but be safe!)

---

**After running this migration, student deletion will work seamlessly!** 🎉

