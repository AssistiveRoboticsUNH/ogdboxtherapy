package edu.uml.odgboxtherapy;

/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.ros.android.RosActivity;
//import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;

/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Odgboxtherapy extends RosActivity {

    private int cameraId;
    private MyRosCameraPreviewView rosCameraPreviewView;

    private Board gameBoard;
    private GestureSubscriber gestureSub;
    private TipPointSubscriber tipPointSub;

    public Odgboxtherapy() {
        super("CameraTutorial", "CameraTutorial");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        //rosCameraPreviewView = (MyRosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);

        //sets up views
        FrameLayout frame = new FrameLayout(this);
        addContentView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rosCameraPreviewView = new MyRosCameraPreviewView(this);
        frame.addView(rosCameraPreviewView);

        //creates game board view
        GameView drawontop = new GameView(this);
        addContentView(drawontop, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        drawontop.setVisibility(View.VISIBLE);
        drawontop.bringToFront();

        gameBoard = drawontop.getBoard();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && rosCameraPreviewView != null) {
            int numberOfCameras = Camera.getNumberOfCameras();
            final Toast toast;
            if (numberOfCameras > 1) {
                cameraId = (cameraId + 1) % numberOfCameras;
                rosCameraPreviewView.releaseCamera();
                rosCameraPreviewView.setCamera(Camera.open(cameraId));
                toast = Toast.makeText(this, "Switching cameras.", Toast.LENGTH_SHORT);
            } else {
                toast = Toast.makeText(this, "No alternative cameras to switch to.", Toast.LENGTH_SHORT);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast.show();
                }
            });
        }
        return true;
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        cameraId = 0;

        rosCameraPreviewView.setCamera(Camera.open(cameraId));

        gestureSub = new GestureSubscriber(gameBoard);
        tipPointSub = new TipPointSubscriber(gameBoard);

        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            nodeMainExecutor.execute(gestureSub, nodeConfiguration);
            nodeMainExecutor.execute(tipPointSub, nodeConfiguration);
            nodeMainExecutor.execute(rosCameraPreviewView, nodeConfiguration);
        } catch (IOException e) {
            // Socket problem
            Log.e("Camera Tutorial", "socket error trying to get networking information from the master uri");
        }

    }
}