package com.example.dailytodolist;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A RecyclerView that always measures its full content height.
 *
 * <p><b>Why this exists.</b> The timeline grid lives inside a {@code ScrollView},
 * so the outer ScrollView is the only thing allowed to scroll. Setting
 * {@code layout_height="wrap_content"} plus {@code nestedScrollingEnabled="false"}
 * is necessary but <em>not sufficient</em>: a vertical {@code LinearLayout} hands
 * its wrap_content child an {@code AT_MOST} spec, and {@link RecyclerView}'s
 * auto-measure clamps the result to that limit. The grid then renders only as
 * many hours as fit the viewport and silently drops the rest of the day.</p>
 *
 * <p>Measuring against {@code UNSPECIFIED} instead makes the RecyclerView report
 * the height its adapter genuinely needs, so all 06:00–23:00 rows are laid out
 * and the ScrollView can reach 23:00. Only use this for short, fixed-length
 * lists that are nested in another scroller — an {@code UNSPECIFIED} measure
 * forces every row to be laid out, which is wasteful for long lists.</p>
 */
public class WrapHeightRecyclerView extends RecyclerView {

    public WrapHeightRecyclerView(@NonNull Context context) {
        super(context);
    }

    public WrapHeightRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapHeightRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs,
                                  int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int unspecifiedHeight =
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        super.onMeasure(widthSpec, unspecifiedHeight);
    }
}
