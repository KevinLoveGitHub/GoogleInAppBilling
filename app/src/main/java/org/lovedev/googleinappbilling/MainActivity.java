package org.lovedev.googleinappbilling;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.lovedev.googleinappbilling.util.IabResult;
import org.lovedev.googleinappbilling.util.Purchase;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    String sku = "1200";
    static final int RC_REQUEST = 10001;
    private RechargeHelper mRechargeHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRechargeHelper = RechargeHelper.getInstance(this);

        findViewById(R.id.pay_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRechargeHelper.pay(sku, new GooglePayCallback() {
                    @Override
                    public void onSuccess(Purchase purchase) {
                        Log.i(TAG, "充值成功：" + purchase.toString());
                    }

                    @Override
                    public void onFail(IabResult result) {
                        Log.i(TAG, "充值失败：" + result.getMessage());
                    }
                });
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mRechargeHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRechargeHelper.onDestroy();
    }
}
