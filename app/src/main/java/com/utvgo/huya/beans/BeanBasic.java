package com.utvgo.huya.beans;

/**
 * Created by lgh on 2018/8/13
 */

public class BeanBasic {


    /**
     * message : success
     * tipsInfo : 您已成功登记手机号码，工作人员将会在活动结束后7个工作日内电话通知中奖用户。
     * code : 1
     */

    private String message;
    private String tipsInfo;
    private String code;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTipsInfo() {
        return tipsInfo;
    }

    public void setTipsInfo(String tipsInfo) {
        this.tipsInfo = tipsInfo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}