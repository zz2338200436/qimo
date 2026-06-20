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

    function formatTime(value) {
        if (!value) {
            return '未知时间';
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value).replace('T', ' ');
        }
        return date.toLocaleString('zh-CN', { hour12: false });
    }

    function formatLabel(value) {
        return value ? String(value).replace(/_/g, ' ') : 'UNKNOWN';
    }

    function getSessionListTitle(session, isCurrent) {
        if (isCurrent) {
            return '当前会话';
        }
        return formatLabel(session.pendingIntent || '最近会话');
    }

    function getCurrentSessionStorageKey() {
        return 'agent.currentSessionId';
    }

    class AgentHistoryPanel {
        constructor(root) {
            this.root = root;
            this.listEl = root.querySelector('[data-agent-history-list]');
            this.detailEl = root.querySelector('[data-agent-history-detail]');
            this.refreshEl = root.querySelector('[data-agent-history-refresh]');
            this.toggleEl = document.querySelector('[data-agent-history-toggle]');
            this.closeEl = root.querySelector('[data-agent-history-close]');
            this.backdropEl = document.querySelector('[data-agent-history-backdrop]');
            this.hoverZoneEl = document.querySelector('[data-agent-history-hover-zone]');
            this.currentSessionId = window.sessionStorage.getItem(getCurrentSessionStorageKey()) || null;
            this.bind();
            this.loadSessions();
        }

        bind() {
            this.refreshEl?.addEventListener('click', () => this.loadSessions());
            this.toggleEl?.addEventListener('click', () => this.toggle());
            this.closeEl?.addEventListener('click', () => this.close());
            this.backdropEl?.addEventListener('click', () => this.close());
            this.hoverZoneEl?.addEventListener('mouseenter', () => this.open());
            document.addEventListener('keydown', (event) => {
                if (event.key === 'Escape' && this.root.classList.contains('agent-history-open')) {
                    this.close();
                }
            });
            window.addEventListener('agent-session-changed', (event) => {
                this.currentSessionId = event?.detail?.sessionId || null;
                this.syncCurrentSessionHighlight();
            });
        }

        open() {
            this.root.classList.add('agent-history-open');
            this.backdropEl?.classList.add('agent-history-open');
            this.toggleEl?.setAttribute('aria-expanded', 'true');
        }

        close() {
            this.root.classList.remove('agent-history-open');
            this.backdropEl?.classList.remove('agent-history-open');
            this.toggleEl?.setAttribute('aria-expanded', 'false');
        }

        toggle() {
            if (this.root.classList.contains('agent-history-open')) {
                this.close();
                return;
            }
            this.open();
        }

        async request(url) {
            const api = global.apiService || null;
            if (api && typeof api.get === 'function') {
                return normalizePayload(await api.get(url));
            }
            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            const payload = await response.json().catch(() => null);
            if (!response.ok || payload?.success === false) {
                throw new Error(payload?.message || `请求失败（${response.status}）`);
            }
            return normalizePayload(payload);
        }

        setLoading(message) {
            if (this.listEl) {
                this.listEl.innerHTML = `<div class="agent-history-empty">${escapeHtml(message || '正在加载历史记录...')}</div>`;
            }
        }

        async loadSessions() {
            this.setLoading('正在加载历史记录...');
            if (this.detailEl) {
                this.detailEl.innerHTML = '<div class="agent-history-empty">请选择一条会话查看详情。</div>';
            }
            try {
                const sessions = await this.request('/api/agent/sessions');
                this.renderSessions(Array.isArray(sessions) ? sessions : []);
            } catch (error) {
                if (this.listEl) {
                    this.listEl.innerHTML = `<div class="agent-history-empty text-danger">${escapeHtml(error.message || '历史记录加载失败')}</div>`;
                }
            }
        }

        renderSessions(sessions) {
            if (!this.listEl) {
                return;
            }
            if (sessions.length === 0) {
                this.listEl.innerHTML = '<div class="agent-history-empty">暂无历史记录。</div>';
                return;
            }
            this.listEl.innerHTML = sessions.map((session, index) => `
                <button type="button" class="agent-history-item${index === 0 ? ' active' : ''}" data-agent-history-session="${escapeHtml(session.sessionId)}">
                    <span class="agent-history-item-title">${escapeHtml(getSessionListTitle(session, false))}</span>
                    <span class="agent-history-item-meta">${escapeHtml(formatTime(session.updatedAt || session.createdAt))}</span>
                    <span class="agent-history-item-status">${escapeHtml(formatLabel(session.status))}</span>
                </button>
            `).join('');
            this.listEl.querySelectorAll('[data-agent-history-session]').forEach((button) => {
                button.addEventListener('click', () => {
                    this.listEl.querySelectorAll('.agent-history-item').forEach(item => item.classList.remove('active'));
                    button.classList.add('active');
                    this.loadSessionDetail(button.getAttribute('data-agent-history-session'));
                });
            });
            this.syncCurrentSessionHighlight();
            this.loadSessionDetail(sessions[0].sessionId);
        }

        syncCurrentSessionHighlight() {
            if (!this.listEl) {
                return;
            }
            this.listEl.querySelectorAll('[data-agent-history-session]').forEach((button) => {
                const sessionId = button.getAttribute('data-agent-history-session');
                const isCurrent = !!this.currentSessionId && sessionId === String(this.currentSessionId);
                button.classList.toggle('agent-history-item-current', isCurrent);
                const titleEl = button.querySelector('.agent-history-item-title');
                if (titleEl) {
                    const fallbackTitle = titleEl.dataset.fallbackTitle || titleEl.textContent || '最近会话';
                    titleEl.dataset.fallbackTitle = fallbackTitle;
                    titleEl.textContent = isCurrent ? '当前会话' : fallbackTitle;
                }
            });
        }

        async loadSessionDetail(sessionId) {
            if (!sessionId || !this.detailEl) {
                return;
            }
            this.detailEl.innerHTML = '<div class="agent-history-empty">正在加载会话详情...</div>';
            try {
                const session = await this.request(`/api/agent/sessions/${sessionId}`);
                this.renderDetail(session || {});
            } catch (error) {
                this.detailEl.innerHTML = `<div class="agent-history-empty text-danger">${escapeHtml(error.message || '会话详情加载失败')}</div>`;
            }
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
                return '<div class="agent-history-empty">暂无消息。</div>';
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
                return '<div class="agent-history-empty">暂无操作。</div>';
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
