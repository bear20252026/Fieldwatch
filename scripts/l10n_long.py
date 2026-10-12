"""Find long strings (>160 chars) missed by the first extraction pass."""
import re, os, collections

PAT = re.compile(r'"((?:[^"\\\n]|\\[^\n]){161,900})"')

def strip(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)

files = []
for root, dirs, fs in os.walk("app/src/main/java/app/fieldwatch/ui"):
    for f in fs:
        if f.endswith(".kt"):
            files.append(os.path.join(root, f))

seen = collections.Counter()
for fp in files:
    for m in PAT.finditer(strip(open(fp, encoding="utf-8").read())):
        seen[m.group(1)] += 1

out = open("scripts/long_strings.txt", "w", encoding="utf-8")
out.write("%d long strings\n" % len(seen))
for s, c in sorted(seen.items(), key=lambda kv: -len(kv[0])):
    out.write("=== x%d ===\n%s\n" % (c, s))
out.close()
print(len(seen), "long strings -> scripts/long_strings.txt")
