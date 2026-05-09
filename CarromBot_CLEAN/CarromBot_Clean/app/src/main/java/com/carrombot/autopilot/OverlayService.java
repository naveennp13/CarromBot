package com.carrombot.autopilot;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private GestureDetector gestureDetector;
    private WindowManager.LayoutParams params;

    private float overlayX = 0;
    private float overlayY = 0;
    private float lastX = 0;
    private float lastY = 0;
    private boolean isCollapsed = true;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Create overlay view
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);
        
        // Setup window parameters
        params = new WindowManager.LayoutParams();
        params.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params.format = PixelFormat.TRANSLUCENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        
        params.width = 300;
        params.height = 500;
        params.x = 10;
        params.y = 100;

        // Add view to window
        windowManager.addView(overlayView, params);

        // Setup gesture detector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                int newX = (int) (overlayX - distanceX);
                int newY = (int) (overlayY - distanceY);

                params.x = newX;
                params.y = newY;

                windowManager.updateViewLayout(overlayView, params);

                overlayX = newX;
                overlayY = newY;

                return true;
            }
        });

        // Setup UI elements
        setupOverlayUI();
    }

    private void setupOverlayUI() {
        // Angle display
        TextView angleDisplay = overlayView.findViewById(R.id.overlayAngle);
        angleDisplay.setText("0°");

        // Power display
        TextView powerDisplay = overlayView.findViewById(R.id.overlayPower);
        powerDisplay.setText("0%");

        // Accuracy indicator
        TextView accuracyDisplay = overlayView.findViewById(R.id.overlayAccuracy);
        accuracyDisplay.setText("Ready");

        // Quick shot button
        Button quickShotBtn = overlayView.findViewById(R.id.quickShotBtn);
        quickShotBtn.setOnClickListener(v -> executeQuickShot());

        // Collapse button
        Button collapseBtn = overlayView.findViewById(R.id.collapseBtn);
        collapseBtn.setOnClickListener(v -> toggleOverlay());

        // Make header draggable
        View header = overlayView.findViewById(R.id.overlayHeader);
        header.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void toggleOverlay() {
        isCollapsed = !isCollapsed;
        
        View content = overlayView.findViewById(R.id.overlayContent);
        if (isCollapsed) {
            content.setVisibility(View.GONE);
            params.height = 50;
        } else {
            content.setVisibility(View.VISIBLE);
            params.height = 500;
        }

        windowManager.updateViewLayout(overlayView, params);
    }

    private void executeQuickShot() {
        // Send broadcast to execute shot
        Intent intent = new Intent("CARROM_EXECUTE_SHOT");
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }
}
