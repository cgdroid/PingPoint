package com.tmhnry.pingpoint.fragment;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.tmhnry.pingpoint.AuthenticationActivity;
import com.tmhnry.pingpoint.Keys;
import com.tmhnry.pingpoint.R;
import com.tmhnry.pingpoint.databinding.FragmentLoginBinding;
import com.tmhnry.pingpoint.databinding.TextfieldBinding;
import com.tmhnry.pingpoint.model.Model;
import com.tmhnry.pingpoint.model.User;

import java.util.HashMap;
import java.util.Map;

import kotlin.text.Regex;

public class LoginFragment extends Fragment {
    public static final String TAG = "org.tensorflow.lite.examples.transfer.fragment.login";
    private FragmentLoginBinding binding;
    private Dialog alert;
    private TextView alertPos;
    private TextView alertNeg;
    private TextView title;
    private TextView mes;

    public LoginFragment() {
        // Required empty public constructor
    }

    public static LoginFragment Builder(Map<String, Object> data) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        validateEmail();
        validatePassword();

        alert = new Dialog(getActivity());
        alert.setContentView(R.layout.dialog_alert_notification);
        alert.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        title = alert.findViewById(R.id.alert_title);
        mes = alert.findViewById(R.id.alert_message);
        alertPos = alert.findViewById(R.id.alert_pos);
        alertNeg = alert.findViewById(R.id.alert_neg);

        binding.wrapperRegister.setOnClickListener(v -> {
            ((AuthenticationActivity) getActivity()).loadFragment(RegisterFragment.TAG, null);
        });

        binding.btnLogin.setOnClickListener(v -> {
            submitForm();
        });

//        vEmail.setPaintFlags(View.INVISIBLE);
//        vPass.setPaintFlags(View.INVISIBLE);

        return binding.getRoot();
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
                return "Your password is incorrect";
            }
            if (!passwordText.matches(String.valueOf(new Regex(".*[A-Z].*")))) {
                return "Your password is incorrect";
            }
            if (!passwordText.matches(String.valueOf(new Regex(".*[a-z].*")))) {
                return "Your password is incorrect";
            }
            if (!passwordText.matches(String.valueOf(new Regex(".*[@#\\$%^&+=].*")))) {
                return "Your password is incorrect";
            }
        }

        return helperText;
    }


    //
    private void submitForm() {
        TextfieldBinding email = binding.txtFieldEmail;
        TextfieldBinding password = binding.txtFieldPassword;
        email.inputLayout.setHelperText(validate(R.string.email));
        password.inputLayout.setHelperText(validate(R.string.password));

        boolean validEmail = email.inputLayout.getHelperText() == null || email.inputLayout.getHelperText().toString().isEmpty();
        boolean validPassword = password.inputLayout.getHelperText() == null || password.inputLayout.getHelperText().toString().isEmpty();

        if (validEmail && validPassword) {
            String em = email.inputEditText.getText().toString();
            String pw = password.inputEditText.getText().toString();
            Map<String, Object> data = new HashMap<>();
            data.put(User.EMAIL_ADDRESS, em);
            data.put(User.PASSWORD, pw);
            Map<String, Object> args = Model.Map(Keys.REQUEST_CODE, AuthenticationActivity.SIGN_IN, Keys.DATA, data);
            User.signIn(args);
        } else
            invalidForm();
    }

    private void resetForm() {
        alertNeg.setVisibility(View.VISIBLE);
        TextfieldBinding email = binding.txtFieldEmail;
        TextfieldBinding password = binding.txtFieldPassword;
        title.setText("Form submitted");
        mes.setText("Processing");
        alertPos.setText("CONTINUE");
        alertNeg.setOnClickListener(view -> {
            email.inputLayout.setHelperText("Required");
            password.inputLayout.setHelperText("Required");
            alert.dismiss();
        });

    }

    //
    private void invalidForm() {
        alertNeg.setVisibility(View.INVISIBLE);
        title.setText("Invalid Format");
        mes.setText("You have provided an invalid format. Please try again.");
        alertPos.setText("OKAY");
        alertPos.setOnClickListener(view -> {
            alert.dismiss();
        });
        alert.show();
    }

    //
    private void validatePassword() {
        TextfieldBinding password = binding.txtFieldPassword;
        password.inputLayout.setHint("PASSWORD");
        password.inputLayout.setStartIconDrawable(R.drawable.icons8_lock_24);
//        textFieldBinding.inputLayout.setHelperText("must be a valid email");
        password.inputEditText.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                password.inputLayout.setHelperText(validate(R.string.password));
            }
        });
    }

    private void validateEmail() {
        TextfieldBinding email = binding.txtFieldEmail;
        email.inputLayout.setStartIconDrawable(R.drawable.icons8_envelope_24);
        email.inputLayout.setEndIconVisible(false);
        email.inputLayout.setHint("EMAIL");
        email.inputEditText.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                email.inputLayout.setHelperText(validate(R.string.email));
            }
        });
    }
}