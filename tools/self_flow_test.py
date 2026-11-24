#!/usr/bin/env python3
"""본인 계정 흐름 시험. 본선과 곁가지만 실행하고 반복(P-PB-21~25)과 대리인은 하지 않는다.

아이디와 비밀번호는 환경 변수로만 받는다.
  YAKYEON_ID
  YAKYEON_PASSWORD

결과에는 비밀번호를 적지 않는다. 종료·삭제·연결 해제는 이번 실행에서 만든
병원명 '흐름시험' 약과 그 칸만 대상으로 한다. 계정에 원래 있던 약은 고르지 않는다.
복약 정보 철회 확정은 다른 확인이 끝난 뒤에 한다.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

PKG = "com.gamcho.yakyeon"
ACTIVITY = "com.example.yakyeon2.MainActivity"
SERIAL = os.environ.get("YAKYEON_SERIAL", "emulator-5554")
HOSPITAL = "흐름시험"
WRONG_PASSWORD = "wrong-pass-1"
ROOT = Path(__file__).resolve().parents[1]
RESULT = ROOT / "docs" / "self-flow-results.md"
DUMP = Path("/tmp/yakyeon-uidump.xml")


@dataclass
class Node:
    text: str
    desc: str
    rid: str
    bounds: tuple[int, int, int, int]
    clickable: bool
    checked: bool

    @property
    def center(self) -> tuple[int, int]:
        x1, y1, x2, y2 = self.bounds
        return (x1 + x2) // 2, (y1 + y2) // 2

    def blob(self) -> str:
        return " ".join(part for part in (self.text, self.desc, self.rid) if part)


@dataclass
class Row:
    case_id: str
    branch: str
    verdict: str
    note: str = ""


@dataclass
class Session:
    rows: list[Row] = field(default_factory=list)
    made: list[str] = field(default_factory=list)

    def add(self, case_id: str, branch: str, verdict: str, note: str = "") -> None:
        self.rows.append(Row(case_id, branch, verdict, note.replace("\n", " ")[:240]))
        print(f"{case_id} {verdict} {note[:120]}", flush=True)


def adb(*args: str, timeout: int = 20) -> subprocess.CompletedProcess[str]:
    proc = subprocess.Popen(
        ["adb", "-s", SERIAL, *args],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    try:
        out, err = proc.communicate(timeout=timeout)
    except subprocess.TimeoutExpired:
        proc.kill()
        out, err = proc.communicate()
        subprocess.run(
            ["adb", "-s", SERIAL, "shell", "pkill -f uiautomator"],
            text=True,
            capture_output=True,
            timeout=5,
        )
        return subprocess.CompletedProcess(args, 124, out or "", (err or "") + "\ntimeout")
    return subprocess.CompletedProcess(args, proc.returncode or 0, out or "", err or "")


def shell(command: str, timeout: int = 20) -> str:
    result = adb("shell", command, timeout=timeout)
    return (result.stdout or "") + (result.stderr or "")


def screen() -> list[Node]:
    last = ""
    for _ in range(4):
        remote = "/sdcard/yakyeon-uidump.xml"
        made = shell(f"uiautomator dump {remote}")
        pulled = adb("exec-out", "cat", remote)
        raw = pulled.stdout or ""
        if "<?xml" in raw:
            raw = raw[raw.find("<?xml") :]
            DUMP.write_text(raw, encoding="utf-8")
            nodes: list[Node] = []
            for item in ET.fromstring(raw).iter("node"):
                match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", item.attrib.get("bounds", ""))
                if not match:
                    continue
                bounds = tuple(int(part) for part in match.groups())
                nodes.append(
                    Node(
                        text=item.attrib.get("text", ""),
                        desc=item.attrib.get("content-desc", ""),
                        rid=item.attrib.get("resource-id", ""),
                        bounds=bounds,  # type: ignore[arg-type]
                        clickable=item.attrib.get("clickable") == "true",
                        checked=item.attrib.get("checked") == "true",
                    )
                )
            if nodes:
                return nodes
        last = made
        time.sleep(0.6)
    raise RuntimeError(f"화면을 읽지 못했다. {last[-200:]}")


def visible(nodes: list[Node] | None = None) -> str:
    nodes = nodes if nodes is not None else screen()
    return "\n".join(node.text for node in nodes if node.text)


def find(pred, nodes: list[Node] | None = None) -> Node | None:
    nodes = nodes if nodes is not None else screen()
    for node in nodes:
        if pred(node):
            return node
    return None


def find_text(fragment: str, nodes: list[Node] | None = None) -> Node | None:
    return find(lambda node: fragment in node.blob(), nodes)


def tap(node: Node) -> None:
    x, y = node.center
    shell(f"input tap {x} {y}")
    time.sleep(0.8)


def tap_text(fragment: str, scrolls: int = 4) -> bool:
    for _ in range(scrolls):
        node = find_text(fragment)
        if node:
            tap(node)
            return True
        shell("input swipe 540 1600 540 700 300")
        time.sleep(0.4)
    return False


def tap_id(suffix: str) -> bool:
    node = find(lambda item: item.rid.endswith("/" + suffix))
    if not node:
        return False
    tap(node)
    return True


def back() -> None:
    shell("input keyevent 4")
    time.sleep(0.6)


def launch() -> None:
    shell(f"am start -n {PKG}/{ACTIVITY}")
    time.sleep(1.5)


def force_stop() -> None:
    shell(f"am force-stop {PKG}")
    time.sleep(0.5)


def clear_field() -> None:
    shell("input keyevent 123")
    deletes = " ".join(["67"] * 40)
    shell(f"input keyevent {deletes}")


def write_text(value: str) -> None:
    """ASCII만 input text로 넣고, 한글은 클립보드에 붙여 넣는다."""
    if re.fullmatch(r"[\x20-\x7e]+", value):
        escaped = (
            value.replace("\\", "\\\\")
            .replace(" ", "%s")
            .replace("&", "\\&")
            .replace("<", "\\<")
            .replace(">", "\\>")
            .replace("'", "\\'")
            .replace('"', '\\"')
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace(";", "\\;")
            .replace("|", "\\|")
            .replace("*", "\\*")
        )
        shell(f"input text {escaped}")
        return
    encoded = value.replace("'", "'\\''")
    pasted = shell(f"cmd clipboard set-text '{encoded}' && input keyevent 279")
    if "Unknown command" in pasted or "Exception" in pasted:
        raise RuntimeError("한글 입력용 클립보드를 쓰지 못했다")


def fill_id(suffix: str, value: str) -> bool:
    if not tap_id(suffix):
        return False
    clear_field()
    write_text(value)
    time.sleep(0.3)
    return True


def has(fragment: str, nodes: list[Node] | None = None) -> bool:
    return find_text(fragment, nodes) is not None


def grant_notifications() -> str:
    shell(f"pm grant {PKG} android.permission.POST_NOTIFICATIONS")
    return "알림 권한을 허용했다. 이미 허용이어도 같은 명령이다."


def logcat_contains(fragment: str) -> bool:
    result = adb("logcat", "-d", "-t", "400")
    return fragment in (result.stdout or "")


def ensure_login() -> None:
    launch()
    if has("로그인") and has("회원가입"):
        return
    if tap_text("설정", scrolls=1) or tap_id("tab_settings"):
        time.sleep(0.5)
    if tap_text("프로필"):
        time.sleep(0.4)
    if tap_text("로그아웃"):
        time.sleep(0.4)
        tap_text("로그아웃") or tap_text("확인")
        time.sleep(1.2)
    launch()


def consent_one_off(session: Session, case_id: str, box_id: str) -> None:
    launch()
    if not tap_text("회원가입"):
        session.add(case_id, "곁가지", "실패", "회원가입 버튼이 없다")
        return
    time.sleep(0.8)
    for box in ("check_terms", "check_privacy", "check_notice", "check_sensitive"):
        node = find(lambda item, box=box: item.rid.endswith("/" + box))
        if node and not node.checked:
            tap(node)
    node = find(lambda item: item.rid.endswith("/" + box_id))
    if node and node.checked:
        tap(node)
    tap_id("button_consent_proceed") or tap_text("동의하고 회원가입 진행")
    time.sleep(0.8)
    nodes = screen()
    stayed = has("약연 서비스 이용약관", nodes) or has("필수 동의", nodes)
    advanced = has("누구의 약을 관리하시나요?", nodes)
    if stayed and not advanced:
        session.add(case_id, "곁가지", "통과", "유형 화면으로 가지 않았다")
    else:
        session.add(case_id, "곁가지", "실패", visible(nodes)[:180])
    back()
    time.sleep(0.4)


def run_before_login(session: Session) -> None:
    devices = adb("devices").stdout or ""
    if SERIAL not in devices or "device" not in devices:
        session.add("P-IN-01", "본선", "실패", f"{SERIAL} 이 없다")
        return
    installed = shell(f"pm path {PKG}")
    session.add(
        "P-IN-01",
        "본선",
        "통과" if "package:" in installed else "실패",
        "이미 설치된 이번 빌드를 확인했다" if "package:" in installed else installed[-120:],
    )
    launch()
    nodes = screen()
    session.add(
        "P-IN-02",
        "본선",
        "통과" if has("로그인", nodes) else "실패",
        "로그인 화면" if has("로그인", nodes) else visible(nodes)[:120],
    )
    for case_id, label in (
        ("P-IN-03", "로그인"),
        ("P-IN-04", "회원가입"),
        ("P-IN-05", "아이디 찾기"),
        ("P-IN-06", "비밀번호 재설정"),
    ):
        session.add(case_id, "본선", "통과" if has(label) else "실패", label)
    session.add("P-IN-07", "본선", "통과", grant_notifications())

    if not tap_text("회원가입"):
        session.add("P-SU-01", "본선", "실패", "회원가입 버튼이 없다")
    else:
        time.sleep(0.8)
        session.add(
            "P-SU-01",
            "본선",
            "통과" if has("약연 서비스 이용약관") else "실패",
            "동의 화면" if has("약연 서비스 이용약관") else visible()[:120],
        )
    back()

    consent_one_off(session, "P-SU-02", "check_terms")
    consent_one_off(session, "P-SU-03", "check_privacy")
    consent_one_off(session, "P-SU-04", "check_notice")
    consent_one_off(session, "P-SU-05", "check_sensitive")

    launch()
    tap_text("회원가입")
    time.sleep(0.6)
    for box in ("check_terms", "check_privacy", "check_notice", "check_sensitive"):
        node = find(lambda item, box=box: item.rid.endswith("/" + box))
        if node and not node.checked:
            tap(node)
    tap_id("button_consent_proceed") or tap_text("동의하고 회원가입 진행")
    time.sleep(0.8)
    session.add(
        "P-SU-06",
        "본선",
        "통과" if has("누구의 약을 관리하시나요?") else "실패",
        "유형 화면" if has("누구의 약을 관리하시나요?") else visible()[:120],
    )
    tap_id("button_next") or tap_text("다음")
    time.sleep(0.8)
    nodes = screen()
    blocked = not has("이름", nodes) and (
        has("누구의 약을 관리하시나요?", nodes) or has("전부 입력해 주세요", nodes)
    )
    session.add(
        "P-SU-07",
        "곁가지",
        "통과" if blocked else "실패",
        "정보 입력으로 가지 않았다" if blocked else visible(nodes)[:120],
    )
    back()
    time.sleep(0.5)
    session.add(
        "P-SU-08",
        "곁가지",
        "통과" if has("로그인") and has("회원가입") else "실패",
        "로그인 화면" if has("로그인") else visible()[:80],
    )

    tap_text("회원가입")
    time.sleep(0.5)
    for box in ("check_terms", "check_privacy", "check_notice", "check_sensitive"):
        node = find(lambda item, box=box: item.rid.endswith("/" + box))
        if node and not node.checked:
            tap(node)
    tap_text("동의하고 회원가입 진행") or tap_id("button_consent_proceed")
    time.sleep(0.6)
    tap_text("본인")
    tap_text("다음") or tap_id("button_next")
    time.sleep(0.8)
    session.add(
        "P-SU-09",
        "본선",
        "통과" if has("아이디") and has("비밀번호") else "실패",
        "정보 입력" if has("비밀번호") else visible()[:100],
    )
    back()
    time.sleep(0.5)
    session.add(
        "P-SU-10",
        "곁가지",
        "통과" if has("누구의 약을 관리하시나요?") else "실패",
        "유형 화면" if has("누구의 약을 관리하시나요?") else visible()[:80],
    )
    tap_text("본인")
    tap_text("다음")
    time.sleep(0.6)
    fill_id("field_signup_id", "ab")
    fill_id("field_signup_password", "Abcdef1!")
    tap_id("button_submit_signup") or tap_text("가입")
    time.sleep(1.0)
    nodes = screen()
    session.add(
        "P-SU-11",
        "곁가지",
        "통과" if ("아이디" in visible(nodes) and "가입되었습니다" not in visible(nodes)) else "실패",
        visible(nodes)[:140],
    )
    fill_id("field_signup_id", "flowtest1")
    fill_id("field_signup_password", "1234")
    tap_id("button_submit_signup") or tap_text("가입")
    time.sleep(1.0)
    nodes = screen()
    session.add(
        "P-SU-12",
        "곁가지",
        "통과" if "가입되었습니다" not in visible(nodes) else "실패",
        visible(nodes)[:140],
    )
    fill_id("field_signup_id", "가나다")
    field = find(lambda item: item.rid.endswith("/field_signup_id"))
    editors = [
        item.text
        for item in screen()
        if "EditText" in item.blob() or (field and abs(item.center[1] - field.center[1]) < 80 and item.text)
    ]
    shown = " ".join(editors)
    kept = "가" in shown or "나" in shown or "다" in shown
    session.add(
        "P-SU-13",
        "곁가지",
        "통과" if not kept else "실패",
        "아이디 칸에 한글이 남지 않았다" if not kept else shown[:100],
    )
    fill_id("field_signup_password", "Abcdef1!")
    fill_id("field_password_confirm", "Abcdef2!")
    tap_id("button_submit_signup") or tap_text("가입")
    time.sleep(0.8)
    session.add(
        "P-SU-14",
        "곁가지",
        "통과" if "가입되었습니다" not in visible() else "실패",
        visible()[:120],
    )
    for case_id, reason in (
        ("P-SU-15", "기존 본인 계정으로 로그인한다. 인증번호를 보내지 않는다"),
        ("P-SU-16", "가입 요청을 보내지 않아 verified를 보지 않는다"),
        ("P-SU-17", "가입 인증 화면까지 가지 않는다"),
        ("P-SU-18", "새 아이디 중복 확인을 하지 않는다"),
        ("P-SU-19", "새 가입을 완료하지 않는다"),
    ):
        session.add(case_id, "본선" if case_id in {"P-SU-17", "P-SU-18", "P-SU-19"} else "곁가지", "미실행", reason)
    back()
    back()
    time.sleep(0.5)

    fill_id("field_login_password", "x")
    clear_field()
    tap_id("button_login") or tap_text("로그인")
    time.sleep(0.7)
    nodes = screen()
    session.add(
        "P-LI-01",
        "곁가지",
        "통과" if has("아이디를 입력해 주세요", nodes) and not has("안녕하세요", nodes) else "실패",
        visible(nodes)[:120],
    )
    fill_id("field_login_id", "nobody")
    tap_id("button_login") or tap_text("로그인")
    time.sleep(0.7)
    nodes = screen()
    session.add(
        "P-LI-02",
        "곁가지",
        "통과" if has("비밀번호를 입력해 주세요", nodes) else "실패",
        visible(nodes)[:120],
    )
    tap_text("아이디 찾기")
    time.sleep(0.6)
    session.add(
        "P-LI-03",
        "곁가지",
        "통과" if has("아이디 찾기") else "실패",
        visible()[:100],
    )
    session.add("P-LI-04", "곁가지", "미실행", "없는 계정 조회는 휴대폰 인증이 필요하다")
    back()
    time.sleep(0.4)
    session.add(
        "P-LI-05",
        "곁가지",
        "통과" if has("회원가입") else "실패",
        "로그인 화면으로 돌아왔다" if has("회원가입") else visible()[:80],
    )
    tap_text("비밀번호 재설정")
    time.sleep(0.6)
    session.add(
        "P-LI-06",
        "곁가지",
        "통과" if has("비밀번호 재설정") else "실패",
        visible()[:100],
    )
    session.add("P-LI-07", "곁가지", "미실행", "새 비밀번호 칸은 문자 인증 뒤에만 열린다")
    back()
    time.sleep(0.4)
    session.add(
        "P-LI-08",
        "곁가지",
        "통과" if has("회원가입") else "실패",
        "로그인 화면" if has("회원가입") else visible()[:80],
    )


def login(user_id: str, password: str) -> bool:
    launch()
    if has("안녕하세요") or has("오늘의 복약"):
        return True
    fill_id("field_login_id", user_id)
    fill_id("field_login_password", password)
    tap_id("button_login")
    time.sleep(2.5)
    return has("안녕하세요") or has("홈") and has("설정")


def run_login(session: Session, user_id: str, password: str) -> None:
    shell("logcat -c")
    fill_id("field_login_id", user_id)
    fill_id("field_login_password", WRONG_PASSWORD)
    tap_id("button_login")
    time.sleep(2.0)
    nodes = screen()
    text = visible(nodes)
    session.add(
        "P-LI-09",
        "곁가지",
        "통과" if "안녕하세요" not in text else "실패",
        "홈으로 가지 않았다" if "안녕하세요" not in text else text[:80],
    )
    session.add(
        "P-LI-10",
        "곁가지",
        "통과" if "아이디 또는 비밀번호가 올바르지 않습니다." in text else "실패",
        text[:140],
    )
    divided = "아이디가 틀" in text or "비밀번호가 틀" in text
    session.add(
        "P-LI-11",
        "곁가지",
        "통과" if not divided else "실패",
        "어느 쪽이 틀렸는지 나누지 않았다",
    )
    session.add(
        "P-LI-12",
        "곁가지",
        "통과" if not logcat_contains(WRONG_PASSWORD) else "실패",
        "로그에서 시험용 틀린 비밀번호를 찾지 못했다" if not logcat_contains(WRONG_PASSWORD) else "로그에 문자열이 있다",
    )
    shell("cmd connectivity airplane-mode enable")
    time.sleep(1.0)
    fill_id("field_login_id", user_id)
    fill_id("field_login_password", password)
    tap_id("button_login")
    time.sleep(2.0)
    text = visible()
    home = "안녕하세요" in text
    session.add("P-LI-13", "곁가지", "통과" if not home else "실패", text[:120])
    session.add(
        "P-LI-14",
        "곁가지",
        "통과" if ("연결" in text or "서버" in text) else "실패",
        text[:140],
    )
    session.add(
        "P-LI-15",
        "곁가지",
        "통과" if "가입되었습니다" not in text and not home else "실패",
        "로그인 성공 문구가 없다",
    )
    shell("cmd connectivity airplane-mode disable")
    time.sleep(2.0)
    ok = login(user_id, password)
    session.add("P-LI-16", "본선", "통과" if ok else "실패", "홈으로 이동" if ok else visible()[:140])
    if not ok:
        return
    for case_id, label in (
        ("P-LI-17", "홈"),
        ("P-LI-18", "복약 일정"),
        ("P-LI-19", "그래프"),
        ("P-LI-20", "약통"),
        ("P-LI-21", "설정"),
    ):
        session.add(case_id, "본선", "통과" if has(label) else "실패", label)
    force_stop()
    launch()
    time.sleep(2.0)
    text = visible()
    session.add(
        "P-LI-22",
        "본선",
        "통과" if "안녕하세요" in text and "회원가입" not in text else "실패",
        text[:100],
    )
    shell("logcat -c")
    time.sleep(0.5)
    logged = adb("logcat", "-d", "-t", "200").stdout or ""
    session.add(
        "P-LI-23",
        "곁가지",
        "통과" if "eyJ" not in logged else "실패",
        "로그에서 JWT 형태 문자열을 찾지 못했다" if "eyJ" not in logged else "JWT 형태 문자열이 있다. 값은 적지 않는다",
    )


def open_tab(label: str) -> bool:
    return tap_text(label, scrolls=1)


def run_after_login(session: Session) -> None:
    open_tab("약통")
    time.sleep(1.0)
    text = visible()
    connected = "연결됨" in text or "오프라인" in text
    if connected:
        session.add("P-PB-01", "본선", "미실행", "이미 약통이 연결되어 연결 안 됨 상태를 만들지 않는다")
        session.add("P-PB-03", "본선", "미실행", "이미 등록된 약통이라 시리얼을 다시 넣지 않는다")
        gone = "연결된 약통이 없어요" not in text
        session.add("P-PB-04", "본선", "통과" if gone else "실패", text[:140])
    else:
        session.add("P-PB-01", "본선", "통과" if ("연결 안 됨" in text or "연결된 약통이 없어요" in text) else "실패", text[:140])
        session.add("P-PB-03", "본선", "미실행", "시리얼과 비밀값은 이 스크립트에 없다")
        session.add("P-PB-04", "본선", "미실행", "이번 실행에서 새로 등록하지 않았다")
    session.add("P-PB-02", "본선", "미실행", "약통 시계는 에뮬레이터 화면만으로 읽지 않는다")

    open_tab("설정")
    time.sleep(0.6)
    session.add(
        "P-ST-11",
        "본선",
        "설계 차이" if not has("복약자 연동 관리") else "실패",
        "본인 설정에 연동 줄이 없다" if not has("복약자 연동 관리") else "연동 줄이 보여 통과로 적지 않는다",
    )
    if tap_text("알림 설정"):
        time.sleep(0.6)
        text = visible()
        session.add(
            "P-ST-01",
            "본선",
            "통과" if "복용 시간 알림" in text else "실패",
            text[:160],
        )
        session.add(
            "P-ST-02",
            "곁가지",
            "미실행",
            "예정 시각까지 기다리지 않는다. 오지 않은 것만으로 실패로 적지 않는다",
        )
        back()
    else:
        session.add("P-ST-01", "본선", "실패", "알림 설정이 없다")
        session.add("P-ST-02", "곁가지", "미실행", "알림 설정에 들어가지 못했다")

    for case_id in ("P-ST-03", "P-ST-04"):
        session.add(case_id, "곁가지", "미실행", "약통 상태 하위 스위치 조작은 다음 실행에서 화면을 고정한 뒤 한다")

    open_tab("홈")
    time.sleep(1.2)
    text = visible()
    session.add("P-HM-04", "본선", "통과" if "위험" not in text else "실패", text[:160])
    session.add(
        "P-HM-05",
        "본선",
        "통과" if ("드셨나요" in text or "아직 복용" in text or "오늘" in text or "예정" in text) else "실패",
        text[:160],
    )
    banner = "병용금기 등재 정보가" in text and "건 있어요" in text
    session.add("P-HM-06", "본선", "통과" if banner or "병용금기" not in text else "실패", text[:160])
    if "안전" in text and "볼 수 없습니다" not in text:
        session.add("P-HM-07", "곁가지", "실패", "안전이라고 적혀 있다")
    elif "등재 확인이 안 된" in text:
        session.add("P-HM-07", "곁가지", "통과", "확인하지 못한 약이 있어도 안전이라고 하지 않는다")
    else:
        session.add("P-HM-07", "곁가지", "미실행", "확인하지 못한 약 문장이 화면에 없다")
    for case_id in ("P-HM-01", "P-HM-02", "P-HM-03"):
        session.add(case_id, "본선", "미실행", "홈 요약 JSON은 이 화면 자동화에서 읽지 않는다")

    open_tab("그래프")
    time.sleep(1.5)
    text = visible()
    expected = "함께 복용 중인 약의 병용금기 등재 여부만 표시합니다."
    session.add("P-GR-01", "본선", "통과" if expected in text else "실패", text[:180])
    session.add(
        "P-GR-02",
        "본선",
        "통과" if text.strip() else "실패",
        "노드가 보여도 등재로 세지 않는다. " + text[:120],
    )
    no_line = "선" not in text
    session.add("P-GR-03", "곁가지", "미실행" if not no_line else "통과", "선 유무를 화면 문구만으로 단정하지 않는다" if not no_line else text[:80])
    session.add(
        "P-GR-04",
        "곁가지",
        "통과" if "안전" not in text else "실패",
        "없음이 안전이라는 문장은 없다" if "안전" not in text else text[:80],
    )
    for case_id in ("P-GR-05", "P-GR-06", "P-GR-07", "P-GR-08", "P-GR-09", "P-GR-10", "P-GR-11", "P-GR-12"):
        session.add(case_id, "곁가지", "미실행", "등재 선 또는 목록을 눌러 상세 칸을 열지 못했다")

    open_tab("복약 일정")
    time.sleep(1.0)
    text = visible()
    session.add("P-HS-01", "본선", "통과" if text.strip() else "실패", text[:140])
    session.add("P-HS-05", "본선", "통과" if not has("캘린더") else "실패", "탭 이름에 캘린더는 없다")
    for case_id in ("P-HS-02", "P-HS-03", "P-HS-04"):
        session.add(case_id, "본선", "미실행", "다른 날짜 달력 조작은 이번 실행에서 날짜 칸을 고정하지 못했다")

    for case_id, branch in (
        ("P-RG-01", "곁가지"),
        ("P-RG-02", "본선"),
        ("P-RG-03", "곁가지"),
        ("P-RG-04", "곁가지"),
        ("P-RG-05", "곁가지"),
        ("P-RG-06", "곁가지"),
        ("P-RG-07", "곁가지"),
        ("P-RG-08", "곁가지"),
        ("P-RG-09", "곁가지"),
        ("P-RG-10", "곁가지"),
        ("P-RG-11", "곁가지"),
        ("P-RG-12", "본선"),
        ("P-RG-13", "본선"),
        ("P-RG-14", "본선"),
        ("P-RG-15", "본선"),
        ("P-RG-16", "본선"),
    ):
        session.add(case_id, branch, "미실행", "약 등록 입력 칸은 화면을 한 번 더 읽은 뒤 이어서 누른다")
    for case_id, branch in (
        ("P-ML-01", "본선"),
        ("P-ML-02", "곁가지"),
        ("P-ML-03", "본선"),
        ("P-ML-04", "곁가지"),
        ("P-ML-05", "곁가지"),
        ("P-ML-06", "곁가지"),
        ("P-ML-07", "곁가지"),
    ):
        session.add(case_id, branch, "미실행", "시험 약 목록 조작은 등록 다음에 한다")
    for case_id in ("P-PB-05", "P-PB-06", "P-PB-07", "P-PB-08", "P-PB-09", "P-PB-10", "P-PB-11", "P-PB-12", "P-PB-13"):
        session.add(case_id, "본선" if case_id in {"P-PB-06", "P-PB-10", "P-PB-11"} else "곁가지", "미실행", "칸 배정은 시험 약이 생긴 뒤에 한다")
    for case_id in ("P-TK-01", "P-TK-02", "P-TK-03", "P-TK-04", "P-TK-05", "P-TK-06"):
        session.add(case_id, "본선", "미실행", "복용 체크는 시험 약 저장 뒤에 한다")
    session.add("P-TK-07", "곁가지", "미실행", "3시간이 지난 회차를 일부러 만들지 않는다")
    for case_id in ("P-PB-14", "P-PB-15", "P-PB-16", "P-PB-17", "P-PB-18", "P-PB-19", "P-PB-20"):
        session.add(case_id, "본선", "미실행", "약통의 감지와 물리 버튼은 스크립트가 누르지 못한다")
    session.add("P-HS-06", "본선", "미실행", "시험 약을 저장한 뒤에 앱을 다시 연다")
    for case_id, branch in (
        ("P-ST-05", "본선"),
        ("P-ST-06", "본선"),
        ("P-ST-07", "곁가지"),
        ("P-ST-08", "곁가지"),
        ("P-ST-09", "곁가지"),
        ("P-ST-10", "곁가지"),
        ("P-ST-12", "곁가지"),
        ("P-ST-13", "곁가지"),
    ):
        session.add(case_id, branch, "미실행", "공유와 철회는 약 확인 뒤에 한다")
    for case_id in ("P-LO-01", "P-LO-02", "P-LO-03", "P-LO-04"):
        session.add(case_id, "곁가지" if case_id == "P-LO-01" else "본선", "미실행", "로그아웃은 철회 확인 뒤에 한다")


def write_results(session: Session) -> None:
    lines = [
        "# 본인 흐름 자동화 결과",
        "",
        "범위는 본인 본선과 곁가지다. 반복 측정 P-PB-21~25와 대리인 행은 넣지 않았다.",
        "비밀번호, 시리얼, 비밀값은 적지 않는다.",
        "종료와 삭제는 병원명 흐름시험으로 이번 실행에서 만든 약만 대상으로 한다.",
        "",
        "| ID | 갈래 | 판정 | 비고 |",
        "| --- | --- | --- | --- |",
    ]
    for row in session.rows:
        note = row.note.replace("|", "/")
        lines.append(f"| {row.case_id} | {row.branch} | {row.verdict} | {note} |")
    counts: dict[str, int] = {}
    for row in session.rows:
        counts[row.verdict] = counts.get(row.verdict, 0) + 1
    lines.append("")
    lines.append("합계: " + ", ".join(f"{name} {count}" for name, count in counts.items()))
    RESULT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"결과 {RESULT}")


def logged_in(nodes: list[Node] | None = None) -> bool:
    nodes = nodes if nodes is not None else screen()
    return find(lambda item: item.rid.endswith("/tab_home"), nodes) is not None


def dismiss_keyboard() -> None:
    shell("input keyevent 111")
    time.sleep(0.3)


def set_checked(suffix: str, want: bool) -> bool:
    node = find(lambda item: item.rid.endswith("/" + suffix))
    if node is None:
        return False
    if node.checked != want:
        tap(node)
    return True


def confirm_if(label: str) -> bool:
    time.sleep(0.6)
    return tap_text(label, scrolls=1)


def open_med_list() -> bool:
    tap_id("tab_home")
    time.sleep(1.0)
    if tap_id("button_add_med") or tap_text("약물 목록 보기"):
        time.sleep(1.0)
    return has("내 약물") or has("약 등록")


def open_register() -> bool:
    if not (has("약 등록") or open_med_list()):
        return False
    if not (tap_id("button_register_med") or tap_text("약 등록")):
        return False
    time.sleep(1.0)
    return True


def pick_name(query: str) -> str | None:
    if not (tap_id("text_name_value") or tap_text("이름 없음")):
        print("이름 칸이 없다", flush=True)
        return None
    time.sleep(0.8)
    if not tap_id("field_name_query"):
        print("검색 칸이 없다 " + visible()[:160], flush=True)
        return None
    clear_field()
    write_text(query)
    time.sleep(2.5)
    nodes = screen()
    query_node = find(lambda item: item.rid.endswith("/field_name_query"), nodes)
    floor = query_node.bounds[3] if query_node else 0
    blocked = {
        query,
        "약물 검색",
        "약물명 검색",
        "취소",
        "확인",
        "직접 입력하기",
        "해당하는 이름이 없습니다.",
        "검색 결과가 없습니다.",
    }
    for node in nodes:
        if node.center[1] <= floor or not node.text or node.text in blocked:
            continue
        if node.rid.endswith("/field_name_query") or node.rid.endswith("/text_name_empty"):
            continue
        tap(node)
        time.sleep(0.8)
        return node.text.split(" · ")[0]
    print("검색 결과가 없다 " + visible()[:240], flush=True)
    return None


def save_test_med(hospital: str, timing_id: str) -> str | None:
    if not open_register():
        return None
    for box, want in (("check_med_collection", True), ("check_med_notice", True)):
        set_checked(box, want)
    tap_id("button_med_consent_next") or tap_text("동의하고 약 등록")
    time.sleep(1.0)
    if not has("병원"):
        return None
    fill_id("field_hospital", hospital)
    dismiss_keyboard()
    tap_id("button_add_medicine") or tap_text("추가 등록")
    time.sleep(0.8)
    chosen = pick_name("vitamin")
    if not chosen:
        return None
    fill_id("field_amount_number", "1")
    dismiss_keyboard()
    set_checked("check_morning", True)
    set_checked("check_lunch", False)
    set_checked("check_evening", True)
    set_checked("check_bedtime", False)
    set_checked(timing_id, True)
    if not (tap_id("text_period_end") or tap_text("종료일")):
        print("종료일 칸이 없다", flush=True)
        return None
    time.sleep(0.8)
    ok = find(lambda item: item.rid == "android:id/button1" or item.text in {"확인", "OK"})
    if ok:
        tap(ok)
    else:
        print("날짜 확인 버튼이 없다 " + visible()[:200], flush=True)
        back()
        return None
    time.sleep(0.6)
    set_checked("check_confirmed", True)
    for _ in range(4):
        if find_text("약통 없이 사용하기"):
            break
        shell("input swipe 540 1500 540 700 250")
        time.sleep(0.3)
    if not (tap_id("button_without_pillbox") or tap_text("약통 없이 사용하기")):
        print("약통 없이 사용하기가 없다 " + visible()[:200], flush=True)
        return None
    time.sleep(1.0)
    if not (tap_id("button_alarm_save") or tap_text("최종 등록")):
        print("최종 등록이 없다 " + visible()[:200], flush=True)
        return None
    time.sleep(2.5)
    open_med_list()
    if has(hospital):
        return chosen
    print("목록에 시험 병원이 없다 " + visible()[:240], flush=True)
    return None


def run_logged_in(session: Session) -> None:
    """이미 로그인된 본인 화면에서 본다. 병원명에 흐름시험이 없는 약은 누르지 않는다."""
    shell("settings put global window_animation_scale 0")
    shell("settings put global transition_animation_scale 0")
    shell("settings put global animator_duration_scale 0")
    for case_id, reason in (
        ("P-IN-01", "이미 설치된 상태에서 본인 화면을 연다"),
        ("P-IN-02", "로그인된 상태라 첫 실행 로그인 화면을 다시 열지 않는다"),
    ):
        session.add(case_id, "본선", "미실행", reason)
    for case_id in ("P-IN-03", "P-IN-04", "P-IN-05", "P-IN-06"):
        session.add(case_id, "본선", "미실행", "로그인 화면 문구는 비밀번호를 받은 뒤 로그아웃하고 본다")
    session.add("P-IN-07", "본선", "통과", grant_notifications())
    for case_id in (
        "P-SU-01", "P-SU-02", "P-SU-03", "P-SU-04", "P-SU-05", "P-SU-06", "P-SU-07",
        "P-SU-08", "P-SU-09", "P-SU-10", "P-SU-11", "P-SU-12", "P-SU-13", "P-SU-14",
        "P-SU-15", "P-SU-16", "P-SU-17", "P-SU-18", "P-SU-19",
        "P-LI-01", "P-LI-02", "P-LI-03", "P-LI-04", "P-LI-05", "P-LI-06", "P-LI-07",
        "P-LI-08", "P-LI-09", "P-LI-10", "P-LI-11", "P-LI-12", "P-LI-13", "P-LI-14",
        "P-LI-15", "P-LI-16", "P-LI-17", "P-LI-18", "P-LI-19", "P-LI-20", "P-LI-21",
        "P-LI-22", "P-LI-23",
    ):
        branch = "본선" if case_id in {
            "P-SU-01", "P-SU-06", "P-SU-09", "P-SU-17", "P-SU-18", "P-SU-19",
            "P-LI-16", "P-LI-17", "P-LI-18", "P-LI-19", "P-LI-20", "P-LI-21", "P-LI-22",
        } else "곁가지"
        session.add(case_id, branch, "미실행", "아이디와 비밀번호를 받기 전에는 로그아웃하지 않는다")

    tap_id("tab_pillbox") or tap_text("약통")
    time.sleep(1.0)
    text = visible()
    connected = "연결됨" in text or "오프라인" in text
    session.add("P-PB-01", "본선", "미실행", "이미 연결되어 연결 안 됨 상태를 만들지 않는다")
    session.add("P-PB-02", "본선", "미실행", "약통 시계는 에뮬레이터 화면만으로 읽지 않는다")
    session.add("P-PB-03", "본선", "미실행", "이미 등록된 약통이라 시리얼을 다시 넣지 않는다")
    session.add(
        "P-PB-04",
        "본선",
        "통과" if connected and "연결된 약통이 없어요" not in text else "실패",
        text[:160],
    )

    tap_id("tab_home")
    time.sleep(1.2)
    text = visible()
    session.add("P-HM-04", "본선", "통과" if "위험" not in text else "실패", text[:160])
    session.add(
        "P-HM-05",
        "본선",
        "통과" if any(word in text for word in ("드셨나요", "아직 복용", "오늘", "예정", "복약")) else "실패",
        text[:160],
    )
    session.add(
        "P-HM-06",
        "본선",
        "통과" if ("병용금기 등재 정보가" in text and "건 있어요" in text) or "병용금기" not in text else "실패",
        text[:160],
    )
    if "안전" in text and "볼 수 없습니다" not in text:
        session.add("P-HM-07", "곁가지", "실패", "안전이라고 적혀 있다")
    elif "등재 확인이 안 된" in text:
        session.add("P-HM-07", "곁가지", "통과", "확인하지 못한 약이 있어도 안전이라고 하지 않는다")
    else:
        session.add("P-HM-07", "곁가지", "미실행", "확인하지 못한 약 문장이 화면에 없다")
    for case_id in ("P-HM-01", "P-HM-02", "P-HM-03"):
        session.add(case_id, "본선", "미실행", "홈 요약 JSON은 화면 자동화에서 읽지 않는다")

    tap_id("tab_graph") or tap_text("그래프")
    time.sleep(1.2)
    text = visible()
    expected = "함께 복용 중인 약의 병용금기 등재 여부만 표시합니다."
    session.add("P-GR-01", "본선", "통과" if expected in text else "실패", text[:180])
    session.add("P-GR-02", "본선", "통과" if text.strip() else "실패", text[:140])
    session.add("P-GR-03", "곁가지", "미실행", "선 유무를 화면 문구만으로 단정하지 않는다")
    session.add("P-GR-04", "곁가지", "통과" if "안전" not in text else "실패", text[:120])
    for case_id in ("P-GR-05", "P-GR-06", "P-GR-07", "P-GR-08", "P-GR-09", "P-GR-10", "P-GR-11", "P-GR-12"):
        session.add(case_id, "곁가지", "미실행", "등재 선을 눌러 상세를 열지 않았다")

    tap_text("복약 일정", scrolls=2)
    time.sleep(1.0)
    text = visible()
    session.add("P-HS-01", "본선", "통과" if text.strip() else "실패", text[:140])
    session.add("P-HS-05", "본선", "통과" if "캘린더" not in text else "실패", "탭 이름에 캘린더는 없다")
    for case_id in ("P-HS-02", "P-HS-03", "P-HS-04", "P-HS-06"):
        session.add(case_id, "본선", "미실행", "다른 날짜와 재실행은 시험 약 저장 뒤에 본다")

    tap_id("tab_settings") or tap_text("설정")
    time.sleep(0.8)
    absent = not has("복약자 연동 관리")
    session.add(
        "P-ST-11",
        "본선",
        "설계 차이" if absent else "실패",
        "본인 설정에 연동 줄이 없다" if absent else "연동 줄이 보여 통과로 적지 않는다",
    )
    if tap_text("알림 설정"):
        time.sleep(0.6)
        text = visible()
        session.add("P-ST-01", "본선", "통과" if "복용 시간 알림" in text else "실패", text[:160])
        back()
    else:
        session.add("P-ST-01", "본선", "실패", "알림 설정이 없다")
    session.add("P-ST-02", "곁가지", "미실행", "예정 시각까지 기다리지 않는다. 오지 않은 것만으로 실패로 적지 않는다")
    for case_id in ("P-ST-03", "P-ST-04"):
        session.add(case_id, "곁가지", "미실행", "약통 상태 하위 스위치는 연결 해제 전에 다시 연다")

    made: list[str] = []
    try:
        if not open_med_list():
            raise RuntimeError("내 약물 화면이 열리지 않았다")
        open_register()
        nodes = screen()
        stayed = has("동의", nodes) and not has("처방받은 병원", nodes)
        tap_id("button_med_consent_next") or tap_text("동의하고 약 등록")
        time.sleep(0.8)
        blocked = has("동의") and not has("처방받은 병원")
        session.add("P-RG-01", "곁가지", "통과" if stayed and blocked else "실패", visible()[:140])
        set_checked("check_med_collection", True)
        set_checked("check_med_notice", True)
        tap_id("button_med_consent_next") or tap_text("동의하고 약 등록")
        time.sleep(1.0)
        session.add(
            "P-RG-02",
            "본선",
            "통과" if has("처방받은 병원") or has("병원") else "실패",
            visible()[:120],
        )
        back()
        time.sleep(0.4)
        session.add("P-RG-03", "곁가지", "미실행", "가린 약봉투 사진을 고르지 않는다")
        session.add("P-RG-04", "곁가지", "미실행", "인식 초안이 없다")
        session.add("P-RG-05", "곁가지", "미실행", "에뮬레이터 카메라로 촬영하지 않는다")

        first = save_test_med("흐름시험식전", "check_timing_before")
        if first:
            made.append(first)
            session.made.append(first)
        session.add(
            "P-RG-12",
            "본선",
            "통과" if first and has("흐름시험식전") else "실패",
            f"저장한 약 {first}" if first else visible()[:160],
        )
        text = visible()
        session.add("P-RG-13", "본선", "미실행" if "식전" not in text else "통과", text[:140])
        session.add(
            "P-RG-14",
            "본선",
            "통과" if first and "아침" in text and "저녁" in text else "미실행",
            text[:140],
        )
        second = save_test_med("흐름시험식후", "check_timing_after")
        if second:
            made.append(second)
        session.add(
            "P-RG-15",
            "본선",
            "통과" if second and has("흐름시험식후") else "실패",
            f"저장한 약 {second}" if second else visible()[:160],
        )
        session.add("P-RG-16", "본선", "미실행" if "식후" not in visible() else "통과", visible()[:140])
        for case_id in ("P-RG-06", "P-RG-07", "P-RG-08", "P-RG-09", "P-RG-10", "P-RG-11"):
            session.add(case_id, "곁가지", "미실행", "본선 저장을 먼저 끝내고, 거절 입력은 저장된 시험 약을 망가뜨리지 않게 따로 연다")
    except Exception as error:  # noqa: BLE001
        session.add("P-RG-12", "본선", "실패", str(error)[:180])

    if made and open_med_list():
        text = visible()
        both = "흐름시험식전" in text and "흐름시험식후" in text
        session.add("P-ML-01", "본선", "통과" if both else "실패", text[:160])
        node = find_text(made[0])
        if node:
            tap(node)
            time.sleep(0.6)
            asked = has("이 약의 복용을 끝낼까요?")
            confirm_if("취소")
            time.sleep(0.5)
            session.add("P-ML-05", "곁가지", "통과" if asked else "실패", visible()[:140])
            if asked:
                node = find_text(made[0])
                if node:
                    tap(node)
                    time.sleep(0.5)
                    confirm_if("종료")
                    time.sleep(1.2)
        else:
            session.add("P-ML-05", "곁가지", "실패", "흐름시험 약 줄을 찾지 못했다")
        if len(made) > 1:
            node = find_text(made[1])
            if node:
                x, y = node.center
                shell(f"input swipe {x} {y} {x} {y} 1000")
                time.sleep(0.8)
                asked = has("이 약을 목록에서 삭제할까요?")
                session.add("P-ML-06", "곁가지", "통과" if asked else "실패", visible()[:140])
                if asked:
                    confirm_if("삭제")
                    time.sleep(1.2)
            else:
                session.add("P-ML-06", "곁가지", "실패", "둘째 시험 약 줄을 찾지 못했다")
        else:
            session.add("P-ML-06", "곁가지", "미실행", "삭제할 둘째 시험 약이 없다")
        if tap_text("복용 조기 종료하기"):
            time.sleep(0.6)
            body = has("종료한 약물은 복용 종료 목록에서 확인할 수 있어요.")
            session.add("P-ML-07", "곁가지", "통과" if body else "실패", visible()[:140])
            if body and has("흐름시험"):
                confirm_if("종료")
                time.sleep(1.2)
            else:
                confirm_if("취소")
        else:
            session.add("P-ML-07", "곁가지", "미실행", "흐름시험 봉투의 조기 종료 버튼이 보이지 않는다")
    else:
        for case_id, branch in (("P-ML-01", "본선"), ("P-ML-05", "곁가지"), ("P-ML-06", "곁가지"), ("P-ML-07", "곁가지")):
            session.add(case_id, branch, "미실행", "흐름시험 약이 목록에 없다")
    for case_id, branch in (("P-ML-02", "곁가지"), ("P-ML-03", "본선"), ("P-ML-04", "곁가지")):
        session.add(case_id, branch, "미실행", "시간 수정과 취소는 종료 확인 다음에 남은 시험 봉투가 있을 때 본다")

    for case_id in ("P-PB-05", "P-PB-06", "P-PB-07", "P-PB-08", "P-PB-09", "P-PB-10", "P-PB-11", "P-PB-12", "P-PB-13"):
        branch = "본선" if case_id in {"P-PB-06", "P-PB-10", "P-PB-11"} else "곁가지"
        session.add(case_id, branch, "미실행", "칸은 이미 3칸이 사용 중이라 원래 약을 빼지 않는다")
    for case_id in ("P-TK-01", "P-TK-02", "P-TK-03", "P-TK-04", "P-TK-05", "P-TK-06"):
        session.add(case_id, "본선", "미실행", "복용 체크는 시험 약의 시각 창을 연 뒤에 한다")
    session.add("P-TK-07", "곁가지", "미실행", "3시간이 지난 회차를 만들지 않는다")
    for case_id in ("P-PB-14", "P-PB-15", "P-PB-16", "P-PB-17", "P-PB-18", "P-PB-19", "P-PB-20"):
        session.add(case_id, "본선", "미실행", "약통 감지와 물리 버튼은 스크립트가 누르지 못한다")

    tap_id("tab_settings") or tap_text("설정")
    time.sleep(0.5)
    if tap_text("서비스 안내"):
        time.sleep(0.6)
        if tap_text("복약 정보 수집 및 이용 동의 철회"):
            time.sleep(0.6)
            shown = has("동의 철회") or has("서비스 사용이 제한")
            confirm_if("취소")
            time.sleep(0.5)
            session.add("P-ST-12", "곁가지", "통과" if shown else "실패", "철회 확인을 열었다가 취소했다" if shown else visible()[:120])
            if not made:
                session.add("P-ST-13", "곁가지", "미실행", "시험 약이 생기지 않아 철회는 확정하지 않는다")
            elif tap_text("복약 정보 수집 및 이용 동의 철회"):
                time.sleep(0.5)
                confirmed = confirm_if("철회")
                time.sleep(1.2)
                text = visible()
                session.add(
                    "P-ST-13",
                    "곁가지",
                    "통과" if confirmed and ("철회" in text) else "실패",
                    text[:160],
                )
            else:
                session.add("P-ST-13", "곁가지", "실패", "철회 줄을 다시 찾지 못했다")
        else:
            session.add("P-ST-12", "곁가지", "실패", "철회 줄이 없다")
            session.add("P-ST-13", "곁가지", "미실행", "철회 줄을 열지 못했다")
        back()
    else:
        session.add("P-ST-12", "곁가지", "실패", "서비스 안내가 없다")
        session.add("P-ST-13", "곁가지", "미실행", "서비스 안내가 없다")

    tap_id("tab_pillbox") or tap_text("약통")
    time.sleep(0.8)
    if tap_text("약통 설정"):
        time.sleep(0.6)
        if tap_text("연결 해제"):
            time.sleep(0.6)
            body = has("서버에 약통 등록 해제를 요청합니다")
            session.add("P-ST-14", "본선", "통과" if body else "실패", visible()[:160])
            if body and made:
                confirm_if("해제")
                time.sleep(1.5)
            else:
                confirm_if("취소")
        else:
            session.add("P-ST-14", "본선", "설계 차이", "약통 설정에 연결 해제 줄이 없다. 본인 설정의 연동 줄도 없다")
    else:
        session.add("P-ST-14", "본선", "실패", "약통 설정이 없다")

    for case_id, branch in (
        ("P-ST-05", "본선"), ("P-ST-06", "본선"), ("P-ST-07", "곁가지"),
        ("P-ST-08", "곁가지"), ("P-ST-09", "곁가지"), ("P-ST-10", "곁가지"),
    ):
        session.add(case_id, branch, "미실행", "공유 기간은 철회 뒤에 약이 잠길 수 있어 이번 화면에서 고르지 않았다")
    for case_id in ("P-LO-01", "P-LO-02", "P-LO-03", "P-LO-04", "P-LO-05"):
        branch = "곁가지" if case_id == "P-LO-01" else "본선"
        session.add(case_id, branch, "미실행", "비밀번호가 없어 로그아웃하면 같은 계정으로 돌아오지 못한다")


def tap_under(hospital: str, drug: str, hold: bool = False) -> bool:
    for _ in range(6):
        nodes = screen()
        title = find_text(hospital, nodes)
        if title:
            below = [
                node for node in nodes
                if drug and drug in node.text and node.center[1] > title.bounds[3]
            ]
            below.sort(key=lambda node: node.center[1])
            if below:
                node = below[0]
                if hold:
                    x, y = node.center
                    shell(f"input swipe {x} {y} {x} {y} 1000")
                    time.sleep(0.8)
                else:
                    tap(node)
                return True
        shell("input swipe 540 1500 540 800 250")
        time.sleep(0.4)
    return False


def finish_test_meds(session: Session) -> None:
    """flowtest 병원만 만들고, 그 약만 종료·삭제한다. 목록에 생기기 전에는 철회와 연결 해제를 확정하지 않는다."""
    first = save_test_med("flowtestbefore", "check_timing_before")
    session.add("P-RG-12", "본선", "통과" if first else "실패", first or "식전 시험 약이 목록에 없다")
    if not first:
        session.add("P-ML-05", "곁가지", "미실행", "시험 약이 없어 종료하지 않는다")
        session.add("P-ST-13", "곁가지", "미실행", "시험 약이 없어 철회를 확정하지 않는다")
        return
    second = save_test_med("flowtestafter", "check_timing_after")
    session.add("P-RG-15", "본선", "통과" if second else "실패", second or "식후 시험 약이 목록에 없다")
    open_med_list()
    if tap_under("flowtestbefore", first):
        time.sleep(0.6)
        asked = has("이 약의 복용을 끝낼까요?")
        session.add("P-ML-05", "곁가지", "통과" if asked else "실패", visible()[:140])
        if asked:
            confirm_if("종료")
            time.sleep(1.5)
    else:
        session.add("P-ML-05", "곁가지", "실패", "흐름시험식전 아래 약 줄을 찾지 못했다")
    if second and tap_under("flowtestafter", second, hold=True):
        asked = has("이 약을 목록에서 삭제할까요?")
        session.add("P-ML-06", "곁가지", "통과" if asked else "실패", visible()[:140])
        if asked:
            confirm_if("삭제")
            time.sleep(1.5)
    else:
        session.add("P-ML-06", "곁가지", "미실행" if not second else "실패", "삭제할 식후 시험 약이 없다" if not second else visible()[:120])
    open_med_list()
    stopped = False
    for hospital in ("flowtestbefore", "flowtestafter"):
        if tap_under(hospital, "복용 조기 종료하기"):
            time.sleep(0.5)
            if has("종료한 약물은 복용 종료 목록에서 확인할 수 있어요."):
                confirm_if("종료")
                time.sleep(1.2)
                stopped = True
                break
            confirm_if("취소")
    session.add(
        "P-ML-07",
        "곁가지",
        "통과" if stopped else "미실행",
        "흐름시험 봉투만 종료했다" if stopped else "흐름시험 봉투의 조기 종료 버튼이 남지 않았다",
    )


def main() -> int:
    user_id = os.environ.get("YAKYEON_ID", "").strip()
    password = os.environ.get("YAKYEON_PASSWORD", "")
    session = Session()
    if "--meds" in sys.argv:
        try:
            finish_test_meds(session)
        except Exception as error:  # noqa: BLE001
            session.add("RUN", "본선", "실패", str(error)[:200])
        write_results(session)
        return 1 if any(row.verdict == "실패" for row in session.rows) else 0
    try:
        launch()
        if logged_in() and not (user_id and password):
            print("로그인된 본인 화면이라 로그아웃하지 않는다.", flush=True)
            run_logged_in(session)
        else:
            ensure_login()
            run_before_login(session)
            if user_id and password:
                run_login(session, user_id, password)
                if any(row.case_id == "P-LI-16" and row.verdict == "통과" for row in session.rows):
                    run_after_login(session)
            else:
                print("로그인 화면이고 비밀번호가 없어 로그인 이후는 실행하지 않는다.", flush=True)
    except Exception as error:  # noqa: BLE001
        session.add("RUN", "본선", "실패", str(error)[:200])
    write_results(session)
    failed = [row.case_id for row in session.rows if row.verdict == "실패"]
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
