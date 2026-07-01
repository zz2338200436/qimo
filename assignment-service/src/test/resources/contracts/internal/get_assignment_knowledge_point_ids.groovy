import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_assignment_knowledge_point_ids"
    request {
        method GET()
        url "/internal/assignments/2001/knowledge-point-ids"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([501, 502])
    }
}
