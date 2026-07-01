import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "update_user_profile"
    request {
        method PUT()
        url "/api/users/7"
        headers {
            contentType(applicationJson())
            accept(applicationJson())
        }
        body(
                name : "教师新名",
                phone: "13900000007"
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
                message: "用户信息更新成功",
                data: [
                        id      : 7,
                        username: "teacher7",
                        name    : "教师新名",
                        email   : "teacher7@example.com",
                        phone   : "13900000007",
                        roles   : ["TEACHER"]
                ]
        )
    }
}
