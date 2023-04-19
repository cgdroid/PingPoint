package com.tmhnry.pingpoint;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.tmhnry.pingpoint.database.Firebase;
import com.tmhnry.pingpoint.databinding.ActivityMainBinding;
import com.tmhnry.pingpoint.fragment.AccountFragment;
import com.tmhnry.pingpoint.fragment.AdminHomeFragment;
import com.tmhnry.pingpoint.fragment.EmployeeHomeFragment;
import com.tmhnry.pingpoint.fragment.HomeFragment;
import com.tmhnry.pingpoint.model.Attendance;
import com.tmhnry.pingpoint.model.Company;
import com.tmhnry.pingpoint.model.Entity;
import com.tmhnry.pingpoint.model.Model;
import com.tmhnry.pingpoint.model.User;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements Model.FirebaseQueryListener, AccountFragment.OnCompanySetup {
    public static final int SIGN_UP_COMPANY = 0;
    public static final int SIGN_UP_ENTITY = 6;
    public static final int SIGN_IN_COMPANY = 7;
    public static final int GET_COMPANY = 4;
    public static final int GET_USER = 3;
    public static final int GET_ENTITY = 5;
    public static final int GET_ATTENDANCE = 10;

    ActivityMainBinding binding;
    String fragmentTag;
    Map<String, float[][][]> imageData;
    SQLiteDatabase db;
    LoggingBenchmark benchmark;
    Dialog loading;
    Dialog prompt;
    Dialog accountType;
    Dialog logOut;
    TextView code;
    StringBuilder codeSb;
    String type;
    TextView alertPos, alertNeg;
    TextView alertTitle, alertMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        Firebase.Create();
//        User.initModels(this);
//        User.signOut();

        Chrono.Create(Locale.getDefault());
        Firebase.Create();
        FirebaseUser user = Firebase.getUser();

        setLoadingDialog();

        setLogOutDialog();

        setAccountTypeDialog();

        setPromptDialog();

        if (user == null) {
            startActivity(new Intent(this, AuthenticationActivity.class));
            finish();
            return;
        }

        Company.initModels(this);
        User.initModels(this);
        Entity.initModels(this);
        Attendance.initModels(this);

        if (User.getModels().size() < 1) {
            Map<String, Object> data = Model.Map(User.EMAIL_ADDRESS, user.getEmail());
            Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, GET_USER, Keys.DATA, data);
            User.retrieve(args);
        } else {
//            Map<String, Object> data = Model.Map(Entity.USER_KEY, User.getKey());
//            Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, GET_ENTITY, Keys.DATA, data);
//            Entity.retrieve(args);
            Map<String, Object> attendanceData = Model.Map(Attendance.ENTITY_KEY, User.getKey());
            Map<String, Object> attendanceArgs = Model.Map(Keys.REQUEST_CODE, GET_ATTENDANCE, Keys.DATA, attendanceData);
            Attendance.retrieve(attendanceArgs);
        }

