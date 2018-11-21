package com.salesforce.samples.smartsyncexplorer;

import android.text.TextUtils;

import com.salesforce.androidsdk.smartsync.model.SalesforceObject;
import com.salesforce.androidsdk.smartsync.target.SyncTarget;
import com.salesforce.androidsdk.smartsync.util.Constants;

import org.json.JSONObject;

public class ProductObject extends SalesforceObject {

    public static final String PRODUCT_NAME = "Name";
    public static final String PRODUCT_ID = "Id";
    public static final String PRODUCT_CODE = "ProductCode";

    public String getProductFamily() {
        return sanitizeText(rawData.optString(PRODUCT_FAMILY));
    }

    public static final String PRODUCT_FAMILY = "Family";



    private final boolean isLocallyCreated;
    private final boolean isLocallyUpdated;
    private final boolean isLocallyDeleted;

    private final boolean isLocallyModified;

    /**
     * Parameterized constructor.
     *
     * @param data Raw data.
     */
    public ProductObject(JSONObject data) {
        super(data);
        objectType = Constants.PRODUCTS;
        objectId = data.optString(Constants.ID);
        name = data.optString(PRODUCT_NAME);
        isLocallyCreated = data.optBoolean(SyncTarget.LOCALLY_CREATED);
        isLocallyDeleted = data.optBoolean(SyncTarget.LOCALLY_DELETED);
        isLocallyUpdated = data.optBoolean(SyncTarget.LOCALLY_UPDATED);
        isLocallyModified = isLocallyCreated || isLocallyUpdated || isLocallyDeleted;
    }

    public  String getProductName() {
        return sanitizeText(rawData.optString(PRODUCT_NAME));
    }

    public  String getProductId() {
        return sanitizeText(rawData.optString(PRODUCT_ID));
    }

    public  String getProductCode() {
        return sanitizeText(rawData.optString(PRODUCT_CODE));
    }

    /**
     * Returns whether the contact has been locally modified or not.
     *
     * @return True - if the contact has been locally modified, False - otherwise.
     */
    public boolean isLocallyModified() {
        return isLocallyModified;
    }

    /**
     * Returns whether the contact has been locally deleted or not.
     *
     * @return True - if the contact has been locally deleted, False - otherwise.
     */
    public boolean isLocallyDeleted() {
        return isLocallyDeleted;
    }

    /**
     * Returns whether the contact has been locally created or not.
     *
     * @return True - if the contact has been locally created, False - otherwise.
     */
    public boolean isLocallyCreated() {
        return isLocallyCreated;
    }


    private String sanitizeText(String text) {
        if (TextUtils.isEmpty(text) || text.equals(Constants.NULL_STRING)) {
            return Constants.EMPTY_STRING;
        }
        return text;
    }
}

