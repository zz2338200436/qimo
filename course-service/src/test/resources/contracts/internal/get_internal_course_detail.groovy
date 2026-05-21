import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_internal_course_detail"
    request {
        method GET()
        url "/internal/courses/101"
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
                id          : 101,
                courseName  : "Distributed Systems",
                courseCode  : "DS101",
                credit      : 3,
                totalHours  : 48,
                teacherId   : 7,
                courseStatus: "ACTIVE",
                semester    : "2026-Fall",
                studentCount: 36
        )
    }
}
