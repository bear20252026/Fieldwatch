"""Wrap user-facing string literals with l10n(...) and generate L10nDict.kt.

Safety model:
- Only literals that exactly match a dictionary key are touched.
- Keys found in dangerous contexts (comparisons, when-branches, map/json
  lookups, const vals, SCREAMING_CASE vals) are dropped globally.
- Lines with annotations (@SerialName etc.), block comments, line comments,
  package/import lines are never wrapped.
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
SRC = os.path.join(ROOT, "app", "src", "main", "java", "app", "fieldwatch")

def load_dicts():
    merged = {}
    for name in ("zh_dict_ui.json", "zh_dict_domain.json"):
        with open(os.path.join(HERE, name), encoding="utf-8") as f:
            merged.update(json.load(f))
    return merged

DICT = load_dicts()

# (file relpath with forward slashes, key prefix) pairs never to wrap —
# e.g. literals assigned to Kotlin `const val` across lines.
EXCLUDE = {
    ("ui/screen/LiveScreens.kt", "No live emitters match the current filter"),
}

DANGER_PATTERNS = [
    re.compile(r'[=!]=\s*"%s"\s*$' % re.escape(k)) for k in ()
]
# Compiled per-line checks given the key text.
def line_is_dangerous(line: str, key: str) -> bool:
    q = '"%s"' % key
    for pat in (
        r'[=!]=\s*"' + re.escape(key) + r'"',            # == "k" / != "k"
        r'"' + re.escape(key) + r'"\s*[=!]=',            # "k" == / "k" !=
        r'"' + re.escape(key) + r'"\s*->',               # when branch key
        r'"' + re.escape(key) + r'"\s+in\b',             # "k" in collection
        r'\bin\s+"%s"\b' % re.escape(key),               # x in "k"
        r'\.\w*\(\s*"%s"\s*[,)]' % re.escape(key),       # .contains("k"), .get..("k"), .put("k") ...
        r'\.\w+\(\s*"%s"\s*\)' % re.escape(key),
        r'\bequals\(\s*"%s"' % re.escape(key),
        r'\[\s*"%s"\s*\]' % re.escape(key),              # map["k"]
        r'\bIntent\(\s*"%s"' % re.escape(key),           # Intent("action")
        r'\bconst val\s+\w+\s*=\s*"%s"' % re.escape(key),
        r'\bval\s+[A-Z][A-Z0-9_]{2,}\s*=\s*"%s"' % re.escape(key),  # SCREAMING_CASE val
        r'\barrayOf\(\s*"%s"' % re.escape(key),
        r'\blistOf\(\s*"%s"' % re.escape(key),
        r'\bsetOf\(\s*"%s"' % re.escape(key),
        r'\bmapOf\(\s*"%s"' % re.escape(key),
    ):
        if re.search(pat, line):
            return True
    return False

# 1) danger scan across every kotlin file (any package dir)
kt_files = []
for root, dirs, files in os.walk(SRC):
    for f in files:
        if f.endswith(".kt"):
            kt_files.append(os.path.join(root, f))

dropped = {}
for path in kt_files:
    with open(path, encoding="utf-8") as fh:
        for i, line in enumerate(fh):
            for key in list(DICT):
                if key in line and line_is_dangerous(line, key):
                    dropped.setdefault(key, []).append(
                        os.path.relpath(path, ROOT) + ":" + str(i + 1)
                    )
for key in dropped:
    print("DROP (danger context):", key, "->", dropped[key][0])
    DICT.pop(key, None)

# 2) wrap literals in target files
IMPORT_LINE = "import app.fieldwatch.l10n.l10n"
ANNOTATION_RE = re.compile(r"@\w+")
stats = {}
for path in kt_files:
    rel = os.path.relpath(path, SRC)
    if rel.startswith("l10n" + os.sep):
        continue
    with open(path, encoding="utf-8") as fh:
        lines = fh.read().split("\n")
    has_import = any(IMPORT_LINE in ln for ln in lines)
    # candidate keys: quoted literal present in an UNwrapped form on some line
    text = "\n".join(lines)
    rel_norm = rel.replace(os.sep, "/")
    keys_here = []
    for k in DICT:
        if any(rel_norm == f and k.startswith(pfx) for f, pfx in EXCLUDE):
            continue
        q = '"%s"' % k
        wrapped = 'l10n(%s)' % q
        if q in text and (wrapped not in text or q not in wrapped):
            # check at least one occurrence is not already l10n("k")
            if re.search(r'(?<!l10n\()' % () + re.escape(q), text):
                keys_here.append(k)
    if not keys_here:
        continue
    changed = 0
    in_block = False
    out = []
    for ln in lines:
        stripped = ln.strip()
        if in_block:
            out.append(ln)
            if "*/" in ln:
                in_block = False
            continue
        if stripped.startswith("/*"):
            in_block = "*/" not in ln
            out.append(ln)
            continue
        skip_line = (
            stripped.startswith("package ")
            or stripped.startswith("import ")
            or stripped.startswith("//")
            or "@SerialName" in ln
            or re.search(r'@\w+\("[^"]*"\)', ln)
        )
        if not skip_line and '"' in ln:
            code_part = ln.split("//")[0]
            for k in keys_here:
                q = '"%s"' % k
                pat = re.compile(r'(?<!l10n\()' + re.escape(q))
                if pat.search(code_part):
                    ln = pat.sub('l10n(%s)' % q, ln)
                    changed += 1
        out.append(ln)
    if changed:
        if not has_import:
            # insert import after the last existing import (or after package line)
            last_import = -1
            for i, ln in enumerate(out):
                if ln.startswith("import "):
                    last_import = i
            if last_import >= 0:
                out.insert(last_import + 1, IMPORT_LINE)
            else:
                for i, ln in enumerate(out):
                    if ln.startswith("package "):
                        out.insert(i + 1, "")
                        out.insert(i + 2, IMPORT_LINE)
                        break
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write("\n".join(out))
        stats[rel] = changed

print("\nWrapped %d files:" % len(stats))
for rel, n in sorted(stats.items(), key=lambda kv: -kv[1]):
    print("  %4d  %s" % (n, rel))

# 3) generate L10nDict.kt
def kesc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")

def emit_map(name, entries, fh):
    fh.write("internal val %s: Map<String, String> = mapOf(\n" % name)
    for k in sorted(entries):
        fh.write('    "%s" to "%s",\n' % (kesc(k), kesc(entries[k])))
    fh.write(")\n\n")

with open(os.path.join(SRC, "l10n", "L10nDict.kt"), "w", encoding="utf-8", newline="\n") as fh:
    fh.write("package app.fieldwatch.l10n\n\n")
    fh.write("// Generated by scripts/l10n_wrap.py from scripts/zh_dict_*.json. Do not hand-edit;\n")
    fh.write("// edit the JSON files and re-run the script.\n\n")
    with open(os.path.join(HERE, "zh_dict_ui.json"), encoding="utf-8") as f:
        ui = json.load(f)
    with open(os.path.join(HERE, "zh_dict_domain.json"), encoding="utf-8") as f:
        dom = json.load(f)
    for k in list(dropped):
        ui.pop(k, None)
        dom.pop(k, None)
    emit_map("ZH_UI", ui, fh)
    emit_map("ZH_DOMAIN", dom, fh)
    fh.write("val ZH: Map<String, String> = ZH_UI + ZH_DOMAIN\n")

print("\nDict entries kept: %d (ui) + %d (domain), dropped %d" % (
    len(ui), len(dom), len(dropped)))
