package com.tmhnry.pingpoint;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.tmhnry.pingpoint.databinding.ActivityAuthBinding;
import com.tmhnry.pingpoint.fragment.LoginFragment;
import com.tmhnry.pingpoint.fragment.RegisterFragment;
import com.tmhnry.pingpoint.model.Model;
import com.tmhnry.pingpoint.model.User;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationActivity extends AppCompatActivity implements Model.FirebaseQueryListener {
    public static int SIGN_IN = 1;
    public static int SIGN_UP = 2;
    private ActivityAuthBinding binding;

    private Dialog loading;
    private Dialog alert;

    private TextView alertPos, alertNeg;
    private TextView alertTitle, alertMessage;

    Map<String, Boolean> querySuccessful;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        querySuccessful = new HashMap<>();
        User.initModels(this);

        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loading = new Dialog(this);
        loading.setCancelable(false);
        loading.setContentView(R.layout.dialog_loading_indicator);
        loading.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alert = new Dialog(this);
        alert.setContentView(R.layout.dialog_alert_notification);
        alert.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertPos = alert.findViewById(R.id.alert_pos);
        alertNeg = alert.findViewById(R.id.alert_neg);
        alertMessage = alert.findViewById(R.id.alert_message);
        alertTitle = alert.findViewById(R.id.alert_title);
        alertNeg.setVisibility(View.INVISIBLE);
        loadFragment(LoginFragment.TAG, null);
    }


    private void startNextActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void onShowDialog(String message) {
        ((TextView) loading.findViewById(R.id.txt_loading_message)).setText(message);
        loading.show();
    }

    public void loadFragment(String fragmentId, Map<String, Object> data) {
        Fragment fragment;
        if (fragmentId.equals(LoginFragment.TAG)) {
            fragment = LoginFragment.Builder(null);
        } else {
            fragment = RegisterFragment.Builder(null);
        }
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .replace(binding.fragment.getId(), fragment)
                .commit();
    }

    private boolean allQueriesCompleted() {
        boolean complete = true;
        for (Boolean queryComplete : querySuccessful.values()) {
            if (!queryComplete) {
                complete = false;
                break;
            }
        }
        return complete;
    }


    private void setState() {
        // checks query status every 1 second
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (allQueriesCompleted()) {
                    handler.removeCallbacks(this);
                    handler = null;
                    return;
                }
                setState();
            }
        }, 1000);
    }

    @Override
    public void onStartQuery(String name, int requestCode) {
        if (name.equals(User.TABLE_NAME) && requestCode == SIGN_IN) {
            onShowDialog("Loading...");
        }
        if (name.equals(User.TABLE_NAME) && requestCode == SIGN_UP) {
            onShowDialog("Loading...");
        }
//        querySuccessful.put(name, false);
//        if (name.equals(Company.TABLE_NAME)) {
//            Toast.makeText(this, "Checking if code is valid. Please wait", Toast.LENGTH_SHORT).show();
//        }
    }


    @Override
    public void onFailQuery(String name, int requestCode) {
        if (name.equals(User.TABLE_NAME) && requestCode == SIGN_IN) {
            loading.dismiss();
            alertTitle.setText("Invalid Credentials");
            alertMessage.setText("Your email address or password is incorrect.");
            alertPos.setText("OKAY");
            alertPos.setOnClickListener(view -> {
                alert.dismiss();
            });
            alertNeg.setVisibility(View.INVISIBLE);
            alert.show();
        }
        if (name.equals(User.TABLE_NAME) && requestCode == -1) {
            loading.dismiss();
            alertTitle.setText("Processing Failed");
            alertMessage.setText("An error occurred during registration. Please try again.");
            alertPos.setText("OKAY");
            alertPos.setOnClickListener(view -> {
                alert.dismiss();
            });
            alertNeg.setVisibility(View.INVISIBLE);
            alert.show();
        }
        if (name.equals(User.TABLE_NAME) && requestCode == SIGN_UP) {
            loading.dismiss();
            alertTitle.setText("Email Already Exists");
            alertMessage.setText("A user with that email has already existed. Please provide a different email address.");
            alertPos.setText("OKAY");
            alertPos.setOnClickListener(view -> {
                alert.dismiss();
            });
            alertNeg.setVisibility(View.INVISIBLE);
            alert.show();
        }
    }

    @Override
    public void onSuccessQuery(String name, int requestCode) {
        querySuccessful.put(name, true);
//        It is important to call this before setState else setState all queries prior to this call
//        may be finished causing the dialog to be cancelled
//        if (name.equals(Company.TABLE_NAME)) {
//            accountType.dismiss();
//        }
        if (name.equals(User.TABLE_NAME) && requestCode == SIGN_IN) {
            startNextActivity();
            loading.dismiss();
        }
        if (name.equals(User.TABLE_NAME) && requestCode == SIGN_UP) {
            loading.dismiss();
            alertTitle.setText("Registration Successful");
            alertMessage.setText("Do you want to log in with these credentials?");
            alertPos.setText("CONTINUE");
            alertPos.setOnClickListener(view -> {
                alert.dismiss();
                User user = User.getModels().values().stream().findAny().get();
                Map<String, Object> data = new HashMap<>();
                data.put(User.EMAIL_ADDRESS, user.emailAddress);
                data.put(User.PASSWORD, user.password);
                Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, AuthenticationActivity.SIGN_IN, Keys.DATA, data);
                User.signIn(args);
//                UserOld.login(userCredentials, loginListener);
            });
            alertNeg.setVisibility(View.VISIBLE);
            alertNeg.setText("CANCEL");
            alertNeg.setOnClickListener(view -> {
                User.signOut();
                alert.dismiss();
            });
            alert.show();
        }
        if (handler != null) {
            return;
        }
        handler = new Handler();
        setState();
    }

    @Override
    public void onBackPressed() {
    }
}
