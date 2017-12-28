package org.lovedev.googleinappbilling;

import org.lovedev.googleinappbilling.util.IabResult;
import org.lovedev.googleinappbilling.util.Purchase;

public interface GooglePayCallback {
    void onSuccess(Purchase purchase);
    void onFail(IabResult result);
}