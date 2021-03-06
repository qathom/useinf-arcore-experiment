package com.test.ar;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.AnimationData;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelReference selectedModel = null;
    private ModelManager modelManager;
    private NodeManager nodeManager;
    private InfoDialog dialog;
    private Button buttonSelectModel;
    private ModelAnimator animator;
    private int nextAnimation;
    private ArrayList<String> objectVisited = new ArrayList<>();
    private ProgressDialog progressDialog;

    /**
     * Threshold before showing the modal
     * with contextual information regarding the object
     * Specified in meters (m)
     */
    private float thresholdShowModal = (float) 0.38;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main);

        // AR fragment
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // Models
        modelManager = new ModelManager(this);

        // Select model
        buttonSelectModel = (Button) findViewById(R.id.buttonSelectModel);
        buttonSelectModel.setOnClickListener((View view) -> {
            showModelModal();
        });

        // Delete/reset all objects
        Button buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonReset.setOnClickListener((View view) -> {
            nodeManager.deleteAll();
        });

        // Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading model...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        // Hide navigation bar
        hideNavigationBar();

        // Setup AR
        setupAr();
    }

    private void setupAr() {
        // On tap listener
        arFragment.setOnTapArPlaneListener(this::onPlaneTap);

        // On update listener
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);

        nodeManager = new NodeManager(this, arFragment);
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void playAnimation(ModelRenderable model) {
        try {
            if (animator == null || !animator.isRunning()) {
                AnimationData data = model.getAnimationData(nextAnimation);

                nextAnimation = (nextAnimation + 1) % model.getAnimationDataCount();
                animator = new ModelAnimator(data, model);
                animator.start();

                Log.d(
                        TAG,
                        String.format(
                                "Starting animation %s - %d ms long", data.getName(), data.getDurationMs()));
            }
        } catch (Exception e) {
            Log.d(TAG, String.format(
                    "An error occurred while playing the animation %s", e.getMessage()));
        }
    }

    /**
     * Listener for plane tap gestures
     * @param hitResult
     * @param plane
     * @param motionEvent
     */
    private void onPlaneTap(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if (selectedModel == null) {
            // Open the modal so that the user can select an object
            showModelModal();
            return;
        }

        // Create the transformable node and add it to the anchor.
        nodeManager.add(arFragment, hitResult, selectedModel);
    }

    private String getContextualText(TransformableNode node) {
        long count = objectVisited.stream().filter(n -> n.equals(node.getName())).count();
        String base = node.getName().toUpperCase();

        if (count >= 1) {
            return base + "\n\n" + "Welcome back! You already visited this object " + count + " times.";
        }

        return base + "\n\n" + "This is your first visit.";
    }

    public void showInfoDialog(TransformableNode node) {
        String text = getContextualText(node);

        if (dialog == null) {
            // Add in history
            objectVisited.add(node.getName());

            dialog = new InfoDialog(this, text);
        }

        if (dialog.isShowing()) {
            return;
        }

        dialog.show();
    }

    public void hideInfoDialog() {
        if (dialog == null) {
            return;
        }

        if (!dialog.isShowing()) {
            return;
        }

        dialog.hide();
        dialog = null;
    }

    private void showModelModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose a model");

        // ArrayList to String[]
        String[] stringArray = modelManager.getModelResources().toArray(new String[0]);

        builder.setItems(stringArray, (dialog, index) -> {
            String modelName = stringArray[index];

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    ModelReference model = modelManager.get(modelName);

                    if (model != null) {
                        selectedModel = model;
                        showPlaceObjetToast(modelName);
                        return;
                    }

                    progressDialog.show();

                    modelManager.buildModel(modelName)
                            .whenComplete((r, t) -> {
                                selectedModel = modelManager.get(modelName);

                                progressDialog.dismiss();

                                showPlaceObjetToast(modelName);
                            });
                }
            });
        });

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void showPlaceObjetToast(String modelName) {
        Toast.makeText(getApplicationContext(), "You can now place" + " " + modelName, Toast.LENGTH_LONG).show();
    }

    /**
     *
     * @param unusedframeTime
     */
    public void onUpdate(FrameTime unusedframeTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Camera camera = frame.getCamera();

        if (camera.getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        Vector3 cameraPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();

        // Get closest object
        ArrayList<TransformableNode> nodes = nodeManager.getAll();
        TransformableNode closestNode = null;

        float closestDistance = -1;

        for (TransformableNode node : nodes) {
            // Use the utility class to get distance between vectors
            float distance = Utility.getDistanceBetweenVectors(cameraPosition, node.getWorldPosition());

            if (closestDistance == -1 || distance < closestDistance) {
                closestNode = node;
                closestDistance = distance;
                node.getRenderable();
            }
        }
        ;

        if (closestNode == null) {
            return;
        }

        // Distance
        if (closestDistance < thresholdShowModal) {
            // Focus object
            closestNode.select();

            showInfoDialog(closestNode);

            playAnimation(modelManager.get(closestNode.getName()).getModel());
        } else {
            hideInfoDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listener
        if (arFragment != null) {
            arFragment.setOnTapArPlaneListener(null);
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}

