const fs = require('fs');
const path = require('path');

const repoRoot = path.resolve(__dirname, '..');
const nginxPath = path.join(repoRoot, 'deploy', 'nginx', 'nginx.conf');
const content = fs.readFileSync(nginxPath, 'utf8');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const streamLocationMatch = content.match(/location\s+=\s+\/api\/agent\/chat\/stream\s*\{([\s\S]*?)\n\s*\}/);

assert(streamLocationMatch, 'nginx must define an exact /api/agent/chat/stream location before the generic /api/ location.');

const streamLocation = streamLocationMatch[1];
const streamLocationIndex = content.indexOf(streamLocationMatch[0]);
const genericApiIndex = content.indexOf('location /api/');

assert(genericApiIndex > streamLocationIndex, 'agent SSE location must appear before generic /api/ proxy location.');
assert(streamLocation.includes('proxy_buffering off;'), 'agent SSE location must disable proxy_buffering.');
assert(streamLocation.includes('proxy_cache off;'), 'agent SSE location must disable proxy_cache.');
assert(streamLocation.includes('gzip off;'), 'agent SSE location must disable gzip.');
assert(streamLocation.includes('add_header X-Accel-Buffering no always;'), 'agent SSE location must emit X-Accel-Buffering: no.');
assert(streamLocation.includes('proxy_read_timeout 300s;'), 'agent SSE location must allow long reads.');
assert(streamLocation.includes('proxy_send_timeout 300s;'), 'agent SSE location must allow long sends.');

console.log('nginx agent SSE contract passed.');
