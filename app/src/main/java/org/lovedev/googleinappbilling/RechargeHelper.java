package org.lovedev.googleinappbilling;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.lovedev.googleinappbilling.util.IabHelper;
import org.lovedev.googleinappbilling.util.IabResult;
import org.lovedev.googleinappbilling.util.Inventory;
import org.lovedev.googleinappbilling.util.Purchase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Kevin
 * @data 2017/12/28
 */
public class RechargeHelper {
    private static final String TAG = "RechargeHelper";
    private static final String mItemType = "inapp";
    private static final int RC_REQUEST = 10001;
    private static final String appKey = BuildConfig.PAY_KEY;
    private IabHelper mHelper;
    private Activity mActivity;
    private String mSku;
    private GooglePayCallback mPayCallback;



    public static RechargeHelper getInstance(Activity activity) {
        return new RechargeHelper(activity);
    }

    private RechargeHelper(Activity activity) {
        mActivity = activity;
    }

    void pay(String sku, GooglePayCallback payCallback) {
        mSku = sku;
        mPayCallback = payCallback;
        initGooglePay();
    }

    private void initGooglePay() {
        //与Google的服务连接，并传入密钥验证
        //appkey为google开发者后台审核过后的publickey
        mHelper = new IabHelper(mActivity, appKey);
        mHelper.enableDebugLogging(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } else {
                    mPayCallback.onFail(result);
                }
            }
        });
    }

    /**
     * 查询商品的回调
     */
    private IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            //库存商品查询成功
            if (result.isSuccess()) {
                //输出日志，便于跟踪代码
                Log.d(TAG, "查询成功");
                //库存商品查询成功后，进行与当前sku的匹配，确保当前传入的sku在库存中是否存在
                Purchase premiumPurchase = inventory.getPurchase(mSku);
                //匹配到库存中有当前的sku，说明查询到了需要消耗的商品，执行消耗操作
                if (premiumPurchase != null) {
                    Log.d(TAG, "查询需要消耗的商品之后执行消耗操作");
                    //google商品消耗方法，并实现消耗回调
                    mHelper.consumeAsync(inventory.getPurchase(mSku), mConsumeFinishedListener);
                    //没有查询到要消耗的商品（可能已经消耗成功，或者该商品没有购买过）
                } else {
                    //那就直接执行购买操作
                    GooglePay();
                }

            }
            //select失败
            if (result.isFailure()) {
                Log.d(TAG, "查询失败");
                mPayCallback.onFail(result);
            }
        }
    };

    /**
     * 消耗回调
     */
    private IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (result.isSuccess()) {
                //当消耗成功后，purchase为消耗成功的对象，判断purchase的sku是否和当前的sku相同，确保消耗的商品正确
                //其实这行代码也是一句废话，个人觉得还是比较一下比较好
                if (purchase.getSku().equals(mSku)) {
                    Log.d(TAG, "消耗成功");
                    //因为google明确要求，需要先消耗后购买管理商品，所以当消耗成功后，发起购买
                    //购买方法
                    GooglePay();
                }
            } else {
                Log.d(TAG, "消耗失败");
                mPayCallback.onFail(result);
            }
        }
    };

    /**
     * 自定义购买方法
     */
    private void GooglePay() {

        //sku  当前商品的内购ID

        //inapp  因为google支付和iOS支付一样，都是属于应用内支付，所以这里要传inapp

        //RC_REQUEST  这个是作为支付请求代码传

        //mPurchaseFinishedListener  购买回调

        //payload作为透传参数，我这里传的是订单号。
        //因为订单号不能唯一，所以使用当前时间生成订单号
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        String payload = BuildConfig.APPLICATION_ID + sdf.format(new Date());

        //支付方法
        mHelper.launchPurchaseFlow(mActivity, mSku, mItemType, RC_REQUEST, mPurchaseFinishedListener, payload);
    }

    /**
     * 购买回调，需要在onActivityResult中调用handleActivityResult方法
     */
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "购买回调");
            //失败方法
            if(!result.isSuccess()){
                Log.d(TAG, "购买失败");
            }else {
                Log.d(TAG, "购买成功");
                //支付成功之后如果需要重复购买的话，需要进行消耗商品
                //（这里的消耗和查询的消耗是一样的方法，之所以调用两次消耗方法，是因为害怕如果因为网络延迟在在某一步没有消耗，所以做的一个加固消耗操作）
                if(purchase.getSku().equals(mSku)){
                    Log.d(TAG, "支付成功之后如果需要消耗商品");
                    //执行下号方法
                    mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
                        @Override
                        public void onConsumeFinished(Purchase purchase1, IabResult result1) {
                            if (result1.isSuccess()) {
                                mPayCallback.onSuccess(purchase1);
                            } else {
                                mPayCallback.onFail(result1);
                            }
                        }
                    });
                }

            }
        }
    };


    void handleActivityResult(int requestCode, int resultCode, Intent data) {
        mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    void onDestroy() {
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }
}
