package com.example.dailytodolist;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailytodolist.model.WeekDay;
import com.example.dailytodolist.util.GradientContrast;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal day strip above the timeline, laid out like the top row of a
 * physical planner.
 *
 * <p>Renders Monday-to-Sunday for the week containing the selected day. The
 * selected cell is filled with the current theme accent; every other cell stays
 * on the plain surface colour with an outline. Tapping a cell reports the
 * tapped date back through {@link OnDaySelectedListener}, which reloads the
 * timeline for that date.</p>
 */
public class DaySelectorAdapter extends RecyclerView.Adapter<DaySelectorAdapter.DayHolder> {

    /** Notified when the user taps a day cell. */
    public interface OnDaySelectedListener {
        void onDaySelected(String dateIso);
    }

    /** Resolved once per bind cycle so a theme change is picked up on rebind. */
    private static final int[] WEEKDAY_LABELS = {
            R.string.weekday_mon,
            R.string.weekday_tue,
            R.string.weekday_wed,
            R.string.weekday_thu,
            R.string.weekday_fri,
            R.string.weekday_sat,
            R.string.weekday_sun
    };

    private final Context context;
    private final List<WeekDay> days = new ArrayList<>();
    private String selectedIso;
    private OnDaySelectedListener listener;
    private int accentColor;

    public DaySelectorAdapter(Context context, int accentColor) {
        this.context = context;
        this.accentColor = accentColor;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
        notifyDataSetChanged();
    }

    public void setOnDaySelectedListener(OnDaySelectedListener listener) {
        this.listener = listener;
    }

    /** Replaces the week and marks {@code selectedIso} as the active cell. */
    public void submit(List<WeekDay> week, String selectedIso) {
        days.clear();
        this.selectedIso = selectedIso;
        if (week != null) {
            days.addAll(week);
        }
        notifyDataSetChanged();
    }

    /** Moves the highlight without rebuilding the list. */
    public void setSelectedIso(String dateIso) {
        this.selectedIso = dateIso;
        notifyDataSetChanged();
    }

    public String getSelectedIso() {
        return selectedIso;
    }

    @NonNull
    @Override
    public DayHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_tab, parent, false);
        return new DayHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull DayHolder holder, int position) {
        holder.bind(days.get(position));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    class DayHolder extends RecyclerView.ViewHolder {

        private final View root;
        private final TextView weekday;
        private final TextView dayOfMonth;

        DayHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.dayTabRoot);
            weekday = itemView.findViewById(R.id.dayTabWeekday);
            dayOfMonth = itemView.findViewById(R.id.dayTabDayOfMonth);
        }

        void bind(WeekDay day) {
            weekday.setText(WEEKDAY_LABELS[day.getDayOfWeekIndex()]);
            dayOfMonth.setText(day.getDayOfMonthLabel());

            boolean selected = day.getDateIso().equals(selectedIso);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            // Softer pill than the old 12dp cell: a full-round chip reads as a
            // date token rather than a table cell.
            shape.setCornerRadius(18 * context.getResources().getDisplayMetrics().density);

            if (selected) {
                // Filled with the accent, so the text has to be chosen from the
                // fill colour: a light accent needs dark text to stay readable.
                shape.setColor(accentColor);
                int onAccent = GradientContrast.onColorFor(accentColor);
                weekday.setTextColor(onAccent);
                dayOfMonth.setTextColor(onAccent);
            } else {
                // Unselected chips are the card surface with a hairline outline.
                // colorSurfaceVariant (#F0F0F0) was tried here and vanished: it
                // is only 15/255 away from the white card behind it, so the
                // strip lost its chip boundaries entirely. The 18dp radius is
                // what makes these read as date tokens rather than table cells.
                shape.setColor(surfaceColor());
                shape.setStroke(
                        Math.round(context.getResources().getDisplayMetrics().density),
                        outlineColor());
                weekday.setTextColor(onSurfaceVariantColor());
                dayOfMonth.setTextColor(onSurfaceColor());
            }
            root.setBackground(shape);

            // "Today" gets a subtle underline via textStyle so it is findable
            // even when another day is selected.
            dayOfMonth.setTypeface(null, day.isToday()
                    ? android.graphics.Typeface.BOLD_ITALIC
                    : android.graphics.Typeface.BOLD);

            root.setOnClickListener(v -> {
                if (listener != null && !day.getDateIso().equals(selectedIso)) {
                    listener.onDaySelected(day.getDateIso());
                }
            });
        }
    }

    // Theme roles are resolved through the current theme rather than a fixed
    // @color/, because the app ships one palette per theme and the plain
    // colour resources are not overridden for every theme. The custom
    // textPrimary/cardBackground attrs are declared in attrs.xml but never
    // assigned by any style, so they must not be used here.

    private int surfaceColor() {
        return resolveThemeColor(com.google.android.material.R.attr.colorSurface,
                android.R.attr.colorBackground);
    }

    private int onSurfaceColor() {
        return resolveThemeColor(com.google.android.material.R.attr.colorOnSurface,
                android.R.attr.textColorPrimary);
    }

    private int onSurfaceVariantColor() {
        return resolveThemeColor(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                android.R.attr.textColorSecondary);
    }

    private int outlineColor() {
        return resolveThemeColor(com.google.android.material.R.attr.colorOutline,
                android.R.attr.textColorSecondary);
    }

    /** First attr that the current theme actually defines, else the fallback. */
    private int resolveThemeColor(int attr, int fallbackAttr) {
        android.util.TypedValue value = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(attr, value, true)
                && value.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        if (context.getTheme().resolveAttribute(fallbackAttr, value, true)
                && value.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        // Last resort: a neutral that is legible on either surface.
        return context.getColor(R.color.theme_text_primary);
    }
}
