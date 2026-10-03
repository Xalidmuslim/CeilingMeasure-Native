import json
import os
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "build" / "book-source" / "content.json"
OUT = ROOT / "app" / "src" / "main" / "assets" / "book" / "book.db"

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
            s["id"], s["chapter"], int(s.get("number", 0)), s.get("title", ""),
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

blocks = data.get("blocks", [])
rows = []
for i, b in enumerate(blocks):
    rows.append((
        i,
        b.get("sectionId") or section_for(i),
        b.get("sourceId"),
        b.get("tag"),
        b.get("type", "text"),
        b.get("text", ""),
    ))

db.executemany(
    "INSERT INTO blocks(block_index,section_id,source_id,tag,type,text) VALUES (?,?,?,?,?,?)",
    rows
)
db.commit()

check = {
    "sections": db.execute("SELECT COUNT(*) FROM sections").fetchone()[0],
    "blocks": db.execute("SELECT COUNT(*) FROM blocks").fetchone()[0],
    "arabicBlocks": db.execute("SELECT COUNT(*) FROM blocks WHERE type IN ('arabic','mixed')").fetchone()[0],
    "exercises": db.execute("SELECT COUNT(*) FROM blocks WHERE type='exercise-heading'").fetchone()[0],
}
db.close()

if check["sections"] < 81:
    raise SystemExit(f"Too few sections in DB: {check}")
if check["blocks"] < 1000:
    raise SystemExit(f"Too few blocks in DB: {check}")
if OUT.stat().st_size < 300_000:
    raise SystemExit(f"DB unexpectedly small: {OUT.stat().st_size}")

print(json.dumps({"db": str(OUT), "size": OUT.stat().st_size, **check}, ensure_ascii=False))
