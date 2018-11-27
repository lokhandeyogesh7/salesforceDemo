package com.salesforce.samples.smartsyncexplorer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.salesforce.samples.smartsyncexplorer.sync.ContactSyncAdapter;

public class VisitSyncService extends Service {

    private static final Object SYNC_ADAPTER_LOCK = new Object();
    private static VisitReportSyncAdapter CONTACT_SYNC_ADAPTER = null;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (SYNC_ADAPTER_LOCK) {
            if (CONTACT_SYNC_ADAPTER == null) {
                CONTACT_SYNC_ADAPTER = new VisitReportSyncAdapter(getApplicationContext(),
                        true, false);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return CONTACT_SYNC_ADAPTER.getSyncAdapterBinder();
    }
}