//        Attendance.initModels(this);
//        Attendance.retrieve(this);

        imageData = new HashMap<>();

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            switch (itemId) {
                case R.id.nav_home:
                    loadHomeFragment();
                    break;
                default:
                    loadFragment(AccountFragment.TAG);
            }
            return true;
        });

        binding.bottomNavigation.setOnItemReselectedListener(item -> {
            return;
        });

        loadHomeFragment();
        Context context = getApplicationContext();
        VisionModelProvider.Create().connectToDB(context);
        TransferLearningModelWrapper model = new TransferLearningModelWrapper(context);
        VisionModelProvider.setModel(model).retrieveLocalSamples(context);

        benchmark = VisionModelProvider.getBenchmark();
    }

    public void openLogOutDialog() {
        logOut.show();
    }

    private void setLogOutDialog() {
        logOut = new Dialog(this);
        logOut.setContentView(R.layout.dialog_alert_notification);
        logOut.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView alertPos, alertNeg;
        TextView alertTitle, alertMessage;
        alertPos = logOut.findViewById(R.id.alert_pos);
        alertTitle = logOut.findViewById(R.id.alert_title);
        alertNeg = logOut.findViewById(R.id.alert_neg);
        alertMessage = logOut.findViewById(R.id.alert_message);

        alertTitle.setText("Continue?");
        alertMessage.setText("Logging out will erase all your local data, including the images used to identify your account. Continue?");

        alertPos.setOnClickListener(view -> {
            logOut.cancel();
            User.signOut();
            VisionModelProvider.deleteData(getApplicationContext());
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
        });

        alertNeg.setOnClickListener(view -> {
            logOut.cancel();
        });
    }

    private void setPromptDialog() {
        prompt = new Dialog(this);
        prompt.setContentView(R.layout.dialog_alert_notification);
        prompt.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertTitle = prompt.findViewById(R.id.alert_title);
        alertPos = prompt.findViewById(R.id.alert_pos);
        alertNeg = prompt.findViewById(R.id.alert_neg);

        alertPos.setOnClickListener(view -> {
            prompt.cancel();
            navigate(R.id.nav_account);
            loadFragment(AccountFragment.TAG);
        });
        alertNeg.setOnClickListener(view -> {
            prompt.cancel();
        });
        alertMessage = prompt.findViewById(R.id.alert_message);
    }

    private void setLoadingDialog() {
        loading = new Dialog(this);
        loading.setCancelable(false);
        loading.setContentView(R.layout.dialog_loading_indicator);
        loading.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void setAccountTypeDialog() {
        accountType = new Dialog(this);
        accountType.setContentView(R.layout.dialog_account_type);
        accountType.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        AppCompatButton admin = accountType.findViewById(R.id.admin);
        AppCompatButton member = accountType.findViewById(R.id.member);
        TextInputEditText editName = accountType.findViewById(R.id.edit_name);
        TextInputEditText editCode = accountType.findViewById(R.id.edit_code);
        TextInputEditText editAddress = accountType.findViewById(R.id.edit_address);
        code = accountType.findViewById(R.id.code);

        type = Entity.ADMIN;
        admin.setActivated(true);
        member.setActivated(false);

        admin.setOnClickListener(view -> {
            type = Entity.ADMIN;
            admin.setActivated(true);
            member.setActivated(false);
            admin.setBackgroundColor(Color.GRAY);
            admin.setTextColor(Color.WHITE);
            member.setBackgroundColor(Color.WHITE);
            member.setTextColor(Color.BLACK);
            editName.setVisibility(View.VISIBLE);
            editCode.setVisibility(View.GONE);
            editAddress.setVisibility(View.VISIBLE);
            code.setVisibility(View.VISIBLE);
        });

        member.setOnClickListener(view -> {
            type = Entity.MEMBER;
            admin.setActivated(false);
            member.setActivated(true);
            member.setBackgroundColor(Color.GRAY);
            member.setTextColor(Color.WHITE);
            admin.setBackgroundColor(Color.WHITE);
            admin.setTextColor(Color.BLACK);
            editName.setVisibility(View.GONE);
            editCode.setVisibility(View.VISIBLE);
            editAddress.setVisibility(View.GONE);
            code.setVisibility(View.GONE);
        });

        AppCompatButton proceed = accountType.findViewById(R.id.proceed);
        proceed.setOnClickListener(view -> {
            if (member.isActivated()) {
                String code = editCode.getText().toString().trim();
                Map<String, Object> data = new HashMap<>();
                data.put(Company.CODE, code);
                Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, SIGN_IN_COMPANY, Keys.DATA, data);
                Company.retrieve(args);
            } else {
                Map<String, Object> data = new HashMap<>();
                String name = editName.getText().toString();
                String address = editAddress.getText().toString();
                data.put(Company.USER_KEY, User.getKey());
                data.put(Company.CODE, codeSb.toString());
                data.put(Company.NAME, name);
                data.put(Company.ADDRESS, address);
                Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, SIGN_UP_COMPANY, Keys.DATA, data);
                Company.signUp(args);
            }
            accountType.dismiss();
        });
    }

    public void openAccountTypeDialog() {
        codeSb = new StringBuilder();
        Random rand = new Random();
        while (codeSb.length() < 5) {
            codeSb.append(rand.nextInt(9));
        }
        code.setText("COMPANY CODE: " + codeSb.toString());
        accountType.show();
    }

    public void navigate(int navId) {
        binding.bottomNavigation.setSelectedItemId(navId);
    }

    public void onShowDialog(String message) {
        ((TextView) loading.findViewById(R.id.txt_loading_message)).setText(message);
        loading.show();
    }

    public void loadHomeFragment() {
        loadFragment(HomeFragment.TAG);
//        if (Entity.getModels().size() == 0) {
//            loadFragment(AdminHomeFragment.TAG);
//            return;
//        }
//        if (Entity.getType().equals(Entity.ADMIN)) {
//            loadFragment(AdminHomeFragment.TAG);
//            return;
//        }

//        loadFragment(EmployeeHomeFragment.TAG);

    }

    public void loadFragment(String fragmentTag) {
        this.fragmentTag = fragmentTag;

        Fragment fragment;
        switch (fragmentTag) {
            case AdminHomeFragment.TAG:
                fragment = AdminHomeFragment.Builder();
                break;
            case EmployeeHomeFragment.TAG:
                fragment = EmployeeHomeFragment.Builder();
                break;
            default:
                fragment = HomeFragment.Builder();
        }

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .replace(binding.fragment.getId(), fragment, fragmentTag)
                .commit();
    }

    @Override
    public void onStartQuery(String name, int requestCode) {
        if (name.equals(User.TABLE_NAME) && requestCode == GET_USER) {
            onShowDialog("Retrieving user data...");
        }
        if (name.equals(Entity.TABLE_NAME) && requestCode == GET_ENTITY) {
            onShowDialog("Retrieving user data...");
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == SIGN_UP_COMPANY) {
            onShowDialog("Adding company to the database...");
        }
        if (name.equals(Entity.TABLE_NAME) && requestCode == SIGN_UP_ENTITY) {
            onShowDialog("Linking user data to the company...");
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == SIGN_IN_COMPANY) {
            onShowDialog("Retrieving company data...");
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == GET_COMPANY) {
            onShowDialog("Retrieving company data...");
        }
        if (name.equals(Attendance.TABLE_NAME) && requestCode == GET_ATTENDANCE) {
            onShowDialog("Fetching attendance data...");
        }
    }

    @Override
    public void onSuccessQuery(String name, int requestCode) {
        if (name.equals(User.TABLE_NAME) && requestCode == GET_USER) {
//            Map<String, Object> data = Model.Map(Entity.USER_KEY, User.getKey());
//            Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, GET_ENTITY, Keys.DATA, data);
            Map<String, Object> attendanceData = Model.Map(Attendance.ENTITY_KEY, User.getKey());
            Map<String, Object> attendanceArgs = Model.Map(Keys.REQUEST_CODE, GET_ATTENDANCE, Keys.DATA, attendanceData);
            Attendance.retrieve(attendanceArgs);
//            Entity.retrieve(args);
        }
        if (name.equals(Entity.TABLE_NAME) && requestCode == GET_ENTITY) {
            Map<String, Object> data = Model.Map(Company.CODE, Entity.getCompanyCode());
            Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, GET_COMPANY, Keys.DATA, data);
            Company.retrieve(args);
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == SIGN_IN_COMPANY) {
            Map<String, Object> data = new HashMap<>();
            data.put(Entity.ADDRESS, "");
            data.put(Entity.MOBILE, User.getMobileNumber());
            data.put(Entity.GENDER, Entity.Gender.MALE);
            data.put(Entity.USER_KEY, User.getKey());
            data.put(Entity.COMPANY_NAME, Company.getName());
            data.put(Entity.COMPANY_KEY, Company.getKey());
            data.put(Entity.COMPANY_CODE, Company.getCode());
            data.put(Entity.JOIN_DATE, Chrono.now());
            data.put(Entity.FULL_NAME, User.getFullName());
            data.put(Entity.TYPE, Entity.MEMBER);
            data.put(Entity.KEY, Entity.requestKey());
            Entity.append(Model.List(Entity.Model(data)), SIGN_UP_ENTITY);
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == SIGN_UP_COMPANY) {
            Map<String, Object> data = new HashMap<>();
            data.put(Entity.ADDRESS, "");
            data.put(Entity.MOBILE, User.getMobileNumber());
            data.put(Entity.GENDER, Entity.Gender.MALE);
            data.put(Entity.USER_KEY, User.getKey());
            data.put(Entity.COMPANY_NAME, Company.getName());
            data.put(Entity.COMPANY_KEY, Company.getKey());
            data.put(Entity.COMPANY_CODE, Company.getCode());
            data.put(Entity.JOIN_DATE, Chrono.now());
            data.put(Entity.FULL_NAME, User.getFullName());
            data.put(Entity.TYPE, Entity.ADMIN);
            data.put(Entity.KEY, Entity.requestKey());
            Entity.append(Model.List(Entity.Model(data)), SIGN_UP_ENTITY);
        }
        if (name.equals(Entity.TABLE_NAME) && requestCode == SIGN_UP_ENTITY) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(AccountFragment.TAG);
            if (fragment != null) {
                ((AccountFragment) fragment).updateCompanyButton();
            }
            loading.dismiss();
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == GET_COMPANY) {
//            Map<String, Object> data;
//            if (Entity.getType().equals(Entity.ADMIN)) {
//                data = Model.Map(Attendance.TARGET_KEY, Company.getKey());
//            } else {
//                data = Model.Map(Attendance.ENTITY_KEY, User.getKey());
//            }
//            Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, GET_ATTENDANCE, Keys.DATA, data);
//            Attendance.retrieve(args);
        }
        if (name.equals(Attendance.TABLE_NAME) && requestCode == GET_ATTENDANCE) {
            Fragment home = getSupportFragmentManager().findFragmentByTag(HomeFragment.TAG);
            Fragment adminHome = getSupportFragmentManager().findFragmentByTag(AdminHomeFragment.TAG);
            if (home != null) {
                ((HomeFragment) home).updateAttendances();
            }
            if (adminHome != null) {
                ((AdminHomeFragment) adminHome).updateAttendances();
            }
            loading.dismiss();
        }
    }

    @Override
    public void onFailQuery(String name, int requestCode) {
        if (name.equals(Entity.TABLE_NAME) && requestCode == GET_ENTITY) {
            loading.dismiss();
            alertTitle.setText("Company not found");
            alertMessage.setText("This account is not associated to any company. Do you want to continue setting up?");
            prompt.show();
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == GET_COMPANY) {
            loading.dismiss();
            alertTitle.setText("Company not found");
            alertMessage.setText("This account is not associated to any company. Do you want to continue setting up?");
            prompt.show();
        }
        if (name.equals(Entity.TABLE_NAME) && requestCode == SIGN_UP_ENTITY) {
            loading.dismiss();
            alertTitle.setText("Entity registration failed");
            alertMessage.setText("An error was encountered while linking to the company");
            prompt.show();
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == SIGN_UP_COMPANY) {
            loading.dismiss();
            alertTitle.setText("Company registration failed");
            alertMessage.setText("The provided code already exists. Please try again...");
            prompt.show();
        }
        if (name.equals(Company.TABLE_NAME) && requestCode == SIGN_IN_COMPANY) {
            loading.dismiss();
            alertTitle.setText("Company not found");
            alertMessage.setText("The provided code doesn't belong to a company. Are you sure about the code?");
            prompt.show();
        }
        if (name.equals(Attendance.TABLE_NAME) && requestCode == GET_ATTENDANCE) {
            loading.dismiss();
        }
        if (requestCode == -1) {
            loading.dismiss();
            alertTitle.setText("Unexpected error");
            alertMessage.setText("An unexpected error has occurred. Please try again later...");
            prompt.show();
        }
    }

    @Override
    public void onCompanySetup() {
        openAccountTypeDialog();
    }
}