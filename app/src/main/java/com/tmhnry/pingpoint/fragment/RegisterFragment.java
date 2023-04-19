package com.tmhnry.pingpoint.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.tmhnry.pingpoint.AuthenticationActivity;
import com.tmhnry.pingpoint.Keys;
import com.tmhnry.pingpoint.R;
import com.tmhnry.pingpoint.databinding.FragmentRegisterBinding;
import com.tmhnry.pingpoint.databinding.TextfieldBinding;
import com.tmhnry.pingpoint.model.Model;
import com.tmhnry.pingpoint.model.User;

import java.util.HashMap;
import java.util.Map;

import kotlin.text.Regex;

public class RegisterFragment extends Fragment {
    public static final String TAG = "org.tensorflow.lite.examples.transfer.fragment.register";
    FragmentRegisterBinding binding;
    private Dialog alert;
    private TextView alertPos, alertNeg;
    private TextView alertTitle, alertMessage;

    public RegisterFragment() {
    }

    public static RegisterFragment Builder(Map<String, Object> data) {
        RegisterFragment fragment = new RegisterFragment();
        Bundle args = new Bundle();
        if (data != null && data.get("code") != null) {
            args.putString("code", (String) data.get("code"));
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(inflater, container, false);

        alert = new Dialog(getActivity());

        alert.setContentView(R.layout.dialog_alert_notification);

        alert.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        alertPos = alert.findViewById(R.id.alert_pos);
        alertNeg = alert.findViewById(R.id.alert_neg);
        alertMessage = alert.findViewById(R.id.alert_message);
        alertTitle = alert.findViewById(R.id.alert_title);

        setEmailInput();
        setFullNameInput();
        setPasswordInput();
        setConfirmPasswordInput();
        setPhoneInput();

        binding.btnRegister.setOnClickListener(v -> {
            submitForm();
        });

        binding.wrapperLogin.setOnClickListener(v -> {
            ((AuthenticationActivity) getActivity()).loadFragment(LoginFragment.TAG, null);
        });

        return binding.getRoot();
    }

    private void submitForm() {
        TextfieldBinding email = binding.txtFieldEmail;
        TextfieldBinding password = binding.txtFieldPassword;
        TextfieldBinding confirmPassword = binding.txtFieldConfirmPassword;
        TextfieldBinding fullName = binding.txtFieldFullName;
        TextfieldBinding phone = binding.txtFieldPhone;

        phone.inputLayout.setHelperText(validate(R.string.phone));
        email.inputLayout.setHelperText(validate(R.string.email));
        password.inputLayout.setHelperText(validate(R.string.password));
        confirmPassword.inputLayout.setHelperText(validate(R.string.confirm_password));
        fullName.inputLayout.setHelperText(validate(R.string.full_name));

        boolean validEm = email.inputLayout.getHelperText() == null || email.inputLayout.getHelperText().toString().isEmpty();
        boolean validPw = password.inputLayout.getHelperText() == null || password.inputLayout.getHelperText().toString().isEmpty();
        boolean validFn = fullName.inputLayout.getHelperText() == null || fullName.inputLayout.getHelperText().toString().isEmpty();
        boolean validPn = phone.inputLayout.getHelperText() == null || phone.inputLayout.getHelperText().toString().isEmpty();
        boolean validCp = confirmPassword.inputLayout.getHelperText() == null || confirmPassword.inputLayout.getHelperText().toString().isEmpty();

        if (validEm && validPw && validFn && validCp && validPn) {
            String em = email.inputEditText.getText().toString();
            String pw = password.inputEditText.getText().toString();
            String fn = fullName.inputEditText.getText().toString();
            String pn = phone.inputEditText.getText().toString();

            Map<String, Object> data = new HashMap<>();
//
            data.put(User.EMAIL_ADDRESS, em);
            data.put(User.PASSWORD, pw);
            data.put(User.FULL_NAME, fn);
            data.put(User.PHONE, pn);

            Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, AuthenticationActivity.SIGN_UP, Keys.DATA, data);
            User.signUp(args);
            resetForm();
        } else
            invalidForm();
    }

    private void resetForm() {
        alertNeg.setVisibility(View.VISIBLE);
        TextfieldBinding email = binding.txtFieldEmail;
        TextfieldBinding password = binding.txtFieldPassword;
        alertTitle.setText("Form submitted");
        alertMessage.setText("Processing");
        alertPos.setText("CONTINUE");
        alertPos.setOnClickListener(view -> {
            email.inputLayout.setHelperText("Required");
            password.inputLayout.setHelperText("Required");
            alert.dismiss();
        });

    }

    private void invalidForm() {
        alertNeg.setVisibility(View.INVISIBLE);
        alertTitle.setText("Invalid Format");
        alertMessage.setText("You have provided an invalid format. Please try again.");
        alertPos.setText("OKAY");
        alertPos.setOnClickListener(view -> {
            alert.dismiss();
        });
        alert.show();
    }

