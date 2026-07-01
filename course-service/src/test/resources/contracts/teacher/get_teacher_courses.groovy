import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_teacher_courses"
    request {
        method GET()
        urlPath("/api/teacher/courses") {
            headers {
                header("X-User-Id", "7")
                accept(applicationJson())
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                success: true,
                code: 200,
                message: "获取课程列表成功",
                data: [
                        content         : [[
                                                   id          : 101,
                                                   courseName  : "Distributed Systems",
                                                   courseCode  : "DS101",
                                                   credit      : 3,
                                                   totalHours  : 48,
                                                   teacherId   : 7,
                                                   courseStatus: "ACTIVE",
                                                   semester    : "2026-Fall",
                                                   studentCount: 36
                                           ]],
                        pageNumber      : 1,
                        pageSize        : 10,
                        totalElements   : 1,
                        totalPages      : 1,
                        first           : true,
                        last            : true,
                        offset          : 0,
                        numberOfElements: 1,
                        empty           : false
                ]
        )
    }
}
