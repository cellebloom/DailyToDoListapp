package com.example.dailytodolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import com.example.dailytodolist.util.ThemeColors;

public class LoginActivity extends BaseActivity {

    private EditText emailInput, passwordInput;
    private TextView actionButton, forgotPasswordLink, bottomLink, guestLink, eyeIcon;
    private TextView checkIcon, appTitle, tagline;
    private LinearLayout googleButton, innerGradientIcon, outerIconFrame;
    private CheckBox rememberCheckbox;
    private LinearLayout mainLayout, loginCard;
    private LinearLayout emailFieldContainer, passwordFieldContainer;
    private boolean isPasswordVisible = false;
    private boolean isDarkMode = false;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // Add GoogleSignInHelper variable
    private GoogleSignInHelper googleSignInHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        isDarkMode = ThemeManager.isDarkMode(this);

        initViews();
        applyAllGradients();
        loadSavedCredentials();
        applyDarkMode();

        // Initialize Google Sign-In
        initGoogleSignIn();

        setupClickListeners();

        // Update header text with string resources
        if (appTitle != null) {
            appTitle.setText(R.string.app_name);
        }
        if (tagline != null) {
            tagline.setText(R.string.tagline);
        }
    }

    private void initGoogleSignIn() {
        googleSignInHelper = GoogleSignInHelper.getInstance();
        googleSignInHelper.init(this, new GoogleSignInHelper.GoogleSignInCallback() {
            @Override
            public void onSuccess(GoogleSignInAccount account) {
                // Google sign in successful
                String userId = account.getId();
                String userName = account.getDisplayName();
                String userEmail = account.getEmail();

                if (userName == null || userName.isEmpty()) {
                    userName = userEmail != null ? userEmail.split("@")[0] : "GoogleUser";
                }

                UserManager.getInstance().setUser(UserManager.AccountType.GOOGLE, userId, userName, userEmail);
                UserManager.getInstance().saveSession(LoginActivity.this);

                Toast.makeText(LoginActivity.this, getString(R.string.welcome_back, userName), Toast.LENGTH_SHORT).show();

                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        mainLayout = findViewById(R.id.mainLayout);
        loginCard = findViewById(R.id.loginCard);
        innerGradientIcon = findViewById(R.id.innerGradientIcon);
        outerIconFrame = findViewById(R.id.outerIconFrame);

        // Initialize header elements
        checkIcon = findViewById(R.id.checkIcon);
        appTitle = findViewById(R.id.appTitle);
        tagline = findViewById(R.id.tagline);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        // Get the container LinearLayouts for email and password
        emailFieldContainer = (LinearLayout) emailInput.getParent();
        passwordFieldContainer = (LinearLayout) passwordInput.getParent();

        actionButton = findViewById(R.id.actionButton);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        bottomLink = findViewById(R.id.bottomLink);
        guestLink = findViewById(R.id.guestLink);
        eyeIcon = findViewById(R.id.eyeIcon);

        googleButton = findViewById(R.id.googleButton);
        rememberCheckbox = findViewById(R.id.rememberCheckbox);

        // Set hint text from resources
        if (emailInput != null) {
            emailInput.setHint(R.string.email_hint);
        }
        if (passwordInput != null) {
            passwordInput.setHint(R.string.password_hint);
        }
        if (rememberCheckbox != null) {
            rememberCheckbox.setText(R.string.remember_me);
        }
        if (actionButton != null) {
            actionButton.setText(R.string.login);
        }
        if (guestLink != null) {
            guestLink.setText(R.string.continue_as_guest);
        }
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setText(R.string.forgot_password);
        }

        TextView googleText = findViewById(R.id.googleText);
        if (googleText != null) {
            googleText.setText(R.string.sign_in_with_google);
        }

        TextView orText = findViewById(R.id.orText);
        if (orText != null) {
            orText.setText(R.string.or);
        }

        TextView orContinueText = findViewById(R.id.orContinueText);
        if (orContinueText != null) {
            orContinueText.setText(R.string.or_continue_as);
        }

        TextView dontHaveAccount = findViewById(R.id.dontHaveAccount);
        if (dontHaveAccount != null) {
            dontHaveAccount.setText(R.string.dont_have_account);
        }

        if (bottomLink != null) {
            bottomLink.setText(R.string.sign_up);
        }

        TextView emailLabel = findViewById(R.id.emailLabel);
        if (emailLabel != null) {
            emailLabel.setText(R.string.email);
        }

        TextView passwordLabel = findViewById(R.id.passwordLabel);
        if (passwordLabel != null) {
            passwordLabel.setText(R.string.password);
        }
    }

    private void applyDarkMode() {
        isDarkMode = ThemeManager.isDarkMode(this);

        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];
        // The whole login screen sits on the theme gradient, so the header text
        // is chosen from that gradient instead of a fixed dark/light pair.
        int onGradient = ThemeColors.onGradient(gradientColors[0], gradientColors[1]);

        if (checkIcon != null) checkIcon.setTextColor(onGradient);
        if (appTitle != null) appTitle.setTextColor(onGradient);
        if (tagline != null) tagline.setTextColor(onGradient);

        // Icon frame: themed surface with a themed hairline border.
        if (outerIconFrame != null) {
            GradientDrawable frameBg = new GradientDrawable();
            frameBg.setShape(GradientDrawable.RECTANGLE);
            frameBg.setCornerRadius(30 * getResources().getDisplayMetrics().density);
            frameBg.setColor(ThemeColors.surface(outerIconFrame));
            frameBg.setStroke(2, ThemeColors.outline(outerIconFrame));
            outerIconFrame.setBackground(frameBg);
        }

        // Login card background.
        if (loginCard != null) {
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
            cardBg.setColor(ThemeColors.surface(loginCard));
            cardBg.setStroke(1, ThemeColors.outline(loginCard));
            loginCard.setBackground(cardBg);
        }

        // Email and Password field containers.
        if (emailFieldContainer != null) {
            emailFieldContainer.setBackground(fieldBackground(emailFieldContainer));
        }
        if (passwordFieldContainer != null) {
            passwordFieldContainer.setBackground(fieldBackground(passwordFieldContainer));
        }

        // Text colors.
        TextView emailLabel = findViewById(R.id.emailLabel);
        TextView passwordLabel = findViewById(R.id.passwordLabel);
        TextView dontHaveAccount = findViewById(R.id.dontHaveAccount);
        TextView orText = findViewById(R.id.orText);
        TextView orContinueText = findViewById(R.id.orContinueText);
        TextView googleText = findViewById(R.id.googleText);

        if (emailLabel != null) emailLabel.setTextColor(ThemeColors.onSurface(emailLabel));
        if (passwordLabel != null) passwordLabel.setTextColor(ThemeColors.onSurface(passwordLabel));
        if (dontHaveAccount != null) dontHaveAccount.setTextColor(ThemeColors.onSurfaceVariant(dontHaveAccount));
        if (orText != null) orText.setTextColor(ThemeColors.onSurfaceVariant(orText));
        if (orContinueText != null) orContinueText.setTextColor(ThemeColors.onSurfaceVariant(orContinueText));
        if (googleText != null) googleText.setTextColor(ThemeColors.onSurface(googleText));

        // EditText styles.
        if (emailInput != null) {
            emailInput.setTextColor(ThemeColors.onSurface(emailInput));
            emailInput.setHintTextColor(ThemeColors.onSurfaceVariant(emailInput));
        }
        if (passwordInput != null) {
            passwordInput.setTextColor(ThemeColors.onSurface(passwordInput));
            passwordInput.setHintTextColor(ThemeColors.onSurfaceVariant(passwordInput));
        }

        // Checkbox text and tint.
        if (rememberCheckbox != null) {
            rememberCheckbox.setTextColor(ThemeColors.onSurface(rememberCheckbox));
            rememberCheckbox.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(ThemeColors.primary(rememberCheckbox)));
        }

        // Guest button: outlined, transparent fill.
        if (guestLink != null) {
            guestLink.setTextColor(ThemeColors.onSurfaceVariant(guestLink));
            GradientDrawable guestBg = new GradientDrawable();
            guestBg.setShape(GradientDrawable.RECTANGLE);
            guestBg.setCornerRadius(25 * getResources().getDisplayMetrics().density);
            guestBg.setStroke(2, ThemeColors.outline(guestLink));
            guestBg.setColor(android.graphics.Color.TRANSPARENT);
            guestLink.setBackground(guestBg);
        }

        // Google button: outlined, transparent fill.
        if (googleButton != null) {
            GradientDrawable googleBg = new GradientDrawable();
            googleBg.setShape(GradientDrawable.RECTANGLE);
            googleBg.setCornerRadius(25 * getResources().getDisplayMetrics().density);
            googleBg.setStroke(2, ThemeColors.outline(googleButton));
            googleBg.setColor(android.graphics.Color.TRANSPARENT);
            googleButton.setBackground(googleBg);
        }

        // Accent links.
        if (forgotPasswordLink != null) forgotPasswordLink.setTextColor(accentColor);
        if (bottomLink != null) bottomLink.setTextColor(accentColor);
    }

    /** Rounded field container painted with the recessed surface role. */
    private GradientDrawable fieldBackground(View field) {
        GradientDrawable fieldBg = new GradientDrawable();
        fieldBg.setShape(GradientDrawable.RECTANGLE);
        fieldBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        fieldBg.setColor(ThemeColors.surfaceVariant(field));
        fieldBg.setStroke(1, ThemeColors.outline(field));
        return fieldBg;
    }

    private void applyAllGradients() {
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        // 1. Apply gradient to main background
        if (mainLayout != null) {
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    gradientColors
            );
            mainLayout.setBackground(gradientDrawable);
        }

        // 2. Apply gradient to Login button
        if (actionButton != null) {
            GradientDrawable buttonGradient = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    gradientColors
            );
            buttonGradient.setCornerRadius(27 * getResources().getDisplayMetrics().density);
            actionButton.setBackground(buttonGradient);

            // The label sits on the gradient, not on the surface, so its colour
            // has to come from the gradient. Leaving the layout's colorOnSurface
            // here made the label dark-on-dark on hotpink (1.40:1), because that
            // gradient ends at pure black.
            actionButton.setTextColor(
                    ThemeColors.onGradient(gradientColors[0], gradientColors[1]));
        }

        // 3. Apply gradient to Inner Square Icon
        if (innerGradientIcon != null) {
            GradientDrawable iconGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    gradientColors
            );
            iconGradient.setCornerRadius(20 * getResources().getDisplayMetrics().density);
            innerGradientIcon.setBackground(iconGradient);
        }

        // Update accent text colors (but not in dark mode where they might be overridden)
        if (!isDarkMode) {
            if (forgotPasswordLink != null) forgotPasswordLink.setTextColor(accentColor);
            if (bottomLink != null) bottomLink.setTextColor(accentColor);
            if (eyeIcon != null) eyeIcon.setTextColor(accentColor);
        } else {
            if (eyeIcon != null) eyeIcon.setTextColor(accentColor);
        }
    }

    private void loadSavedCredentials() {
        boolean isRemembered = sharedPreferences.getBoolean("remember_me", false);
        if (isRemembered) {
            String savedEmail = sharedPreferences.getString("email", "");
            String savedPassword = sharedPreferences.getString("password", "");
            emailInput.setText(savedEmail);
            passwordInput.setText(savedPassword);
            rememberCheckbox.setChecked(true);
        }
    }

    private void setupClickListeners() {
        actionButton.setOnClickListener(v -> performLogin());

        forgotPasswordLink.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, R.string.enter_email_first, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this, getString(R.string.reset_link_sent, email), Toast.LENGTH_SHORT).show();
            }
        });

        bottomLink.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, R.string.sign_up_coming_soon, Toast.LENGTH_SHORT).show();
        });

        guestLink.setOnClickListener(v -> {
            UserManager.getInstance().setGuest();
            UserManager.getInstance().saveSession(LoginActivity.this);
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });

        googleButton.setOnClickListener(v -> {
            // Start Google Sign-In
            if (googleSignInHelper != null) {
                googleSignInHelper.signIn();
            } else {
                Toast.makeText(LoginActivity.this, "Google Sign-In not initialized", Toast.LENGTH_SHORT).show();
            }
        });

        eyeIcon.setOnClickListener(v -> togglePasswordVisibility());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (googleSignInHelper != null) {
            googleSignInHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
            eyeIcon.setText("👁");
            isPasswordVisible = false;
        } else {
            passwordInput.setTransformationMethod(null);
            eyeIcon.setText("👁‍🗨");
            isPasswordVisible = true;
        }
        passwordInput.setSelection(passwordInput.getText().length());
    }

    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.email_required));
            emailInput.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.valid_email));
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.password_required));
            passwordInput.requestFocus();
            return;
        }

        if (password.length() < 3) {
            passwordInput.setError(getString(R.string.password_min_length));
            passwordInput.requestFocus();
            return;
        }

        if (rememberCheckbox.isChecked()) {
            editor.putBoolean("remember_me", true);
            editor.putString("email", email);
            editor.putString("password", password);
            editor.apply();
        } else {
            editor.putBoolean("remember_me", false);
            editor.remove("email");
            editor.remove("password");
            editor.apply();
        }

        UserManager.getInstance().setUser(UserManager.AccountType.LOCAL,
                "local_" + email.replaceAll("[^a-zA-Z0-9]", "_"),
                email.split("@")[0], email);
        UserManager.getInstance().saveSession(this);

        Toast.makeText(this, getString(R.string.welcome_back, email.split("@")[0]), Toast.LENGTH_SHORT).show();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDarkMode = ThemeManager.isDarkMode(this);
        applyAllGradients();
        applyDarkMode();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyAllGradients();
        applyDarkMode();
    }
}