import json
import sys
import urllib.request
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
EMOJI_FILE = PROJECT / "shared/src/commonMain/composeResources/files/data-by-group.json"
OUTPUT_FOLDER = PROJECT / "shared/src/commonMain/composeResources/files/emoji-names"
CLDR_URL = "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json"
CLDR_SOURCES = {
    "base": f"{CLDR_URL}/cldr-annotations-full/annotations/{{locale}}/annotations.json",
    "derived": f"{CLDR_URL}/cldr-annotations-derived-full/annotationsDerived/{{locale}}/annotations.json",
}
LANGUAGES = ["es", "pt", "pt-PT", "fr", "it", "de", "nl", "pl", "uk", "ru", "tr", "id", "vi", "hi", "ja", "ko", "zh"]
LAYERED_LANGUAGES = {"pt-PT": ["pt", "pt-PT"]}
VARIATION_SELECTOR = "️"
INHERITED_MARKER = "↑↑↑"
SEPARATOR = " | "


def download_folder():
    if len(sys.argv) < 2:
        sys.exit("Usage: python3 -I scripts/build-emoji-names.py <download folder>")
    folder = Path(sys.argv[1])
    folder.mkdir(parents=True, exist_ok=True)
    return folder


def cldr_annotations(folder, source, locale):
    file = folder / source / f"{locale}.json"
    if not file.exists():
        file.parent.mkdir(parents=True, exist_ok=True)
        print(f"Downloading {source} {locale}")
        with urllib.request.urlopen(CLDR_SOURCES[source].format(locale=locale), timeout=120) as reply:
            file.write_bytes(reply.read())
    data = json.loads(file.read_text(encoding="utf-8"))
    root = data.get("annotations") or data.get("annotationsDerived")
    return root["annotations"]


def words_of(entry):
    words = []
    for word in entry.get("tts", []) + entry.get("default", []):
        word = word.strip()
        if word and word != INHERITED_MARKER and word not in words:
            words.append(word)
    return words


def merged_annotations(folder, language):
    merged = {}
    for locale in LAYERED_LANGUAGES.get(language, [language]):
        for source in CLDR_SOURCES:
            for emoji, entry in cldr_annotations(folder, source, locale).items():
                words = words_of(entry)
                if words:
                    merged[emoji.replace(VARIATION_SELECTOR, "")] = words
    return merged


def app_emojis():
    groups = json.loads(EMOJI_FILE.read_text(encoding="utf-8"))
    return [item["emoji"] for group in groups for item in group["emojis"]]


def main():
    folder = download_folder()
    emojis = app_emojis()
    OUTPUT_FOLDER.mkdir(parents=True, exist_ok=True)
    total_bytes = 0
    for language in LANGUAGES:
        annotations = merged_annotations(folder, language)
        names = {}
        missing = []
        for emoji in emojis:
            words = annotations.get(emoji.replace(VARIATION_SELECTOR, ""))
            if words:
                names[emoji] = SEPARATOR.join(words)
            else:
                missing.append(emoji)
        output = OUTPUT_FOLDER / f"{language}.json"
        output.write_text(json.dumps(names, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        size = output.stat().st_size
        total_bytes += size
        print(f"{language}: {len(names)}/{len(emojis)} emojis, {size // 1024} KB, missing {''.join(missing[:20])}")
    print(f"Total {total_bytes / 1024 / 1024:.2f} MB")


main()