    private String validate(int id) {
        String helperText = "";
        if (id == R.string.email) {
            String emailText = binding.txtFieldEmail
                    .inputEditText
                    .getText()
                    .toString()
                    .trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                return "Please provide a valid email";
            }
        }
        if (id == R.string.password) {
            String passwordText = binding.txtFieldPassword
                    .inputEditText
                    .getText()
                    .toString()
                    .trim();
            if (passwordText.length() < 8) {
                return "Password must be at least 8-characters long";
            }
            if (!passwordText.matches(String.valueOf(new Regex(".*[A-Z].*")))) {
                return "Password must contain at least 1 upper-case character";
            }
            if (!passwordText.matches(String.valueOf(new Regex(".*[a-z].*")))) {
                return "Password must contain 1 lower-case character";
            }
            if (!passwordText.matches(String.valueOf(new Regex(".*[@#\\$%^&+=].*")))) {
                return "Password must contain 1 special character (@#\\$%^&+=)";
            }
        }
        if (id == R.string.confirm_password) {
            String passwordText = binding.txtFieldPassword
                    .inputEditText
                    .getText()
                    .toString()
                    .trim();
            String confirmText = binding.txtFieldConfirmPassword
                    .inputEditText
                    .getText()
                    .toString()
                    .trim();
            if(!passwordText.equals(confirmText)){
                return "Passwords don't match";
            }
        }
        if (id == R.string.full_name) {
            String firstNameText = binding.txtFieldFullName
                    .inputEditText
                    .getText()
                    .toString()
                    .trim();
            if (firstNameText.length() < 3) {
                return "Name is too short";
            }
            if (!firstNameText.matches(String.valueOf(new Regex(".*[A-Z].*")))) {
                return "Please provide at least 1 upper-case character";
            }
            if (!firstNameText.matches(String.valueOf(new Regex(".*[a-z].*")))) {
                return "Please provide at least 1 lower-case character";
            }
            if (firstNameText.matches(String.valueOf(new Regex(".*[@#\\$%^&+=].*")))) {
                return "We don't allow special characters (@#\\$%^&+=)";
            }
        }
        if (id == R.string.phone) {
            String contactText = binding.txtFieldPhone
                    .inputEditText
                    .getText()
                    .toString()
                    .trim();
            if (contactText.length() != 11) {
                return "Phone number must be 11-characters long";
            }
            if (contactText.matches(String.valueOf(new Regex(".*[A-Z].*")))) {
                return "Unfortunately, we don't allow letters";
            }
            if (contactText.matches(String.valueOf(new Regex(".*[a-z].*")))) {
                return "Unfortunately, we don't allow letters";
            }
            if (contactText.matches(String.valueOf(new Regex(".*[@#\\$%^&+=].*")))) {
                return "Sorry, we don't allow special characters (@#\\$%^&+=)";
            }
        }
        return helperText;
    }

    private void setEmailInput() {
        TextfieldBinding txtFieldBinding = binding.txtFieldEmail;
        txtFieldBinding.inputLayout.setStartIconDrawable(R.drawable.icons8_envelope_24);
        txtFieldBinding.inputLayout.setEndIconVisible(false);
        txtFieldBinding.inputLayout.setHint("EMAIL");
        txtFieldBinding.inputEditText.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                txtFieldBinding.inputLayout.setHelperText(validate(R.string.email));
            }
        });
    }

    private void setPasswordInput() {
        TextfieldBinding txtFieldBinding = binding.txtFieldPassword;
        txtFieldBinding.inputLayout.setHint("PASSWORD");
        txtFieldBinding.inputLayout.setStartIconDrawable(R.drawable.icons8_lock_24);
//        txtFieldBinding.inputLayout.setHelperText("must be a valid email");
        txtFieldBinding.inputEditText.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                txtFieldBinding.inputLayout.setHelperText(validate(R.string.password));
            }
        });
    }


    private void setConfirmPasswordInput() {
        TextfieldBinding txtFieldBinding = binding.txtFieldConfirmPassword;
        txtFieldBinding.inputLayout.setHint("CONFIRM PASSWORD");
        txtFieldBinding.inputLayout.setStartIconDrawable(R.drawable.icons8_lock_24);
//        txtFieldBinding.inputLayout.setHelperText("must be a valid email");
        txtFieldBinding.inputEditText.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                txtFieldBinding.inputLayout.setHelperText(validate(R.string.confirm_password));
            }
        });
    }

    private void setFullNameInput() {
        TextfieldBinding txtFieldBinding = binding.txtFieldFullName;
        txtFieldBinding.inputLayout.setHint("FULL NAME");
        txtFieldBinding.inputLayout.setEndIconVisible(false);
        txtFieldBinding.inputLayout.setStartIconDrawable(R.drawable.icons8_contacts_24);
        txtFieldBinding.inputEditText.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                txtFieldBinding.inputLayout.setHelperText(validate(R.string.full_name));
            }
        });
    }

    private void setPhoneInput() {
        TextfieldBinding inputField = binding.txtFieldPhone;
        inputField.inputLayout.setHint("PHONE");
        inputField.inputLayout.setEndIconVisible(false);
        inputField.inputLayout.setStartIconDrawable(R.drawable.icons8_news_24);
        inputField.inputEditText.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                inputField.inputLayout.setHelperText(validate(R.string.phone));
            }
        });
    }
}