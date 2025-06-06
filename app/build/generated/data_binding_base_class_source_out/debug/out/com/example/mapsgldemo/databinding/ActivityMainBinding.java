// Generated by view binder compiler. Do not edit!
package com.example.mapsgldemo.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.mapsgldemo.R;
import com.mapbox.maps.MapView;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityMainBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final ScrollView SCROLLERID;

  @NonNull
  public final View emptyViewBr;

  @NonNull
  public final View emptyViewTl;

  @NonNull
  public final MapView mapView;

  @NonNull
  public final ConstraintLayout outerConstraint;

  @NonNull
  public final LinearLayout outerLinearLayout;

  @NonNull
  public final ScrollView outerScrollView;

  @NonNull
  public final TextView statusTextView;

  @NonNull
  public final TimelineBinding timelineView;

  private ActivityMainBinding(@NonNull ConstraintLayout rootView, @NonNull ScrollView SCROLLERID,
      @NonNull View emptyViewBr, @NonNull View emptyViewTl, @NonNull MapView mapView,
      @NonNull ConstraintLayout outerConstraint, @NonNull LinearLayout outerLinearLayout,
      @NonNull ScrollView outerScrollView, @NonNull TextView statusTextView,
      @NonNull TimelineBinding timelineView) {
    this.rootView = rootView;
    this.SCROLLERID = SCROLLERID;
    this.emptyViewBr = emptyViewBr;
    this.emptyViewTl = emptyViewTl;
    this.mapView = mapView;
    this.outerConstraint = outerConstraint;
    this.outerLinearLayout = outerLinearLayout;
    this.outerScrollView = outerScrollView;
    this.statusTextView = statusTextView;
    this.timelineView = timelineView;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityMainBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityMainBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_main, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityMainBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.SCROLLER_ID;
      ScrollView SCROLLERID = ViewBindings.findChildViewById(rootView, id);
      if (SCROLLERID == null) {
        break missingId;
      }

      id = R.id.empty_view_br;
      View emptyViewBr = ViewBindings.findChildViewById(rootView, id);
      if (emptyViewBr == null) {
        break missingId;
      }

      id = R.id.empty_view_tl;
      View emptyViewTl = ViewBindings.findChildViewById(rootView, id);
      if (emptyViewTl == null) {
        break missingId;
      }

      id = R.id.mapView;
      MapView mapView = ViewBindings.findChildViewById(rootView, id);
      if (mapView == null) {
        break missingId;
      }

      ConstraintLayout outerConstraint = (ConstraintLayout) rootView;

      id = R.id.outerLinearLayout;
      LinearLayout outerLinearLayout = ViewBindings.findChildViewById(rootView, id);
      if (outerLinearLayout == null) {
        break missingId;
      }

      id = R.id.outerScrollView;
      ScrollView outerScrollView = ViewBindings.findChildViewById(rootView, id);
      if (outerScrollView == null) {
        break missingId;
      }

      id = R.id.statusTextView;
      TextView statusTextView = ViewBindings.findChildViewById(rootView, id);
      if (statusTextView == null) {
        break missingId;
      }

      id = R.id.timeline_view;
      View timelineView = ViewBindings.findChildViewById(rootView, id);
      if (timelineView == null) {
        break missingId;
      }
      TimelineBinding binding_timelineView = TimelineBinding.bind(timelineView);

      return new ActivityMainBinding((ConstraintLayout) rootView, SCROLLERID, emptyViewBr,
          emptyViewTl, mapView, outerConstraint, outerLinearLayout, outerScrollView, statusTextView,
          binding_timelineView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
