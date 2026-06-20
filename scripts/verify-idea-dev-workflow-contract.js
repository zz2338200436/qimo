const fs = require('node:fs');

function read(path) {
  return fs.readFileSync(path, 'utf8');
}

function assertExists(path, message) {
  if (!fs.existsSync(path)) {
    throw new Error(`${message} Missing path: ${path}`);
  }
}

function assertNotExists(path, message) {
  if (fs.existsSync(path)) {
    throw new Error(`${message} Unexpected path: ${path}`);
  }
}

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(`${message} Unexpected: ${needle}`);
  }
}

const compose = read('docker-compose.yml');
const gateway = read('config-server/src/main/resources/config-repo/gateway.yml');
const gatewayDev = fs.existsSync('config-server/src/main/resources/config-repo/gateway-dev.yml')
  ? read('config-server/src/main/resources/config-repo/gateway-dev.yml')
  : '';
const startScript = fs.existsSync('scripts/start-idea-dev-frontend.ps1')
  ? read('scripts/start-idea-dev-frontend.ps1')
  : '';
const infraScript = fs.existsSync('scripts/start-dev-infra.ps1')
  ? read('scripts/start-dev-infra.ps1')
  : '';
const runtimeSmoke = fs.existsSync('scripts/verify-idea-dev-runtime-smoke.js')
  ? read('scripts/verify-idea-dev-runtime-smoke.js')
  : '';
const frontendServer = read('scripts/frontend_dev_server.py');
const frontendReadme = fs.existsSync('frontend/README.md')
  ? read('frontend/README.md')
  : '';
const manualAcceptance = fs.existsSync('docs/manual-acceptance-test-plan.md')
  ? read('docs/manual-acceptance-test-plan.md')
  : '';

assertExists(
  'scripts/start-idea-dev-frontend.ps1',
  'IDEA dev workflow should keep the single retained startup script.'
);
assertNotExists(
  'scripts/start-runtime-smoke-stack.ps1',
  'IDEA dev workflow should not keep the legacy jar smoke-stack launcher.'
);
assertNotExists(
  'scripts/start-java-service.ps1',
  'IDEA dev workflow should not keep the legacy single-service jar launcher.'
);
assertNotExists(
  'scripts/verify-start-java-service-contract.ps1',
  'IDEA dev workflow should not keep verifier files that only validate removed jar launchers.'
);
assertNotExists(
  'scripts/verify-runtime-smoke-stack-agent-contract.js',
  'IDEA dev workflow should not keep verifier files that only validate removed jar launchers.'
);
assertNotExists(
  'scripts/verify-runtime-smoke-stack-jvm-contract.js',
  'IDEA dev workflow should not keep verifier files that only validate removed jar launchers.'
);
assertNotExists(
  'scripts/verify-runtime-jvm-limits.js',
  'IDEA dev workflow should not keep verifier files that only validate removed jar launchers.'
);

assertIncludes(
  compose,
  './config-server/src/main/resources/config-repo:/config-repo:ro',
  'Docker config-server should mount the local config repo for IDEA development.'
);
assertIncludes(
  compose,
  'SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS: file:/config-repo',
  'Docker config-server should read the mounted config repo instead of jar-bundled config.'
);
assertIncludes(
  gatewayDev,
  'frontend-url: http://localhost:5500',
  'Gateway dev config should route static frontend requests to the IDEA/Python frontend server.'
);
assertIncludes(
  gateway,
  '${LEGACY_FRONTEND_URL:${legacy.frontend-url:http://localhost:8090}}',
  'Gateway static route should allow dev profile config to override the frontend URL without breaking Docker env overrides.'
);
assertIncludes(
  gatewayDev,
  'http://127.0.0.1:5500',
  'Gateway dev CORS should allow the local frontend server.'
);
assertIncludes(
  infraScript,
  'docker compose up -d mysql redis rabbitmq registry-server config-server',
  'Infrastructure startup script should start only infrastructure containers.'
);
assertIncludes(
  startScript,
  'scripts\\start-dev-infra.ps1',
  'IDEA dev startup script should delegate infrastructure startup to scripts/start-dev-infra.ps1.'
);
assertIncludes(
  startScript,
  'frontend_dev_server.py',
  'IDEA dev startup script should start the Python frontend server.'
);
assertNotIncludes(
  startScript,
  'legacy-monolith',
  'IDEA dev startup script should not start the Docker frontend/legacy monolith.'
);
assertIncludes(
  frontendServer,
  'GATEWAY_PORT = 8080',
  'Frontend dev server should proxy API calls to the local IDEA gateway.'
);
assertIncludes(
  runtimeSmoke,
  '/api/auth/captcha',
  'IDEA runtime smoke should verify captcha through the frontend-to-gateway proxy path.'
);
assertIncludes(
  runtimeSmoke,
  'X-Captcha-Key',
  'IDEA runtime smoke should verify captcha key exposure for login.'
);
assertIncludes(
  runtimeSmoke,
  'agent-chat-panel.js',
  'IDEA runtime smoke should verify Agent frontend resources load from the local frontend server.'
);
assertIncludes(
  frontendReadme,
  'start-idea-dev-frontend.ps1',
  'Frontend README should point to the retained IDEA startup script.'
);
assertNotIncludes(
  frontendReadme,
  'start-runtime-smoke-stack.ps1',
  'Frontend README should not point to removed jar startup scripts.'
);
assertNotIncludes(
  frontendReadme,
  'start-java-service.ps1',
  'Frontend README should not point to removed jar startup scripts.'
);
assertNotIncludes(
  manualAcceptance,
  'verify-runtime-jvm-limits.js',
  'Manual acceptance guidance should not point to the removed jar-launcher verifier.'
);
assertIncludes(
  manualAcceptance,
  'verify-idea-dev-workflow-contract.js',
  'Manual acceptance guidance should include the retained IDEA workflow contract.'
);
assertIncludes(
  manualAcceptance,
  'verify-idea-dev-runtime-smoke.js',
  'Manual acceptance guidance should include the retained IDEA runtime smoke.'
);

console.log('IDEA dev workflow contract OK');
