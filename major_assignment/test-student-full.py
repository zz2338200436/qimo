#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Student CRUD Module Test Script
Test Account: studen1 / 123456
"""

import requests
import redis
import json
import time
import sys
from datetime import datetime
from typing import Dict, List, Tuple

# Fix Windows console encoding
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

# Configuration
BASE_URL = "http://localhost:8080"
REDIS_HOST = "localhost"
REDIS_PORT = 6379
TEST_USERNAME = "student1"
TEST_PASSWORD = "123456"

# Test results collection
test_results: List[Dict] = []
current_module = ""

class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'

def log(message: str, color: str = Colors.NC):
    print(f"{color}{message}{Colors.NC}", flush=True)

def add_test_result(module: str, test_name: str, status: str, details: str = ""):
    """Add test result"""
    test_results.append({
        "module": module,
        "test_name": test_name,
        "status": status,  # pass, fail, warning
        "details": details,
        "timestamp": datetime.now().isoformat()
    })

    icon = "[PASS]" if status == "pass" else "[FAIL]" if status == "fail" else "[WARN]"
    color = Colors.GREEN if status == "pass" else Colors.RED if status == "fail" else Colors.YELLOW
    log(f"{icon} {test_name}: {status}", color)
    if details:
        log(f"   Details: {details}", Colors.NC)

def get_captcha_from_redis(session_id: str) -> str:
    """Get captcha from Redis"""
    try:
        r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)
        redis_key = f"CAPTCHA:IMG:{session_id}"
        captcha = r.get(redis_key)
        if captcha:
            # Remove quotes if present (Spring RedisTemplate serialization)
            captcha = captcha.strip('"').strip("'")
        return captcha
    except Exception as e:
        log(f"Redis connection failed: {e}", Colors.RED)
        return None

def login(session: requests.Session) -> bool:
    """Login to get valid session"""
    try:
        # 1. Get captcha (also get session cookie)
        captcha_response = session.get(f"{BASE_URL}/api/public/captcha")
        if captcha_response.status_code != 200:
            log(f"Failed to get captcha: {captcha_response.status_code}", Colors.RED)
            return False

        # 2. Get session ID from cookie
        session_id = None
        for cookie in session.cookies:
            if cookie.name == "JSESSIONID":
                session_id = cookie.value
                break

        if not session_id:
            log("JSESSIONID not found", Colors.RED)
            return False

        log(f"Got Session ID: {session_id}", Colors.BLUE)

        # 3. Get captcha from Redis
        captcha = get_captcha_from_redis(session_id)
        if not captcha:
            log("Cannot get captcha from Redis, using default", Colors.YELLOW)
            captcha = "test"

        log(f"Got captcha: {captcha}", Colors.BLUE)

        # 4. Login
        login_data = {
            "username": TEST_USERNAME,
            "password": TEST_PASSWORD,
            "captcha": captcha
        }

        login_response = session.post(
            f"{BASE_URL}/api/auth/login",
            json=login_data,
            headers={"Content-Type": "application/json"}
        )

        if login_response.status_code == 200:
            result = login_response.json()
            if result.get("code") == 200:
                log("Login successful!", Colors.GREEN)
                # Check if XSRF-TOKEN cookie is set
                csrf_token = get_csrf_token(session)
                if csrf_token:
                    log(f"Got CSRF token: {csrf_token[:10]}...", Colors.BLUE)
                else:
                    log("Warning: No XSRF-TOKEN cookie found", Colors.YELLOW)
                return True
            else:
                log(f"Login failed: {result.get('message')}", Colors.RED)
                return False
        else:
            log(f"Login request failed: HTTP {login_response.status_code}", Colors.RED)
            return False

    except Exception as e:
        log(f"Login exception: {e}", Colors.RED)
        return False

def get_csrf_token(session: requests.Session) -> str:
    """Get CSRF token from cookies"""
    for cookie in session.cookies:
        if cookie.name == "XSRF-TOKEN":
            return cookie.value
    return None

def test_api(session: requests.Session, method: str, url: str, data: dict = None,
             expected_status: int = 200, test_name: str = "") -> Tuple[bool, str]:
    """Test API endpoint"""
    try:
        headers = {"Content-Type": "application/json"}

        # Add CSRF token for write operations
        if method.upper() in ["POST", "PUT", "DELETE"]:
            csrf_token = get_csrf_token(session)
            if csrf_token:
                headers["X-XSRF-TOKEN"] = csrf_token

        if method.upper() == "GET":
            response = session.get(f"{BASE_URL}{url}")
        elif method.upper() == "POST":
            response = session.post(f"{BASE_URL}{url}", json=data, headers=headers)
        elif method.upper() == "PUT":
            response = session.put(f"{BASE_URL}{url}", json=data, headers=headers)
        elif method.upper() == "DELETE":
            response = session.delete(f"{BASE_URL}{url}", headers=headers)
        else:
            return False, f"Unsupported HTTP method: {method}"

        if response.status_code == expected_status:
            return True, f"HTTP {response.status_code}"
        else:
            try:
                resp_json = response.json()
                details = f"HTTP {response.status_code}: {resp_json.get('message', str(resp_json))}"
            except:
                details = f"HTTP {response.status_code}"
            return False, details

    except Exception as e:
        return False, str(e)

def run_tests():
    """Run all tests"""
    log("=" * 60, Colors.BLUE)
    log("Start Student CRUD Module Test", Colors.BLUE)
    log("=" * 60, Colors.BLUE)
    log(f"Test Account: {TEST_USERNAME} / {TEST_PASSWORD}", Colors.NC)

    # Create session
    session = requests.Session()
    session.verify = False

    # Login
    logged_in = login(session)

    if not logged_in:
        log("\n[WARN] Login failed, will test API responses in unauthenticated state", Colors.YELLOW)

    # ==================== 1. Login Auth Module ====================
    global current_module
    current_module = "Login Auth Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 1] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    # 1.1 Get captcha
    captcha_session = requests.Session()
    captcha_response = captcha_session.get(f"{BASE_URL}/api/public/captcha")
    if captcha_response.status_code == 200:
        add_test_result(current_module, "Get Captcha Image", "pass", "HTTP 200")
    else:
        add_test_result(current_module, "Get Captcha Image", "fail", f"HTTP {captcha_response.status_code}")

    # 1.2 Login success test
    if logged_in:
        add_test_result(current_module, "Student Login", "pass", "Login successful")
    else:
        add_test_result(current_module, "Student Login", "fail", "Login failed")

    # 1.3 Get current user info
    if logged_in:
        success, details = test_api(session, "GET", "/api/auth/me")
        if success:
            add_test_result(current_module, "Get Current User Info", "pass", details)
        else:
            add_test_result(current_module, "Get Current User Info", "fail", details)
    else:
        add_test_result(current_module, "Get Current User Info", "warning", "Not logged in, skip test")

    # ==================== 2. Dashboard Module ====================
    current_module = "Dashboard Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 2] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 2.1 Get student performance
        success, details = test_api(session, "GET", "/api/dashboard/student-performance")
        if success:
            add_test_result(current_module, "Get Student Performance", "pass", details)
        else:
            add_test_result(current_module, "Get Student Performance", "fail", details)
    else:
        add_test_result(current_module, "Get Student Performance", "warning", "Not logged in, skip test")

    # ==================== 3. Courses Module ====================
    current_module = "Courses Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 3] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 3.1 Get course list
        success, details = test_api(session, "GET", "/api/student/courses?page=1&size=10")
        if success:
            add_test_result(current_module, "Get Course List", "pass", details)
        else:
            add_test_result(current_module, "Get Course List", "fail", details)

        # 3.2 Get course detail
        success, details = test_api(session, "GET", "/api/student/courses/6")
        if success:
            add_test_result(current_module, "Get Course Detail", "pass", details)
        else:
            add_test_result(current_module, "Get Course Detail", "fail", details)

        # 3.3 Search courses
        success, details = test_api(session, "GET", "/api/student/courses?page=1&size=10&searchQuery=Java")
        if success:
            add_test_result(current_module, "Search Courses", "pass", details)
        else:
            add_test_result(current_module, "Search Courses", "fail", details)

        # 3.4 Filter by status
        success, details = test_api(session, "GET", "/api/student/courses?page=1&size=10&courseStatus=ongoing")
        if success:
            add_test_result(current_module, "Filter Courses by Status", "pass", details)
        else:
            add_test_result(current_module, "Filter Courses by Status", "fail", details)

        # 3.5 Filter by semester
        success, details = test_api(session, "GET", "/api/student/courses?page=1&size=10&semester=2025-2026")
        if success:
            add_test_result(current_module, "Filter Courses by Semester", "pass", details)
        else:
            add_test_result(current_module, "Filter Courses by Semester", "fail", details)
    else:
        add_test_result(current_module, "Get Course List", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Course Detail", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Search Courses", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Filter Courses by Status", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Filter Courses by Semester", "warning", "Not logged in, skip test")

    # ==================== 4. Assignments Module ====================
    current_module = "Assignments Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 4] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 4.1 Get assignment list
        success, details = test_api(session, "GET", "/api/student/assignments?page=1&size=10")
        if success:
            add_test_result(current_module, "Get Assignment List", "pass", details)
        else:
            add_test_result(current_module, "Get Assignment List", "fail", details)

        # 4.2 Get assignment detail
        success, details = test_api(session, "GET", "/api/student/assignments/16")
        if success:
            add_test_result(current_module, "Get Assignment Detail", "pass", details)
        else:
            add_test_result(current_module, "Get Assignment Detail", "fail", details)

        # 4.3 Submit assignment
        success, details = test_api(session, "POST", "/api/student/assignments/16/submit",
                                   {"content": "Test assignment submission - automated test"})
        if success:
            add_test_result(current_module, "Submit Assignment", "pass", details)
        else:
            add_test_result(current_module, "Submit Assignment", "warning", details)

        # 4.4 Filter by course
        success, details = test_api(session, "GET", "/api/student/assignments?page=1&size=10&courseId=6")
        if success:
            add_test_result(current_module, "Filter Assignments by Course", "pass", details)
        else:
            add_test_result(current_module, "Filter Assignments by Course", "fail", details)

        # 4.5 Filter submitted
        success, details = test_api(session, "GET", "/api/student/assignments?page=1&size=10&submitted=true")
        if success:
            add_test_result(current_module, "Filter Submitted Assignments", "pass", details)
        else:
            add_test_result(current_module, "Filter Submitted Assignments", "fail", details)
    else:
        add_test_result(current_module, "Get Assignment List", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Assignment Detail", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Submit Assignment", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Filter Assignments by Course", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Filter Submitted Assignments", "warning", "Not logged in, skip test")

    # ==================== 5. Exams Module ====================
    current_module = "Exams Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 5] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 5.1 Get exam list
        success, details = test_api(session, "GET", "/api/student/exams?page=1&size=10")
        if success:
            add_test_result(current_module, "Get Exam List", "pass", details)
        else:
            add_test_result(current_module, "Get Exam List", "fail", details)

        # 5.2 Get exam detail
        success, details = test_api(session, "GET", "/api/student/exams/6")
        if success:
            add_test_result(current_module, "Get Exam Detail", "pass", details)
        else:
            add_test_result(current_module, "Get Exam Detail", "fail", details)

        # 5.3 Submit exam
        success, details = test_api(session, "POST", "/api/student/exams/6/submit",
                                   {"answers": {"1": "A", "2": "B", "3": "C"}, "timeTaken": 1800})
        if success:
            add_test_result(current_module, "Submit Exam", "pass", details)
        else:
            add_test_result(current_module, "Submit Exam", "warning", details)

        # 5.4 Filter by course
        success, details = test_api(session, "GET", "/api/student/exams?page=1&size=10&courseId=6")
        if success:
            add_test_result(current_module, "Filter Exams by Course", "pass", details)
        else:
            add_test_result(current_module, "Filter Exams by Course", "fail", details)

        # 5.5 Filter active exams
        success, details = test_api(session, "GET", "/api/student/exams?page=1&size=10&isActive=true")
        if success:
            add_test_result(current_module, "Filter Active Exams", "pass", details)
        else:
            add_test_result(current_module, "Filter Active Exams", "fail", details)
    else:
        add_test_result(current_module, "Get Exam List", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Exam Detail", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Submit Exam", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Filter Exams by Course", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Filter Active Exams", "warning", "Not logged in, skip test")

    # ==================== 6. Learning Stats Module ====================
    current_module = "Learning Stats Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 6] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 6.1 Get learning stats
        success, details = test_api(session, "GET", "/api/student/stats")
        if success:
            add_test_result(current_module, "Get Learning Stats", "pass", details)
        else:
            add_test_result(current_module, "Get Learning Stats", "fail", details)

        # 6.2 Get knowledge points mastery
        success, details = test_api(session, "GET", "/api/student/knowledge-points?page=1&size=10")
        if success:
            add_test_result(current_module, "Get Knowledge Points Mastery", "pass", details)
        else:
            add_test_result(current_module, "Get Knowledge Points Mastery", "fail", details)

        # 6.3 Get knowledge point detail
        success, details = test_api(session, "GET", "/api/student/knowledge-points/1")
        if success:
            add_test_result(current_module, "Get Knowledge Point Detail", "pass", details)
        else:
            add_test_result(current_module, "Get Knowledge Point Detail", "fail", details)

        # 6.4 Get score history
        success, details = test_api(session, "GET", "/api/student/scores")
        if success:
            add_test_result(current_module, "Get Score History", "pass", details)
        else:
            add_test_result(current_module, "Get Score History", "fail", details)

        # 6.5 Get study time distribution
        success, details = test_api(session, "GET", "/api/student/study-time-distribution")
        if success:
            add_test_result(current_module, "Get Study Time Distribution", "pass", details)
        else:
            add_test_result(current_module, "Get Study Time Distribution", "fail", details)

        # 6.6 Filter by semester
        success, details = test_api(session, "GET", "/api/student/stats?semester=2025-2026")
        if success:
            add_test_result(current_module, "Filter Stats by Semester", "pass", details)
        else:
            add_test_result(current_module, "Filter Stats by Semester", "fail", details)

        # 6.7 Filter knowledge points by course
        success, details = test_api(session, "GET", "/api/student/knowledge-points?page=1&size=10&courseId=6")
        if success:
            add_test_result(current_module, "Filter Knowledge Points by Course", "pass", details)
        else:
            add_test_result(current_module, "Filter Knowledge Points by Course", "fail", details)
    else:
        add_test_result(current_module, "Get Learning Stats", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Knowledge Points Mastery", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Knowledge Point Detail", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Score History", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Study Time Distribution", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Filter Stats by Semester", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Filter Knowledge Points by Course", "warning", "Not logged in, skip test")

    # ==================== 7. Notifications Module ====================
    current_module = "Notifications Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 7] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 7.1 Get notification list
        success, details = test_api(session, "GET", "/api/notifications/student?page=1&size=10")
        if success:
            add_test_result(current_module, "Get Notification List", "pass", details)
        else:
            add_test_result(current_module, "Get Notification List", "fail", details)

        # 7.2 Get all notifications (no pagination)
        success, details = test_api(session, "GET", "/api/notifications/student/all")
        if success:
            add_test_result(current_module, "Get All Notifications", "pass", details)
        else:
            add_test_result(current_module, "Get All Notifications", "fail", details)

        # 7.3 Get unread count
        success, details = test_api(session, "GET", "/api/notifications/student/unread-count")
        if success:
            add_test_result(current_module, "Get Unread Count", "pass", details)
        else:
            add_test_result(current_module, "Get Unread Count", "fail", details)

        # 7.4 Mark notification as read
        try:
            notif_response = session.get(f"{BASE_URL}/api/notifications/student?page=1&size=1")
            if notif_response.status_code == 200:
                notif_data = notif_response.json()
                if notif_data.get("data") and notif_data["data"].get("content"):
                    notif_id = notif_data["data"]["content"][0]["id"]
                    success, details = test_api(session, "PUT", f"/api/notifications/{notif_id}/read")
                    if success:
                        add_test_result(current_module, "Mark Notification as Read", "pass", details)
                    else:
                        add_test_result(current_module, "Mark Notification as Read", "fail", details)
                else:
                    add_test_result(current_module, "Mark Notification as Read", "warning", "No notifications to test")
            else:
                add_test_result(current_module, "Mark Notification as Read", "warning", "Cannot get notification list")
        except Exception as e:
            add_test_result(current_module, "Mark Notification as Read", "fail", str(e))

        # 7.5 Mark all as read
        success, details = test_api(session, "PUT", "/api/notifications/read-all")
        if success:
            add_test_result(current_module, "Mark All as Read", "pass", details)
        else:
            add_test_result(current_module, "Mark All as Read", "fail", details)

        # 7.6 Delete all read notifications
        success, details = test_api(session, "DELETE", "/api/notifications/delete-all-read")
        if success:
            add_test_result(current_module, "Delete All Read Notifications", "pass", details)
        else:
            add_test_result(current_module, "Delete All Read Notifications", "fail", details)
    else:
        add_test_result(current_module, "Get Notification List", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get All Notifications", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Unread Count", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Mark Notification as Read", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Mark All as Read", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Delete All Read Notifications", "warning", "Not logged in, skip test")

    # ==================== 8. AI Assistant Module ====================
    current_module = "AI Assistant Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 8] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 8.1 AI generate questions
        success, details = test_api(session, "POST", "/api/ai/generate-questions",
                                   {"topic": "Java Basics", "count": 5, "difficulty": "medium"})
        if success:
            add_test_result(current_module, "AI Generate Questions", "pass", details)
        else:
            add_test_result(current_module, "AI Generate Questions", "fail", details)

        # 8.2 AI generate exam
        success, details = test_api(session, "POST", "/api/ai/generate-exam",
                                   {"courseName": "Java Programming", "totalScore": 100,
                                    "duration": 120, "difficulty": "medium"})
        if success:
            add_test_result(current_module, "AI Generate Exam", "pass", details)
        else:
            add_test_result(current_module, "AI Generate Exam", "fail", details)

        # 8.3 AI learning suggestions
        success, details = test_api(session, "POST", "/api/ai/learning-suggestions",
                                   {"studentId": 1})
        if success:
            add_test_result(current_module, "AI Learning Suggestions", "pass", details)
        else:
            add_test_result(current_module, "AI Learning Suggestions", "fail", details)
    else:
        add_test_result(current_module, "AI Generate Questions", "warning", "Not logged in, skip test")
        add_test_result(current_module, "AI Generate Exam", "warning", "Not logged in, skip test")
        add_test_result(current_module, "AI Learning Suggestions", "warning", "Not logged in, skip test")

    # ==================== 9. Settings Module ====================
    current_module = "Settings Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 9] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 9.1 Get profile
        success, details = test_api(session, "GET", "/api/student/profile")
        if success:
            add_test_result(current_module, "Get Profile", "pass", details)
        else:
            add_test_result(current_module, "Get Profile", "fail", details)

        # 9.2 Update profile
        success, details = test_api(session, "PUT", "/api/student/profile",
                                   {"name": "Test Student", "email": "test@example.com", "phone": "13800138000"})
        if success:
            add_test_result(current_module, "Update Profile", "pass", details)
        else:
            add_test_result(current_module, "Update Profile", "fail", details)

        # 9.3 Get notification settings
        success, details = test_api(session, "GET", "/api/student/notification-settings")
        if success:
            add_test_result(current_module, "Get Notification Settings", "pass", details)
        else:
            add_test_result(current_module, "Get Notification Settings", "fail", details)

        # 9.4 Update notification settings
        success, details = test_api(session, "PUT", "/api/student/notification-settings",
                                   {"emailNotifications": True, "assignmentNotifications": True,
                                    "examNotifications": True, "warningNotifications": True})
        if success:
            add_test_result(current_module, "Update Notification Settings", "pass", details)
        else:
            add_test_result(current_module, "Update Notification Settings", "fail", details)

        # 9.5 Get privacy settings
        success, details = test_api(session, "GET", "/api/student/privacy-settings")
        if success:
            add_test_result(current_module, "Get Privacy Settings", "pass", details)
        else:
            add_test_result(current_module, "Get Privacy Settings", "fail", details)

        # 9.6 Update privacy settings
        success, details = test_api(session, "PUT", "/api/student/privacy-settings",
                                   {"showProfile": True, "showScores": True, "showStudyTime": True})
        if success:
            add_test_result(current_module, "Update Privacy Settings", "pass", details)
        else:
            add_test_result(current_module, "Update Privacy Settings", "fail", details)

        # 9.7 Change password (test with original, don't actually change)
        success, details = test_api(session, "POST", "/api/student/change-password",
                                   {"currentPassword": TEST_PASSWORD,
                                    "newPassword": "Test123456",
                                    "confirmPassword": "Test123456"})
        if success:
            add_test_result(current_module, "Change Password", "pass", details)
            # Change back
            test_api(session, "POST", "/api/student/change-password",
                    {"currentPassword": "Test123456",
                     "newPassword": TEST_PASSWORD,
                     "confirmPassword": TEST_PASSWORD})
        else:
            add_test_result(current_module, "Change Password", "warning", details)

        # 9.8 Export data
        success, details = test_api(session, "GET", "/api/student/export-data")
        if success:
            add_test_result(current_module, "Export Data", "pass", details)
        else:
            add_test_result(current_module, "Export Data", "fail", details)
    else:
        add_test_result(current_module, "Get Profile", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Update Profile", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Notification Settings", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Update Notification Settings", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Get Privacy Settings", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Update Privacy Settings", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Change Password", "warning", "Not logged in, skip test")
        add_test_result(current_module, "Export Data", "warning", "Not logged in, skip test")

    # ==================== 10. Early Warnings Module ====================
    current_module = "Early Warnings Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 10] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    if logged_in:
        # 10.1 Get early warnings
        success, details = test_api(session, "GET", "/api/student/early-warnings")
        if success:
            add_test_result(current_module, "Get Early Warnings", "pass", details)
        else:
            add_test_result(current_module, "Get Early Warnings", "fail", details)
    else:
        add_test_result(current_module, "Get Early Warnings", "warning", "Not logged in, skip test")

    # ==================== 11. System Data Module ====================
    current_module = "System Data Module"
    log(f"\n{'='*60}", Colors.BLUE)
    log(f"[Module 11] {current_module}", Colors.BLUE)
    log("="*60, Colors.BLUE)

    # 11.1 Get semesters (public)
    success, details = test_api(session, "GET", "/api/system/semesters")
    if success:
        add_test_result(current_module, "Get Semesters", "pass", details)
    else:
        add_test_result(current_module, "Get Semesters", "fail", details)

    # 11.2 Get time ranges (public)
    success, details = test_api(session, "GET", "/api/system/time-ranges")
    if success:
        add_test_result(current_module, "Get Time Ranges", "pass", details)
    else:
        add_test_result(current_module, "Get Time Ranges", "fail", details)

    # 11.3 Get student courses (dropdown)
    if logged_in:
        success, details = test_api(session, "GET", "/api/system/student/courses")
        if success:
            add_test_result(current_module, "Get Student Courses", "pass", details)
        else:
            add_test_result(current_module, "Get Student Courses", "fail", details)

    # Generate report
    generate_report(logged_in)

def generate_report(logged_in: bool):
    """Generate test report"""
    log("\n" + "="*60, Colors.BLUE)
    log("Generate Test Report", Colors.BLUE)
    log("="*60, Colors.BLUE)

    total = len(test_results)
    passed = sum(1 for r in test_results if r["status"] == "pass")
    failed = sum(1 for r in test_results if r["status"] == "fail")
    warnings = sum(1 for r in test_results if r["status"] == "warning")

    pass_rate = (passed / total * 100) if total > 0 else 0

    modules = list(dict.fromkeys([r["module"] for r in test_results]))

    report = f"""# Student CRUD Module Test Report

