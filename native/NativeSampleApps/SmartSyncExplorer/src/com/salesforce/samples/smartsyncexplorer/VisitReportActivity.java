package com.salesforce.samples.smartsyncexplorer;

import android.accounts.Account;
import android.app.LoaderManager;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;

import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.support.constraint.Constraints.TAG;

public class VisitReportActivity extends SalesforceActivity implements LoaderManager.LoaderCallbacks<List<VisitReportObject>> {

    private LoadCompleteReceiver loadCompleteReceiver;
    private AtomicBoolean isRegistered;
    private static final int VISIT_LOADER_ID = 1;
    private VisitReportLoader visitReportLoader;
    private static final String SYNC_CONTENT_AUTHORITY = "com.salesforce.samples.smartsyncexplorer.visitreportsyncadapter";
    private static final long SYNC_FREQUENCY_ONE_HOUR = 1 * 60;
    SmartSyncSDKManager sdkManager;
    private SmartStore smartStore;
    RecyclerView rvVisitReport;
    VisitReportAdapter mAdapter;
    private SearchView searchView;
    boolean isLoadFinished = false;
    List<VisitReportObject> vrObjects = new ArrayList<>();
    public static final int MY_PERMISSIONS_REQUEST_WRITE_CALENDAR = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_report);
        rvVisitReport = findViewById(R.id.rvVisitReports);
        getActionBar().setTitle("Visit Report");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        loadCompleteReceiver = new LoadCompleteReceiver();
        isRegistered = new AtomicBoolean(false);
        sdkManager = SmartSyncSDKManager.getInstance();
        smartStore = sdkManager.getSmartStore(sdkManager.getUserAccountManager().getCurrentUser());
        visitReportLoader = new VisitReportLoader(this, SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser());
    }

    @Override
    public void onResume(RestClient client) {
        vrObjects = new ArrayList<>();
        getLoaderManager().initLoader(VISIT_LOADER_ID, null, this);
        if (!isRegistered.get()) {
            registerReceiver(loadCompleteReceiver,
                    new IntentFilter(VisitReportLoader.LOAD_COMPLETE_INTENT_ACTION));
        }
        isRegistered.set(true);

        // Setup periodic sync
        setupPeriodicSync();
        // Sync now
        requestSync(true /* sync down only */);

        System.out.println("onresume client");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SampleDatabase  sampleDatabase = Room.databaseBuilder(getApplicationContext(),
                        SampleDatabase.class, getString(R.string.db_name)).allowMainThreadQueries().build();

//                System.out.println("sample date bse saved records are "+sampleDatabase.daoAccess().fetchAllData().get(1).getSlNo());

            }
        });


        vrObjects = visitReportLoader.loadInBackground();
        mAdapter = new VisitReportAdapter(vrObjects);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvVisitReport.setLayoutManager(mLayoutManager);
        rvVisitReport.setAdapter(mAdapter);

        getAllPlansAndSave(client);

        mAdapter.setOnItemClickListener(new VisitReportAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                VisitReportObject sObject = vrObjects.get(position);
                System.out.println("cliked object id is " + sObject.getvRName());
                Intent intent = new Intent(VisitReportActivity.this, ProductDetailsActivity.class);
                intent.putExtra("product_code", sObject.getvRName());
                System.out.println("product code " + sObject.getvRName());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(int position, View v) {

            }
        });

        final SalesforceSDKManager sdkManager = SalesforceSDKManager.getInstance();

        Account useraccount = SmartSyncSDKManager.getInstance().getUserAccountManager().buildAccount(sdkManager.getUserAccountManager().getCurrentUser());
        final UserAccount user = sdkManager.getUserAccountManager().buildUserAccount(useraccount);

        VisitReportLoader offlineOrderLoaders = new VisitReportLoader(VisitReportActivity.this, user);
        offlineOrderLoaders.syncUp();

        System.out.println("visit report loader is called ");

        if (!isLoadFinished) {
            callVisitReportApi(client);
        }


    }

    private void callVisitReportApi(final RestClient client) {
        RestRequest restRequest =
                null;
        try {
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT name,status__c,expenses__c,subject__c,related_plan__c,Description__c FROM visit_report__c");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success visit reports request" + request);
                System.out.println("success visit reports" + response);
                try {
                    smartStore.clearSoup("visitreport");
                    insertVisitReports(response.asJSONObject().getJSONArray("records"));
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        vrObjects = visitReportLoader.loadInBackground();
                        mAdapter = new VisitReportAdapter(vrObjects);
                        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                        rvVisitReport.setLayoutManager(mLayoutManager);
                        rvVisitReport.setAdapter(mAdapter);

                        getAllPlansAndSave(client);

                        mAdapter.setOnItemClickListener(new VisitReportAdapter.ClickListener() {
                            @Override
                            public void onItemClick(int position, View v) {
                                VisitReportObject sObject = vrObjects.get(position);
                                System.out.println("cliked object id is " + sObject.getvRName());
                                Intent intent = new Intent(VisitReportActivity.this, ProductDetailsActivity.class);
                                intent.putExtra("product_code", sObject.getvRName());
                                System.out.println("product code " + sObject.getvRName());
                                startActivity(intent);
                            }

                            @Override
                            public void onItemLongClick(int position, View v) {

                            }
                        });
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                exception.printStackTrace();
                System.out.println("failed " + exception.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vrObjects = visitReportLoader.loadInBackground();
                        mAdapter = new VisitReportAdapter(vrObjects);
                        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                        rvVisitReport.setLayoutManager(mLayoutManager);
                        rvVisitReport.setAdapter(mAdapter);

                        mAdapter.setOnItemClickListener(new VisitReportAdapter.ClickListener() {
                            @Override
                            public void onItemClick(int position, View v) {
                                VisitReportObject sObject = vrObjects.get(position);
                                System.out.println("cliked object id is " + sObject.getvRName());
                                Intent intent = new Intent(VisitReportActivity.this, ProductDetailsActivity.class);
                                intent.putExtra("product_code", sObject.getvRName());
                                startActivity(intent);
                            }

                            @Override
                            public void onItemLongClick(int position, View v) {

                            }
                        });
                    }
                });
            }
        });

    }

    private void getAllPlansAndSave(RestClient client) {
        RestRequest restRequest =
                null;
        try {
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT name,id FROM plan__c");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success visit plans request" + request);
                System.out.println("success visit plans" + response);
                try {
                    smartStore.clearSoup("plans");
                    insertPlans(response.asJSONObject().getJSONArray("records"));
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception exception) {
                exception.printStackTrace();
                System.out.println("failed " + exception.getMessage());

            }
        });
    }

    private void insertPlans(JSONArray records) {
        try {
            if (records != null) {
                for (int i = 0; i < records.length(); i++) {
                    if (records.get(i) != null) {
                        try {
                            smartStore.upsert("plans", records.getJSONObject(i));
                        } catch (JSONException exc) {
                            Log.e(TAG, "Error occurred while attempting to insert account. "
                                    + "Please verify validity of JSON data set.");
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error occurred while attempting to insert accounts. "
                    + "Please verify validity of JSON data set.");
        }
    }

    private void insertVisitReports(JSONArray records) {
        try {
            if (records != null) {
                for (int i = 0; i < records.length(); i++) {
                    if (records.get(i) != null) {
                        try {
                            smartStore.upsert(VisitReportLoader.VISITREPORT_SOUP, records.getJSONObject(i));
                        } catch (JSONException exc) {
                            Log.e(TAG, "Error occurred while attempting to insert account. "
                                    + "Please verify validity of JSON data set.");
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error occurred while attempting to insert accounts. "
                    + "Please verify validity of JSON data set.");
        }
    }

    @Override
    public Loader<List<VisitReportObject>> onCreateLoader(int i, Bundle bundle) {
        visitReportLoader = new VisitReportLoader(this, SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser());
        System.out.println("visit list loader " + visitReportLoader.loadInBackground());
        return visitReportLoader;
    }

    @Override
    public void onLoadFinished(Loader<List<VisitReportObject>> loader, List<VisitReportObject> visitReportObjects) {
        refreshList(visitReportObjects);
        if (visitReportObjects.size() != vrObjects.size()) {
            isLoadFinished = true;
        }
        System.out.println("load finished " + visitReportObjects);
    }

    @Override
    public void onLoaderReset(Loader<List<VisitReportObject>> loader) {
        System.out.println("load reset " + loader);
        refreshList(null);
    }

    private void refreshList(List<VisitReportObject> data) {
        // NB: We feed the data to nameFilter, and in turns it feeds the (filtered) data to listAdapter
        if (data == null) {
            System.out.println("refreshed list is called ");
            //nameFilter.setData(contactLoader.loadInBackground());
        } else {
            //nameFilter.setData(data);
            System.out.println("refreshed list is called " + data);
        }
    }

    private class LoadCompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (VisitReportLoader.LOAD_COMPLETE_INTENT_ACTION.equals(action)) {
                    refreshList();
                }
            }
        }
    }

    private void refreshList() {
        getLoaderManager().getLoader(VISIT_LOADER_ID).forceLoad();
    }


    @Override
    public void onResume() {
        super.onResume();
       visitReportLoader.syncUp();
        if (visitReportLoader!=null){
            System.out.println("on resume "+visitReportLoader.loadInBackground());
        }
    }

    @Override
    public void onPause() {
        if (isRegistered.get()) {
            unregisterReceiver(loadCompleteReceiver);
        }
        isRegistered.set(false);
    	/*getLoaderManager().destroyLoader(CONTACT_LOADER_ID);
		contactLoader = null;*/
        super.onPause();
    }

    private void setupPeriodicSync() {
        Account account = SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentAccount();
        System.out.println("periodic sync is called " + account.name);
        /*
         * Enables sync automatically for this provider. To enable almost
         * instantaneous sync when records are modified locally, a call needs
         * to be made by the content provider to notify the sync provider
         * that the underlying data set has changed. Since we don't use cursors
         * in this sample application, we simply enable periodic sync every hour.
         */
        ContentResolver.setSyncAutomatically(account, SYNC_CONTENT_AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, SYNC_CONTENT_AUTHORITY,
                Bundle.EMPTY, SYNC_FREQUENCY_ONE_HOUR);
    }

    private void requestSync(boolean syncDownOnly) {
        Account account = SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentAccount();
        Bundle extras = new Bundle();
        extras.putBoolean(VisitReportSyncAdapter.SYNC_DOWN_ONLY, syncDownOnly);
        ContentResolver.requestSync(account, SYNC_CONTENT_AUTHORITY, extras);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatementload finished
        if (id == R.id.action_search) {
            return true;
        } else if (id == R.id.action_add) {
            System.out.println("add new ");
            startActivity(new Intent(this, AddVisitActivity.class));
        } else {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_visit_report, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = new SearchView(this);
       /* searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);*/
        int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) searchView.findViewById(id);
        textView.setTextColor(Color.WHITE);
        searchItem.setActionView(searchView);
        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                mAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                mAdapter.getFilter().filter(query);
                return false;
            }
        });
        return true;
    }


}
