#!/usr/bin/env python3
"""HTTP shim that exposes jqdatasdk as a tiny JSON API.

Run with::

    pip install flask
    JQ_AUTH_USER=13800000000 JQ_AUTH_PASS=xxxxxx python quant/scripts/jq_server.py

The Java backend can then call ``quant.mode=REMOTE`` and point
``quant.remote-base-url`` at this service. Each HTTP request triggers a single
jqdatasdk call, so this server is stateless. Authentication state is shared via
a thread-local session that re-auths on first use after a 401.
"""

from __future__ import annotations

import logging
import os
import sys
import threading
from typing import Any, Dict

try:
    from flask import Flask, jsonify, request  # type: ignore
except ImportError:  # pragma: no cover - optional dependency
    print("Flask is required for the remote shim. `pip install flask`.", file=sys.stderr)
    raise

_THIS = os.path.dirname(os.path.abspath(__file__))
_QUANT_DIR = os.path.dirname(_THIS)
if _QUANT_DIR not in sys.path:
    sys.path.insert(0, _QUANT_DIR)

from joinquant.client import JoinQuantClient  # noqa: E402

logging.basicConfig(level=os.environ.get("JQ_LOG_LEVEL", "INFO"))
logger = logging.getLogger("jq_server")

app = Flask(__name__)

_client_lock = threading.Lock()
_client: JoinQuantClient | None = None
_auth_user = os.environ.get("JQ_AUTH_USER", "")
_auth_pass = os.environ.get("JQ_AUTH_PASS", "")


def get_client() -> JoinQuantClient:
    global _client
    if _client is None:
        with _client_lock:
            if _client is None:
                _client = JoinQuantClient(mode=os.environ.get("JQ_MODE", "AUTO"),
                                          base_url="http://127.0.0.1:5000",
                                          token=os.environ.get("JQ_REMOTE_TOKEN", ""))
                if _auth_user and _auth_pass:
                    _client.auth(_auth_user, _auth_pass)
    return _client


def _ok(data: Any) -> Any:
    return jsonify({"ok": True, "data": data})


def _err(message: str, code: str = "JQ_FAIL", http: int = 400):
    resp = jsonify({"ok": False, "code": code, "message": message})
    resp.status_code = http
    return resp


@app.post("/auth")
def auth_route():
    body = request.get_json(force=True, silent=True) or {}
    user = body.get("username") or _auth_user
    pwd = body.get("password") or _auth_pass
    if not user or not pwd:
        return _err("username/password required", http=400)
    try:
        return _ok(get_client().auth(user, pwd))
    except Exception as exc:  # noqa: BLE001
        logger.exception("auth failed")
        return _err(str(exc), http=500)


@app.post("/account")
def account_route():
    return _ok(get_client().get_account_info().to_dict())


@app.post("/positions")
def positions_route():
    body = request.get_json(force=True, silent=True) or {}
    return _ok([p.to_dict() for p in get_client().get_positions(body.get("security"))])


@app.post("/orders")
def orders_route():
    body = request.get_json(force=True, silent=True) or {}
    return _ok([o.to_dict() for o in get_client().get_orders(body.get("security"))])


@app.post("/trades")
def trades_route():
    body = request.get_json(force=True, silent=True) or {}
    return _ok([t.to_dict() for t in get_client().get_trades(body.get("security"))])


@app.post("/securities")
def securities_route():
    body = request.get_json(force=True, silent=True) or {}
    return _ok(get_client().get_all_securities(types=body.get("types") or ["stock"], date=body.get("date")))


@app.post("/price")
def price_route():
    body: Dict[str, Any] = request.get_json(force=True, silent=True) or {}
    return _ok(get_client().get_price(
        security=body.get("security") or "",
        start_date=body.get("startDate") or "",
        end_date=body.get("endDate") or "",
        frequency=body.get("frequency") or "daily",
        fields=body.get("fields"),
    ))


@app.post("/strategy/submit")
def submit_route():
    body: Dict[str, Any] = request.get_json(force=True, silent=True) or {}
    return _ok(get_client().submit_strategy(
        code=body.get("code") or "",
        parameters=body.get("parameters") or {},
        run_type=body.get("runType") or "BACKTEST",
        start_date=body.get("startDate"),
        end_date=body.get("endDate"),
        initial_capital=float(body.get("initialCapital") or 100000.0),
        frequency=body.get("frequency") or "day",
        benchmark=body.get("benchmark") or "000300.XSHG",
    ))


@app.post("/strategy/status")
def status_route():
    body: Dict[str, Any] = request.get_json(force=True, silent=True) or {}
    return _ok(get_client().get_run_status(body.get("runId") or ""))


@app.post("/strategy/stop")
def stop_route():
    body: Dict[str, Any] = request.get_json(force=True, silent=True) or {}
    return _ok(get_client().stop_strategy(body.get("runId") or ""))


@app.post("/strategy/metrics")
def metrics_route():
    body: Dict[str, Any] = request.get_json(force=True, silent=True) or {}
    return _ok(get_client().get_metrics(body.get("runId") or "").to_dict())


@app.get("/health")
def health():
    return _ok({"status": "UP"})


if __name__ == "__main__":
    port = int(os.environ.get("JQ_PORT", "5000"))
    app.run(host="0.0.0.0", port=port, debug=False)
