package edu.uml.odgboxtherapy;

import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

/**
 * Created by chris on 9/18/15.
 */

public class TipPointSubscriber implements NodeMain {

    private final Board gameBoard;

    public TipPointSubscriber(Board gameBoard) {
        this.gameBoard = gameBoard;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<geometry_msgs.Point> subscriber = connectedNode.newSubscriber("/ball_mover/tip_point", geometry_msgs.Point._TYPE);
        subscriber.addMessageListener(new MessageListener<geometry_msgs.Point>() {
            @Override
            public void onNewMessage(geometry_msgs.Point message) {

                //do not update ball's position if the ball is not found
                if(message.getX() == Ball.NOT_FOUND_COORD
                        || message.getY() == Ball.NOT_FOUND_COORD) {
                        // || GestureSubscriber.lastGesture != GestureSubscriber.GRASPING) { TODO implement grasping
                    return;
                }

                gameBoard.getBall().setTargetX( CompressedImagePublisher.xtrans
                        + message.getX() * CompressedImagePublisher.xmag / CompressedImagePublisher.SCALE_FACTOR);
                gameBoard.getBall().setTargetY( CompressedImagePublisher.ytrans
                        + message.getY() * CompressedImagePublisher.ymag / CompressedImagePublisher.SCALE_FACTOR);

                Log.d("myODG", "I heard: " + message.getX() + " " + message.getY());
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
        return GraphName.of("sony_ball_motion_sub");
    }
}
