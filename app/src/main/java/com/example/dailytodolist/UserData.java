package com.example.dailytodolist;

import java.util.HashMap;
import java.util.Map;

public class UserData {
    private int coins;
    private int loginStreak;
    private long lastLoginDate;
    private Map<String, Boolean> unlockedThemes;
    private String selectedTheme;

    public UserData() {
        this.coins = 200;
        this.loginStreak = 0;
        this.lastLoginDate = 0;
        this.unlockedThemes = new HashMap<>();
        this.selectedTheme = ThemeManager.THEME_DEFAULT;
        unlockedThemes.put(ThemeManager.THEME_DEFAULT, true);
    }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    public void addCoins(int amount) { this.coins += amount; }

    public int getLoginStreak() { return loginStreak; }
    public void setLoginStreak(int streak) { this.loginStreak = streak; }

    public long getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(long date) { this.lastLoginDate = date; }

    public boolean isThemeUnlocked(String themeId) {
        return unlockedThemes.getOrDefault(themeId, false);
    }

    public void unlockTheme(String themeId) {
        unlockedThemes.put(themeId, true);
    }

    public Map<String, Boolean> getUnlockedThemes() {
        return unlockedThemes;
    }

    public String getSelectedTheme() {
        return selectedTheme;
    }

    public void setSelectedTheme(String theme) {
        this.selectedTheme = theme;
    }

    public int getThemeUnlockCost(String themeId) {
        switch (themeId) {
            case ThemeManager.THEME_HOTPINK: return 150;
            case ThemeManager.THEME_SUNSET:  return 120;
            case ThemeManager.THEME_LIME:
            case ThemeManager.THEME_MINT:    return 100;
            default:                         return 0;
        }
    }

    public void resetToDefault() {
        this.selectedTheme = ThemeManager.THEME_DEFAULT;
        this.unlockedThemes.clear();
        this.unlockedThemes.put(ThemeManager.THEME_DEFAULT, true);
    }
}
