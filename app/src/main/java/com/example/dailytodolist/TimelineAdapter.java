package com.example.dailytodolist;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailytodolist.model.TimeCategory;
import com.example.dailytodolist.model.TimelineSlots;

import java.util.ArrayList;
import java.util.List;

/**
 * Hour-slot adapter for the Time-Blocking Grid.
 *
 * <p>Renders one row per hour (06:00–23:00 by default). A task appears in the
 * row matching the hour of its start time, tinted with its category colour.
 * Slots with no task show the hour label and a blank right-hand side, so the
 * grid keeps a stable shape whether or not the day is busy.</p>
 *
 * <p>Anytime tasks are never passed here: they have no interval, so they are
 * listed in the backlog strip above the grid instead.</p>
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.SlotHolder> {

    /** One rendered row: its label plus the blocks that start in that hour. */
    private static final class Slot {
        final String label;
        final List<TaskWithDate> tasks;

        Slot(String label, List<TaskWithDate> tasks) {
            this.label = label;
            this.tasks = tasks;
        }
    }

    private final Context context;
    private final List<Slot> slots = new ArrayList<>();

    public TimelineAdapter(Context context) {
        this.context = context;
    }

    /**
     * Replaces the contents.
     *
     * @param tasks scheduled tasks only; Anytime entries are ignored defensively
     */
    public void submit(List<TaskWithDate> tasks) {
        slots.clear();

        // The grid is a fixed 06:00–23:00 frame, so it renders the same number
        // of rows on every day. A null or empty list therefore yields an empty
        // day, not an empty grid.
        List<TaskWithDate> source = tasks == null ? new ArrayList<>() : tasks;

        TimelineSlots buckets = new TimelineSlots();
        for (TaskWithDate task : source) {
            if (task == null || task.isAnytime()) {
                continue;
            }
            buckets.add(new TimelineSlots.TimeBlockRow(
                    task.getTaskName(), task.getStartMinute(), task.getEndMinute()));
        }

        // One Slot per hour, each holding the tasks bucketed into it.
        for (int i = 0; i < buckets.getSlotCount(); i++) {
            List<TaskWithDate> inSlot = new ArrayList<>();
            int hour = buckets.hourAt(i);
            for (TaskWithDate task : source) {
                if (task == null || task.isAnytime()) {
                    continue;
                }
                if (hourOf(task.getStartMinute(), buckets) == hour) {
                    inSlot.add(task);
                }
            }
            slots.add(new Slot(buckets.getSlotLabel(i), inSlot));
        }

        notifyDataSetChanged();
    }

    /** Clamped hour used for placement, matching {@link TimelineSlots#add}. */
    private static int hourOf(int startMinute, TimelineSlots buckets) {
        int hour = startMinute / 60;
        if (hour < buckets.getFirstHour()) {
            return buckets.getFirstHour();
        }
        if (hour > buckets.getLastHour()) {
            return buckets.getLastHour();
        }
        return hour;
    }

    @NonNull
    @Override
    public SlotHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_slot, parent, false);
        return new SlotHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotHolder holder, int position) {
        holder.bind(slots.get(position));
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    /** Renders one hour row. */
    class SlotHolder extends RecyclerView.ViewHolder {

        private final TextView timeLabel;
        private final LinearLayout blocksContainer;

        SlotHolder(@NonNull View itemView) {
            super(itemView);
            timeLabel = itemView.findViewById(R.id.slotTimeLabel);
            blocksContainer = itemView.findViewById(R.id.slotBlocks);
        }

        void bind(Slot slot) {
            timeLabel.setText(slot.label);
            blocksContainer.removeAllViews();
            for (TaskWithDate task : slot.tasks) {
                blocksContainer.addView(createBlockView(task));
            }
        }

        private View createBlockView(TaskWithDate task) {
            View block = LayoutInflater.from(context)
                    .inflate(R.layout.item_timeline_block, blocksContainer, false);

            TextView title = block.findViewById(R.id.blockTitle);
            TextView range = block.findViewById(R.id.blockTimeRange);

            title.setText(task.getTaskName());
            range.setText(task.getTimeRangeLabel());

            int color = context.getColor(task.getTimeCategory().getColorRes());
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(10 * context.getResources().getDisplayMetrics().density);
            shape.setColor(color);
            block.setBackground(shape);

            // Pick text colour from the fill so the label stays readable on
            // light and dark category colours alike.
            int onColor = isDark(color) ? android.graphics.Color.WHITE
                    : android.graphics.Color.parseColor("#1F2B48");
            title.setTextColor(onColor);
            range.setTextColor(onColor);

            // Optional description inside the block, hidden when absent.
            TextView description = block.findViewById(R.id.blockDescription);
            if (description != null) {
                if (task.hasDescription()) {
                    description.setVisibility(View.VISIBLE);
                    description.setText(task.getDescription());
                    description.setTextColor(onColor);
                } else {
                    description.setVisibility(View.GONE);
                }
            }

            return block;
        }
    }

    /** True when a colour is dark enough to need light text on it. */
    private static boolean isDark(int color) {
        double luminance = (0.299 * android.graphics.Color.red(color)
                + 0.587 * android.graphics.Color.green(color)
                + 0.114 * android.graphics.Color.blue(color)) / 255.0;
        return luminance < 0.6;
    }
}
