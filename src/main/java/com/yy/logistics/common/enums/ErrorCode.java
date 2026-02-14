package com.yy.logistics.common.enums;

public enum ErrorCode {

    SUCCESS("00000", "操作成功"),
    BAD_REQUEST("A0400", "请求参数错误"),
    VALIDATION_ERROR("A0401", "参数校验失败"),
    UNAUTHORIZED("A0301", "未认证或登录已过期"),
    FORBIDDEN("A0303", "无权限访问"),
    NOT_FOUND("A0404", "资源不存在"),
    METHOD_NOT_ALLOWED("A0405", "请求方法不支持"),
    USER_EXISTS("A0410", "用户已存在"),
    USER_NOT_FOUND("A0411", "用户不存在"),
    BAD_CREDENTIALS("A0412", "账号或密码错误"),
    ACCOUNT_DISABLED("A0413", "账号已禁用"),
    TOKEN_INVALID("A0414", "Token无效或已过期"),
    ORDER_NOT_FOUND("A0415", "订单不存在"),
    WAYBILL_NOT_FOUND("A0416", "运单不存在"),
    ORDER_STATUS_INVALID("A0417", "订单状态不允许当前操作"),
    ROLE_NOT_MATCH("A0418", "当前账号角色不匹配"),
    TASK_NOT_FOUND("A0419", "任务不存在"),
    TASK_STATUS_INVALID("A0420", "任务状态不允许当前操作"),
    ADDRESS_NOT_FOUND("A0421", "地址不存在"),
    OLD_PASSWORD_INVALID("A0422", "原密码错误"),
    BUSINESS_ERROR("B0001", "业务处理失败"),
    SYSTEM_ERROR("B5000", "系统繁忙，请稍后再试");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
