import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_teacher_classes"
    request {
        method GET()
        urlPath("/api/teacher/classes") {
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
                message: "获取班级列表成功",
                data: [[
                               id          : 501,
                               className   : "软件 2301",
                               year        : "2023",
                               capacity    : 40,
                               studentCount: 36,
                               teacherId   : 7,
                               majorId     : 2,
                               majorName   : "软件工程",
                               courseName  : "Distributed Systems、Algorithms",
                               courseCount : 2
                       ]]
        )
    }
}
