package com.salesforce.samples.smartsyncexplorer;

import android.text.TextUtils;

import com.salesforce.androidsdk.smartsync.model.SalesforceObject;
import com.salesforce.androidsdk.smartsync.target.SyncTarget;
import com.salesforce.androidsdk.smartsync.util.Constants;

import org.json.JSONObject;

public class VisitReportObject extends SalesforceObject {

    public static final String V_R_NAME = "Name";
    public static final String V_R_STATUS = "Status__c";
    public static final String V_R_EXPENSES = "Expenses__c";
    public static final String V_R_SUBJECT = "Subject__c";
    public static final String V_R_RELATED_PLAN = "Related_Plan__c";
    public static final String V_R_DESCRIPTION = "Description__c";

    public String getvRDescription() {
        return sanitizeText(rawData.optString(V_R_DESCRIPTION));
    }

    public String getvRName() {
        return sanitizeText(rawData.optString(V_R_NAME));
    }

    public String getvRStatus() {
        return sanitizeText(rawData.optString(V_R_STATUS));
    }

    public String getvRExpenses() {
        return sanitizeText(rawData.optString(V_R_EXPENSES));
    }

    public String getvRSubject() {
        return sanitizeText(rawData.optString(V_R_SUBJECT));
    }

    public String getvRRelatedPlan() {
        return sanitizeText(rawData.optString(V_R_RELATED_PLAN));
    }

    public boolean isLocallyUpdated() {
        return isLocallyUpdated;
    }

    private final boolean isLocallyCreated;
    private final boolean isLocallyUpdated;
    private final boolean isLocallyDeleted;

    private final boolean isLocallyModified;

    /**
     * Parameterized constructor.
     *
     * @param data Raw data.
     */
    public VisitReportObject(JSONObject data) {
        super(data);
        objectType = Constants.VISIT_REPORT;
        objectId = data.optString(Constants.ID);
        name = data.optString(V_R_NAME);
        isLocallyCreated = data.optBoolean(SyncTarget.LOCALLY_CREATED);
        isLocallyDeleted = data.optBoolean(SyncTarget.LOCALLY_DELETED);
        isLocallyUpdated = data.optBoolean(SyncTarget.LOCALLY_UPDATED);
        isLocallyModified = isLocallyCreated || isLocallyUpdated || isLocallyDeleted;
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
