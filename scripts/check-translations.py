import filecmp
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

SRC = Path(__file__).resolve().parents[1] / "shared/src"
COMPOSE = SRC / "commonMain/composeResources"
ANDROID = SRC / "androidMain/res"
PLACEHOLDER = re.compile(r"%\d+\$[sd]")
PARTIAL_LANGUAGES = {"pt-rPT"}
FOUR_FORM_LANGUAGES = {"pl", "uk", "ru"}
OTHER_ONLY_LANGUAGES = {"ja", "ko", "zh-rCN", "zh", "vi", "id", "in"}
ONE_COVERS_OTHER_NUMBERS = {"uk", "ru", "hi"}


def entries(path):
    result = {}
    for element in ET.parse(path).getroot():
        name = element.get("name")
        if element.tag == "string":
            result[name] = ("string", {"": element.text or ""})
        elif element.tag == "plurals":
            result[name] = ("plurals", {item.get("quantity"): item.text or "" for item in element})
    return result


def placeholders(text):
    return sorted(PLACEHOLDER.findall(text))


def check_file(english_path, translated_path, is_android, is_partial, problems, language):
    try:
        english = entries(english_path)
        translated = entries(translated_path)
    except ET.ParseError as error:
        problems.append(f"{translated_path}: XML error {error}")
        return
    raw = translated_path.read_text(encoding="utf-8")
    missing = set(english) - set(translated)
    extra = set(translated) - set(english)
    if missing and not is_partial:
        problems.append(f"{translated_path}: missing {sorted(missing)}")
    if extra:
        problems.append(f"{translated_path}: unknown keys {sorted(extra)}")
    for key, (kind, values) in translated.items():
        if key not in english:
            continue
        english_kind, english_values = english[key]
        if kind != english_kind:
            problems.append(f"{translated_path}: {key} is {kind}, English is {english_kind}")
            continue
        english_placeholders = placeholders(next(iter(english_values.values())) if kind == "string" else english_values.get("other", ""))
        if kind == "plurals":
            if language in FOUR_FORM_LANGUAGES:
                needed = {"one", "few", "many", "other"}
            elif language in OTHER_ONLY_LANGUAGES:
                needed = {"other"}
            else:
                needed = {"one", "other"}
            if not needed <= set(values):
                problems.append(f"{translated_path}: {key} needs {sorted(needed)}, has {sorted(values)}")
        for quantity, text in values.items():
            if placeholders(text) != english_placeholders and not (kind == "plurals" and quantity == "one" and language not in ONE_COVERS_OTHER_NUMBERS and set(placeholders(text)) <= set(english_placeholders)):
                problems.append(f"{translated_path}: {key}[{quantity}] placeholders {placeholders(text)} vs {english_placeholders}")
            english_text = english_values.get(quantity, english_values.get("other", ""))
            if english_text.endswith("...") and not text.endswith("..."):
                problems.append(f"{translated_path}: {key} must end with ...")
            if english_text.count("**") != text.count("**"):
                problems.append(f"{translated_path}: {key} ** markers differ")
            if is_android:
                if re.search(r"(?<!\\)'", text):
                    problems.append(f"{translated_path}: {key} has unescaped apostrophe")
                if re.search(r"(?<!%)%(?![\d%])", text.replace("%%", "")):
                    problems.append(f"{translated_path}: {key} has lone %")
            else:
                if "\\'" in text or '\\"' in text:
                    problems.append(f"{translated_path}: {key} has \\' or \\\" (Compose shows the backslash)")
    if "<!--" in raw:
        problems.append(f"{translated_path}: has comments")


def all_languages():
    return sorted(folder.name.removeprefix("values-") for folder in COMPOSE.glob("values-*") if folder.is_dir())


def check_indonesian_copies_match(problems):
    indonesian = COMPOSE / "values-id"
    old_code_copy = COMPOSE / "values-in"
    if not indonesian.exists() and not old_code_copy.exists():
        return
    comparison = filecmp.dircmp(indonesian, old_code_copy)
    for name in comparison.left_only:
        problems.append(f"{old_code_copy / name}: missing (values-in must be an exact copy of values-id)")
    for name in comparison.right_only:
        problems.append(f"{old_code_copy / name}: not in values-id (values-in must be an exact copy of values-id)")
    _, differing, unreadable = filecmp.cmpfiles(indonesian, old_code_copy, comparison.common_files, shallow=False)
    for name in differing + unreadable:
        problems.append(f"{old_code_copy / name}: differs from values-id (values-in must be an exact copy)")


def main():
    languages = sys.argv[1:] or all_languages()
    problems = []
    check_indonesian_copies_match(problems)
    for language in languages:
        is_partial = language in PARTIAL_LANGUAGES
        compose_folder = COMPOSE / f"values-{language}"
        english_files = sorted((COMPOSE / "values").glob("strings_*.xml"))
        for english_path in english_files:
            translated_path = compose_folder / english_path.name
            if translated_path.exists():
                check_file(english_path, translated_path, False, is_partial, problems, language)
            elif not is_partial:
                problems.append(f"{translated_path}: file missing")
        android_path = ANDROID / f"values-{language}/strings.xml"
        if android_path.exists():
            check_file(ANDROID / "values/strings.xml", android_path, True, is_partial, problems, language)
        elif not is_partial and language != "id":
            problems.append(f"{android_path}: file missing")
    for problem in problems:
        print(problem)
    print(f"{len(problems)} problems in {', '.join(languages)}")
    if problems:
        sys.exit(1)


main()
