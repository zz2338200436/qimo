const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(`${message} Found unexpected snippet: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/student-notifications.html', 'utf8');

[
  "let notificationListState = {",
  "function getActiveNotificationState() {",
  "async function refreshUnreadNotificationCount() {",
  "async function refreshNotificationListView(options = {}) {",
  'notificationListState = {',
  'page,',
  'size,',
  'filter',
  'await refreshUnreadNotificationCount();',
  "await refreshNotificationListView();",
  "showMessage('通知已标记为已读', 'success');",
  "showMessage('所有通知已标记为已读', 'success');",
  "showMessage('通知已删除', 'success');",
  "showMessage('已读通知已清空', 'success');"
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'student notifications contract mismatch.');
});

[
  "const unreadNotifications = document.querySelectorAll('.notification-item:not(.read)');",
  "const readNotifications = document.querySelectorAll('.notification-item.read');",
  "const unreadCount = document.querySelectorAll('.notification-item:not(.read)').length;",
  "notificationItem.classList.add('read');",
  "markAsReadBtn.remove();",
  "notificationItem.style.opacity = '0';",
  "notification.remove();",
  "showMessage('没有未读通知', 'info');",
  "showMessage('没有已读通知可以删除', 'info');"
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'student notifications should not derive server notification state from current DOM snapshot.'
  );
});

console.log('student notifications contract OK');
