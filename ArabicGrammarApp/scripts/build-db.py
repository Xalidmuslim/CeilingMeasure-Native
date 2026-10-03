import json
import re
import sqlite3
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "build" / "book-source" / "content.json"
OUT = ROOT / "app" / "src" / "main" / "assets" / "book" / "book.db"

AR_RE = re.compile(r"[\u0600-\u06ff\u0750-\u077f\u08a0-\u08ff]")
CYR_RE = re.compile(r"[А-Яа-яЁё]")
ANSWER_RE = re.compile(r"^[\s_—–-]{8,}$")

def normalize_text(value):
    text = unicodedata.normalize("NFC", str(value or ""))
    text = text.replace("\u00a0", " ").replace("\u200e", "").replace("\u200f", "")
    text = text.replace("§", "")
    text = re.sub(r"[\t\r ]+", " ", text)
    text = re.sub(r" *\n *", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)

    # Repair line breaks created by HTML tables.
    text = re.sub(r"\n(?=(?:муж\.?\s*рода|жен\.?\s*рода|число)\b)", " ", text, flags=re.I)
    text = re.sub(r"\bединств\.?\s*число\b", "единственное число", text, flags=re.I)
    text = re.sub(r"\bдвойств\.?\s*число\b", "двойственное число", text, flags=re.I)
    text = re.sub(r"\bмножеств\.?\s*число\b", "множественное число", text, flags=re.I)

    # Keep Cyrillic and Arabic runs separated so Android BiDi can lay them out predictably.
    text = re.sub(r"(?<=[\u0600-\u06ff\u0750-\u077f\u08a0-\u08ff])(?=[А-Яа-яЁё])", " ", text)
    text = re.sub(r"(?<=[А-Яа-яЁё])(?=[\u0600-\u06ff\u0750-\u077f\u08a0-\u08ff])", " ", text)
    text = re.sub(r"(?<=\))(?=[А-Яа-яЁё\u0600-\u06ff])", " ", text)
    text = re.sub(r"(?<=:)(?=[^\s\n])", " ", text)
    text = re.sub(r"(\d+)\)\s*", r"\1) ", text)
    text = re.sub(r"(^|\n)\.(?=[А-Яа-яЁё])", r"\1", text)
    text = re.sub(r" {2,}", " ", text)

    return text.strip()

def normalized_type(raw_type, text):
    if raw_type in ("section-heading", "exercise-heading"):
        return raw_type
    if ANSWER_RE.fullmatch(text):
        return "answer-line"

    has_ar = bool(AR_RE.search(text))
    has_cyr = bool(CYR_RE.search(text))

    # Any block that contains both writing systems must never be forced into RTL.
    if has_ar and has_cyr:
        base = "mixed"
    elif has_ar:
        base = "arabic"
    else:
        base = "text"

    lines = [line.strip() for line in text.splitlines() if line.strip()]
    if len(lines) >= 3 and base in ("text", "mixed", "arabic"):
        return "table"
    return base

if not SRC.exists():
    raise SystemExit(f"Missing source JSON: {SRC}")

data = json.loads(SRC.read_text(encoding="utf-8"))
OUT.parent.mkdir(parents=True, exist_ok=True)
if OUT.exists():
    OUT.unlink()

db = sqlite3.connect(OUT)
db.executescript("""
PRAGMA journal_mode=OFF;
PRAGMA synchronous=OFF;
CREATE TABLE meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
CREATE TABLE sections (
    id TEXT PRIMARY KEY,
    chapter TEXT NOT NULL,
    number INTEGER NOT NULL,
    title TEXT NOT NULL,
    start_index INTEGER,
    end_index INTEGER,
    found INTEGER NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    exercise_count INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE blocks (
    block_index INTEGER PRIMARY KEY,
    section_id TEXT,
    source_id INTEGER,
    tag TEXT,
    type TEXT NOT NULL,
    text TEXT NOT NULL
);
CREATE INDEX idx_sections_chapter_number ON sections(chapter, number);
CREATE INDEX idx_blocks_section ON blocks(section_id, block_index);
CREATE INDEX idx_blocks_type ON blocks(type);
""")

