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
            .replace(/\*\*([^*\n][^*\n]*?)\*\*/g, '<strong>$1</strong>')
            .replace(/`([^`\n]+?)`/g, '<code>$1</code>');
    }

    function formatAttachmentSize(size) {
        const bytes = Number(size);
        if (!Number.isFinite(bytes) || bytes <= 0) {
            return '';
        }
        if (bytes >= 1024 * 1024) {
            return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
        }
        if (bytes >= 1024) {
            return `${(bytes / 1024).toFixed(1)} KB`;
        }
        return `${bytes} B`;
    }

    function parseWebSearchResponse(value) {
        const text = String(value == null ? '' : value).trim();
        const sourcesMatch = text.match(/(?:^|\n)\s*(?:可参考的来源|以下是可参考的来源)(?:（([^）]+)）|\(([^)]+)\))?[：:]\s*/);
        if (!sourcesMatch) {
            const legacyHeaderMatch = text.match(/^联网搜索完成[，,]\s*以下是可参考的来源(?:（([^）]+)）|\(([^)]+)\))?[：:]\s*/);
            if (legacyHeaderMatch) {
                const legacyQuery = (legacyHeaderMatch[1] || legacyHeaderMatch[2] || '').trim();
                const legacyBody = text.slice(legacyHeaderMatch[0].length).trim();
                const parsed = parseWebSearchSources('', legacyQuery, legacyBody);
                return parsed ? {
                    ...parsed,
                    explanation: ''
                } : null;
            }
            if (!looksLikeLooseWebSearchResponse(text)) {
                return null;
            }
            const answerMatch = text.match(/^([^\n]+?)\n+(.*)$/s);
            if (!answerMatch) {
                return null;
            }
            const answerParts = splitWebSearchAnswer(answerMatch[1].trim());
            const parsed = parseWebSearchSources(answerParts.answer, '', answerMatch[2].trim());
            return parsed ? {
                ...parsed,
                explanation: answerParts.explanation || parsed.explanation || ''
            } : null;
        }

        const query = (sourcesMatch[1] || sourcesMatch[2] || '').trim();
        const markerIndex = text.indexOf(sourcesMatch[0]);
        const answerBlock = markerIndex >= 0 ? text.slice(0, markerIndex).trim() : '';
        const body = markerIndex >= 0 ? text.slice(markerIndex + sourcesMatch[0].length).trim() : '';
        const answerParts = splitWebSearchAnswer(answerBlock);
        const parsed = parseWebSearchSources(answerParts.answer, query, body);
        return parsed ? {
            ...parsed,
            explanation: answerParts.explanation || parsed.explanation || ''
        } : null;
    }

    function splitWebSearchAnswer(value) {
        const text = String(value || '').trim();
        if (!text) {
            return { answer: '', explanation: '' };
        }
        const lines = text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
        const answerLines = [];
        const explanationLines = [];
        let explanationStarted = false;
        for (const line of lines) {
            if (!explanationStarted && /^解释[：:]/.test(line)) {
                explanationStarted = true;
                explanationLines.push(line.replace(/^解释[：:]\s*/, '').trim());
                continue;
            }
            if (explanationStarted) {
                explanationLines.push(line);
                continue;
            }
            answerLines.push(line);
        }
        return {
            answer: answerLines.join(' ').trim(),
            explanation: explanationLines.join(' ').trim()
        };
    }

    function parseWebSearchSources(answer, query, body) {
        if (!body) {
            return {
                answer,
                query,
                explanation: '',
                sources: []
            };
        }

        const itemMatches = [...body.matchAll(/(?:^|\n)(\d+)\.\s+([^\n]+)\n([\s\S]*?)(?=(?:\n\d+\.\s+[^\n]+\n)|$)/g)];
        if (!itemMatches.length) {
            const fallbackParsed = parseLooseWebSearchSources(body);
            return fallbackParsed.sources.length ? {
                answer,
                query,
                explanation: fallbackParsed.explanation,
                sources: fallbackParsed.sources
            } : null;
        }

        const firstItemIndex = body.search(/(?:^|\n)\d+\.\s+[^\n]+\n/);
        const preface = firstItemIndex > 0 ? body.slice(0, firstItemIndex).trim() : '';
        const prefaceExplanation = preface.replace(/^解释[：:]\s*/, '').trim();

        const sources = itemMatches.map((match) => {
            const title = (match[2] || '').trim();
            const block = (match[3] || '').trim();
            const summaryMatch = block.match(/摘要[：:]\s*([\s\S]*?)(?=\n链接[：:]|\n$|$)/);
            const linkMatch = block.match(/链接[：:]\s*(https?:\/\/\S+)/);
            return {
                index: Number(match[1]),
                title,
                summary: summaryMatch ? summaryMatch[1].trim() : '',
                link: linkMatch ? linkMatch[1].trim() : ''
            };
        }).filter((item) => item.title || item.summary || item.link);

        return {
            answer,
            query,
            explanation: prefaceExplanation,
            sources
        };
    }

    function looksLikeLooseWebSearchResponse(text) {
        if (!text) {
            return false;
        }
        return /^\S.*\n+\d+\s*\n+\S+/s.test(text)
            && /https?:\/\/\S+/.test(text);
    }

    function parseLooseWebSearchSources(body) {
        const lines = String(body || '').split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
        const sources = [];
        const explanationLines = [];
        let current = null;

        const pushCurrent = () => {
            if (!current) {
                return;
            }
            current.summary = current.summary.trim();
            if (current.title || current.summary || current.link) {
                sources.push(current);
            }
            current = null;
        };

        for (const line of lines) {
            const indexOnlyMatch = line.match(/^(\d+)\s*$/);
            const indexedTitleMatch = line.match(/^(\d+)\.\s*(.+)$/);
            if (indexOnlyMatch) {
                pushCurrent();
                current = {
                    index: Number(indexOnlyMatch[1]),
                    title: '',
                    summary: '',
                    link: ''
                };
                continue;
            }
            if (indexedTitleMatch) {
                pushCurrent();
                current = {
                    index: Number(indexedTitleMatch[1]),
                    title: indexedTitleMatch[2].trim(),
                    summary: '',
                    link: ''
                };
                continue;
            }
            if (!current) {
                explanationLines.push(line.replace(/^解释[：:]\s*/, ''));
                continue;
            }
            if (!current.title) {
                current.title = line;
                continue;
            }

            const inlineLinkMatch = line.match(/链接[：:]\s*(https?:\/\/\S+)/);
            if (inlineLinkMatch) {
                current.link = current.link || inlineLinkMatch[1].trim();
                const summaryPart = line.replace(/链接[：:]\s*https?:\/\/\S+/, '').trim().replace(/[：:]\s*$/, '').trim();
                if (summaryPart) {
                    current.summary = `${current.summary} ${summaryPart}`.trim();
                }
                continue;
            }

            if (/^https?:\/\/\S+$/.test(line)) {
                current.link = current.link || line;
                continue;
            }

            current.summary = `${current.summary} ${line.replace(/^摘要[：:]\s*/, '')}`.trim();
        }

        pushCurrent();
        return {
            explanation: explanationLines.join(' ').trim(),
            sources
        };
    }

    function renderWebSearchResponse(value) {
        const parsed = parseWebSearchResponse(value);
        if (!parsed) {
            return null;
        }

        const summaryHtml = parsed.answer
            ? `<p class="agent-web-search-summary">${escapeHtml(parsed.answer)}</p>`
            : (parsed.sources.length
                ? `<p class="agent-web-search-summary">已完成联网搜索${parsed.query ? `：<strong>${escapeHtml(parsed.query)}</strong>` : ''}。下面是可参考的来源。</p>`
                : `<p class="agent-web-search-summary">已完成联网搜索${parsed.query ? `：<strong>${escapeHtml(parsed.query)}</strong>` : ''}。</p>`);

        const explanationHtml = parsed.explanation
            ? `<p class="agent-web-search-explanation">解释：${escapeHtml(parsed.explanation)}</p>`
            : '';

        const sourcesHtml = parsed.sources.length
            ? `
                <details class="agent-web-search-sources">
                    <summary>相关网址与来源</summary>
                    <div class="agent-web-search-results">
                        ${parsed.sources.map((source) => `
                            <article class="agent-web-search-result">
                                <div class="agent-web-search-result-index">${escapeHtml(source.index)}</div>
                                <div class="agent-web-search-result-body">
                                    <h4>${source.link
                                        ? `<a href="${escapeHtml(source.link)}" target="_blank" rel="noopener noreferrer">${escapeHtml(source.title || source.link)}</a>`
                                        : escapeHtml(source.title || `来源 ${source.index}`)}</h4>
                                    ${source.summary ? `<p>${escapeHtml(source.summary)}</p>` : ''}
                                    ${source.link ? `<div class="agent-web-search-result-link"><a href="${escapeHtml(source.link)}" target="_blank" rel="noopener noreferrer">${escapeHtml(source.link)}</a></div>` : ''}
                                </div>
                            </article>
                        `).join('')}
                    </div>
                </details>
            `
            : '';

        return `
            <section class="agent-web-search-response" aria-label="联网搜索结果">
                ${summaryHtml}
                ${explanationHtml}
                ${sourcesHtml}
            </section>
        `;
    }

    function isStructuredWebSearchPayload(value) {
        if (!value || typeof value !== 'object' || Array.isArray(value)) {
            return false;
        }
        return Array.isArray(value.results)
            && value.results.some((item) => item && typeof item === 'object'
                && (item.title || item.url || item.snippet));
    }

    function renderStructuredWebSearchResponse(value, fallbackMessage) {
        if (!isStructuredWebSearchPayload(value)) {
            return null;
        }
        const query = String(value.query || '').trim();
        const explanation = String(value.explanation || '').trim();
        const fallbackParsed = parseWebSearchResponse(fallbackMessage);
        const preferredSummary = [value.answer, value.content, value.summary, value.message]
            .find((item) => item != null
                && typeof item !== 'object'
                && String(item).trim()
                && !isGenericResponseMessage(String(item).trim()));
        const fallbackSummary = String(fallbackMessage || '').trim();
        const summaryText = String(
            preferredSummary
            || fallbackParsed?.answer
            || (fallbackSummary && !isGenericResponseMessage(fallbackSummary) ? fallbackSummary : '')
        ).trim();
        const explanationText = explanation || fallbackParsed?.explanation || '';
        const summaryHtml = summaryText
            ? `<p class="agent-web-search-summary">${escapeHtml(summaryText)}</p>`
            : `<p class="agent-web-search-summary">已完成联网搜索${query ? `：<strong>${escapeHtml(query)}</strong>` : ''}。</p>`;
        const explanationHtml = explanationText
            ? `<p class="agent-web-search-explanation">解释：${escapeHtml(explanationText)}</p>`
            : '';
        const sourcesHtml = value.results.length
            ? `
                <details class="agent-web-search-sources">
                    <summary>相关网址与来源${query ? `：${escapeHtml(query)}` : ''}</summary>
                    <div class="agent-web-search-results">
                        ${value.results.map((source, index) => {
                            const title = String(source.title || source.url || `来源 ${index + 1}`).trim();
                            const link = String(source.url || '').trim();
                            const summary = String(source.snippet || source.summary || '').trim();
                            return `
                                <article class="agent-web-search-result">
                                    <div class="agent-web-search-result-index">${escapeHtml(index + 1)}</div>
                                    <div class="agent-web-search-result-body">
                                        <h4>${link
                                            ? `<a href="${escapeHtml(link)}" target="_blank" rel="noopener noreferrer">${escapeHtml(title)}</a>`
                                            : escapeHtml(title)}</h4>
                                        ${summary ? `<p>${escapeHtml(summary)}</p>` : ''}
                                        ${link ? `<div class="agent-web-search-result-link"><a href="${escapeHtml(link)}" target="_blank" rel="noopener noreferrer">${escapeHtml(link)}</a></div>` : ''}
                                    </div>
                                </article>
                            `;
                        }).join('')}
                    </div>
                </details>
            `
            : '';

        return `
            <section class="agent-web-search-response" aria-label="联网搜索结果">
                ${summaryHtml}
                ${explanationHtml}
                ${sourcesHtml}
            </section>
        `;
    }

    function renderAgentRichText(value) {
        const webSearchHtml = renderWebSearchResponse(value);
        if (webSearchHtml) {
            return webSearchHtml;
        }

        const lines = String(value == null ? '' : value).split(/\r?\n/);
        const blocks = [];
        let unorderedListItems = [];
        let orderedListItems = [];
        let blockquoteLines = [];
        let codeFenceLang = '';
        let codeFenceLines = [];

        const formatInline = (text) => escapeHtml(text)
            .replace(/\*\*([^*\n][^*\n]*?)\*\*/g, '<strong>$1</strong>')
            .replace(/`([^`\n]+?)`/g, '<code>$1</code>');

        const flushUnorderedList = () => {
            if (unorderedListItems.length === 0) {
                return;
            }
            blocks.push(`<ul>${unorderedListItems.map(item => `<li>${item}</li>`).join('')}</ul>`);
            unorderedListItems = [];
        };

        const flushOrderedList = () => {
            if (orderedListItems.length === 0) {
                return;
            }
            blocks.push(`<ol>${orderedListItems.map(item => `<li>${item}</li>`).join('')}</ol>`);
            orderedListItems = [];
        };

        const flushBlockquote = () => {
            if (blockquoteLines.length === 0) {
                return;
            }
            const content = blockquoteLines
                .map(line => `<p>${formatInline(line)}</p>`)
                .join('');
            blocks.push(`<blockquote>${content}</blockquote>`);
            blockquoteLines = [];
        };

        const flushCodeFence = () => {
            if (codeFenceLines.length === 0 && !codeFenceLang) {
                return;
            }
            const langClass = codeFenceLang ? ` class="language-${escapeHtml(codeFenceLang)}"` : '';
            blocks.push(`<pre><code${langClass}>${escapeHtml(codeFenceLines.join('\n'))}</code></pre>`);
            codeFenceLang = '';
            codeFenceLines = [];
        };

        const flushAll = () => {
            flushUnorderedList();
            flushOrderedList();
            flushBlockquote();
        };

        const parseTable = (startIndex) => {
            const headerLine = lines[startIndex];
            const separatorLine = lines[startIndex + 1];
            if (!headerLine || !separatorLine) {
                return null;
            }
            if (!/^\s*\|?(\s*:?-{3,}:?\s*\|)+\s*:?-{3,}:?\s*\|?\s*$/.test(separatorLine)) {
                return null;
            }
            const tableLines = [headerLine, separatorLine];
            let cursor = startIndex + 2;
            while (cursor < lines.length && /^\s*\|.*\|\s*$/.test(lines[cursor])) {
                tableLines.push(lines[cursor]);
                cursor += 1;
            }

            const toCells = (line) => line
                .trim()
                .replace(/^\|/, '')
                .replace(/\|$/, '')
                .split('|')
                .map(cell => formatInline(cell.trim()));

            const headerCells = toCells(tableLines[0]);
            const bodyRows = tableLines.slice(2).map((line) => toCells(line));
            const html = `
                <div class="agent-md-table-wrap">
                    <table class="agent-md-table">
                        <thead><tr>${headerCells.map(cell => `<th>${cell}</th>`).join('')}</tr></thead>
                        <tbody>${bodyRows.map(row => `<tr>${row.map(cell => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody>
                    </table>
                </div>
            `;
            return { html, nextIndex: cursor - 1 };
        };

        for (let index = 0; index < lines.length; index += 1) {
            const line = lines[index];
            const trimmed = line.trim();

            const fenceMatch = trimmed.match(/^```(\S+)?\s*$/);
            if (fenceMatch) {
                if (codeFenceLines.length > 0 || codeFenceLang) {
                    flushCodeFence();
                } else {
                    flushAll();
                    codeFenceLang = fenceMatch[1] || '';
                }
                continue;
            }

            if (codeFenceLang || codeFenceLines.length > 0) {
                codeFenceLines.push(line);
                continue;
            }

            if (!trimmed) {
                flushAll();
                continue;
            }

            const table = parseTable(index);
            if (table) {
                flushAll();
                blocks.push(table.html);
                index = table.nextIndex;
                continue;
            }

            const heading = trimmed.match(/^#{1,4}\s+(.+)$/);
            if (heading) {
                flushAll();
                blocks.push(`<h4>${formatInline(heading[1])}</h4>`);
                continue;
            }

            const blockquote = trimmed.match(/^>\s?(.*)$/);
            if (blockquote) {
                flushUnorderedList();
                flushOrderedList();
                blockquoteLines.push(blockquote[1] || '');
                continue;
            }
            flushBlockquote();

            const bullet = trimmed.match(/^[-*]\s+(.+)$/);
            if (bullet) {
                flushOrderedList();
                unorderedListItems.push(formatInline(bullet[1]));
                continue;
            }

            const ordered = trimmed.match(/^\d+\.\s+(.+)$/);
            if (ordered) {
                flushUnorderedList();
                orderedListItems.push(formatInline(ordered[1]));
                continue;
            }

            flushAll();
            blocks.push(`<p>${formatInline(trimmed)}</p>`);
        }

        flushAll();
        flushCodeFence();

        return blocks.join('') || '<p></p>';
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

    function normalizeActionStatus(value) {
        return String(value || '').trim().toUpperCase();
    }

    function isTerminalActionStatus(value) {
        return ['EXECUTED', 'FAILED', 'CANCELLED', 'EXPIRED'].includes(normalizeActionStatus(value));
    }

    function buildActionState(action) {
        if (!action || typeof action !== 'object') {
            return null;
        }
        const result = action.result && typeof action.result === 'object' ? action.result : {};
        return {
            actionId: action.actionId,
            sessionId: action.sessionId,
            intent: action.intent,
            status: normalizeActionStatus(action.status),
            riskLevel: action.riskLevel,
            result,
            message: result.message || action.errorMessage || AGENT_VALUE_LABELS[normalizeActionStatus(action.status)] || '操作已更新',
            errorMessage: action.errorMessage,
            createdAt: action.createdAt,
            updatedAt: action.updatedAt,
            executedAt: action.executedAt,
            confirmedAt: action.confirmedAt
        };
    }

    function buildActionLookup(actions) {
        const lookup = new Map();
        if (!Array.isArray(actions)) {
            return lookup;
        }
        actions.forEach((action) => {
            const actionId = action?.actionId;
            if (actionId == null) {
                return;
            }
            lookup.set(String(actionId), buildActionState(action));
        });
        return lookup;
    }

    function buildHistoryReplayPayload(message, actionLookup = new Map()) {
        const metadata = message?.metadata;
        if (!metadata || typeof metadata !== 'object') {
            return null;
        }
        const responseType = metadata.responseType;
        if (responseType === 'DATA' && Object.prototype.hasOwnProperty.call(metadata, 'data')) {
            return {
                sessionId: message.sessionId,
                responseType,
                message: message.content,
                data: metadata.data
            };
        }
        if (responseType === 'ACTION_PREVIEW' && metadata.actionPreview) {
            const actionState = actionLookup.get(String(metadata.actionPreview.actionId)) || null;
            return {
                sessionId: message.sessionId,
                responseType,
                message: message.content,
                actionPreview: metadata.actionPreview,
                actionState
            };
        }
        return null;
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

    function getClassId(item) {
        return item?.id ?? item?.classId ?? item?.class_id ?? null;
    }

    function isClassLike(value) {
        return value && typeof value === 'object' && !Array.isArray(value)
            && hasValue(getClassId(value))
            && ('className' in value || 'name' in value || 'classCode' in value)
            && ('studentCount' in value || 'year' in value || 'majorName' in value || 'courseName' in value);
    }

    function renderClassMetaItem(label, value) {
        if (value == null || value === '') {
            return '';
        }
        return `
            <div class="agent-class-meta-item">
                <dt>${escapeHtml(label)}</dt>
                <dd>${renderValue(value)}</dd>
            </div>
        `;
    }

    function renderClassCard(item) {
        const classId = getClassId(item);
        const className = item.className || item.name || item.classCode || `班级 ${classId || ''}`.trim();
        const majorName = item.majorName || '';
        const courseName = item.courseName || '';
        const meta = [
            renderClassMetaItem('班级ID', classId),
            renderClassMetaItem('年级', item.year),
            renderClassMetaItem('学生数', item.studentCount),
            renderClassMetaItem('容量', item.capacity),
            renderClassMetaItem('专业', majorName),
            renderClassMetaItem('课程', courseName),
            renderClassMetaItem('教师', item.teacherName)
        ].filter(Boolean).join('');

        return `
            <article class="agent-class-card">
                <div class="agent-class-card-header">
                    <div>
                        <strong>${escapeHtml(className)}</strong>
                        ${(majorName || courseName) ? `<p>${escapeHtml([majorName, courseName].filter(Boolean).join(' · '))}</p>` : ''}
                    </div>
                    ${item.studentCount != null ? `<span class="agent-class-badge">${escapeHtml(item.studentCount)} 人</span>` : ''}
                </div>
                ${meta ? `<dl class="agent-class-meta">${meta}</dl>` : ''}
            </article>
        `;
    }

    function renderClassList(classes) {
        return `<div class="agent-class-list">${classes.map(renderClassCard).join('')}</div>`;
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
        if (Array.isArray(value.classes) && value.classes.every(isClassLike)) {
            return `${renderCollectionSummary(value, 'classes')}${renderClassList(value.classes)}`;
        }
        if (isStudentAssignmentScorePayload(value)) {
            return renderStudentAssignmentScorePayload(value);
        }
        if (isStudentExamScorePayload(value)) {
            return renderStudentExamScorePayload(value);
        }
        if (isPendingAssignmentPayload(value)) {
            return renderPendingAssignmentPayload(value);
        }
        return null;
    }

    function isAssignmentLike(value) {
        return value
            && typeof value === 'object'
            && !Array.isArray(value)
            && hasValue(value.id)
            && hasValue(value.title)
            && ('courseName' in value || 'dueDate' in value || 'submission' in value);
    }

    function isPendingAssignmentPayload(value) {
        const items = value?.pendingAssignments?.content;
        return Array.isArray(items) && items.every(isAssignmentLike);
    }

    function isStudentAssignmentScoreLike(value) {
        return value
            && typeof value === 'object'
            && !Array.isArray(value)
            && hasValue(value.relatedId)
            && hasValue(value.title)
            && ('score' in value || 'totalScore' in value || 'courseName' in value || 'submitDate' in value || 'completedAt' in value);
    }

    function isStudentAssignmentScorePayload(value) {
        const items = value?.assignments;
        return Array.isArray(items) && items.length > 0 && items.every(isStudentAssignmentScoreLike);
    }

    function isStudentExamScoreLike(value) {
        return value
            && typeof value === 'object'
            && !Array.isArray(value)
            && hasValue(value.relatedId)
            && hasValue(value.title)
            && ('score' in value || 'totalScore' in value || 'courseName' in value || 'completedAt' in value || 'examDate' in value);
    }

    function isStudentExamScorePayload(value) {
        const items = value?.exams;
        return Array.isArray(items) && items.length > 0 && items.every(isStudentExamScoreLike);
    }

    function renderPendingAssignmentCard(assignment) {
        const submitted = Boolean(assignment?.submission);
        const statusLabel = submitted ? '已提交' : '待提交';
        const statusClass = submitted ? 'submitted' : 'pending';
        const meta = [
            assignment.courseName ? `<span>课程：${escapeHtml(assignment.courseName)}</span>` : '',
            assignment.dueDate ? `<span>截止：${escapeHtml(assignment.dueDate)}</span>` : '',
            assignment.id != null ? `<span>作业ID：${escapeHtml(assignment.id)}</span>` : ''
        ].filter(Boolean).join('');

        return `
            <article class="agent-course-card agent-assignment-card">
                <div class="agent-course-card-header">
                    <div>
                        <strong>${escapeHtml(assignment.title || '未命名作业')}</strong>
                        <p class="agent-assignment-status agent-assignment-status-${statusClass}">${statusLabel}</p>
                    </div>
                </div>
                ${meta ? `<div class="agent-question-draft-summary">${meta}</div>` : ''}
            </article>
        `;
    }

    function renderPendingAssignmentPayload(value) {
        const page = value.pendingAssignments || {};
        const items = Array.isArray(page.content) ? page.content : [];
        const pendingCount = items.filter(item => !item?.submission).length;
        const summary = renderCollectionSummary({
            totalElements: page.totalElements ?? items.length,
            numberOfElements: page.numberOfElements ?? items.length,
            pendingCount
        }, 'pendingAssignments');
        return `
            ${summary}
            <div class="agent-course-list">
                ${items.map(renderPendingAssignmentCard).join('')}
            </div>
        `;
    }

    function renderStudentAssignmentScoreCard(assignment) {
        const score = assignment?.score;
        const totalScore = assignment?.totalScore;
        const scoreText = score != null
            ? `${escapeHtml(score)}${totalScore != null ? ` / ${escapeHtml(totalScore)}` : ''}`
            : '未出分';
        const meta = [
            assignment.courseName ? `<span>课程：${escapeHtml(assignment.courseName)}</span>` : '',
            assignment.relatedId != null ? `<span>作业ID：${escapeHtml(assignment.relatedId)}</span>` : '',
            assignment.submitDate ? `<span>提交：${escapeHtml(assignment.submitDate)}</span>` : '',
            assignment.completedAt ? `<span>完成：${escapeHtml(assignment.completedAt)}</span>` : '',
            assignment.rank != null ? `<span>排名：${escapeHtml(assignment.rank)}</span>` : ''
        ].filter(Boolean).join('');

        return `
            <article class="agent-course-card agent-assignment-card">
                <div class="agent-course-card-header">
                    <div>
                        <strong>${escapeHtml(assignment.title || '未命名作业')}</strong>
                        <p class="agent-assignment-status agent-assignment-status-submitted">成绩：${scoreText}</p>
                    </div>
                </div>
                ${meta ? `<div class="agent-question-draft-summary">${meta}</div>` : ''}
            </article>
        `;
    }

    function renderStudentAssignmentScorePayload(value) {
        const items = Array.isArray(value.assignments) ? value.assignments : [];
        const gradedCount = items.filter(item => item?.score != null).length;
        const summary = renderCollectionSummary({
            totalElements: items.length,
            gradedCount
        }, 'assignments');
        return `
            ${summary}
            <div class="agent-course-list">
                ${items.map(renderStudentAssignmentScoreCard).join('')}
            </div>
        `;
    }

    function renderStudentExamScoreCard(exam) {
        const score = exam?.score;
        const totalScore = exam?.totalScore ?? exam?.total;
        const scoreText = score != null
            ? `${escapeHtml(score)}${totalScore != null ? ` / ${escapeHtml(totalScore)}` : ''}`
            : '未出分';
        const meta = [
            exam.courseName ? `<span>课程：${escapeHtml(exam.courseName)}</span>` : '',
            exam.relatedId != null ? `<span>考试ID：${escapeHtml(exam.relatedId)}</span>` : '',
            exam.completedAt ? `<span>完成：${escapeHtml(exam.completedAt)}</span>` : '',
            exam.examDate ? `<span>考试时间：${escapeHtml(exam.examDate)}</span>` : '',
            exam.rank != null ? `<span>排名：${escapeHtml(exam.rank)}</span>` : ''
        ].filter(Boolean).join('');

        return `
            <article class="agent-course-card agent-assignment-card">
                <div class="agent-course-card-header">
                    <div>
                        <strong>${escapeHtml(exam.title || exam.examName || '未命名考试')}</strong>
                        <p class="agent-assignment-status agent-assignment-status-submitted">成绩：${scoreText}</p>
                    </div>
                </div>
                ${meta ? `<div class="agent-question-draft-summary">${meta}</div>` : ''}
            </article>
        `;
    }

    function renderStudentExamScorePayload(value) {
        const items = Array.isArray(value.exams) ? value.exams : [];
        const gradedCount = items.filter(item => item?.score != null).length;
        const summary = renderCollectionSummary({
            totalElements: items.length,
            gradedCount
        }, 'exams');
        return `
            ${summary}
            <div class="agent-course-list">
                ${items.map(renderStudentExamScoreCard).join('')}
            </div>
        `;
    }

    function isStructuredDataPayload(value) {
        if (value == null) {
            return false;
        }
        if (Array.isArray(value)) {
            return value.length > 0 && (
                value.every(isQuestionLike)
                || value.every(isCourseLike)
                || value.every(isClassLike)
                || value.every(isStudentAssignmentScoreLike)
                || value.every(isStudentExamScoreLike)
            );
        }
        if (typeof value !== 'object') {
            return false;
        }
        return Boolean(
            isCourseDetailPayload(value)
            || isQuestionBankEnvelope(value)
            || isQuestionBankPayload(value)
            || renderCollectionPayload(value)
            || (Array.isArray(value.questions) && value.questions.every(isQuestionLike))
            || extractLegacyQuestionPayload(value)
            || isCourseLike(value)
            || isClassLike(value)
            || isQuestionLike(value)
        );
    }

    function isGenericResponseMessage(value) {
        const text = String(value == null ? '' : value).trim().replace(/[。.!！]+$/, '');
        return !text || ['已处理', '执行完成', '查询完成', '操作完成', '请求完成', '联网搜索完成'].includes(text);
    }

    function getPlainDataText(payload) {
        const data = payload?.data;
        if (data == null) {
            return payload?.message || '已处理';
        }
        if (typeof data !== 'object') {
            return String(data);
        }
        if (Array.isArray(data)) {
            return data.map(item => typeof item === 'object' ? JSON.stringify(item) : String(item)).join('\n');
        }

        const preferred = [
            data.answer,
            data.content,
            data.summary,
            data.message,
            data.result
        ].find(item => item != null && typeof item !== 'object' && String(item).trim());
        const message = payload?.message;
        if (message && !isGenericResponseMessage(message)) {
            if (preferred && String(preferred).trim() !== String(message).trim()) {
                return `${message}\n\n${preferred}`;
            }
            return message;
        }
        if (preferred) {
            return String(preferred);
        }

        const lines = Object.entries(data)
            .filter(([key]) => !SKIP_META_KEYS.has(key))
            .filter(([, value]) => value == null || typeof value !== 'object')
            .map(([key, value]) => `${formatAgentLabel(key)}：${renderScalarText(value)}`);
        return lines.length ? lines.join('\n') : (message || '已处理');
    }

    function renderScalarText(value) {
        if (typeof value === 'boolean') {
            return value ? '是' : '否';
        }
        return AGENT_VALUE_LABELS[value] || String(value == null ? '无' : value);
    }

    function buildQuestionDraftSummary(value, questions) {
        const topic = value.topic || value.title || '题目草稿';
        const difficulty = value.difficulty || '未指定难度';
        const expectedCount = value.count || value.questionCount || questions.length;
        const actualCount = value.actualCount || questions.length;
        const sufficiency = value.partial ? '数量不足' : '题库充足';
        const source = value.source || value.sourcePath || value.mode || '题库文档';
        return `
            <div class="agent-question-draft-summary" aria-label="题目草稿摘要">
                <span>${escapeHtml(topic)}</span>
                <span>${escapeHtml(difficulty)}</span>
                <span>已生成 ${escapeHtml(actualCount)}/${escapeHtml(expectedCount)}</span>
                <span>${escapeHtml(sufficiency)}</span>
                <span>来源：${escapeHtml(source)}</span>
            </div>
        `;
    }

    function buildQuestionDraftCommands(value, questions) {
        return `
            <div class="agent-question-draft-toolbar" aria-label="题目草稿操作">
                <button type="button" class="agent-question-action agent-question-action-primary" data-agent-question-action="publish" title="发布给班级">
                    <i class="fa fa-paper-plane" aria-hidden="true"></i><span>发布给班级</span>
                </button>
                <button type="button" class="agent-question-action" data-agent-question-action="edit" title="编辑题目">
                    <i class="fa fa-pencil" aria-hidden="true"></i><span>编辑</span>
                </button>
                <button type="button" class="agent-question-action" data-agent-question-action="regenerate" title="重新生成">
                    <i class="fa fa-refresh" aria-hidden="true"></i><span>重新生成</span>
                </button>
                <button type="button" class="agent-question-action" data-agent-copy-questions title="复制全部题目">
                    <i class="fa fa-copy" aria-hidden="true"></i><span>复制</span>
                </button>
            </div>
        `;
    }

    function buildQuestionDraftPayload(value, questions) {
        const topic = value.topic || null;
        const displayTopic = topic || value.title || '题目';
        const difficulty = value.difficulty || '未指定难度';
        const count = value.count || value.questionCount || questions.length;
        return {
            selectionMode: 'GENERATED_QUESTIONS',
            title: value.assignmentTitle || value.title || `${displayTopic}课堂练习`,
            topic,
            difficulty,
            count,
            actualCount: value.actualCount || questions.length,
            partial: Boolean(value.partial),
            source: value.source || value.sourcePath || value.mode || '题库文档',
            content: formatStudentQuestionDraftContent(questions),
            questions
        };
    }

    function encodeQuestionDraftPayload(payload) {
        return JSON.stringify(payload).replace(/</g, '\\u003c');
    }

    function renderQuestionDraftPanel(value, questions) {
        const payload = buildQuestionDraftPayload(value, questions);
        return `
            <section class="agent-question-draft" aria-label="题目草稿">
                <script type="application/json" data-agent-question-draft-payload>${encodeQuestionDraftPayload(payload)}</script>
                <div class="agent-question-draft-header">
                    <div>
                        <p class="agent-question-draft-eyebrow">题目草稿</p>
                        <strong>已生成 ${escapeHtml(questions.length)} 道题</strong>
                    </div>
                    ${buildQuestionDraftCommands(value, questions)}
                </div>
                ${buildQuestionDraftSummary(value, questions)}
                ${renderQuestionList(questions)}
            </section>
        `;
    }

    function buildResponseTitle(payload) {
        const data = payload?.data || {};
        const questionPayload = isQuestionBankEnvelope(data)
            ? data.questionBank
            : (isQuestionBankPayload(data) || (Array.isArray(data.questions) && data.questions.every(isQuestionLike)))
                ? data
                : extractLegacyQuestionPayload(data);
        if (questionPayload?.questions?.length) {
            const topic = questionPayload.topic || questionPayload.title || '';
            return `已生成 ${questionPayload.questions.length} 道${topic ? topic : ''}题`;
        }
        return payload.message || '执行完成';
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
                    <strong data-agent-question-field="type">${escapeHtml(type)}</strong>
                    ${metaItems ? `<div class="agent-question-meta">${metaItems}</div>` : ''}
                </div>
                <div class="agent-question-content" data-agent-question-field="content">${renderAgentText(content)}</div>
                ${options.length ? `
                    <ol class="agent-question-options" type="A">
                        ${options.map(option => `<li class="agent-question-option" data-agent-question-field="option">${renderAgentText(option)}</li>`).join('')}
                    </ol>
                ` : ''}
                ${answer ? `<div class="agent-question-answer"><strong>答案：</strong><span data-agent-question-field="answer">${renderAgentText(answer)}</span></div>` : ''}
                ${explanation ? `<div class="agent-question-explanation"><strong>解析：</strong><span data-agent-question-field="explanation">${renderAgentText(explanation)}</span></div>` : ''}
            </article>
        `;
    }

    function renderQuestionList(questions) {
        return `<div class="agent-question-list">${questions.map((question, index) => renderQuestionCard(question, index)).join('')}</div>`;
    }

    function formatQuestionDraftContent(questions) {
        return questions.map((question, index) => {
            const content = question.content || question.questionText || question.title || '暂无题目内容';
            const answer = question.answer || question.correctAnswer || question.correctAnswers || '';
            const explanation = question.explanation || question.analysis || '';
            const options = Array.isArray(question.options) && question.options.length
                ? `\n选项：${question.options.join('；')}`
                : '';
            const answerText = answer ? `\n答案：${Array.isArray(answer) ? answer.join('、') : answer}` : '';
            const explanationText = explanation ? `\n解析：${explanation}` : '';
            return `${index + 1}. ${content}${options}${answerText}${explanationText}`;
        }).join('\n\n');
    }

    function formatStudentQuestionDraftContent(questions) {
        const blocks = questions.map((question, index) => {
            const content = question.content || question.questionText || question.title || '暂无题目内容';
            const options = Array.isArray(question.options)
                ? question.options
                    .map((option, optionIndex) => `${String.fromCharCode(65 + optionIndex)}. ${option}`)
                    .join('\n')
                : '';
            return `${index + 1}. ${content}${options ? `\n${options}` : ''}`;
        });
        return `题目如下：\n${blocks.join('\n\n')}`;
    }

    function getAssignmentResult(result) {
        const payload = result?.result || {};
        return payload.assignment || payload.data || payload;
    }

    function renderAssignmentPublishResult(result) {
        const assignment = getAssignmentResult(result) || {};
        const title = assignment.title || assignment.assignmentTitle || '作业';
        const id = assignment.id || assignment.assignmentId || '';
        const dueDate = assignment.dueDate || assignment.deadline || '';
        const chips = [
            id ? `<span>作业ID：${escapeHtml(id)}</span>` : '',
            dueDate ? `<span>截止：${escapeHtml(dueDate)}</span>` : ''
        ].filter(Boolean).join('');
        return `
            <article class="agent-action-success-card">
                <div>
                    <p class="agent-action-success-eyebrow">发布完成</p>
                    <strong>作业已发布</strong>
                    <p>《${escapeHtml(title)}》已发布到所选课程和班级。</p>
                </div>
                ${chips ? `<div class="agent-action-success-meta">${chips}</div>` : ''}
            </article>
        `;
    }

    function escapeMarkdown(value) {
        return String(value == null ? '' : value)
            .replace(/\\/g, '\\\\')
            .replace(/([*_`[\]])/g, '\\$1');
    }

    function markdownLabelValue(label, value) {
        if (value == null || value === '') {
            return '';
        }
        return `- ${label}：${String(value)}`;
    }

    function markdownCourseItem(course) {
        const courseId = getCourseId(course);
        const lines = [
            `- **${escapeMarkdown(course.courseName || course.courseCode || `课程 ${courseId || ''}`.trim() || '未命名课程')}**`,
            course.courseCode ? `  - 课程代码：${escapeMarkdown(course.courseCode)}` : '',
            course.description ? `  - 简介：${escapeMarkdown(course.description)}` : '',
            markdownLabelValue('课程ID', courseId),
            markdownLabelValue('学期', course.semester),
            markdownLabelValue('学分', course.credit),
            markdownLabelValue('总学时', course.totalHours),
            markdownLabelValue('学生数', course.studentCount),
            markdownLabelValue('开始时间', course.startDate),
            markdownLabelValue('结束时间', course.endDate)
        ].filter(Boolean);
        return lines.join('\n');
    }

    function markdownCourseDetail(course) {
        const courseId = getCourseId(course);
        const title = course.courseName || course.courseCode || `课程 ${courseId || ''}`.trim() || '课程详情';
        return [
            `## ${escapeMarkdown(title)}`,
            course.description ? `\n${escapeMarkdown(course.description)}` : '',
            '',
            markdownLabelValue('课程ID', courseId),
            markdownLabelValue('课程代码', course.courseCode),
            markdownLabelValue('课程类别', course.courseCategory),
            markdownLabelValue('课程状态', course.courseStatus),
            markdownLabelValue('学期', course.semester),
            markdownLabelValue('学分', course.credit),
            markdownLabelValue('总学时', course.totalHours),
            markdownLabelValue('学生数', course.studentCount),
            markdownLabelValue('开始时间', course.startDate),
            markdownLabelValue('结束时间', course.endDate),
            markdownLabelValue('负责人ID', course.courseDirector),
            markdownLabelValue('考核方式', course.assessmentMethod)
        ].filter(Boolean).join('\n');
    }

    function markdownClassItem(item) {
        const classId = getClassId(item);
        const className = item.className || item.name || item.classCode || `班级 ${classId || ''}`.trim() || '未命名班级';
        return [
            `- **${escapeMarkdown(className)}**`,
            markdownLabelValue('班级ID', classId),
            markdownLabelValue('年级', item.year),
            markdownLabelValue('学生数', item.studentCount),
            markdownLabelValue('容量', item.capacity),
            markdownLabelValue('专业', item.majorName),
            markdownLabelValue('课程', item.courseName),
            markdownLabelValue('教师', item.teacherName)
        ].filter(Boolean).join('\n');
    }

    function markdownAssignmentItem(assignment) {
        const score = assignment?.score;
        const totalScore = assignment?.totalScore ?? assignment?.total;
        const scoreText = score != null
            ? `${score}${totalScore != null ? ` / ${totalScore}` : ''}`
            : '';
        const status = assignment?.submission
            ? '已提交'
            : assignment?.score != null
                ? `成绩：${scoreText}`
                : assignment?.dueDate
                    ? '待提交'
                    : '';
        return [
            `- **${escapeMarkdown(assignment.title || assignment.examName || '未命名任务')}**`,
            status ? `  - 状态：${escapeMarkdown(status)}` : '',
            markdownLabelValue('课程', assignment.courseName),
            markdownLabelValue('作业ID', assignment.id ?? assignment.relatedId),
            markdownLabelValue('考试ID', assignment.relatedId && assignment.examDate ? assignment.relatedId : null),
            markdownLabelValue('截止', assignment.dueDate),
            markdownLabelValue('提交', assignment.submitDate),
            markdownLabelValue('完成', assignment.completedAt),
            markdownLabelValue('考试时间', assignment.examDate),
            markdownLabelValue('排名', assignment.rank)
        ].filter(Boolean).join('\n');
    }

    function stringifyQueryValue(value) {
        if (value == null || value === '') {
            return '无';
        }
        if (Array.isArray(value)) {
            return value.map(item => stringifyQueryValue(item)).join('，');
        }
        if (typeof value === 'object') {
            if (isCourseLike(value)) {
                return value.courseName || value.courseCode || String(getCourseId(value) || '');
            }
            if (isClassLike(value)) {
                return value.className || value.name || value.classCode || String(getClassId(value) || '');
            }
            if (isAssignmentLike(value) || isStudentAssignmentScoreLike(value) || isStudentExamScoreLike(value)) {
                return value.title || value.examName || String(value.id || value.relatedId || '');
            }
            return Object.entries(value)
                .filter(([key]) => !SKIP_META_KEYS.has(key))
                .map(([key, item]) => `${formatAgentLabel(key)}=${stringifyQueryValue(item)}`)
                .join('；');
        }
        return String(value);
    }

    function renderQueryMarkdown(value, message = '') {
        if (value == null) {
            return String(message || '').trim();
        }
        if (Array.isArray(value)) {
            if (value.every(isCourseLike)) {
                return value.map(markdownCourseItem).join('\n\n');
            }
            if (value.every(isClassLike)) {
                return value.map(markdownClassItem).join('\n\n');
            }
            if (value.every(item => isAssignmentLike(item) || isStudentAssignmentScoreLike(item) || isStudentExamScoreLike(item))) {
                return value.map(markdownAssignmentItem).join('\n\n');
            }
            return value.map(item => `- ${stringifyQueryValue(item)}`).join('\n');
        }
        if (typeof value !== 'object') {
            return String(value);
        }
        if (isCourseDetailPayload(value)) {
            return markdownCourseDetail(value.course);
        }
        if (isCourseLike(value)) {
            return markdownCourseItem(value);
        }
        if (Array.isArray(value.courses) && value.courses.every(isCourseLike)) {
            const summary = Object.entries(value)
                .filter(([key]) => key !== 'courses')
                .filter(([, item]) => item != null && item !== '')
                .map(([key, item]) => `- ${formatAgentLabel(key)}：${stringifyQueryValue(item)}`)
                .join('\n');
            return [summary, value.courses.map(markdownCourseItem).join('\n\n')].filter(Boolean).join('\n\n');
        }
        if (Array.isArray(value.classes) && value.classes.every(isClassLike)) {
            const summary = Object.entries(value)
                .filter(([key]) => key !== 'classes')
                .filter(([, item]) => item != null && item !== '')
                .map(([key, item]) => `- ${formatAgentLabel(key)}：${stringifyQueryValue(item)}`)
                .join('\n');
            return [summary, value.classes.map(markdownClassItem).join('\n\n')].filter(Boolean).join('\n\n');
        }
        if (isPendingAssignmentPayload(value)) {
            const page = value.pendingAssignments || {};
            const items = Array.isArray(page.content) ? page.content : [];
            const summary = [
                markdownLabelValue('总数', page.totalElements ?? items.length),
                markdownLabelValue('当前数量', page.numberOfElements ?? items.length),
                markdownLabelValue('待提交数量', items.filter(item => !item?.submission).length)
            ].filter(Boolean).join('\n');
            return [summary, items.map(markdownAssignmentItem).join('\n\n')].filter(Boolean).join('\n\n');
        }
        if (isStudentAssignmentScorePayload(value)) {
            const items = Array.isArray(value.assignments) ? value.assignments : [];
            const summary = [
                markdownLabelValue('总数', items.length),
                markdownLabelValue('已出分数量', items.filter(item => item?.score != null).length)
            ].filter(Boolean).join('\n');
            return [summary, items.map(markdownAssignmentItem).join('\n\n')].filter(Boolean).join('\n\n');
        }
        if (isStudentExamScorePayload(value)) {
            const items = Array.isArray(value.exams) ? value.exams : [];
            const summary = [
                markdownLabelValue('总数', items.length),
                markdownLabelValue('已出分数量', items.filter(item => item?.score != null).length)
            ].filter(Boolean).join('\n');
            return [summary, items.map(markdownAssignmentItem).join('\n\n')].filter(Boolean).join('\n\n');
        }
        if (isStructuredWebSearchPayload(value)) {
            return null;
        }
        if (isQuestionBankEnvelope(value) || isQuestionBankPayload(value) || (Array.isArray(value.questions) && value.questions.every(isQuestionLike)) || extractLegacyQuestionPayload(value) || isQuestionLike(value)) {
            return null;
        }
        const entries = Object.entries(value).filter(([key]) => !SKIP_META_KEYS.has(key));
        if (!entries.length) {
            return String(message || '').trim();
        }
        return entries
            .map(([key, item]) => `- ${formatAgentLabel(key)}：${stringifyQueryValue(item)}`)
            .join('\n');
    }

    function shouldRenderQueryAsMarkdown(value) {
        if (value == null) {
            return false;
        }
        if (Array.isArray(value)) {
            return value.every(isCourseLike)
                || value.every(isClassLike)
                || value.every(item => isAssignmentLike(item) || isStudentAssignmentScoreLike(item) || isStudentExamScoreLike(item));
        }
        if (typeof value !== 'object') {
            return false;
        }
        if (isStructuredWebSearchPayload(value)) {
            return false;
        }
        if (isQuestionBankEnvelope(value) || isQuestionBankPayload(value) || (Array.isArray(value.questions) && value.questions.every(isQuestionLike)) || extractLegacyQuestionPayload(value) || isQuestionLike(value)) {
            return false;
        }
        return isCourseDetailPayload(value)
            || isCourseLike(value)
            || (Array.isArray(value.courses) && value.courses.every(isCourseLike))
            || (Array.isArray(value.classes) && value.classes.every(isClassLike))
            || isPendingAssignmentPayload(value)
            || isStudentAssignmentScorePayload(value)
            || isStudentExamScorePayload(value);
    }

    function renderActionExecutionResult(preview, result) {
        const status = normalizeActionStatus(result?.status || result?.result?.status);
        if (status === 'EXECUTED' && (result?.intent === 'PUBLISH_ASSIGNMENT' || preview?.intent === 'PUBLISH_ASSIGNMENT')) {
            return renderAssignmentPublishResult(result);
        }
        return `
            <p><strong>${escapeHtml(result.message || '操作已执行')}</strong></p>
            <div class="agent-data-result">${renderValue(result.result || {})}</div>
        `;
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
        return renderQuestionDraftPanel(value, value.questions);
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
                return renderQuestionDraftPanel(value, value.questions);
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
        const date = parseAgentDate(value);
        if (!date) {
            return '';
        }
        return date.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    }

    function createMessage(role, html, state, options = {}) {
        const node = document.createElement('div');
        const timeValue = parseAgentDate(options.timestamp) || new Date();
        const avatarLabel = role === 'user' ? '教师头像' : '学习助手头像';
        const avatarText = role === 'user' ? 'T' : 'AI';
        node.className = `agent-message agent-message-${role}${state ? ` agent-message-${state}` : ''}`;
        node.innerHTML = `
            <div class="agent-message-avatar" role="img" aria-label="${avatarLabel}">
                <span>${avatarText}</span>
            </div>
            <div class="agent-message-content">
                <div class="agent-message-body">${html}</div>
                <time class="agent-message-time" datetime="${timeValue.toISOString()}">${formatMessageTime(timeValue)}</time>
            </div>
        `;
        return node;
    }

    class AgentChatPanel {
        constructor(root, options = {}) {
            this.root = root;
            this.shellEl = root.closest('[data-agent-shell]') || root;
            this.shouldRestoreSession = this.shellEl?.dataset?.agentRestoreSession !== 'false';
            this.role = options.role || 'USER';
            this.sessionId = null;
            this.sessionStorageKey = getCurrentSessionStorageKey();
            this.messagesEl = root.querySelector('[data-agent-messages]');
            this.formEl = root.querySelector('[data-agent-form]');
            this.inputEl = root.querySelector('[data-agent-input]');
            this.submitEl = root.querySelector('[data-agent-submit]');
            this.attachTriggerEl = root.querySelector('[data-agent-attach-trigger]');
            this.attachmentInputEl = root.querySelector('[data-agent-attachment-input]');
            this.attachmentListEl = root.querySelector('[data-agent-attachment-list]');
            this.shouldAutoScrollMessages = true;
            this.messagesScrollThreshold = 72;
            this.isLoading = false;
            this.currentController = null;
            this.thinkingEl = null;
            this.isRestoringSession = false;
            this.pendingAttachments = [];
            this.ensureMessagesWrap();
            this.scrollToBottomButton = this.createScrollToBottomButton();
            this.bind();
            this.syncConversationState();
            this.updateScrollToBottomVisibility();
            this.restoreCurrentSessionId();
            this.renderPendingAttachments();
        }

        setRestoringSessionState(isRestoring) {
            this.isRestoringSession = isRestoring;
            this.shellEl?.classList.toggle('agent-shell-restoring-session', isRestoring);
            document.documentElement?.classList.toggle('agent-restoring-session', isRestoring);
        }

        bind() {
            if (!this.formEl || !this.inputEl) {
                return;
            }
            this.messagesEl?.addEventListener('scroll', () => {
                this.shouldAutoScrollMessages = this.isNearMessagesBottom();
                this.updateScrollToBottomVisibility();
            });
            window.addEventListener('resize', () => {
                this.updateScrollToBottomVisibility();
            });
            this.formEl.addEventListener('submit', (event) => {
                event.preventDefault();
                if (this.isLoading) {
                    this.pause();
                    return;
                }
                this.submitInputMessage();
            });
            this.inputEl.addEventListener('keydown', (event) => {
                if (event.key !== 'Enter' || event.shiftKey || event.ctrlKey || event.metaKey || event.altKey) {
                    return;
                }
                event.preventDefault();
                if (this.isLoading) {
                    this.pause();
                    return;
                }
                this.submitInputMessage();
            });
            this.submitEl?.addEventListener('click', (event) => {
                if (this.isLoading) {
                    event.preventDefault();
                    this.pause();
                }
            });
            this.attachTriggerEl?.addEventListener('click', () => {
                if (this.isLoading || !this.attachmentInputEl) {
                    return;
                }
                this.attachmentInputEl.value = '';
                this.attachmentInputEl.click();
            });
            this.attachmentInputEl?.addEventListener('change', async (event) => {
                const input = event.target;
                if (!input?.files?.length) {
                    return;
                }
                await this.handleAttachmentSelection(Array.from(input.files));
                input.value = '';
            });
            this.root.addEventListener('click', (event) => {
                const button = event.target.closest('[data-agent-copy-questions]');
                if (!button || !this.root.contains(button)) {
                    return;
                }
                this.copyQuestionDraft(button);
            });
            this.root.addEventListener('click', (event) => {
                const button = event.target.closest('[data-agent-attachment-remove]');
                if (!button || !this.root.contains(button) || this.isLoading) {
                    return;
                }
                this.removePendingAttachment(button.getAttribute('data-agent-attachment-remove'));
            });
            this.root.addEventListener('click', (event) => {
                const button = event.target.closest('[data-agent-question-action]');
                if (!button || !this.root.contains(button) || this.isLoading) {
                    return;
                }
                this.handleQuestionDraftAction(button);
            });
            this.root.addEventListener('click', (event) => {
                const confirmButton = event.target.closest('[data-agent-publish-confirm]');
                if (confirmButton && this.root.contains(confirmButton) && !this.isLoading) {
                    this.confirmQuestionDraftPublish(confirmButton);
                    return;
                }
                const cancelButton = event.target.closest('[data-agent-publish-cancel]');
                if (cancelButton && this.root.contains(cancelButton)) {
                    cancelButton.closest('.agent-question-publish-panel')?.remove();
                }
            });
            this.root.addEventListener('change', (event) => {
                const select = event.target.closest('[data-agent-publish-course]');
                if (select && this.root.contains(select)) {
                    this.refreshPublishClassOptions(select.closest('.agent-question-publish-panel'));
                    return;
                }
                const classSelect = event.target.closest('[data-agent-publish-class]');
                if (classSelect && this.root.contains(classSelect)) {
                    const panel = classSelect.closest('.agent-question-publish-panel');
                    const confirmButton = panel?.querySelector('[data-agent-publish-confirm]');
                    const courseValue = panel?.querySelector('[data-agent-publish-course]')?.value;
                    if (confirmButton) {
                        confirmButton.disabled = !courseValue || !classSelect.value;
                    }
                }
            });
            this.root.addEventListener('click', (event) => {
                const button = event.target.closest('[data-agent-command]');
                if (!button || !this.root.contains(button) || this.isLoading) {
                    return;
                }
                const command = button.getAttribute('data-agent-command') || '';
                this.inputEl.value = command;
                this.send(command, { agentCommand: command, agentCommandSource: 'toolbar' });
            });
            window.addEventListener('agent-session-selected', (event) => {
                const sessionId = event?.detail?.sessionId;
                const sessionLabel = event?.detail?.label || '';
                this.switchSession(sessionId, { announce: false, sessionLabel });
            });
            window.addEventListener('agent-session-reset', () => {
                this.resetSession();
            });
        }

        append(role, html, state, options = {}) {
            if (!this.messagesEl) {
                return null;
            }
            const message = createMessage(role, html, state, options);
            this.messagesEl.appendChild(message);
            this.syncConversationState();
            this.scrollMessagesToBottom();
            return message;
        }

        appendEmpty(role, state) {
            return this.append(role, '', state);
        }

        appendFeedbackMessage(level, text) {
            const normalizedLevel = level === 'error' ? 'error' : level === 'warning' ? 'warning' : 'info';
            return this.append('agent', `<p>${escapeHtml(text || '')}</p>`, `feedback-message feedback-message-${normalizedLevel}`);
        }

        async handleAttachmentSelection(files) {
            if (!Array.isArray(files) || files.length === 0) {
                return;
            }
            const nextAttachments = [];
            for (const file of files) {
                if (!(file instanceof File)) {
                    continue;
                }
                if (file.size > 10 * 1024 * 1024) {
                    this.appendFeedbackMessage('warning', `附件“${file.name}”超过 10 MB，暂不支持上传。`);
                    continue;
                }
                try {
                    nextAttachments.push(await this.readAttachmentFile(file));
                } catch (error) {
                    this.appendFeedbackMessage('error', `读取附件“${file.name}”失败，请重试。`);
                }
            }
            if (!nextAttachments.length) {
                return;
            }
            this.pendingAttachments = [...this.pendingAttachments, ...nextAttachments];
            this.renderPendingAttachments();
        }

        readAttachmentFile(file) {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => {
                    const result = String(reader.result || '');
                    const base64Index = result.indexOf(',');
                    resolve({
                        id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
                        name: file.name,
                        contentType: file.type || 'application/octet-stream',
                        size: file.size,
                        base64: base64Index >= 0 ? result.slice(base64Index + 1) : result
                    });
                };
                reader.onerror = () => reject(reader.error || new Error('读取附件失败'));
                reader.readAsDataURL(file);
            });
        }

        removePendingAttachment(attachmentId) {
            if (!attachmentId) {
                return;
            }
            this.pendingAttachments = this.pendingAttachments.filter((attachment) => attachment.id !== attachmentId);
            this.renderPendingAttachments();
        }

        renderPendingAttachments() {
            if (!this.attachmentListEl) {
                return;
            }
            if (!this.pendingAttachments.length) {
                this.attachmentListEl.innerHTML = '';
                return;
            }
            this.attachmentListEl.innerHTML = this.pendingAttachments.map((attachment) => {
                const sizeText = formatAttachmentSize(attachment.size);
                return `
                    <span class="agent-attachment-chip">
                        <span>${escapeHtml(attachment.name)}${sizeText ? ` · ${escapeHtml(sizeText)}` : ''}</span>
                        <button type="button" aria-label="移除附件 ${escapeHtml(attachment.name)}" data-agent-attachment-remove="${escapeHtml(attachment.id)}">
                            <i class="fa fa-times" aria-hidden="true"></i>
                        </button>
                    </span>
                `;
            }).join('');
        }

        buildOutgoingAttachmentHtml(attachments) {
            if (!Array.isArray(attachments) || attachments.length === 0) {
                return '';
            }
            return `
                <div class="agent-attachment-list">
                    ${attachments.map((attachment) => {
                        const sizeText = formatAttachmentSize(attachment.size);
                        return `
                            <span class="agent-attachment-chip">
                                <i class="fa fa-paperclip" aria-hidden="true"></i>
                                <span>${escapeHtml(attachment.name || '附件')}${sizeText ? ` · ${escapeHtml(sizeText)}` : ''}</span>
                            </span>
                        `;
                    }).join('')}
                </div>
            `;
        }

        cloneAttachmentsForContext(attachments) {
            if (!Array.isArray(attachments) || attachments.length === 0) {
                return [];
            }
            return attachments.map((attachment) => ({
                name: attachment.name,
                contentType: attachment.contentType || 'application/octet-stream',
                size: attachment.size,
                base64: attachment.base64
            })).filter((attachment) => attachment.name && attachment.base64);
        }

        clearRenderedMessages() {
            if (!this.messagesEl) {
                return;
            }
            this.messagesEl.querySelectorAll('.agent-message').forEach((message) => {
                message.remove();
            });
            this.updateScrollToBottomVisibility();
        }

        scrollMessagesToBottom(options = {}) {
            if (!this.messagesEl) {
                return;
            }
            const { smooth = false, force = false } = options;
            const shouldScroll = force || this.shouldAutoScrollMessages;
            if (!shouldScroll) {
                this.updateScrollToBottomVisibility();
                return;
            }
            const behavior = smooth ? 'smooth' : 'auto';
            this.messagesEl.scrollTo({
                top: this.messagesEl.scrollHeight,
                behavior
            });
            this.shouldAutoScrollMessages = true;
            this.updateScrollToBottomVisibility();
            const schedule =
                typeof requestAnimationFrame === 'function'
                    ? requestAnimationFrame
                    : (callback) => setTimeout(callback, 0);
            schedule(() => {
                if (!this.messagesEl) {
                    return;
                }
                this.messagesEl.scrollTo({
                    top: this.messagesEl.scrollHeight,
                    behavior: 'auto'
                });
                this.updateScrollToBottomVisibility();
            });
        }

        ensureMessagesWrap() {
            if (!this.messagesEl) {
                return;
            }
            const parent = this.messagesEl.parentElement;
            if (parent?.classList.contains('agent-messages-wrap')) {
                this.messagesWrapEl = parent;
                return;
            }
            const wrap = document.createElement('div');
            wrap.className = 'agent-messages-wrap';
            parent?.insertBefore(wrap, this.messagesEl);
            wrap.appendChild(this.messagesEl);
            this.messagesWrapEl = wrap;
        }

        createScrollToBottomButton() {
            if (!this.messagesWrapEl) {
                return null;
            }
            const existing = this.messagesWrapEl.querySelector('[data-agent-scroll-bottom]');
            if (existing) {
                return existing;
            }
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'agent-scroll-bottom-button';
            button.setAttribute('data-agent-scroll-bottom', 'true');
            button.setAttribute('aria-label', '快速回到底部');
            button.innerHTML = '<i class="fa fa-arrow-down" aria-hidden="true"></i>';
            button.addEventListener('click', () => {
                this.shouldAutoScrollMessages = true;
                this.scrollMessagesToBottom({ smooth: true, force: true });
            });
            this.messagesWrapEl.appendChild(button);
            return button;
        }

        isNearMessagesBottom(threshold = this.messagesScrollThreshold) {
            if (!this.messagesEl) {
                return true;
            }
            const distanceFromBottom = this.messagesEl.scrollHeight - this.messagesEl.scrollTop - this.messagesEl.clientHeight;
            return distanceFromBottom <= threshold;
        }

        updateScrollToBottomVisibility() {
            if (!this.scrollToBottomButton || !this.messagesEl) {
                return;
            }
            const hasOverflow = this.messagesEl.scrollHeight - this.messagesEl.clientHeight > this.messagesScrollThreshold;
            const isVisible = hasOverflow && !this.isNearMessagesBottom();
            this.scrollToBottomButton.classList.toggle('agent-scroll-bottom-visible', isVisible);
            this.scrollToBottomButton.hidden = !isVisible;
            this.scrollToBottomButton.setAttribute('aria-hidden', String(!isVisible));
        }

        showThinking() {
            this.removeThinking();
            this.thinkingEl = this.append('agent', '<p>思考中...</p>', 'thinking');
        }

        removeThinking() {
            if (this.thinkingEl) {
                this.thinkingEl.remove();
                this.thinkingEl = null;
                this.syncConversationState();
            }
        }

        setConversationState(hasConversation) {
            this.shellEl?.classList.toggle('agent-shell-has-conversation', hasConversation);
        }

        syncConversationState() {
            if (this.isRestoringSession) {
                this.setConversationState(true);
                return;
            }
            if (!this.messagesEl || typeof this.messagesEl.querySelectorAll !== 'function') {
                this.setConversationState(false);
                return;
            }
            const realMessages = this.messagesEl.querySelectorAll('.agent-message').length;
            this.setConversationState(realMessages > 0);
        }

        pause() {
            if (this.currentController) {
                this.currentController.abort();
            }
        }

        readQuestionDraft(button) {
            const draft = button?.closest?.('.agent-question-draft');
            const payloadNode = draft?.querySelector('[data-agent-question-draft-payload]');
            if (!draft || !payloadNode?.textContent) {
                return null;
            }
            try {
                const payload = JSON.parse(payloadNode.textContent);
                const questions = Array.from(draft.querySelectorAll('.agent-question-card')).map((card, index) => {
                    const original = payload.questions?.[index] || {};
                    const options = Array.from(card.querySelectorAll('[data-agent-question-field="option"]'))
                        .map(option => option.innerText.trim())
                        .filter(Boolean);
                    return {
                        ...original,
                        type: card.querySelector('[data-agent-question-field="type"]')?.innerText.trim() || original.type || original.questionType,
                        content: card.querySelector('[data-agent-question-field="content"]')?.innerText.trim() || original.content || original.questionText,
                        options,
                        answer: card.querySelector('[data-agent-question-field="answer"]')?.innerText.trim() || original.answer || original.correctAnswer,
                        explanation: card.querySelector('[data-agent-question-field="explanation"]')?.innerText.trim() || original.explanation || original.analysis
                    };
                });
                return {
                    ...payload,
                    questions,
                    actualCount: questions.length,
                    content: formatStudentQuestionDraftContent(questions)
                };
            } catch (error) {
                this.appendFeedbackMessage('error', '题目草稿数据读取失败，请重新生成后再操作。');
                return null;
            }
        }

        writeQuestionDraft(button, payload) {
            const draft = button?.closest?.('.agent-question-draft');
            const payloadNode = draft?.querySelector('[data-agent-question-draft-payload]');
            if (!payloadNode || !payload) {
                return;
            }
            payloadNode.textContent = JSON.stringify(payload).replace(/</g, '\\u003c');
        }

        handleQuestionDraftAction(button) {
            const action = button.getAttribute('data-agent-question-action') || '';
            if (action === 'edit') {
                this.toggleQuestionDraftEditing(button);
                return;
            }
            const draft = this.readQuestionDraft(button);
            if (!draft) {
                return;
            }
            this.writeQuestionDraft(button, draft);
            if (action === 'publish') {
                this.openQuestionDraftPublishPanel(button, draft);
                return;
            }
            if (action === 'regenerate') {
                this.regenerateQuestionDraft(draft);
            }
        }

        toggleQuestionDraftEditing(button) {
            const draft = button.closest('.agent-question-draft');
            if (!draft) {
                return;
            }
            const isEditing = draft.classList.toggle('agent-question-draft-editing');
            draft.querySelectorAll('[data-agent-question-field]').forEach(field => {
                field.setAttribute('contenteditable', String(isEditing));
                field.setAttribute('spellcheck', 'false');
            });
            const label = button.querySelector('span');
            if (label) {
                label.textContent = isEditing ? '完成编辑' : '编辑';
            }
            button.setAttribute('title', isEditing ? '完成编辑' : '编辑题目');
            if (!isEditing) {
                const updatedDraft = this.readQuestionDraft(button);
                this.writeQuestionDraft(button, updatedDraft);
                this.appendFeedbackMessage('info', '题目草稿已更新。');
            } else {
                draft.querySelector('[data-agent-question-field="content"]')?.focus();
            }
        }

        async openQuestionDraftPublishPanel(button, draft) {
            const container = button.closest('.agent-question-draft');
            if (!container) {
                return;
            }
            container.querySelector('.agent-question-publish-panel')?.remove();
            const panel = document.createElement('div');
            const defaultTitle = draft.title || `${draft.topic || '题目'}课堂练习`;
            panel.className = 'agent-question-publish-panel';
            panel.innerHTML = `
                <div class="agent-question-publish-fields">
                    <label class="agent-question-publish-title-field">
                        <span>作业标题</span>
                        <input type="text" data-agent-publish-title maxlength="80" value="${escapeHtml(defaultTitle)}" required>
                    </label>
                    <label>
                        <span>课程</span>
                        <select data-agent-publish-course disabled>
                            <option value="">加载课程中...</option>
                        </select>
                    </label>
                    <label>
                        <span>班级</span>
                        <select data-agent-publish-class disabled>
                            <option value="">加载班级中...</option>
                        </select>
                    </label>
                </div>
                <div class="agent-question-publish-actions">
                    <button type="button" class="agent-question-action agent-question-action-primary" data-agent-publish-confirm disabled>
                        <i class="fa fa-paper-plane" aria-hidden="true"></i><span>确认发布</span>
                    </button>
                    <button type="button" class="agent-question-action" data-agent-publish-cancel>
                        <i class="fa fa-times" aria-hidden="true"></i><span>取消</span>
                    </button>
                </div>
                <p class="agent-question-publish-status">正在加载课程和班级...</p>
            `;
            container.querySelector('.agent-question-draft-header')?.after(panel);
            panel.__questionDraft = draft;
            try {
                const [courses, classes] = await Promise.all([
                    this.fetchTeacherOptions('/api/teacher/courses'),
                    this.fetchTeacherOptions('/api/teacher/classes')
                ]);
                panel.__courses = courses;
                panel.__classes = classes;
                this.renderPublishOptions(panel);
            } catch (error) {
                panel.querySelector('.agent-question-publish-status').textContent = error.message || '课程和班级加载失败，请稍后重试。';
            }
        }

        renderPublishOptions(panel) {
            if (!panel) {
                return;
            }
            const courses = Array.isArray(panel.__courses) ? panel.__courses : [];
            const classes = Array.isArray(panel.__classes) ? panel.__classes : [];
            const currentCourseId = window.agentCurrentCourseId || window.currentCourseId;
            const currentClassId = window.agentCurrentClassId || window.currentClassId;
            const courseSelect = panel.querySelector('[data-agent-publish-course]');
            const classSelect = panel.querySelector('[data-agent-publish-class]');
            const status = panel.querySelector('.agent-question-publish-status');
            if (courseSelect) {
                courseSelect.disabled = courses.length === 0;
                courseSelect.innerHTML = [
                    '<option value="">请选择课程</option>',
                    ...courses.map(course => {
                        const id = this.getOptionId(course);
                        const name = this.getCourseOptionName(course, id);
                        return id ? `<option value="${escapeHtml(id)}">${escapeHtml(name)}</option>` : '';
                    }).filter(Boolean)
                ].join('');
                if (currentCourseId) {
                    courseSelect.value = String(currentCourseId);
                }
            }
            if (classSelect) {
                classSelect.disabled = classes.length === 0;
                if (currentClassId) {
                    classSelect.dataset.pendingValue = String(currentClassId);
                }
            }
            this.refreshPublishClassOptions(panel);
            const confirmButton = panel.querySelector('[data-agent-publish-confirm]');
            if (confirmButton) {
                confirmButton.disabled = !courseSelect?.value || !classSelect?.value;
            }
            if (status) {
                status.textContent = courses.length && classes.length
                    ? '请选择发布目标后确认。'
                    : '未加载到可发布的课程或班级。';
            }
        }

        refreshPublishClassOptions(panel) {
            if (!panel) {
                return;
            }
            const classSelect = panel.querySelector('[data-agent-publish-class]');
            const courseSelect = panel.querySelector('[data-agent-publish-course]');
            const confirmButton = panel.querySelector('[data-agent-publish-confirm]');
            if (!classSelect) {
                return;
            }
            const selectedCourseId = courseSelect?.value || '';
            const allClasses = Array.isArray(panel.__classes) ? panel.__classes : [];
            const matchingClasses = allClasses.filter(item => {
                const classCourseId = item.courseId ?? item.course_id;
                return !selectedCourseId || !classCourseId || String(classCourseId) === selectedCourseId;
            });
            classSelect.disabled = matchingClasses.length === 0;
            classSelect.innerHTML = [
                '<option value="">请选择班级</option>',
                ...matchingClasses.map(item => {
                    const id = this.getOptionId(item);
                    const name = this.getClassOptionName(item, id);
                    return id ? `<option value="${escapeHtml(id)}">${escapeHtml(name)}</option>` : '';
                }).filter(Boolean)
            ].join('');
            const pendingValue = classSelect.dataset.pendingValue;
            if (pendingValue && matchingClasses.some(item => String(this.getOptionId(item)) === pendingValue)) {
                classSelect.value = pendingValue;
                delete classSelect.dataset.pendingValue;
            }
            if (confirmButton) {
                confirmButton.disabled = !selectedCourseId || !classSelect.value;
            }
        }

        confirmQuestionDraftPublish(button) {
            const panel = button.closest('.agent-question-publish-panel');
            const draft = panel?.__questionDraft;
            const titleInput = panel?.querySelector('[data-agent-publish-title]');
            const title = titleInput?.value.trim();
            const courseId = Number(panel?.querySelector('[data-agent-publish-course]')?.value);
            const classId = Number(panel?.querySelector('[data-agent-publish-class]')?.value);
            if (!draft) {
                this.appendFeedbackMessage('error', '题目草稿数据读取失败，请重新生成后再发布。');
                return;
            }
            if (!title) {
                this.appendFeedbackMessage('warning', '请填写作业标题后再发布。');
                titleInput?.focus();
                return;
            }
            if (!courseId || !classId) {
                this.appendFeedbackMessage('warning', '请选择课程和班级后再发布。');
                return;
            }
            const updatedDraft = {
                ...draft,
                title
            };
            panel.__questionDraft = updatedDraft;
            this.publishQuestionDraft(updatedDraft, { courseId, classId });
        }

        publishQuestionDraft(draft, target) {
            const courseId = Number(target?.courseId);
            const classId = Number(target?.classId);
            const title = draft.title || `${draft.topic || '题目'}课堂练习`;
            const message = `发布作业，课程ID ${courseId}，班级ID ${classId}，标题是${title}，使用当前题目草稿`;
            this.inputEl.value = message;
            this.send(message, {
                agentCommand: message,
                agentCommandSource: 'question-draft-toolbar',
                agentCommandAction: 'publish_question_draft',
                currentCourseId: courseId,
                currentClassId: classId,
                forceJson: true,
                questionDraft: {
                    ...draft,
                    title,
                    courseId,
                    classId,
                    selectionMode: 'GENERATED_QUESTIONS'
                }
            });
        }

        async fetchTeacherOptions(url) {
            const response = await this.requestJson(url);
            return this.extractListPayload(response);
        }

        async requestJson(url) {
            const api = getApiService();
            const response = api
                ? await api.request(url, { method: 'GET' })
                : await fetch(url, {
                    method: 'GET',
                    credentials: 'include',
                    headers: this.buildJsonHeaders()
                }).then(item => item.json());
            if (!response || response.success === false) {
                throw new Error(response?.message || '数据加载失败');
            }
            return response.data || response;
        }

        buildJsonHeaders() {
            const headers = { 'Content-Type': 'application/json' };
            const csrfToken = typeof getCsrfToken === 'function' ? getCsrfToken() : null;
            const userId = window.sessionStorage.getItem('userId');
            const activeRole = window.sessionStorage.getItem('activeRole') || window.sessionStorage.getItem('role');
            const roles = window.sessionStorage.getItem('roles') || activeRole;
            const token = window.sessionStorage.getItem('token');
            if (csrfToken) headers['X-XSRF-TOKEN'] = csrfToken;
            if (userId) headers['X-User-Id'] = userId;
            if (activeRole) headers['X-Active-Role'] = activeRole;
            if (roles) headers['X-Roles'] = roles;
            if (token) headers['Authorization'] = `Bearer ${token}`;
            return headers;
        }

        extractListPayload(value) {
            if (Array.isArray(value)) {
                return value;
            }
            if (Array.isArray(value?.content)) {
                return value.content;
            }
            if (Array.isArray(value?.records)) {
                return value.records;
            }
            if (Array.isArray(value?.data)) {
                return value.data;
            }
            if (Array.isArray(value?.data?.content)) {
                return value.data.content;
            }
            return [];
        }

        getOptionId(item) {
            return item?.id ?? item?.courseId ?? item?.course_id ?? item?.classId ?? item?.class_id ?? '';
        }

        getCourseOptionName(course, id) {
            return course?.courseName || course?.course_name || course?.name || course?.courseCode || `课程 ${id}`;
        }

        getClassOptionName(item, id) {
            return item?.className || item?.class_name || item?.name || item?.classCode || `班级 ${id}`;
        }

        regenerateQuestionDraft(draft) {
            const count = draft.count || draft.actualCount || draft.questions?.length || 5;
            const topic = draft.topic ? `${draft.topic}题` : '题';
            const difficulty = draft.difficulty || '原难度';
            const message = `重新生成 ${count} 道${topic}，难度${difficulty}`;
            this.inputEl.value = message;
            this.send(message, {
                agentCommand: message,
                agentCommandSource: 'question-draft-toolbar',
                agentCommandAction: 'regenerate_question_draft',
                questionDraft: draft
            });
        }

        async copyQuestionDraft(button) {
            const draft = button.closest('.agent-question-draft');
            const text = draft?.innerText?.trim() || '';
            if (!text) {
                this.appendFeedbackMessage('warning', '没有可复制的题目内容。');
                return;
            }
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                } else {
                    const textarea = document.createElement('textarea');
                    textarea.value = text;
                    textarea.setAttribute('readonly', '');
                    textarea.style.position = 'fixed';
                    textarea.style.top = '-9999px';
                    document.body.appendChild(textarea);
                    textarea.select();
                    document.execCommand('copy');
                    textarea.remove();
                }
                this.appendFeedbackMessage('info', '题目草稿已复制。');
            } catch (error) {
                this.appendFeedbackMessage('error', '复制失败，请手动选择题目内容复制。');
            }
        }

        restoreCurrentSessionId() {
            if (!this.shouldRestoreSession) {
                window.sessionStorage.removeItem(this.sessionStorageKey);
                this.setRestoringSessionState(false);
                return;
            }
            const sessionId = window.sessionStorage.getItem(this.sessionStorageKey);
            if (sessionId) {
                this.switchSession(sessionId, { announce: false, restoring: true });
            } else {
                this.setRestoringSessionState(false);
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

        async switchSession(sessionId, options = {}) {
            if (!sessionId || this.isLoading) {
                return;
            }
            this.sessionId = sessionId;
            this.persistCurrentSessionId(sessionId);
            this.notifySessionChanged(sessionId);
            this.setRestoringSessionState(options.restoring === true);
            this.syncConversationState();
            try {
                const session = await this.requestSessionDetail(sessionId);
                this.renderSessionHistory(session);
            } catch (error) {
                if (options.restoring) {
                    this.sessionId = null;
                    this.persistCurrentSessionId(null);
                    this.clearRenderedMessages();
                }
                if (options.announce) {
                    this.appendFeedbackMessage('error', error.message || '历史会话加载失败');
                }
            } finally {
                this.setRestoringSessionState(false);
                this.syncConversationState();
                if (this.inputEl) {
                    this.inputEl.focus();
                }
            }
        }

        resetSession() {
            if (this.isLoading) {
                this.pause();
            }
            this.sessionId = null;
            this.setRestoringSessionState(false);
            this.persistCurrentSessionId(null);
            this.clearRenderedMessages();
            this.removeThinking();
            this.syncConversationState();
            window.dispatchEvent(new CustomEvent('agent-session-cleared'));
            if (this.inputEl) {
                this.inputEl.value = '';
                this.inputEl.focus();
            }
        }

        async requestSessionDetail(sessionId) {
            const api = getApiService();
            if (api && typeof api.get === 'function') {
                const response = await api.get(`/api/agent/sessions/${sessionId}`);
                if (response?.success === false) {
                    throw new Error(response.message || '历史会话加载失败');
                }
                return response?.data || response;
            }
            const response = await fetch(`/api/agent/sessions/${sessionId}`, {
                method: 'GET',
                credentials: 'include',
                headers: this.buildJsonHeaders()
            });
            const payload = await response.json().catch(() => null);
            if (!response.ok || payload?.success === false) {
                throw new Error(payload?.message || `请求失败（${response.status}）`);
            }
            return payload?.data || payload;
        }

        renderSessionHistory(session) {
            if (!this.messagesEl) {
                return;
            }
            const messages = Array.isArray(session?.messages) ? session.messages : [];
            const actionLookup = buildActionLookup(session?.actions);
            this.clearRenderedMessages();
            messages.forEach((message) => {
                const role = String(message.role || '').toUpperCase() === 'USER' ? 'user' : 'agent';
                const replayPayload = role === 'agent' ? buildHistoryReplayPayload(message, actionLookup) : null;
                if (replayPayload) {
                    this.renderResponse(replayPayload, {
                        timestamp: message.createdAt || new Date()
                    });
                    return;
                }
                this.append(role, renderAgentRichText(message.content || ''), '', {
                    timestamp: message.createdAt || new Date()
                });
            });
            this.removeThinking();
            this.syncConversationState();
            this.scrollMessagesToBottom();
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
            if (this.attachTriggerEl) {
                this.attachTriggerEl.disabled = isLoading;
            }
            if (this.attachmentInputEl) {
                this.attachmentInputEl.disabled = isLoading;
            }
        }

        buildPageContext(options = {}) {
            const context = {
                page: document.body?.dataset?.page || document.documentElement?.dataset?.page || location.pathname
            };
            if (options.agentCommand) {
                context.agentCommand = options.agentCommand;
            }
            if (options.agentCommandSource) {
                context.agentCommandSource = options.agentCommandSource;
            }
            if (options.agentCommandAction) {
                context.agentCommandAction = options.agentCommandAction;
            }
            if (options.questionDraft && typeof options.questionDraft === 'object') {
                context.questionDraft = { ...options.questionDraft };
            }
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
            if (options.currentCourseId || currentCourseId) {
                context.currentCourseId = Number(options.currentCourseId || currentCourseId);
            }
            const currentClassId = window.agentCurrentClassId || window.currentClassId;
            if (options.currentClassId || currentClassId) {
                context.currentClassId = Number(options.currentClassId || currentClassId);
            }
            const questionFilter = window.agentQuestionFilter || window.currentQuestionFilter;
            if (questionFilter && typeof questionFilter === 'object') {
                context.questionFilter = { ...questionFilter };
            }
            const attachments = this.cloneAttachmentsForContext(options.attachments || this.pendingAttachments);
            if (attachments.length > 0) {
                context.attachments = attachments;
            }
            return context;
        }

        prepareOutgoingMessage(rawMessage, options = {}) {
            const message = String(rawMessage || '').trim();
            if (!message) {
                return null;
            }
            const detail = {
                message,
                displayMessage: options.displayMessage || message,
                options: { ...options }
            };
            const event = new CustomEvent('agent-before-send', {
                bubbles: true,
                cancelable: true,
                detail
            });
            this.formEl?.dispatchEvent(event);
            if (event.defaultPrevented) {
                return null;
            }
            const preparedMessage = String(detail.message || '').trim();
            if (!preparedMessage) {
                return null;
            }
            const preparedOptions = detail.options && typeof detail.options === 'object'
                ? { ...detail.options }
                : {};
            preparedOptions.displayMessage = String(detail.displayMessage || preparedMessage).trim() || preparedMessage;
            return {
                message: preparedMessage,
                options: preparedOptions
            };
        }

        submitInputMessage() {
            const rawMessage = this.inputEl.value.trim();
            const fallbackMessage = this.pendingAttachments.length > 0
                ? '请结合我上传的附件继续处理。'
                : '';
            const prepared = this.prepareOutgoingMessage(rawMessage || fallbackMessage, {
                displayMessage: rawMessage || fallbackMessage
            });
            if (!prepared) {
                return;
            }
            this.send(prepared.message, prepared.options);
        }

        async send(message, options = {}) {
            if (!message) {
                return;
            }
            const outgoingAttachments = this.cloneAttachmentsForContext(options.attachments || this.pendingAttachments);
            this.append(
                'user',
                `${renderAgentRichText(options.displayMessage || message)}${this.buildOutgoingAttachmentHtml(outgoingAttachments)}`,
                '',
                options
            );
            this.inputEl.value = '';
            this.currentController = new AbortController();
            this.setLoading(true);
            this.showThinking();
            try {
                if (options.forceJson === true) {
                    await this.sendJsonChat(message, options);
                } else {
                    await this.sendStream(message, options);
                }
                this.removeThinking();
                if (!options.attachments && outgoingAttachments.length > 0) {
                    this.pendingAttachments = [];
                    this.renderPendingAttachments();
                }
            } catch (error) {
                if (error?.name === 'AgentStreamFallback') {
                    try {
                        await this.sendJsonChat(message, options);
                        this.removeThinking();
                        if (!options.attachments && outgoingAttachments.length > 0) {
                            this.pendingAttachments = [];
                            this.renderPendingAttachments();
                        }
                        return;
                    } catch (fallbackError) {
                        this.removeThinking();
                        if (fallbackError?.name === 'AbortError') {
                            this.appendFeedbackMessage('info', '已暂停本次请求。');
                        } else {
                            this.appendFeedbackMessage('error', fallbackError.message || '请求失败');
                        }
                        return;
                    }
                }
                this.removeThinking();
                if (error?.name === 'AbortError') {
                    this.appendFeedbackMessage('info', '已暂停本次请求。');
                } else {
                    this.appendFeedbackMessage('error', error.message || '请求失败');
                }
            } finally {
                this.currentController = null;
                this.setLoading(false);
            }
        }

        async sendJsonChat(message, options = {}) {
            const requestBody = {
                message,
                sessionId: this.sessionId,
                context: this.buildPageContext(options)
            };
            const payload = await this.request('/api/agent/chat', requestBody, this.currentController);
            this.sessionId = payload.sessionId || this.sessionId;
            this.persistCurrentSessionId(this.sessionId);
            this.notifySessionChanged(this.sessionId);
            this.renderResponse(payload);
            return payload;
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

        async sendStream(message, options = {}) {
            const payload = await this.requestStream('/api/agent/chat/stream', {
                message,
                sessionId: this.sessionId,
                context: this.buildPageContext(options)
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
                    body.innerHTML = renderAgentRichText(text);
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

        renderResponse(payload, options = {}) {
            if (payload.responseType === 'ACTION_PREVIEW') {
                this.renderPreview(payload.actionPreview, { ...options, actionState: payload.actionState });
                initMetaToggles(this.root);
                this.scrollMessagesToBottom();
                return;
            }
            if (payload.responseType === 'DATA') {
                const structuredWebSearchHtml = renderStructuredWebSearchResponse(payload.data, payload.message);
                if (structuredWebSearchHtml) {
                    this.append('agent', structuredWebSearchHtml, '', options);
                    this.scrollMessagesToBottom();
                    return;
                }
                if (shouldRenderQueryAsMarkdown(payload.data)) {
                    const queryMarkdown = renderQueryMarkdown(payload.data, payload.message);
                    if (queryMarkdown) {
                        this.append('agent', renderAgentRichText(queryMarkdown), '', options);
                        this.scrollMessagesToBottom();
                        return;
                    }
                }
                if (!isStructuredDataPayload(payload.data)) {
                    this.append('agent', renderAgentRichText(getPlainDataText(payload)), '', options);
                    this.scrollMessagesToBottom();
                    return;
                }
                const responseTitle = buildResponseTitle(payload);
                this.append('agent', `
                    <p><strong>${renderAgentText(responseTitle)}</strong></p>
                    <div class="agent-data-result">${renderValue(payload.data)}</div>
                `, '', options);
                initMetaToggles(this.root);
                this.scrollMessagesToBottom();
                return;
            }
            this.append('agent', renderAgentRichText(payload.message || '已处理'), '', options);
            this.scrollMessagesToBottom();
        }

        renderPreview(preview, options = {}) {
            if (!preview) {
                this.appendFeedbackMessage('warning', '缺少操作预览。');
                return;
            }
            const actionState = options.actionState || null;
            const actionStatus = normalizeActionStatus(actionState?.status);
            const isTerminal = isTerminalActionStatus(actionStatus);
            const footerHtml = isTerminal
                ? `
                    <div class="agent-action-status agent-action-status-${escapeHtml(actionStatus.toLowerCase())}">
                        <i class="fa fa-check-circle"></i> ${escapeHtml(AGENT_VALUE_LABELS[actionStatus] || actionStatus)}
                    </div>
                `
                : `
                    <button type="button" class="btn btn-sm btn-primary" data-agent-confirm="${escapeHtml(preview.actionId)}">
                        <i class="fa fa-check"></i> 确认执行
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-secondary" data-agent-dismiss>
                        取消
                    </button>
                `;
            const resultHtml = isTerminal
                ? `<div class="agent-action-history-result">${renderActionExecutionResult(preview, actionState)}</div>`
                : '';
            const cardId = `agent-action-${preview.actionId}`;
            this.append('agent', `
                <div class="agent-action-card" id="${cardId}">
                    <div class="agent-action-header">
                        <div>
                            <strong>${escapeHtml(preview.title || preview.intent)}</strong>
                            <span class="agent-risk-badge">${escapeHtml(preview.riskLevel || 'LOW')}</span>
                        </div>
                        <span class="agent-action-id">#${escapeHtml(preview.actionId)}</span>
                    </div>
                    <p>${escapeHtml(preview.summary || '请确认是否执行该操作。')}</p>
                    <div class="agent-data-result">${renderValue(preview.preview || {})}</div>
                    ${resultHtml}
                    <div class="agent-action-footer">
                        ${footerHtml}
                    </div>
                </div>
            `, '', options);
            const card = this.root.querySelector(`#${cardId}`);
            const confirmButton = card?.querySelector('[data-agent-confirm]');
            const dismissButton = card?.querySelector('[data-agent-dismiss]');
            if (!isTerminal) {
                confirmButton?.addEventListener('click', () => this.confirm(preview, confirmButton));
                dismissButton?.addEventListener('click', () => this.cancel(preview, dismissButton, confirmButton, card));
            }
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
                this.appendFeedbackMessage('info', result.message || '操作已取消。');
            } catch (error) {
                dismissButton.disabled = false;
                dismissButton.innerHTML = '取消';
                this.appendFeedbackMessage('error', error.message || '取消操作失败');
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
                    this.appendFeedbackMessage('warning', '二级确认未通过，操作仍在等待确认。');
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
                this.append('agent', renderActionExecutionResult(preview, result));
                initMetaToggles(this.root);
                this.scrollMessagesToBottom();
            } catch (error) {
                button.disabled = false;
                button.innerHTML = '<i class="fa fa-check"></i> 确认执行';
                if (dismissButton) {
                    dismissButton.disabled = false;
                }
                this.appendFeedbackMessage('error', error.message || '确认执行失败');
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
