package com.example.textmonitoraccessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.app.NotificationManager;
import android.content.Context;
import android.app.NotificationChannel;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;

public class MyAccessibilityService extends AccessibilityService {

    private final Map<String, String> toxicToNeutral = new HashMap<String, String>() {{
        put("shit", "bad");
        put("disgusting", "unpleasant");
        put("fat", "chubby");
        put("ugly", "unattractive");
        put("stupid", "unwise");
    }};

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e("MyAccessibilityService", "Root node is null");
            return;
        }

        boolean detected = scanTextContent(rootNode);
        if (detected) {
            showToxicNotification();
        }
    }

    private boolean scanTextContent(AccessibilityNodeInfo node) {
        if (node == null) return false;

        boolean harmfulDetected = false;

        CharSequence text = node.getText();
        if (text != null) {
            String originalText = text.toString();
            for (Map.Entry<String, String> entry : toxicToNeutral.entrySet()) {
                String toxic = entry.getKey();
                String neutral = entry.getValue();

                if (originalText.toLowerCase().contains(toxic)) {
                    harmfulDetected = true;
                    Log.d("ToxicWordDetected", "Found: " + toxic + " in: " + originalText);

                    // If the node is editable, replace the text
                    if (node.isEditable() && node.isFocused()) {
                        String updatedText = originalText.replaceAll("(?i)" + toxic, neutral);
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText);
                        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    }
                    break;
                }
            }
        }

        // Recursively scan all children
        for (int i = 0; i < node.getChildCount(); i++) {
            harmfulDetected |= scanTextContent(node.getChild(i));
        }

        return harmfulDetected;
    }

    private void showToxicNotification() {
        String channelId = "toxic_text_channel";
        String channelName = "Toxic Text Alert";

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Toxic Content Detected")
                .setContentText("Current screen contains toxic words.")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify(1, builder.build());
    }

    @Override
    public void onInterrupt() {
        Log.d("MyAccessibilityService", "Service interrupted");
    }
}
