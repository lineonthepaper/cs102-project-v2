	•	Supabase Auth (email/password) + role-based access (admin/instructor/TA)
	•	Student CRUD (ID, Name, Class, optional Email/Phone)
	•	Face data capture/import (webcam & file upload; 10–20 images/student)
	•	Image validation (face detected, quality checks)
	•	Face data storage (images/embeddings linked to student)
	•	Session CRUD (Course, Date, Start/End, Location)
	•	Roster management (select enrolled students; no duplicates)
	•	Open/close sessions; block deletion while active
	•	Live webcam feed (WebRTC) with overlays (boxes, Name/ID, confidence)
	•	Face detection (e.g., Haar/YOLO) + preprocessing (grayscale/normalize/resize)
	•	Recognition pipeline (configurable threshold; ≥8 FPS target)
	•	Auto attendance marking (Present/Late with timestamp & “Auto” method)
	•	Late threshold setting (e.g., 15 min)
	•	Duplicate prevention (cooldown timer)
	•	Low-confidence confirmation prompt
	•	Manual override (status dropdown + notes; logs “Manual”)
	•	Auto-mark remaining as Absent on session close
	•	Multi-face handling (prioritize higher confidence)
	•	Reporting UI (present/absent/late summaries; filters)
	•	Export CSV/XLSX/PDF (ID, Name, Status, Timestamp, Confidence, Method, Notes)
	•	Optional email reports
	•	Tabs/navigation: Students, Sessions, Live Recognition, Reports, Settings
	•	Searchable student table with thumbnails
	•	Camera controls + “No Camera” handling
	•	Accessibility (keyboard, tooltips) & responsive UI (no freezes; background threads)
	•	Settings panel (camera index, thresholds, timers, DB config)
	•	Externalized config (properties/env)
	•	Logging/audit trail (events, overrides)
	•	Non-functional targets (≥8 FPS; <300ms/face; ≥70% accuracy in good lighting; ≤5 clicks for key actions)
	•	Bonus: multi-face tracking, liveness checks, mask-aware recognition, CSV roster import, analytics dashboard, cloud sync, reopen/archive with audit

￼