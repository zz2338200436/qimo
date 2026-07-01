(function attachAgentHistoryPanel(global) {
    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function normalizePayload(response) {
        if (response && typeof response === 'object' && 'success' in response && 'data' in response) {
            if (response.success === false) {
                throw new Error(response.message || '请求失败');
            }
            return response.data;
        }
        return response;
    }

    function parseAgentDate(value) {
        if (!value) {
            return null;
        }
        if (value instanceof Date) {
            return Number.isNaN(value.getTime()) ? null : value;
        }
        if (typeof value === 'string') {
            const trimmed = value.trim();
            const localMatch = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/);
            if (localMatch) {
                const [, year, month, day, hour, minute, second = '0', fraction = '0'] = localMatch;
                return new Date(
                    Number(year),
                    Number(month) - 1,
                    Number(day),
                    Number(hour),
                    Number(minute),
                    Number(second),
                    Number(fraction.slice(0, 3).padEnd(3, '0'))
                );
            }
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? null : date;
    }

    function formatTime(value) {
        if (!value) {
            return '未知时间';
        }
        const date = parseAgentDate(value);
        if (!date) {
            return String(value).replace('T', ' ');
        }
        return date.toLocaleString('zh-CN', { hour12: false });
    }

    function formatLabel(value) {
        return value ? String(value).replace(/_/g, ' ') : 'UNKNOWN';
    }

    function normalizeDate(value) {
        return parseAgentDate(value);
    }

    function formatRelativeGroup(date) {
        if (!date) {
            return '更早';
        }
        const now = new Date();
        const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const startOfTarget = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        const diffDays = Math.round((startOfToday - startOfTarget) / 86400000);
        if (diffDays <= 0) {
            return '今天';
        }
        if (diffDays === 1) {
            return '昨天';
        }
        if (diffDays < 7) {
            return '7天内';
        }
        return '更早';
    }

    function formatCompactTime(value) {
        const date = normalizeDate(value);
        if (!date) {
            return '未知时间';
        }
        return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' });
    }

    function firstPresent(...values) {
        return values.find((value) => value != null && String(value).trim() !== '') || '';
    }

    function normalizeIntent(value) {
        return String(value || '').trim().toUpperCase();
    }

    function getIntentPresentation(value) {
        const catalog = {
            GENERATE_QUESTIONS: { eyebrow: '课堂练习题', summary: '继续补充题型、难度或知识点要求。' },
            GENERATE_EXAM: { eyebrow: '试卷生成', summary: '继续细化课程、分值、时长或题量结构。' },
            LEARNING_SUGGESTIONS: { eyebrow: '学习建议', summary: '继续追问个性化辅导建议与后续安排。' },
            QUERY_LEARNING_SUMMARY: { eyebrow: '学情分析', summary: '继续查看班级、课程或知识点的学习情况。' },
            PUBLISH_ASSIGNMENT: { eyebrow: '作业发布', summary: '继续确认作业标题、课程与截止时间。' },
            PUBLISH_EXAM: { eyebrow: '考试发布', summary: '继续确认考试标题、时间与时长。' }
        };
        return catalog[normalizeIntent(value)] || { eyebrow: '教学对话', summary: '继续追问、改写或整理刚才的教学材料。' };
    }

    function getStatusLabel(status, isCurrent) {
        if (isCurrent) {
            return '当前';
        }
        const catalog = {
            PROCESSING: '生成中',
            EXECUTED: '已完成',
            COMPLETED: '已完成',
            WAITING_CONFIRMATION: '待确认',
            PENDING_SECOND_CONFIRMATION: '待确认',
            FAILED: '失败',
            CANCELLED: '已取消'
        };
        return catalog[String(status || '').trim().toUpperCase()] || '已保存';
    }

    function getSessionEyebrow(session, isCurrent) {
        if (isCurrent) {
            return '当前对话';
        }
        const intentInfo = getIntentPresentation(session.pendingIntent);
        return firstPresent(session.label, intentInfo.eyebrow, '教学对话');
    }

    function getSessionSummary(session) {
        if (session.summary) {
            return session.summary;
        }
        const artifactTitle = firstPresent(
            session.artifacts?.latestGeneratedTitle,
            session.artifacts?.title
        );
        if (artifactTitle) {
            return `最近内容：${artifactTitle}`;
        }
        const questionCount = session.artifacts?.latestGeneratedQuestionCount;
        if (questionCount) {
            return `最近生成 ${questionCount} 道题目，可继续调整难度、题型或解析。`;
        }
        return getIntentPresentation(session.pendingIntent).summary;
    }

    function getSessionMeta(session) {
        const timeText = formatCompactTime(session.updatedAt || session.createdAt);
        const roleText = session.userRole === 'STUDENT' ? '学生端' : '教师端';
        return `${timeText} · ${roleText}`;
    }

    function getSessionListTitle(session, isCurrent) {
        return session.title
            || session.artifacts?.customTitle
            || session.artifacts?.latestGeneratedTitle
            || session.summary
            || session.label
            || `${getIntentPresentation(session.pendingIntent).eyebrow}会话`;
    }

    function getCurrentSessionStorageKey() {
        return 'agent.currentSessionId';
    }

    function renderHistoryState(state, options = {}) {
        const title = options.title || '暂无数据';
        const description = options.description || '';
        const iconClass = options.iconClass || (state === 'error' ? 'fa-exclamation-circle' : state === 'loading' ? 'fa-spinner fa-spin' : 'fa-clock-o');
        const stateClass = state === 'error' ? 'error' : state === 'loading' ? 'loading' : 'empty';

        return `
            <div class="agent-history-state agent-history-state-${stateClass}" data-agent-history-state="${stateClass}">
                <div class="agent-history-state-icon">
                    <i class="fa ${escapeHtml(iconClass)}" aria-hidden="true"></i>
                </div>
                <div class="agent-history-state-body">
                    <div class="agent-history-state-title">${escapeHtml(title)}</div>
                    ${description ? `<div class="agent-history-state-text">${escapeHtml(description)}</div>` : ''}
                </div>
            </div>
        `;
    }

    class AgentHistoryPanel {
        constructor(root) {
            this.root = root;
            this.listEl = root.querySelector('[data-agent-history-list]');
            this.detailEl = root.querySelector('[data-agent-history-detail]');
            this.refreshEl = root.querySelector('[data-agent-history-refresh]');
            this.kickerEl = root.querySelector('[data-agent-history-kicker]');
            this.titleEl = root.querySelector('[data-agent-history-title]');
            this.copyEl = root.querySelector('[data-agent-history-copy]');
            this.toggleEls = Array.from(document.querySelectorAll('[data-agent-history-toggle]'));
            this.closeEl = root.querySelector('[data-agent-history-close]');
            this.backdropEl = document.querySelector('[data-agent-history-backdrop]');
            this.hoverZoneEl = document.querySelector('[data-agent-history-hover-zone]');
            this.searchToggleEls = Array.from(document.querySelectorAll('[data-agent-history-search-toggle]'));
            this.searchInputEl = root.querySelector('[data-agent-history-search-input]');
            this.newSessionEls = Array.from(document.querySelectorAll('[data-agent-history-new-session]'));
            this.currentSessionId = window.sessionStorage.getItem(getCurrentSessionStorageKey()) || null;
            this.hasLoadedSessions = false;
            this.searchTerm = '';
            this.sessions = [];
            this.lastAnchorEl = null;
            this.searchActive = false;
            this.openMenuSessionId = null;
            this.bind();
            this.renderIdleState();
            this.syncSearchUi();
        }

        bindHistoryPreviewOpeners() {
            document.querySelectorAll('[data-agent-history-preview-open]').forEach((button) => {
                button.addEventListener('click', () => this.open());
            });
        }

        bind() {
            this.refreshEl?.addEventListener('click', () => this.loadSessions());
            this.toggleEls.forEach((button) => button.addEventListener('click', () => {
                if (!this.root.contains(button)) {
                    this.lastAnchorEl = button;
                }
                this.toggle();
            }));
            this.closeEl?.addEventListener('click', () => this.close());
            this.backdropEl?.addEventListener('click', () => this.close());
            this.hoverZoneEl?.addEventListener('mouseenter', () => this.open());
            this.searchToggleEls.forEach((button) => {
                button.addEventListener('click', () => {
                    if (!this.root.contains(button)) {
                        this.lastAnchorEl = button;
                    }
                    this.toggleSearch();
                });
            });
            this.searchInputEl?.addEventListener('input', (event) => {
                this.searchTerm = String(event.target.value || '').trim().toLowerCase();
                this.renderSessions(this.sessions);
            });
            this.newSessionEls.forEach((button) => {
                button.addEventListener('click', () => this.startNewSession());
            });
            document.addEventListener('keydown', (event) => {
                if (event.key === 'Escape' && this.root.classList.contains('agent-history-open')) {
                    if (this.openMenuSessionId) {
                        this.closeSessionMenu();
                        return;
                    }
                    this.close();
                }
            });
            document.addEventListener('click', (event) => {
                if (!this.openMenuSessionId || this.root.contains(event.target)) {
                    return;
                }
                this.closeSessionMenu();
            });
            window.addEventListener('resize', () => {
                if (this.root.classList.contains('agent-history-open')) {
                    this.applyFloatingLayout();
                }
            });
            window.addEventListener('agent-session-changed', (event) => {
                this.currentSessionId = event?.detail?.sessionId || null;
                if (this.hasLoadedSessions) {
                    this.loadSessions();
                }
                this.syncCurrentSessionHighlight();
            });
            window.addEventListener('agent-session-cleared', () => {
                this.currentSessionId = null;
                if (this.hasLoadedSessions) {
                    this.loadSessions();
                }
                this.syncCurrentSessionHighlight();
            });
        }

        setRefreshLoading(isLoading) {
            if (!this.refreshEl) {
                return;
            }

            this.refreshEl.disabled = isLoading;
            this.refreshEl.innerHTML = isLoading
                ? '<i class="fa fa-spinner fa-spin"></i> 刷新中'
                : '<i class="fa fa-refresh"></i> 刷新';
        }

        open() {
            this.applyFloatingLayout();
            this.root.classList.add('agent-history-open');
            this.backdropEl?.classList.add('agent-history-open');
            this.toggleEls.forEach((button) => button.setAttribute('aria-expanded', 'true'));
            this.root.closest('[data-agent-shell]')?.classList.add('agent-history-open');
            this.syncSearchUi();
            this.root.focus();
            if (!this.hasLoadedSessions) {
                this.loadSessions();
            }
        }

        close() {
            this.root.classList.remove('agent-history-open');
            this.backdropEl?.classList.remove('agent-history-open');
            this.toggleEls.forEach((button) => button.setAttribute('aria-expanded', 'false'));
            this.root.closest('[data-agent-shell]')?.classList.remove('agent-history-open');
            Object.assign(this.root.style, {
                left: '',
                top: '',
                right: '',
                bottom: '',
                width: '',
                maxWidth: '',
                maxHeight: '',
                zIndex: '',
                position: ''
            });
            this.lastAnchorEl?.focus?.();
        }

        toggle() {
            if (this.root.classList.contains('agent-history-open')) {
                this.close();
                return;
            }
            this.open();
        }

        async request(url, options = {}) {
            const method = String(options.method || 'GET').toUpperCase();
            const api = global.apiService || null;
            if (api) {
                if (method === 'GET' && typeof api.get === 'function') {
                    return normalizePayload(await api.get(url));
                }
                if (method === 'PATCH' && typeof api.request === 'function') {
                    return normalizePayload(await api.request(url, {
                        method: 'PATCH',
                        body: JSON.stringify(options.body || {})
                    }));
                }
                if (method === 'DELETE' && typeof api.delete === 'function') {
                    return normalizePayload(await api.delete(url));
                }
            }
            const response = await fetch(url, {
                method,
                credentials: 'include',
                headers: {
                    'Accept': 'application/json',
                    ...(options.body ? { 'Content-Type': 'application/json' } : {})
                },
                ...(options.body ? { body: JSON.stringify(options.body) } : {})
            });
            const payload = await response.json().catch(() => null);
            if (!response.ok || payload?.success === false) {
                throw new Error(payload?.message || `请求失败（${response.status}）`);
            }
            return normalizePayload(payload);
        }

        setLoading(message) {
            if (this.listEl) {
                this.listEl.innerHTML = renderHistoryState('loading', {
                    title: '正在加载历史记录',
                    description: message || '正在获取最近会话，请稍候...'
                });
            }
        }

        renderIdleState() {
            if (this.listEl) {
                this.listEl.innerHTML = renderHistoryState('empty', {
                    title: '打开后查看历史',
                    description: '打开左侧边栏后，会在这里显示最近会话。'
                });
            }
            if (this.detailEl) {
                this.detailEl.innerHTML = renderHistoryState('empty', {
                    title: '尚未加载会话详情',
                    description: '选择一条会话后，这里会显示消息记录和操作详情。'
                });
            }
        }

        async loadSessions() {
            this.setRefreshLoading(true);
            this.setLoading('正在加载历史记录...');
            if (this.detailEl) {
                this.detailEl.innerHTML = renderHistoryState('empty', {
                    title: '请选择一条会话',
                    description: '左侧选择最近会话后，这里会显示消息与操作详情。'
                });
            }
            try {
                const sessions = await this.request('/api/agent/sessions');
                this.hasLoadedSessions = true;
                this.sessions = Array.isArray(sessions) ? sessions : [];
                this.renderSessions(this.sessions);
            } catch (error) {
                if (this.listEl) {
                    this.listEl.innerHTML = renderHistoryState('error', {
                        title: '历史记录加载失败',
                        description: error.message || '请稍后刷新重试。'
                    });
                }
            } finally {
                this.setRefreshLoading(false);
            }
        }

        renderSessions(sessions) {
            if (!this.listEl) {
                return;
            }
            const filtered = this.filterSessions(sessions);
            if (filtered.length === 0) {
                this.listEl.innerHTML = renderHistoryState('empty', {
                    title: this.searchTerm ? '没有找到匹配的会话' : '暂无历史记录',
                    description: this.searchTerm ? '试试别的关键词，或开启一段新对话。' : '完成一次对话后，最近会话会显示在这里。'
                });
                return;
            }
            const groups = this.groupSessions(filtered);
            let activeSessionId = this.currentSessionId && filtered.some((session) => String(session.sessionId) === String(this.currentSessionId))
                ? String(this.currentSessionId)
                : String(filtered[0].sessionId);
            this.listEl.innerHTML = groups.map((group) => `
                <section class="agent-history-group">
                    <h4 class="agent-history-group-label">${escapeHtml(group.label)}</h4>
                    ${group.sessions.map((session) => {
                        const sessionId = String(session.sessionId);
                        const isActive = sessionId === activeSessionId;
                        const title = getSessionListTitle(session, isActive);
                        const eyebrow = getSessionEyebrow(session, isActive);
                        const summary = getSessionSummary(session);
                        const status = getStatusLabel(session.status, this.currentSessionId === sessionId);
                        return `
                            <div class="agent-history-item${isActive ? ' active' : ''}" data-agent-history-row="${escapeHtml(sessionId)}">
                                <button type="button" class="agent-history-item-main" data-agent-history-session="${escapeHtml(sessionId)}" aria-label="继续会话：${escapeHtml(title)}">
                                    <span class="agent-history-item-eyebrow">${escapeHtml(eyebrow)}</span>
                                    <span class="agent-history-item-title">${escapeHtml(title)}</span>
                                    <span class="agent-history-item-summary">${escapeHtml(summary)}</span>
                                    <span class="agent-history-item-footer">
                                        <span class="agent-history-item-meta">${escapeHtml(getSessionMeta(session))}</span>
                                        <span class="agent-history-item-status">${escapeHtml(status)}</span>
                                    </span>
                                </button>
                                <div class="agent-history-item-menu-wrap">
                                    <button type="button" class="agent-history-item-more" data-agent-history-menu-toggle="${escapeHtml(sessionId)}" aria-haspopup="menu" aria-expanded="false" aria-label="更多操作：${escapeHtml(title)}">
                                        <i class="fa fa-ellipsis-h" aria-hidden="true"></i>
                                    </button>
                                    <div class="agent-history-item-menu" data-agent-history-menu="${escapeHtml(sessionId)}" role="menu" hidden>
                                        <button type="button" role="menuitem" data-agent-history-rename="${escapeHtml(sessionId)}">
                                            <i class="fa fa-pencil" aria-hidden="true"></i>
                                            <span>重命名会话</span>
                                        </button>
                                        <button type="button" role="menuitem" class="agent-history-menu-danger" data-agent-history-delete="${escapeHtml(sessionId)}">
                                            <i class="fa fa-trash-o" aria-hidden="true"></i>
                                            <span>删除会话</span>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        `;
                    }).join('')}
                </section>
            `).join('');
            this.listEl.querySelectorAll('[data-agent-history-session]').forEach((button) => {
                button.addEventListener('click', () => {
                    this.closeSessionMenu();
                    this.listEl.querySelectorAll('.agent-history-item').forEach(item => item.classList.remove('active'));
                    button.closest('.agent-history-item')?.classList.add('active');
                    const sessionId = button.getAttribute('data-agent-history-session');
                    const label = button.querySelector('.agent-history-item-title')?.textContent || '最近会话';
                    if (!sessionId) {
                        return;
                    }
                    window.dispatchEvent(new CustomEvent('agent-session-selected', {
                        detail: {
                            sessionId,
                            label
                        }
                    }));
                    this.close();
                });
            });
            this.listEl.querySelectorAll('[data-agent-history-menu-toggle]').forEach((button) => {
                button.addEventListener('click', (event) => {
                    event.stopPropagation();
                    const sessionId = button.getAttribute('data-agent-history-menu-toggle');
                    this.toggleSessionMenu(sessionId);
                });
            });
            this.listEl.querySelectorAll('[data-agent-history-rename]').forEach((button) => {
                button.addEventListener('click', (event) => {
                    event.stopPropagation();
                    this.renameSession(button.getAttribute('data-agent-history-rename'));
                });
            });
            this.listEl.querySelectorAll('[data-agent-history-delete]').forEach((button) => {
                button.addEventListener('click', (event) => {
                    event.stopPropagation();
                    this.deleteSession(button.getAttribute('data-agent-history-delete'));
                });
            });
            this.syncCurrentSessionHighlight();
        }

        toggleSessionMenu(sessionId) {
            if (!sessionId) {
                return;
            }
            if (this.openMenuSessionId === sessionId) {
                this.closeSessionMenu();
                return;
            }
            this.closeSessionMenu();
            const menu = this.listEl?.querySelector(`[data-agent-history-menu="${CSS.escape(sessionId)}"]`);
            const toggle = this.listEl?.querySelector(`[data-agent-history-menu-toggle="${CSS.escape(sessionId)}"]`);
            if (!menu || !toggle) {
                return;
            }
            menu.hidden = false;
            toggle.setAttribute('aria-expanded', 'true');
            this.openMenuSessionId = sessionId;
        }

        closeSessionMenu() {
            if (!this.listEl) {
                this.openMenuSessionId = null;
                return;
            }
            this.listEl.querySelectorAll('[data-agent-history-menu]').forEach((menu) => {
                menu.hidden = true;
            });
            this.listEl.querySelectorAll('[data-agent-history-menu-toggle]').forEach((button) => {
                button.setAttribute('aria-expanded', 'false');
            });
            this.openMenuSessionId = null;
        }

        findSession(sessionId) {
            return this.sessions.find((session) => String(session.sessionId) === String(sessionId)) || null;
        }

        async renameSession(sessionId) {
            const session = this.findSession(sessionId);
            if (!session) {
                return;
            }
            this.closeSessionMenu();
            const currentTitle = getSessionListTitle(session, String(session.sessionId) === String(this.currentSessionId));
            const nextTitle = window.prompt('请输入新的会话名称', currentTitle);
            if (nextTitle == null) {
                return;
            }
            const trimmedTitle = nextTitle.trim();
            if (!trimmedTitle) {
                window.alert('会话名称不能为空');
                return;
            }
            try {
                const updated = await this.request(`/api/agent/sessions/${encodeURIComponent(sessionId)}`, {
                    method: 'PATCH',
                    body: { title: trimmedTitle }
                });
                this.sessions = this.sessions.map((item) => String(item.sessionId) === String(sessionId)
                    ? { ...item, ...(updated || {}), title: updated?.title || trimmedTitle }
                    : item);
                this.renderSessions(this.sessions);
            } catch (error) {
                window.alert(error.message || '重命名失败，请稍后重试。');
            }
        }

        async deleteSession(sessionId) {
            const session = this.findSession(sessionId);
            if (!session) {
                return;
            }
            this.closeSessionMenu();
            const title = getSessionListTitle(session, String(session.sessionId) === String(this.currentSessionId));
            const confirmed = window.confirm(`确定删除会话“${title}”吗？删除后无法恢复。`);
            if (!confirmed) {
                return;
            }
            try {
                await this.request(`/api/agent/sessions/${encodeURIComponent(sessionId)}`, { method: 'DELETE' });
                const deletedCurrent = String(this.currentSessionId || '') === String(sessionId);
                this.sessions = this.sessions.filter((item) => String(item.sessionId) !== String(sessionId));
                if (deletedCurrent) {
                    this.startNewSession({ keepOpen: true });
                }
                this.renderSessions(this.sessions);
            } catch (error) {
                window.alert(error.message || '删除失败，请稍后重试。');
            }
        }

        filterSessions(sessions) {
            if (!this.searchTerm) {
                return sessions;
            }
            return sessions.filter((session) => {
                const source = [
                    getSessionListTitle(session, false),
                    session.pendingIntent,
                    session.status,
                    formatTime(session.updatedAt || session.createdAt)
                ].join(' ').toLowerCase();
                return source.includes(this.searchTerm);
            });
        }

        groupSessions(sessions) {
            const order = ['今天', '昨天', '7天内', '更早'];
            const bucket = new Map(order.map((label) => [label, []]));
            sessions.forEach((session) => {
                const label = formatRelativeGroup(normalizeDate(session.updatedAt || session.createdAt));
                bucket.get(label)?.push(session);
            });
            return order
                .map((label) => ({ label, sessions: bucket.get(label) || [] }))
                .filter((group) => group.sessions.length > 0);
        }

        syncCurrentSessionHighlight() {
            if (!this.listEl) {
                return;
            }
            this.listEl.querySelectorAll('[data-agent-history-session]').forEach((button) => {
                const sessionId = button.getAttribute('data-agent-history-session');
                const isCurrent = !!this.currentSessionId && sessionId === String(this.currentSessionId);
                const row = button.closest('.agent-history-item') || button;
                row.classList.toggle('agent-history-item-current', isCurrent);
                const eyebrowEl = button.querySelector('.agent-history-item-eyebrow');
                if (eyebrowEl) {
                    const fallbackEyebrow = eyebrowEl.dataset.fallbackEyebrow || eyebrowEl.textContent || '教学对话';
                    eyebrowEl.dataset.fallbackEyebrow = fallbackEyebrow;
                    eyebrowEl.textContent = isCurrent ? '当前对话' : fallbackEyebrow;
                }
                const statusEl = button.querySelector('.agent-history-item-status');
                if (statusEl) {
                    const fallbackStatus = statusEl.dataset.fallbackStatus || statusEl.textContent || '已保存';
                    statusEl.dataset.fallbackStatus = fallbackStatus;
                    statusEl.textContent = isCurrent ? '当前' : fallbackStatus;
                }
            });
        }

        async loadSessionDetail(sessionId) {
            if (!sessionId || !this.detailEl) {
                return;
            }
            this.detailEl.innerHTML = renderHistoryState('loading', {
                title: '正在加载会话详情',
                description: '正在获取消息记录与操作历史，请稍候...'
            });
            try {
                const session = await this.request(`/api/agent/sessions/${sessionId}`);
                this.renderDetail(session || {});
            } catch (error) {
                this.detailEl.innerHTML = renderHistoryState('error', {
                    title: '会话详情加载失败',
                    description: error.message || '请稍后刷新重试。'
                });
            }
        }

        toggleSearch() {
            const shell = this.root.closest('[data-agent-shell]');
            const shouldOpen = !this.root.classList.contains('agent-history-open');
            const nextActive = shouldOpen ? true : !this.searchActive;
            this.searchActive = nextActive;
            shell?.classList.toggle('agent-history-search-active', nextActive);
            this.syncSearchUi();
            if (shouldOpen) {
                this.open();
            }
            if (nextActive) {
                this.searchInputEl?.focus();
            } else if (this.searchInputEl) {
                this.searchInputEl.value = '';
                this.searchTerm = '';
                this.renderSessions(this.sessions);
            }
        }

        startNewSession(options = {}) {
            window.sessionStorage.removeItem(getCurrentSessionStorageKey());
            this.currentSessionId = null;
            window.dispatchEvent(new CustomEvent('agent-session-reset'));
            const shell = this.root.closest('[data-agent-shell]');
            this.searchActive = false;
            shell?.classList.remove('agent-history-search-active');
            this.syncSearchUi();
            if (!options.keepOpen) {
                this.close();
            }
        }

        syncSearchUi() {
            const shell = this.root.closest('[data-agent-shell]');
            shell?.classList.toggle('agent-history-search-active', this.searchActive);
            if (this.kickerEl) {
                this.kickerEl.textContent = this.searchActive ? '搜索会话' : '历史会话';
            }
            if (this.titleEl) {
                this.titleEl.textContent = this.searchActive ? '搜索历史会话' : '选择要继续的对话';
            }
            if (this.copyEl) {
                this.copyEl.textContent = this.searchActive
                    ? '输入标题、状态或时间，快速定位之前的教学记录'
                    : '按时间浏览最近的生成与追问记录';
            }
        }

        applyFloatingLayout() {
            const anchor = this.lastAnchorEl || this.toggleEls.find((button) => !this.root.contains(button)) || this.toggleEls[0];
            if (!anchor || typeof anchor.getBoundingClientRect !== 'function') {
                return;
            }

            const rect = anchor.getBoundingClientRect();
            const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 1280;
            const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 720;
            const width = Math.min(560, Math.max(320, viewportWidth - 24));
            const left = Math.max(12, Math.min(rect.right - width + 12, viewportWidth - width - 12));
            const top = Math.max(12, Math.min(rect.top - 12, viewportHeight - 120));
            const maxHeight = Math.max(320, viewportHeight - top - 12);

            Object.assign(this.root.style, {
                position: 'fixed',
                left: `${left}px`,
                top: `${top}px`,
                right: 'auto',
                bottom: 'auto',
                width: `${width}px`,
                maxWidth: `${width}px`,
                maxHeight: `${maxHeight}px`,
                height: `${maxHeight}px`,
                zIndex: '1082'
            });
        }

        renderDetail(session) {
            const messages = Array.isArray(session.messages) ? session.messages : [];
            const actions = Array.isArray(session.actions) ? session.actions : [];
            const sessionId = session.sessionId || '';
            const continueLabel = formatLabel(session.pendingIntent || '最近会话');
            this.detailEl.innerHTML = `
                <div class="agent-history-detail-header">
                    <div>
                        <h4>会话 #${escapeHtml(sessionId)}</h4>
                        <p>${escapeHtml(formatTime(session.createdAt))} - ${escapeHtml(formatTime(session.updatedAt))}</p>
                    </div>
                    <div class="agent-history-detail-actions">
                        <span class="agent-history-item-status">${escapeHtml(formatLabel(session.status))}</span>
                        <button type="button" class="btn btn-sm btn-primary" data-agent-history-continue="${escapeHtml(sessionId)}" data-agent-history-label="${escapeHtml(continueLabel)}">
                            继续此会话
                        </button>
                    </div>
                </div>
                <div class="agent-history-columns">
                    <div>
                        <h5>消息记录</h5>
                        ${this.renderMessages(messages)}
                    </div>
                    <div>
                        <h5>操作记录</h5>
                        ${this.renderActions(actions)}
                    </div>
                </div>
            `;
            const continueButton = this.detailEl.querySelector('[data-agent-history-continue]');
            continueButton?.addEventListener('click', () => {
                window.dispatchEvent(new CustomEvent('agent-session-selected', {
                    detail: {
                        sessionId,
                        label: continueLabel
                    }
                }));
                this.close();
            });
        }

        renderMessages(messages) {
            if (messages.length === 0) {
                return renderHistoryState('empty', {
                    title: '暂无消息',
                    description: '这个会话里还没有保存的消息记录。'
                });
            }
            return `<div class="agent-history-message-list">${messages.map(message => `
                <div class="agent-history-message">
                    <span>${escapeHtml(formatLabel(message.role))}</span>
                    <p>${escapeHtml(message.content || '')}</p>
                    <small>${escapeHtml(formatTime(message.createdAt))}</small>
                </div>
            `).join('')}</div>`;
        }

        renderActions(actions) {
            if (actions.length === 0) {
                return renderHistoryState('empty', {
                    title: '暂无操作',
                    description: '这个会话里还没有已确认的操作记录。'
                });
            }
            return `<div class="agent-history-action-list">${actions.map(action => `
                <div class="agent-history-action">
                    <div>
                        <strong>${escapeHtml(formatLabel(action.intent))}</strong>
                        <span>${escapeHtml(formatLabel(action.riskLevel))}</span>
                    </div>
                    <p>${escapeHtml(formatLabel(action.status))}</p>
                    <small>${escapeHtml(formatTime(action.updatedAt || action.createdAt))}</small>
                </div>
            `).join('')}</div>`;
        }
    }

    function initAll() {
        document.querySelectorAll('[data-agent-history-panel]').forEach((root) => {
            if (root.dataset.agentHistoryInitialized === 'true') {
                return;
            }
            root.dataset.agentHistoryInitialized = 'true';
            new AgentHistoryPanel(root);
        });
    }

    global.AgentHistoryPanel = AgentHistoryPanel;
    global.initAgentHistoryPanels = initAll;
    document.addEventListener('DOMContentLoaded', initAll);
})(window);
