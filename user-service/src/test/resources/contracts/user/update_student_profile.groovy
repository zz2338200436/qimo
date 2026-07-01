import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "update_student_profile"
    request {
        method PUT()
        url "/api/users/students/42"
        headers {
            contentType(applicationJson())
            accept(applicationJson())
        }
        body(
                realName: "学生新名",
                email   : "new42@example.com",
                avatar  : "/avatars/42-new.png"
        )
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                success: true,
                code: 200,
                message: "学生信息更新成功",
                data: [
                        studentId: 42,
                        username : "student42",
                        realName : "学生新名",
                        email    : "new42@example.com",
                        phone    : "13800000042",
                        avatar   : "/avatars/42-new.png",
                        className: "未知班级",
                        roles    : ["STUDENT"]
                ]
        )
    }
}