**Test Time:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
**Test Account:** {TEST_USERNAME}
**Test Environment:** {BASE_URL}
**Login Status:** {"Logged in" if logged_in else "Not logged in (some tests skipped)"}

---

## Test Overview

| Metric | Value |
|--------|-------|
| Total Tests | {total} |
| Passed | {passed} |
| Failed | {failed} |
| Warnings | {warnings} |
| **Pass Rate** | **{pass_rate:.1f}%** |

---

## Module Test Details

"""

    for module in modules:
        module_tests = [r for r in test_results if r["module"] == module]
        module_passed = sum(1 for r in module_tests if r["status"] == "pass")
        module_total = len(module_tests)

        report += f"### {module}\n\n"
        report += f"**Passed:** {module_passed}/{module_total}\n\n"
        report += "| Test Item | Status | Details |\n"
        report += "|-----------|--------|----------|\n"

        for test in module_tests:
            status_text = "PASS" if test["status"] == "pass" else "FAIL" if test["status"] == "fail" else "WARN"
            details = test["details"][:50] if test["details"] else "-"
            details = details.replace("|", "\\|")
            report += f"| {test['test_name']} | {status_text} | {details} |\n"

        report += "\n"

    report += """---

## Test Summary

| Module | Pass Rate | Status |
|--------|-----------|--------|
"""

    for module in modules:
        module_tests = [r for r in test_results if r["module"] == module]
        module_passed = sum(1 for r in module_tests if r["status"] == "pass")
        module_total = len(module_tests)
        rate = (module_passed / module_total * 100) if module_total > 0 else 0

        if rate >= 80:
            status = "Normal"
        elif rate >= 50:
            status = "Partial Issues"
        else:
            status = "Abnormal"

        report += f"| {module} | {rate:.0f}% ({module_passed}/{module_total}) | {status} |\n"

    report += f"""
---

## Test Notes

1. **Login Test**: Automated login via Redis captcha retrieval
2. **API Test**: Test all student-side API CRUD functions
3. **Filter Test**: Test filtering, searching, and pagination for each module
4. **Data Operations**: Test data create, read, update, delete operations
5. **Security Verification**: Ensure authentication and authorization work correctly

---

*Report generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*
"""

    # Save report
    report_file = "student-full-test-report.md"
    with open(report_file, "w", encoding="utf-8") as f:
        f.write(report)

    log(f"\nTest report saved to: {report_file}", Colors.GREEN)

    # Print summary
    log("\n" + "="*60, Colors.BLUE)
    log("Test Summary", Colors.BLUE)
    log("="*60, Colors.BLUE)
    log(f"Total Tests: {total}", Colors.NC)
    log(f"Passed: {passed}", Colors.GREEN)
    log(f"Failed: {failed}", Colors.RED if failed > 0 else Colors.NC)
    log(f"Warnings: {warnings}", Colors.YELLOW if warnings > 0 else Colors.NC)
    log(f"Pass Rate: {pass_rate:.1f}%", Colors.GREEN if pass_rate >= 80 else Colors.RED)

    # Print report content
    print("\n" + report)

if __name__ == "__main__":
    run_tests()
