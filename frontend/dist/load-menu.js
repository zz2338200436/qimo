// 加载侧边栏菜单
function loadSidebarMenu() {
    console.log('开始加载侧边栏菜单...');
    
    // 获取侧边栏菜单容器
    const menuContainer = document.querySelector('.sidebar-menu');
    if (!menuContainer) {
        console.error('侧边栏菜单容器未找到');
        return;
    }
    
    // 检查是否有动态菜单注释
    const hasDynamicMenuComment = menuContainer.innerHTML.includes('DYNAMIC_MENU');
    if (hasDynamicMenuComment) {
        console.log('发现动态菜单注释，开始生成菜单...');
        
        // 生成静态菜单HTML
        const menuHTML = `
            <a href="teacher-dashboard.html" class="menu-item">
                <i class="fa fa-dashboard"></i>
                <span>仪表盘</span>
            </a>
            <a href="teacher-courses.html" class="menu-item">
                <i class="fa fa-book"></i>
                <span>课程管理</span>
            </a>
            <a href="teacher-assignments.html" class="menu-item">
                <i class="fa fa-file-text"></i>
                <span>教学任务</span>
            </a>
            <a href="teacher-student-dashboard.html" class="menu-item">
                <i class="fa fa-bar-chart"></i>
                <span>学习数据</span>
            </a>
            <a href="teacher-warning.html" class="menu-item">
                <i class="fa fa-exclamation-triangle"></i>
                <span>学情预警</span>
            </a>
            <a href="teacher-knowledge.html" class="menu-item">
                <i class="fa fa-lightbulb-o"></i>
                <span>知识点分析</span>
            </a>
            <a href="teacher-ai-tools.html" class="menu-item">
                <i class="fa fa-lightbulb-o"></i>
                <span>AI辅助工具</span>
            </a>
            <a href="teacher-settings.html" class="menu-item">
                <i class="fa fa-cog"></i>
                <span>系统设置</span>
            </a>
        `;
        
        // 替换动态菜单注释为实际菜单
        menuContainer.innerHTML = menuHTML;
        console.log('侧边栏菜单加载完成');
    } else {
        console.log('未发现动态菜单注释，跳过菜单加载');
    }
}

// 页面加载完成后自动执行
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadSidebarMenu);
} else {
    loadSidebarMenu();
}