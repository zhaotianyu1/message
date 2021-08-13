package com.juphoon.chatbotmaap;

import android.content.Context;

public class RcsChatbotPermission {
    // 权限申请回调
    private RcsChatbotPermissionActivity.PermissionCallback callback;
    // 需要申请的权限
    private String[] permissions;
    private Context context;

    public RcsChatbotPermission(Context context) {
        this.context = context;
    }

    public static RcsChatbotPermission with(Context context) {
        RcsChatbotPermission permisson = new RcsChatbotPermission(context);
        return permisson;
    }

    public RcsChatbotPermission permisson(String[] permissons) {
        this.permissions = permissons;
        return this;
    }

    public RcsChatbotPermission callback(RcsChatbotPermissionActivity.PermissionCallback callback) {
        this.callback = callback;
        return this;
    }

    public void request() {
        if (permissions == null || permissions.length <= 0) {
            return;
        }
        RcsChatbotPermissionActivity.request(context, permissions, callback);
    }
}
