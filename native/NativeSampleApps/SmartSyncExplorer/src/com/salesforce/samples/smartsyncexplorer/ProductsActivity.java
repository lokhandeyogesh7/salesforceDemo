package com.salesforce.samples.smartsyncexplorer;

import android.accounts.Account;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.webkit.MimeTypeMap;
import android.widget.SearchView;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.rest.files.FileRequests;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
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
    public static final int MY_PERMISSIONS_REQUEST_WRITE_CALENDAR = 123;
    ProgressDialog progressDialog;

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
            System.out.println("exception is " + e.getMessage());
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

                       /* for (int i = 0; i < productObjects.size(); i++) {
                            setUpPricebookSoup(client, productObjects.get(i).getProductCode());
                        }*/
                        smartStore.clearSoup("attachments");
                        for (int j = 0; j < productObjects.size(); j++) {
                            setAttachmentSoup(client, productObjects.get(j).getProductId());
                        }

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
    */
        } catch (UnsupportedEncodingException e) {
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

    private void setAttachmentSoup(final RestClient client, final String productCode) {
        progressDialog.show();
        progressDialog.setMessage("Loading Attachments...");
        RestRequest restRequest =
                null;
        try {
            /*restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT ContentDownloadUrl from ContentDocumentLink WHERE ContentDocumentId='" + productCode + "'");*/

            restRequest = RestRequest.getRequestForQuery(
                    getString(R.string.api_version), "SELECT ContentDocument.title,LinkedEntityId,ContentDocumentId,ContentDocument.FileType from ContentDocumentLink WHERE LinkedEntityId='" + productCode + "'");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("getString(R.string.api_version) " + getString(R.string.api_version));

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse response) {
                System.out.println("success product Activity" + request);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.show();
                        progressDialog.setMessage("Loading Attachments...");
                    }
                });

                try {
                    System.out.println("success product Activity size of records" + response.asJSONObject().getJSONArray("records"));
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }

                //System.out.println("success product Activity Attachment" + response.asJSONObject().getJSONArray("records"));
                System.out.println("success product Activity Attachment product name is " + productCode);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                try {
                    if (response.asJSONObject().getJSONArray("records").length() != 0) {
                        //for (int i = 0; i < response.asJSONObject().getJSONArray("records").length(); i++) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray jsonArray = response.asJSONObject().getJSONArray("records");
                                    inserAttachments(jsonArray);
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject jsonObject = jsonArray.getJSONObject(i).getJSONObject("ContentDocument");
                                        System.out.println("content document "+jsonObject);
                                        //JSONObject jsonObject1 =jsonObject.getJSONObject("attributes");
                                        //System.out.println(" attributes "+jsonObject1);

                                        requestBlob(client, jsonArray.getJSONObject(i).getString("ContentDocumentId"),jsonObject.getString("FileType"));
                                    }
                                    //downloadFile(client, response, productCode);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                // downloadFile(client, response, productCode);

                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                            }
                        });
                    }

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }


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
                    }
                });
            }
        });
    }

    private void requestBlob(final RestClient client, final String contentDocumentId, final String fileType) {

        RestRequest restRequest =
                null;
        List<String> strings = new ArrayList<>();
        strings.add("Id");
        strings.add("VersionData");
        try {
            restRequest = RestRequest.getRequestForQuery(
                    /* getString(R.string.api_version), "select id, versiondata from contentversion where contentdocumentid='0696F000008EDZGQA4'");*/
                    getString(R.string.api_version), "select id, versiondata from contentversion where contentdocumentid='"+contentDocumentId+"'");
            /*restRequest = RestRequest.getRequestForRetrieve(
                    getString(R.string.api_version), "ContentVersion", "0696F000008EDZGQA4", strings);*/
            // restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version),)
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
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                LongOperation longOperation = new LongOperation(client, versionData,fileType,contentDocumentId);
                longOperation.execute();

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


    private class LongOperation extends AsyncTask<String, Void, String> {

        RestClient client;
        String versionData,fileType,contentDocumentId;
        byte[] arrBytes;

        public LongOperation(RestClient client1, String versionDat1a, String fileType, String contentDocumentId) {
            this.client = client1;
            this.versionData = versionDat1a;
            this.fileType = fileType;
            this.contentDocumentId = contentDocumentId;
        }

        @Override
        protected String doInBackground(String... params) {
      /*      try {
                String s = client.getClientInfo().resolveUrl(versionData).toString();
                URL url = new URL(s);
                System.out.println("string url is "+url);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Bearer " + client.getAuthToken());
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-Type", "application/json");

                urlConnection.connect();

                int statusCode = urlConnection.getResponseCode();
                System.out.println(" request body is "+urlConnection.getRequestMethod());
                System.out.println(" request body is "+urlConnection.getHeaderField(1));
                if (statusCode == 200) {
                    InputStream it = new BufferedInputStream(urlConnection.getInputStream());
                    InputStreamReader read = new InputStreamReader(it);
                    BufferedReader buff = new BufferedReader(read);
                    StringBuilder dta = new StringBuilder();
                    String chunks;
                    while ((chunks = buff.readLine()) != null) {
                        dta.append(chunks);
                    }
                    System.out.println(" blob is "+chunks+">>> "+it.read());
                } else {
                    //Handle else
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            return "Executed";


*/

            String result;
            String inputLine;
            System.out.println("do in background ");
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

                InputStream is = new ByteArrayInputStream(bytes);

                String  mime = URLConnection.guessContentTypeFromName(stringUrl);
                System.out.println("mimetype is "+mime);

                arrBytes = bytes;


                while ((inputLine = reader.readLine()) != null) {
                    stringBuilder.append(inputLine);
                }
                //Close our InputStream and Buffered reader
                reader.close();
                streamReader.close();
                //Set our result equal to our stringBuilder
                result = stringBuilder.toString();


             /*   InputStream is = connection.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);

                ByteArrayInputStream baf = new ByteArrayInputStream();
                int current = 0;
                while ((current = bis.read()) != -1) {
                    baf.append((byte) current);
                    arrBytes.
                }

               arrBytes = IOUtils.toByteArray(is);*/


               /* ByteArrayInputStream fileInputStream = new ByteArrayInputStream(connection.getInputStream());
                int bytesAvailable = fileInputStream.available();

                int maxBufferSize = 1024 * 1024;//1 mb buffer - set size according to your need
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[] buffer = new byte[bufferSize];

                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    //dataOutputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }*/

                /* dataOutputStream.writeBytes(lineEnd);*/

               /* ByteArrayBuffer baf = new ByteArrayBuffer(bufferSize);
                int current = 0;
                while (bytesRead > 0) {
                    baf.append((byte) current);
                    //arrBytes.
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
*/
                //arrBytes = baf.toByteArray();

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
            String path = Environment.getExternalStorageDirectory() + "/" + "SalesForce/";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileOutputStream fos = null;

            /*byte[] bytes = result.getBytes("UTF-8");
            System.out.println("bytes is "+bytes);*/
            try {
                fos = new FileOutputStream(new File(path + contentDocumentId+ "." + fileType.toLowerCase().trim()));

                //Get the entity from our response
                fos.write(arrBytes);
                fos.close();

                System.out.println("post Execute " + result);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    private void inserAttachments(final JSONArray records) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.show();
                progressDialog.setMessage("Saving Attachments...");
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
                                    if (progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                }
                            }
                        }
                        if (progressDialog.isShowing()) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }
                    }
                } catch (JSONException e) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Log.e(TAG, "Error occurred while attempting to insert accounts. "
                            + "Please verify validity of JSON data set.");
                }

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


    private void downloadFile(final RestClient client, final RestResponse result, final String productName) {
        //Get the external storage directory and make a new directory for our attachments
        new Thread(new Runnable() {
            public void run() {
               /* if (!progressDialog.isShowing()) {
                    progressDialog.show();
                    progressDialog.setMessage("Downloading Attachments...");
                }*/
                String path = Environment.getExternalStorageDirectory() + "/" + "SalesForce/";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                try {
                    //This is the array from our query response for attachments
                    JSONArray records = result.asJSONObject().getJSONArray("records");

                    System.out.println("records inside download loop " + records);
                    System.out.println("records inside download loop " + records.length());

                    //Loop through our attachment records
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject jsonObject = records.getJSONObject(i).getJSONObject("ContentDocument");
                        String strTitle = jsonObject.getString("Title");
                        String fileType = jsonObject.getString("FileType");
                        String linkedEntityId = records.getJSONObject(i).getString("ContentDocumentId");
                        JSONObject contentDocument = records.getJSONObject(i).getJSONObject("ContentDocument");
                        JSONObject attributes = records.getJSONObject(i).getJSONObject("attributes");
                        String url = attributes.getString("url");

                        System.out.println("inside download loop title is " + strTitle + "   filetype is " + fileType + "   liked entity id is " + linkedEntityId);

                        //String attUrl = client.getClientInfo().resolveUrl(records.getJSONObject(i).getString("Body")).toString();
                        // String attUrl = client.getClientInfo().resolveUrl("/servlet/servlet.FileDownload?file=" + linkedEntityId).toString();
                        //String attUrl = client.getClientInfo().resolveUrl(url).toString()+"/"+linkedEntityId; /*+ linkedEntityId).toString()*/
                        String attUrl = client.getClientInfo().resolveUrl(url).toString();

                        //Create a new HttpClient
                        HttpClient tempClient = new DefaultHttpClient();

                        System.out.println("Attachment url is " + attUrl);

                        //parse our URL into a new URI
                        URI theUrl = new URI(attUrl);

                        //Create a new get request
                        HttpGet getRequest = new HttpGet();

                        //set the URI
                        getRequest.setURI(theUrl);

                        //set the header with the auth token we got from our instance of RestClient
                        getRequest.setHeader("Authorization", "Bearer " + client.getAuthToken());

                        //execute the request and put it in an HttpReponse
                        HttpResponse response = tempClient.execute(getRequest);

                        System.out.println("request is " + getRequest.getParams());

                        //Status line from our response of our execution of the request
                        StatusLine statusLine = response.getStatusLine();
                        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

                            //Create a new File Output Stream pointing to the directory we just created
                            FileOutputStream fos = new FileOutputStream(new File(path + linkedEntityId.trim() + "." + fileType.toLowerCase().trim()));

                            //Get the entity from our response
                            HttpEntity entity = response.getEntity();

                            //Write to the file output stream
                            entity.writeTo(fos);
                            entity.consumeContent();
                            fos.flush();
                            fos.close();
                            System.out.println("success  >>>" + statusLine.getReasonPhrase());
                            /*if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }*/
                        } else {
                           /* if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }*/
                            // Closes the connection.
                            System.out.println("error " + statusLine.getReasonPhrase());
                            response.getEntity().getContent().close();
                            throw new IOException(statusLine.getReasonPhrase());
                        }
                    }
                } catch (Exception e) {
                    //error handling
                   /* if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }*/
                    e.printStackTrace();
                    System.out.println("download exception " + e.getMessage());
                }

            }
        }).start();
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
}
