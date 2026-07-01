/**
 * 国际化管理模块 (I18n Manager)
 * 提供语言切换、文本翻译和语言偏好存储功能
 */
const I18nManager = (function() {
    // 配置
    const STORAGE_KEY = 'preferred_language';
    const DEFAULT_LANG = 'zh-CN';
    const SUPPORTED_LANGS = ['zh-CN', 'en-US', 'ja-JP'];
    
    // 状态
    let currentLang = DEFAULT_LANG;
    let translations = {};
    let isInitialized = false;
    
    // 内置默认翻译（后备方案）
    const DEFAULT_TRANSLATIONS = {
        'zh-CN': {
            login: {
                student: { title: '学生登录', subtitle: '智能后台学习辅助系统' },
                teacher: { title: '教师登录', subtitle: '智能后台学习辅助系统' },
                form: {
                    username: '用户名', username_placeholder: '请输入您的用户名',
                    password: '密码', password_placeholder: '请输入您的密码',
                    captcha: '验证码', captcha_placeholder: '请输入验证码',
                    remember_me: '记住我', submit: '登录', submitting: '登录中...'
                },
                links: { forgot_password: '忘记密码？', contact_admin: '联系管理员', no_account: '还没有账号？', switch_to_teacher: '您是教师？', switch_to_student: '您是学生？', click_to_login: '点击登录' },
                errors: { empty_fields: '请填写完整的登录信息', invalid_credentials: '登录失败：请检查您的用户名和密码', not_student: '登录失败：您不是学生用户', not_teacher: '登录失败：您不是教师用户', no_user_info: '登录失败：无法获取用户信息', no_role_info: '登录失败：无法获取用户角色信息', request_failed: '请求失败，请稍后重试' }
            }
        },
        'en-US': {
            login: {
                student: { title: 'Student Login', subtitle: 'Intelligent Learning Assistant System' },
                teacher: { title: 'Teacher Login', subtitle: 'Intelligent Learning Assistant System' },
                form: {
                    username: 'Username', username_placeholder: 'Enter your username',
                    password: 'Password', password_placeholder: 'Enter your password',
                    captcha: 'Captcha', captcha_placeholder: 'Enter captcha',
                    remember_me: 'Remember me', submit: 'Login', submitting: 'Logging in...'
                },
                links: { forgot_password: 'Forgot password?', contact_admin: 'Contact Admin', no_account: "Don't have an account?", switch_to_teacher: 'Are you a teacher?', switch_to_student: 'Are you a student?', click_to_login: 'Click to login' },
                errors: { empty_fields: 'Please fill in all required fields', invalid_credentials: 'Login failed: Please check your username and password', not_student: 'Login failed: You are not a student user', not_teacher: 'Login failed: You are not a teacher user', no_user_info: 'Login failed: Unable to get user information', no_role_info: 'Login failed: Unable to get user role information', request_failed: 'Request failed, please try again later' }
            }
        },
        'ja-JP': {
            login: {
                student: { title: '学生ログイン', subtitle: 'インテリジェント学習支援システム' },
                teacher: { title: '教師ログイン', subtitle: 'インテリジェント学習支援システム' },
                form: {
                    username: 'ユーザー名', username_placeholder: 'ユーザー名を入力してください',
                    password: 'パスワード', password_placeholder: 'パスワードを入力してください',
                    captcha: '認証コード', captcha_placeholder: '認証コードを入力してください',
                    remember_me: 'ログイン状態を保持', submit: 'ログイン', submitting: 'ログイン中...'
                },
                links: { forgot_password: 'パスワードをお忘れですか？', contact_admin: '管理者に連絡', no_account: 'アカウントをお持ちでないですか？', switch_to_teacher: '教師の方はこちら', switch_to_student: '学生の方はこちら', click_to_login: 'ログインはこちら' },
                errors: { empty_fields: 'すべての必須項目を入力してください', invalid_credentials: 'ログイン失敗：ユーザー名またはパスワードを確認してください', not_student: 'ログイン失敗：学生ユーザーではありません', not_teacher: 'ログイン失敗：教師ユーザーではありません', no_user_info: 'ログイン失敗：ユーザー情報を取得できません', no_role_info: 'ログイン失敗：ユーザー権限情報を取得できません', request_failed: 'リクエストに失敗しました。後でもう一度お試しください' }
            }
        }
    };

    /**
     * 加载翻译文件
     */
    async function loadTranslations(lang) {
        try {
            console.log(`I18n: Fetching translations from /i18n/${lang}.json`);
            const response = await fetch(`/i18n/${lang}.json`);
            console.log(`I18n: Fetch response status: ${response.status}`);
            if (!response.ok) {
                throw new Error(`Failed to load translations for ${lang}, status: ${response.status}`);
            }
            const data = await response.json();
            console.log(`I18n: Successfully loaded translations for ${lang}`, data);
            return data;
        } catch (error) {
            console.warn(`I18n: Failed to load ${lang} translations:`, error);
            return null;
        }
    }

    /**
     * 获取嵌套对象的值
     */
    function getNestedValue(obj, path) {
        return path.split('.').reduce((current, key) => {
            return current && current[key] !== undefined ? current[key] : null;
        }, obj);
    }

    /**
     * 检测浏览器语言
     */
    function detectBrowserLanguage() {
        const browserLang = navigator.language || navigator.userLanguage;
        if (browserLang.startsWith('zh')) {
            return 'zh-CN';
        } else if (browserLang.startsWith('en')) {
            return 'en-US';
        } else if (browserLang.startsWith('ja')) {
            return 'ja-JP';
        }
        return DEFAULT_LANG;
    }

    /**
     * 初始化国际化
     */
    async function init() {
        console.log('I18n: init() called, isInitialized:', isInitialized);
        if (isInitialized) {
            console.log('I18n: Already initialized, skipping');
            return;
        }

        try {
            // 优先从 localStorage 读取语言偏好
            let savedLang = null;
            try {
                savedLang = localStorage.getItem(STORAGE_KEY);
                console.log('I18n: Saved language from localStorage:', savedLang);
            } catch (e) {
                console.warn('I18n: localStorage not available');
            }

            // 确定使用的语言
            if (savedLang && SUPPORTED_LANGS.includes(savedLang)) {
                currentLang = savedLang;
                console.log('I18n: Using saved language:', currentLang);
            } else {
                currentLang = detectBrowserLanguage();
                console.log('I18n: Using detected browser language:', currentLang);
            }

            // 加载翻译文件
            const loadedTranslations = await loadTranslations(currentLang);
            if (loadedTranslations) {
                translations = loadedTranslations;
                console.log('I18n: Loaded translations from file');
            } else {
                // 使用内置默认翻译
                translations = DEFAULT_TRANSLATIONS[currentLang] || DEFAULT_TRANSLATIONS[DEFAULT_LANG];
                console.log('I18n: Using default translations');
            }

            isInitialized = true;
            console.log('I18n: Initialization complete, currentLang:', currentLang);
            
            // 更新页面翻译
            updatePageTranslations();
            
            // 更新语言切换按钮状态
            updateLanguageSwitcher();
        } catch (error) {
            console.error('I18n: Initialization failed:', error);
            // 即使初始化失败，也设置为已初始化，避免重复尝试
            isInitialized = true;
            // 使用默认翻译
            translations = DEFAULT_TRANSLATIONS[DEFAULT_LANG];
            currentLang = DEFAULT_LANG;
        }
    }

    /**
     * 获取翻译文本
     */
    function t(key, params = {}) {
        const value = getNestedValue(translations, key);
        
        if (value === null) {
            console.warn(`I18n: Missing translation for key "${key}" in language "${currentLang}"`);
            return key;
        }

        // 支持参数替换 {param}
        let result = value;
        Object.keys(params).forEach(param => {
            result = result.replace(new RegExp(`\\{${param}\\}`, 'g'), params[param]);
        });

        return result;
    }

    /**
     * 设置语言
     */
    async function setLanguage(lang) {
        console.log(`I18n: setLanguage called with lang="${lang}", currentLang="${currentLang}", isInitialized=${isInitialized}`);
        
        // 等待初始化完成
        if (!isInitialized) {
            console.log('I18n: Waiting for initialization to complete...');
            await init();
        }
        
        if (!SUPPORTED_LANGS.includes(lang)) {
            console.warn(`I18n: Unsupported language "${lang}"`);
            return false;
        }

        if (lang === currentLang) {
            console.log('I18n: Language already set, skipping');
            return true;
        }

        try {
            // 加载新语言的翻译
            console.log(`I18n: Loading translations for ${lang}...`);
            const loadedTranslations = await loadTranslations(lang);
            if (loadedTranslations) {
                translations = loadedTranslations;
                console.log(`I18n: Loaded translations from file for ${lang}`);
            } else {
                // 使用内置默认翻译
                translations = DEFAULT_TRANSLATIONS[lang] || DEFAULT_TRANSLATIONS[DEFAULT_LANG];
                console.log(`I18n: Using default translations for ${lang}`);
            }
            currentLang = lang;
            console.log(`I18n: currentLang updated to ${currentLang}`);

            // 保存语言偏好
            try {
                localStorage.setItem(STORAGE_KEY, lang);
                console.log(`I18n: Saved language preference: ${lang}`);
            } catch (e) {
                console.warn('I18n: Failed to save language preference');
            }

            // 更新页面翻译
            console.log('I18n: Updating page translations...');
            updatePageTranslations();
            
            // 更新语言切换按钮状态
            updateLanguageSwitcher();
            
            console.log('I18n: Language switch complete');
            return true;
        } catch (error) {
            console.error('I18n: setLanguage failed:', error);
            return false;
        }
    }

    /**
     * 获取当前语言
     */
    function getLanguage() {
        return currentLang;
    }

    /**
     * 更新页面所有翻译元素
     */
    function updatePageTranslations() {
        console.log('I18n: updatePageTranslations called, currentLang:', currentLang);
        
        // 更新 data-i18n 属性的元素（文本内容）
        const i18nElements = document.querySelectorAll('[data-i18n]');
        console.log('I18n: Found', i18nElements.length, 'elements with data-i18n attribute');
        
        i18nElements.forEach(element => {
            const key = element.getAttribute('data-i18n');
            const translation = t(key);
            if (translation !== key) {
                element.textContent = translation;
            }
        });

        // 更新 data-i18n-placeholder 属性的元素
        document.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
            const key = element.getAttribute('data-i18n-placeholder');
            const translation = t(key);
            if (translation !== key) {
                element.placeholder = translation;
            }
        });

        // 更新 data-i18n-title 属性的元素
        document.querySelectorAll('[data-i18n-title]').forEach(element => {
            const key = element.getAttribute('data-i18n-title');
            const translation = t(key);
            if (translation !== key) {
                element.title = translation;
            }
        });

        // 更新页面标题
        const titleElement = document.querySelector('[data-i18n-document-title]');
        if (titleElement) {
            const key = titleElement.getAttribute('data-i18n-document-title');
            const translation = t(key);
            if (translation !== key) {
                document.title = translation;
            }
        }
        
        console.log('I18n: Page translations updated');
    }

    /**
     * 更新语言切换按钮状态
     */
    function updateLanguageSwitcher() {
        document.querySelectorAll('[data-lang]').forEach(btn => {
            const lang = btn.getAttribute('data-lang');
            if (lang === currentLang) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    }

    /**
     * 绑定语言切换事件
     */
    function bindLanguageSwitcher() {
        document.querySelectorAll('[data-lang]').forEach(btn => {
            // 防止重复绑定
            if (btn.hasAttribute('data-i18n-bound')) {
                return;
            }
            btn.setAttribute('data-i18n-bound', 'true');
            
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                console.log('I18n: Language button clicked, lang:', btn.getAttribute('data-lang'));
                const lang = btn.getAttribute('data-lang');
                await setLanguage(lang);
            });
        });
        console.log('I18n: Language switcher bound');
    }

    // 公开 API
    return {
        init,
        t,
        setLanguage,
        getLanguage,
        updatePageTranslations,
        bindLanguageSwitcher,
        SUPPORTED_LANGS
    };
})();

// 导出到全局
window.I18nManager = I18nManager;
