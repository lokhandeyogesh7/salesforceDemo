package com.salesforce.samples.smartsyncexplorer;

import android.support.v7.app.AppCompatActivity;
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

import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.target.SyncTarget;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.ui.SalesforceActivity;
import com.salesforce.samples.VisitReportLoader;
import com.salesforce.samples.smartsyncexplorer.loaders.ContactListLoader;
import com.salesforce.samples.smartsyncexplorer.objects.ContactObject;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AddVisitActivity extends SalesforceActivity {

    EditText etExpenses, etSubject, etDescription;
    Spinner spStatus;
    SearchableSpinner etPlan;
    private UserAccount curAccount;
    private SmartStore smartStore;
    private SyncManager syncMgr;
    String selectedPlan, selectedStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_visit);
        etPlan = findViewById(R.id.etPlan);
        etExpenses = findViewById(R.id.etExpenses);
        etSubject = findViewById(R.id.etSubject);
        etDescription = findViewById(R.id.etDescription);
        spStatus = findViewById(R.id.spStatus);

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
                arrPlans.add(plans.get(i).getPLAN_NAME());
            }
        } catch (JSONException e) {
            Log.e(getLocalClassName(), "JSONException occurred while parsing", e);
        } catch (SmartSqlHelper.SmartSqlException e) {
            Log.e(getLocalClassName(), "SmartSqlException occurred while fetching data", e);
        }

        System.out.println("plans Are " + plans);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, arrPlans);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        etPlan.setAdapter(adapter);
        etPlan.setTitle("Select Plan");
        etPlan.setPositiveButton("OK");

        List<String> arrStatus = new ArrayList<>();
        arrStatus.add("--None--");
        arrStatus.add("Submitted for Approval");
        arrStatus.add("Approved");
        arrStatus.add("Rejected");

        ArrayAdapter<String> adapter1 =
                new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, arrStatus);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(adapter1);

        etPlan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("hsdjgd " + adapterView.getItemAtPosition(i));
                selectedPlan = String.valueOf(adapterView.getItemAtPosition(i));
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
        curAccount = SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser();
        //getLoaderManager().initLoader(CONTACT_DETAIL_LOADER_ID, null, this).forceLoad();
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
                contact.put(Constants.ID, "local_" + System.currentTimeMillis()
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
                //if (isCreate) {
                smartStore.create(VisitReportLoader.VISITREPORT_SOUP, contact);
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
}
