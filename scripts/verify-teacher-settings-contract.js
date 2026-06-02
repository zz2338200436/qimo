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

const pageContent = fs.readFileSync('frontend/dist/teacher-settings.html', 'utf8');

[
  "const confirmPassword = document.getElementById('confirmPassword').value;",
  "const response = await apiService.post('/api/auth/change-password', {",
  'currentPassword: currentPassword,',
  'newPassword: newPassword,',
  'confirmPassword: confirmPassword'
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'teacher settings password contract mismatch.');
});

[
  "const response = await apiService.put('/api/auth/notification-settings', settings);",
  "localStorage.setItem('teacherNotificationSettings', JSON.stringify(settings));",
  '当前环境下支持会话级通知偏好同步，本次修改会同时保存在当前浏览器。',
  '教师通知偏好会同步到当前登录会话，并保留当前浏览器副本；当前环境下尚未接通账号级持久化。'
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'teacher settings notification preferences contract mismatch.');
});

assertNotIncludes(
  pageContent,
  "const response = await apiService.post('/api/auth/change-password', {\n                    currentPassword: currentPassword,\n                    newPassword: newPassword\n                });",
  'teacher settings should not omit confirmPassword when submitting change-password.'
);

[
  '当前环境下支持查看通知偏好，本次修改会先保存在当前浏览器。',
  '教师通知偏好会先保存在当前浏览器，后续接通账号级同步后可继续沿用。',
  "showMessage('通知设置已保存到当前浏览器', 'success');",
  '通知偏好已同步到当前账号，并保留当前浏览器副本。'
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'teacher settings should no longer describe notification preferences as browser-local only.'
  );
});

console.log('teacher settings contract OK');
