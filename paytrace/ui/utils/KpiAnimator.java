package com.paytrace.ui.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Animates a numeric Label from 0 to its target value with smooth easing.
 * Makes dashboard KPIs feel alive instead of jarring numbers appearing.
 */
public class KpiAnimator {

    /** Animate an integer count-up. */
    public static void animateInt(Label label, int target) {
        animateInt(label, target, "");
    }

    public static void animateInt(Label label, int target, String suffix) {
        if (label == null) return;
        SimpleDoubleProperty value = new SimpleDoubleProperty(0);
        value.addListener((obs, o, n) ->
                label.setText((int) n.doubleValue() + suffix));
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(value, 0)),
                new KeyFrame(Duration.millis(700), new KeyValue(value, target))
        );
        t.play();
    }

    /** Animate a percentage label like "47.5%". */
    public static void animatePercent(Label label, double targetPct) {
        if (label == null) return;
        SimpleDoubleProperty value = new SimpleDoubleProperty(0);
        value.addListener((obs, o, n) ->
                label.setText(String.format("%.1f%%", n.doubleValue())));
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(value, 0)),
                new KeyFrame(Duration.millis(700), new KeyValue(value, targetPct))
        );
        t.play();
    }
}