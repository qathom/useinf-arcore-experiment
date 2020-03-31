package com.test.ar;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.AnimationData;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;

/**
 * This activity uses the Sceneform UX package to make common AR tasks easier.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelReference selectedModel = null;
    private ModelManager modelManager;
    private NodeManager nm;
    private InfoDialog dialog;
    private LoadModelTask modelTask;
    private Button buttonSelectModel;
    private ModelAnimator animator;
    private int nextAnimation;

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

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        nm = new NodeManager(this, arFragment.getArSceneView().getScene());

        // Models
        modelManager = new ModelManager(this);

        // Select model
        buttonSelectModel = (Button) findViewById(R.id.buttonSelectModel);
        buttonSelectModel.setOnClickListener((View view) -> {
            showModelModal();
        });
        buttonSelectModel.setEnabled(false);

        // Delete/reset all objects
        Button buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonReset.setOnClickListener((View view) -> {
            this.nm.deleteAll();
        });

        arFragment.setOnTapArPlaneListener(this::onPlaneTap);

        // Render 3D models
        modelTask = new LoadModelTask();
        modelTask.execute();

        // Hide navigation bar
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void onPlayAnimation(ModelRenderable model) {
        try {
            if (animator == null || !animator.isRunning()) {
                AnimationData data = model.getAnimationData(nextAnimation);

                nextAnimation = (nextAnimation + 1) % model.getAnimationDataCount();
                animator = new ModelAnimator(data, model);
                animator.start();
                Toast toast = Toast.makeText(this, data.getName(), Toast.LENGTH_SHORT);

                Log.d(
                        TAG,
                        String.format(
                                "Starting animation %s - %d ms long", data.getName(), data.getDurationMs()));
            }
        } catch (Exception e) {
            Log.d(TAG,  String.format(
                    "An error occurred while playing the animation %s", e.getMessage()));
        }
    }

    private void onPlaneTap(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if (selectedModel == null) {
            showModelModal();
            Toast toast =
                    Toast.makeText(this, "Select an object to insert in the scene", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        // Create the Anchor.
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable andy and add it to the anchor.
        nm.add(arFragment.getTransformationSystem(), anchorNode, selectedModel);

        // Update
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
    }

    public void showInfoDialog(TransformableNode node) {
        if (dialog == null) {
            dialog = new InfoDialog(this, node.getName());
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

        builder.setItems(stringArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                String modelName = stringArray[index];
                ModelReference model = modelManager.get(modelName);

                if (model == null) {
                    Log.e(TAG, "Invalid model for: " + modelName);
                    return;
                }

                Toast.makeText(getApplicationContext(), "You can now place" + " " + modelName, Toast.LENGTH_LONG).show();

                selectedModel = model;
            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    public void onUpdate(FrameTime unusedframeTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Camera camera = frame.getCamera();

        if (camera.getTrackingState() == TrackingState.TRACKING) {
            Vector3 cameraPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();

            // Get closest object
            ArrayList<TransformableNode> nodes = nm.getAll();
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
            };

            if (closestNode == null) {
                return;
            }

            // Distance
            if (closestDistance < thresholdShowModal) {
                closestNode.select();

                showInfoDialog(closestNode);

                Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");

                onPlayAnimation(modelManager.get(closestNode.getName()).getModel());
            } else {
                hideInfoDialog();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listener
        arFragment.setOnTapArPlaneListener(null);

        // Cancel on update

        // Cancel running task(s) to avoid memory leaks
        if (modelTask != null) {
            modelTask.cancel(true);
        }
    }

    private class LoadModelTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            // Render models on UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    modelManager.buildAll();
                }
            });

            return true;
        }

        @Override
        protected void onPostExecute(Boolean res) {
            buttonSelectModel.setEnabled(res);
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

