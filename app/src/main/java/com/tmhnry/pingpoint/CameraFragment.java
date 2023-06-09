/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.tmhnry.pingpoint;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysis.ImageReaderMode;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.tmhnry.pingpoint.CameraFragmentViewModel.TrainingState;
import com.tmhnry.pingpoint.api.TransferLearningModel.Prediction;
import com.tmhnry.pingpoint.databinding.CameraFragmentBinding;
import com.tmhnry.pingpoint.vision.CameraUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The main fragment of the classifier.
 * <p>
 * Camera functionality (through CameraX) is heavily based on the official example:
 * https://github.com/android/camera/tree/master/CameraXBasic.
 */
public class CameraFragment extends Fragment {
    private static final String TAG = CameraFragment.class.getSimpleName();
    private static final LensFacing LENS_FACING = LensFacing.BACK;
    private static final int LONG_PRESS_DURATION = 500;
    private static final int SAMPLE_COLLECTION_DELAY = 300;
    private TextureView viewFinder;
    private Integer viewFinderRotation = null;
    private Size bufferDimens = new Size(0, 0);
    private Size viewFinderDimens = new Size(0, 0);
    private CameraFragmentViewModel viewModel;
    private long sampleCollectionButtonPressedTime;
    private boolean isCollectingSamples = false;
    private final Handler sampleCollectionHandler = new Handler(Looper.getMainLooper());
    private final HelpDialog helpDialog = new HelpDialog();

    // When the user presses the "add sample" button for some class,
    // that class will be added to this queue. It is later extracted by
    // InferenceThread and processed.
    private final ConcurrentLinkedQueue<String> addSampleRequests = new ConcurrentLinkedQueue<>();
    private LoggingBenchmark inferenceBenchmark;
    SQLiteDatabase db;

