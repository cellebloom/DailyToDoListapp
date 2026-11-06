package com.example.dailytodolist;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class GradientHelper {

    public static void applyHeaderGradient(View headerView, String themeId, Context context) {
        int[] colors = getGradientColors(themeId, context);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, // Top-left to bottom-right (135 degrees)
                colors
        );
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        headerView.setBackground(gradientDrawable);
    }

    public static void applyButtonGradient(TextView button, String themeId, Context context) {
        int[] colors = getGradientColors(themeId, context);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                colors
        );
        gradientDrawable.setCornerRadius(27 * context.getResources().getDisplayMetrics().density); // 27dp in pixels
        button.setBackground(gradientDrawable);
    }

    private static int[] getGradientColors(String themeId, Context context) {
        switch (themeId) {
            case "cream_rose":
                return new int[]{
                        ContextCompat.getColor(context, R.color.cream_rose_start),
                        ContextCompat.getColor(context, R.color.cream_rose_end)
                };
            case "cyan_hotpink":
                return new int[]{
                        ContextCompat.getColor(context, R.color.cyan_hotpink_start),
                        ContextCompat.getColor(context, R.color.cyan_hotpink_end)
                };
            case "cyan_indigo":
                return new int[]{
                        ContextCompat.getColor(context, R.color.cyan_indigo_start),
                        ContextCompat.getColor(context, R.color.cyan_indigo_end)
                };
            case "hotpink":
                return new int[]{
                        ContextCompat.getColor(context, R.color.hot_pink_start),
                        ContextCompat.getColor(context, R.color.hot_pink_end)
                };
            case "lime":
                return new int[]{
                        ContextCompat.getColor(context, R.color.lime_blue_start),
                        ContextCompat.getColor(context, R.color.lime_blue_end)
                };
            case "magenta_blue":
                return new int[]{
                        ContextCompat.getColor(context, R.color.magenta_blue_start),
                        ContextCompat.getColor(context, R.color.magenta_blue_end)
                };
            case "sunset":
                return new int[]{
                        ContextCompat.getColor(context, R.color.sunset_start),
                        ContextCompat.getColor(context, R.color.sunset_end)
                };
            case "yellow_cyan":
                return new int[]{
                        ContextCompat.getColor(context, R.color.yellow_cyan_start),
                        ContextCompat.getColor(context, R.color.yellow_cyan_end)
                };
            case "yellow_red":
                return new int[]{
                        ContextCompat.getColor(context, R.color.yellow_red_start),
                        ContextCompat.getColor(context, R.color.yellow_red_end)
                };
            case "mint":
                return new int[]{
                        ContextCompat.getColor(context, R.color.mint_primary),
                        ContextCompat.getColor(context, R.color.mint_secondary)
                };
            default:
                return new int[]{
                        ContextCompat.getColor(context, R.color.gradient_start),
                        ContextCompat.getColor(context, R.color.gradient_end)
                };
        }
    }
}