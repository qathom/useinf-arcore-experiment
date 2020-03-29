package com.test.ar;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;

public class DoubleTapDetector implements Node.OnTouchListener {
    private Context context;
    private NodeListener listener;
    private GestureDetector gd;

    DoubleTapDetector(Context ctx) {
        context = ctx;

        gd = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                listener.onNodeDoubleTap();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                this.onDoubleTap(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                listener.onNodeTouched();
                return true;
            }
        });
    }

    public void setListener(NodeListener lt) {
        listener = lt;
    }

    @Override
    public boolean onTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
        return gd.onTouchEvent(motionEvent);
    }
}
