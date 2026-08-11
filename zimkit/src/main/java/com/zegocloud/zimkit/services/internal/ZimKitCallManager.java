package com.zegocloud.zimkit.services.internal;

public class ZimKitCallManager {
    private static ZimKitCallManager instance;
    private CallCallback currentCallback; // Holds the active UI listener

    private ZimKitCallManager() {}

    public static synchronized ZimKitCallManager getInstance() {
        if (instance == null) {
            instance = new ZimKitCallManager();
        }
        return instance;
    }

    // Call this from the UI when it is created
    public void registerCallback(CallCallback callback) {
        this.currentCallback = callback;
    }

    // Call this from the UI when it is destroyed
    public void unregisterCallback() {
        this.currentCallback = null;
    }

    // This is called inside your internal service events handler
    public void triggerCallInfoEvent(String id) {
        if (currentCallback != null) {
            currentCallback.callInfo(id);
        } else {
            // Safe: Event happened before UI was ready.
            // Log it or save it as a pending state if needed.
        }
    }
}
