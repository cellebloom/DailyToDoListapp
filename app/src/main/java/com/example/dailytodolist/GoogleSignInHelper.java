package com.example.dailytodolist;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class GoogleSignInHelper {
    private static final String TAG = "GoogleSignInHelper";
    private static GoogleSignInHelper instance;
    private GoogleSignInClient googleSignInClient;
    private GoogleSignInCallback callback;
    private Activity activity;

    private GoogleSignInHelper() {}

    public static GoogleSignInHelper getInstance() {
        if (instance == null) {
            instance = new GoogleSignInHelper();
        }
        return instance;
    }

    public interface GoogleSignInCallback {
        void onSuccess(GoogleSignInAccount account);
        void onFailure(String errorMessage);
    }

    public void init(Activity activity, GoogleSignInCallback callback) {
        this.activity = activity;
        this.callback = callback;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestId()
                .build();

        googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).startActivityForResult(signInIntent, 1001);
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (callback != null) {
                    callback.onSuccess(account);
                }
            } catch (ApiException e) {
                Log.e(TAG, "Google sign in failed", e);
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            }
        }
    }

    public boolean isSignedIn() {
        if (activity == null) return false;
        return GoogleSignIn.getLastSignedInAccount(activity) != null;
    }

    public void signOut() {
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
    }

    public GoogleSignInAccount getCurrentAccount() {
        if (activity == null) return null;
        return GoogleSignIn.getLastSignedInAccount(activity);
    }
}