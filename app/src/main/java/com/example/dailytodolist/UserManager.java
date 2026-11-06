package com.example.dailytodolist;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {

    private static final String SESSION_PREFS = "SessionPrefs";
    private static final String KEY_ACCOUNT_TYPE = "account_type";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_EMAIL = "email";

    private static UserManager instance;

    public enum AccountType {
        GUEST, LOCAL, GOOGLE
    }

    private AccountType accountType = AccountType.GUEST;
    private String userId = "guest";
    private String displayName = "Guest";
    private String email = "";

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getAccountTypeLabel() {
        switch (accountType) {
            case GOOGLE: return "Google";
            case LOCAL:  return "Local";
            default:     return "Guest";
        }
    }

    public void setUser(AccountType type, String id, String name, String userEmail) {
        this.accountType = type;
        this.userId = id;
        this.displayName = name;
        this.email = userEmail;
    }

    public void setGuest() {
        setUser(AccountType.GUEST, "guest", "Guest", "");
    }

    public SharedPreferences getUserPrefs(Context ctx) {
        return ctx.getSharedPreferences("user_" + userId, Context.MODE_PRIVATE);
    }

    public SharedPreferences getTasksPrefs(Context ctx) {
        return ctx.getSharedPreferences("tasks_" + userId, Context.MODE_PRIVATE);
    }

    public SharedPreferences getCalendarPrefs(Context ctx) {
        return ctx.getSharedPreferences("calendar_" + userId, Context.MODE_PRIVATE);
    }

    public void saveSession(Context ctx) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_ACCOUNT_TYPE, accountType.name());
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_DISPLAY_NAME, displayName);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public boolean hasSavedSession(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE);
        return prefs.contains(KEY_ACCOUNT_TYPE);
    }

    public void loadSession(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE);
        String typeStr = prefs.getString(KEY_ACCOUNT_TYPE, "GUEST");
        try {
            accountType = AccountType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            accountType = AccountType.GUEST;
        }
        userId = prefs.getString(KEY_USER_ID, "guest");
        displayName = prefs.getString(KEY_DISPLAY_NAME, "Guest");
        email = prefs.getString(KEY_EMAIL, "");
    }

    public void clearSession(Context ctx) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        setGuest();
    }
}
