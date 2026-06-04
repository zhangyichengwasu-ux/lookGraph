#!/usr/bin/env python3
"""供应商平台接口请求脚本 — 对账单 & 开票记录

用法:
  python api_client.py <command> [args...]

Commands (对账单):
  ts-detail <id>
  ts-query [--page 1] [--size 10] [--status STATUS]
  ts-pending-invoice [--supplier-id ID]
  ts-export-pending [--supplier-id ID]
  ts-export-detail <id>
  ts-export [--id ID ...]
  ts-export-product [--id ID ...]
  ts-export-product-by-id <id>
  ts-generate

Commands (开票记录):
  inv-pre-generate --ids 1,2,3
  inv-generate --ids 1,2,3 --invoice-no NO --amount 100.00
  inv-query [--page 1] [--size 10]
  inv-detail <id>
  inv-by-settlement <id>
  inv-upload <file_path>
  inv-upload-verify --record-id ID --urls url1,url2
  inv-download <id>
  inv-export [--page 1] [--size 10]
  inv-export-details [--page 1] [--size 10]
  inv-export-detail <id>
  inv-by-record <invoiceRecordId>
"""
import argparse
import json
import sys
import os
from pathlib import Path
import urllib.request
import urllib.error

BASE_URL = os.environ.get("API_BASE_URL", "http://localhost:8080")
TOKEN = os.environ.get("API_TOKEN", "")


def headers(content_type="application/json"):
    h = {"Content-Type": content_type}
    if TOKEN:
        h["Authorization"] = f"Bearer {TOKEN}"
    return h


def get(path):
    req = urllib.request.Request(f"{BASE_URL}{path}", headers=headers())
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)


def post(path, body=None):
    data = json.dumps(body or {}).encode()
    req = urllib.request.Request(f"{BASE_URL}{path}", data=data, headers=headers(), method="POST")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)


def post_file(path, file_path):
    import mimetypes, uuid
    boundary = uuid.uuid4().hex
    fname = Path(file_path).name
    with open(file_path, "rb") as f:
        file_data = f.read()
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{fname}"\r\n'
        f"Content-Type: {mimetypes.guess_type(fname)[0] or 'application/octet-stream'}\r\n\r\n"
    ).encode() + file_data + f"\r\n--{boundary}--\r\n".encode()
    h = headers(f"multipart/form-data; boundary={boundary}")
    req = urllib.request.Request(f"{BASE_URL}{path}", data=body, headers=h, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)


def download(path, out_file):
    req = urllib.request.Request(f"{BASE_URL}{path}", headers=headers())
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            Path(out_file).write_bytes(r.read())
        print(f"Saved: {out_file}")
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)


def download_post(path, body, out_file):
    data = json.dumps(body).encode()
    req = urllib.request.Request(f"{BASE_URL}{path}", data=data, headers=headers(), method="POST")
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            Path(out_file).write_bytes(r.read())
        print(f"Saved: {out_file}")
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)


def pp(data):
    print(json.dumps(data, ensure_ascii=False, indent=2))


# ── 对账单 ────────────────────────────────────────────────────────────────────

def ts_detail(args):
    pp(get(f"/transaction-statement/detail/{args.id}"))

def ts_query(args):
    pp(post("/transaction-statement/query", {"page": args.page, "size": args.size, "status": args.status}))

def ts_pending_invoice(args):
    body = {}
    if args.supplier_id:
        body["supplierId"] = args.supplier_id
    pp(post("/transaction-statement/pending-invoice", body))

def ts_export_pending(args):
    body = {}
    if args.supplier_id:
        body["supplierId"] = args.supplier_id
    download_post("/transaction-statement/pending-invoice/export", body, "待开票记录.xlsx")

def ts_export_detail(args):
    download(f"/transaction-statement/detail/{args.id}/export", f"对账单明细_{args.id}.xlsx")

def ts_export(args):
    body = {"ids": args.id} if args.id else {}
    download_post("/transaction-statement/export", body, "对账单.xlsx")

def ts_export_product(args):
    body = {"ids": args.id} if args.id else {}
    download_post("/transaction-statement/export/product-dimension", body, "对账单商品明细.xlsx")

def ts_export_product_by_id(args):
    download(f"/transaction-statement/detail/{args.id}/export/product-dimension", f"对账单商品明细_{args.id}.xlsx")

def ts_generate(args):
    pp(post("/transaction-statement/generate"))


# ── 开票记录 ──────────────────────────────────────────────────────────────────

def inv_pre_generate(args):
    pp(post("/invoice-record/pre-generate", {"transactionStatementIds": [int(i) for i in args.ids.split(",")]}))

