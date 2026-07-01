import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "get_internal_assignment_detail"
    request {
        method GET()
        url "/internal/assignments/2001"
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
                id             : 2001,
                title          : "Lab Report",
                description    : "Analyze a distributed framework case study",
                courseId       : 101,
                teacherId      : 7,
                dueDate        : "2026-06-01",
                publishDate    : "2026-05-20",
                maxScore       : 100,
                isActive       : true,
                status         : "ACTIVE",
                submissionCount: 0,
                submittedCount : 0,
                gradedCount    : 0,
                totalStudents  : 36
        )
    }
}
