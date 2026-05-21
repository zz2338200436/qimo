import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_internal_user_profile"
    request {
        method GET()
        url "/internal/users/7"
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
                id      : 7,
                username: "teacher7",
                name    : "教师七",
                email   : "teacher7@example.com",
                phone   : "13900000007",
                roles   : ["TEACHER"]
        )
    }
}
