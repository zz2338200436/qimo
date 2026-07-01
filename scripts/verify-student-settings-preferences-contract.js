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

const settingsContent = fs.readFileSync('frontend/dist/student-settings.html', 'utf8');
const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');

[
  'notificationSettings: false,',
  'privacySettings: false,',
  'avatarUpload: true,'
].forEach(snippet => {
  assertIncludes(apiContent, snippet, 'student capability matrix should align with local-only preference semantics.');
});

[
  'notificationSettings: true,',
  'privacySettings: true,'
].forEach(snippet => {
  assertNotIncludes(apiContent, snippet, 'student capability matrix should not advertise placeholder preference endpoints as connected.');
});

[
  'id="notificationStorageNotice"',
  '当前环境下通知偏好以本浏览器保存为准。',
  'id="privacyStorageNotice"',
  '当前环境下隐私设置以本浏览器保存为准。',
  "showMessage('通知设置已保存在当前浏览器。', 'success');",
  "showMessage('隐私设置已保存在当前浏览器。', 'success');"
].forEach(snippet => {
  assertIncludes(settingsContent, snippet, 'student settings preferences contract mismatch.');
});

[
  'studentAPI.getNotificationSettings()',
  'studentAPI.updateNotificationSettings(formData)',
  'studentAPI.getPrivacySettings()',
  'studentAPI.updatePrivacySettings(formData)',
  "showMessage('通知设置保存成功', 'success');",
  "showMessage('隐私设置保存成功', 'success');",
  '通知设置已保存在当前浏览器，服务器同步失败',
  '隐私设置已保存在当前浏览器，服务器同步失败',
  '已加载当前浏览器保存的通知设置。',
  '已加载当前浏览器保存的隐私设置。'
].forEach(snippet => {
  assertNotIncludes(settingsContent, snippet, 'student settings preferences should no longer imply server persistence or call placeholder preference APIs.');
});

console.log('student settings preferences contract OK');
