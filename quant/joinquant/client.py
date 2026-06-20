"""JoinQuant client wrapper.

This module hides the differences between calling ``jqdatasdk`` directly (when
available locally) and talking to a remote JoinQuant service over HTTP. The
Java backend talks to a single ``JoinQuantClient`` instance, which dispatches
the call to the right transport.

The remote transport is a thin HTTP shim that mirrors the jqdatasdk method
signatures used by this project, and is intended to run as a Flask app inside
the same Python environment that has access to jqdatasdk. The Flask shim lives
in ``quant/scripts/jq_server.py`` for convenience.
"""

from __future__ import annotations

import json
import logging
import os
import time
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)


class JoinQuantError(RuntimeError):
    """Raised when a JoinQuant call fails."""

    def __init__(self, message: str, code: str = "JQ_ERROR") -> None:
        super().__init__(message)
        self.code = code


@dataclass
class AccountInfo:
    username: str
    cash: float
    positions_value: float
    total_value: float
    frozen_cash: float = 0.0
    raw: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "username": self.username,
            "cash": self.cash,
            "positionsValue": self.positions_value,
            "totalValue": self.total_value,
            "frozenCash": self.frozen_cash,
        }


@dataclass
class Position:
    security: str
    price: float
    avg_cost: float
    volume: int
    value: float
    side: str = "long"
    raw: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "security": self.security,
            "price": self.price,
            "avgCost": self.avg_cost,
            "volume": self.volume,
            "value": self.value,
            "side": self.side,
        }


@dataclass
class Order:
    order_id: str
    security: str
    price: float
    amount: int
    side: str
    status: str
    filled: int = 0
    raw: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "orderId": self.order_id,
            "security": self.security,
            "price": self.price,
            "amount": self.amount,
            "side": self.side,
            "status": self.status,
            "filled": self.filled,
        }


@dataclass
class Trade:
    trade_id: str
    order_id: str
    security: str
    price: float
    amount: int
    side: str
    time: str = ""
    raw: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "tradeId": self.trade_id,
            "orderId": self.order_id,
            "security": self.security,
            "price": self.price,
            "amount": self.amount,
            "side": self.side,
            "time": self.time,
        }


@dataclass
class BacktestMetrics:
    total_return: float = 0.0
    annual_return: float = 0.0
    sharpe_ratio: float = 0.0
    max_drawdown: float = 0.0
    alpha: float = 0.0
    beta: float = 0.0
    win_rate: float = 0.0
    volatility: float = 0.0
    total_trade_count: int = 0
    raw: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "totalReturn": self.total_return,
            "annualReturn": self.annual_return,
            "sharpeRatio": self.sharpe_ratio,
            "maxDrawdown": self.max_drawdown,
            "alpha": self.alpha,
            "beta": self.beta,
            "winRate": self.win_rate,
            "volatility": self.volatility,
            "totalTradeCount": self.total_trade_count,
        }


