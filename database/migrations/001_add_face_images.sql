-- Add face_images column to users table
-- This will store face image data as a JSONB array

ALTER TABLE public.users
ADD COLUMN face_images jsonb DEFAULT '[]'::jsonb;

-- Add a comment to describe the column
COMMENT ON COLUMN public.users.face_images IS 'JSON array storing base64 encoded face images for facial recognition (max 8 images)';