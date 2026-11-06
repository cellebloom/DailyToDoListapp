package com.example.dailytodolist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class SimpleThemeAdapter extends RecyclerView.Adapter<SimpleThemeAdapter.ViewHolder> {

    private ArrayList<String> themeNames;

    public SimpleThemeAdapter(ArrayList<String> themeNames) {
        this.themeNames = themeNames;
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
        String themeName = themeNames.get(position);
        holder.themeName.setText(themeName);

        // Set different gradients based on position
        switch (position) {
            case 0:
                holder.itemView.setBackgroundResource(R.drawable.gradient_default);
                break;
            case 1:
                holder.itemView.setBackgroundResource(R.drawable.gradient_pink);
                break;
            case 2:
                holder.itemView.setBackgroundResource(R.drawable.gradient_ocean);
                break;
            case 3:
                holder.itemView.setBackgroundResource(R.drawable.gradient_forest);
                break;
            case 4:
                holder.itemView.setBackgroundResource(R.drawable.gradient_sunset);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return themeNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView themeName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            themeName = itemView.findViewById(R.id.themeName);
        }
    }
}