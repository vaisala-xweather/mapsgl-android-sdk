// Generated by view binder compiler. Do not edit!
package com.example.mapsgldemo.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.mapsgldemo.R;
import com.example.mapsgldemo.TimelineControls;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class TimelineBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final TextView DateRangeHeaderTextView;

  @NonNull
  public final ConstraintLayout EndCL;

  @NonNull
  public final ConstraintLayout SpeedCL;

  @NonNull
  public final TextView TimeLineHeaderTextView;

  @NonNull
  public final TextView animationTextview;

  @NonNull
  public final ImageView configButton;

  @NonNull
  public final TextView currentDateTextView;

  @NonNull
  public final TextView currentTimeTextView;

  @NonNull
  public final TextView endLabel;

  @NonNull
  public final Button endMinusDayButton;

  @NonNull
  public final Button endMinusHourButton;

  @NonNull
  public final Button endPlusDayButton;

  @NonNull
  public final Button endPlusHourButton;

  @NonNull
  public final TextView endTimeTextview;

  @NonNull
  public final TextView instructionsTextview;

  @NonNull
  public final ImageView intervalBackButton;

  @NonNull
  public final ImageView intervalForwardButton;

  @NonNull
  public final ImageView layerMenuButton;

  @NonNull
  public final ImageView leftArrowImage;

  @NonNull
  public final ImageView locationButton;

  @NonNull
  public final Button playButton;

  @NonNull
  public final ImageView playButtonImage;

  @NonNull
  public final ConstraintLayout playControlsCS;

  @NonNull
  public final ProgressBar progressBar;

  @NonNull
  public final ImageView rightArrowImage;

  @NonNull
  public final ConstraintLayout settingsCS;

  @NonNull
  public final Button settingsCloseButton;

  @NonNull
  public final Button speedHalfButton;

  @NonNull
  public final ImageView speedImage;

  @NonNull
  public final Button speedOneButton;

  @NonNull
  public final Button speedQuarterButton;

  @NonNull
  public final Button speedTwoButton;

  @NonNull
  public final ConstraintLayout startCL;

  @NonNull
  public final TextView startLabel;

  @NonNull
  public final Button startMinusDayButton;

  @NonNull
  public final Button startMinusHourButton;

  @NonNull
  public final Button startPlusDayButton;

  @NonNull
  public final Button startPlusHourButton;

  @NonNull
  public final TextView startTimeTextView;

  @NonNull
  public final ConstraintLayout timelineConstraintLayout;

  @NonNull
  public final TimelineControls timelineControls;

  private TimelineBinding(@NonNull ConstraintLayout rootView,
      @NonNull TextView DateRangeHeaderTextView, @NonNull ConstraintLayout EndCL,
      @NonNull ConstraintLayout SpeedCL, @NonNull TextView TimeLineHeaderTextView,
      @NonNull TextView animationTextview, @NonNull ImageView configButton,
      @NonNull TextView currentDateTextView, @NonNull TextView currentTimeTextView,
      @NonNull TextView endLabel, @NonNull Button endMinusDayButton,
      @NonNull Button endMinusHourButton, @NonNull Button endPlusDayButton,
      @NonNull Button endPlusHourButton, @NonNull TextView endTimeTextview,
      @NonNull TextView instructionsTextview, @NonNull ImageView intervalBackButton,
      @NonNull ImageView intervalForwardButton, @NonNull ImageView layerMenuButton,
      @NonNull ImageView leftArrowImage, @NonNull ImageView locationButton,
      @NonNull Button playButton, @NonNull ImageView playButtonImage,
      @NonNull ConstraintLayout playControlsCS, @NonNull ProgressBar progressBar,
      @NonNull ImageView rightArrowImage, @NonNull ConstraintLayout settingsCS,
      @NonNull Button settingsCloseButton, @NonNull Button speedHalfButton,
      @NonNull ImageView speedImage, @NonNull Button speedOneButton,
      @NonNull Button speedQuarterButton, @NonNull Button speedTwoButton,
      @NonNull ConstraintLayout startCL, @NonNull TextView startLabel,
      @NonNull Button startMinusDayButton, @NonNull Button startMinusHourButton,
      @NonNull Button startPlusDayButton, @NonNull Button startPlusHourButton,
      @NonNull TextView startTimeTextView, @NonNull ConstraintLayout timelineConstraintLayout,
      @NonNull TimelineControls timelineControls) {
    this.rootView = rootView;
    this.DateRangeHeaderTextView = DateRangeHeaderTextView;
    this.EndCL = EndCL;
    this.SpeedCL = SpeedCL;
    this.TimeLineHeaderTextView = TimeLineHeaderTextView;
    this.animationTextview = animationTextview;
    this.configButton = configButton;
    this.currentDateTextView = currentDateTextView;
    this.currentTimeTextView = currentTimeTextView;
    this.endLabel = endLabel;
    this.endMinusDayButton = endMinusDayButton;
    this.endMinusHourButton = endMinusHourButton;
    this.endPlusDayButton = endPlusDayButton;
    this.endPlusHourButton = endPlusHourButton;
    this.endTimeTextview = endTimeTextview;
    this.instructionsTextview = instructionsTextview;
    this.intervalBackButton = intervalBackButton;
    this.intervalForwardButton = intervalForwardButton;
    this.layerMenuButton = layerMenuButton;
    this.leftArrowImage = leftArrowImage;
    this.locationButton = locationButton;
    this.playButton = playButton;
    this.playButtonImage = playButtonImage;
    this.playControlsCS = playControlsCS;
    this.progressBar = progressBar;
    this.rightArrowImage = rightArrowImage;
    this.settingsCS = settingsCS;
    this.settingsCloseButton = settingsCloseButton;
    this.speedHalfButton = speedHalfButton;
    this.speedImage = speedImage;
    this.speedOneButton = speedOneButton;
    this.speedQuarterButton = speedQuarterButton;
    this.speedTwoButton = speedTwoButton;
    this.startCL = startCL;
    this.startLabel = startLabel;
    this.startMinusDayButton = startMinusDayButton;
    this.startMinusHourButton = startMinusHourButton;
    this.startPlusDayButton = startPlusDayButton;
    this.startPlusHourButton = startPlusHourButton;
    this.startTimeTextView = startTimeTextView;
    this.timelineConstraintLayout = timelineConstraintLayout;
    this.timelineControls = timelineControls;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static TimelineBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static TimelineBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.timeline, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static TimelineBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.DateRangeHeaderTextView;
      TextView DateRangeHeaderTextView = ViewBindings.findChildViewById(rootView, id);
      if (DateRangeHeaderTextView == null) {
        break missingId;
      }

      id = R.id.EndCL;
      ConstraintLayout EndCL = ViewBindings.findChildViewById(rootView, id);
      if (EndCL == null) {
        break missingId;
      }

      id = R.id.SpeedCL;
      ConstraintLayout SpeedCL = ViewBindings.findChildViewById(rootView, id);
      if (SpeedCL == null) {
        break missingId;
      }

      id = R.id.TimeLineHeaderTextView;
      TextView TimeLineHeaderTextView = ViewBindings.findChildViewById(rootView, id);
      if (TimeLineHeaderTextView == null) {
        break missingId;
      }

      id = R.id.animationTextview;
      TextView animationTextview = ViewBindings.findChildViewById(rootView, id);
      if (animationTextview == null) {
        break missingId;
      }

      id = R.id.config_button;
      ImageView configButton = ViewBindings.findChildViewById(rootView, id);
      if (configButton == null) {
        break missingId;
      }

      id = R.id.currentDateTextView;
      TextView currentDateTextView = ViewBindings.findChildViewById(rootView, id);
      if (currentDateTextView == null) {
        break missingId;
      }

      id = R.id.currentTimeTextView;
      TextView currentTimeTextView = ViewBindings.findChildViewById(rootView, id);
      if (currentTimeTextView == null) {
        break missingId;
      }

      id = R.id.end_Label;
      TextView endLabel = ViewBindings.findChildViewById(rootView, id);
      if (endLabel == null) {
        break missingId;
      }

      id = R.id.end_minus_day_button;
      Button endMinusDayButton = ViewBindings.findChildViewById(rootView, id);
      if (endMinusDayButton == null) {
        break missingId;
      }

      id = R.id.end_minus_hour_button;
      Button endMinusHourButton = ViewBindings.findChildViewById(rootView, id);
      if (endMinusHourButton == null) {
        break missingId;
      }

      id = R.id.end_plus_day_button;
      Button endPlusDayButton = ViewBindings.findChildViewById(rootView, id);
      if (endPlusDayButton == null) {
        break missingId;
      }

      id = R.id.end_plus_hour_button;
      Button endPlusHourButton = ViewBindings.findChildViewById(rootView, id);
      if (endPlusHourButton == null) {
        break missingId;
      }

      id = R.id.endTimeTextview;
      TextView endTimeTextview = ViewBindings.findChildViewById(rootView, id);
      if (endTimeTextview == null) {
        break missingId;
      }

      id = R.id.instructionsTextview;
      TextView instructionsTextview = ViewBindings.findChildViewById(rootView, id);
      if (instructionsTextview == null) {
        break missingId;
      }

      id = R.id.interval_back_button;
      ImageView intervalBackButton = ViewBindings.findChildViewById(rootView, id);
      if (intervalBackButton == null) {
        break missingId;
      }

      id = R.id.interval_forward_button;
      ImageView intervalForwardButton = ViewBindings.findChildViewById(rootView, id);
      if (intervalForwardButton == null) {
        break missingId;
      }

      id = R.id.layer_menu_button;
      ImageView layerMenuButton = ViewBindings.findChildViewById(rootView, id);
      if (layerMenuButton == null) {
        break missingId;
      }

      id = R.id.left_arrow_image;
      ImageView leftArrowImage = ViewBindings.findChildViewById(rootView, id);
      if (leftArrowImage == null) {
        break missingId;
      }

      id = R.id.location_button;
      ImageView locationButton = ViewBindings.findChildViewById(rootView, id);
      if (locationButton == null) {
        break missingId;
      }

      id = R.id.play_button;
      Button playButton = ViewBindings.findChildViewById(rootView, id);
      if (playButton == null) {
        break missingId;
      }

      id = R.id.play_button_image;
      ImageView playButtonImage = ViewBindings.findChildViewById(rootView, id);
      if (playButtonImage == null) {
        break missingId;
      }

      id = R.id.playControlsCS;
      ConstraintLayout playControlsCS = ViewBindings.findChildViewById(rootView, id);
      if (playControlsCS == null) {
        break missingId;
      }

      id = R.id.progressBar;
      ProgressBar progressBar = ViewBindings.findChildViewById(rootView, id);
      if (progressBar == null) {
        break missingId;
      }

      id = R.id.right_arrow_image;
      ImageView rightArrowImage = ViewBindings.findChildViewById(rootView, id);
      if (rightArrowImage == null) {
        break missingId;
      }

      id = R.id.settingsCS;
      ConstraintLayout settingsCS = ViewBindings.findChildViewById(rootView, id);
      if (settingsCS == null) {
        break missingId;
      }

      id = R.id.settings_close_button;
      Button settingsCloseButton = ViewBindings.findChildViewById(rootView, id);
      if (settingsCloseButton == null) {
        break missingId;
      }

      id = R.id.speed_half_button;
      Button speedHalfButton = ViewBindings.findChildViewById(rootView, id);
      if (speedHalfButton == null) {
        break missingId;
      }

      id = R.id.speed_image;
      ImageView speedImage = ViewBindings.findChildViewById(rootView, id);
      if (speedImage == null) {
        break missingId;
      }

      id = R.id.speed_one_button;
      Button speedOneButton = ViewBindings.findChildViewById(rootView, id);
      if (speedOneButton == null) {
        break missingId;
      }

      id = R.id.speed_quarter_button;
      Button speedQuarterButton = ViewBindings.findChildViewById(rootView, id);
      if (speedQuarterButton == null) {
        break missingId;
      }

      id = R.id.speed_two_button;
      Button speedTwoButton = ViewBindings.findChildViewById(rootView, id);
      if (speedTwoButton == null) {
        break missingId;
      }

      id = R.id.startCL;
      ConstraintLayout startCL = ViewBindings.findChildViewById(rootView, id);
      if (startCL == null) {
        break missingId;
      }

      id = R.id.start_label;
      TextView startLabel = ViewBindings.findChildViewById(rootView, id);
      if (startLabel == null) {
        break missingId;
      }

      id = R.id.start_minus_day_button;
      Button startMinusDayButton = ViewBindings.findChildViewById(rootView, id);
      if (startMinusDayButton == null) {
        break missingId;
      }

      id = R.id.start_minus_hour_button;
      Button startMinusHourButton = ViewBindings.findChildViewById(rootView, id);
      if (startMinusHourButton == null) {
        break missingId;
      }

      id = R.id.start_plus_day_button;
      Button startPlusDayButton = ViewBindings.findChildViewById(rootView, id);
      if (startPlusDayButton == null) {
        break missingId;
      }

      id = R.id.start_plus_hour_button;
      Button startPlusHourButton = ViewBindings.findChildViewById(rootView, id);
      if (startPlusHourButton == null) {
        break missingId;
      }

      id = R.id.startTimeTextView;
      TextView startTimeTextView = ViewBindings.findChildViewById(rootView, id);
      if (startTimeTextView == null) {
        break missingId;
      }

      ConstraintLayout timelineConstraintLayout = (ConstraintLayout) rootView;

      id = R.id.timeline_controls;
      TimelineControls timelineControls = ViewBindings.findChildViewById(rootView, id);
      if (timelineControls == null) {
        break missingId;
      }

      return new TimelineBinding((ConstraintLayout) rootView, DateRangeHeaderTextView, EndCL,
          SpeedCL, TimeLineHeaderTextView, animationTextview, configButton, currentDateTextView,
          currentTimeTextView, endLabel, endMinusDayButton, endMinusHourButton, endPlusDayButton,
          endPlusHourButton, endTimeTextview, instructionsTextview, intervalBackButton,
          intervalForwardButton, layerMenuButton, leftArrowImage, locationButton, playButton,
          playButtonImage, playControlsCS, progressBar, rightArrowImage, settingsCS,
          settingsCloseButton, speedHalfButton, speedImage, speedOneButton, speedQuarterButton,
          speedTwoButton, startCL, startLabel, startMinusDayButton, startMinusHourButton,
          startPlusDayButton, startPlusHourButton, startTimeTextView, timelineConstraintLayout,
          timelineControls);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
