Final Conclusion of Testing 26Oct

Deliverable 0: Others
Automatic Logout on Refresh (Bug): User is logged out automatically when refreshing the browser, losing the current session.

Deliverable 1: Student Enrollment & Management (Partially implemented)
Add optional phone & email fields to the Add Student & Supabase DB respectively
Add edit student feature
Add delete student feature
Need add this feature (Capture or import 10–20 face images per student via webcam or files (JPEG/PNG). Validate images (e.g., face detected, sufficient quality) before storing)
Webcam Capture (Not Implemented Yet): Implement and test the “Capture” button in the GUI to take images at 1-second intervals, perform face detection and preprocessing (grayscale conversion, lighting normalization, cropping), and display student thumbnails into the current searchable table.

Deliverable 2:Attendance Session Management  (Partially implemented)
Add Roster (Not Implemented Yet): Implement roster creation and management by selecting enrolled students; ensure no duplicate entries.
Open/Close Session (Not Implemented Yet): Enable session lifecycle control, opening, closing, and preventing deletion of active sessions.
Attendance Status Update (Incomplete): Add the “Late” status to complement existing “Present” and “Absent” options during manual marking.
Marking Method (Not Implemented Yet): Add a “Method” field in the roster to indicate whether attendance was marked manually or automatically.
Attendance Session Management (Partial Issue): Sometimes entering Course ID, Title, and Description does not allow the session to be saved, even when the inputs are valid.

Deliverable 3: Face Detection & Recognition  (Not Implemented yet)
Face Detection & Recognition (Not Implemented Yet): Implement live webcam feed capture, face detection, preprocessing (grayscale, normalization, resizing), and recognition against enrolled students. Display bounding boxes, Name/ID, and Confidence Score on the video feed; log unrecognized faces. To Implement In: the “Mark Attendance (Live Recognition)” section of the GUI. [Like add the (Live Recognition feature)


Deliverable 4: Marking Attendance (Automatic & Manual) (Partially Implemented)

Automatic Marking (Not Implemented Yet): Implement automatic marking where recognized students are marked as Present or Late based on confidence and timestamp, with cooldown timer to prevent duplicates. To Implement In: the “Live Recognition” page, integrated with the Roster Table
Manual Marking (Partial Implementation): Manual marking is available; add “Late” option and convert the current row of buttons into a dropdown for selecting Present/Absent/Late.(Doc says want dropdown, but maybe not too impt?)
Manual Override Logging: Ensure manual changes override auto-marked attendance and log the method as “Manual.” To Implement In: the “Session Management / Roster Table” page and “Live Recognition” page.


Deliverable 5: Reporting & Export (Not Implemented yet)
Reporting & Export (Not Implemented Yet): Implement session summary display in tables/charts and enable export as CSV, XLSX, or PDF with all relevant fields. To Implement In: the “Reports” tab/page of the GUI, with filterable views and export functionality.

Deliverable 6: Graphical User Interface (GUI) & Usability (Partially Implemented)

Live Recognition, Reports, Settings Tabs (Not Implemented Yet): Implement these sections in the sidebar/menu.
Live Recognition Tab (Not Implemented Yet): Show webcam feed with overlays, camera controls, and display exception messages (e.g., “No Camera”).
Tooltips (Not Implemented Yet): Add tooltips for buttons and input fields to help users understand their functions.


Deliverable 7: Configuration & Logging (Not Implemented yet)

Settings Tab (Not Implemented Yet): Create a page for users to adjust camera index, recognition threshold, timers, and database path; ensure changes are saved permanently.
Event Logging (Not Implemented Yet): Implement a centralized logging system to record actions (e.g., “Student S12345 enrolled”) with timestamps, working across Students, Sessions, Live Recognition, and Reports.



