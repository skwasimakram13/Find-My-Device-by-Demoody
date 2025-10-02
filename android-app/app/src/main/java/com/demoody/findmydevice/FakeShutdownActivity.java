package com.demoody.findmydevice;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.demoody.findmydevice.utils.PreferenceManager;

public class FakeShutdownActivity extends Activity {
    private static final String TAG = "FakeShutdownActivity";
    private static final long DELAY_BEFORE_PASSWORD_PROMPT = 3000; // 3 seconds
    
    private PreferenceManager preferenceManager;
    private LinearLayout passwordPrompt;
    private EditText passwordInput;
    private Button confirmBtn;
    private Button cancelBtn;
    private ImageView shutdownIcon;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_shutdown);
        
        preferenceManager = new PreferenceManager(this);
        
        // Make this activity appear over lock screen and other apps
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                           WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                           WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                           WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        initializeViews();
        startFakeShutdownSequence();
    }
    
    private void initializeViews() {
        passwordPrompt = findViewById(R.id.passwordPrompt);
        passwordInput = findViewById(R.id.passwordInput);
        confirmBtn = findViewById(R.id.confirmBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        shutdownIcon = findViewById(R.id.shutdownIcon);
        
        confirmBtn.setOnClickListener(v -> handlePasswordSubmit());
        cancelBtn.setOnClickListener(v -> handleCancel());
    }
    
    private void startFakeShutdownSequence() {
        // Show fake shutdown animation for a few seconds
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            // Show password prompt after delay
            passwordPrompt.setVisibility(View.VISIBLE);
            passwordInput.requestFocus();
        }, DELAY_BEFORE_PASSWORD_PROMPT);
    }
    
    private void handlePasswordSubmit() {
        String enteredPassword = passwordInput.getText().toString();
        String correctPassword = preferenceManager.getFakeShutdownPassword();
        
        if (correctPassword.equals(enteredPassword)) {
            // Correct password - allow "shutdown" (actually just close the app)
            Toast.makeText(this, "Shutting down...", Toast.LENGTH_SHORT).show();
            finishAffinity(); // Close all activities
        } else {
            // Wrong password
            Toast.makeText(this, getString(R.string.fake_shutdown_wrong_password), Toast.LENGTH_SHORT).show();
            passwordInput.setText("");
            passwordInput.requestFocus();
            
            // Optionally, you could implement a delay or limit attempts
        }
    }
    
    private void handleCancel() {
        // Cancel shutdown - return to normal operation
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from closing the fake shutdown
        // In a real theft scenario, the thief shouldn't be able to easily bypass this
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Try to stay visible even when paused
        // Note: This behavior might be restricted on newer Android versions
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // The real system shutdown will proceed regardless of this activity
        // This is just a deterrent for casual thieves
    }
}