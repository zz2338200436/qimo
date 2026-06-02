(function attachCommonUi(global) {
    const SIDEBAR_STORAGE_KEY = 'sidebarCollapsed';

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

        if (!sidebar) {
            return;
        }

        sidebar.classList.toggle('collapsed', !!isCollapsed);
        if (mainContent) {
            mainContent.classList.toggle('collapsed', !!isCollapsed);
        }
    }

    function isSidebarCollapsed() {
        return localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
    }

    function initializeSidebar(options = {}) {
        applySidebarState(isSidebarCollapsed(), options);
    }

    function toggleSidebar(options = {}) {
        const nextCollapsed = !isSidebarCollapsed();
        localStorage.setItem(SIDEBAR_STORAGE_KEY, String(nextCollapsed));
        applySidebarState(nextCollapsed, options);
        return nextCollapsed;
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
            global.location.href = options.redirectUrl || 'teacher-login.html?v=20260523-1';
            return false;
        }

        try {
            JSON.parse(user);
        } catch (error) {
            console.warn('解析教师登录信息失败，已清理本地状态:', error);
            sessionStorage.removeItem('token');
            sessionStorage.removeItem('user');
            global.location.href = options.redirectUrl || 'teacher-login.html?v=20260523-1';
            return false;
        }

        return true;
    }

    async function loadUserDropdown(containerId, userType) {
        const container = document.getElementById(containerId);
        if (!container) {
            return false;
        }

        const response = await fetch('components/user-dropdown.html?v=20260523-1');
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

    async function loadTeacherSidebar(containerId, options = {}) {
        const container = document.getElementById(containerId);
        if (!container) {
            return false;
        }

        const response = await fetch(options.componentUrl || 'components/teacher-sidebar-nav.html?v=20260523-1');
        if (!response.ok) {
            throw new Error(`加载教师侧边栏失败: ${response.status} ${response.statusText}`);
        }

        container.innerHTML = await response.text();

        global.setTimeout(() => {
            if (typeof global.initSidebar === 'function') {
                global.initSidebar();
            } else {
                highlightActiveNavItem({ selector: '.sidebar .menu-item, .menu-item' });
                initializeSidebar();
            }
        }, Number.isFinite(options.delayMs) ? options.delayMs : 100);

        return true;
    }

    function bindSidebarToggle(buttonId, options = {}) {
        const button = document.getElementById(buttonId);
        if (!button) {
            return;
        }

        button.addEventListener('click', () => toggleSidebar(options));
    }

    global.CommonUI = {
        highlightActiveNavItem,
        initializeSidebar,
        toggleSidebar,
        showToast,
        showErrorToast,
        showSuccessToast,
        showMessage,
        showButtonLoading,
        hideButtonLoading,
        reportResourceLoadFailure,
        handleApiError,
        requireStudentSession,
        requireTeacherSession,
        loadUserDropdown,
        loadTeacherUserDropdown,
        loadTeacherSidebar,
        bindSidebarToggle,
        isSidebarCollapsed
    };
})(window);
