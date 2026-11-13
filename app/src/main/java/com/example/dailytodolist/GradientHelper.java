package com.example.dailytodolist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.TextView;

import com.example.dailytodolist.util.GradientContrast;
import com.google.android.material.button.MaterialButton;

/**
 * Paints the active theme's two-colour gradient onto a pill-shaped control.
 *
 * <p><b>One implementation, two screens.</b> The Add Task screen's "Save Task"
 * button and the home screen's "Add New Task" button used to build their
 * gradients separately, and that is exactly how they drifted apart: one was a
 * plain {@link TextView} given a {@link GradientDrawable}, the other a
 * {@link MaterialButton} whose own default tint kept winning. Both now go
 * through {@link #pillDrawable}, so the stops, the direction and the corner
 * radius cannot disagree again.</p>
 *
 * <p><b>Why a MaterialButton needs special handling.</b> A Material button
 * carries a {@code backgroundTint} from its style ({@code colorPrimary}), and
 * AppCompat re-applies that tint whenever its drawable state changes. Handing it
 * a gradient therefore produces a solid {@code colorPrimary} fill — the flat blue
 * that prompted this class. Two things are needed:</p>
 * <ol>
 *   <li>{@code app:backgroundTint="@null"} in the layout, so the widget does not
 *       hold a tint in the first place.</li>
 *   <li>{@code setBackgroundTintList(null)} in code, called <em>after</em>
 *       {@code setBackground(...)}. Before the background is replaced,
 *       {@code MaterialButton} routes the call into its internal
 *       {@code MaterialButtonHelper}, and the tint on our new drawable is never
 *       cleared.</li>
 * </ol>
 *
 * <p>Gradients come from {@link ThemeManager}; readable label colours come from
 * {@link GradientContrast} rather than a hardcoded white, because several theme
 * gradients ({@code sunset}, {@code cream_rose}, {@code mint}) are light enough
 * that white text would disappear.</p>
 */
public final class GradientHelper {

    /** Corner radius of every gradient pill, in dp (half the 52dp pill height). */
    public static final float PILL_RADIUS_DP = 26f;

    private GradientHelper() {
        // Static helpers only.
    }

    /**
     * The active theme's gradient stops.
     *
     * <p>Delegates to {@link ThemeManager#getThemeGradientColors} so every screen
     * paints from the same palette. The private colour table this class used to
     * carry has been removed: it had its own theme ids and two of them
     * ({@code "hotpink"}, {@code "lime"}) read different colour names from
     * {@code ThemeManager}, so the same theme id could produce two gradients.</p>
     */
    public static int[] colorsFor(Context context) {
        return ThemeManager.getThemeGradientColors(
                context, ThemeManager.getCurrentTheme(context));
    }

    /** A left-to-right gradient rounded into a pill. */
    public static GradientDrawable pillDrawable(Context context, int[] colors) {
        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, colors);
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(
                PILL_RADIUS_DP * context.getResources().getDisplayMetrics().density);
        return shape;
    }

    /** The label colour that stays readable on top of this gradient. */
    public static int labelColorFor(int[] colors) {
        if (colors == null || colors.length == 0) {
            return GradientContrast.WHITE;
        }
        return GradientContrast.onColorForGradient(
                colors[0], colors[colors.length - 1]);
    }

    /**
     * Applies a gradient header to an arbitrary view (no pill radius, no label).
     */
    public static void applyHeaderGradient(View headerView, Context context, int[] colors) {
        if (headerView == null || colors == null || colors.length == 0) {
            return;
        }
        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        shape.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        headerView.setBackground(shape);
    }

    /**
     * Applies the gradient to a plain {@link TextView}.
     *
     * <p>Used by the Add Task screen's time pills and Save button. No ripple: a
     * {@code TextView} would need a foreground drawable for that, and these
     * controls never had press feedback of their own.</p>
     */
    public static void applyPill(TextView view, Context context, int[] colors) {
        if (view == null || colors == null || colors.length == 0) {
            return;
        }
        view.setBackground(pillDrawable(context, colors));
        view.setTextColor(labelColorFor(colors));
    }

    /**
     * Applies the gradient to a {@link MaterialButton}: default tint removed and
     * press feedback restored.
     */
    public static void applyPill(MaterialButton button, Context context, int[] colors) {
        if (button == null || colors == null || colors.length == 0) {
            return;
        }
        int labelColor = labelColorFor(colors);

        // A translucent label colour: light ripple on dark gradients, dark ripple
        // on light ones, so the press never vanishes into the background.
        int rippleColor = GradientContrast.setAlpha(labelColor, 0x40);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(rippleColor),
                pillDrawable(context, colors),
                null));

        // Order matters. While the widget still owns its original background,
        // MaterialButton forwards this call to MaterialButtonHelper, which never
        // touches the drawable we just installed — so the style's colorPrimary
        // tint would keep painting the button solid blue.
        button.setBackgroundTintList(null);

        button.setTextColor(labelStatesFor(labelColor));
    }

    /**
     * Label colours: full strength when enabled, 40 % opacity when disabled.
     *
     * <p>A {@link ColorStateList} rather than one colour, because
     * {@code setEnabled(false)} is used elsewhere (the AI auto-schedule button)
     * and a single colour would not fade on its own.</p>
     */
    public static ColorStateList labelStatesFor(int color) {
        int disabled = GradientContrast.setAlpha(color, 102); // 40 %
        return new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{disabled, color});
    }
}
