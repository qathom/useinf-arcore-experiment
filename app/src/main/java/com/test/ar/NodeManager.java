package com.test.ar;

import android.content.Context;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.util.ArrayList;
import java.util.List;

public class NodeManager {
    private Context context;
    private ArFragment arFragment;
    private ArrayList<TransformableNode> nodes = new ArrayList<>();

    NodeManager(Context ctx, ArFragment fragment) {
        context = ctx;
        arFragment = fragment;
    }

    public void add(ArFragment arFragment, HitResult hitResult, ModelReference modelRef) {
        TransformationSystem system = arFragment.getTransformationSystem();

        // Create the anchor
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable object and add it to the anchor
        TransformableNode node = new TransformableNode(system);
        node.setName(modelRef.getId());
        node.setParent(anchorNode);
        node.setRenderable(modelRef.getModel());
        node.setOnTapListener((v, event) -> {
            // Update active node
            node.select();
        });
        node.select();

        DoubleTapDetector detector = new DoubleTapDetector(context);
        detector.setListener(new NodeListener() {
            @Override
            public void onNodeTouched() {
                node.select();
            }

            @Override
            public void onNodeDoubleTap() {
                // Delete from scene
                arFragment.getArSceneView().getScene().removeChild(node.getParent());
            }
         }
        );

        // Add node in the collection
        nodes.add(node);

        node.setOnTouchListener(detector);
    }

    public ArrayList<TransformableNode> getAll() {
        return nodes;
    }

    public void deleteAll() {
        Scene scene = arFragment.getArSceneView().getScene();
        List<Node> children = new ArrayList<>(scene.getChildren());

        for (Node node: children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                }
            }
            if (!(node instanceof Camera) && !(node instanceof Sun)) {
                node.setParent(null);
            }
        }

        for (TransformableNode node : nodes) {
            scene.removeChild(node.getParent());
        }

        nodes.clear();
    }
}
