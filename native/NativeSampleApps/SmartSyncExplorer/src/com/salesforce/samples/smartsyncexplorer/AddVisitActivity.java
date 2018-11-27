package com.salesforce.samples.smartsyncexplorer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.target.SyncTarget;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.ui.SalesforceActivity;
import com.salesforce.samples.VisitReportLoader;
import com.salesforce.samples.smartsyncexplorer.loaders.ContactListLoader;
import com.salesforce.samples.smartsyncexplorer.objects.ContactObject;

import org.json.JSONException;
import org.json.JSONObject;

public class AddVisitActivity extends SalesforceActivity {

    EditText etPlan, etExpenses, etSubject, etDescription;
    Spinner spStatus;
    private UserAccount curAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_visit);
        etPlan = findViewById(R.id.etPlan);
        etExpenses = findViewById(R.id.etExpenses);
        etSubject = findViewById(R.id.etSubject);
        etDescription = findViewById(R.id.etDescription);
        spStatus = findViewById(R.id.spStatus);
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
        String plan ="";
        String status = "";
        String subject = "";

        final SmartStore smartStore = SmartSyncSDKManager.getInstance().getSmartStore(curAccount);
        JSONObject contact;
        try {
            contact = new JSONObject();
            contact.put(Constants.ID, "local_" + System.currentTimeMillis()
                    + Constants.EMPTY_STRING);
            final JSONObject attributes = new JSONObject();
            attributes.put(Constants.TYPE.toLowerCase(), VisitReportLoader.VISITREPORT_SOUP);
            contact.put(Constants.ATTRIBUTES, attributes);
            //contact.put(VisitReportObject.V_R_NAME, firstName);
            contact.put(VisitReportObject.V_R_EXPENSES, expenses);
            contact.put(VisitReportObject.V_R_RELATED_PLAN, plan);
            contact.put(VisitReportObject.V_R_STATUS, status);
            contact.put(VisitReportObject.V_R_SUBJECT, subject);
            contact.put(SyncTarget.LOCAL, true);
            contact.put(SyncTarget.LOCALLY_CREATED, true);
            //if (isCreate) {
                smartStore.create(ContactListLoader.CONTACT_SOUP, contact);
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
