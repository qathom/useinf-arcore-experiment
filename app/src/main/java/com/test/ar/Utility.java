package com.test.ar;

import com.google.ar.sceneform.math.Vector3;

public class Utility {
    public static float getDistanceBetweenVectors(Vector3 to, Vector3 from) {
        // Compute the difference vector between the two hit locations.
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float dz = to.z - from.z;

        // Compute the straight-line distance (distanceMeters)
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
