"""Bundles the Inter font (SIL OFL 1.1) into app/src/main/assets/web/fonts.

Downloads @fontsource/inter from npm and keeps woff2 files for the latin,
latin-ext, cyrillic and cyrillic-ext subsets, weights 300-600.
Run from the project root: python3 tools/bundle_fonts.py
"""
import glob, os, re, shutil, subprocess, tarfile, tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "web", "fonts")
KEEP = {"latin", "latin-ext", "cyrillic", "cyrillic-ext"}

tmp = tempfile.mkdtemp()
subprocess.run(["npm", "pack", "@fontsource/inter@5", "--silent"], cwd=tmp, check=True)
with tarfile.open(glob.glob(os.path.join(tmp, "*.tgz"))[0]) as t:
    t.extractall(tmp)
src = os.path.join(tmp, "package")

os.makedirs(os.path.join(OUT, "files"), exist_ok=True)
css = ["/* Inter by Rasmus Andersson, SIL Open Font License 1.1 (see OFL.txt). */"]
for w in (300, 400, 500, 600):
    text = open(os.path.join(src, f"{w}.css")).read()
    for m in re.finditer(r"/\* inter-([a-z-]+)-%d-normal \*/\s*@font-face \{(.*?)\}" % w, text, re.S):
        sub, body = m.group(1), m.group(2)
        if sub not in KEEP:
            continue
        name = f"inter-{sub}-{w}-normal.woff2"
        shutil.copy(os.path.join(src, "files", name), os.path.join(OUT, "files", name))
        rng = re.search(r"unicode-range:([^;]+);", body).group(1).strip()
        css.append(
            "@font-face{font-family:'Inter';font-style:normal;font-display:swap;"
            f"font-weight:{w};src:url(files/{name}) format('woff2');unicode-range:{rng}}}"
        )
open(os.path.join(OUT, "inter.css"), "w").write("\n".join(css) + "\n")
shutil.copy(os.path.join(src, "LICENSE"), os.path.join(OUT, "OFL.txt"))
print(f"Bundled {len(css) - 1} font files into {OUT}")
