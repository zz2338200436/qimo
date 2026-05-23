const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(message + ` Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(message + ` Unexpected: ${needle}`);
  }
}

const teacherLogin = fs.readFileSync('frontend/dist/teacher-login.html', 'utf8');
const studentLogin = fs.readFileSync('frontend/dist/student-login.html', 'utf8');
const apiJs = fs.readFileSync('frontend/dist/api.js', 'utf8');
const gatewayYaml = fs.readFileSync('gateway/src/main/resources/application.yml', 'utf8');

for (const [name, content] of [
  ['teacher-login.html', teacherLogin],
  ['student-login.html', studentLogin]
]) {
  assertIncludes(content, '/api/auth/captcha', `${name} should load captcha from gateway auth route.`);
  assertNotIncludes(content, "/api/public/captcha", `${name} should not use legacy captcha endpoint.`);
  assertIncludes(content, 'CaptchaKey', `${name} should track captchaKey for login submission.`);
}

assertIncludes(apiJs, 'async login(username, password, captcha, captchaKey)', 'AuthAPI.login should accept captchaKey.');
assertIncludes(apiJs, 'captchaKey', 'AuthAPI.login should send captchaKey in login payload.');
assertIncludes(gatewayYaml, '- X-Captcha-Key', 'Gateway should expose X-Captcha-Key so frontend can read captcha headers.');
assertIncludes(apiJs, 'function clearAuthSession()', 'api.js should provide a shared auth session cleanup helper.');
assertIncludes(apiJs, 'function persistAuthSession(authData)', 'api.js should provide a shared auth session persistence helper.');
assertIncludes(apiJs, 'return normalizedUrl.pathname === \'/api/auth/login\' || normalizedUrl.pathname === \'/api/auth/refresh\';', 'api.js should detect auth login and refresh requests when attaching auth headers.');
assertIncludes(apiJs, 'const accessToken = isLoginRequest ? null : getAccessToken();', 'login requests should not inherit stale Authorization headers.');
assertIncludes(teacherLogin, 'window.clearAuthSession()', 'teacher login should clear stale auth state before submitting a new login.');
assertIncludes(studentLogin, 'window.clearAuthSession()', 'student login should clear stale auth state before submitting a new login.');
assertIncludes(studentLogin, 'window.persistAuthSession(result.data);', 'student login should persist the full auth session payload, not only token/user.');
assertIncludes(apiJs, 'clearAuthSession();', 'AuthAPI.logout should delegate to the shared auth cleanup helper.');
assertIncludes(fs.readFileSync('frontend/dist/components/user-dropdown.html', 'utf8'), 'sessionStorage.removeItem(\'refreshToken\');', 'user dropdown logout should clear refreshToken.');
assertIncludes(fs.readFileSync('frontend/dist/components/user-dropdown.html', 'utf8'), 'sessionStorage.removeItem(\'activeRole\');', 'user dropdown logout should clear activeRole.');
assertIncludes(fs.readFileSync('frontend/dist/components/user-dropdown.html', 'utf8'), 'sessionStorage.removeItem(\'role\');', 'user dropdown logout should clear role.');

console.log('login page contract OK');
