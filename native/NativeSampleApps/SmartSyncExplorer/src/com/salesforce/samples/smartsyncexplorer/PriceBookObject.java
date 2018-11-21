package com.salesforce.samples.smartsyncexplorer;

import android.text.TextUtils;

import com.salesforce.androidsdk.smartsync.model.SalesforceObject;
import com.salesforce.androidsdk.smartsync.target.SyncTarget;
import com.salesforce.androidsdk.smartsync.util.Constants;

import org.json.JSONObject;

public class PriceBookObject extends SalesforceObject {



    public static final String PRODUCT_DETAIL_NAME = "Name";
    public static final String PRODUCT_DETAIL_ID = "Product2Id";
    public static final String PRODUCT_DETAIL_CODE = "ProductCode";
    public static final String PRODUCT_DETAIL_STD_PRICE = "UseStandardPrice";
    public static final String PRODUCT_DETAIL_UNIT_PRICE = "UnitPrice";
    public static final String PRODUCT_DETAIL_IS_ACTIVE = "IsActive";

    private final boolean isLocallyCreated;
    private final boolean isLocallyUpdated;
    private final boolean isLocallyDeleted;

    private final boolean isLocallyModified;

    /**
     * Parameterized constructor.
     *
     * @param data Raw data.
     */
    public PriceBookObject(JSONObject data) {
        super(data);
        objectType = Constants.PRODUCTS;
        objectId = data.optString(PRODUCT_DETAIL_CODE);
        name = data.optString(PRODUCT_DETAIL_NAME);
        isLocallyCreated = data.optBoolean(SyncTarget.LOCALLY_CREATED);
        isLocallyDeleted = data.optBoolean(SyncTarget.LOCALLY_DELETED);
        isLocallyUpdated = data.optBoolean(SyncTarget.LOCALLY_UPDATED);
        isLocallyModified = isLocallyCreated || isLocallyUpdated || isLocallyDeleted;
    }

    public  String getProductDetailName() {
        return sanitizeText(rawData.optString(PRODUCT_DETAIL_NAME));
    }

    public  String getProductDetailId() {
        return sanitizeText(rawData.optString(PRODUCT_DETAIL_ID));
    }

    public  String getProductDetailCode() {
        return sanitizeText(rawData.optString(PRODUCT_DETAIL_CODE));
    }

    public  String getProductDetailStdPrice() {
        return sanitizeText(rawData.optString(PRODUCT_DETAIL_STD_PRICE));
    }

    public  String getProductDetailUnitPrice() {
        return sanitizeText(rawData.optString(PRODUCT_DETAIL_UNIT_PRICE));
    }

    public  String getProductDetailIsActive() {
        return sanitizeText(rawData.optString(PRODUCT_DETAIL_IS_ACTIVE));
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
