const fs = require('node:fs');

function read(path) {
  return fs.readFileSync(path, 'utf8');
}

function assertExists(path, message) {
  if (!fs.existsSync(path)) {
    throw new Error(`${message} Missing path: ${path}`);
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

assertExists(
  'scripts/start-dev-local-stack.ps1',
  'Local dev stack workflow should provide a top-level startup script.'
);

const localStack = read('scripts/start-dev-local-stack.ps1');
const infraScript = read('scripts/start-dev-infra.ps1');
const frontendReadme = read('frontend/README.md');
const applicationYml = read('agent-service/src/main/resources/application.yml');

assertIncludes(
  infraScript,
  'param(',
  'Infrastructure startup script should expose parameters for stack composition.'
);
assertIncludes(
  infraScript,
  'mysql',
  'Infrastructure startup script should still support MySQL startup.'
);
assertIncludes(
  localStack,
  'start-dev-infra.ps1',
  'Local dev stack script should reuse the shared infrastructure startup script.'
);
assertIncludes(
  localStack,
  'start-idea-dev-frontend.ps1',
  'Local dev stack script should reuse the retained frontend startup script.'
);
assertIncludes(
  localStack,
  '-SkipDocker',
  'Local dev stack script should prevent duplicate Docker startup when delegating frontend startup.'
);
assertIncludes(
  localStack,
  'mvn -DskipTests install',
  'Local dev stack script should build shared Maven modules before launching services.'
);
assertIncludes(
  localStack,
  '[string[]]$Exclude',
  'Local dev stack script should support excluding selected services.'
);
assertIncludes(
  localStack,
  '.runtime-logs',
  'Local dev stack script should write service logs into .runtime-logs.'
);
assertIncludes(
  localStack,
  'Wait-PortReleased',
  'Local dev stack script should wait for service ports to be fully released before relaunching a JVM service.'
);
assertIncludes(
  localStack,
  '-PassThru',
  'Local dev stack script should capture the background service process so startup failures can be detected early.'
);
assertIncludes(
  localStack,
  'HasExited',
  'Local dev stack script should stop waiting for health checks when a launched service exits early.'
);
assertIncludes(
  localStack,
  'Get-PortConflictMessage',
  'Local dev stack script should share one formatter for port-conflict diagnostics across startup failure paths.'
);
assertIncludes(
  localStack,
  'Test-RepoOwnedProcess',
  'Local dev stack script should distinguish repo-owned listener processes from external ones before killing them.'
);
assertIncludes(
  localStack,
  'Resolve-ConflictingPort',
  'Local dev stack script should attempt repo-owned port cleanup and retry startup when a service exits because its port is still occupied.'
);
assertIncludes(
  localStack,
  'Test-LogContainsPortInUse',
  'Local dev stack script should inspect service logs for transient port-in-use failures when no current listener remains.'
);
assertIncludes(
  localStack,
  'Exit code: unknown',
  'Local dev stack script should report unknown exit codes explicitly instead of leaving the message blank.'
);
assertIncludes(
  localStack,
  'Get-ListeningProcessDetails',
  'Local dev stack script should resolve the process details behind a port conflict.'
);
assertIncludes(
  localStack,
  'CommandLine',
  'Local dev stack script should report the conflicting process command line when a service port stays occupied.'
);
assertIncludes(
  localStack,
  'registry-server',
  'Local dev stack script should include registry-server in the default local JVM service set.'
);
assertIncludes(
  localStack,
  'config-server',
  'Local dev stack script should include config-server in the default local JVM service set.'
);
assertIncludes(
  localStack,
  'gateway',
  'Local dev stack script should include gateway in the default local JVM service set.'
);
assertIncludes(
  localStack,
  'Resolve-AgentLlmStartupState',
  'Local dev stack script should derive agent LLM enablement from the current config instead of hardcoding it.'
);
assertIncludes(
  localStack,
  'Resolve-AgentRagStartupState',
  'Local dev stack script should derive agent RAG enablement explicitly for the local dev stack.'
);
assertIncludes(
  localStack,
  'Resolve-AgentQuestionBankStartupState',
  'Local dev stack script should derive agent question bank enablement explicitly for the local dev stack.'
);
assertIncludes(
  localStack,
  'Get-AgentLlmApiKeyEnvVarNames',
  'Local dev stack script should extract the configured Agent LLM API key env var names from config.'
);
assertIncludes(
  localStack,
  'AGENT_RAG_ENABLED',
  'Local dev stack script should inject AGENT_RAG_ENABLED when launching agent-service.'
);
assertIncludes(
  localStack,
  'AGENT_QUESTION_BANK_ENABLED',
  'Local dev stack script should inject AGENT_QUESTION_BANK_ENABLED when launching agent-service.'
);
assertIncludes(
  localStack,
  'config-repo\\agent-service.yml',
  'Local dev stack script should inspect the config-center agent-service definition used by the dev stack.'
);
assertNotIncludes(
  localStack,
  'major_assignment',
  'Local dev stack script should not depend on the transition monolith.'
);
assertIncludes(
  frontendReadme,
  'start-dev-local-stack.ps1',
  'Frontend README should mention the optional full local stack command.'
);
assertIncludes(
  frontendReadme,
  'agent.llm.api-key',
  'Frontend README should describe how the local stack auto-detects the Agent LLM API key env var from config.'
);
assertIncludes(
  frontendReadme,
  'AGENT_RAG_ENABLED=true',
  'Frontend README should document that local knowledge-base RAG is enabled in the dev stack by default.'
);
assertIncludes(
  applicationYml,
  'AGENT_QUESTION_BANK_ENABLED:true',
  'Agent service dev profile should enable the question bank by default for local development.'
);

console.log('Dev local stack contract OK');
