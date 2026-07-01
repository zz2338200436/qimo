(function attachCommonUi(global) {
    const SIDEBAR_STORAGE_KEY = 'sidebarDesktopCollapsed';
    const MOBILE_SIDEBAR_BREAKPOINT = 768;

    function isMobileViewport() {
        return global.innerWidth <= MOBILE_SIDEBAR_BREAKPOINT;
    }

    function getCurrentPageName() {
        return global.location.pathname.split('/').pop() || 'index.html';
    }

    function highlightActiveNavItem(options = {}) {
        const selector = options.selector || '.menu-item, .sidebar .menu-item, .sidebar-nav a';
        const currentPage = options.currentPage || getCurrentPageName();
        const links = document.querySelectorAll(selector);

        links.forEach((link) => {
            link.classList.remove('active');
        });

        links.forEach((link) => {
            const href = link.getAttribute('href');
            if (href === currentPage) {
                link.classList.add('active');
            }
        });
    }

    function applySidebarState(isCollapsed, options = {}) {
        const sidebar = document.getElementById(options.sidebarId || 'sidebar');
        const mainContent = document.getElementById(options.mainContentId || 'mainContent');
        const overlay = document.getElementById(options.overlayId || 'sidebarOverlay');

        if (!sidebar) {
            return;
        }

        if (isMobileViewport()) {
            sidebar.classList.remove('desktop-collapsed');
            sidebar.classList.toggle('mobile-open', !!isCollapsed);
            if (mainContent) {
                mainContent.classList.remove('collapsed');
            }
            if (overlay) {
                overlay.classList.toggle('active', !!isCollapsed);
                overlay.hidden = !isCollapsed;
            }
            document.body.classList.toggle('sidebar-mobile-open', !!isCollapsed);
        } else {
            sidebar.classList.remove('mobile-open');
            sidebar.classList.toggle('desktop-collapsed', !!isCollapsed);
            if (mainContent) {
                mainContent.classList.toggle('collapsed', !!isCollapsed);
            }
            if (overlay) {
                overlay.classList.remove('active');
                overlay.hidden = true;
            }
            document.body.classList.remove('sidebar-mobile-open');
        }
    }

    function isSidebarCollapsed() {
        return localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
    }

    function initializeSidebar(options = {}) {
        applySidebarState(isSidebarCollapsed(), options);
    }

    function toggleSidebar(options = {}) {
        if (isMobileViewport()) {
            const sidebar = document.getElementById(options.sidebarId || 'sidebar');
            const nextOpen = !(sidebar && sidebar.classList.contains('mobile-open'));
            applySidebarState(nextOpen, options);
            return nextOpen;
        }

        const nextCollapsed = !isSidebarCollapsed();
        localStorage.setItem(SIDEBAR_STORAGE_KEY, String(nextCollapsed));
        applySidebarState(nextCollapsed, options);
        return nextCollapsed;
    }

    function closeMobileSidebar(options = {}) {
        if (!isMobileViewport()) {
            return false;
        }
        applySidebarState(false, options);
        return true;
    }

    function ensureSidebarOverlay(options = {}) {
        const overlayId = options.overlayId || 'sidebarOverlay';
        let overlay = document.getElementById(overlayId);
        if (overlay) {
            return overlay;
        }

        overlay = document.createElement('button');
        overlay.type = 'button';
        overlay.id = overlayId;
        overlay.className = 'sidebar-overlay';
        overlay.hidden = true;
        overlay.setAttribute('aria-label', '关闭侧边栏');
        overlay.addEventListener('click', () => closeMobileSidebar(options));
        document.body.appendChild(overlay);
        return overlay;
    }

    function bindSidebarDismissHandlers(options = {}) {
        ensureSidebarOverlay(options);

        if (!global.__commonUiSidebarEscapeBound) {
            document.addEventListener('keydown', (event) => {
                if (event.key === 'Escape') {
                    closeMobileSidebar(options);
                }
            });
            global.__commonUiSidebarEscapeBound = true;
        }

        if (!global.__commonUiSidebarResizeBound) {
            global.addEventListener('resize', () => {
                applySidebarState(isSidebarCollapsed(), options);
            });
            global.__commonUiSidebarResizeBound = true;
        }
    }

    function clearMessageContainer(messageContainer) {
        if (!messageContainer) {
            return;
        }
        messageContainer.innerHTML = '';
    }

    function getToastIconClass(type) {
        if (type === 'success') {
            return 'fa-check-circle';
        }
        if (type === 'danger' || type === 'warning' || type === 'error') {
            return 'fa-exclamation-circle';
        }
        return 'fa-info-circle';
    }

    function getToastLevelClass(type) {
        if (type === 'success') {
            return 'success-notification';
        }
        if (type === 'danger' || type === 'warning' || type === 'error') {
            return 'error-notification';
        }
        return 'notification';
    }

    function showToast(text, type = 'info', options = {}) {
        const duration = Number.isFinite(options.duration) ? options.duration : 3000;
        const dismissible = options.dismissible !== false;
        const host = document.body;
        if (!host) {
            return null;
        }

        const notification = document.createElement('div');
        notification.className = `notification ${getToastLevelClass(type)}`.trim();
        notification.innerHTML = `
            <div class="notification-content">
                <i class="fa ${getToastIconClass(type)}"></i>
                <span>${text}</span>
            </div>
            ${dismissible ? `
                <button type="button" class="notification-close" aria-label="Close">
                    <i class="fa fa-times"></i>
                </button>
            ` : ''}
        `;

        host.appendChild(notification);

        if (dismissible) {
            const closeButton = notification.querySelector('.notification-close');
            if (closeButton) {
                closeButton.addEventListener('click', () => {
                    if (notification.parentElement) {
                        notification.remove();
                    }
                });
            }
        }

        global.setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, duration);

        return notification;
    }

    function showErrorToast(text, options = {}) {
        return showToast(text, 'error', options);
    }

    function showSuccessToast(text, options = {}) {
        return showToast(text, 'success', options);
    }

    function showMessage(text, type = 'info', options = {}) {
        const containerId = options.containerId || 'messageContainer';
        const duration = Number.isFinite(options.duration) ? options.duration : 3500;
        const dismissible = options.dismissible !== false;
        const messageContainer = document.getElementById(containerId);

        if (!messageContainer) {
            return;
        }

        const closeButton = dismissible ? `
            <button class="message-close" type="button" aria-label="关闭提示">
                <i class="fas fa-times"></i>
            </button>
        ` : '';

        messageContainer.innerHTML = `
            <div class="message message-${type}">
                <span class="message-text">${text}</span>
                ${closeButton}
            </div>
        `;

        if (dismissible) {
            const closeBtn = messageContainer.querySelector('.message-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', () => clearMessageContainer(messageContainer));
            }
        }

        global.setTimeout(() => {
            if (messageContainer.firstElementChild) {
                clearMessageContainer(messageContainer);
            }
        }, duration);
    }

    function showButtonLoading(element, options = {}) {
        if (!element) {
            return;
        }
        if (!element.dataset.originalContent) {
            element.dataset.originalContent = element.innerHTML;
        }
        element.innerHTML = options.loadingHtml || '<i class="fa fa-spinner fa-spin"></i> 加载中...';
        element.disabled = true;
    }

    function hideButtonLoading(element) {
        if (!element) {
            return;
        }
        if (element.dataset.originalContent) {
            element.innerHTML = element.dataset.originalContent;
            delete element.dataset.originalContent;
        }
        element.disabled = false;
    }

    function resolveElement(target) {
        if (!target) {
            return null;
        }
        if (typeof target === 'string') {
            return document.querySelector(target);
        }
        return target;
    }

    function clearState(target) {
        const element = resolveElement(target);
        if (!element) {
            return null;
        }
        element.innerHTML = '';
        element.removeAttribute('data-feedback-state');
        return element;
    }

    function buildStateActionHtml(options = {}) {
        if (!options.actionText) {
            return '';
        }
        return `
            <button type="button" class="btn btn-primary btn-sm feedback-state-action">
                ${options.actionText}
            </button>
        `;
    }

    function renderStateBlock(target, options = {}) {
        const element = clearState(target);
        if (!element) {
            return null;
        }

        const iconClass = options.iconClass || 'fa-info-circle';
        const title = options.title || '';
        const description = options.description || '';
        const stateClass = options.stateClass || 'info';

        element.dataset.feedbackState = stateClass;
        element.innerHTML = `
            <div class="feedback-state feedback-state-${stateClass}">
                <div class="feedback-state-icon" aria-hidden="true">
                    <i class="fa ${iconClass}"></i>
                </div>
                <div class="feedback-state-body">
                    ${title ? `<div class="feedback-state-title">${title}</div>` : ''}
                    ${description ? `<div class="feedback-state-text">${description}</div>` : ''}
                    ${buildStateActionHtml(options)}
                </div>
            </div>
        `;

        if (typeof options.onAction === 'function') {
            const actionButton = element.querySelector('.feedback-state-action');
            if (actionButton) {
                actionButton.addEventListener('click', options.onAction);
            }
        }

        return element;
    }

    function setElementLoadingState(target, options = {}) {
        return renderStateBlock(target, {
            stateClass: 'loading',
            iconClass: 'fa-spinner fa-spin',
            title: options.title || '加载中',
            description: options.description || '正在获取数据，请稍候...'
        });
    }

    function renderEmptyState(target, options = {}) {
        return renderStateBlock(target, {
            stateClass: 'empty',
            iconClass: options.iconClass || 'fa-inbox',
            title: options.title || '暂无数据',
            description: options.description || '当前没有可显示的内容。'
        });
    }

    function renderErrorState(target, options = {}) {
        return renderStateBlock(target, {
            stateClass: 'error',
            iconClass: options.iconClass || 'fa-exclamation-circle',
            title: options.title || '加载失败',
            description: options.description || '请求失败，请稍后重试。',
            actionText: options.actionText || '',
            onAction: options.onAction
        });
    }

    function reportResourceLoadFailure(message, options = {}) {
        console.error(message);

        const errorContainer = document.getElementById(options.errorContainerId || 'browser-error-container');
        const errorMessage = document.getElementById(options.errorMessageId || 'browser-error-message');
        const errorStack = document.getElementById(options.errorStackId || 'browser-error-stack');
        const notificationText = options.notificationText || '页面资源加载异常，请刷新后重试';

        if (errorContainer && errorMessage) {
            errorMessage.textContent = notificationText;
            if (errorStack) {
                errorStack.textContent = message;
            }
            errorContainer.style.display = 'block';
        }

        showErrorToast(notificationText);
    }

    function handleApiError(error, options = {}) {
        console.error(options.logPrefix || 'API Error:', error);
        showToast(error?.message || options.fallbackMessage || '请求失败，请重试', options.type || 'danger');
    }

    function requireStudentSession(options = {}) {
        const sessionContext = typeof global.getStudentSessionContext === 'function'
            ? global.getStudentSessionContext()
            : null;
        const user = sessionStorage.getItem('user');

        if (!user && !(sessionContext && sessionContext.canRender)) {
            global.location.href = options.redirectUrl || 'student-login.html';
            return false;
        }

        return true;
    }

    function requireTeacherSession(options = {}) {
        const user = sessionStorage.getItem('user');
        if (!user) {
            global.location.href = options.redirectUrl || 'teacher-login.html?v=20260608-theme-4';
            return false;
        }

        try {
            JSON.parse(user);
        } catch (error) {
            console.warn('解析教师登录信息失败，已清理本地状态:', error);
            sessionStorage.removeItem('token');
            sessionStorage.removeItem('user');
            global.location.href = options.redirectUrl || 'teacher-login.html?v=20260608-theme-4';
            return false;
        }

        return true;
    }

    async function loadUserDropdown(containerId, userType) {
        const container = document.getElementById(containerId);
        if (!container) {
            return false;
        }

        const response = await fetch('components/user-dropdown.html?v=20260608-theme-4');
        if (!response.ok) {
            throw new Error(`加载用户下拉组件失败: ${response.status} ${response.statusText}`);
        }

        container.innerHTML = await response.text();

        const scripts = container.querySelectorAll('script');
        scripts.forEach((oldScript) => {
            const script = document.createElement('script');
            if (oldScript.src) {
                script.src = oldScript.src;
            } else {
                script.textContent = oldScript.textContent;
            }
            document.body.appendChild(script);
            oldScript.remove();
        });

        if (typeof global.UserDropdown !== 'undefined') {
            new global.UserDropdown(containerId, userType);
        }

        return true;
    }

    function loadTeacherUserDropdown(containerId) {
        return loadUserDropdown(containerId, 'teacher');
    }

    async function loadStudentSidebar(containerId, options = {}) {
        const container = document.getElementById(containerId);
        if (!container) {
            return false;
        }

        const response = await fetch(options.componentUrl || 'components/sidebar-nav.html?v=20260608-theme-4');
        if (!response.ok) {
            throw new Error(`加载学生侧边栏失败: ${response.status} ${response.statusText}`);
        }

        container.innerHTML = await response.text();

        global.setTimeout(() => {
            bindSidebarDismissHandlers(options);
            highlightActiveNavItem({ selector: '.sidebar .menu-item, .menu-item' });
            initializeSidebar(options);
        }, Number.isFinite(options.delayMs) ? options.delayMs : 100);

        return true;
    }

    async function loadTeacherSidebar(containerId, options = {}) {
        const container = document.getElementById(containerId);
        if (!container) {
            return false;
        }

        const response = await fetch(options.componentUrl || 'components/teacher-sidebar-nav.html?v=20260608-theme-4');
        if (!response.ok) {
            throw new Error(`加载教师侧边栏失败: ${response.status} ${response.statusText}`);
        }

        container.innerHTML = await response.text();

        global.setTimeout(() => {
            highlightActiveNavItem({ selector: '.sidebar .menu-item, .menu-item' });
            bindSidebarDismissHandlers(options);
            initializeSidebar();
            highlightActiveNavItem({ selector: '.sidebar .menu-item, .menu-item' });
            initializeSidebar();
        }, Number.isFinite(options.delayMs) ? options.delayMs : 100);

        return true;
    }

    function bindSidebarToggle(buttonId, options = {}) {
        const button = document.getElementById(buttonId);
        if (!button) {
            return;
        }

        bindSidebarDismissHandlers(options);
        button.addEventListener('click', () => toggleSidebar(options));
    }

    global.CommonUI = {
        highlightActiveNavItem,
        initializeSidebar,
        toggleSidebar,
        closeMobileSidebar,
        bindSidebarDismissHandlers,
        showToast,
        showErrorToast,
        showSuccessToast,
        showMessage,
        showButtonLoading,
        hideButtonLoading,
        clearState,
        setElementLoadingState,
        renderEmptyState,
        renderErrorState,
        reportResourceLoadFailure,
        handleApiError,
        requireStudentSession,
        requireTeacherSession,
        loadUserDropdown,
        loadTeacherUserDropdown,
        loadStudentSidebar,
        loadTeacherSidebar,
        bindSidebarToggle,
        isSidebarCollapsed
    };
})(window);