book = data.get("book", {})
stats = data.get("stats", {})
sources = data.get("sources", {})
meta = {
    "title": book.get("title", ""),
    "author": book.get("author", ""),
    "edition": book.get("edition", ""),
    "generatedAt": data.get("generatedAt", ""),
    "sourcePages": str(stats.get("sourcePages", 0)),
    "blocks": str(stats.get("blocks", 0)),
    "arabicChars": str(stats.get("arabicChars", 0)),
    "coreSectionsFound": str(stats.get("coreSectionsFound", 0)),
    "badEncodingBlocks": str(stats.get("badEncodingBlocks", 0)),
    "primarySource": sources.get("primary", ""),
    "verificationSource": sources.get("verification", ""),
}
db.executemany("INSERT INTO meta(key,value) VALUES (?,?)", meta.items())

sections = data.get("sections", [])
for s in sections:
    db.execute(
        """INSERT INTO sections
        (id,chapter,number,title,start_index,end_index,found,score,exercise_count)
        VALUES (?,?,?,?,?,?,?,?,?)""",
        (
            s["id"], s["chapter"], int(s.get("number", 0)), normalize_text(s.get("title", "")),
            s.get("startIndex"), s.get("endIndex"), 1 if s.get("found") else 0,
            int(s.get("score", 0)), int(s.get("exerciseCount", 0))
        )
    )

by_range = []
for s in sections:
    if s.get("startIndex") is not None and s.get("endIndex") is not None:
        by_range.append((int(s["startIndex"]), int(s["endIndex"]), s["id"]))

def section_for(index):
    for start, end, section_id in by_range:
        if start <= index < end:
            return section_id
    return None

rows = []
qc = {
    "removedSectionSigns": 0,
    "arabicTypedWithCyrillic": 0,
    "standaloneGarbage": 0,
    "answerLines": 0,
    "tables": 0,
}

for i, b in enumerate(data.get("blocks", [])):
    raw = str(b.get("text", ""))
    qc["removedSectionSigns"] += raw.count("§")
    text = normalize_text(raw)

    if ANSWER_RE.fullmatch(text):
        block_type = "answer-line"
        text = ""
        qc["answerLines"] += 1
    else:
        block_type = normalized_type(b.get("type", "text"), text)

    if block_type == "table":
        qc["tables"] += 1

    if block_type == "arabic" and CYR_RE.search(text):
        qc["arabicTypedWithCyrillic"] += 1

    if text and not (AR_RE.search(text) or CYR_RE.search(text) or re.search(r"[A-Za-z0-9]", text)):
        if block_type not in ("answer-line",):
            qc["standaloneGarbage"] += 1
            continue

    rows.append((
        i,
        b.get("sectionId") or section_for(i),
        b.get("sourceId"),
        b.get("tag"),
        block_type,
        text,
    ))

db.executemany(
    "INSERT INTO blocks(block_index,section_id,source_id,tag,type,text) VALUES (?,?,?,?,?,?)",
    rows
)
db.commit()

check = {
    "sections": db.execute("SELECT COUNT(*) FROM sections").fetchone()[0],
    "blocks": db.execute("SELECT COUNT(*) FROM blocks").fetchone()[0],
    "arabicBlocks": db.execute("SELECT COUNT(*) FROM blocks WHERE type='arabic'").fetchone()[0],
    "mixedBlocks": db.execute("SELECT COUNT(*) FROM blocks WHERE type='mixed'").fetchone()[0],
    "exercises": db.execute("SELECT COUNT(*) FROM blocks WHERE type='exercise-heading'").fetchone()[0],
    "tables": db.execute("SELECT COUNT(*) FROM blocks WHERE type='table'").fetchone()[0],
}
db.close()

if check["sections"] < 81:
    raise SystemExit(f"Too few sections in DB: {check}")
if check["blocks"] < 1000:
    raise SystemExit(f"Too few blocks in DB: {check}")
if OUT.stat().st_size < 300_000:
    raise SystemExit(f"DB unexpectedly small: {OUT.stat().st_size}")
if qc["arabicTypedWithCyrillic"] != 0:
    raise SystemExit(f"Mixed Cyrillic/Arabic block wrongly typed Arabic: {qc}")
if qc["standaloneGarbage"] > 8:
    raise SystemExit(f"Too much punctuation-only garbage: {qc}")

print(json.dumps({
    "db": str(OUT),
    "size": OUT.stat().st_size,
    **check,
    **qc,
}, ensure_ascii=False))
