package com.test.ar;

import com.google.ar.sceneform.rendering.ModelRenderable;

public class ModelReference {
    private String id;
    private ModelRenderable model;

    ModelReference(String id, ModelRenderable model) {
        this.id = id;
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public ModelRenderable getModel() {
        return model;
    }
}
