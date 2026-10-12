"""Extract string literals: ui/ fully, domain/ per-file counts to cherry-pick."""
import re, os, collections

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "java", "app", "fieldwatch"))
PAT = re.compile(r'"([^"\\\n$]{1,160})"')

def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    return src

def extract(paths, outfile, per_file_stat=None):
    counts = collections.Counter()
    for rel in paths:
        src = strip_comments(open(os.path.join(ROOT, rel), encoding="utf-8").read())
        for m in PAT.finditer(src):
            s = m.group(1).strip()
            if s:
                counts[s] += 1
                if per_file_stat is not None:
                    per_file_stat[rel] += 1
    rows = sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))
    with open(outfile, "w", encoding="utf-8") as out:
        for s, c in rows:
            out.write(f"{c}\t{s}\n")
    return len(counts)

ui_files, domain_files = [], []
for root, dirs, fs in os.walk(ROOT):
    for f in fs:
        if f.endswith(".kt"):
            rel = os.path.relpath(os.path.join(root, f), ROOT)
            if os.sep + "ui" + os.sep in rel or rel.startswith("ui" + os.sep):
                ui_files.append(rel)
            elif rel.startswith("domain" + os.sep):
                domain_files.append(rel)

print("ui unique:", extract(ui_files, os.path.join(os.path.dirname(__file__), "ui_strings.tsv")))
stat = collections.Counter()
extract(domain_files, os.path.join(os.path.dirname(__file__), "domain_strings_all.tsv"), stat)
with open(os.path.join(os.path.dirname(__file__), "domain_files.tsv"), "w", encoding="utf-8") as out:
    for rel, c in stat.most_common():
        out.write(f"{c}\t{rel}\n")
