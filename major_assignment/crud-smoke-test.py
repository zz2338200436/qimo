#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
教师端课程管理 CRUD 冒烟测试
- 绕过图形验证码：从 Redis 直接读 CAPTCHA:IMG:<jsessionid>
- 绕过 CSRF 问题：严格按真实浏览器流程，从 XSRF-TOKEN cookie 读值 -> X-XSRF-TOKEN 头
  （验证修复后的 SecurityConfig 是否生效）
"""
import sys
import json
import time
import redis
import requests

BASE = "http://localhost:8080"
TEACHER_USER = "teacher1"
TEACHER_PASS = "123456"

r_redis = redis.Redis(host="localhost", port=6379, db=0, decode_responses=True)
s = requests.Session()


def step(title):
    print(f"\n=== {title} ===")


def show(resp, note=""):
    body = resp.text
    if len(body) > 400:
        body = body[:400] + "..."
    print(f"  {note}HTTP {resp.status_code}  {body}")
    return resp


def csrf_headers():
    tok = s.cookies.get("XSRF-TOKEN")
    return {"X-XSRF-TOKEN": tok} if tok else {}


def login():
    step("login")
    # 1) 先访问一次业务静态页让容器生成 session (JSESSIONID)
    s.get(f"{BASE}/teacher-login.html")
    # 2) 拉验证码（服务端会把 session 与 captcha 绑定到 Redis）
    s.get(f"{BASE}/api/public/captcha")
    jsess = s.cookies.get("JSESSIONID")
    if not jsess:
        # Spring 对静态页没发 JSESSIONID，以 API 响应为准
        # 抓一次需要 session 的接口
        s.get(f"{BASE}/api/auth/me")
        jsess = s.cookies.get("JSESSIONID")
    print(f"  JSESSIONID = {jsess}")
    captcha = r_redis.get(f"CAPTCHA:IMG:{jsess}")
    if captcha and captcha.startswith('"') and captcha.endswith('"'):
        captcha = captcha[1:-1]
    print(f"  captcha from redis = {captcha}")
    if not captcha:
        print("!! 未找到 captcha，流程终止")
        sys.exit(1)

    # 3) 登录
    resp = s.post(
        f"{BASE}/api/auth/login",
        json={"username": TEACHER_USER, "password": TEACHER_PASS, "captcha": captcha},
        headers={"Content-Type": "application/json"},
    )
    show(resp, "login -> ")
    if resp.status_code != 200 or not resp.json().get("data"):
        sys.exit(1)
    print(f"  logged-in cookies: "
          f"JSESSIONID_TEACHER={s.cookies.get('JSESSIONID_TEACHER')}, "
          f"XSRF-TOKEN={s.cookies.get('XSRF-TOKEN')}")


def read_list():
    step("R (list)")
    resp = s.get(f"{BASE}/api/teacher/courses?page=1&size=10")
    show(resp, "GET /api/teacher/courses -> ")
    return resp.json()


def create():
    step("C (create)")
    body = {
        "courseCode": "CRUDTEST",
        "courseName": "CRUD冒烟测试课",
        "courseCategory": "选修",
        "credit": 2,
        "totalHours": 32,
        "teacherId": 1,
        "courseDirector": 1,
        "assessmentMethod": "综合评价",
        "courseStatus": "未开始",
        "semester": "第一学期",
        "startDate": "2026-06-01",
        "endDate": "2026-08-31",
        "maxStudents": 50,
        "description": "自动化冒烟测试，用完即删",
    }
    resp = s.post(
        f"{BASE}/api/teacher/courses",
        json=body,
        headers={"Content-Type": "application/json", **csrf_headers()},
    )
    show(resp, "POST /api/teacher/courses -> ")
    if resp.status_code not in (200, 201):
        return None
    data = resp.json().get("data") or {}
    return data.get("id")


def update(course_id):
    step(f"U (update id={course_id})")
    body = {
        "id": course_id,
        "courseCode": "CRUDTEST",
        "courseName": "CRUD冒烟测试课[已修改]",
        "courseCategory": "选修",
        "credit": 3,
        "totalHours": 48,
        "teacherId": 1,
        "courseDirector": 1,
        "assessmentMethod": "综合评价",
        "courseStatus": "进行中",
        "semester": "第一学期",
        "startDate": "2026-06-01",
        "endDate": "2026-08-31",
        "maxStudents": 60,
        "description": "PUT 更新测试",
    }
    resp = s.put(
        f"{BASE}/api/teacher/courses/{course_id}",
        json=body,
        headers={"Content-Type": "application/json", **csrf_headers()},
    )
    show(resp, f"PUT /api/teacher/courses/{course_id} -> ")
    return resp.status_code in (200, 204)


def delete(course_id):
    step(f"D (delete id={course_id})")
    resp = s.delete(
        f"{BASE}/api/teacher/courses/{course_id}",
        headers={**csrf_headers()},
    )
    show(resp, f"DELETE /api/teacher/courses/{course_id} -> ")
    return resp.status_code in (200, 204)


def main():
    login()
    read_list()

    cid = create()
    if not cid:
        print("!! Create 失败")
        sys.exit(1)

    # 再读一次确认创建成功
    step("R (verify created)")
    resp = s.get(f"{BASE}/api/teacher/courses/{cid}")
    show(resp, f"GET /api/teacher/courses/{cid} -> ")

    ok_u = update(cid)
    ok_d = delete(cid)

    # 最终结果汇总
    print("\n=== 结果 ===")
    print(f"  Create: {'✅' if cid else '❌'}  (id={cid})")
    print(f"  Update: {'✅' if ok_u else '❌'}")
    print(f"  Delete: {'✅' if ok_d else '❌'}")


if __name__ == "__main__":
    main()
