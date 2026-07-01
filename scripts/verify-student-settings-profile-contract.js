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

[
  'id="profilePersistenceNotice"',
  '姓名、邮箱、手机号会同步到账号资料；专业、年级以当前浏览器补充信息为准。',
  'id="classReadOnlyNotice"',
  '班级由系统班级关系自动生成，当前页不可手动修改。',
  'id="profile-class" type="text" class="form-control" value="" readonly',
  "showMessage('账号资料已保存，专业/年级已保存在当前浏览器。', 'success');"
].forEach(snippet => {
  assertIncludes(settingsContent, snippet, 'student settings profile contract mismatch.');
});

[
  "major: document.getElementById('profile-major')?.value?.trim() || ''",
  "grade: document.getElementById('gradeSelect')?.value || ''",
  "className: document.getElementById('profile-class')?.value?.trim() || ''",
  "showMessage('个人信息保存成功', 'success');"
].forEach(snippet => {
  assertNotIncludes(settingsContent, snippet, 'student settings profile should no longer imply all academic fields persist through the profile API.');
});

console.log('student settings profile contract OK');
