#!/usr/bin/env python3
"""Create a reviewable, allow-listed source tree; never uploads or invokes git."""
from pathlib import Path
import re
import shutil
import zipfile
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/github-ready'
FILES = ['README.md', 'README.ko.md', 'AGENTS.md', 'CLAUDE.md', 'AndroidManifest.xml', '.gitignore', 'LICENSE',
         'THIRD_PARTY_NOTICES.md', 'PRIVACY.md', 'PRIVACY.en.md', 'CONTRIBUTING.md',
         'docs/INSTALL.md', 'docs/INSTALL.en.md', 'docs/AGENT_INSTALL.md', 'docs/AGENT_INSTALL.en.md', 'docs/media/foldpatch-cover.png',
         'docs/media/settings-fold3.png', 'docs/media/calibration-fold3.png', 'docs/media/keyboard-fold3.png',
         'docs/REAL_USE.md', 'docs/REAL_USE.en.md',
         'docs/media/chrome-before-fold3.jpg', 'docs/media/chrome-after-fold3.jpg',
         'docs/media/split-before-fold3.jpg', 'docs/media/split-after-fold3.jpg',
         'docs/media/range-before-fold3.jpg', 'docs/media/range-after-fold3.jpg',
         'docs/BUILD.md', 'docs/BUILD.en.md', 'docs/ARCHITECTURE.md', 'docs/ARCHITECTURE.en.md',
         'docs/LOCALIZATION.md', 'docs/COMPATIBILITY.md', 'docs/RELEASE_REVIEW.md', 'docs/RELEASE_NOTES.md', 'docs/touch-side-validation.md',
         'docs/BACKLOG.md', 'docs/BRAND.md', 'docs/SHIZUKU_POLICY.md', 'docs/research/2026-09-20-input-and-recovery.md',
         'docs/COMPATIBILITY.en.md', 'docs/RELEASE_REVIEW.en.md', 'docs/RELEASE_NOTES.en.md',
         'docs/BACKLOG.en.md', 'docs/WHY.md', 'docs/WHY.en.md',
         'docs/research/2026-09-20-development-history.md',
         'docs/research/2026-09-20-runtime-continuity.md',
         'docs/research/2026-09-20-first-run-validation.md',
         'docs/research/2026-09-20-android17-feasibility.md',
         'docs/research/2026-09-20-fold-system-ui-restore.md',
         'docs/research/2026-09-20-home-stability.md']
PATTERNS = ['src/**/*.java', 'res/**/*.xml', 'res/**/*.png', 'libs/*.jar', 'libs/README.md',
            'libs/SHA256SUMS', 'licenses/*.txt', 'tests/**/*.java',
            'scripts/*.sh', 'scripts/*.py', 'experiments/native-screen/*.java',
            '.github/ISSUE_TEMPLATE/*.md', '.github/workflows/*.yml']
paths = {ROOT / f for f in FILES}
for pattern in PATTERNS:
    paths.update(ROOT.glob(pattern))
for path in paths:
    if not path.is_file() or path.is_symlink():
        raise SystemExit(f'Missing or symlinked public input: {path.relative_to(ROOT)}')
    if path.suffix in ('.md', '.java', '.xml', '.py', '.sh', '.svg', '.yml', '.yaml'):
        content = path.read_text(encoding='utf-8')
        if re.search(r'/home/[a-zA-Z0-9_-]+/|172\.(?:1[6-9]|2[0-9]|3[01])\.[0-9]+\.[0-9]+|BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY', content):
            raise SystemExit(f'Private development data in {path.relative_to(ROOT)}')
# Fixed output only; refuse symlinks before replacing the previous generated tree.
if OUT.is_symlink():
    raise SystemExit('Refusing symlinked staging directory')
if OUT.exists():
    shutil.rmtree(OUT)
OUT.mkdir(parents=True)
for path in sorted(paths):
    relative = path.relative_to(ROOT)
    target = OUT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, target)
# Check Markdown links and HTML screenshot/link targets against the public tree.
for path in OUT.rglob('*.md'):
    content = path.read_text(encoding='utf-8')
    links = re.findall(r'\]\(([^)]+)\)', content)
    links += re.findall(r'''(?:src|href)=["']([^"']+)["']''', content)
    for link in links:
        if '://' in link or link.startswith('#'):
            continue
        target = (path.parent / link.split('#')[0]).resolve()
        if not target.exists() or not target.is_relative_to(OUT.resolve()):
            raise SystemExit(f'Broken public link in {path.relative_to(OUT)}: {link}')
archive = ROOT / 'build/foldpatch-source.zip'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for path in sorted(OUT.rglob('*')):
        if path.is_file():
            info = zipfile.ZipInfo('FoldPatch/' + path.relative_to(OUT).as_posix(), (2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            z.writestr(info, path.read_bytes())
print(f'Prepared {len(paths)} public files in {OUT}')
print(f'Source archive: {archive}')