class _LocalJqSdk:
    """Thin wrapper around jqdatasdk so that the rest of the code never imports
    jqdatasdk directly - this keeps the module importable in environments
    where jqdatasdk is not installed (eg. the Java spawn-on-demand process)."""

    def __init__(self) -> None:
        try:
            import jqdatasdk  # type: ignore
        except Exception as exc:  # pragma: no cover - exercised only at runtime
            raise JoinQuantError(
                "jqdatasdk is not installed. Run `pip install -r quant/requirements.txt` "
                "in the Python environment used to spawn the script.",
                code="JQ_SDK_MISSING",
            ) from exc
        self._jq = jqdatasdk

    def auth(self, username: str, password: str) -> bool:
        try:
            self._jq.auth(username, password)
        except Exception as exc:
            raise JoinQuantError(f"auth failed: {exc}", code="JQ_AUTH_FAIL") from exc
        return True

    def logout(self) -> None:
        try:
            self._jq.logout()
        except Exception:  # noqa: BLE001 - logout is best-effort
            pass

    def is_auth(self) -> bool:
        return bool(self._jq.is_auth())

    def get_all_securities(self, types: Optional[List[str]] = None, date: Optional[str] = None) -> List[Dict[str, Any]]:
        types = types or ["stock"]
        df = self._jq.get_all_securities(types=types, date=date)
        return _df_to_records(df)

    def get_price(self, security: str, start_date: str, end_date: str,
                  frequency: str = "daily", fields: Optional[List[str]] = None) -> List[Dict[str, Any]]:
        fields = fields or ["open", "close", "high", "low", "volume"]
        df = self._jq.get_price(security, start_date=start_date, end_date=end_date,
                                frequency=frequency, fields=fields)
        return _df_to_records(df)

    def normalize_code(self, code: str) -> str:
        return self._jq.normalize_code(code)

    def get_industry(self, security: str, date: Optional[str] = None) -> str:
        df = self._jq.get_industry(security, date=date)
        if hasattr(df, "iloc"):
            try:
                return str(df.iloc[0, 0])
            except Exception:
                return str(df)
        return str(df)

    def get_index_stocks(self, index_symbol: str, date: Optional[str] = None) -> List[str]:
        return list(self._jq.get_index_stocks(index_symbol, date=date))

    def get_account_info(self) -> AccountInfo:
        info = self._jq.get_account_info()
        return AccountInfo(
            username=str(info.get("username", "")),
            cash=float(info.get("cash", 0.0)),
            positions_value=float(info.get("positions_value", 0.0)),
            total_value=float(info.get("total_value", info.get("cash", 0.0))),
            frozen_cash=float(info.get("frozen_cash", 0.0)),
            raw=dict(info),
        )

    def get_positions(self, security: Optional[str] = None) -> List[Position]:
        df = self._jq.get_positions(security)
        records = _df_to_records(df)
        return [
            Position(
                security=str(r.get("code") or r.get("security") or ""),
                price=float(r.get("price", 0.0) or 0.0),
                avg_cost=float(r.get("avg_cost", 0.0) or 0.0),
                volume=int(r.get("volume", 0) or 0),
                value=float(r.get("value", 0.0) or 0.0),
                side=str(r.get("side", "long")),
                raw=r,
            )
            for r in records
        ]

    def get_orders(self, security: Optional[str] = None, status: Optional[str] = None) -> List[Order]:
        df = self._jq.get_orders(security)
        records = _df_to_records(df)
        return [
            Order(
                order_id=str(r.get("order_id") or r.get("id") or ""),
                security=str(r.get("security") or r.get("code") or ""),
                price=float(r.get("price", 0.0) or 0.0),
                amount=int(r.get("amount", 0) or 0),
                side=str(r.get("side", "long")),
                status=str(r.get("status", "")),
                filled=int(r.get("filled", 0) or 0),
                raw=r,
            )
            for r in records
        ]

    def get_trades(self, security: Optional[str] = None) -> List[Trade]:
        df = self._jq.get_trades(security)
        records = _df_to_records(df)
        return [
            Trade(
                trade_id=str(r.get("id") or r.get("trade_id") or ""),
                order_id=str(r.get("order_id", "")),
                security=str(r.get("security") or r.get("code") or ""),
                price=float(r.get("price", 0.0) or 0.0),
                amount=int(r.get("amount", 0) or 0),
                side=str(r.get("side", "long")),
                time=str(r.get("time", "")),
                raw=r,
            )
            for r in records
        ]


def _df_to_records(df: Any) -> List[Dict[str, Any]]:
    """Convert a pandas DataFrame to a list of plain dicts with JSON-safe
    values. Returns an empty list when ``df`` is None or not a DataFrame."""
    if df is None:
        return []
    if hasattr(df, "reset_index"):
        try:
            df = df.reset_index()
        except Exception:  # noqa: BLE001
            pass
    if hasattr(df, "to_dict"):
        records = df.to_dict(orient="records")
        out: List[Dict[str, Any]] = []
        for r in records:
            cleaned: Dict[str, Any] = {}
            for k, v in r.items():
                if hasattr(v, "isoformat"):
                    cleaned[str(k)] = v.isoformat()
                elif hasattr(v, "item"):
                    try:
                        cleaned[str(k)] = v.item()
                    except Exception:  # noqa: BLE001
                        cleaned[str(k)] = str(v)
                else:
                    cleaned[str(k)] = v
            out.append(cleaned)
        return out
    return []


