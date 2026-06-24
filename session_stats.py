#!/usr/bin/env python3
"""
会话消耗统计工具 - 从 Claude Code jsonl 日志中提取每个会话的 token 和调用次数。

用法:
  # 统计单个项目的所有会话
  python3 session_stats.py ~/.claude/projects/-Users-zhangyicheng-zcyl-backend/

  # 统计多个项目
  python3 session_stats.py \
    ~/.claude/projects/-Users-zhangyicheng-zcyl-backend/ \
    ~/.claude/projects/-Users-zhangyicheng-Documents-GitHub-lookGraph/

  # JSON 格式输出，按会话详情
  python3 session_stats.py --json ~/.claude/projects/xxx/
"""

import json
import os
import sys
import argparse
from datetime import datetime, timezone, timedelta
from collections import defaultdict
from typing import Optional

LOOKGRAPH_TOOL_PATTERNS = [
    "lookgraph/",
    "look_graph",
    "semantic_search.py",
    "class_context.py",
    "method_context.py",
    "project_init.py",
    "project_summary.py",
    "project_update.py",
    "impact_analysis.py",
    "method_callchain.py",
    "class_relations.py",
    "semantic_annotate.py",
    "class_methods.py",
    "module_classes.py",
    "project_list.py",
]


def classify_tool(tool_name: str, tool_input: dict) -> str:
    """判断工具调用是否属于 LookGraph"""
    if tool_name == "Bash":
        cmd = tool_input.get("command", "")
        cmd_text = json.dumps(tool_input)
        for pat in LOOKGRAPH_TOOL_PATTERNS:
            if pat in cmd or pat in cmd_text:
                return "lookgraph"
        return "bash_other"
    return tool_name


def analyze_session(filepath: str) -> Optional[dict]:
    entries = []
    try:
        with open(filepath, "r") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    entries.append(json.loads(line))
                except json.JSONDecodeError:
                    continue
    except Exception:
        return None

    if not entries:
        return None

    first_ts = None
    last_ts = None
    session_id = None
    model_name = None

    total_input_tokens = 0
    total_output_tokens = 0
    total_cache_read = 0
    total_cache_creation = 0
    turn_count = 0

    # Tool call stats
    tool_calls_by_name = defaultdict(int)
    lookgraph_tool_calls = 0
    traditional_tool_calls = 0  # Grep + Read + Glob
    bash_other_calls = 0
    thinking_count = 0

    for e in entries:
        t = e.get("type")

        if t == "assistant":
            turn_count += 1
            msg = e.get("message", {})
            usage = msg.get("usage", {})
            total_input_tokens += usage.get("input_tokens", 0)
            total_output_tokens += usage.get("output_tokens", 0)
            total_cache_read += usage.get("cache_read_input_tokens", 0)
            total_cache_creation += usage.get("cache_creation_input_tokens", 0)

            if not model_name:
                model_name = msg.get("model", "")

            # Tool calls and thinking blocks are nested inside content
            for block in msg.get("content", []):
                if not isinstance(block, dict):
                    continue
                block_type = block.get("type", "")
                if block_type == "tool_use":
                    tool_name = block.get("name", "unknown")
                    tool_input = block.get("input", {})
                    category = classify_tool(tool_name, tool_input)
                    tool_calls_by_name[category] += 1
                    if category == "lookgraph":
                        lookgraph_tool_calls += 1
                    elif category in ("Grep", "Read", "Glob"):
                        traditional_tool_calls += 1
                    elif category == "bash_other":
                        bash_other_calls += 1
                elif block_type == "thinking":
                    thinking_count += 1

        if e.get("sessionId"):
            session_id = e["sessionId"]

        ts = e.get("timestamp")
        if ts:
            if first_ts is None or ts < first_ts:
                first_ts = ts
            if last_ts is None or ts > last_ts:
                last_ts = ts

    if turn_count == 0:
        return None

    total_tool_calls = sum(tool_calls_by_name.values())

    # Classify session type: 只要用过 LookGraph 就是 LG 会话
    if lookgraph_tool_calls > 0:
        session_type = "lookgraph"
    elif total_tool_calls == 0:
        session_type = "chat_only"
    else:
        session_type = "non_lg"

    duration_minutes = 0
    if first_ts and last_ts:
        try:
            for fmt_str in ["%Y-%m-%dT%H:%M:%S.%fZ", "%Y-%m-%dT%H:%M:%SZ"]:
                try:
                    start = datetime.strptime(first_ts, fmt_str).replace(tzinfo=timezone.utc)
                    end = datetime.strptime(last_ts, fmt_str).replace(tzinfo=timezone.utc)
                    duration_minutes = round((end - start).total_seconds() / 60, 1)
                    break
                except (ValueError, TypeError):
                    continue
        except Exception:
            pass

    return {
        "session_id": session_id or os.path.basename(filepath).replace(".jsonl", ""),
        "first_ts": first_ts,
        "last_ts": last_ts,
        "duration_min": duration_minutes,
        "model": model_name,
        "turns": turn_count,
        "total_tool_calls": total_tool_calls,
        "lg_tool_calls": lookgraph_tool_calls,
        "grep_read_calls": traditional_tool_calls,
        "bash_other_calls": bash_other_calls,
        "tool_breakdown": dict(tool_calls_by_name),
        "thinking_count": thinking_count,
        "input_tokens": total_input_tokens,
        "output_tokens": total_output_tokens,
        "total_tokens": total_input_tokens + total_output_tokens,
        "cache_read_tokens": total_cache_read,
        "cache_creation_tokens": total_cache_creation,
        "session_type": session_type,
    }


