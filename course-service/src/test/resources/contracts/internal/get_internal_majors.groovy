import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_internal_majors"
    request {
        method GET()
        url "/internal/courses/majors"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
                [
                        id       : 2,
                        majorName: "软件工程"
                ]
        ])
    }
}