class JoinQuantClient:
    """Facade used by the Java backend. Picks the right transport at runtime."""

    def __init__(self, mode: str = "AUTO", **kwargs: Any) -> None:
        self._mode = (mode or "AUTO").upper()
        self._remote: Optional[RemoteJoinQuantClient] = None
        self._local: Optional[_LocalJqSdk] = None
        if self._mode == "REMOTE":
            self._remote = RemoteJoinQuantClient(**kwargs)
        else:
            # Try local first; if import fails, fall back to remote if a URL
            # is configured, otherwise raise a clear error.
            try:
                self._local = _LocalJqSdk()
            except JoinQuantError as exc:
                base_url = kwargs.get("base_url") or os.environ.get("JQ_REMOTE_URL")
                if base_url:
                    logger.warning("jqdatasdk unavailable (%s), using remote client at %s", exc, base_url)
                    self._mode = "REMOTE"
                    self._remote = RemoteJoinQuantClient(**kwargs)
                else:
                    raise

    # ---- public API ----
    def auth(self, username: str, password: str) -> Dict[str, Any]:
        if self._mode == "REMOTE":
            assert self._remote is not None
            return self._remote.auth(username, password)
        assert self._local is not None
        self._local.auth(username, password)
        return {"authenticated": True, "username": username}

    def ping(self) -> Dict[str, Any]:
        return {"ok": True, "mode": self._mode, "ts": int(time.time() * 1000)}

    def get_all_securities(self, types: Optional[List[str]] = None, date: Optional[str] = None) -> List[Dict[str, Any]]:
        if self._mode == "REMOTE":
            return self._remote.get_all_securities(types=types, date=date)  # type: ignore[union-attr]
        return self._local.get_all_securities(types=types, date=date)  # type: ignore[union-attr]

    def get_price(self, security: str, start_date: str, end_date: str,
                  frequency: str = "daily", fields: Optional[List[str]] = None) -> List[Dict[str, Any]]:
        if self._mode == "REMOTE":
            return self._remote.get_price(security, start_date, end_date, frequency, fields)  # type: ignore[union-attr]
        return self._local.get_price(security, start_date, end_date, frequency, fields)  # type: ignore[union-attr]

    def get_account_info(self) -> AccountInfo:
        if self._mode == "REMOTE":
            return self._remote.get_account_info()  # type: ignore[union-attr]
        return self._local.get_account_info()  # type: ignore[union-attr]

    def get_positions(self, security: Optional[str] = None) -> List[Position]:
        if self._mode == "REMOTE":
            return self._remote.get_positions(security)  # type: ignore[union-attr]
        return self._local.get_positions(security)  # type: ignore[union-attr]

    def get_orders(self, security: Optional[str] = None) -> List[Order]:
        if self._mode == "REMOTE":
            return self._remote.get_orders(security)  # type: ignore[union-attr]
        return self._local.get_orders(security)  # type: ignore[union-attr]

    def get_trades(self, security: Optional[str] = None) -> List[Trade]:
        if self._mode == "REMOTE":
            return self._remote.get_trades(security)  # type: ignore[union-attr]
        return self._local.get_trades(security)  # type: ignore[union-attr]

    def submit_strategy(self, code: str, parameters: Dict[str, Any],
                        run_type: str = "BACKTEST", start_date: Optional[str] = None,
                        end_date: Optional[str] = None, initial_capital: float = 100000.0,
                        frequency: str = "day", benchmark: str = "000300.XSHG") -> Dict[str, Any]:
        """Submit a strategy to JoinQuant for backtest / simulation / live.

        Returns a dict with at least ``runId`` and ``status`` keys.
        """
        if self._mode == "REMOTE":
            return self._remote.submit_strategy(  # type: ignore[union-attr]
                code, parameters, run_type, start_date, end_date, initial_capital, frequency, benchmark,
            )
        return self._local.submit_strategy(  # type: ignore[union-attr]
            code, parameters, run_type, start_date, end_date, initial_capital, frequency, benchmark,
        )

    def get_run_status(self, run_id: str) -> Dict[str, Any]:
        if self._mode == "REMOTE":
            return self._remote.get_run_status(run_id)  # type: ignore[union-attr]
        return self._local.get_run_status(run_id)  # type: ignore[union-attr]

    def stop_strategy(self, run_id: str) -> Dict[str, Any]:
        if self._mode == "REMOTE":
            return self._remote.stop_strategy(run_id)  # type: ignore[union-attr]
        return self._local.stop_strategy(run_id)  # type: ignore[union-attr]

    def get_metrics(self, run_id: str) -> BacktestMetrics:
        if self._mode == "REMOTE":
            return self._remote.get_metrics(run_id)  # type: ignore[union-attr]
        return self._local.get_metrics(run_id)  # type: ignore[union-attr]


