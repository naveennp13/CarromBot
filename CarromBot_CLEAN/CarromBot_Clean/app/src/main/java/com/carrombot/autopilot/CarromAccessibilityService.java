package com.carrombot.autopilot;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.WindowManager;
import android.widget.Toast;

public class CarromAccessibilityService extends AccessibilityService {

    private static final String TAG = "CarromBot";
    private static final String CARROM_POOL_PACKAGE = "com.outofthebit.carrom";
    
    private CarromBotReceiver receiver;
    private Handler mainHandler;
    private boolean autoPlayEnabled = false;
    private boolean gameDetected = false;
    
    // Game state tracking
    private float strikerX, strikerY;
    private float[][] coinPositions = new float[9][2];
    private float carromX, carromY;
    private boolean boardUpdated = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Register broadcast receiver
        receiver = new CarromBotReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("CARROM_START_AUTOPLAY");
        filter.addAction("CARROM_STOP_AUTOPLAY");
        registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        String packageName = event.getPackageName().toString();
        
        // Check if Carrom Pool is active
        if (packageName.contains("carrom") || packageName.equals(CARROM_POOL_PACKAGE)) {
            gameDetected = true;
            
            // Update game state based on accessibility events
            updateGameState(event);
            
            // Process auto-play if enabled
            if (autoPlayEnabled) {
                processAutoPlay();
            }
        }
    }

    private void updateGameState(AccessibilityEvent event) {
        try {
            // Get root node for window content
            var rootNode = getRootInActiveWindow();
            if (rootNode == null) return;

            // Traverse accessibility tree to find game elements
            traverseNode(rootNode);
            boardUpdated = true;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void traverseNode(android.view.accessibility.AccessibilityNodeInfo node) {
        if (node == null) return;

        try {
            // Look for clickable elements that might be the striker or coins
            if (node.isClickable() && node.getContentDescription() != null) {
                String description = node.getContentDescription().toString().toLowerCase();
                
                // Detect striker
                if (description.contains("striker") || description.contains("cue")) {
                    android.graphics.Rect bounds = new android.graphics.Rect();
                    node.getBoundsInScreen(bounds);
                    strikerX = bounds.centerX();
                    strikerY = bounds.centerY();
                }
                
                // Detect coins
                if (description.contains("coin") || description.contains("ball")) {
                    android.graphics.Rect bounds = new android.graphics.Rect();
                    node.getBoundsInScreen(bounds);
                    // Store coin position
                }
                
                // Detect carrom/queen
                if (description.contains("carrom") || description.contains("queen")) {
                    android.graphics.Rect bounds = new android.graphics.Rect();
                    node.getBoundsInScreen(bounds);
                    carromX = bounds.centerX();
                    carromY = bounds.centerY();
                }
            }

            // Recursively check children
            for (int i = 0; i < node.getChildCount(); i++) {
                var child = node.getChild(i);
                if (child != null) {
                    traverseNode(child);
                    child.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processAutoPlay() {
        if (!boardUpdated) return;

        mainHandler.post(() -> {
            try {
                // Calculate optimal shot
                ShotCalculator calculator = new ShotCalculator(
                    strikerX, strikerY,
                    coinPositions,
                    carromX, carromY
                );
                
                ShotCalculator.OptimalShot shot = calculator.calculateOptimalShot();
                
                if (shot != null) {
                    // Execute the shot
                    executeShotWithDelay(shot);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        boardUpdated = false;
    }

    private void executeShotWithDelay(ShotCalculator.OptimalShot shot) {
        mainHandler.postDelayed(() -> {
            try {
                // Simulate drag motion from striker to target
                performDragMotion(
                    strikerX, strikerY,
                    shot.targetX, shot.targetY,
                    shot.power
                );
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500);
    }

    private void performDragMotion(float startX, float startY, float endX, float endY, float power) {
        // Use dispatchGesture to perform swipe/drag motion
        // This requires API 24+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.accessibilityservice.GestureDescription.Builder gestureBuilder = 
                new android.accessibilityservice.GestureDescription.Builder();
            
            // Create stroke path
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(startX, startY);
            
            // Calculate end point based on angle and power
            float deltaX = endX - startX;
            float deltaY = endY - startY;
            float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            float ratio = power / 100f;
            
            float finalX = startX + (deltaX * ratio);
            float finalY = startY + (deltaY * ratio);
            
            path.lineTo(finalX, finalY);
            
            long duration = (long) (300 + (power * 10)); // Longer drag for more power
            
            gestureBuilder.addStroke(
                new android.accessibilityservice.GestureDescription.StrokeDescription(
                    path, 0, duration
                )
            );
            
            dispatchGesture(gestureBuilder.build(), null, null);
        }
    }

    @Override
    public void onInterrupt() {
        // Handle service interruption
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    // Broadcast receiver for controlling auto-play
    private class CarromBotReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            String action = intent.getAction();
            if (action != null) {
                if (action.equals("CARROM_START_AUTOPLAY")) {
                    autoPlayEnabled = true;
                } else if (action.equals("CARROM_STOP_AUTOPLAY")) {
                    autoPlayEnabled = false;
                }
            }
        }
    }
}
