/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.email;

/**
 * 邮件引擎的业务场景.
 * <p>
 * {@code hint} 决定改写后的**结构与行动指向**（先说什么、后说什么、结尾要客户做什么）。
 *
 * @since 2026-11-17
 */
public enum CrmEmailEngineScene {

    /** 询盘回复. */
    INQUIRY_REPLY("inquiry_reply", "询盘回复",
            "客户询盘后的首次回复。先确认已收到并致谢，再逐条回应客户的关切，"
                    + "结尾给出明确的下一步（如索取规格、安排样品或报价时间）。"),

    /** 报价 / 议价. */
    QUOTATION("quotation", "报价与议价",
            "报价或价格谈判。先给结论（价格 / 交期 / 起订量），再说明依据与报价有效期，"
                    + "结尾请对方确认并说明回复时限。"),

    /** 订单确认. */
    ORDER_CONFIRM("order_confirm", "订单确认",
            "订单确认。复述订单要素（型号 / 数量 / 金额 / 交期 / 贸易术语 / 付款方式），"
                    + "请对方确认无误，并说明单据与定金要求。"),

    /** 交货通知. */
    SHIPMENT("shipment", "交货通知",
            "交货通知。给出单号 / 船期 / 目的港 / 预计到港时间，附上需要对方准备或确认的单证清单。"),

    /** 催款. */
    PAYMENT("payment", "催款",
            "催款。明确金额、到期日与付款方式，给出银行信息占位，语气坚定但不生硬，"
                    + "结尾请求对方在期限内完成付款。"),

    /** 客诉处理. */
    COMPLAINT("complaint", "客诉处理",
            "客诉处理。先共情并致歉，再说明已采取的措施与时间点，最后给出补偿或后续动作，"
                    + "不要辩解、不要推责。"),

    /** 展会邀约. */
    EXHIBITION("exhibition", "展会邀约",
            "展会邀约。说明展会名称 / 时间 / 展位与产品亮点，邀请对方安排会面，并给出联系方式。"),

    /** 跟进问候. */
    FOLLOWUP("followup", "跟进问候",
            "跟进问候。简要回顾上次沟通的内容，给出一个新的价值点（新品 / 价格 / 库存），"
                    + "试探对方下一步意向，不强推。");

    /** 场景码（前端传参 / 落库 template_type 字段）. */
    private final String code;

    /** 中文标签（前端展示）. */
    private final String label;

    /** 写进 prompt 的结构约定. */
    private final String hint;

    /**
     * 构造一个场景枚举.
     *
     * @param code  场景码
     * @param label 中文标签
     * @param hint  prompt 里的结构约定
     */
    CrmEmailEngineScene(String code, String label, String hint) {
        this.code = code;
        this.label = label;
        this.hint = hint;
    }

    /**
     * 按场景码取枚举；取不到时回落到询盘回复.
     *
     * @param code 场景码
     * @return 场景枚举
     */
    public static CrmEmailEngineScene of(String code) {
        if (code != null) {
            for (CrmEmailEngineScene scene : values()) {
                if (scene.code.equalsIgnoreCase(code.trim())) {
                    return scene;
                }
            }
        }
        return INQUIRY_REPLY;
    }

    /**
     * 取场景码.
     *
     * @return 场景码
     */
    public String getCode() {
        return code;
    }

    /**
     * 取中文标签.
     *
     * @return 中文标签
     */
    public String getLabel() {
        return label;
    }

    /**
     * 取 prompt 里的结构约定.
     *
     * @return 结构约定
     */
    public String getHint() {
        return hint;
    }
}
