const fs = require('node:fs');
const path = require('node:path');

const repoRoot = path.resolve(__dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8');
}

function assertContains(content, expected, label) {
  if (!content.includes(expected)) {
    throw new Error(`${label} is missing ${expected}`);
  }
}

function main() {
  const smokeStack = read('scripts/start-runtime-smoke-stack.ps1');
  const singleService = read('scripts/start-java-service.ps1');

  assertContains(smokeStack, '$DefaultServiceJvmArguments', 'runtime smoke stack');
  assertContains(smokeStack, "'-Xmx256m'", 'runtime smoke stack');
  assertContains(smokeStack, "'-XX:MaxMetaspaceSize=160m'", 'runtime smoke stack');
  assertContains(smokeStack, "'-XX:ReservedCodeCacheSize=64m'", 'runtime smoke stack');
  assertContains(smokeStack, "'-XX:ActiveProcessorCount=2'", 'runtime smoke stack');
  assertContains(smokeStack, '[string[]]$JvmArguments = $DefaultServiceJvmArguments', 'Start-JarService');
  assertContains(smokeStack, "$JvmArguments + @('-jar', $jarName)", 'runtime smoke stack launch command');

  assertContains(singleService, '[string[]]$JvmArguments = @()', 'single service launcher');
  assertContains(singleService, "$argumentList = $JvmArguments + @('-jar',", 'single service launcher');

  console.log('Runtime JVM limit launcher checks passed');
}

main();
