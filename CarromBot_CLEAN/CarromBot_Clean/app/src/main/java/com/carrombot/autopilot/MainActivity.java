package com.carrombot.autopilot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button autoPlayBtn, settingsBtn, startServiceBtn;
    private Switch overlaySwitch;
    private SeekBar powerSeekBar, angleSeekBar;
    private TextView powerValue, angleValue, statusText;
    private SharedPreferences prefs;
    private boolean autoPlayActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize preferences
        prefs = getSharedPreferences("CarromBot", MODE_PRIVATE);

        // Initialize views
        autoPlayBtn = findViewById(R.id.autoPlayBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        startServiceBtn = findViewById(R.id.startServiceBtn);
        overlaySwitch = findViewById(R.id.overlaySwitch);
        powerSeekBar = findViewById(R.id.powerSeekBar);
        angleSeekBar = findViewById(R.id.angleSeekBar);
        powerValue = findViewById(R.id.powerValue);
        angleValue = findViewById(R.id.angleValue);
        statusText = findViewById(R.id.statusText);

        // Set initial values
        powerSeekBar.setProgress(prefs.getInt("power", 50));
        angleSeekBar.setProgress(prefs.getInt("angle", 90));

        // Auto-play button
        autoPlayBtn.setOnClickListener(v -> toggleAutoPlay());

        // Start overlay service
        startServiceBtn.setOnClickListener(v -> startOverlayService());

        // Settings button
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // Overlay toggle
        overlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (canDrawOverlay()) {
                    startOverlayService();
                } else {
                    Toast.makeText(this, "Please enable overlay permission in settings", Toast.LENGTH_SHORT).show();
                    overlaySwitch.setChecked(false);
                    openOverlaySettings();
                }
            } else {
                stopOverlayService();
            }
        });

        // Power seek bar listener
        powerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int power = progress;
                powerValue.setText(power + "%");
                prefs.edit().putInt("power", power).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Angle seek bar listener
        angleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int angle = progress;
                angleValue.setText(angle + "°");
                prefs.edit().putInt("angle", angle).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateStatus();
    }

    private void toggleAutoPlay() {
        autoPlayActive = !autoPlayActive;
        
        if (autoPlayActive) {
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Please enable Accessibility Service in settings", Toast.LENGTH_LONG).show();
                openAccessibilitySettings();
                autoPlayActive = false;
                return;
            }
            
            autoPlayBtn.setText("AUTO-PLAY: ON");
            autoPlayBtn.setBackgroundColor(getResources().getColor(R.color.active_green));
            statusText.setText("🟢 AUTO-PLAY ACTIVE - Ready to strike!");
            
            // Send broadcast to start auto-play
            Intent intent = new Intent("CARROM_START_AUTOPLAY");
            intent.putExtra("power", powerSeekBar.getProgress());
            intent.putExtra("angle", angleSeekBar.getProgress());
            sendBroadcast(intent);
        } else {
            autoPlayBtn.setText("AUTO-PLAY: OFF");
            autoPlayBtn.setBackgroundColor(getResources().getColor(R.color.inactive_red));
            statusText.setText("⚪ Idle - Tap AUTO-PLAY to begin");
            
            Intent intent = new Intent("CARROM_STOP_AUTOPLAY");
            sendBroadcast(intent);
        }
    }

    private void startOverlayService() {
        if (canDrawOverlay()) {
            Intent serviceIntent = new Intent(this, OverlayService.class);
            startService(serviceIntent);
            Toast.makeText(this, "Overlay Service Started", Toast.LENGTH_SHORT).show();
        } else {
            openOverlaySettings();
        }
    }

    private void stopOverlayService() {
        Intent serviceIntent = new Intent(this, OverlayService.class);
        stopService(serviceIntent);
    }

    private boolean canDrawOverlay() {
        return Settings.canDrawOverlays(this);
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        startActivity(intent);
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services != null) {
                return services.contains(getPackageName() + "/" + CarromAccessibilityService.class.getName());
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private void updateStatus() {
        if (isAccessibilityServiceEnabled()) {
            statusText.setText("✅ Accessibility Service: ENABLED");
        } else {
            statusText.setText("❌ Accessibility Service: DISABLED");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
