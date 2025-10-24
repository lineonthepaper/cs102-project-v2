-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.attendance_records (
  id bigint NOT NULL DEFAULT nextval('attendance_records_id_seq'::regclass),
  session_id bigint NOT NULL,
  user_id character varying NOT NULL,
  status character varying NOT NULL,
  checkin_time timestamp without time zone,
  checkout_time timestamp without time zone,
  notes character varying,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT attendance_records_pkey PRIMARY KEY (id),
  CONSTRAINT attendance_records_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT attendance_records_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.attendance_sessions(id)
);
CREATE TABLE public.attendance_sessions (
  id bigint NOT NULL DEFAULT nextval('attendance_sessions_id_seq'::regclass),
  section_id bigint NOT NULL,
  session_date date NOT NULL,
  scheduled_start_time time without time zone,
  scheduled_end_time time without time zone,
  status character varying NOT NULL DEFAULT 'SCHEDULED'::text,
  notes text,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT attendance_sessions_pkey PRIMARY KEY (id),
  CONSTRAINT attendance_sessions_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id)
);
CREATE TABLE public.courses (
  id bigint NOT NULL DEFAULT nextval('courses_id_seq'::regclass),
  code character varying NOT NULL UNIQUE,
  title character varying NOT NULL,
  description text,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT courses_pkey PRIMARY KEY (id)
);
CREATE TABLE public.section_assignments (
  id bigint NOT NULL DEFAULT nextval('section_assignments_id_seq'::regclass),
  section_id bigint NOT NULL,
  user_id character varying NOT NULL,
  role character varying NOT NULL CHECK (role::text = ANY (ARRAY['INSTRUCTOR'::text, 'TA'::text])),
  is_active boolean NOT NULL DEFAULT true,
  assigned_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT section_assignments_pkey PRIMARY KEY (id),
  CONSTRAINT section_assignments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT section_assignments_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id)
);
CREATE TABLE public.section_enrollments (
  id bigint NOT NULL DEFAULT nextval('section_enrollments_id_seq'::regclass),
  section_id bigint NOT NULL,
  user_id character varying NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  enrolled_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT section_enrollments_pkey PRIMARY KEY (id),
  CONSTRAINT section_enrollments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT section_enrollments_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id)
);
CREATE TABLE public.sections (
  id bigint NOT NULL DEFAULT nextval('sections_id_seq'::regclass),
  course_id bigint NOT NULL,
  section_code character varying NOT NULL,
  meeting_day integer CHECK (meeting_day >= 0 AND meeting_day <= 6),
  start_time time without time zone,
  end_time time without time zone,
  location character varying,
  max_capacity integer,
  day_of_week text,
  schedule text,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  year integer,
  semester integer CHECK (semester = ANY (ARRAY[1, 2])),
  CONSTRAINT sections_pkey PRIMARY KEY (id),
  CONSTRAINT sections_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id)
);
CREATE TABLE public.ta_assignments (
  id bigint NOT NULL DEFAULT nextval('ta_assignments_id_seq'::regclass),
  user_id character varying NOT NULL,
  course_id bigint,
  section_id bigint,
  assigned_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  is_active boolean NOT NULL DEFAULT true,
  updated_at timestamp without time zone,
  CONSTRAINT ta_assignments_pkey PRIMARY KEY (id),
  CONSTRAINT ta_assignments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT ta_assignments_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id),
  CONSTRAINT ta_assignments_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id)
);
CREATE TABLE public.users (
  id character varying NOT NULL,
  email character varying NOT NULL UNIQUE,
  first_name character varying NOT NULL,
  last_name character varying NOT NULL,
  auth_id character varying,
  is_student boolean NOT NULL DEFAULT false,
  is_instructor boolean NOT NULL DEFAULT false,
  is_ta boolean NOT NULL DEFAULT false,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  updated_at timestamp without time zone,
  CONSTRAINT users_pkey PRIMARY KEY (id)
);