package com.examverse.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.util.Duration;

/**
 * AnimationUtil - Utility class for creating smooth animations
 * Provides reusable animation effects for UI elements
 */
public class AnimationUtil {

    /**
     * Create a fade-in animation
     * @param node Node to animate
     * @param duration Duration in milliseconds
     * @return FadeTransition
     */
    public static FadeTransition fadeIn(Node node, double duration) {
        FadeTransition fade = new FadeTransition(Duration.millis(duration), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        return fade;
    }

    /**
     * Create a fade-out animation
     * @param node Node to animate
     * @param duration Duration in milliseconds
     * @return FadeTransition
     */
    public static FadeTransition fadeOut(Node node, double duration) {
        FadeTransition fade = new FadeTransition(Duration.millis(duration), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        return fade;
    }

    /**
     * Fade out, execute action, then fade in
     * @param node Node to animate
     * @param onComplete Action to execute between fades
     * @param duration Duration of each fade in milliseconds
     */
    public static void fadeTransition(Node node, Runnable onComplete, double duration) {
        FadeTransition fadeOut = fadeOut(node, duration);
        FadeTransition fadeIn = fadeIn(node, duration);

        fadeOut.setOnFinished(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
            fadeIn.play();
        });

        fadeOut.play();
    }

    /**
     * Create a scale animation (pulse effect)
     * @param node Node to animate
     * @param fromScale Starting scale
     * @param toScale Ending scale
     * @param duration Duration in milliseconds
     * @return ScaleTransition
     */
    public static ScaleTransition scale(Node node, double fromScale, double toScale, double duration) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(duration), node);
        scale.setFromX(fromScale);
        scale.setFromY(fromScale);
        scale.setToX(toScale);
        scale.setToY(toScale);
        return scale;
    }

    /**
     * Create a pulsing animation (continuous scale up and down)
     * @param node Node to animate
     * @param duration Duration of one pulse cycle in milliseconds
     * @return Timeline
     */
    public static Timeline pulse(Node node, double duration) {
        ScaleTransition scaleUp = scale(node, 1.0, 1.05, duration / 2);
        ScaleTransition scaleDown = scale(node, 1.05, 1.0, duration / 2);

        SequentialTransition pulse = new SequentialTransition(scaleUp, scaleDown);
        pulse.setCycleCount(Timeline.INDEFINITE);

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(duration), e -> pulse.play()));
        return timeline;
    }

    /**
     * Create a slide-in animation from specified direction
     * @param node Node to animate
     * @param fromX Starting X position
     * @param toX Ending X position
     * @param duration Duration in milliseconds
     * @return TranslateTransition
     */
    public static TranslateTransition slideIn(Node node, double fromX, double toX, double duration) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(duration), node);
        slide.setFromX(fromX);
        slide.setToX(toX);
        return slide;
    }

    /**
     * Create a rotation animation
     * @param node Node to animate
     * @param fromAngle Starting angle in degrees
     * @param toAngle Ending angle in degrees
     * @param duration Duration in milliseconds
     * @return RotateTransition
     */
    public static RotateTransition rotate(Node node, double fromAngle, double toAngle, double duration) {
        RotateTransition rotate = new RotateTransition(Duration.millis(duration), node);
        rotate.setFromAngle(fromAngle);
        rotate.setToAngle(toAngle);
        return rotate;
    }

    /**
     * Create a glow effect animation
     * @param node Node to apply glow effect
     * @param duration Duration in milliseconds
     * @return Timeline
     */
    public static Timeline glow(Node node, double duration) {
        GaussianBlur blur = new GaussianBlur(0);
        node.setEffect(blur);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(duration / 2), new KeyValue(blur.radiusProperty(), 10)),
                new KeyFrame(Duration.millis(duration), new KeyValue(blur.radiusProperty(), 0))
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        return timeline;
    }

    /**
     * Create a sequential animation chain
     * @param animations Transitions to chain
     * @return SequentialTransition
     */
    public static SequentialTransition chain(Transition... animations) {
        return new SequentialTransition(animations);
    }

    /**
     * Create a parallel animation (multiple animations at once)
     * @param animations Transitions to play simultaneously
     * @return ParallelTransition
     */
    public static ParallelTransition parallel(Transition... animations) {
        return new ParallelTransition(animations);
    }
}