package com.salesforce.samples.smartsyncexplorer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.salesforce.androidsdk.smartstore.store.AlterSoupLongOperation.TAG;

public class ProductDetailsActivity extends SalesforceActivity {

    private SmartStore smartStore;
    private SyncManager syncMgr;
    String productCode;
    RecyclerView rvDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        rvDetails = findViewById(R.id.rvDetails);
        SmartSyncSDKManager sdkManager = SmartSyncSDKManager.getInstance();
        smartStore = sdkManager.getSmartStore(sdkManager.getUserAccountManager().getCurrentUser());
        syncMgr = SyncManager.getInstance(sdkManager.getUserAccountManager().getCurrentUser());
        productCode = getIntent().getStringExtra("product_code");
        System.out.println("product code " + productCode);

        getActionBar().setTitle("Attachments");
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
      /*  final QuerySpec querySpec1 = QuerySpec.buildExactQuerySpec(
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
        System.out.println("fhgffjfjfgggfgf " + sObject.getProductDetailName());
        System.out.println("fhgffjfjfgggfgf  results1 " + results1);*/

/*
        final QuerySpec querySpec1 = QuerySpec.buildExactQuerySpec(
                "attachments", AttachmentObject.ATTACHMENT_ID, productCode, null, null, 1);
        JSONArray results1 = null;
        List<AttachmentObject> sObject = new ArrayList<>();
        try {
            results1 = smartStore.query(querySpec1, 0);
            System.out.println("size is>>>"+results1.length());
            for (int i = 0; i < results1.length(); i++) {
                sObject.add(new AttachmentObject(results1.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SmartSqlHelper.SmartSqlException e) {
            Log.e(TAG, "SmartSqlException occurred while fetching data", e);
        }
        System.out.println("size is>>>"+sObject.size());
        for (int i = 0; i < sObject.size(); i++) {
            System.out.println("getAttachmentTitle " + sObject.get(i).getAttachmentTitle());
            System.out.println("fhgffjfjfgggfgf  results1 " + results1);
        }*/

        /*final QuerySpec querySpec = QuerySpec.buildAllQuerySpec("attachments",
                AttachmentObject.ATTACHMENT_ID, QuerySpec.Order.ascending, 100000);*/
        final QuerySpec querySpec = QuerySpec.buildExactQuerySpec(
                "attachments", AttachmentObject.ATTACHMENT_ID, productCode, null, null, 10000);
        JSONArray results = null;
        final List<AttachmentObject> products = new ArrayList<>();
        try {
            results = smartStore.query(querySpec, 0);
            for (int i = 0; i < results.length(); i++) {
                products.add(new AttachmentObject(results.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SmartSqlHelper.SmartSqlException e) {
            Log.e(TAG, "SmartSqlException occurred while fetching data", e);
        }
        System.out.println("products in do products    attachmnnts >>>>>> /n" + products);

        AttachmentAdapter mAdapter = new AttachmentAdapter(products);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvDetails.setLayoutManager(mLayoutManager);
        rvDetails.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new AttachmentAdapter.ClickListener() {
            @SuppressLint("IntentReset")
            @Override
            public void onItemClick(int position, View v) {
                String path = Environment.getExternalStorageDirectory() + "/" + "SalesForce/";
                File file = new File(path + products.get(position).getAttachmentTitle() + "." + products.get(position).getContentDocumentfileType().toLowerCase());
                System.out.println("uri is " + file);
                if (file.exists()) {
                    Uri uri;
                    if (Build.VERSION.SDK_INT < 24) {
                        uri = Uri.fromFile(file);
                    } else {
                        uri = Uri.parse(file.getPath()); // My work-around for new SDKs, doesn't work on older ones.
                    }
                    Intent getIntent = new Intent(Intent.ACTION_VIEW);

                    getIntent.setDataAndType(uri, getMimeType(uri.toString()));
                    startActivityForResult(Intent.createChooser(getIntent, "Open"), 101);
                    //startActivity(getIntent);
                } else {
                    Toast.makeText(ProductDetailsActivity.this, "Unable to find application that will open " + products.get(position).getContentDocumentfileType().toLowerCase(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onItemLongClick(int position, View v) {

            }
        });
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    public void onResume(RestClient client) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}
