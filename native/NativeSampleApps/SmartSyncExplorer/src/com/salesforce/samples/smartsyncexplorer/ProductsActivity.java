package com.salesforce.samples.smartsyncexplorer;

import android.accounts.Account;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.ui.SalesforceActivity;
import com.salesforce.samples.smartsyncexplorer.loaders.ContactListLoader;
import com.salesforce.samples.smartsyncexplorer.objects.ContactObject;
import com.salesforce.samples.smartsyncexplorer.sync.ContactSyncAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.support.constraint.Constraints.TAG;

public class ProductsActivity extends SalesforceActivity implements LoaderManager.LoaderCallbacks<List<ProductObject>> {

    private LoadCompleteReceiver loadCompleteReceiver;
    private AtomicBoolean isRegistered;
    private static final int PRODUCT_LOADER_ID = 1;
    private ProductListLoader productListLoader;
    private static final String SYNC_CONTENT_AUTHORITY = "com.salesforce.samples.smartsyncexplorer.sync.productsyncadapter";
    private static final long SYNC_FREQUENCY_ONE_HOUR = 1 * 60 * 60;
    SmartSyncSDKManager sdkManager;
    private SmartStore smartStore;
    RecyclerView rvProducts;
    ProductListAdapter mAdapter;
    private SearchView searchView;
    List<ProductObject> productObjects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);
        getActionBar().setTitle("Product List");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        rvProducts = findViewById(R.id.rvProducts);


        loadCompleteReceiver = new LoadCompleteReceiver();
        isRegistered = new AtomicBoolean(false);
        sdkManager = SmartSyncSDKManager.getInstance();
        smartStore = sdkManager.getSmartStore(sdkManager.getUserAccountManager().getCurrentUser());

    }

    @Override
    public void onResume(final RestClient client) {
        productObjects = new ArrayList<>();
        getLoaderManager().initLoader(PRODUCT_LOADER_ID, null, this);
        if (!isRegistered.get()) {
            registerReceiver(loadCompleteReceiver,
                    new IntentFilter(ContactListLoader.LOAD_COMPLETE_INTENT_ACTION));
        }
        isRegistered.set(true);


        RestRequest restRequest =
                null;
        try {
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT name,id,ProductCode FROM product2");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println("exception is "+e.getMessage());
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success product Activity product2" + request);
                System.out.println("success product Activity product2" + response);
                try {
                    System.out.println("success product Activity sdsdsdsdsd" + response.asJSONObject().getJSONArray("records"));
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                try {
                    smartStore.clearSoup("products");
                    insertProducts(response.asJSONObject().getJSONArray("records"));
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        productObjects = productListLoader.loadInBackground();
                        System.out.println("firdst product id is " + productObjects.get(0).getProductCode());
                        mAdapter = new ProductListAdapter(productObjects);
                        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                        rvProducts.setLayoutManager(mLayoutManager);
                        rvProducts.setAdapter(mAdapter);

                        for (int i = 0; i < productObjects.size(); i++) {
                            setUpPricebookSoup(client, productObjects.get(i).getProductCode());
                        }

                        for (int j = 0; j < productObjects.size(); j++) {
                            setAttachmentSoup(client, productObjects.get(j).getProductId(),productObjects.get(j).getProductName());
                        }
                       /* for (int j = 0; j < productObjects.size(); j++) {
                            setNotesSoup(client, productObjects.get(j).getProductId(),productObjects.get(j).getProductName());
                        }*/

                        mAdapter.setOnItemClickListener(new ProductListAdapter.ClickListener() {
                            @Override
                            public void onItemClick(int position, View v) {
                                ProductObject sObject = productObjects.get(position);
                                System.out.println("cliked object id is " + sObject.getProductCode());
                                Intent intent = new Intent(ProductsActivity.this, ProductDetailsActivity.class);
                                intent.putExtra("product_code", sObject.getProductCode());
                                System.out.println("product code " + sObject.getProductCode());
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
                        productObjects = productListLoader.loadInBackground();
                        mAdapter = new ProductListAdapter(productObjects);
                        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                        rvProducts.setLayoutManager(mLayoutManager);
                        rvProducts.setAdapter(mAdapter);

                        mAdapter.setOnItemClickListener(new ProductListAdapter.ClickListener() {
                            @Override
                            public void onItemClick(int position, View v) {
                                ProductObject sObject = productObjects.get(position);
                                System.out.println("cliked object id is " + sObject.getProductCode());
                                Intent intent = new Intent(ProductsActivity.this, ProductDetailsActivity.class);
                                intent.putExtra("product_code", sObject.getProductCode());
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
        // Setup periodic sync
        setupPeriodicSync();
        // Sync now
        requestSync(true /* sync down only */);
    }

    private void setNotesSoup(RestClient client, String productCode, final String productName) {

        RestRequest restRequest =
                null;
        try {
           /* restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT Name,(select id,title from NotesAndAttachments) from Account WHERE ParentId='" + productCode + "'"); */
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT Title FROM Files WHERE ParentId='" + productCode + "'");
            /*restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT Body,CreatedById,CreatedDate,Id,IsDeleted,IsPrivate,LastModifiedById,LastModifiedDate,OwnerId,ParentId,SystemModstamp,Title FROM Note WHERE ParentId='" + productCode + "'");
    */    } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success product Activity" + request);
                System.out.println("success product Activity" + response);
                System.out.println("success product Activity NoteAndAttachment" + response);
                System.out.println("success product Activity NoteAndAttachment product name is " + productName);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });
            }
        });
    }

    private void setAttachmentSoup(RestClient client, String productCode, final String productName) {

        RestRequest restRequest =
                null;
        try {
           /* restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT Name,ContentType from Attachment WHERE ParentId='" + productCode + "'");*/
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT ContentDocument.title,LinkedEntityId,ContentDocumentId,ContentDocument.FileType from ContentDocumentLink WHERE LinkedEntityId='" + productCode + "'");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("getString(R.string.api_version) "+getString(R.string.api_version));

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success product Activity" + request);
                System.out.println("success product Activity" + response);
                System.out.println("success product Activity Attachment" + response);
                System.out.println("success product Activity Attachment product name is " + productName);

                try {
                    if (response.asJSONObject().getJSONArray("records").length()!=0){
                        for (int i = 0; i <response.asJSONObject().getJSONArray("records").length() ; i++) {
                            //DownloadTask downloadTask = new DownloadTask(this,response.asJSONObject().getJSONArray("records").getJSONObject());
                        }


                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });
            }
        });
    }

    private void setUpPricebookSoup(RestClient client, String productCode) {

        RestRequest restRequest =
                null;
        try {
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT ProductCode,Product2Id,name,UseStandardPrice,UnitPrice from PricebookEntry WHERE ProductCode='" + productCode + "'");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success product Activity" + request);
                System.out.println("success product Activity" + response);
                System.out.println("success product Activity setUpPricebookSoup" + response);
                try {
                    insertPriceBook(response.asJSONObject().getJSONArray("records"));
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });
            }
        });
    }

    private void insertPriceBook(JSONArray accounts) {
        try {
            if (accounts != null) {
               /* for (int i = 0; i < accounts.length(); i++) {
                    if (accounts.get(i) != null) {
                        try {*/
                System.out.println("price book soup id " + accounts.getJSONObject(0));
                smartStore.upsert("priceBook", accounts.getJSONObject(0));
                        /*} catch (JSONException exc) {
                            Log.e(TAG, "Error occurred while attempting to insert account. "
                                    + "Please verify validity of JSON data set.");
                        }*/
                //  }
                // }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error occurred while attempting to insert accounts. "
                    + "Please verify validity of JSON data set.");
        }
    }


    public void insertProducts(JSONArray accounts) {
        try {
            if (accounts != null) {
                for (int i = 0; i < accounts.length(); i++) {
                    if (accounts.get(i) != null) {
                        try {
                            smartStore.upsert("products", accounts.getJSONObject(i));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = new SearchView(this);
       /* searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);*/
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

    @Override
    public void onDestroy() {
        loadCompleteReceiver = null;
        super.onDestroy();
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
        extras.putBoolean(ContactSyncAdapter.SYNC_DOWN_ONLY, syncDownOnly);
        ContentResolver.requestSync(account, SYNC_CONTENT_AUTHORITY, extras);
    }


    private void refreshList() {
        getLoaderManager().getLoader(PRODUCT_LOADER_ID).forceLoad();
    }

    @Override
    public Loader<List<ProductObject>> onCreateLoader(int i, Bundle bundle) {
        productListLoader = new ProductListLoader(this, SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser());
        System.out.println("contact list loader " + productListLoader.loadInBackground());
        return productListLoader;
    }

    @Override
    public void onLoadFinished(Loader<List<ProductObject>> loader, List<ProductObject> o) {
        refreshList(o);
    }

    @Override
    public void onLoaderReset(Loader<List<ProductObject>> loader) {
        refreshList(null);
    }

    private void refreshList(List<ProductObject> data) {
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
                if (ContactListLoader.LOAD_COMPLETE_INTENT_ACTION.equals(action)) {
                    refreshList();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        } else {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }



    public class DownloadTask extends AsyncTask<Integer, Integer, Void> {

        private static final String PEFERENCE_FILE = "preference";
        private static final String ISDOWNLOADED = "isdownloaded";
        SharedPreferences settings;
        SharedPreferences.Editor editor;
        Context context;
        public DownloadTask(Context context){
            this.context = context;
        }

        protected void onPreExecute(){
            //Create the notification in the statusbar
        }

        @Override
        protected Void doInBackground(Integer... integers) {
            //This is where we would do the actual download stuff
            //for now I'm just going to loop for 10 seconds
            // publishing progress every second

            int count;

            try {


                URL url = new URL("filename url");
                URLConnection connexion = url.openConnection();
                connexion.connect();

                int lenghtOfFile = connexion.getContentLength();
                Log.d("ANDRO_ASYNC", "Lenght of file: " + lenghtOfFile);

                InputStream input = new BufferedInputStream(url.openStream());
                //OutputStream output = new FileOutputStream("/sdcard/foldername/temp.zip");
                OutputStream output = new FileOutputStream("/sdcard/foldername/himages.zip");
                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    //publishProgress(""+(int)((total*100)/lenghtOfFile));
                    Log.d("%Percentage%",""+(int)((total*100)/lenghtOfFile));
                    onProgressUpdate((int)((total*100)/lenghtOfFile));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                File file = new File(Environment.getExternalStorageDirectory()
                        + "/foldername/"+"_images.zip");
                File path = new File(Environment.getExternalStorageDirectory()
                        + "/foldername");

            } catch (Exception e) {}


            return null;
        }
        protected void onProgressUpdate(Integer... progress) {
            //This method runs on the UI thread, it receives progress updates
            //from the background thread and publishes them to the status bar
        }
        protected void onPostExecute(Void result)    {
            //The task is complete, tell the status bar about it
        }
    }
}
