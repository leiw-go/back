"""
策略示例: 双均线趋势跟踪
====================================

该文件展示了一个最简的聚宽策略骨架, 兼容 jqdatasdk + 聚宽回测环境。
管理平台的用户可以直接拷贝此模板, 修改后粘贴到策略编辑器中。
"""


def initialize(context):
    """initialize 会在回测/模拟开始时调用一次, 用于设置参数与全局状态."""
    # 选股范围: 沪深 300 成分股
    context.stock_pool = get_index_stocks("000300.XSHG")
    # 调仓频率
    run_daily(market_open, time="open")
    # 策略参数
    context.short_window = int(g.params.get("shortWindow", 5))
    context.long_window = int(g.params.get("longWindow", 20))


def market_open(context):
    """每日开盘时执行的交易逻辑."""
    for stock in context.stock_pool:
        prices = attribute_history(stock, context.long_window + 1, "1d", ["close"])
        if prices is None or len(prices) < context.long_window:
            continue

        short_ma = prices["close"][-context.short_window:].mean()
        long_ma = prices["close"][-context.long_window:].mean()

        current_position = context.portfolio.positions.get(stock)
        if short_ma > long_ma and (current_position is None or current_position.amount == 0):
            order_value(stock, context.portfolio.cash / max(len(context.stock_pool), 1))
        elif short_ma < long_ma and current_position is not None and current_position.amount > 0:
            order_target(stock, 0)
