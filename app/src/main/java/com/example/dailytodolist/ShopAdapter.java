package com.example.dailytodolist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {

    private ArrayList<ThemeItem> themeList;
    private UserData userData;
    private OnShopItemClickListener listener;

    public interface OnShopItemClickListener {
        void onThemePurchased(String themeId, int price);
    }

    public ShopAdapter(ArrayList<ThemeItem> themeList, UserData userData, OnShopItemClickListener listener) {
        this.themeList = themeList;
        this.userData = userData;
        this.listener = listener;
    }

    public void updateUserData(UserData newUserData) {
        this.userData = newUserData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThemeItem theme = themeList.get(position);
        holder.themeName.setText(theme.getName());

        boolean isOwned = userData.isThemeUnlocked(theme.getId());
        int price = theme.getPrice();

        if (isOwned) {
            holder.priceText.setText("OWNED");
            holder.priceText.setTextColor(0xFF4CAF50);
        } else if (price == 0) {
            holder.priceText.setText("FREE");
            holder.priceText.setTextColor(0xFF2196F3);
        } else {
            holder.priceText.setText(price + " COINS");
            holder.priceText.setTextColor(0xFFFFD700);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!isOwned && listener != null) {
                listener.onThemePurchased(theme.getId(), price);
            } else if (isOwned) {
                Toast.makeText(v.getContext(), "Already owned!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return themeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView themeName, priceText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            themeName = itemView.findViewById(R.id.themeName);
            priceText = itemView.findViewById(R.id.priceText);
        }
    }
}
