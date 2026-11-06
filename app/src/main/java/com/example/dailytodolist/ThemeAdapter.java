package com.example.dailytodolist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    private ArrayList<ThemeItem> themeList;
    private UserData userData;
    private OnThemeClickListener listener;

    public interface OnThemeClickListener {
        void onThemeClick(String themeId);
    }

    public ThemeAdapter(ArrayList<ThemeItem> themeList, UserData userData, OnThemeClickListener listener) {
        this.themeList = themeList;
        this.userData = userData;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_theme_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThemeItem theme = themeList.get(position);
        holder.themeName.setText(theme.getName());

        String selectedThemeId = userData.getSelectedTheme();
        boolean isApplied = selectedThemeId != null && selectedThemeId.equals(theme.getId());
        boolean isOwned = userData.isThemeUnlocked(theme.getId());
        int price = getThemePrice(theme.getId());

        if (isOwned) {
            holder.themePrice.setText("OWNED");
            holder.themePrice.setTextColor(0xFF4CAF50);
        } else {
            holder.themePrice.setText(price + " COINS");
            holder.themePrice.setTextColor(0xFFFFD700);
        }

        if (isApplied) {
            holder.statusText.setText("ACTIVE");
            holder.statusText.setTextColor(0xFF4CAF50);
            holder.statusText.setBackgroundResource(R.drawable.status_applied_bg);
        } else if (isOwned) {
            holder.statusText.setText("APPLY");
            holder.statusText.setTextColor(0xFF2196F3);
        } else {
            holder.statusText.setText("NOT OWNED");
            holder.statusText.setTextColor(0xFF757575);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && isOwned && !isApplied) {
                listener.onThemeClick(theme.getId());
            }
        });
    }

    private int getThemePrice(String themeId) {
        switch (themeId) {
            case ThemeManager.THEME_HOTPINK: return 150;
            case ThemeManager.THEME_SUNSET:  return 120;
            case ThemeManager.THEME_LIME:
            case ThemeManager.THEME_MINT:    return 100;
            default:                         return 0;
        }
    }

    public void updateUserData(UserData newUserData) {
        this.userData = newUserData;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return themeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView themeName, themePrice, statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            themeName = itemView.findViewById(R.id.themeName);
            themePrice = itemView.findViewById(R.id.themePrice);
            statusText = itemView.findViewById(R.id.statusText);
        }
    }
}
