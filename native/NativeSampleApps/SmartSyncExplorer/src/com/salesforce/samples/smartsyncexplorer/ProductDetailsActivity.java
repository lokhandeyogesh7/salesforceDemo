package com.salesforce.samples.smartsyncexplorer;

import android.os.Bundle;
import android.util.Log;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.ui.SalesforceActivity;
import com.salesforce.samples.smartsyncexplorer.loaders.ContactListLoader;
import com.salesforce.samples.smartsyncexplorer.objects.ContactObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.salesforce.androidsdk.smartstore.store.AlterSoupLongOperation.TAG;

public class ProductDetailsActivity extends SalesforceActivity {

    private SmartStore smartStore;
    private SyncManager syncMgr;
    String productCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        SmartSyncSDKManager sdkManager = SmartSyncSDKManager.getInstance();
        smartStore = sdkManager.getSmartStore(sdkManager.getUserAccountManager().getCurrentUser());
        syncMgr = SyncManager.getInstance(sdkManager.getUserAccountManager().getCurrentUser());
        productCode = getIntent().getStringExtra("product_code");
        System.out.println("product code "+productCode);
    }

    @Override
    public void onResume() {
        super.onResume();
     /*   final QuerySpec querySpec = QuerySpec.buildAllQuerySpec("priceBook",
                PriceBookObject.PRODUCT_DETAIL_NAME, QuerySpec.Order.ascending, 100000);
        JSONArray results = null;
        List<PriceBookObject> products = new ArrayList<>();
       *//* try {
            JSONObject contact = smartStore.retrieve("priceBook",
                    smartStore.lookupSoupEntryId("priceBook",
                            Constants.ID, productCode)).getJSONObject(0);
            System.out.println("sinmgle object is "+contact);

        } catch (JSONException e) {
            e.printStackTrace();
        }*//*
        try {
            results = smartStore.query(querySpec, 0);
            for (int i = 0; i < results.length(); i++) {
                products.add(new PriceBookObject(results.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SmartSqlHelper.SmartSqlException e) {
            Log.e(TAG, "SmartSqlException occurred while fetching data", e);
        }
        System.out.println("products in do products    product details>>>>>> /n" + products);*/
        final QuerySpec querySpec1 = QuerySpec.buildExactQuerySpec(
               "priceBook", PriceBookObject.PRODUCT_DETAIL_CODE, productCode, null, null, 1);
        JSONArray results1 = null;
        PriceBookObject sObject = null;
        try {
            results1 = smartStore.query(querySpec1, 0);
            if (results1 != null) {
                sObject = new PriceBookObject(results1.getJSONObject(0));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SmartSqlHelper.SmartSqlException e) {
            Log.e(TAG, "SmartSqlException occurred while fetching data", e);
        }
        System.out.println("fhgffjfjfgggfgf "+sObject.getProductDetailName());
        System.out.println("fhgffjfjfgggfgf  results1 "+results1);

    }

    @Override
    public void onResume(RestClient client) {

    }
}
