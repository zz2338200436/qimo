# Student CRUD Module Test Report

**Test Time:** 2026-05-11 11:04:18
**Test Account:** student1
**Test Environment:** http://localhost:8080
**Login Status:** Not logged in (some tests skipped)

---

## Test Overview

| Metric | Value |
|--------|-------|
| Total Tests | 46 |
| Passed | 0 |
| Failed | 4 |
| Warnings | 42 |
| **Pass Rate** | **0.0%** |

---

## Module Test Details

### Login Auth Module

**Passed:** 0/3

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Captcha Image | FAIL | HTTP 404 |
| Student Login | FAIL | Login failed |
| Get Current User Info | WARN | Not logged in, skip test |

### Dashboard Module

**Passed:** 0/1

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Student Performance | WARN | Not logged in, skip test |

### Courses Module

**Passed:** 0/5

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Course List | WARN | Not logged in, skip test |
| Get Course Detail | WARN | Not logged in, skip test |
| Search Courses | WARN | Not logged in, skip test |
| Filter Courses by Status | WARN | Not logged in, skip test |
| Filter Courses by Semester | WARN | Not logged in, skip test |

### Assignments Module

**Passed:** 0/5

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Assignment List | WARN | Not logged in, skip test |
| Get Assignment Detail | WARN | Not logged in, skip test |
| Submit Assignment | WARN | Not logged in, skip test |
| Filter Assignments by Course | WARN | Not logged in, skip test |
| Filter Submitted Assignments | WARN | Not logged in, skip test |

### Exams Module

**Passed:** 0/5

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Exam List | WARN | Not logged in, skip test |
| Get Exam Detail | WARN | Not logged in, skip test |
| Submit Exam | WARN | Not logged in, skip test |
| Filter Exams by Course | WARN | Not logged in, skip test |
| Filter Active Exams | WARN | Not logged in, skip test |

### Learning Stats Module

**Passed:** 0/7

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Learning Stats | WARN | Not logged in, skip test |
| Get Knowledge Points Mastery | WARN | Not logged in, skip test |
| Get Knowledge Point Detail | WARN | Not logged in, skip test |
| Get Score History | WARN | Not logged in, skip test |
| Get Study Time Distribution | WARN | Not logged in, skip test |
| Filter Stats by Semester | WARN | Not logged in, skip test |
| Filter Knowledge Points by Course | WARN | Not logged in, skip test |

### Notifications Module

**Passed:** 0/6

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Notification List | WARN | Not logged in, skip test |
| Get All Notifications | WARN | Not logged in, skip test |
| Get Unread Count | WARN | Not logged in, skip test |
| Mark Notification as Read | WARN | Not logged in, skip test |
| Mark All as Read | WARN | Not logged in, skip test |
| Delete All Read Notifications | WARN | Not logged in, skip test |

### AI Assistant Module

**Passed:** 0/3

| Test Item | Status | Details |
|-----------|--------|----------|
| AI Generate Questions | WARN | Not logged in, skip test |
| AI Generate Exam | WARN | Not logged in, skip test |
| AI Learning Suggestions | WARN | Not logged in, skip test |

### Settings Module

**Passed:** 0/8

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Profile | WARN | Not logged in, skip test |
| Update Profile | WARN | Not logged in, skip test |
| Get Notification Settings | WARN | Not logged in, skip test |
| Update Notification Settings | WARN | Not logged in, skip test |
| Get Privacy Settings | WARN | Not logged in, skip test |
| Update Privacy Settings | WARN | Not logged in, skip test |
| Change Password | WARN | Not logged in, skip test |
| Export Data | WARN | Not logged in, skip test |

### Early Warnings Module

**Passed:** 0/1

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Early Warnings | WARN | Not logged in, skip test |

### System Data Module

**Passed:** 0/2

| Test Item | Status | Details |
|-----------|--------|----------|
| Get Semesters | FAIL | HTTP 404 |
| Get Time Ranges | FAIL | HTTP 404 |

---

## Test Summary

| Module | Pass Rate | Status |
|--------|-----------|--------|
| Login Auth Module | 0% (0/3) | Abnormal |
| Dashboard Module | 0% (0/1) | Abnormal |
| Courses Module | 0% (0/5) | Abnormal |
| Assignments Module | 0% (0/5) | Abnormal |
| Exams Module | 0% (0/5) | Abnormal |
| Learning Stats Module | 0% (0/7) | Abnormal |
| Notifications Module | 0% (0/6) | Abnormal |
| AI Assistant Module | 0% (0/3) | Abnormal |
| Settings Module | 0% (0/8) | Abnormal |
| Early Warnings Module | 0% (0/1) | Abnormal |
| System Data Module | 0% (0/2) | Abnormal |

---

## Test Notes

1. **Login Test**: Automated login via Redis captcha retrieval
2. **API Test**: Test all student-side API CRUD functions
3. **Filter Test**: Test filtering, searching, and pagination for each module
4. **Data Operations**: Test data create, read, update, delete operations
5. **Security Verification**: Ensure authentication and authorization work correctly

---

*Report generated: 2026-05-11 11:04:18*
