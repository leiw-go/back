#!/usr/bin/env python3
"""Command line entry point for JoinQuant operations.

The Java backend invokes this script as a child process and communicates
through a single JSON payload on stdin. The script writes a single JSON
response on stdout and exits.

Invocation contract (one shot, JSON in/out):

    echo '{"action": "ping"}' | python3 jq_cli.py
    echo '{"action": "auth", "username": "...", "password": "..."}' \\
        | python3 jq_cli.py

Actions:
    ping                          -> {"ok": true, "mode": "...", "ts": ...}
    auth                          -> authenticate, return {"ok": true, ...}
    get_account_info              -> {"username", "cash", "positionsValue", ...}
    get_positions                 -> [Position, ...]
    get_orders                    -> [Order, ...]
    get_trades                    -> [Trade, ...]
    get_securities                -> [Security, ...]
    get_price                     -> [Bar, ...]
    submit_strategy               -> {"runId", "status", ...}
    get_run_status                -> {"runId", "status", ...}
    stop_strategy                 -> {"runId", "status": "STOPPED"}
    get_metrics                   -> BacktestMetrics

The default transport is jqdatasdk (local). To use a remote JoinQuant
service, set the ``JQ_REMOTE_URL`` environment variable. To force remote
mode, also set ``JQ_MODE=REMOTE``.
"""

from __future__ import annotations

import json
import logging
import os
import sys
import traceback
from typing import Any, Dict

# Allow `python quant/scripts/jq_cli.py` from the backend project root
_THIS = os.path.dirname(os.path.abspath(__file__))
_QUANT_DIR = os.path.dirname(_THIS)
if _QUANT_DIR not in sys.path:
    sys.path.insert(0, _QUANT_DIR)

from joinquant.client import (  # noqa: E402
    JoinQuantClient,
    JoinQuantError,
    to_json,
)

logging.basicConfig(
    level=os.environ.get("JQ_LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    stream=sys.stderr,
)
logger = logging.getLogger("jq_cli")


def _build_client() -> JoinQuantClient:
    mode = os.environ.get("JQ_MODE", "AUTO")
    return JoinQuantClient(
        mode=mode,
        base_url=os.environ.get("JQ_REMOTE_URL", "http://127.0.0.1:5000"),
        token=os.environ.get("JQ_REMOTE_TOKEN", ""),
        timeout=int(os.environ.get("JQ_TIMEOUT", "30")),
    )


def _read_payload() -> Dict[str, Any]:
    raw = sys.stdin.read()
    if not raw.strip():
        return {"action": "ping"}
    try:
        return json.loads(raw)
    except json.JSONDecodeError as exc:
        raise JoinQuantError(f"invalid JSON payload: {exc}", code="JQ_BAD_INPUT") from exc


def _dispatch(client: JoinQuantClient, payload: Dict[str, Any]) -> Any:
    action = payload.get("action")
    if not action:
        raise JoinQuantError("missing 'action' field", code="JQ_BAD_INPUT")

    if action == "ping":
        return client.ping()

    if action == "auth":
        username = payload.get("username") or ""
        password = payload.get("password") or ""
        return client.auth(username, password)

    if action == "get_account_info":
        return client.get_account_info().to_dict()

    if action == "get_positions":
        return [p.to_dict() for p in client.get_positions(payload.get("security"))]

    if action == "get_orders":
        return [o.to_dict() for o in client.get_orders(payload.get("security"))]

    if action == "get_trades":
        return [t.to_dict() for t in client.get_trades(payload.get("security"))]

    if action == "get_securities":
        return client.get_all_securities(
            types=payload.get("types") or ["stock"],
            date=payload.get("date"),
        )

    if action == "get_price":
        return client.get_price(
            security=payload.get("security") or "",
            start_date=payload.get("startDate") or "",
            end_date=payload.get("endDate") or "",
            frequency=payload.get("frequency") or "daily",
            fields=payload.get("fields"),
        )

    if action == "submit_strategy":
        return client.submit_strategy(
            code=payload.get("code") or "",
            parameters=payload.get("parameters") or {},
            run_type=payload.get("runType") or "BACKTEST",
            start_date=payload.get("startDate"),
            end_date=payload.get("endDate"),
            initial_capital=float(payload.get("initialCapital") or 100000.0),
            frequency=payload.get("frequency") or "day",
            benchmark=payload.get("benchmark") or "000300.XSHG",
        )

    if action == "get_run_status":
        return client.get_run_status(payload.get("runId") or "")

    if action == "stop_strategy":
        return client.stop_strategy(payload.get("runId") or "")

    if action == "get_metrics":
        return client.get_metrics(payload.get("runId") or "").to_dict()

    raise JoinQuantError(f"unknown action: {action}", code="JQ_BAD_ACTION")


def main() -> int:
    try:
        payload = _read_payload()
    except JoinQuantError as exc:
        sys.stdout.write(json.dumps({"ok": False, "code": exc.code, "message": str(exc)}))
        return 0

    client: JoinQuantClient | None = None
    try:
        client = _build_client()
        data = _dispatch(client, payload)
        sys.stdout.write(json.dumps({"ok": True, "data": data}, ensure_ascii=False))
    except JoinQuantError as exc:
        logger.warning("JoinQuant error: %s", exc)
        sys.stdout.write(json.dumps({"ok": False, "code": exc.code, "message": str(exc)}))
    except Exception as exc:  # noqa: BLE001 - we want to report anything
        logger.error("unexpected error: %s\n%s", exc, traceback.format_exc())
        sys.stdout.write(json.dumps({
            "ok": False,
            "code": "JQ_INTERNAL",
            "message": f"{type(exc).__name__}: {exc}",
        }))
    finally:
        try:
            if client is not None and getattr(client, "_local", None) is not None:
                client._local.logout()  # type: ignore[attr-defined]
        except Exception:  # noqa: BLE001
            pass
    return 0


if __name__ == "__main__":
    sys.exit(main())
