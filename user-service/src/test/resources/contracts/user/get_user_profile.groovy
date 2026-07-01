import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_user_profile"
    request {
        method GET()
        url "/api/users/7"
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
                message: "获取用户成功",
                data: [
                        id      : 7,
                        username: "teacher7",
                        name    : "教师七",
                        email   : "teacher7@example.com",
                        phone   : "13900000007",
                        roles   : ["TEACHER"]
                ]
        )
    }
}
