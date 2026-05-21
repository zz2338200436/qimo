import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_internal_user_roles"
    request {
        method GET()
        url "/internal/users/7/roles"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(["TEACHER"])
    }
}