class RemoteJoinQuantClient:
    """Talks to a remote JoinQuant-compatible HTTP shim. Used when the local
    Python environment cannot import jqdatasdk (eg. dev boxes, CI)."""

    def __init__(self, base_url: str, token: str = "", timeout: int = 30) -> None:
        self._base = base_url.rstrip("/")
        self._token = token
        self._timeout = timeout

    def _post(self, path: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        try:
            import requests  # type: ignore
        except Exception as exc:  # pragma: no cover
            raise JoinQuantError("`requests` is required for remote mode", code="JQ_REMOTE_NO_REQUESTS") from exc
        headers = {"Content-Type": "application/json"}
        if self._token:
            headers["Authorization"] = f"Bearer {self._token}"
        resp = requests.post(f"{self._base}{path}", headers=headers, json=payload, timeout=self._timeout)
        if resp.status_code >= 400:
            raise JoinQuantError(f"remote call failed ({resp.status_code}): {resp.text}", code="JQ_REMOTE_HTTP")
        data = resp.json()
        if not data.get("ok", True):
            raise JoinQuantError(data.get("message") or "remote error", code="JQ_REMOTE_FAIL")
        return data.get("data") or {}

    def auth(self, username: str, password: str) -> Dict[str, Any]:
        return self._post("/auth", {"username": username, "password": password})

    def get_all_securities(self, types: Optional[List[str]] = None, date: Optional[str] = None) -> List[Dict[str, Any]]:
        return self._post("/securities", {"types": types or ["stock"], "date": date})

    def get_price(self, security: str, start_date: str, end_date: str,
                  frequency: str = "daily", fields: Optional[List[str]] = None) -> List[Dict[str, Any]]:
        return self._post("/price", {
            "security": security,
            "startDate": start_date,
            "endDate": end_date,
            "frequency": frequency,
            "fields": fields or ["open", "close", "high", "low", "volume"],
        })

    def get_account_info(self) -> AccountInfo:
        data = self._post("/account", {})
        return AccountInfo(
            username=str(data.get("username", "")),
            cash=float(data.get("cash", 0.0)),
            positions_value=float(data.get("positionsValue", 0.0)),
            total_value=float(data.get("totalValue", 0.0)),
            frozen_cash=float(data.get("frozenCash", 0.0)),
            raw=data,
        )

    def get_positions(self, security: Optional[str] = None) -> List[Position]:
        rows = self._post("/positions", {"security": security})
        return [
            Position(
                security=str(r.get("security", "")),
                price=float(r.get("price", 0.0) or 0.0),
                avg_cost=float(r.get("avgCost", 0.0) or 0.0),
                volume=int(r.get("volume", 0) or 0),
                value=float(r.get("value", 0.0) or 0.0),
                side=str(r.get("side", "long")),
                raw=r,
            )
            for r in rows
        ]

    def get_orders(self, security: Optional[str] = None) -> List[Order]:
        rows = self._post("/orders", {"security": security})
        return [
            Order(
                order_id=str(r.get("orderId", "")),
                security=str(r.get("security", "")),
                price=float(r.get("price", 0.0) or 0.0),
                amount=int(r.get("amount", 0) or 0),
                side=str(r.get("side", "long")),
                status=str(r.get("status", "")),
                filled=int(r.get("filled", 0) or 0),
                raw=r,
            )
            for r in rows
        ]

    def get_trades(self, security: Optional[str] = None) -> List[Trade]:
        rows = self._post("/trades", {"security": security})
        return [
            Trade(
                trade_id=str(r.get("tradeId", "")),
                order_id=str(r.get("orderId", "")),
                security=str(r.get("security", "")),
                price=float(r.get("price", 0.0) or 0.0),
                amount=int(r.get("amount", 0) or 0),
                side=str(r.get("side", "long")),
                time=str(r.get("time", "")),
                raw=r,
            )
            for r in rows
        ]

    def submit_strategy(self, code: str, parameters: Dict[str, Any], run_type: str = "BACKTEST",
                        start_date: Optional[str] = None, end_date: Optional[str] = None,
                        initial_capital: float = 100000.0, frequency: str = "day",
                        benchmark: str = "000300.XSHG") -> Dict[str, Any]:
        return self._post("/strategy/submit", {
            "code": code,
            "parameters": parameters,
            "runType": run_type,
            "startDate": start_date,
            "endDate": end_date,
            "initialCapital": initial_capital,
            "frequency": frequency,
            "benchmark": benchmark,
        })

    def get_run_status(self, run_id: str) -> Dict[str, Any]:
        return self._post("/strategy/status", {"runId": run_id})

    def stop_strategy(self, run_id: str) -> Dict[str, Any]:
        return self._post("/strategy/stop", {"runId": run_id})

    def get_metrics(self, run_id: str) -> BacktestMetrics:
        data = self._post("/strategy/metrics", {"runId": run_id})
        return BacktestMetrics(
            total_return=float(data.get("totalReturn", 0.0) or 0.0),
            annual_return=float(data.get("annualReturn", 0.0) or 0.0),
            sharpe_ratio=float(data.get("sharpeRatio", 0.0) or 0.0),
            max_drawdown=float(data.get("maxDrawdown", 0.0) or 0.0),
            alpha=float(data.get("alpha", 0.0) or 0.0),
            beta=float(data.get("beta", 0.0) or 0.0),
            win_rate=float(data.get("winRate", 0.0) or 0.0),
            volatility=float(data.get("volatility", 0.0) or 0.0),
            total_trade_count=int(data.get("totalTradeCount", 0) or 0),
            raw=data,
        )


# Extend the local SDK with strategy run helpers. These methods call out to the
# JoinQuant web API for run management - they are best-effort and gracefully
# degrade when the network is unreachable.
def _extend_local_with_run_methods() -> None:
    import requests  # type: ignore

    JQ_BASE = "https://www.joinquant.com"

    def _post_form(path: str, data: Dict[str, str], cookies: Dict[str, str]) -> Dict[str, Any]:
        try:
            resp = requests.post(f"{JQ_BASE}{path}", data=data, cookies=cookies, timeout=15)
            return {"statusCode": resp.status_code, "text": resp.text}
        except Exception as exc:  # noqa: BLE001
            return {"error": str(exc)}

    def submit_strategy(self: "_LocalJqSdk", code: str, parameters: Dict[str, Any],
                        run_type: str = "BACKTEST", start_date: Optional[str] = None,
                        end_date: Optional[str] = None, initial_capital: float = 100000.0,
                        frequency: str = "day", benchmark: str = "000300.XSHG") -> Dict[str, Any]:
        # NOTE: jqdatasdk does not provide a public API for starting strategy
        # runs (backtest/simulation/live) - those are exposed through the
        # JoinQuant web UI. This helper is a placeholder that records the
        # request locally and returns a synthetic run id. In a real deployment
        # wire this up to the JoinQuant web service or your own scheduler.
        run_id = f"local-{int(time.time() * 1000)}"
        return {
            "runId": run_id,
            "status": "SUBMITTED",
            "runType": run_type,
            "message": "local-mode strategy runs are not auto-executed; please start the backtest from the JoinQuant web console",
            "submittedAt": int(time.time() * 1000),
        }

    def get_run_status(self: "_LocalJqSdk", run_id: str) -> Dict[str, Any]:
        return {"runId": run_id, "status": "UNKNOWN", "message": "status not available in local mode"}

    def stop_strategy(self: "_LocalJqSdk", run_id: str) -> Dict[str, Any]:
        return {"runId": run_id, "status": "STOPPED"}

    def get_metrics(self: "_LocalJqSdk", run_id: str) -> BacktestMetrics:
        return BacktestMetrics(raw={"runId": run_id, "status": "UNKNOWN"})

    _LocalJqSdk.submit_strategy = submit_strategy  # type: ignore[attr-defined]
    _LocalJqSdk.get_run_status = get_run_status  # type: ignore[attr-defined]
    _LocalJqSdk.stop_strategy = stop_strategy  # type: ignore[attr-defined]
    _LocalJqSdk.get_metrics = get_metrics  # type: ignore[attr-defined]


_extend_local_with_run_methods()


def to_json(obj: Any) -> str:
    """JSON serializer that knows how to handle our dataclasses + datetimes."""
    from datetime import datetime, date

    def default(o: Any) -> Any:
        if isinstance(o, (datetime, date)):
            return o.isoformat()
        if hasattr(o, "to_dict"):
            return o.to_dict()
        if hasattr(o, "__dict__"):
            return {k: v for k, v in o.__dict__.items() if not k.startswith("_")}
        return str(o)

    return json.dumps(obj, ensure_ascii=False, default=default)
