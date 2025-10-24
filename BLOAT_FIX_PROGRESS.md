# Bloat Fix in Progress

## ✅ Completed
1. MapStruct added to build.gradle.kts
2. EntityMapper interface created (83 lines)
3. CourseService refactored (57→54 lines)
4. SectionService refactored (122→67 lines, -55 lines!)

## ⏳ In Progress
- Updating remaining services (Student, TA, Instructor, Attendance, Auth)
- Removing all duplicated dayNumberToName/dayNameToNumber methods

## 📊 Estimated Savings
- Target: -465 lines
- Current: ~60 lines saved
- Remaining: ~405 lines to remove

## Next
Update the 5 big services that have the most bloat.
