import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_student_assignments"
    request {
        method GET()
        urlPath("/api/student/assignments") {
            headers {
                header("X-User-Id", "42")
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
                message: "获取作业列表成功",
                data: [
                        content         : [[
                                                   id         : 2001,
                                                   title      : "Lab Report",
                                                   description: "Analyze a distributed framework case study",
                                                   courseId   : 101,
                                                   courseName : "Distributed Systems",
                                                   dueDate    : "2026-06-01",
                                                   publishDate: "2026-05-20",
                                                   teacherId  : 7,
                                                   teacherName: "Dr. Chen",
                                                   isActive   : true,
                                                   submission : null
                                           ]],
                        totalPages      : 1,
                        totalElements   : 1,
                        size            : 10,
                        number          : 0,
                        first           : true,
                        last            : true,
                        numberOfElements: 1,
                        empty           : false
                ]
        )
    }
}
