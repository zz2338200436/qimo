const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assertContains(file, expected) {
  const content = read(file);
  if (!content.includes(expected)) {
    throw new Error(`${file} does not contain expected text: ${expected}`);
  }
}

assertContains('agent-service/src/main/resources/application.yml', 'rag:');
assertContains('agent-service/src/main/resources/application.yml', 'AGENT_RAG_ENABLED');
assertContains('agent-service/src/main/resources/application.yml', 'OLLAMA_EMBEDDING_MODEL');
assertContains('agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java', 'QUERY_RAG_KNOWLEDGE');
assertContains('agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java', 'ragKnowledgeService.answer');
assertContains('agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java', '只能依据下方知识库片段回答');
assertContains('docs/rag-knowledge-base.md', 'RAG 外挂知识库');

console.log('agent rag contract verified');
