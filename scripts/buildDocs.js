const fs = require("node:fs");
const path = require("node:path");
const JSON5 = require("json5");

const { MCVER } = require("../const");

const variables = {
  mcVer: () => MCVER,
  blocksAddedTable: () => generateBlocksAddedTable(),
};

const rootDir = path.resolve(__dirname, "..");
const doctemplatesDir = path.join(rootDir, "doctemplates");
const docsDir = path.join(rootDir, "docs");
const vanillaPath = path.join(rootDir, "vanilla.json");
const archetypesPath = path.join(rootDir, "archetypes.json5");
const KEY_PREFIX = "minecraft:";

function readJson(filePath) {
  const raw = fs.readFileSync(filePath, "utf8");
  return JSON5.parse(raw);
}

function normalizeKey(key) {
  if (key.startsWith(KEY_PREFIX)) {
    return key.slice(KEY_PREFIX.length);
  }
  return key;
}

function generateBlocksAddedTable() {
  if (!fs.existsSync(vanillaPath) || !fs.existsSync(archetypesPath)) {
    return "| Block Name |\n| --- |\n| *(Doc generation error: Data files missing)* |";
  }

  const vanillaRaw = readJson(vanillaPath);
  const archetypesRaw = readJson(archetypesPath);

  const addedBlocks = [];

  for (const [key, vanillaValue] of Object.entries(vanillaRaw)) {
    if (vanillaValue === "") {
      const normalizedKey = normalizeKey(key);
      const archetypeValue = archetypesRaw[normalizedKey];
      
      if (typeof archetypeValue === "string" && archetypeValue !== "") {
        addedBlocks.push(normalizedKey);
      }
    }
  }

  if (addedBlocks.length === 0) {
    return "| Block Name |\n| --- |\n| *(Doc generation error: No added blocks)* |";
  }

  addedBlocks.sort((a, b) => a.localeCompare(b));

  let table = "| Blocks (" + addedBlocks.length + ") |\n| --- |\n";
  for (const block of addedBlocks) {
    table += `| ${block} |\n`;
  }

  return table.trim();
}

function processDirectory(srcDir, destDir) {
  const entries = fs.readdirSync(srcDir, { withFileTypes: true });

  for (const entry of entries) {
    const srcPath = path.join(srcDir, entry.name);
    const destPath = path.join(destDir, entry.name);

    if (entry.isDirectory()) {
      fs.mkdirSync(destPath, { recursive: true });
      processDirectory(srcPath, destPath);
    } else if (entry.isFile()) {
      const ext = path.extname(entry.name).toLowerCase();
      const textExtensions = [".md", ".txt"];

      if (textExtensions.includes(ext)) {
        let content = fs.readFileSync(srcPath, "utf8");
        
        content = content.replace(/%([a-zA-Z0-9_]+)%/g, (match, varName) => {
          if (Object.prototype.hasOwnProperty.call(variables, varName)) {
            return variables[varName]();
          }
          return match;
        });
        
        fs.writeFileSync(destPath, content, "utf8");
      } else {
        fs.copyFileSync(srcPath, destPath);
      }
    }
  }
}

function main() {
  if (!fs.existsSync(doctemplatesDir)) {
    console.warn("doctemplates directory was not found. Skipping doc generation.");
    return;
  }

  if (fs.existsSync(docsDir)) {
    fs.rmSync(docsDir, { recursive: true, force: true });
  }
  fs.mkdirSync(docsDir, { recursive: true });

  processDirectory(doctemplatesDir, docsDir);
  console.log("buildDocs complete. Documentation generated in ./docs");
}

try {
  main();
} catch (error) {
  console.error(`buildDocs failed: ${error.message}`);
  process.exitCode = 1;
}