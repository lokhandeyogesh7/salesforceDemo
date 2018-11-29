/*
 * Copyright (c) 2014-present, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.samples.smartsyncexplorer.ui;

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
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartstore.ui.SmartStoreInspectorActivity;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.ui.SalesforceListActivity;
import com.salesforce.samples.smartsyncexplorer.ApiObjects;
import com.salesforce.samples.smartsyncexplorer.NetworkChangeReceiver;
import com.salesforce.samples.smartsyncexplorer.ProductsActivity;
import com.salesforce.samples.smartsyncexplorer.R;
import com.salesforce.samples.smartsyncexplorer.SampleDatabase;
import com.salesforce.samples.smartsyncexplorer.VisitReportActivity;
import com.salesforce.samples.smartsyncexplorer.loaders.ContactListLoader;
import com.salesforce.samples.smartsyncexplorer.objects.ContactObject;
import com.salesforce.samples.smartsyncexplorer.sync.ContactSyncAdapter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main activity.
 *
 * @author bhariharan
 */
public class MainActivity extends SalesforceListActivity implements
        OnQueryTextListener, OnCloseListener, NetworkChangeReceiver.ConnectivityReceiverListener,
        LoaderManager.LoaderCallbacks<List<ContactObject>> {

    public static final String OBJECT_ID_KEY = "object_id";
    public static final String OBJECT_TITLE_KEY = "object_title";
    public static final String OBJECT_NAME_KEY = "object_name";
    private static final String SYNC_CONTENT_AUTHORITY = "com.salesforce.samples.smartsyncexplorer.sync.contactsyncadapter";
    //private static final long SYNC_FREQUENCY_ONE_HOUR = 1 * 60 * 60;
    private static final long SYNC_FREQUENCY_ONE_HOUR = 1 * 60;
    private static final int CONTACT_LOADER_ID = 1;
    private static final int CONTACT_COLORS[] = {
            Color.rgb(26, 188, 156),
            Color.rgb(46, 204, 113),
            Color.rgb(52, 152, 219),
            Color.rgb(155, 89, 182),
            Color.rgb(52, 73, 94),
            Color.rgb(22, 160, 133),
            Color.rgb(39, 174, 96),
            Color.rgb(41, 128, 185),
            Color.rgb(142, 68, 173),
            Color.rgb(44, 62, 80),
            Color.rgb(241, 196, 15),
            Color.rgb(230, 126, 34),
            Color.rgb(231, 76, 60),
            Color.rgb(149, 165, 166),
            Color.rgb(243, 156, 18),
            Color.rgb(211, 84, 0),
            Color.rgb(192, 57, 43),
            Color.rgb(189, 195, 199),
            Color.rgb(127, 140, 141)
    };

    private SearchView searchView;
    private ContactListAdapter listAdapter;
    private NameFieldFilter nameFilter;
    private LogoutDialogFragment logoutConfirmationDialog;
    private ContactListLoader contactLoader;
    private LoadCompleteReceiver loadCompleteReceiver;
    private AtomicBoolean isRegistered;

    NetworkChangeReceiver connectionBroadcast;
    RestClient restClient;
    SampleDatabase sampleDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getActionBar().setTitle(R.string.main_activity_title);
//        contactLoader = new ContactListLoader(this, SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser());

        loadCompleteReceiver = new LoadCompleteReceiver();
        connectionBroadcast = new NetworkChangeReceiver(this);
        isRegistered = new AtomicBoolean(false);

    }

    @Override
    public void onLogoutComplete() {
        super.onLogoutComplete();
        // If refresh token is revoked - ClientManager does a logout that doesn't finish top activity activity or show login
        if (!isChild()) {
            recreate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(connectionBroadcast,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

       /* connectivityReceiverListener = new NetworkChangeReceiver.ConnectivityReceiverListener() {
            @Override
            public void onNetworkConnectionChanged(boolean isConnected) {
                if (isConnected) {
                    Toast.makeText(MainActivity.this, "connected to internet", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "connected to internet lost", Toast.LENGTH_SHORT).show();
                }
            }
        };*/


        listAdapter = new ContactListAdapter(this, R.layout.list_item);
        System.out.println("current user resume is " + SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser());
        //contactLoader = new ContactListLoader(this, SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser());
        if (contactLoader != null) {
            listAdapter.setData(contactLoader.loadInBackground());
            getListView().setAdapter(listAdapter);
            nameFilter = new NameFieldFilter(listAdapter, contactLoader.loadInBackground());
            logoutConfirmationDialog = new LogoutDialogFragment();
        }
    }

    @Override
    public void onResume(RestClient client) {
        restClient = client;
        // Loader initialization and receiver registration
        getLoaderManager().initLoader(CONTACT_LOADER_ID, null, this);
        if (!isRegistered.get()) {
            registerReceiver(loadCompleteReceiver,
                    new IntentFilter(ContactListLoader.LOAD_COMPLETE_INTENT_ACTION));
        }
        isRegistered.set(true);

        if (connectionBroadcast.isOnline(this)) {
            sampleDatabase = Room.databaseBuilder(getApplicationContext(),
                    SampleDatabase.class, getString(R.string.db_name)).allowMainThreadQueries().build();
            List<ApiObjects> apiObjects = sampleDatabase.daoAccess().fetchAllData();
            for (int i = 0; i < apiObjects.size(); i++) {
                System.out.println("api objects are " + apiObjects.get(i).getFieldList());
                HashMap yourHashMap = new Gson().fromJson(apiObjects.get(i).getFieldList(), HashMap.class);
                updateServer(yourHashMap, apiObjects.get(i).getObjectType(), apiObjects.get(i).getSlNo(),client);
            }

        }


        // Setup periodic sync
        setupPeriodicSync();

        // Sync now
        requestSync(true /* sync down only */);


        //try basis

       /* RestRequest restRequest =
                null;
        try {
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT name,id FROM product2");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success " + request);
                System.out.println("success " + response);
            }

            @Override
            public void onError(Exception exception) {
                exception.printStackTrace();
                System.out.println("failed " + exception.getMessage());
            }
        });*/

    }

    /**
     * Setup periodic sync
     */
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

    /**
     * Request a sync
     *
     * @param syncDownOnly if true, only a sync down is done, if false a sync up followed by a sync down is done
     */
    private void requestSync(boolean syncDownOnly) {
        Account account = SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentAccount();
        Bundle extras = new Bundle();
        extras.putBoolean(ContactSyncAdapter.SYNC_DOWN_ONLY, syncDownOnly);
        ContentResolver.requestSync(account, SYNC_CONTENT_AUTHORITY, extras);
    }

    @Override
    public void onPause() {
        if (isRegistered.get()) {
            unregisterReceiver(loadCompleteReceiver);
        }
        isRegistered.set(false);
        if (connectionBroadcast != null) {
            unregisterReceiver(connectionBroadcast);
        }
    	/*getLoaderManager().destroyLoader(CONTACT_LOADER_ID);
		contactLoader = null;*/
        super.onPause();
    }

    @Override
    public void onDestroy() {
        loadCompleteReceiver = null;
        connectionBroadcast = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar_menu, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = new SearchView(this);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        searchItem.setActionView(searchView);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
           /* case R.id.action_refresh:
                Toast.makeText(this, "Synchronizing...", Toast.LENGTH_SHORT).show();
                requestSync(false *//* sync up + sync down *//*);
                return true;*/
            case R.id.action_logout:
                logoutConfirmationDialog.show(getFragmentManager(), "LogoutDialog");
                return true;
            /*case R.id.action_switch_user:
                launchAccountSwitcherActivity();
                return true;*/
           /* case R.id.action_inspect_db:
                launchSmartStoreInspectorActivity();
                return true;*/
            case R.id.action_add:
                launchDetailActivity(Constants.EMPTY_STRING, "New Contact",
                        Constants.EMPTY_STRING);
                return true;
            case R.id.action_products:
                startActivity(new Intent(this, ProductsActivity.class));
                return true;
            case R.id.action_visit_report:
                startActivity(new Intent(this, VisitReportActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchSmartStoreInspectorActivity() {
        this.startActivity(SmartStoreInspectorActivity.getIntent(this, false, null));
    }

    private void launchAccountSwitcherActivity() {
        final Intent i = new Intent(this, SalesforceSDKManager.getInstance().getAccountSwitcherActivityClass());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(i);
    }

    @Override
    public Loader<List<ContactObject>> onCreateLoader(int id, Bundle args) {
        contactLoader = new ContactListLoader(this, SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser());
        System.out.println("contact list loader " + contactLoader.loadInBackground());
        return contactLoader;
    }

    @Override
    public void onLoaderReset(Loader<List<ContactObject>> loader) {
        refreshList(null);
    }

    @Override
    public void onLoadFinished(Loader<List<ContactObject>> loader,
                               List<ContactObject> data) {
        refreshList(data);
    }

    @Override
    public boolean onClose() {
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        nameFilter.setFilterTerm(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        nameFilter.setFilterTerm(newText);
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final ContactObject sObject = listAdapter.getItem(position);
        System.out.println("contact object id is " + sObject.getObjectId());
        launchDetailActivity(sObject.getObjectId(), sObject.getName(),
                sObject.getTitle());
    }

    private void refreshList() {
        getLoaderManager().getLoader(CONTACT_LOADER_ID).forceLoad();
    }

    private void refreshList(List<ContactObject> data) {
        // NB: We feed the data to nameFilter, and in turns it feeds the (filtered) data to listAdapter
        if (data == null) {
            System.out.println("refreshed list is called ");
            nameFilter.setData(contactLoader.loadInBackground());
        } else {
            nameFilter.setData(data);
        }
    }

    private void launchDetailActivity(String objId, String objName,
                                      String objTitle) {
        final Intent detailIntent = new Intent(this, DetailActivity.class);
        detailIntent.addCategory(Intent.CATEGORY_DEFAULT);
        detailIntent.putExtra(OBJECT_ID_KEY, objId);
        detailIntent.putExtra(OBJECT_TITLE_KEY, objTitle);
        detailIntent.putExtra(OBJECT_NAME_KEY, objName);
        startActivity(detailIntent);
    }

    private void filterList(String filterTerm) {
        nameFilter.filter(filterTerm);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        System.out.println("isComnnecteed is " + isConnected);
        if (isConnected) {
            Toast.makeText(MainActivity.this, "connected to internet", Toast.LENGTH_SHORT).show();
           /* SampleDatabase sampleDatabase = Room.databaseBuilder(getApplicationContext(),
                    SampleDatabase.class, getString(R.string.db_name)).allowMainThreadQueries().build();
            List<ApiObjects> apiObjects = sampleDatabase.daoAccess().fetchAllData();
            for (int i = 0; i < apiObjects.size(); i++) {
                System.out.println("api objects are " + apiObjects.get(i).getFieldList());
                HashMap yourHashMap = new Gson().fromJson(apiObjects.get(i).getFieldList(), HashMap.class);
                updateServer(yourHashMap, apiObjects.get(i).getObjectType(), apiObjects.get(i).getSlNo());
            }
*/
        } else {
            Toast.makeText(MainActivity.this, "connection to internet lost", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateServer(HashMap<String, Object> fields, String objectType, final int slNo, RestClient client) {

        final RestRequest restRequest;
        try {
            restRequest = RestRequest.getRequestForCreate(getString(R.string.api_version), objectType, fields);
        } catch (Exception e) {
            //MainActivity.showError(this, e);
            e.printStackTrace();
            return;
        }
        //RestClient restClient = new RestClient();

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                System.out.println("result of api number  is " + slNo);
                System.out.println("result of request is" + request);
                System.out.println("result of response is" + result+">>> "+slNo);
                if (result.isSuccess()){
                    //sampleDatabase.daoAccess().deleteRecord();
                }
            }

            @Override
            public void onError(Exception e) {
                //MainActivity.showError(DetailActivity.this, e);
                e.printStackTrace();
            }
        });
    }

    /**
     * Custom array adapter to supply data to the list view.
     *
     * @author bhariharan
     */
    private static class ContactListAdapter extends ArrayAdapter<ContactObject> {

        private int listItemLayoutId;
        private List<ContactObject> sObjects;
        private String filterTerm;

        /**
         * Parameterized constructor.
         *
         * @param context          Context.
         * @param listItemLayoutId List item view resource ID.
         */
        public ContactListAdapter(Context context, int listItemLayoutId) {
            super(context, listItemLayoutId);
            this.listItemLayoutId = listItemLayoutId;
        }

        /**
         * Sets data to this adapter.
         *
         * @param data Data.
         */
        public void setData(List<ContactObject> data) {
            clear();
            sObjects = data;
            if (data != null) {
                addAll(data);
                notifyDataSetChanged();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(listItemLayoutId, null);
            }
            if (sObjects != null) {
                final ContactObject sObject = sObjects.get(position);
                if (sObject != null) {
                    final TextView objName = (TextView) convertView.findViewById(R.id.obj_name);
                    final TextView objType = (TextView) convertView.findViewById(R.id.obj_type);
                    final TextView objImage = (TextView) convertView.findViewById(R.id.obj_image);
                    if (objName != null) {
                        if (sObject.getName().contains("null")) {
                            objName.setText(sObject.getName().replace("null", ""));
                        } else {
                            objName.setText(sObject.getName());
                        }
                    }
                    if (objType != null) {
                        objType.setText(sObject.getTitle());
                    }
                    if (objImage != null) {
                        final String firstName = sObject.getFirstName();
                        String initials = Constants.EMPTY_STRING;
                        if (firstName.length() > 0) {
                            initials = firstName.substring(0, 1);
                        }
                        objImage.setText(initials);
                        setBubbleColor(objImage, firstName);
                    }
                    final ImageView syncImage = (ImageView) convertView.findViewById(R.id.sync_status_view);
                    if (syncImage != null && sObject.isLocallyModified()) {
                        syncImage.setImageResource(R.drawable.sync_local);
                    } else {
                        syncImage.setImageResource(R.drawable.sync_success);
                    }
                }
            }
            return convertView;
        }

        private void setBubbleColor(TextView tv, String firstName) {
            firstName = firstName.trim();
            int code = 0;
            if (!TextUtils.isEmpty(firstName)) {
                for (int i = 0; i < firstName.length(); i++) {
                    code += firstName.charAt(i);
                }
            }
            int colorIndex = code % CONTACT_COLORS.length;
            int color = CONTACT_COLORS[colorIndex];
            final GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(color);
            drawable.setShape(GradientDrawable.OVAL);
            tv.setBackground(drawable);
        }
    }

    /**
     * A simple utility class to implement filtering.
     *
     * @author bhariharan
     */
    private static class NameFieldFilter extends Filter {

        private ContactListAdapter adpater;
        private List<ContactObject> data;
        private String filterTerm;

        /**
         * Parameterized constructor.
         *
         * @param adapter  List adapter.
         * @param origList List to perform filtering against.
         */
        public NameFieldFilter(ContactListAdapter adapter, List<ContactObject> origList) {
            this.adpater = adapter;
            this.data = origList;
            this.filterTerm = null;
        }

        /**
         * Sets the original data set.
         *
         * @param data Original data set.
         */
        public void setData(List<ContactObject> data) {
            System.out.println("data in adapter is " + data);
            this.data = data;
            filter(filterTerm);
        }

        /**
         * Sets the filter term
         *
         * @param filterTerm
         * @return
         */
        public void setFilterTerm(String filterTerm) {
            this.filterTerm = filterTerm;
            filter(filterTerm);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (data == null) {
                return null;
            }
            final FilterResults results = new FilterResults();
            if (TextUtils.isEmpty(constraint)) {
                results.values = data;
                results.count = data.size();
                return results;
            }
            final String filterString = constraint.toString().toLowerCase();
            int count = data.size();
            String filterableString;
            final List<ContactObject> resultSet = new ArrayList<ContactObject>();
            for (int i = 0; i < count; i++) {
                filterableString = data.get(i).getName();
                if (filterableString.toLowerCase().contains(filterString)) {
                    resultSet.add(data.get(i));
                }
            }
            results.values = resultSet;
            results.count = resultSet.size();
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            System.out.println("publish results" + results);
            if (results != null && results.values != null) {
                adpater.setData((List<ContactObject>) results.values);
            }
        }
    }

    /**
     * A simple receiver for load complete events.
     *
     * @author bhariharan
     */
    private class LoadCompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (ContactListLoader.LOAD_COMPLETE_INTENT_ACTION.equals(action)) {
                    refreshList();
                }
            }
        }
    }
}
