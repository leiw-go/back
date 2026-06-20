"""
joinquant - A wrapper around the JoinQuant (jqdatasdk) Python SDK.

This package exposes a high-level client that:
  * authenticates with a JoinQuant account (or a remote JoinQuant-compatible service)
  * queries securities, price data, fundamentals
  * lists / starts / stops backtests and simulations
  * reads positions / orders / trades for a given run
  * evaluates the latest metric snapshot of a strategy

The wrapper is intentionally tolerant: it will *not* crash on import when
``jqdatasdk`` is missing - instead it falls back to a HTTP-based client that
talks to a remote JoinQuant service. This keeps the Java back-end able to
spawn the script on machines where only ``requests`` is installed.
"""

from .client import JoinQuantClient, JoinQuantError, RemoteJoinQuantClient  # noqa: F401
