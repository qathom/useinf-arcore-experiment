package com.test.ar;

import android.content.Context;
import android.util.Log;

import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class ModelManager {
    private Context context;
    private ArrayList<String> modelResources = new ArrayList<>();
    private ArrayList<ModelReference> models = new ArrayList<>();

    ModelManager(Context ctx) {
        context = ctx;

        // List of resources (3D model objects) - sfb files
        modelResources.add("vase");
        modelResources.add("teapot");
        modelResources.add("elephant");
        modelResources.add("owl");
    }

    public ArrayList<String> getModelResources() {
        return modelResources;
    }

    private void setModelRenderable(String id, ModelRenderable model) {
        models.add(new ModelReference(id, model));
    }

    public CompletableFuture<Void> buildModel(String resName) {
        int resId = context.getResources().getIdentifier("raw/" + resName, null, context.getPackageName());

        return ModelRenderable.builder()
                .setSource(context, resId)
                .build()
                .thenAccept(renderable -> setModelRenderable(resName, renderable))
                .exceptionally(
                        throwable -> {
                            Log.e("ModelManager", "Error for model: " + resName);
                            return null;
                        });
    }

    public ModelReference get(String resName) {
        for (ModelReference ref : models) {
            if (ref.getId() == resName) {
                return ref;
            }
        }

        return null;
    }
}
