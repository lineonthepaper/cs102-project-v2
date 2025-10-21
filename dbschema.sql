-- Actionable schema for local Postgres/Supabase

CREATE SCHEMA IF NOT EXISTS public;

-- Sequences required by nextval() defaults
CREATE SEQUENCE IF NOT EXISTS attendance_records_id_seq;
CREATE SEQUENCE IF NOT EXISTS attendance_sessions_id_seq;
CREATE SEQUENCE IF NOT EXISTS courses_id_seq;
CREATE SEQUENCE IF NOT EXISTS section_assignments_id_seq;
CREATE SEQUENCE IF NOT EXISTS section_enrollments_id_seq;
CREATE SEQUENCE IF NOT EXISTS sections_id_seq;
CREATE SEQUENCE IF NOT EXISTS ta_assignments_id_seq;

-- Sequences for generating user identifiers like S0000001 / I0000001
CREATE SEQUENCE IF NOT EXISTS users_student_seq;
CREATE SEQUENCE IF NOT EXISTS users_instructor_seq;

-- Users must be created first (referenced by many tables)
CREATE TABLE IF NOT EXISTS public.users (
  id text NOT NULL,
  email character varying NOT NULL UNIQUE,
  first_name character varying NOT NULL,
  last_name character varying NOT NULL,
  auth_id text,
  is_student boolean NOT NULL DEFAULT false,
  is_instructor boolean NOT NULL DEFAULT false,
  is_ta boolean NOT NULL DEFAULT false,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT users_pkey PRIMARY KEY (id)
);

-- Generate user.id when not provided (students and instructors)
CREATE OR REPLACE FUNCTION public.users_generate_id()
RETURNS trigger AS $$
BEGIN
  IF NEW.id IS NULL OR NEW.id = '' THEN
    IF COALESCE(NEW.is_student, FALSE) THEN
      NEW.id := 'S' || lpad(nextval('users_student_seq')::text, 7, '0');
    ELSIF COALESCE(NEW.is_instructor, FALSE) THEN
      NEW.id := 'I' || lpad(nextval('users_instructor_seq')::text, 7, '0');
    ELSE
      NEW.id := 'U' || lpad(nextval('users_student_seq')::text, 7, '0');
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS users_generate_id_before_insert ON public.users;
CREATE TRIGGER users_generate_id_before_insert
BEFORE INSERT ON public.users
FOR EACH ROW
EXECUTE FUNCTION public.users_generate_id();

-- Courses
CREATE TABLE IF NOT EXISTS public.courses (
  id bigint NOT NULL DEFAULT nextval('courses_id_seq'::regclass),
  code text NOT NULL UNIQUE,
  title text NOT NULL,
  description text,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT courses_pkey PRIMARY KEY (id)
);

-- Sections (depends on courses)
CREATE TABLE IF NOT EXISTS public.sections (
  id bigint NOT NULL DEFAULT nextval('sections_id_seq'::regclass),
  course_id bigint NOT NULL,
  section_code text NOT NULL UNIQUE,
  term_label text,
  meeting_day smallint CHECK (meeting_day >= 0 AND meeting_day <= 6),
  start_time time without time zone,
  end_time time without time zone,
  location text,
  max_capacity integer,
  day_of_week text,
  schedule text,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT sections_pkey PRIMARY KEY (id),
  CONSTRAINT sections_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id)
);

-- Attendance sessions (depends on sections)
CREATE TABLE IF NOT EXISTS public.attendance_sessions (
  id bigint NOT NULL DEFAULT nextval('attendance_sessions_id_seq'::regclass),
  section_id bigint NOT NULL,
  session_date date NOT NULL,
  scheduled_start_time time without time zone,
  scheduled_end_time time without time zone,
  status text NOT NULL DEFAULT 'SCHEDULED'::text,
  notes text,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT attendance_sessions_pkey PRIMARY KEY (id),
  CONSTRAINT attendance_sessions_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id)
);

-- Attendance records (depends on attendance_sessions and users)
CREATE TABLE IF NOT EXISTS public.attendance_records (
  id bigint NOT NULL DEFAULT nextval('attendance_records_id_seq'::regclass),
  session_id bigint NOT NULL,
  user_id text NOT NULL,
  status text NOT NULL,
  checkin_time timestamp without time zone,
  checkout_time timestamp without time zone,
  notes text,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT attendance_records_pkey PRIMARY KEY (id),
  CONSTRAINT attendance_records_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.attendance_sessions(id),
  CONSTRAINT attendance_records_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Section assignments (depends on sections and users)
CREATE TABLE IF NOT EXISTS public.section_assignments (
  id bigint NOT NULL DEFAULT nextval('section_assignments_id_seq'::regclass),
  section_id bigint NOT NULL,
  user_id text NOT NULL,
  role text NOT NULL CHECK (role = ANY (ARRAY['INSTRUCTOR'::text, 'TA'::text])),
  is_active boolean NOT NULL DEFAULT true,
  assigned_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT section_assignments_pkey PRIMARY KEY (id),
  CONSTRAINT section_assignments_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id),
  CONSTRAINT section_assignments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Section enrollments (depends on sections and users)
CREATE TABLE IF NOT EXISTS public.section_enrollments (
  id bigint NOT NULL DEFAULT nextval('section_enrollments_id_seq'::regclass),
  section_id bigint NOT NULL,
  user_id text NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  enrolled_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT section_enrollments_pkey PRIMARY KEY (id),
  CONSTRAINT section_enrollments_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id),
  CONSTRAINT section_enrollments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- TA assignments (depends on sections, courses, and users)
CREATE TABLE IF NOT EXISTS public.ta_assignments (
  id bigint NOT NULL DEFAULT nextval('ta_assignments_id_seq'::regclass),
  user_id text NOT NULL,
  course_id bigint,
  section_id bigint,
  assigned_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  is_active boolean NOT NULL DEFAULT true,
  updated_at timestamp without time zone,
  CONSTRAINT ta_assignments_pkey PRIMARY KEY (id),
  CONSTRAINT ta_assignments_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id),
  CONSTRAINT ta_assignments_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id),
  CONSTRAINT ta_assignments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);