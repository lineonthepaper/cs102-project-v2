-- Add jsonb column to store parsed face profiles (embeddings per image)
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS face_profiles jsonb;

-- Initialize nulls to empty array for consistency
UPDATE public.users
SET face_profiles = '[]'::jsonb
WHERE face_profiles IS NULL;


