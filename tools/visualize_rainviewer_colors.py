#!/usr/bin/env python3
"""
Download RainViewer color CSV and generate an HTML visualizer.

Run:
  python3 tools/visualize_rainviewer_colors.py

The script writes: tools/rainviewer_colors.html
"""
import csv
import html
import os
import sys
from urllib.request import urlopen

URL = "https://www.rainviewer.com/files/rainviewer_api_colors_table.csv"
OUT = os.path.join(os.path.dirname(__file__), "rainviewer_colors.html")


def fetch_csv(url: str) -> str:
    with urlopen(url, timeout=20) as r:
        raw = r.read()
    return raw.decode("utf-8", errors="replace")


def parse_csv(text: str):
    # Use csv.reader over non-empty lines. Try to locate the header row that
    # starts with 'dBZ' or similar.
    lines = [l for l in text.splitlines() if l.strip()]
    reader = csv.reader(lines)
    rows = list(reader)
    header_idx = None
    for i, row in enumerate(rows[:5]):
        if row and row[0].lower().startswith("dbz"):
            header_idx = i
            break
    if header_idx is None:
        # fallback: first row
        header_idx = 0
    header = rows[header_idx]
    data = rows[header_idx + 1 :]
    scheme_names = [c.strip() for c in header[1:]]
    colors_by_scheme = {name: [] for name in scheme_names}
    for row in data:
        if not row:
            continue
        for j, name in enumerate(scheme_names):
            idx = 1 + j
            val = row[idx].strip() if idx < len(row) else ""
            # Some entries may be empty or malformed; normalize to empty string
            colors_by_scheme[name].append(val if val.startswith("#") else "")
    return scheme_names, colors_by_scheme


def build_html(scheme_names, colors_by_scheme):
    title = "RainViewer Color Schemes"
    rows = max(len(colors_by_scheme[n]) for n in scheme_names)
    parts = [
        "<!doctype html>",
        "<html><head><meta charset=\"utf-8\"><title>" + html.escape(title) + "</title>",
        "<style>body{font-family:system-ui,Arial;margin:12px;} .grid{display:flex;gap:8px;align-items:flex-start;}",
        ".col{width:160px;border:1px solid #ddd;padding:6px;border-radius:6px;background:#fff}",
        ".hdr{font-weight:600;margin-bottom:6px;font-size:13px}",
        ".swatches{display:block;border:1px solid #eee}",
        ".swatch{height:4px}",
        ".label{font-size:11px;color:#444;margin-top:6px}",
        "</style></head><body>",
        "<h2>RainViewer Color Schemes</h2>",
        "<p>Generated from: <a href=\"https://www.rainviewer.com/files/rainviewer_api_colors_table.csv\">rainviewer_api_colors_table.csv</a></p>",
        "<div class=\"grid\">",
    ]
    for name in scheme_names:
        colors = colors_by_scheme.get(name, [])
        parts.append("<div class=\"col\">")
        parts.append("<div class=\"hdr\">" + html.escape(name or "(unnamed)") + "</div>")
        parts.append("<div class=\"swatches\">")
        # Draw from highest DBZ (last rows) at top to low at bottom
        for c in reversed(colors):
            if c:
                parts.append(f"<div class=\"swatch\" title=\"{html.escape(c)}\" style=\"background:{html.escape(c)}\"></div>")
            else:
                parts.append("<div class=\"swatch\" style=\"background:transparent;border-bottom:1px dotted #eee\"></div>")
        parts.append("</div>")
        parts.append("<div class=\"label\">blocks: " + str(len(colors)) + "</div>")
        parts.append("</div>")
    parts.append("</div>")
    parts.append("</body></html>")
    return "\n".join(parts)


def main():
    print("Fetching CSV...")
    try:
        text = fetch_csv(URL)
    except Exception as e:
        print("Failed to download CSV:", e, file=sys.stderr)
        sys.exit(2)
    scheme_names, colors_by_scheme = parse_csv(text)
    html_out = build_html(scheme_names, colors_by_scheme)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(html_out)
    print("Wrote:", OUT)


if __name__ == "__main__":
    main()
