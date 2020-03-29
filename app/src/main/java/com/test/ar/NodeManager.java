package com.test.ar;

import android.content.Context;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.util.ArrayList;

public class NodeManager {
    private ArrayList<TransformableNode> nodes = new ArrayList<>();
    private TransformableNode activeNode;
    private Context context;
    private Scene scene;

    NodeManager(Context ctx, Scene s) {
        context = ctx;
        scene = s;
    }

    public void add(TransformationSystem system, AnchorNode anchorNode, ModelReference modelRef) {
        // Create the transformable andy and add it to the anchor.
        TransformableNode node = new TransformableNode(system);
        node.setName(modelRef.getId());
        node.setParent(anchorNode);
        node.setRenderable(modelRef.getModel());
        node.setOnTapListener((v, event) -> {
            // Update active node
            node.select();
            this.setActive(node);
        });
        node.select();

        DoubleTapDetector detector = new DoubleTapDetector(context);
        detector.setListener(new NodeListener() {
            @Override
            public void onNodeTouched() {
                // Delete on double tap
                setActive(node);
                node.select();
            }

            @Override
            public void onNodeDoubleTap() {
                // Delete on double tap
                deleteActive();
            }
         }
        );

        node.setOnTouchListener(detector);

        // Add node in the collection
        nodes.add(node);

        // Set new node as active
        setActive(node);
    }

    public void setActive(TransformableNode node) {
        activeNode = node;
    }

    public TransformableNode getActive() {
        return activeNode;
    }

    public ArrayList<TransformableNode> getAll() {
        return nodes;
    }

    public void deleteAll() {
        for (TransformableNode node : nodes) {
            scene.removeChild(node.getParent());
        }

        nodes.clear();

        activeNode = null;
    }

    private void deleteActive() {
        if (activeNode == null) {
            return;
        }

        // Delete from collection
        nodes.removeIf(n -> n == activeNode);

        // Delete from scene
        scene.removeChild(activeNode.getParent());

        activeNode = null;
    }
}
