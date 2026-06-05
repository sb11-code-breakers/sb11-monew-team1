import json
import os
import re
import sys

import anthropic
import requests

API_BASE = "https://api.github.com"


def get_headers(token: str) -> dict:
    return {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }


def load_file(path: str) -> str:
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        return ""


def get_valid_new_lines(diff_text: str) -> dict[str, set[int]]:
    """Return {file_path: set_of_added_line_numbers} from the diff."""
    valid: dict[str, set[int]] = {}
    current_file = None
    new_line = 0

    for raw in diff_text.split("\n"):
        if raw.startswith("+++ b/"):
            current_file = raw[6:].strip()
            valid.setdefault(current_file, set())
            new_line = 0
        elif raw.startswith("@@"):
            m = re.search(r"\+(\d+)(?:,\d+)?", raw)
            if m:
                new_line = int(m.group(1)) - 1
        elif current_file is not None:
            if not raw.startswith("-"):
                new_line += 1
                if raw.startswith("+"):
                    valid[current_file].add(new_line)

    return valid


def call_claude(conventions: str, diff: str) -> dict:
    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])

    system_prompt = f"""당신은 모뉴(MoNew) 프로젝트의 코드 리뷰어입니다.
아래 컨벤션 문서를 기준으로 PR diff를 리뷰하세요.

{conventions}

## 응답 형식
순수 JSON만 출력하세요. 마크다운 코드 블록 없이 다음 형식만:

{{
  "summary": "변경 사항 요약 2-3줄",
  "overall": "컨벤션 위반 / 버그 / 개선점 / 잘된점 종합 평가",
  "file_comments": [
    {{"path": "파일경로", "line": 42, "body": "코멘트 내용"}}
  ]
}}

규칙:
- file_comments는 최대 10개
- 추가된 줄(+ 로 시작하는 줄)에만 코멘트 가능
- 위반 사항 없으면 file_comments는 빈 배열"""

    response = client.messages.create(
        model="claude-haiku-4-5-20251001",
        max_tokens=4096,
        system=[
            {
                "type": "text",
                "text": system_prompt,
                "cache_control": {"type": "ephemeral"},
            }
        ],
        messages=[
            {
                "role": "user",
                "content": f"다음 PR diff를 리뷰해주세요:\n\n```diff\n{diff}\n```",
            }
        ],
    )

    if response.stop_reason == "max_tokens":
        raise RuntimeError(
            "Claude response was truncated (max_tokens reached). "
            "Increase max_tokens or reduce the diff size."
        )

    text = response.content[0].text.strip()
    text = re.sub(r"^```(?:json)?\s*", "", text)
    text = re.sub(r"\s*```$", "", text)
    return json.loads(text)


def post_review(review_data: dict, valid_lines: dict[str, set[int]]) -> None:
    token = os.environ["BOT_GITHUB_TOKEN"]
    repo = os.environ["REPO_NAME"]
    pr_number = int(os.environ["PR_NUMBER"])
    head_sha = os.environ["PR_HEAD_SHA"]

    body = (
        "## 🤖 Claude 코드 리뷰\n\n"
        f"### 변경 요약\n{review_data['summary']}\n\n"
        f"### 종합 평가\n{review_data['overall']}\n\n"
        "---\n*이 리뷰는 자동 생성되었습니다. 최종 판단은 리뷰어가 합니다.*"
    )

    comments = [
        {
            "path": fc["path"],
            "line": fc["line"],
            "side": "RIGHT",
            "body": fc["body"],
        }
        for fc in review_data.get("file_comments", [])[:10]
        if fc.get("path") in valid_lines and fc.get("line") in valid_lines[fc["path"]]
    ]

    payload = {
        "commit_id": head_sha,
        "body": body,
        "event": "COMMENT",
        "comments": comments,
    }

    url = f"{API_BASE}/repos/{repo}/pulls/{pr_number}/reviews"
    resp = requests.post(url, headers=get_headers(token), json=payload, timeout=30)
    resp.raise_for_status()
    print(f"Review posted: {resp.json().get('html_url', 'ok')}")


def main() -> None:
    diff = load_file("pr_diff.txt")
    if not diff.strip():
        print("No diff found, skipping review.")
        sys.exit(0)

    conventions = load_file("docs/conventions.md")
    valid_lines = get_valid_new_lines(diff)
    review_data = call_claude(conventions, diff)
    post_review(review_data, valid_lines)


if __name__ == "__main__":
    main()