    /**
     * Set up a responsive preview for the view finder.
     */
    private void startCamera() {
        viewFinderRotation = CameraUtils.getDisplaySurfaceRotation(viewFinder.getDisplay());
        if (viewFinderRotation == null) {
            viewFinderRotation = 0;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        viewFinder.getDisplay().getRealMetrics(metrics);
        Rational screenAspectRatio = new Rational(metrics.widthPixels, metrics.heightPixels);

        PreviewConfig config = new PreviewConfig.Builder()
                .setLensFacing(LENS_FACING)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .build();

        Preview preview = new Preview(config);

        preview.setOnPreviewOutputUpdateListener(previewOutput -> {
            ViewGroup parent = (ViewGroup) viewFinder.getParent();
            parent.removeView(viewFinder);
            parent.addView(viewFinder, 0);

            viewFinder.setSurfaceTexture(previewOutput.getSurfaceTexture());

            Integer rotation = CameraUtils.getDisplaySurfaceRotation(viewFinder.getDisplay());
            updateTransform(rotation, previewOutput.getTextureSize(), viewFinderDimens);
        });

        viewFinder.addOnLayoutChangeListener((
                view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            Size newViewFinderDimens = new Size(right - left, bottom - top);
            Integer rotation = CameraUtils.getDisplaySurfaceRotation(viewFinder.getDisplay());
            updateTransform(rotation, bufferDimens, newViewFinderDimens);
        });

        HandlerThread inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                .setLensFacing(LENS_FACING)
                .setCallbackHandler(new Handler(inferenceThread.getLooper()))
                .setImageReaderMode(ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(analysisConfig);
        imageAnalysis.setAnalyzer(inferenceAnalyzer);

        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        db = context.openOrCreateDatabase("EmployeeTracker", MODE_PRIVATE, null);
        inferenceBenchmark = VisionModelProvider.getBenchmark();
    }

    private final ImageAnalysis.Analyzer inferenceAnalyzer =
            (imageProxy, rotationDegrees) -> {
                final String id = UUID.randomUUID().toString();

                inferenceBenchmark.startStage(id, "preprocess");
                Bitmap bitmap = CameraUtils.yuvCameraImageToBitmap(imageProxy);
                float[][][] image = CameraUtils.prepareCameraImage(bitmap, rotationDegrees);
                inferenceBenchmark.endStage(id, "preprocess");

                // Adding samples is also handled by inference thread / use case.
                // We don't use CameraX ImageCapture since it has very high latency (~650ms on Pixel 2 XL)
                // even when using .MIN_LATENCY.
                String category = addSampleRequests.poll();
                if (category != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("rotation", rotationDegrees);
                    data.put("id", id);
                    data.put("image", image);
                    data.put("bitmap", bitmap);
                    data.put("category", category);
                    viewModel.increaseNumSamples(VisionModelProvider.Create().addSampleToModel(true, data));
                } else {
                    // We don't perform inference when adding samples, since we should be in capture mode
                    // at the time, so the inference results are not actually displayed.
                    inferenceBenchmark.startStage(id, "predict");
                    if(VisionModelProvider.getModel() == null){
                        return;
                    }
                    Prediction[] predictions = VisionModelProvider.getModel().predict(image);
                    if (predictions == null) {
                        return;
                    }
                    inferenceBenchmark.endStage(id, "predict");

                    for (Prediction prediction : predictions) {
                        viewModel.setConfidence(prediction.getClassName(), prediction.getConfidence());
                    }
                }
                inferenceBenchmark.finish(id);
            };

    public final View.OnTouchListener onAddSampleTouchListener =
            (view, motionEvent) -> {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isCollectingSamples = true;
                        sampleCollectionButtonPressedTime = SystemClock.uptimeMillis();
                        sampleCollectionHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        long timePressed =
                                                SystemClock.uptimeMillis() - sampleCollectionButtonPressedTime;
                                        view.findViewById(view.getId()).performClick();
                                        if (timePressed < LONG_PRESS_DURATION) {
                                            sampleCollectionHandler.postDelayed(this, LONG_PRESS_DURATION);
                                        } else if (isCollectingSamples) {
                                            String className = getClassNameFromResourceId(view.getId());
                                            viewModel.setNumCollectedSamples(
                                                    viewModel.getNumSamples().getValue().get(className) + 1);
                                            sampleCollectionHandler.postDelayed(this, SAMPLE_COLLECTION_DELAY);
                                            viewModel.setSampleCollectionLongPressed(true);
                                        }
                                    }
                                });
                        break;
                    case MotionEvent.ACTION_UP:
                        sampleCollectionHandler.removeCallbacksAndMessages(null);
                        isCollectingSamples = false;
                        VisionModelProvider.Create().saveSamplesLocally(getActivity());
                        viewModel.setSampleCollectionLongPressed(false);
                        break;
                    default:
                        break;
                }
                return true;
            };

    public final View.OnClickListener onAddSampleClickListener =
            view -> {
                String className = getClassNameFromResourceId(view.getId());
                addSampleRequests.add(className);
            };

    private String getClassNameFromResourceId(int id) {
        String className;
        if (id == R.id.class_btn_1) {
            className = "1";
        } else if (id == R.id.class_btn_2) {
            className = "2";
        } else if (id == R.id.class_btn_3) {
            className = "3";
        } else if (id == R.id.class_btn_4) {
            className = "4";
        } else {
            throw new RuntimeException("Listener called for unexpected view");
        }
        return className;
    }

    /**
     * Fit the camera preview into [viewFinder].
     *
     * @param rotation            view finder rotation.
     * @param newBufferDimens     camera preview dimensions.
     * @param newViewFinderDimens view finder dimensions.
     */
    private void updateTransform(Integer rotation, Size newBufferDimens, Size newViewFinderDimens) {
        if (Objects.equals(rotation, viewFinderRotation)
                && Objects.equals(newBufferDimens, bufferDimens)
                && Objects.equals(newViewFinderDimens, viewFinderDimens)) {
            return;
        }

        if (rotation == null) {
            return;
        } else {
            viewFinderRotation = rotation;
        }

        if (newBufferDimens.getWidth() == 0 || newBufferDimens.getHeight() == 0) {
            return;
        } else {
            bufferDimens = newBufferDimens;
        }

        if (newViewFinderDimens.getWidth() == 0 || newViewFinderDimens.getHeight() == 0) {
            return;
        } else {
            viewFinderDimens = newViewFinderDimens;
        }

        Log.d(TAG, String.format("Applying output transformation.\n"
                + "View finder size: %s.\n"
                + "Preview output size: %s\n"
                + "View finder rotation: %s\n", viewFinderDimens, bufferDimens, viewFinderRotation));
        Matrix matrix = new Matrix();

        float centerX = viewFinderDimens.getWidth() / 2f;
        float centerY = viewFinderDimens.getHeight() / 2f;

        matrix.postRotate(-viewFinderRotation.floatValue(), centerX, centerY);

        float bufferRatio = bufferDimens.getHeight() / (float) bufferDimens.getWidth();

        int scaledWidth;
        int scaledHeight;
        if (viewFinderDimens.getWidth() > viewFinderDimens.getHeight()) {
            scaledHeight = viewFinderDimens.getWidth();
            scaledWidth = Math.round(viewFinderDimens.getWidth() * bufferRatio);
        } else {
            scaledHeight = viewFinderDimens.getHeight();
            scaledWidth = Math.round(viewFinderDimens.getHeight() * bufferRatio);
        }

        float xScale = scaledWidth / (float) viewFinderDimens.getWidth();
        float yScale = scaledHeight / (float) viewFinderDimens.getHeight();

        matrix.preScale(xScale, yScale, centerX, centerY);

        viewFinder.setTransform(matrix);
    }

    boolean hasDataSet = false;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        viewModel = ViewModelProviders.of(this).get(CameraFragmentViewModel.class);
        viewModel.setTrainBatchSize(VisionModelProvider.getModel().getTrainBatchSize());

        Map<String, Integer> data = VisionModelProvider.getCategoryCount();
        if(!data.isEmpty()){
            hasDataSet = true;
        }
        for (String category : data.keySet()) {
            int count = data.get(category);
            viewModel.increaseNumSamples(category, count);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {

        CameraFragmentBinding dataBinding =
                DataBindingUtil.inflate(inflater, R.layout.camera_fragment, container, false);

        dataBinding.setLifecycleOwner(getViewLifecycleOwner());
        dataBinding.setVm(viewModel);
        View rootView = dataBinding.getRoot();

        for (int buttonId : new int[]{
                R.id.class_btn_1, R.id.class_btn_2, R.id.class_btn_3, R.id.class_btn_4}) {
            rootView.findViewById(buttonId).setOnClickListener(onAddSampleClickListener);
            rootView.findViewById(buttonId).setOnTouchListener(onAddSampleTouchListener);
        }

        if(hasDataSet){
            rootView.findViewById(R.id.class_btn_1).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.class_btn_4).setVisibility(View.INVISIBLE);
        }

        if (viewModel.getCaptureMode().getValue()) {
            ((RadioButton) rootView.findViewById(R.id.capture_mode_button)).setChecked(true);
        } else {
            ((RadioButton) rootView.findViewById(R.id.inference_mode_button)).setChecked(true);
        }

        RadioGroup toggleButtonGroup = rootView.findViewById(R.id.mode_toggle_button_group);
        toggleButtonGroup.setOnCheckedChangeListener(
                (radioGroup, checkedId) -> {
                    if (viewModel.getTrainingState().getValue() == TrainingState.NOT_STARTED) {
                        ((RadioButton) rootView.findViewById(R.id.capture_mode_button)).setChecked(true);
                        ((RadioButton) rootView.findViewById(R.id.inference_mode_button)).setChecked(false);

                        Snackbar.make(
                                        requireActivity().findViewById(R.id.classes_bar),
                                        "Inference can only start after training is done.",
                                        BaseTransientBottomBar.LENGTH_LONG)
                                .show();
                    } else {
                        if (checkedId == R.id.capture_mode_button) {
                            viewModel.setCaptureMode(true);
                        } else {
                            viewModel.setCaptureMode(false);
                            Snackbar.make(
                                            requireActivity().findViewById(R.id.classes_bar),
                                            "Point your camera at one of the trained objects.",
                                            BaseTransientBottomBar.LENGTH_LONG)
                                    .show();
                        }
                    }
                });

        Button helpButton = rootView.findViewById(R.id.help_button);
        helpButton.setOnClickListener(
                (button) -> {
//                    helpDialog.show(requireActivity().getSupportFragmentManager(), "Help Dialog");
                });
        // Display HelpDialog when opened.
//        helpDialog.show(requireActivity().getSupportFragmentManager(), "Help Dialog");

        return dataBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        viewFinder = requireActivity().findViewById(R.id.view_finder);
        viewFinder.post(this::startCamera);

        viewModel
                .getTrainingState()
                .observe(
                        getViewLifecycleOwner(),
                        trainingState -> {
                            switch (trainingState) {
                                case STARTED:
                                    VisionModelProvider.getModel().enableTraining((epoch, loss) -> viewModel.setLastLoss(loss));
                                    if (!viewModel.getInferenceSnackbarWasDisplayed().getValue()) {
                                        Snackbar.make(
                                                        requireActivity().findViewById(R.id.classes_bar),
                                                        R.string.switch_to_inference_hint,
                                                        BaseTransientBottomBar.LENGTH_LONG)
                                                .show();
                                        viewModel.markInferenceSnackbarWasCalled();
                                    }
                                    break;
                                case PAUSED:
                                    VisionModelProvider.getModel().disableTraining();
                                    break;
                                case NOT_STARTED:
                                    break;
                            }
                        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Normalizes a camera image to [0; 1], cropping it to size expected by the model and adjusting
     * for camera rotation.
     */

    // Binding adapters:
    @BindingAdapter({"categoryId", "captureMode", "inferenceText", "captureText"})
    public static void setClassSubtitleText(TextView view, Integer categoryId, boolean captureMode, Float inferenceText, Integer captureText) {
        if (captureMode) {
            view.setText(captureText != null ? Integer.toString(captureText) : "0");
        } else {
            CameraActivity activity = (CameraActivity) view.getContext();
            if(categoryId == 1 && inferenceText != null && inferenceText > 0.8f && !activity.handlerStarted){
                activity.onIdentityVerified();
            }
            view.setText(String.format(Locale.getDefault(), "%.2f ", inferenceText != null ? inferenceText : 0.f));
        }
    }

    @BindingAdapter({"android:visibility"})
    public static void setViewVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter({"highlight"})
    public static void setClassButtonHighlight(View view, boolean highlight) {
        int drawableId;
        if (highlight) {
            drawableId = R.drawable.btn_default_highlight;
        } else {
            drawableId = R.drawable.btn_default;
        }
        view.setBackground(AppCompatResources.getDrawable(view.getContext(), drawableId));
    }
}
