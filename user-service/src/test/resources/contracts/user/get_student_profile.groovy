import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_student_profile"
    request {
        method GET()
        url "/api/users/students/42"
        headers {
            accept(applicationJson())
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
                message: "获取学生详情成功",
                data: [
                        studentId: 42,
                        username : "student42",
                        realName : "学生四二",
                        email    : "student42@example.com",
                        phone    : "13800000042",
                        avatar   : "/avatars/42.png",
                        className: "未知班级",
                        roles    : ["STUDENT"]
                ]
        )
    }
}
