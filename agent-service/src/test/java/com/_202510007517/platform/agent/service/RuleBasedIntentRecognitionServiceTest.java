package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedIntentRecognitionServiceTest {

    private final RuleBasedIntentRecognitionService service = new RuleBasedIntentRecognitionService();

    @Test
    void recognizesAssignmentPublishing() {
        RecognizedIntent result = service.recognize("给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("title", "Spring Cloud实验");
        assertThat(result.slots()).containsEntry("courseName", "Java");
        assertThat(result.slots()).containsEntry("maxScore", 100);
    }

    @Test
    void recognizesAssignmentPublishingFromSemesterCourseAndClassPhrase() {
        RecognizedIntent result = service.recognize(
                "请在 2025-2026-2 学期《云计算技术》软件工程23级1班 这门课下发布作业：标题“Java基础”，中等难度，2道题，满分100分，截止时间 2026-06-17 23:59。");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
        assertThat(result.slots())
                .containsEntry("semester", "2025-2026-2")
                .containsEntry("courseName", "云计算技术")
                .containsEntry("className", "软件工程23级1班")
                .containsEntry("title", "Java基础")
                .containsEntry("maxScore", 100)
                .containsEntry("dueDate", "2026-06-17 23:59");
    }

    @Test
    void recognizesExamPublishing() {
        RecognizedIntent result = service.recognize("给Java课程发布考试，标题是期末考试，时长90分钟，满分100");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_EXAM);
        assertThat(result.slots()).containsEntry("title", "期末考试");
        assertThat(result.slots()).containsEntry("courseName", "Java");
        assertThat(result.slots()).containsEntry("duration", 90);
        assertThat(result.slots()).containsEntry("maxScore", 100);
    }

    @Test
    void recognizesExamPublishingTimes() {
        RecognizedIntent result = service.recognize(
                "发布考试，课程ID 91005，标题是Agent阶段测验，开始时间2026-12-30 09:00:00，结束时间2026-12-30 10:30:00，时长90分钟");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_EXAM);
        assertThat(result.slots())
                .containsEntry("courseId", 91005L)
                .containsEntry("title", "Agent阶段测验")
                .containsEntry("startTime", "2026-12-30 09:00:00")
                .containsEntry("endTime", "2026-12-30 10:30:00")
                .containsEntry("duration", 90);
    }

    @Test
    void recognizesCourseCreation() {
        RecognizedIntent result = service.recognize(
                "创建课程，课程名称是分布式框架技术，课程代码是DFT101，学分3，总学时48");

        assertThat(result.intent()).isEqualTo(AgentIntent.CREATE_COURSE);
        assertThat(result.slots())
                .containsEntry("courseName", "分布式框架技术")
                .containsEntry("courseCode", "DFT101")
                .containsEntry("credit", 3)
                .containsEntry("totalHours", 48);
    }

    @Test
    void recognizesCourseUpdating() {
        RecognizedIntent result = service.recognize(
                "更新课程ID 101，课程名称是高级分布式框架技术，课程代码是DFT201，学分4，总学时64");

        assertThat(result.intent()).isEqualTo(AgentIntent.UPDATE_COURSE);
        assertThat(result.slots())
                .containsEntry("courseId", 101L)
                .containsEntry("courseName", "高级分布式框架技术")
                .containsEntry("courseCode", "DFT201")
                .containsEntry("credit", 4)
                .containsEntry("totalHours", 64);
    }

    @Test
    void recognizesCourseDeleting() {
        RecognizedIntent result = service.recognize("删除课程ID 101");

        assertThat(result.intent()).isEqualTo(AgentIntent.DELETE_COURSE);
        assertThat(result.slots()).containsEntry("courseId", 101L);
    }

    @Test
    void recognizesClassCreation() {
        RecognizedIntent result = service.recognize("创建班级，班级名称是软件2301，年级2023，容量40，课程ID 101，专业ID 2");

        assertThat(result.intent()).isEqualTo(AgentIntent.CREATE_CLASS);
        assertThat(result.slots())
                .containsEntry("className", "软件2301")
                .containsEntry("year", "2023")
                .containsEntry("capacity", 40)
                .containsEntry("courseId", 101L)
                .containsEntry("majorId", 2L);
    }

    @Test
    void recognizesColloquialClassCreation() {
        RecognizedIntent result = service.recognize("帮我开一个软件2302班，2023级，最多45人，挂到课程101和专业2下面");

        assertThat(result.intent()).isEqualTo(AgentIntent.CREATE_CLASS);
        assertThat(result.slots())
                .containsEntry("className", "软件2302")
                .containsEntry("year", "2023")
                .containsEntry("capacity", 45)
                .containsEntry("courseId", 101L)
                .containsEntry("majorId", 2L);
    }

    @Test
    void recognizesExamUpdating() {
        RecognizedIntent result = service.recognize("更新考试，标题是期末考试补考，时长60分钟");

        assertThat(result.intent()).isEqualTo(AgentIntent.UPDATE_EXAM);
        assertThat(result.slots()).containsEntry("title", "期末考试补考");
        assertThat(result.slots()).containsEntry("duration", 60);
    }

    @Test
    void recognizesExamDeleting() {
        RecognizedIntent result = service.recognize("删除考试，标题是过期补考");

        assertThat(result.intent()).isEqualTo(AgentIntent.DELETE_EXAM);
        assertThat(result.slots()).containsEntry("title", "过期补考");
    }

    @Test
    void recognizesExamGrading() {
        RecognizedIntent result = service.recognize("批改考试，给92分");

        assertThat(result.intent()).isEqualTo(AgentIntent.GRADE_EXAM);
        assertThat(result.slots()).containsEntry("maxScore", 92);
    }

    @Test
    void recognizesExamDetailLookup() {
        RecognizedIntent result = service.recognize("查看考试详情");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_EXAM_DETAIL);
    }

    @Test
    void recognizesExamSubmissionLookup() {
        RecognizedIntent result = service.recognize("查看考试提交记录");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_EXAM_SUBMISSIONS);
    }

    @Test
    void recognizesAssignmentUpdating() {
        RecognizedIntent result = service.recognize("更新作业，标题是微服务网关实验，满分100分");

        assertThat(result.intent()).isEqualTo(AgentIntent.UPDATE_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("title", "微服务网关实验");
        assertThat(result.slots()).containsEntry("maxScore", 100);
    }

    @Test
    void recognizesAssignmentDeleting() {
        RecognizedIntent result = service.recognize("删除作业，标题是过期练习");

        assertThat(result.intent()).isEqualTo(AgentIntent.DELETE_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("title", "过期练习");
    }

    @Test
    void recognizesAssignmentGrading() {
        RecognizedIntent result = service.recognize("批改作业，给95分");

        assertThat(result.intent()).isEqualTo(AgentIntent.GRADE_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("maxScore", 95);
    }

    @Test
    void recognizesAssignmentSubmissionLookup() {
        RecognizedIntent result = service.recognize("查看作业提交记录");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS);
    }

    @Test
    void recognizesAssignmentSubmissionLookupById() {
        RecognizedIntent result = service.recognize("查看作业ID 9940521提交记录");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS);
        assertThat(result.slots()).containsEntry("assignmentId", 9940521L);
    }

    @Test
    void recognizesAssignmentDetailLookup() {
        RecognizedIntent result = service.recognize("查看作业详情");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENT_DETAIL);
    }

    @Test
    void recognizesAssignmentSubmission() {
        RecognizedIntent result = service.recognize("帮我提交数据库作业，内容是实验报告已完成");

        assertThat(result.intent()).isEqualTo(AgentIntent.SUBMIT_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("assignmentTitle", "数据库作业");
        assertThat(result.slots()).containsEntry("content", "实验报告已完成");
    }

    @Test
    void recognizesAssignmentSubmissionById() {
        RecognizedIntent result = service.recognize("提交作业ID 9940521，内容是浏览器冒烟提交内容");

        assertThat(result.intent()).isEqualTo(AgentIntent.SUBMIT_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("assignmentId", 9940521L);
        assertThat(result.slots()).containsEntry("content", "浏览器冒烟提交内容");
    }

    @Test
    void recognizesExamSubmission() {
        RecognizedIntent result = service.recognize("帮我提交Java期末考试，用时45分钟");

        assertThat(result.intent()).isEqualTo(AgentIntent.SUBMIT_EXAM);
        assertThat(result.slots()).containsEntry("examTitle", "Java期末考试");
        assertThat(result.slots()).containsEntry("timeTaken", 45);
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractsExamSubmissionAnswers() {
        RecognizedIntent result = service.recognize("提交Java期末考试，用时45分钟，答案是1:A,2:B,essay:已完成");

        assertThat(result.intent()).isEqualTo(AgentIntent.SUBMIT_EXAM);
        assertThat((Map<String, String>) result.slots().get("answers"))
                .containsEntry("1", "A")
                .containsEntry("2", "B")
                .containsEntry("essay", "已完成");
    }

    @Test
    void recognizesReadOnlyQueries() {
        assertThat(service.recognize("我有哪些待提交作业").intent()).isEqualTo(AgentIntent.QUERY_PENDING_ASSIGNMENTS);
        assertThat(service.recognize("查看我的课程").intent()).isEqualTo(AgentIntent.QUERY_COURSES);
        assertThat(service.recognize("查看课程详情").intent()).isEqualTo(AgentIntent.QUERY_COURSE_DETAIL);
        assertThat(service.recognize("查看班级列表").intent()).isEqualTo(AgentIntent.QUERY_CLASSES);
        assertThat(service.recognize("查看班级详情").intent()).isEqualTo(AgentIntent.QUERY_CLASS_DETAIL);
        assertThat(service.recognize("查看考试列表").intent()).isEqualTo(AgentIntent.QUERY_EXAMS);
        assertThat(service.recognize("查看我的成绩").intent()).isEqualTo(AgentIntent.QUERY_SCORES);
        assertThat(service.recognize("查看我的通知").intent()).isEqualTo(AgentIntent.QUERY_NOTIFICATIONS);
        assertThat(service.recognize("查看我的未读通知数量").intent()).isEqualTo(AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT);
        assertThat(service.recognize("查看教师仪表盘").intent()).isEqualTo(AgentIntent.QUERY_TEACHER_DASHBOARD);
        assertThat(service.recognize("查看班级学情概览").intent()).isEqualTo(AgentIntent.QUERY_TEACHER_DASHBOARD);
        assertThat(service.recognize("查看学生学习汇总").intent()).isEqualTo(AgentIntent.QUERY_LEARNING_SUMMARY);
        assertThat(service.recognize("查看班级学习汇总").intent()).isEqualTo(AgentIntent.QUERY_LEARNING_SUMMARY);
        assertThat(service.recognize("查看成绩趋势").intent()).isEqualTo(AgentIntent.QUERY_SCORE_TREND);
        assertThat(service.recognize("查看班级成绩趋势").intent()).isEqualTo(AgentIntent.QUERY_SCORE_TREND);
        RecognizedIntent knowledgePoints = service.recognize("查看课程ID 91005 的知识点");
        assertThat(knowledgePoints.intent()).isEqualTo(AgentIntent.QUERY_KNOWLEDGE_POINTS);
        assertThat(knowledgePoints.slots()).containsEntry("courseId", 91005L);
        RecognizedIntent namedKnowledgePoints = service.recognize("查询云计算技术课程知识点");
        assertThat(namedKnowledgePoints.intent()).isEqualTo(AgentIntent.QUERY_KNOWLEDGE_POINTS);
        assertThat(namedKnowledgePoints.slots()).containsEntry("courseName", "云计算技术");
        RecognizedIntent mastery = service.recognize("查看学生21课程3知识点掌握情况");
        assertThat(mastery.intent()).isEqualTo(AgentIntent.QUERY_KNOWLEDGE_MASTERY);
        assertThat(mastery.slots()).containsEntry("studentId", 21L).containsEntry("courseId", 3L);
        assertThat(service.recognize("查看学习统计").intent()).isEqualTo(AgentIntent.QUERY_STUDENT_STATS);
        assertThat(service.recognize("查看学习时间分布").intent()).isEqualTo(AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION);
    }

    @Test
    void recognizesMarkAllNotificationsReadCommand() {
        RecognizedIntent result = service.recognize("把我的所有通知标为已读");

        assertThat(result.intent()).isEqualTo(AgentIntent.MARK_ALL_NOTIFICATIONS_READ);
    }

    @Test
    void recognizesMarkSingleNotificationReadCommand() {
        RecognizedIntent result = service.recognize("把通知ID 9标为已读");

        assertThat(result.intent()).isEqualTo(AgentIntent.MARK_NOTIFICATION_READ);
        assertThat(result.slots()).containsEntry("notificationId", 9L);
    }

    @Test
    void recognizesDeleteSingleNotificationCommand() {
        RecognizedIntent result = service.recognize("删除通知ID 9");

        assertThat(result.intent()).isEqualTo(AgentIntent.DELETE_NOTIFICATION);
        assertThat(result.slots()).containsEntry("notificationId", 9L);
    }

    @Test
    void recognizesDeleteAllReadNotificationsCommand() {
        RecognizedIntent result = service.recognize("删除所有已读通知");

        assertThat(result.intent()).isEqualTo(AgentIntent.DELETE_ALL_READ_NOTIFICATIONS);
    }

    @Test
    void recognizesTeacherSendNotificationCommand() {
        RecognizedIntent result = service.recognize("给学生ID 42发送通知，标题是开课通知，内容是请按时上课，类型是course");

        assertThat(result.intent()).isEqualTo(AgentIntent.SEND_NOTIFICATION);
        assertThat(result.slots())
                .containsEntry("studentId", 42L)
                .containsEntry("title", "开课通知")
                .containsEntry("content", "请按时上课")
                .containsEntry("type", "course");
    }

    @Test
    void recognizesTeacherSendNotificationWhenContentMentionsCourseReminder() {
        RecognizedIntent result = service.recognize("给学生ID 42发送通知，标题是课程提醒，内容是请查看新的课程提醒，类型是course");

        assertThat(result.intent()).isEqualTo(AgentIntent.SEND_NOTIFICATION);
        assertThat(result.slots())
                .containsEntry("studentId", 42L)
                .containsEntry("title", "课程提醒")
                .containsEntry("content", "请查看新的课程提醒")
                .containsEntry("type", "course");
    }

    @Test
    @SuppressWarnings("unchecked")
    void recognizesTeacherSendBatchNotificationCommand() {
        RecognizedIntent result = service.recognize("给学生ID 42,43批量发送通知，标题是开课通知，内容是请按时上课，类型是course");

        assertThat(result.intent()).isEqualTo(AgentIntent.SEND_BATCH_NOTIFICATION);
        assertThat((List<Long>) result.slots().get("studentIds")).containsExactly(42L, 43L);
        assertThat(result.slots())
                .containsEntry("title", "开课通知")
                .containsEntry("content", "请按时上课")
                .containsEntry("type", "course");
    }

    @Test
    @SuppressWarnings("unchecked")
    void recognizesTeacherGroupSendNotificationCommand() {
        RecognizedIntent result = service.recognize("给学生ID 42、43群发通知，标题是开课通知，内容是请按时上课，类型是course");

        assertThat(result.intent()).isEqualTo(AgentIntent.SEND_BATCH_NOTIFICATION);
        assertThat((List<Long>) result.slots().get("studentIds")).containsExactly(42L, 43L);
    }

    @Test
    void doesNotTreatReadOnlyNotificationLookupAsMarkAllReadCommand() {
        RecognizedIntent result = service.recognize("查看所有已读通知");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_NOTIFICATIONS);
    }

    @Test
    void recognizesAiGenerationCommands() {
        RecognizedIntent generateQuestions = service.recognize("生成五道Java选择题");
        assertThat(generateQuestions.intent()).isEqualTo(AgentIntent.GENERATE_QUESTIONS);
        assertThat(generateQuestions.slots())
                .containsEntry("count", 5)
                .containsEntry("topic", "Java")
                .containsEntry("type", "SINGLE_CHOICE");
        assertThat(service.recognize("帮我生成一份Java模拟试卷").intent()).isEqualTo(AgentIntent.GENERATE_EXAM);
        assertThat(service.recognize("生成我的学习建议").intent()).isEqualTo(AgentIntent.GENERATE_LEARNING_SUGGESTIONS);
    }

    @Test
    void extractsQuestionGenerationTopicCountAndDifficulty() {
        RecognizedIntent result = service.recognize("生成2道Java基础中等难度题");

        assertThat(result.intent()).isEqualTo(AgentIntent.GENERATE_QUESTIONS);
        assertThat(result.slots())
                .containsEntry("count", 2)
                .containsEntry("topic", "Java基础")
                .containsEntry("difficulty", "中等");
    }

    @Test
    void recognizesQuestionBankQueries() {
        assertThat(service.recognize("现在题库有什么题目").intent()).isEqualTo(AgentIntent.QUERY_QUESTION_BANK);
        assertThat(service.recognize("查询题库有哪些知识点").intent()).isEqualTo(AgentIntent.QUERY_QUESTION_BANK);
    }

    @Test
    void recognizesRagKnowledgeQuestion() {
        RecognizedIntent result = service.recognize("什么是服务注册与发现？");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_RAG_KNOWLEDGE);
        assertThat(result.confidence()).isEqualTo(0.9);
    }

    @Test
    void keepsBusinessQueriesOutOfRagRouting() {
        RecognizedIntent assignmentQuery = service.recognize("课程ID 91005 有哪些作业？");
        RecognizedIntent deleteNotification = service.recognize("帮我删除通知ID 9");

        assertThat(assignmentQuery.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENTS);
        assertThat(deleteNotification.intent()).isEqualTo(AgentIntent.DELETE_NOTIFICATION);
    }

    @Test
    void returnsUnknownForUnsupportedMessage() {
        RecognizedIntent result = service.recognize("今天食堂吃什么");

        assertThat(result.intent()).isEqualTo(AgentIntent.UNKNOWN);
        assertThat(result.confidence()).isLessThan(0.5);
    }
}