def fmt(n: int) -> str:
    if n >= 1_000_000:
        return f"{n/1_000_000:.1f}M"
    if n >= 1_000:
        return f"{n/1_000:.1f}K"
    return str(n)


def print_table(sessions: list, args):
    if args.json:
        print(json.dumps(sessions, indent=2, ensure_ascii=False))
        return

    if not sessions:
        print("没有找到会话数据")
        return

    print("\n" + "=" * 120)
    print("会话消耗统计")
    print("=" * 120)

    header = (f"{'项目':<12} {'会话ID':<38} {'日期':<12} {'轮次':>5} "
              f"{'工具调用':>7} {'LG':>4} {'Grep/Read':>9} "
              f"{'输入Token':>10} {'输出Token':>9} {'总计Token':>10} {'类型':<12}")
    print(f"\n{header}")
    print("-" * 120)

    total_input = 0
    total_output = 0
    total_turns = 0
    total_tools = 0
    total_lg = 0
    total_grep = 0

    type_labels = {
        "lookgraph": "LG",
        "non_lg": "非LG",
        "chat_only": "纯对话",
    }

    for s in sorted(sessions, key=lambda x: x["total_tokens"], reverse=True):
        date_str = s["first_ts"][:10] if s["first_ts"] else "?"
        type_label = type_labels.get(s["session_type"], s["session_type"])
        print(f"{s['project']:<12} {s['session_id'][:36]:<38} {date_str:<12} "
              f"{s['turns']:>5} {s['total_tool_calls']:>7} "
              f"{s['lg_tool_calls']:>4} {s['grep_read_calls']:>9} "
              f"{fmt(s['input_tokens']):>10} {fmt(s['output_tokens']):>9} "
              f"{fmt(s['total_tokens']):>10} {type_label:<12}")
        total_input += s["input_tokens"]
        total_output += s["output_tokens"]
        total_turns += s["turns"]
        total_tools += s["total_tool_calls"]
        total_lg += s["lg_tool_calls"]
        total_grep += s["grep_read_calls"]

    print("-" * 120)
    print(f"{'合计':<12} {len(sessions)} 个会话{'':>28} "
          f"{total_turns:>5} {total_tools:>7} "
          f"{total_lg:>4} {total_grep:>9} "
          f"{fmt(total_input):>10} {fmt(total_output):>9} "
          f"{fmt(total_input + total_output):>10}")
    print()


def main():
    parser = argparse.ArgumentParser(description="Claude Code 会话消耗统计")
    parser.add_argument("paths", nargs="+", help="项目 jsonl 文件所在目录")
    parser.add_argument("--json", action="store_true", help="JSON 格式输出")
    parser.add_argument("--days", type=int, default=180,
                        help="只统计最近 N 天的会话 (默认 180)")
    parser.add_argument("--min-turns", type=int, default=1,
                        help="最少轮次数 (默认 1)")
    args = parser.parse_args()

    all_sessions = []
    now = datetime.now(timezone.utc)
    cutoff_date = now - timedelta(days=args.days)
    cutoff_str = cutoff_date.strftime("%Y-%m-%dT%H:%M:%S")

    for path in args.paths:
        if not os.path.isdir(path):
            print(f"跳过不存在的目录: {path}", file=sys.stderr)
            continue

        project_label = os.path.basename(path.rstrip("/"))
        jsonl_files = [f for f in os.listdir(path) if f.endswith(".jsonl")]

        for filename in jsonl_files:
            filepath = os.path.join(path, filename)
            try:
                session = analyze_session(filepath)
                if session is None:
                    continue
                if session["turns"] < args.min_turns:
                    continue
                if session["first_ts"] and session["first_ts"] < cutoff_str:
                    continue
                session["project"] = project_label
                all_sessions.append(session)
            except Exception as e:
                print(f"解析失败 {filename}: {e}", file=sys.stderr)

    print_table(all_sessions, args)


if __name__ == "__main__":
    main()
