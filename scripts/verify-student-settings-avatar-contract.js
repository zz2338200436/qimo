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

const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');
const settingsContent = fs.readFileSync('frontend/dist/student-settings.html', 'utf8');

[
  'avatarUpload: true',
  "avatarUpload: '当前 JWT 微服务环境已接通头像上传接口。'",
  "uploadAvatar(avatarData) {",
  "this.apiService.post('/api/student/upload-avatar', avatarData)"
].forEach(snippet => {
  assertIncludes(apiContent, snippet, 'student avatar upload api contract mismatch.');
});

[
  "avatarUpload: false",
  "当前 JWT 微服务环境暂未提供头像上传接口。"
].forEach(snippet => {
  assertNotIncludes(apiContent, snippet, 'student avatar upload api should no longer use false capability or multipart upload.');
});

[
  "uploadAvatar(formData)",
  "this.apiService.post('/api/student/upload-avatar', formData)"
].forEach(snippet => {
  assertNotIncludes(apiContent, snippet, 'student avatar upload api should no longer submit multipart/form-data payloads.');
});

[
  "const avatarDataUrl = event.target.result;",
  "uploadAvatar(avatarDataUrl);",
  "const response = await studentAPI.uploadAvatar({ avatar: avatarDataUrl });",
  "showMessage('头像上传成功', 'success');"
].forEach(snippet => {
  assertIncludes(settingsContent, snippet, 'student settings avatar contract mismatch.');
});

[
  "const formData = new FormData();",
  "formData.append('avatar', file);",
  "const response = await studentAPI.uploadAvatar(formData);",
  "showMessage(response.message, 'info');"
].forEach(snippet => {
  assertNotIncludes(settingsContent, snippet, 'student settings avatar upload should not keep old multipart/unsupported flow.');
});

console.log('student settings avatar contract OK');
