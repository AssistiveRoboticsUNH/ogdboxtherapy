package edu.uml.odgboxtherapy;

import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import java.util.zip.CheckedOutputStream;

/**
 * Created by chris on 9/18/15.
 */

public class GestureSubscriber implements NodeMain {

    private final Board gameBoard;

    /* contains the last pose number read from myo */
    public static int lastGesture = 0;
    public static int GRASPING = 1;

    public GestureSubscriber(Board gameBoard) {
        this.gameBoard = gameBoard;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<std_msgs.Int32> subscriber = connectedNode.newSubscriber("/myo_raw/gesture_num", std_msgs.Int32._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.Int32>() {
            @Override
            public void onNewMessage(std_msgs.Int32 message) {

                int prevLastGesture = lastGesture;
                lastGesture = message.getData();

//                if(SampleCameraControl.getInstance().gameState == SampleCameraControl.State.INITIAL
//                        && lastGesture == 1) {
//                    SampleCameraControl.getInstance().gameState = SampleCameraControl.State.LVL1;
//                }

                Log.d("myODG", "I heard: \"" + message + "\"");
            }
        });
    }

    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("sony_gesture_sub");
    }
}