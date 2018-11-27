package com.salesforce.samples.smartsyncexplorer;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.smartstore.store.AlterSoupLongOperation;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.ui.SalesforceActivity;
import com.salesforce.samples.smartsyncexplorer.loaders.ContactListLoader;
import com.salesforce.samples.smartsyncexplorer.sync.ContactSyncAdapter;

import org.apache.commons.io.*;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
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
    public static final int MY_PERMISSIONS_REQUEST_WRITE_CALENDAR = 123;
    ProgressDialog progressDialog;

    Button btnGetAttachments, btnDownloadAttachments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);
        getActionBar().setTitle("Product List");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        rvProducts = findViewById(R.id.rvProducts);
        btnGetAttachments = findViewById(R.id.btnGetAttachments);
        btnDownloadAttachments = findViewById(R.id.btnDownloadAttchments);
        btnDownloadAttachments.setEnabled(false);


        loadCompleteReceiver = new LoadCompleteReceiver();
        isRegistered = new AtomicBoolean(false);
        sdkManager = SmartSyncSDKManager.getInstance();
        smartStore = sdkManager.getSmartStore(sdkManager.getUserAccountManager().getCurrentUser());
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

    }

    @Override
    public void onResume(final RestClient client) {
        boolean result = checkPermission();
        if (result) {
            productObjects = new ArrayList<>();
            getLoaderManager().initLoader(PRODUCT_LOADER_ID, null, this);
            if (!isRegistered.get()) {
                registerReceiver(loadCompleteReceiver,
                        new IntentFilter(ContactListLoader.LOAD_COMPLETE_INTENT_ACTION));
            }
            isRegistered.set(true);
            callProductApi(client);
        }
    }

    private void callProductApi(final RestClient client) {
        progressDialog.show();
        progressDialog.setMessage("Loading Products...");
        RestRequest restRequest =
                null;
        try {
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT name,id,ProductCode,Family FROM product2");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success product Activity product2" + request);
                System.out.println("success product Activity product2" + response);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
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
                        mAdapter = new ProductListAdapter(productObjects);
                        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                        rvProducts.setLayoutManager(mLayoutManager);
                        rvProducts.setAdapter(mAdapter);

                       /* for (int i = 0; i < productObjects.size(); i++) {
                            setUpPricebookSoup(client, productObjects.get(i).getProductCode());
                        }*/
                        getAttachments(client);

                        /*btnGetAttachments.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getAttachments(client);
                            }
                        });*/

                        mAdapter.setOnItemClickListener(new ProductListAdapter.ClickListener() {
                            @Override
                            public void onItemClick(int position, View v) {
                                ProductObject sObject = productObjects.get(position);
                                System.out.println("cliked object id is " + sObject.getProductId());
                                Intent intent = new Intent(ProductsActivity.this, ProductDetailsActivity.class);
                                intent.putExtra("product_code", sObject.getProductId());
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
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
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
                                intent.putExtra("product_code", sObject.getProductId());
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

    private void getAttachments(RestClient client) {
        smartStore.clearSoup("attachments");

        for (int j = 0; j < productObjects.size(); j++) {
            progressDialog.show();
            progressDialog.setMessage("Loading Attachments...");
            System.out.println("is last before" + j + ">>> " + (productObjects.size() - 1));
            //setAttachmentSoup(client, productObjects.get(j).getProductId(), true);
            if (j == productObjects.size() - 1) {
                System.out.println("is last " + j + ">>> " + (productObjects.size() - 1));
                setAttachmentSoup(client, productObjects.get(j).getProductId(), true);
                //downloadAttchmets(client);
            } else {
                setAttachmentSoup(client, productObjects.get(j).getProductId(), false);
            }
        }
        progressDialog.dismiss();

    }

    private void setAttachmentSoup(final RestClient client, final String productCode, final boolean isLast) {

        RestRequest restRequest =
                null;
        try {
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT ContentDocument.title,LinkedEntityId,ContentDocumentId,ContentDocument.FileType from ContentDocumentLink WHERE LinkedEntityId='" + productCode + "'");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse response) {
                System.out.println("success product Activity" + request);
                progressDialog.dismiss();
                try {
                    if (response.asJSONObject().getJSONArray("records").length() != 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    btnDownloadAttachments.setEnabled(true);
                                    final JSONArray jsonArray = response.asJSONObject().getJSONArray("records");
                                    inserAttachments(jsonArray);
                                    System.out.println("is last "+isLast);
                                    downloadAttchmets(client);
                                    if (isLast) {
                                        System.out.println("is last inside loop "+isLast);
                                    }
                                   /* btnDownloadAttachments.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            progressDialog.setMessage("Download in progress..");
                                            progressDialog.show();
                                            downloadAttchmets(client);
                                        }
                                    });*/
                                } catch (JSONException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                System.out.println("before progress dialog");
                // progressDialog.dismiss();
            }

            @Override
            public void onError(Exception exception) {
                exception.printStackTrace();
               /* if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }*/
                progressDialog.dismiss();
                System.out.println("failed " + exception.getMessage());
            }
        });
    }

    private void downloadAttchmets(RestClient client) {

        if (productObjects == null || productObjects.isEmpty()) {
            productObjects = productListLoader.loadInBackground();
        }

        for (int i = 0; i < productObjects.size(); i++) {

            final QuerySpec querySpec = QuerySpec.buildExactQuerySpec(
                    "attachments", AttachmentObject.ATTACHMENT_ID, productObjects.get(i).getProductId(), null, null, 10000);
            JSONArray results = null;
            final List<AttachmentObject> attachmentObjects = new ArrayList<>();
            try {
                results = smartStore.query(querySpec, 0);
                for (int k = 0; k < results.length(); k++) {
                    attachmentObjects.add(new AttachmentObject(results.getJSONObject(k)));
                }
            } catch (JSONException e) {
                Log.e(AlterSoupLongOperation.TAG, "JSONException occurred while parsing", e);
            } catch (SmartSqlHelper.SmartSqlException e) {
                Log.e(AlterSoupLongOperation.TAG, "SmartSqlException occurred while fetching data", e);
            }

            /*if (attachmentObjects.isEmpty()){
                Toast.makeText(ProductsActivity.this,"Please Click on Get Details",Toast.LENGTH_LONG).show();
            }*/

            System.out.println("attachmentObjects.size() "+attachmentObjects.size()+">>> "+productObjects.get(i).getProductName());

            for (int j = 0; j < attachmentObjects.size(); j++) {
                String cdID = attachmentObjects.get(j).getContentDocumentId();
                String fileType = attachmentObjects.get(j).getContentDocumentfileType();
                System.out.println("content document " + cdID + " >>" + fileType);
                requestBlob(client, cdID, fileType);
            }
            progressDialog.dismiss();
        }
    }

    private void requestBlob(final RestClient client, final String contentDocumentId, final String fileType) {

        RestRequest restRequest =
                null;
        try {
            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "select id, versiondata from contentversion where contentdocumentid='" + contentDocumentId + "'");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                System.out.println("success product request " + request);
                System.out.println("success product Activity" + response);
                System.out.println("success product Activity requestBlob" + response);
                String versionData = "";
                try {
                    versionData = response.asJSONObject().getJSONArray("records").getJSONObject(0).getString("VersionData");
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                final String finalVersionData = versionData;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       /* progressDialog.setMessage("download in progress...");
                        progressDialog.show();*/
                        LongOperation longOperation = new LongOperation(client, finalVersionData, fileType, contentDocumentId);
                        longOperation.execute();
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                exception.printStackTrace();
            }
        });
    }


    @SuppressLint("StaticFieldLeak")
    private class LongOperation extends AsyncTask<String, Void, String> {

        RestClient client;
        String versionData, fileType, contentDocumentId;
        byte[] arrBytes;
        ProgressDialog progressDialog1;

        public LongOperation(RestClient client1, String versionDat1a, String fileType, String contentDocumentId) {
            this.client = client1;
            this.versionData = versionDat1a;
            this.fileType = fileType;
            this.contentDocumentId = contentDocumentId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog1 = new ProgressDialog(ProductsActivity.this);
            progressDialog1.show();
            progressDialog1.setMessage("Downloading Attachments...");
            progressDialog1.setCancelable(false);
        }


        @Override
        protected String doInBackground(String... params) {
            String result;
            String inputLine;
            System.out.println("do in background " + contentDocumentId);
            try {
                String stringUrl = client.getClientInfo().resolveUrl(versionData).toString();
                //Create a URL object holding our url
                URL myUrl = new URL(stringUrl);
                //Create a connection
                HttpURLConnection connection = (HttpURLConnection)
                        myUrl.openConnection();
                //Set methods and timeouts
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + client.getAuthToken());

                //Connect to our url
                connection.connect();
                //Create a new InputStreamReader
                InputStreamReader streamReader = new
                        InputStreamReader(connection.getInputStream());
                //Create a new buffered reader and String Builder
                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();
                //Check if the line we are reading is not null

                byte[] bytes = IOUtils.toByteArray(connection.getInputStream());
                System.out.println("byte array is " + bytes);
                System.out.println("byte array size is " + connection.getContentLength());
                String mime = URLConnection.guessContentTypeFromName(stringUrl);
                System.out.println("mimetype is " + mime);
                arrBytes = bytes;


                while ((inputLine = reader.readLine()) != null) {
                    stringBuilder.append(inputLine);
                }
                //Close our InputStream and Buffered reader
                reader.close();
                streamReader.close();
                //Set our result equal to our stringBuilder
                result = stringBuilder.toString();


            } catch (IOException e) {
                e.printStackTrace();
                result = null;
            }
            System.out.println("result is " + result);
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            // into onPostExecute() but that is upto you
            super.onPostExecute(result);
            if (progressDialog1.isShowing()) {
                progressDialog1.dismiss();
            }

            String path = Environment.getExternalStorageDirectory() + "/" + "SalesForce/";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileOutputStream fos;
            try {
                fos = new FileOutputStream(new File(path + contentDocumentId + "." + fileType.toLowerCase().trim()));
                //Get the entity from our response
                fos.write(arrBytes);
                fos.close();
                System.out.println("post Execute " + result);
                System.out.println("post Execute " + fileType);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    private void inserAttachments(final JSONArray records) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (records != null) {
                        for (int i = 0; i < records.length(); i++) {
                            if (records.get(i) != null) {
                                try {
                                    System.out.println("attachments id " + records.getJSONObject(i));
                                    smartStore.upsert("attachments", records.getJSONObject(i));

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
        });
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


    public boolean checkPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission necessary");
                    alertBuilder.setMessage("Please grant all permissions");
                    alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(ProductsActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_CALENDAR);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                } else {
                    ActivityCompat.requestPermissions(ProductsActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_CALENDAR);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_CALENDAR:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //writeCalendarEvent();
                    //recreate();
                } else {
                    //code for deny
                }
                break;
        }
    }


    //not used


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
}