def inv_generate(args):
    body = {
        "transactionStatementIds": [int(i) for i in args.ids.split(",")],
        "invoiceNo": args.invoice_no,
        "amount": float(args.amount),
    }
    pp(post("/invoice-record/generate", body))

def inv_query(args):
    pp(post("/invoice-record/query", {"page": args.page, "size": args.size}))

def inv_detail(args):
    pp(get(f"/invoice-record/detail/{args.id}"))

def inv_by_settlement(args):
    pp(get(f"/invoice-record/query-by-settlement/{args.id}"))

def inv_upload(args):
    pp(post_file("/invoice-record/upload", args.file_path))

def inv_upload_verify(args):
    body = {"invoiceRecordId": int(args.record_id), "fileUrls": args.urls.split(",")}
    pp(post("/invoice-record/upload-and-verify", body))

def inv_download(args):
    download(f"/invoice-record/download/{args.id}", f"发票记录_{args.id}.xlsx")

def inv_export(args):
    download_post("/invoice-record/query/export", {"page": args.page, "size": args.size}, "开票记录.xlsx")

def inv_export_details(args):
    download_post("/invoice-record/export-details", {"page": args.page, "size": args.size}, "开票记录明细.xlsx")

def inv_export_detail(args):
    download(f"/invoice-record/export-detail/{args.id}", f"开票记录明细_{args.id}.xlsx")

def inv_by_record(args):
    pp(get(f"/invoice-record/by-invoice-record/{args.invoice_record_id}"))


# ── CLI ───────────────────────────────────────────────────────────────────────

def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)

    # 对账单
    s = sub.add_parser("ts-detail"); s.add_argument("id", type=int)
    s = sub.add_parser("ts-query"); s.add_argument("--page", type=int, default=1); s.add_argument("--size", type=int, default=10); s.add_argument("--status")
    s = sub.add_parser("ts-pending-invoice"); s.add_argument("--supplier-id", type=int)
    s = sub.add_parser("ts-export-pending"); s.add_argument("--supplier-id", type=int)
    s = sub.add_parser("ts-export-detail"); s.add_argument("id", type=int)
    s = sub.add_parser("ts-export"); s.add_argument("--id", type=int, nargs="+")
    s = sub.add_parser("ts-export-product"); s.add_argument("--id", type=int, nargs="+")
    s = sub.add_parser("ts-export-product-by-id"); s.add_argument("id", type=int)
    sub.add_parser("ts-generate")

    # 开票记录
    s = sub.add_parser("inv-pre-generate"); s.add_argument("--ids", required=True)
    s = sub.add_parser("inv-generate"); s.add_argument("--ids", required=True); s.add_argument("--invoice-no", required=True); s.add_argument("--amount", required=True)
    s = sub.add_parser("inv-query"); s.add_argument("--page", type=int, default=1); s.add_argument("--size", type=int, default=10)
    s = sub.add_parser("inv-detail"); s.add_argument("id", type=int)
    s = sub.add_parser("inv-by-settlement"); s.add_argument("id", type=int)
    s = sub.add_parser("inv-upload"); s.add_argument("file_path")
    s = sub.add_parser("inv-upload-verify"); s.add_argument("--record-id", required=True); s.add_argument("--urls", required=True)
    s = sub.add_parser("inv-download"); s.add_argument("id", type=int)
    s = sub.add_parser("inv-export"); s.add_argument("--page", type=int, default=1); s.add_argument("--size", type=int, default=10)
    s = sub.add_parser("inv-export-details"); s.add_argument("--page", type=int, default=1); s.add_argument("--size", type=int, default=10)
    s = sub.add_parser("inv-export-detail"); s.add_argument("id", type=int)
    s = sub.add_parser("inv-by-record"); s.add_argument("invoice_record_id", type=int)

    CMDS = {
        "ts-detail": ts_detail, "ts-query": ts_query,
        "ts-pending-invoice": ts_pending_invoice, "ts-export-pending": ts_export_pending,
        "ts-export-detail": ts_export_detail, "ts-export": ts_export,
        "ts-export-product": ts_export_product, "ts-export-product-by-id": ts_export_product_by_id,
        "ts-generate": ts_generate,
        "inv-pre-generate": inv_pre_generate, "inv-generate": inv_generate,
        "inv-query": inv_query, "inv-detail": inv_detail,
        "inv-by-settlement": inv_by_settlement, "inv-upload": inv_upload,
        "inv-upload-verify": inv_upload_verify, "inv-download": inv_download,
        "inv-export": inv_export, "inv-export-details": inv_export_details,
        "inv-export-detail": inv_export_detail, "inv-by-record": inv_by_record,
    }

    args = p.parse_args()
    CMDS[args.cmd](args)


if __name__ == "__main__":
    main()
