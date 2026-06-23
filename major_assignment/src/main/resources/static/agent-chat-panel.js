(function attachAgentChatPanel(global) {
    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function renderAgentText(value) {
        return escapeHtml(value)
            .replace(/\*\*([^*\n][^*\n]*?)\*\*/g, '<strong>$1</strong>');
    }

    function hasValue(value) {
        return value !== undefined && value !== null && value !== '';
    }

    const AGENT_FIELD_LABELS = {
        difficulty: '难度',
        count: '数量',
        actualCount: '实际数量',
        partial: '数量不足',
        topic: '主题',
        topicCount: '主题数',
        questions: '题目',
        questionCount: '题目数',
        responseType: '响应类型',
        message: '消息',
        data: '数据',
        result: '结果',
        actionPreview: '操作预览',
        actionId: '操作编号',
        intent: '操作类型',
        riskLevel: '风险等级',
        title: '标题',
        summary: '摘要',
        preview: '预览',
        idempotencyKey: '幂等键',
        secondConfirmationRequired: '需要二次确认',
        secondConfirmationPhrase: '二次确认口令',
        secondConfirmationPrompt: '二次确认提示',
        sessionId: '会话编号',
        createdAt: '创建时间',
        updatedAt: '更新时间',
        score: '分值',
        answer: '答案',
        analysis: '解析',
        explanation: '解析',
        type: '类型',
        content: '内容',
        options: '选项',
        source: '题目来源',
        sourcePath: '来源文件',
        knowledgePoints: '知识点',
        totalScore: '总分',
        totalQuestions: '题目总数',
        totalKnowledgePoints: '知识点总数',
        duration: '时长',
        courseName: '课程名称'
    };

    /** 不展示给用户的内部字段 */
    const SKIP_META_KEYS = new Set(['aiResult', 'status', 'responseType', 'actionId', 'riskLevel',
        'idempotencyKey', 'secondConfirmationRequired', 'secondConfirmationPhrase',
        'secondConfirmationPrompt', 'sessionId', 'createdAt', 'updatedAt', 'actionPreview']);

    const AGENT_VALUE_LABELS = {
        EXECUTED: '已执行',
        FAILED: '执行失败',
        CANCELLED: '已取消',
        PENDING_CONFIRMATION: '待确认',
        PENDING_SECOND_CONFIRMATION: '待二次确认',
        ACTION_PREVIEW: '待确认操作',
        DATA: '查询结果',
        MESSAGE: '消息',
        LOW: '低风险',
        MEDIUM: '中风险',
        HIGH: '高风险',
        CRITICAL: '严重风险',
        GENERATE_QUESTIONS: '生成题目',
        GENERATE_EXAM: '生成试卷',
        QUERY_COURSES: '查询课程',
        QUERY_ASSIGNMENTS: '查询作业',
        QUERY_EXAMS: '查询考试',
        SEND_NOTIFICATION: '发送通知',
        SEND_BATCH_NOTIFICATION: '批量发送通知',
        MARK_NOTIFICATION_READ: '标记通知已读',
        SUBMIT_ASSIGNMENT: '提交作业',
        SUBMIT_EXAM: '提交考试'
    };

    function formatAgentLabel(value) {
        return AGENT_FIELD_LABELS[value] || value;
    }

    function renderScalarValue(value) {
        if (typeof value === 'boolean') {
            return value ? '是' : '否';
        }
        if (typeof value === 'string') {
            return escapeHtml(AGENT_VALUE_LABELS[value] || value);
        }
        return escapeHtml(value);
    }

    function isChipValue(value) {
        if (value == null || value === '') {
            return false;
        }
        if (Array.isArray(value)) {
            return value.length > 0 && value.every(item => item == null || ['string', 'number', 'boolean'].includes(typeof item));
        }
        return typeof value !== 'object';
    }

    function getApiService() {
        return global.apiService || null;
    }

    function getCurrentSessionStorageKey() {
        return 'agent.currentSessionId';
    }

    function isQuestionLike(value) {
        return value && typeof value === 'object' && !Array.isArray(value)
            && ('content' in value || 'questionText' in value || 'title' in value)
            && ('answer' in value || 'options' in value || 'difficulty' in value || 'score' in value || 'type' in value);
    }

    function getCourseId(course) {
        return course?.id ?? course?.courseId ?? course?.course_id ?? null;
    }

    function isCourseLike(value) {
        return value && typeof value === 'object' && !Array.isArray(value)
            && hasValue(getCourseId(value))
            && ('courseName' in value || 'courseCode' in value)
            && ('credit' in value || 'totalHours' in value || 'semester' in value || 'studentCount' in value);
    }

    function buildCourseDetailCommand(course) {
        const courseId = getCourseId(course);
        return hasValue(courseId) ? `查看课程详情 课程ID ${courseId}` : '查看课程详情';
    }

    function renderCourseMetaItem(label, value) {
        if (value == null || value === '') {
            return '';
        }
        return `
            <div class="agent-course-meta-item">
                <dt>${escapeHtml(label)}</dt>
                <dd>${renderValue(value)}</dd>
            </div>
        `;
    }

    function renderCourseCard(course, options = {}) {
        const showDetailAction = options.showDetailAction !== false;
        const courseId = getCourseId(course);
        const courseName = course.courseName || course.courseCode || `课程 ${courseId || ''}`.trim();
        const courseCode = course.courseCode || '';
        const description = course.description || '';
        const detailCommand = buildCourseDetailCommand(course);
        const meta = [
            renderCourseMetaItem('课程ID', courseId),
            renderCourseMetaItem('学期', course.semester),
            renderCourseMetaItem('学分', course.credit),
            renderCourseMetaItem('总学时', course.totalHours),
            renderCourseMetaItem('学生数', course.studentCount),
            renderCourseMetaItem('开始时间', course.startDate),
            renderCourseMetaItem('结束时间', course.endDate)
        ].filter(Boolean).join('');

        return `
            <article class="agent-course-card">
                <div class="agent-course-card-header">
                    <div>
                        <strong>${escapeHtml(courseName)}</strong>
                        ${courseCode ? `<p>${escapeHtml(courseCode)}</p>` : ''}
                    </div>
                    ${showDetailAction ? `<button type="button" class="btn btn-sm btn-outline-primary" data-agent-command="${escapeHtml(detailCommand)}">
                        查看课程详情
                    </button>` : ''}
                </div>
                ${description ? `<p class="agent-course-description">${renderAgentText(description)}</p>` : ''}
                ${meta ? `<dl class="agent-course-meta">${meta}</dl>` : ''}
            </article>
        `;
    }

    function renderCourseDetailCard(course) {
        const courseId = getCourseId(course);
        const courseName = course.courseName || course.courseCode || `课程 ${courseId || ''}`.trim();
        const courseCode = course.courseCode || '';
        const description = course.description || '';
        const overviewMeta = [
            renderCourseMetaItem('课程ID', courseId),
            renderCourseMetaItem('课程代码', courseCode),
            renderCourseMetaItem('课程类别', course.courseCategory),
            renderCourseMetaItem('课程状态', course.courseStatus),
            renderCourseMetaItem('学期', course.semester),
            renderCourseMetaItem('学分', course.credit)
        ].filter(Boolean).join('');
        const scheduleMeta = [
            renderCourseMetaItem('总学时', course.totalHours),
            renderCourseMetaItem('学生数', course.studentCount),
            renderCourseMetaItem('开始时间', course.startDate),
            renderCourseMetaItem('结束时间', course.endDate),
            renderCourseMetaItem('负责人ID', course.courseDirector),
            renderCourseMetaItem('考核方式', course.assessmentMethod)
        ].filter(Boolean).join('');

        return `
            <article class="agent-course-detail-card">
                <div class="agent-course-detail-header">
                    <div>
                        <p class="agent-course-detail-eyebrow">课程详情</p>
                        <strong>${escapeHtml(courseName)}</strong>
                        ${courseCode ? `<p class="agent-course-detail-code">${escapeHtml(courseCode)}</p>` : ''}
                    </div>
                </div>
                ${description ? `<p class="agent-course-detail-description">${renderAgentText(description)}</p>` : ''}
                ${overviewMeta ? `
                    <section class="agent-course-detail-section">
                        <h4>基础信息</h4>
                        <dl class="agent-course-meta">${overviewMeta}</dl>
                    </section>
                ` : ''}
                ${scheduleMeta ? `
                    <section class="agent-course-detail-section">
                        <h4>教学安排</h4>
                        <dl class="agent-course-meta">${scheduleMeta}</dl>
                    </section>
                ` : ''}
            </article>
        `;
    }

    function renderCourseList(courses) {
        return `<div class="agent-course-list">${courses.map(renderCourseCard).join('')}</div>`;
    }

    function renderCollectionSummary(value, collectionKey) {
        const entries = Object.entries(value)
            .filter(([key]) => key !== collectionKey)
            .filter(([, item]) => item != null && item !== '');
        if (entries.length === 0) {
            return '';
        }
        return `
            <dl class="agent-collection-summary">
                ${entries.map(([key, item]) => `
                    <div>
                        <dt>${escapeHtml(formatAgentLabel(key))}</dt>
                        <dd>${renderScalarValue(item)}</dd>
                    </div>
                `).join('')}
            </dl>
        `;
    }

    function buildCollapsibleMeta(summaryChipsHtml, detailHtml) {
        if (!detailHtml) {
            return summaryChipsHtml || '';
        }
        const id = `agent-meta-${Math.random().toString(36).slice(2, 9)}`;
        return `
            <div class="agent-metadata-summary-bar">
                <button type="button" class="agent-metadata-toggle"
                        data-agent-meta-toggle="${id}"
                        aria-expanded="false"
                        aria-controls="${id}">
                    <i class="fa fa-caret-right" aria-hidden="true"></i> 详情
                </button>
                ${summaryChipsHtml}
            </div>
            <div class="agent-metadata-collapse" id="${id}" style="max-height:0;opacity:0;margin-top:0;" aria-hidden="true">
                ${detailHtml}
            </div>
        `;
    }

    function buildMetaChips(entries) {
        // 挑几个关键字段展示为摘要标签（最多4个），跳过内部字段
        const visibleEntries = entries.filter(([key]) => !SKIP_META_KEYS.has(key));
        const priorityKeys = ['topic', 'difficulty', 'count', 'actualCount', 'totalQuestions', 'totalKnowledgePoints', 'topicCount', 'partial', 'type', 'title', 'intent', 'message'];
        const chips = [];
        for (const key of priorityKeys) {
            const entry = visibleEntries.find(([k]) => k === key);
            if (entry) {
                const [, val] = entry;
                if (!isChipValue(val)) {
                    continue;
                }
                const v = renderScalarValue(val);
                if (v && v !== '无') {
                    chips.push(`<span class="agent-metadata-chip">${escapeHtml(formatAgentLabel(key))}：${v}</span>`);
                }
            }
        }
        // 如果没有匹配到优先级键，取前3个
        if (chips.length === 0) {
            for (const [key, val] of visibleEntries.filter(([, value]) => isChipValue(value)).slice(0, 3)) {
                const v = renderScalarValue(val);
                if (v && v !== '无') {
                    chips.push(`<span class="agent-metadata-chip">${escapeHtml(formatAgentLabel(key))}：${v}</span>`);
                }
            }
        }
        return chips.join('');
    }

    function initMetaToggles(root) {
        if (!root) return;
        root.querySelectorAll('[data-agent-meta-toggle]').forEach(btn => {
            btn.addEventListener('click', () => {
                const target = document.getElementById(btn.getAttribute('data-agent-meta-toggle'));
                if (!target) return;
                const isOpen = btn.getAttribute('aria-expanded') === 'true';
                btn.setAttribute('aria-expanded', String(!isOpen));
                target.setAttribute('aria-hidden', String(isOpen));
                target.style.maxHeight = isOpen ? '0' : (target.scrollHeight + 16) + 'px';
                target.style.opacity = isOpen ? '0' : '1';
                target.style.marginTop = isOpen ? '0' : '10px';
            });
        });
    }

    function renderCollectionPayload(value) {
        if (Array.isArray(value.courses) && value.courses.every(isCourseLike)) {
            return `${renderCollectionSummary(value, 'courses')}${renderCourseList(value.courses)}`;
        }
        return null;
    }

    function renderQuestionCard(question, index) {
        const type = question.type || question.questionType || '题目';
        const content = question.content || question.questionText || question.title || '暂无题目内容';
        const options = Array.isArray(question.options) ? question.options : [];
        const answer = question.answer || question.correctAnswer || question.correctAnswers || '';
        const explanation = question.explanation || question.analysis || '';
        const metaItems = [
            question.difficulty ? `<span>难度：${escapeHtml(question.difficulty)}</span>` : '',
            question.score != null ? `<span>分值：${escapeHtml(question.score)}分</span>` : ''
        ].filter(Boolean).join('');

        return `
            <article class="agent-question-card">
                <div class="agent-question-header">
                    <span class="agent-question-index">${index + 1}</span>
                    <strong>${escapeHtml(type)}</strong>
                    ${metaItems ? `<div class="agent-question-meta">${metaItems}</div>` : ''}
                </div>
                <div class="agent-question-content">${renderAgentText(content)}</div>
                ${options.length ? `
                    <ol class="agent-question-options" type="A">
                        ${options.map(option => `<li class="agent-question-option">${renderAgentText(option)}</li>`).join('')}
                    </ol>
                ` : ''}
                ${answer ? `<div class="agent-question-answer"><strong>答案：</strong>${renderAgentText(answer)}</div>` : ''}
                ${explanation ? `<div class="agent-question-explanation"><strong>解析：</strong>${renderAgentText(explanation)}</div>` : ''}
            </article>
        `;
    }

    function renderQuestionList(questions) {
        return `<div class="agent-question-list">${questions.map((question, index) => renderQuestionCard(question, index)).join('')}</div>`;
    }

    function isQuestionBankPayload(value) {
        return value
            && typeof value === 'object'
            && !Array.isArray(value)
            && Array.isArray(value.questions)
            && value.questions.length > 0
            && hasValue(value.totalQuestions);
    }

    function isQuestionBankEnvelope(value) {
        return value
            && typeof value === 'object'
            && !Array.isArray(value)
            && isQuestionBankPayload(value.questionBank);
    }

    function renderQuestionBankPayload(value) {
        const summaryEntries = Object.entries(value)
            .filter(([key]) => !SKIP_META_KEYS.has(key))
            .filter(([key]) => !['questions', 'topics', 'difficultyBreakdown', 'topicDifficultyBreakdown'].includes(key))
            .filter(([, item]) => item != null && item !== '');
        const chipsHtml = buildMetaChips(summaryEntries);
        const detailHtml = summaryEntries.length
            ? `<dl class="agent-result-summary">
                ${summaryEntries.map(([key, item]) => `
                    <div>
                        <dt>${escapeHtml(formatAgentLabel(key))}</dt>
                        <dd>${renderValue(item)}</dd>
                    </div>
                `).join('')}
            </dl>`
            : '';
        const metaHtml = buildCollapsibleMeta(chipsHtml, detailHtml);
        return `${metaHtml}${renderQuestionList(value.questions)}`;
    }

    function renderQuestionBankEnvelope(value) {
        return renderQuestionBankPayload({
            ...value.questionBank,
            message: value.message || value.questionBank.message
        });
    }

    function extractLegacyQuestionPayload(value) {
        if (!value || typeof value !== 'object' || Array.isArray(value)) {
            return null;
        }
        if (Array.isArray(value.questions) && value.questions.every(isQuestionLike)) {
            return null;
        }
        const legacyQuestions = [
            value.aiResult?.questions,
            value.aiResult?.exam?.questions
        ].find(item => Array.isArray(item) && item.every(isQuestionLike));
        if (!legacyQuestions) {
            return null;
        }
        const source = value.aiResult?.exam && Array.isArray(value.aiResult.exam.questions)
            ? value.aiResult.exam
            : value.aiResult;
        return {
            ...source,
            ...value,
            questions: legacyQuestions
        };
    }

    function renderValue(value) {
        if (value == null) {
            return '<span class="text-muted">无</span>';
        }
        if (Array.isArray(value)) {
            if (value.length === 0) {
                return '<span class="text-muted">暂无数据</span>';
            }
            if (value.every(isQuestionLike)) {
                return renderQuestionList(value);
            }
            if (value.every(isCourseLike)) {
                return renderCourseList(value);
            }
            return `<ul class="agent-result-list">${value.map(item => `<li>${renderValue(item)}</li>`).join('')}</ul>`;
        }
        if (typeof value === 'object') {
            if (isCourseDetailPayload(value)) {
                return renderCourseDetailCard(value.course);
            }
            if (isQuestionBankEnvelope(value)) {
                return renderQuestionBankEnvelope(value);
            }
            if (isQuestionBankPayload(value)) {
                return renderQuestionBankPayload(value);
            }
            const collectionHtml = renderCollectionPayload(value);
            if (collectionHtml) {
                return collectionHtml;
            }
            if (Array.isArray(value.questions) && value.questions.every(isQuestionLike)) {
                const summaryEntries = Object.entries(value)
                    .filter(([key]) => !SKIP_META_KEYS.has(key))
                    .filter(([key]) => key !== 'questions')
                    .filter(([, item]) => item != null && item !== '');
                const chipsHtml = buildMetaChips(summaryEntries);
                const detailHtml = summaryEntries.length
                    ? `<dl class="agent-result-summary">
                        ${summaryEntries.map(([key, item]) => `
                            <div>
                                <dt>${escapeHtml(formatAgentLabel(key))}</dt>
                                <dd>${renderValue(item)}</dd>
                            </div>
                        `).join('')}
                    </dl>`
                    : '';
                const metaHtml = buildCollapsibleMeta(chipsHtml, detailHtml);
                return `${metaHtml}${renderQuestionList(value.questions)}`;
            }
            const legacyQuestions = extractLegacyQuestionPayload(value);
            if (legacyQuestions) {
                return renderValue(legacyQuestions);
            }
            if (isCourseLike(value)) {
                return renderCourseCard(value);
            }
            if (isQuestionLike(value)) {
                return renderQuestionCard(value, 0);
            }
            const entries = Object.entries(value)
                .filter(([key]) => !SKIP_META_KEYS.has(key));
            if (entries.length === 0) {
                return '<span class="text-muted">暂无数据</span>';
            }
            const chipsHtml = buildMetaChips(entries);
            const detailHtml = `
                <dl class="agent-result-map">
                    ${entries.map(([key, item]) => `
                        <div>
                            <dt>${escapeHtml(formatAgentLabel(key))}</dt>
                            <dd>${renderValue(item)}</dd>
                        </div>
                    `).join('')}
                </dl>
            `;
            return buildCollapsibleMeta(chipsHtml, detailHtml);
        }
        return renderScalarValue(value);
    }

    function isCourseDetailPayload(value) {
        return value
            && typeof value === 'object'
            && !Array.isArray(value)
            && value.status === 'EXECUTED'
            && isCourseLike(value.course);
    }

    function formatMessageTime(value = new Date()) {
        const date = value instanceof Date ? value : new Date(value);
        if (Number.isNaN(date.getTime())) {
            return '';
        }
        return date.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    }

    function createMessage(role, html, state) {
        const node = document.createElement('div');
        const now = new Date();
        const avatarLabel = role === 'user' ? '教师头像' : '学习助手头像';
        const avatarText = role === 'user' ? 'T' : 'AI';
        node.className = `agent-message agent-message-${role}${state ? ` agent-message-${state}` : ''}`;
        node.innerHTML = `
            <div class="agent-message-avatar" role="img" aria-label="${avatarLabel}">
                <span>${avatarText}</span>
            </div>
            <div class="agent-message-content">
                <div class="agent-message-body">${html}</div>
                <time class="agent-message-time" datetime="${now.toISOString()}">${formatMessageTime(now)}</time>
            </div>
        `;
        return node;
    }

    class AgentChatPanel {
        constructor(root, options = {}) {
            this.root = root;
            this.role = options.role || 'USER';
            this.sessionId = null;
            this.sessionStorageKey = getCurrentSessionStorageKey();
            this.messagesEl = root.querySelector('[data-agent-messages]');
            this.formEl = root.querySelector('[data-agent-form]');
            this.inputEl = root.querySelector('[data-agent-input]');
            this.submitEl = root.querySelector('[data-agent-submit]');
            this.isLoading = false;
            this.currentController = null;
            this.thinkingEl = null;
            this.restoreCurrentSessionId();
            this.bind();
        }

        bind() {
            if (!this.formEl || !this.inputEl) {
                return;
            }
            this.formEl.addEventListener('submit', (event) => {
                event.preventDefault();
                if (this.isLoading) {
                    this.pause();
                    return;
                }
                this.send(this.inputEl.value.trim());
            });
            this.submitEl?.addEventListener('click', (event) => {
                if (this.isLoading) {
                    event.preventDefault();
                    this.pause();
                }
            });
            this.root.addEventListener('click', (event) => {
                const button = event.target.closest('[data-agent-command]');
                if (!button || !this.root.contains(button) || this.isLoading) {
                    return;
                }
                const command = button.getAttribute('data-agent-command') || '';
                this.inputEl.value = command;
                this.send(command);
            });
            window.addEventListener('agent-session-selected', (event) => {
                const sessionId = event?.detail?.sessionId;
                const sessionLabel = event?.detail?.label || '';
                this.switchSession(sessionId, { announce: true, sessionLabel });
            });
        }

        append(role, html, state) {
            if (!this.messagesEl) {
                return null;
            }
            const message = createMessage(role, html, state);
            this.messagesEl.appendChild(message);
            this.scrollMessagesToBottom();
            return message;
        }

        appendEmpty(role, state) {
            return this.append(role, '', state);
        }

        scrollMessagesToBottom() {
            if (!this.messagesEl) {
                return;
            }
            this.messagesEl.scrollTop = this.messagesEl.scrollHeight;
            requestAnimationFrame(() => {
                if (!this.messagesEl) {
                    return;
                }
                this.messagesEl.scrollTop = this.messagesEl.scrollHeight;
            });
        }

        showThinking() {
            this.removeThinking();
            this.thinkingEl = this.append('agent', '<p>思考中...</p>', 'thinking');
        }

        removeThinking() {
            if (this.thinkingEl) {
                this.thinkingEl.remove();
                this.thinkingEl = null;
            }
        }

        pause() {
            if (this.currentController) {
                this.currentController.abort();
            }
        }

        restoreCurrentSessionId() {
            const sessionId = window.sessionStorage.getItem(this.sessionStorageKey);
            if (sessionId) {
                this.sessionId = sessionId;
                this.notifySessionChanged(sessionId);
            }
        }

        persistCurrentSessionId(sessionId) {
            if (!sessionId) {
                window.sessionStorage.removeItem(this.sessionStorageKey);
                return;
            }
            window.sessionStorage.setItem(this.sessionStorageKey, sessionId);
        }

        notifySessionChanged(sessionId) {
            if (!sessionId) {
                return;
            }
            window.dispatchEvent(new CustomEvent('agent-session-changed', {
                detail: { sessionId }
            }));
        }

        switchSession(sessionId, options = {}) {
            if (!sessionId || this.isLoading) {
                return;
            }
            this.sessionId = sessionId;
            this.persistCurrentSessionId(sessionId);
            this.notifySessionChanged(sessionId);
            if (this.inputEl) {
                this.inputEl.focus();
            }
            if (options.announce) {
                const label = options.sessionLabel ? `：${escapeHtml(options.sessionLabel)}` : '';
                this.append('agent', `<p class="text-muted">已切换到会话 #${escapeHtml(sessionId)}${label}，接下来会继续在这个会话中对话。</p>`);
            }
        }

        setLoading(isLoading) {
            this.isLoading = isLoading;
            if (this.submitEl) {
                this.submitEl.disabled = false;
                this.submitEl.classList.toggle('agent-submit-paused', isLoading);
                this.submitEl.innerHTML = isLoading
                    ? '<i class="fa fa-pause" aria-hidden="true"></i><span class="visually-hidden">暂停</span>'
                    : '<i class="fa fa-paper-plane" aria-hidden="true"></i><span class="visually-hidden">发送</span>';
            }
            if (this.inputEl) {
                this.inputEl.disabled = isLoading;
            }
        }

        buildPageContext() {
            const context = {
                page: document.body?.dataset?.page || document.documentElement?.dataset?.page || location.pathname
            };
            const selectedQuestion = window.agentSelectedQuestion || window.currentQuestion || null;
            if (selectedQuestion && typeof selectedQuestion === 'object') {
                if (selectedQuestion.id) {
                    context.selectedQuestionId = Number(selectedQuestion.id);
                    context.selectedQuestionIds = [Number(selectedQuestion.id)];
                }
                if (selectedQuestion.score || selectedQuestion.points) {
                    context.selectedQuestionScore = Number(selectedQuestion.score || selectedQuestion.points);
                }
                if (selectedQuestion.type || selectedQuestion.questionType) {
                    context.selectedQuestionType = selectedQuestion.type || selectedQuestion.questionType;
                }
                if (selectedQuestion.content || selectedQuestion.title) {
                    context.selectedQuestionContent = selectedQuestion.content || selectedQuestion.title;
                }
            }
            const selectedQuestionIds = window.agentSelectedQuestionIds || window.selectedQuestionIds;
            if (Array.isArray(selectedQuestionIds) && selectedQuestionIds.length > 0) {
                context.selectedQuestionIds = selectedQuestionIds
                    .map(item => Number(item))
                    .filter(item => Number.isFinite(item));
            }
            const currentCourseId = window.agentCurrentCourseId || window.currentCourseId;
            if (currentCourseId) {
                context.currentCourseId = Number(currentCourseId);
            }
            const currentClassId = window.agentCurrentClassId || window.currentClassId;
            if (currentClassId) {
                context.currentClassId = Number(currentClassId);
            }
            const questionFilter = window.agentQuestionFilter || window.currentQuestionFilter;
            if (questionFilter && typeof questionFilter === 'object') {
                context.questionFilter = { ...questionFilter };
            }
            return context;
        }

        async send(message) {
            if (!message) {
                return;
            }
            this.append('user', `<p>${escapeHtml(message)}</p>`);
            this.inputEl.value = '';
            this.currentController = new AbortController();
            this.setLoading(true);
            this.showThinking();
            try {
                await this.sendStream(message);
                this.removeThinking();
            } catch (error) {
                if (error?.name === 'AgentStreamFallback') {
                    try {
                        const requestBody = {
                            message,
                            sessionId: this.sessionId,
                            context: this.buildPageContext()
                        };
                        const payload = await this.request('/api/agent/chat', requestBody, this.currentController);
                        this.sessionId = payload.sessionId || this.sessionId;
                        this.persistCurrentSessionId(this.sessionId);
                        this.notifySessionChanged(this.sessionId);
                        this.removeThinking();
                        this.renderResponse(payload);
                        return;
                    } catch (fallbackError) {
                        this.removeThinking();
                        if (fallbackError?.name === 'AbortError') {
                            this.append('agent', '<p class="text-muted">已暂停本次请求。</p>');
                        } else {
                            this.append('agent', `<p class="text-danger">${escapeHtml(fallbackError.message || '请求失败')}</p>`);
                        }
                        return;
                    }
                }
                this.removeThinking();
                if (error?.name === 'AbortError') {
                    this.append('agent', '<p class="text-muted">已暂停本次请求。</p>');
                } else {
                    this.append('agent', `<p class="text-danger">${escapeHtml(error.message || '请求失败')}</p>`);
                }
            } finally {
                this.currentController = null;
                this.setLoading(false);
            }
        }

        async request(url, body, controller) {
            const api = getApiService();
            const response = api
                ? await api.request(url, {
                    method: 'POST',
                    body: JSON.stringify(body),
                    signal: controller?.signal
                })
                : await fetch(url, {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body),
                    signal: controller?.signal
                }).then(item => item.json());
            if (controller?.signal.aborted) {
                throw new DOMException('Request paused', 'AbortError');
            }
            if (!response || response.success === false) {
                throw new Error(response?.message || '请求失败');
            }
            return response.data || response;
        }

        async sendStream(message) {
            const payload = await this.requestStream('/api/agent/chat/stream', {
                message,
                sessionId: this.sessionId,
                context: this.buildPageContext()
            }, this.currentController);
            this.sessionId = payload.sessionId || this.sessionId;
            this.persistCurrentSessionId(this.sessionId);
            this.notifySessionChanged(this.sessionId);
            return payload;
        }

        async requestStream(url, body, controller) {
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream'
            };
            const csrfToken = typeof getCsrfToken === 'function' ? getCsrfToken() : null;
            const userId = window.sessionStorage.getItem('userId');
            const activeRole = window.sessionStorage.getItem('activeRole') || window.sessionStorage.getItem('role');
            const roles = window.sessionStorage.getItem('roles') || activeRole;
            if (csrfToken) {
                headers['X-XSRF-TOKEN'] = csrfToken;
            }
            if (userId) {
                headers['X-User-Id'] = userId;
            }
            if (activeRole) {
                headers['X-Active-Role'] = activeRole;
            }
            if (roles) {
                headers['X-Roles'] = roles;
            }
            const token = window.sessionStorage.getItem('token');
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const response = await fetch(url, {
                method: 'POST',
                credentials: 'include',
                headers,
                body: JSON.stringify(body),
                signal: controller?.signal
            });

            if (!response.ok) {
                if (response.status >= 500) {
                    const fallbackError = new Error('流式接口不可用');
                    fallbackError.name = 'AgentStreamFallback';
                    throw fallbackError;
                }
                throw new Error(await response.text().catch(() => '') || '请求失败');
            }

            if (!response.body) {
                const fallbackError = new Error('流式接口不可用');
                fallbackError.name = 'AgentStreamFallback';
                throw fallbackError;
            }

            return this.consumeSseResponse(response, controller);
        }

        async consumeSseResponse(response, controller) {
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let assistantMessage = this.thinkingEl;
            if (assistantMessage) {
                this.thinkingEl = null;
            } else {
                assistantMessage = this.appendEmpty('agent', 'thinking');
            }
            let assistantText = '';
            let finalPayload = null;
            let streamStarted = false;

            const updateAssistantText = (text) => {
                assistantText = text;
                if (!assistantMessage) {
                    assistantMessage = this.appendEmpty('agent');
                }
                assistantMessage.classList.remove('agent-message-thinking');
                const body = assistantMessage.querySelector('.agent-message-body');
                if (body) {
                    body.dataset.rawText = text;
                    body.innerHTML = `<p>${renderAgentText(text)}</p>`;
                }
            };

            const drainBuffer = () => {
                const blocks = buffer.split(/\r?\n\r?\n/);
                buffer = blocks.pop() || '';
                return blocks;
            };

            try {
                while (true) {
                    const { value, done } = await reader.read();
                    if (done) {
                        break;
                    }
                    streamStarted = true;
                    buffer += decoder.decode(value, { stream: true });
                    for (const block of drainBuffer()) {
                        const event = this.parseSseEvent(block);
                        if (!event) {
                            continue;
                        }
                        if (event.name === 'session' && event.data?.sessionId) {
                            this.sessionId = event.data.sessionId;
                            this.persistCurrentSessionId(this.sessionId);
                            this.notifySessionChanged(this.sessionId);
                            continue;
                        }
                        if (event.name === 'delta' && event.data?.text) {
                            updateAssistantText(assistantText + event.data.text);
                            continue;
                        }
                        if (event.name === 'result' && event.data) {
                            finalPayload = event.data;
                            if (finalPayload.sessionId) {
                                this.sessionId = finalPayload.sessionId;
                                this.persistCurrentSessionId(this.sessionId);
                                this.notifySessionChanged(this.sessionId);
                            }
                            if (finalPayload.responseType === 'TEXT') {
                                updateAssistantText(finalPayload.message || assistantText || '已处理');
                            } else {
                                if (assistantMessage) {
                                    assistantMessage.remove();
                                    assistantMessage = null;
                                }
                                this.renderResponse(finalPayload);
                            }
                            continue;
                        }
                        if (event.name === 'error') {
                            throw new Error(event.data?.message || '请求失败');
                        }
                    }
                    if (controller?.signal.aborted) {
                        throw new DOMException('Request paused', 'AbortError');
                    }
                }
            } catch (error) {
                if (!streamStarted) {
                    const fallbackError = new Error(error.message || '流式接口不可用');
                    fallbackError.name = 'AgentStreamFallback';
                    throw fallbackError;
                }
                throw error;
            } finally {
                reader.releaseLock?.();
            }

            if (finalPayload) {
                return finalPayload;
            }
            return {
                sessionId: this.sessionId,
                responseType: 'TEXT',
                message: assistantText || '已处理'
            };
        }

        parseSseEvent(block) {
            const lines = block.split(/\r?\n/);
            let name = '';
            const dataParts = [];
            for (const line of lines) {
                if (line.startsWith('event:')) {
                    name = line.slice(6).trim();
                } else if (line.startsWith('data:')) {
                    dataParts.push(line.slice(5).trim());
                }
            }
            if (!name) {
                return null;
            }
            const dataText = dataParts.join('\n');
            if (!dataText) {
                return { name, data: {} };
            }
            try {
                return { name, data: JSON.parse(dataText) };
            } catch (error) {
                return { name, data: { text: dataText } };
            }
        }

        renderResponse(payload) {
            if (payload.responseType === 'ACTION_PREVIEW') {
                this.renderPreview(payload.actionPreview);
                initMetaToggles(this.root);
                this.scrollMessagesToBottom();
                return;
            }
            if (payload.responseType === 'DATA') {
                this.append('agent', `
                    <p><strong>${renderAgentText(payload.message || '执行完成')}</strong></p>
                    <div class="agent-data-result">${renderValue(payload.data)}</div>
                `);
                initMetaToggles(this.root);
                this.scrollMessagesToBottom();
                return;
            }
            this.append('agent', `<p>${renderAgentText(payload.message || '已处理')}</p>`);
            this.scrollMessagesToBottom();
        }

        renderPreview(preview) {
            if (!preview) {
                this.append('agent', '<p class="text-warning">缺少操作预览。</p>');
                return;
            }
            const cardId = `agent-action-${preview.actionId}`;
            this.append('agent', `
                <div class="agent-action-card" id="${cardId}">
                    <div class="agent-action-header">
                        <div>
                            <strong>${escapeHtml(preview.title || preview.intent)}</strong>
                            <span class="agent-risk-badge">${escapeHtml(preview.riskLevel || 'LOW')}</span>
                        </div>
                        <span class="text-muted">#${escapeHtml(preview.actionId)}</span>
                    </div>
                    <p>${escapeHtml(preview.summary || '请确认是否执行该操作。')}</p>
                    <div class="agent-data-result">${renderValue(preview.preview || {})}</div>
                    <div class="agent-action-footer">
                        <button type="button" class="btn btn-sm btn-primary" data-agent-confirm="${escapeHtml(preview.actionId)}">
                            <i class="fa fa-check"></i> 确认执行
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-secondary" data-agent-dismiss>
                            取消
                        </button>
                    </div>
                </div>
            `);
            const card = this.root.querySelector(`#${cardId}`);
            const confirmButton = card?.querySelector('[data-agent-confirm]');
            const dismissButton = card?.querySelector('[data-agent-dismiss]');
            confirmButton?.addEventListener('click', () => this.confirm(preview, confirmButton));
            dismissButton?.addEventListener('click', () => this.cancel(preview, dismissButton, confirmButton, card));
            initMetaToggles(this.root);
            this.scrollMessagesToBottom();
        }

        async cancel(preview, dismissButton, confirmButton, card) {
            dismissButton.disabled = true;
            dismissButton.innerHTML = '<i class="fa fa-spinner fa-spin"></i> 取消中';
            try {
                const result = await this.request(`/api/agent/actions/${preview.actionId}/cancel`, {});
                if (card) {
                    card.classList.add('agent-action-cancelled');
                }
                if (confirmButton) {
                    confirmButton.disabled = true;
                }
                this.append('agent', `<p class="text-muted">${escapeHtml(result.message || '操作已取消。')}</p>`);
            } catch (error) {
                dismissButton.disabled = false;
                dismissButton.innerHTML = '取消';
                this.append('agent', `<p class="text-danger">${escapeHtml(error.message || '取消操作失败')}</p>`);
            }
        }

        async confirm(preview, button) {
            const card = button.closest('.agent-action-card');
            const dismissButton = card?.querySelector('[data-agent-dismiss]');
            const needsSecondConfirmation = button.dataset.agentSecondConfirmation === 'true';
            let secondConfirmationText = null;
            if (needsSecondConfirmation) {
                const phrase = button.dataset.agentSecondConfirmationPhrase || preview.secondConfirmationPhrase || '确认执行';
                const promptText = button.dataset.agentSecondConfirmationPrompt
                    || preview.secondConfirmationPrompt
                    || `该操作风险较高，请输入“${phrase}”完成二级确认。`;
                secondConfirmationText = global.prompt(promptText, '');
                if (secondConfirmationText !== phrase) {
                    this.append('agent', `<p class="text-warning">二级确认未通过，操作仍在等待确认。</p>`);
                    return;
                }
            }
            button.disabled = true;
            button.innerHTML = '<i class="fa fa-spinner fa-spin"></i> 执行中';
            if (dismissButton) {
                dismissButton.disabled = true;
            }
            try {
                const result = await this.request(`/api/agent/actions/${preview.actionId}/confirm`, {
                    idempotencyKey: preview.idempotencyKey,
                    secondConfirmationText
                });
                if (result.status === 'PENDING_SECOND_CONFIRMATION') {
                    const payload = result.result || {};
                    button.disabled = false;
                    button.dataset.agentSecondConfirmation = 'true';
                    button.dataset.agentSecondConfirmationPhrase = payload.secondConfirmationPhrase || preview.secondConfirmationPhrase || '确认执行';
                    button.dataset.agentSecondConfirmationPrompt = payload.secondConfirmationPrompt || result.message || '';
                    button.innerHTML = '<i class="fa fa-shield"></i> 二级确认';
                    this.append('agent', `
                        <p><strong>${escapeHtml(result.message || '需要二级确认')}</strong></p>
                        <div class="agent-data-result">${renderValue(payload)}</div>
                    `);
                    initMetaToggles(this.root);
                    this.scrollMessagesToBottom();
                    return;
                }
                const failed = result.status === 'FAILED';
                button.disabled = true;
                button.innerHTML = failed
                    ? '<i class="fa fa-times"></i> 执行失败'
                    : '<i class="fa fa-check"></i> 已执行';
                this.append('agent', `
                    <p><strong>${escapeHtml(result.message || '操作已执行')}</strong></p>
                    <div class="agent-data-result">${renderValue(result.result || {})}</div>
                `);
                initMetaToggles(this.root);
                this.scrollMessagesToBottom();
            } catch (error) {
                button.disabled = false;
                button.innerHTML = '<i class="fa fa-check"></i> 确认执行';
                if (dismissButton) {
                    dismissButton.disabled = false;
                }
                this.append('agent', `<p class="text-danger">${escapeHtml(error.message || '确认执行失败')}</p>`);
            }
        }
    }

    function initAll() {
        document.querySelectorAll('[data-agent-panel]').forEach((root) => {
            if (root.dataset.agentInitialized === 'true') {
                return;
            }
            root.dataset.agentInitialized = 'true';
            root.agentChatPanel = new AgentChatPanel(root, {
                role: root.getAttribute('data-agent-role') || 'USER'
            });
        });
    }

    global.AgentChatPanel = AgentChatPanel;
    global.initAgentChatPanels = initAll;
    document.addEventListener('DOMContentLoaded', initAll);
})(window);
