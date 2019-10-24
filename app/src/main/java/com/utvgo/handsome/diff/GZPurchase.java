package com.utvgo.handsome.diff;

import android.content.Context;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.model.Response;

import com.utvgo.handsome.config.AppConfig;
import com.utvgo.handsome.bean.BeanVipAuth;
import com.utvgo.handsome.interfaces.CommonCallback;
import com.utvgo.handsome.interfaces.JsonCallback;
import com.utvgo.handsome.utils.XLog;

import com.utvgo.huya.activity.QWebViewActivity;

public class GZPurchase extends IPurchase {

    @Override
    public void pay(final Context context, final CommonCallback callback) {
        String url = DiffConfig.authHost+  "TVMaiShiOrderController/order.utvgo?"+
                "keyNo=" + DiffConfig.getCA(context) +"&vipCode=" + AppConfig.VipCode + "&contentMid=&backUrl=http://" ;

        XLog.d("payurl",url);
        QWebViewActivity.navigateUrl(context, url);
    }

    @Override
    public void auth(final Context context, final AuthCallback authCallback) {

        String url = DiffConfig.authHost + "order/TVUtvgoVipAuthorizationController/checkVipAuthorization.utvgo?keyNo="
                + DiffConfig.getCA(context) + "&regionCode=" + DiffConfig.getRegionCode(context) + "&vipCode=" + AppConfig.VipCode +
                "&contentMid=" + "";

        OkGo.<BeanVipAuth>get(url).cacheMode(CacheMode.NO_CACHE).tag(context).execute(new JsonCallback<BeanVipAuth>() {
            String message = "";
            @Override
            public void onSuccess(Response<BeanVipAuth> response) {
                try{
                    BeanVipAuth vipAuth = response.body();
                    if(vipAuth.getStatus() == 1)
                    {
                        setPurchased();
                    }
                    else
                    {
                        String msg = vipAuth.getExtra().getMsg();
                        if(msg != null)
                        {
                            message = msg;
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish() {
                if(authCallback != null){
                    authCallback.onFinished(message);
                }
            }
        });


    }

    @Override
    public void refreshOrderStatus(final Context context, final AuthCallback callback) {
        auth(context, callback);
    }
}