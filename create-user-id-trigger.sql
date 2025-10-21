-- Function to generate user IDs based on role
CREATE OR REPLACE FUNCTION generate_user_id()
RETURNS TRIGGER AS $$
DECLARE
    prefix TEXT;
    max_num INTEGER;
    new_id TEXT;
BEGIN
    -- If ID is already provided, don't override it
    IF NEW.id IS NOT NULL AND NEW.id != '' THEN
        RETURN NEW;
    END IF;

    -- Determine prefix based on role
    IF NEW.is_instructor THEN
        prefix := 'I';
    ELSIF NEW.is_ta THEN
        prefix := 'T';
    ELSIF NEW.is_student THEN
        prefix := 'S';
    ELSE
        -- Default to S for students
        prefix := 'S';
    END IF;

    -- Get the maximum number for this prefix
    SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM 2) AS INTEGER)), 0)
    INTO max_num
    FROM users
    WHERE id ~ ('^' || prefix || '[0-9]+$');

    -- Generate new ID with zero-padding
    new_id := prefix || LPAD((max_num + 1)::TEXT, 7, '0');
    
    NEW.id := new_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if it exists
DROP TRIGGER IF EXISTS user_id_trigger ON users;

-- Create trigger
CREATE TRIGGER user_id_trigger
    BEFORE INSERT ON users
    FOR EACH ROW
    EXECUTE FUNCTION generate_user_id();

