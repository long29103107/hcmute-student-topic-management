import { copyFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = resolve(projectRoot, 'node_modules', 'flowbite', 'dist', 'flowbite.min.js');
const destination = resolve(
  projectRoot,
  'src',
  'main',
  'resources',
  'static',
  'js',
  'vendor',
  'flowbite.min.js',
);

mkdirSync(dirname(destination), { recursive: true });
copyFileSync(source, destination);
console.log(`Copied Flowbite JavaScript to ${destination}`);
