package com.example.dailytodolist;

import android.content.ClipData;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailytodolist.model.TimeCategory;
import com.example.dailytodolist.model.TimelineEdits;
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
 *
 * <p><b>Interactive behaviours.</b> A block whose end has already passed is
 * dimmed to 50% alpha but stays fully draggable: a missed task is usually
 * rescheduled, not deleted. Long-pressing any block asks the host to start a
 * drag; deletion is a left/right swipe on the row, owned by the host's
 * {@code ItemTouchHelper} (see {@code MainActivity#setupTimelineDrag}). This
 * adapter only exposes what is needed to answer "which task was in that row".</p>
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.SlotHolder> {

    /** One rendered row: its label plus the blocks that start in that hour. */
    private static final class Slot {
        final String label;
        final int hour;
        final List<TaskWithDate> tasks;

        Slot(String label, int hour, List<TaskWithDate> tasks) {
            this.label = label;
            this.hour = hour;
            this.tasks = tasks;
        }
    }

    /** Host callbacks, so the adapter never touches storage or dialogs itself. */
    public interface Listener {

        /**
         * Long-press on a block — always a request to start a drag, for past
         * blocks as well as future ones.
         *
         * @param task      the task the block renders
         * @param blockView the block view itself, so the host can start a drag
         *                  from exactly the block the user grabbed
         */
        void onBlockLongPressed(@NonNull TaskWithDate task, @NonNull View blockView);

        /**
         * A backlog chip was dropped onto the row for {@code hour}.
         *
         * <p>The adapter stays a renderer: it resolves <em>which</em> hour the
         * finger landed on and hands the id back. Storage, reminders and the
         * re-render stay in the host.</p>
         *
         * @param taskId the dropped task's UUID, from the drag's ClipData
         * @param hour   the row's hour, e.g. 11 for the 11:00 slot
         */
        void onTaskDropped(@NonNull String taskId, int hour);
    }

    /** Alpha applied to a block that has already finished. */
    private static final float PAST_ALPHA = 0.5f;

    private final Context context;
    private final List<Slot> slots = new ArrayList<>();
    private Listener listener;

    /** Evaluation instant, refreshed on every submit so a long screen ages. */
    private long nowMillis = System.currentTimeMillis();

    public TimelineAdapter(Context context) {
        this.context = context;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the contents.
     *
     * @param tasks scheduled tasks only; Anytime entries are ignored defensively
     */
    public void submit(List<TaskWithDate> tasks) {
        slots.clear();
        nowMillis = System.currentTimeMillis();

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
            slots.add(new Slot(hourLabel(hour), hour, inSlot));
        }

        notifyDataSetChanged();
    }

    /**
     * The first task rendered in the row at {@code position}, or null for an
     * empty hour.
     *
     * <p>Kept as the adapter's public answer to "what is in this row?" so a caller
     * never has to reach into {@link Slot}. Note that an empty hour is a valid
     * <em>drop</em> target for a drag — moving a task into a free slot is the
     * normal case — so this is not used to gate drops.</p>
     */
    public TaskWithDate taskAt(int position) {
        if (position < 0 || position >= slots.size()) {
            return null;
        }
        List<TaskWithDate> tasks = slots.get(position).tasks;
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    /** Hour represented by the row at {@code position}, or -1 when out of range. */
    public int hourAt(int position) {
        if (position < 0 || position >= slots.size()) {
            return -1;
        }
        return slots.get(position).hour;
    }

    /**
     * The hour tick shown on the spine, e.g. {@code 09:00}.
     *
     * <p>Only the start of the hour, not {@link TimelineSlots#getSlotLabel}'s
     * {@code 09:00 - 10:00} range: the row already covers one hour, and on a
     * spine the label is a tick mark beside a dot. The range form was right for
     * the old table's TIME column but wraps to two lines here and makes the
     * hours look like rows again.</p>
     *
     * <p>{@code getSlotLabel} itself is untouched — it is pinned by six model
     * tests and still describes the slot correctly.</p>
     */
    private static String hourLabel(int hour) {
        return TimeCategory.formatMinuteOfDay(hour * 60);
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

        /** Normal row background, restored when a drag leaves this row. */
        private final GradientDrawable idleBackground = new GradientDrawable();
        private final GradientDrawable hoverBackground = new GradientDrawable();

        SlotHolder(@NonNull View itemView) {
            super(itemView);
            timeLabel = itemView.findViewById(R.id.slotTimeLabel);
            blocksContainer = itemView.findViewById(R.id.slotBlocks);
            buildHoverBackgrounds();
            attachDropListener();
        }

        /**
         * Builds the idle and hover fills once, so hovering does not allocate a
         * drawable on every drag event. Both derive from theme colours; the
         * hover state is the accent at low alpha, which reads as "you can drop
         * here" without hiding the row's own borders.
         */
        private void buildHoverBackgrounds() {
            float radius = 6 * context.getResources().getDisplayMetrics().density;
            int transparent = 0x00000000;

            idleBackground.setShape(GradientDrawable.RECTANGLE);
            idleBackground.setCornerRadius(radius);
            idleBackground.setColor(transparent);

            int accent = com.google.android.material.color.MaterialColors.getColor(
                    context, com.google.android.material.R.attr.colorPrimary,
                    android.graphics.Color.GRAY);
            // ~15% alpha: visible as a tint, light enough to keep text legible.
            int hover = (accent & 0x00FFFFFF) | (0x26 << 24);

            hoverBackground.setShape(GradientDrawable.RECTANGLE);
            hoverBackground.setCornerRadius(radius);
            hoverBackground.setColor(hover);
        }

        /**
         * Accepts a backlog chip dropped on this row's task area.
         *
         * <p>{@code ACTION_DRAG_STARTED} must return true for the rest of the
         * events to arrive at all. Entered/exited only repaint the highlight;
         * the model is touched exclusively on DROP, so a drag that is cancelled
         * or dropped elsewhere changes nothing.</p>
         */
        private void attachDropListener() {
            blocksContainer.setOnDragListener((view, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        // Only claim drags that carry a task id.
                        return event.getClipDescription() != null
                                && event.getClipDescription().hasMimeType(
                                        android.content.ClipDescription.MIMETYPE_TEXT_PLAIN);

                    case DragEvent.ACTION_DRAG_ENTERED:
                        view.setBackground(hoverBackground);
                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:
                    case DragEvent.ACTION_DRAG_ENDED:
                        view.setBackground(idleBackground);
                        return true;

                    case DragEvent.ACTION_DROP: {
                        view.setBackground(idleBackground);
                        int position = getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION || position >= slots.size()) {
                            return false;
                        }
                        String taskId = readTaskId(event.getClipData());
                        if (taskId == null || listener == null) {
                            return false;
                        }
                        listener.onTaskDropped(taskId, slots.get(position).hour);
                        return true;
                    }

                    default:
                        return true;
                }
            });
        }

        /** First plain-text item of the drag payload, or null. */
        private String readTaskId(ClipData clip) {
            if (clip == null || clip.getItemCount() == 0) {
                return null;
            }
            CharSequence text = clip.getItemAt(0).coerceToText(context);
            if (text == null) {
                return null;
            }
            String id = text.toString().trim();
            return id.isEmpty() ? null : id;
        }

        void bind(Slot slot) {
            timeLabel.setText(slot.label);
            blocksContainer.removeAllViews();
            blocksContainer.setBackground(idleBackground);
            for (TaskWithDate task : slot.tasks) {
                blocksContainer.addView(createBlockView(task));
            }
        }

        private View createBlockView(TaskWithDate task) {
            View block = LayoutInflater.from(context)
                    .inflate(R.layout.item_timeline_block, blocksContainer, false);

            TextView title = block.findViewById(R.id.blockTitle);
            TextView range = block.findViewById(R.id.blockTimeRange);
            View categoryDot = block.findViewById(R.id.blockCategoryDot);
            TextView categoryPill = block.findViewById(R.id.blockCategoryPill);

            title.setText(task.getTaskName());
            range.setText(task.getTimeRangeLabel());

            boolean past = TimelineEdits.isPast(
                    task.getDateIso(), task.getEndMinute(), nowMillis);

            TimeCategory category = task.getTimeCategory();
            int color = context.getColor(category.getColorRes());

            // The card itself is a soft surface wash, not a solid category fill:
            // the category now shows as a dot plus a pill instead of the whole
            // block, so the card can stay neutral and keep the text at full
            // colorOnSurface contrast. Tinting the card with the category would
            // reintroduce the light-fill problem this redesign removes.
            if (categoryDot != null) {
                categoryDot.setBackground(ovalOf(color, 5f));
            }
            if (categoryPill != null) {
                categoryPill.setText(context.getString(category.getLabelRes()));
                // ~18% alpha: the category hue reads as a tint behind the label
                // without lowering the label's contrast below AA.
                categoryPill.setBackground(roundedOf(withAlpha(color, 0x2E), 10f));
                // The _text colour, not the fill: fills like Break (#7ED321) are
                // 1.87:1 on a light card, so the pill label needs its own ramp.
                categoryPill.setTextColor(context.getColor(category.getTextColorRes()));
            }

            // Optional description inside the block, hidden when absent.
            TextView description = block.findViewById(R.id.blockDescription);
            if (description != null) {
                if (task.hasDescription()) {
                    description.setVisibility(View.VISIBLE);
                    description.setText(task.getDescription());
                } else {
                    description.setVisibility(View.GONE);
                }
            }

            // Dim, not hide: an expired block is still part of what the day was,
            // and hiding it would make yesterday look empty. Alpha keeps the
            // category colour visible while reading as "already over".
            block.setAlpha(past ? PAST_ALPHA : 1f);

            // Long-press starts a drag for every block, past included. The
            // dimmed alpha above is visual only — it never gates the gesture.
            block.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onBlockLongPressed(task, v);
                }
                return true;
            });

            return block;
        }

        /** Solid circle of {@code color} with the given radius in dp. */
        private GradientDrawable ovalOf(int color, float radiusDp) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(color);
            shape.setSize(
                    Math.round(radiusDp * 2 * context.getResources().getDisplayMetrics().density),
                    Math.round(radiusDp * 2 * context.getResources().getDisplayMetrics().density));
            return shape;
        }

        /** Rounded rectangle of {@code color} with the given corner radius in dp. */
        private GradientDrawable roundedOf(int color, float radiusDp) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(radiusDp * context.getResources().getDisplayMetrics().density);
            shape.setColor(color);
            return shape;
        }

        /** Replaces a colour's alpha channel, keeping its RGB. */
        private int withAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
        }
    }
}
