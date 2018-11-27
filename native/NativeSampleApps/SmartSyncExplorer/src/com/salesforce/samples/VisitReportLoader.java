package com.salesforce.samples;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.util.SyncState;
import com.salesforce.samples.smartsyncexplorer.ProductObject;
import com.salesforce.samples.smartsyncexplorer.VisitReportObject;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class VisitReportLoader extends AsyncTaskLoader<List<VisitReportObject>> {

    public static final String VISITREPORT_SOUP = "visitreport";
    public static final Integer LIMIT = 10000;
    public static final String LOAD_COMPLETE_INTENT_ACTION = "com.salesforce.samples.smartsyncexplorer.loaders.LIST_LOAD_COMPLETE";
    private static final String TAG = "ProductListLoader";
    public static final String SYNC_DOWN_NAME = "syncDownProducts";
    public static final String SYNC_UP_NAME = "syncUpProducts";

    private SmartStore smartStore;
    private SyncManager syncMgr;

    /**
     * Parameterized constructor.
     *
     * @param context Context.
     * @param account User account.
     */
    public VisitReportLoader(Context context, UserAccount account) {
        super(context);
        SmartSyncSDKManager sdkManager = SmartSyncSDKManager.getInstance();
        smartStore = sdkManager.getSmartStore(account);
        syncMgr = SyncManager.getInstance(account);
        // Setup schema if needed
        sdkManager.setupUserStoreFromDefaultConfig();
        // Setup syncs if needed
        sdkManager.setupUserSyncsFromDefaultConfig();
    }


    @Override
    public List<VisitReportObject> loadInBackground() {
        if (!smartStore.hasSoup(VISITREPORT_SOUP)) {
            return null;
        }
        final QuerySpec querySpec = QuerySpec.buildAllQuerySpec(VISITREPORT_SOUP,
                VisitReportObject.V_R_NAME, QuerySpec.Order.ascending, LIMIT);
        JSONArray results = null;
        List<VisitReportObject> products = new ArrayList<VisitReportObject>();
        try {
            results = smartStore.query(querySpec, 0);
            for (int i = 0; i < results.length(); i++) {
                products.add(new VisitReportObject(results.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SmartSqlHelper.SmartSqlException e) {
            Log.e(TAG, "SmartSqlException occurred while fetching data", e);
        }
        System.out.println("products in do products    sasdjgsadjgj>>>>>> /n" + products);
        return products;
    }

    /**
     * Pushes local changes up to the server.
     */
    public synchronized void syncUp() {
        try {
            syncMgr.reSync(SYNC_UP_NAME /* see usersyncs.json */, new SyncManager.SyncUpdateCallback() {

                @Override
                public void onUpdate(SyncState sync) {
                    if (SyncState.Status.DONE.equals(sync.getStatus())) {
                        syncDown();
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SyncManager.SmartSyncException e) {
            Log.e(TAG, "SmartSyncException occurred while attempting to sync up", e);
        }
    }

    /**
     * Pulls the latest records from the server.
     */
    public synchronized void syncDown() {
        try {
            syncMgr.reSync(SYNC_DOWN_NAME /* see usersyncs.json */, new SyncManager.SyncUpdateCallback() {

                @Override
                public void onUpdate(SyncState sync) {
                    if (SyncState.Status.DONE.equals(sync.getStatus())) {
                        fireLoadCompleteIntent();
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SyncManager.SmartSyncException e) {
            Log.e(TAG, "SmartSyncException occurred while attempting to sync down", e);
        }
    }

    /**
     * Fires an intent notifying a registered receiver that fresh data is
     * available. This is for the special case where the data change has
     * been triggered by a background sync, even though the consuming
     * activity is in the foreground. Loaders don't trigger callbacks in
     * the activity unless the load has been triggered using a LoaderManager.
     */
    private void fireLoadCompleteIntent() {
        final Intent intent = new Intent(LOAD_COMPLETE_INTENT_ACTION);
        SalesforceSDKManager.getInstance().getAppContext().sendBroadcast(intent);
    }
}

