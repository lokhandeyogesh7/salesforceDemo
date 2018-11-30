package com.salesforce.samples.smartsyncexplorer;

import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.smartstore.store.IndexSpec;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.target.SoqlSyncDownTarget;
import com.salesforce.androidsdk.smartsync.target.SyncDownTarget;
import com.salesforce.androidsdk.smartsync.target.SyncTarget;
import com.salesforce.androidsdk.smartsync.target.SyncUpTarget;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.smartsync.util.SOQLBuilder;
import com.salesforce.androidsdk.smartsync.util.SyncOptions;
import com.salesforce.androidsdk.smartsync.util.SyncState;
import com.salesforce.androidsdk.ui.SalesforceActivity;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AddVisitActivity extends SalesforceActivity {

    EditText etExpenses, etSubject, etDescription;
    Spinner spStatus;
    SearchableSpinner etPlan;
    private UserAccount curAccount;
    private SmartStore smartStore;
    private SyncManager syncMgr;
    String selectedPlan, selectedStatus;
    private long accountSyncId = -1;
    RestClient restClient;
    SampleDatabase sampleDatabase;
    NetworkChangeReceiver iNetworkChangeReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_visit);
        etPlan = findViewById(R.id.etPlan);
        etExpenses = findViewById(R.id.etExpenses);
        etSubject = findViewById(R.id.etSubject);
        etDescription = findViewById(R.id.etDescription);
        spStatus = findViewById(R.id.spStatus);

        getActionBar().setTitle("Add Visit Report");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        sampleDatabase = Room.databaseBuilder(getApplicationContext(),
                SampleDatabase.class, getString(R.string.db_name)).allowMainThreadQueries().build();
        iNetworkChangeReceiver = new NetworkChangeReceiver();

        SmartSyncSDKManager sdkManager = SmartSyncSDKManager.getInstance();
        smartStore = sdkManager.getSmartStore(sdkManager.getUserAccountManager().getCurrentUser());
        syncMgr = SyncManager.getInstance(sdkManager.getUserAccountManager().getCurrentUser());

        final QuerySpec querySpec = QuerySpec.buildAllQuerySpec("plans",
                PlanObject.PLAN_NAME, QuerySpec.Order.ascending, 1000000);
        JSONArray results = null;
        List<PlanObject> plans = new ArrayList<PlanObject>();
        List<String> arrPlans = new ArrayList<>();
        try {
            results = smartStore.query(querySpec, 0);
            for (int i = 0; i < results.length(); i++) {
                plans.add(new PlanObject(results.getJSONObject(i)));
                arrPlans.add(plans.get(i).getPlanId());
            }
        } catch (JSONException e) {
            Log.e(getLocalClassName(), "JSONException occurred while parsing", e);
        } catch (SmartSqlHelper.SmartSqlException e) {
            Log.e(getLocalClassName(), "SmartSqlException occurred while fetching data", e);
        }

        System.out.println("plans Are " + plans);

        final ArrayAdapter<PlanObject> adapter =
                new ArrayAdapter<PlanObject>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, plans);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        etPlan.setAdapter(adapter);
        etPlan.setTitle("Select Plan");
        etPlan.setPositiveButton("OK");

        List<String> arrStatus = new ArrayList<>();
        arrStatus.add("--None--");
        arrStatus.add("Submitted for Approval");
        arrStatus.add("Approved");
        arrStatus.add("Rejected");

        final ArrayAdapter<String> adapter1 =
                new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, arrStatus);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(adapter1);

        etPlan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                PlanObject planObject =adapter.getItem(i);
                selectedPlan = planObject.getPlanId();
                System.out.println("hsdjgd " + selectedPlan);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedStatus = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onResume(RestClient client) {
        restClient = client;
        curAccount = SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser();
        //getLoaderManager().initLoader(CONTACT_DETAIL_LOADER_ID, null, this).forceLoad();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add_visit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            saveVisit();
        } else {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveVisit() {

        String expenses = etExpenses.getText().toString();
        String subject = etSubject.getText().toString();
        String description = etDescription.getText().toString();
        if (TextUtils.isEmpty(expenses) || selectedPlan.contains("select") || selectedStatus.contains("None") || TextUtils.isEmpty(subject)) {
            Toast.makeText(this, "Please provide all mandatory fields", Toast.LENGTH_LONG).show();
        } else {

            final SmartStore smartStore = SmartSyncSDKManager.getInstance().getSmartStore(curAccount);
            JSONObject contact;
            try {
                contact = new JSONObject();
                contact.put(Constants.VR_ID, "local_" + System.currentTimeMillis()
                        + Constants.EMPTY_STRING);
                final JSONObject attributes = new JSONObject();
                attributes.put(Constants.TYPE.toLowerCase(), VisitReportLoader.VISITREPORT_SOUP);
                contact.put(Constants.ATTRIBUTES, attributes);
                contact.put(VisitReportObject.V_R_DESCRIPTION, description);
                contact.put(VisitReportObject.V_R_EXPENSES, expenses);
                contact.put(VisitReportObject.V_R_RELATED_PLAN, selectedPlan);
                contact.put(VisitReportObject.V_R_STATUS, selectedStatus);
                contact.put(VisitReportObject.V_R_SUBJECT, subject);
                contact.put(SyncTarget.LOCAL, true);
                contact.put(SyncTarget.LOCALLY_CREATED, true);
                contact.put(SyncTarget.LOCALLY_UPDATED, false);
                contact.put(SyncTarget.LOCALLY_DELETED, false);
                //if (isCreate) {
                smartStore.create(VisitReportLoader.VISITREPORT_SOUP, contact);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(VisitReportObject.V_R_DESCRIPTION, description);
                jsonObject.put(VisitReportObject.V_R_EXPENSES, expenses);
                jsonObject.put(VisitReportObject.V_R_RELATED_PLAN, selectedPlan);
                jsonObject.put(VisitReportObject.V_R_STATUS, selectedStatus);
                jsonObject.put(VisitReportObject.V_R_SUBJECT, subject);
                HashMap yourHashMap = new Gson().fromJson(jsonObject.toString(), HashMap.class);

                if (iNetworkChangeReceiver.isOnline(this)) {
                    updateServer(yourHashMap);
                } else {
                    ApiObjects apiObjects = new ApiObjects();
                    apiObjects.setFieldList(jsonObject.toString());
                    apiObjects.setObjectType("visit_report__c");
                    sampleDatabase.daoAccess().insertOnlySingleRecord(apiObjects);
                }

               /* VisitReportLoader visitReportLoader = new VisitReportLoader(AddVisitActivity.this,curAccount);
                visitReportLoader.syncUp();*/
            /*} else {
                smartStore.upsert(ContactListLoader.CONTACT_SOUP, contact);
            }*/
                Toast.makeText(this, "Save successful!", Toast.LENGTH_LONG).show();
                finish();
            } catch (JSONException e) {
                Log.e(getLocalClassName(), "JSONException occurred while parsing", e);
            }
        }
    }


    private void updateServer(HashMap<String, Object> fields) {

        final RestRequest restRequest;
        try {
            restRequest = RestRequest.getRequestForCreate(getString(R.string.api_version), "visit_report__c", fields);
        } catch (Exception e) {
            //MainActivity.showError(this, e);
            e.printStackTrace();
            return;
        }

        restClient.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                //System.out.println("result of api number  is "+slNo);
                System.out.println("result of request is" + request);
                System.out.println("result of response is" + result);
            }

            @Override
            public void onError(Exception e) {
                //MainActivity.showError(DetailActivity.this, e);
                e.printStackTrace();
            }
        });
    }


    /**
     * Pushes local changes up to the server.
     */
    public synchronized void syncUp() {

        final SyncUpTarget target = new SyncUpTarget();

        final SyncOptions options = SyncOptions.optionsForSyncUp(Arrays.asList(VisitReportObject.V_R_DESCRIPTION, VisitReportObject.V_R_EXPENSES, VisitReportObject.V_R_NAME, VisitReportObject.V_R_RELATED_PLAN, VisitReportObject.V_R_STATUS, VisitReportObject.V_R_SUBJECT), SyncState.MergeMode.OVERWRITE);
        try {
            syncMgr.syncUp(target, options, "visitreport", new SyncManager.SyncUpdateCallback() {

                @Override
                public void onUpdate(SyncState sync) {
                    if (SyncState.Status.DONE.equals(sync.getStatus())) {
                        System.out.println("sync done");
                        syncDownOfflineOrder();

                    } else if (SyncState.Status.FAILED.equals(sync.getStatus())) {
                        // DebugLog.d("Case syncUp FAILED");
                        System.out.println("sync failed");

                    } else if (SyncState.Status.RUNNING.equals(sync.getStatus())) {
                        //DebugLog.d("Case syncUp RUNNING");
                        System.out.println("sync running");

                    } else if (SyncState.Status.NEW.equals(sync.getStatus())) {
                        //DebugLog.d("Case syncUp NEW");
                        System.out.println("sync new");
                    }
                }
            });
        } catch (JSONException e) {
            // DebugLog.d("JSONException occurred while parsing"+e);
            System.out.println("JSONException occurred while parsing" + e);
        } catch (SyncManager.SmartSyncException e) {
            System.out.println("JSONException occurred while SmartSyncException" + e);
        }
    }

    final IndexSpec[] indexSpecs = {
            new IndexSpec(VisitReportObject.V_R_DESCRIPTION, SmartStore.Type.string),
            new IndexSpec(VisitReportObject.V_R_EXPENSES, SmartStore.Type.string),
            new IndexSpec(VisitReportObject.V_R_NAME, SmartStore.Type.string),
            new IndexSpec(VisitReportObject.V_R_RELATED_PLAN, SmartStore.Type.string),
            new IndexSpec(VisitReportObject.V_R_STATUS, SmartStore.Type.string),
            new IndexSpec(VisitReportObject.V_R_SUBJECT, SmartStore.Type.string),
            new IndexSpec(SyncTarget.LOCAL, SmartStore.Type.string)
    };

    /**
     * Pulls the latest records from the server.
     */
    public synchronized void syncDownOfflineOrder() {

        smartStore.registerSoup("visitreport", indexSpecs);

        final SyncManager.SyncUpdateCallback callback = new SyncManager.SyncUpdateCallback() {

            @Override
            public void onUpdate(SyncState sync) {
                if (SyncState.Status.DONE.equals(sync.getStatus())) {
                    // fireLoadCompleteIntent();
                    //DebugLog.d("syncDownOfflineOrder done");
                }
            }
        };
        try {
            if (accountSyncId == -1) {

                final SyncOptions options = SyncOptions.optionsForSyncDown(SyncState.MergeMode.LEAVE_IF_CHANGED);

                final String soqlQuery = SOQLBuilder.getInstanceWithFields(VisitReportObject.V_R_DESCRIPTION, VisitReportObject.V_R_EXPENSES, VisitReportObject.V_R_NAME, VisitReportObject.V_R_RELATED_PLAN, VisitReportObject.V_R_STATUS, VisitReportObject.V_R_SUBJECT).from("visit_report__c").limit(100000).build();

                System.out.println("soQl Query " + soqlQuery);

                final SyncDownTarget target = new SoqlSyncDownTarget(soqlQuery);
                final SyncState sync = syncMgr.syncDown(target, options, "visitreport", callback);
                System.out.println("sync is " + sync.asJSON());
                accountSyncId = sync.getId();
            } else {
                syncMgr.reSync(accountSyncId, callback);
            }
        } catch (JSONException e) {
            //DebugLog.e("JSONException occurred while parsing"+e);
            System.out.println("JSONException occurred while parsing" + e);
        } catch (SyncManager.SmartSyncException e) {
            System.out.println("SmartSyncException occurred while attempting to sync down" + e);
        }
    }

}
