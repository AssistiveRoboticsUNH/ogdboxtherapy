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

package edu.uml.odgboxtherapy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.common.base.Preconditions;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Publishes preview frames.
 *
 * @author damonkohler@google.com (Damon Kohler)
 */
class CompressedImagePublisher implements RawImageListener {

    /* ROS objects */
    private final ConnectedNode connectedNode;
    private final Publisher<sensor_msgs.CompressedImage> imagePublisher;

    /* image warping parameters */
    public static final double xmag = 2.2, ymag = 2;
    public static final int xtrans = -750, ytrans = -700;
    public static final double SCALE_FACTOR = .25;

    /* image processing variables */
    private ChannelBufferOutputStream stream;
    private Rect rect;
    private byte[] scaled;

    /* thread handling */
    private Object streamLock;
    private Queue<byte[]> frameQueue;


    public CompressedImagePublisher(ConnectedNode connectedNode) {

        //instantiating ros objects
        NameResolver resolver = connectedNode.getResolver().newChild("/ball_mover/camera");
        this.connectedNode = connectedNode;

        imagePublisher =
                connectedNode.newPublisher(resolver.resolve("image/compressed"),
                        sensor_msgs.CompressedImage._TYPE);

        //instantiating stream buffer for image processing
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        streamLock = new Object();
        frameQueue = new LinkedList<byte[]>();
    }

    @Override
    public void onNewRawImage(byte[] data, Size size) {

        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(size);

        frameQueue.add(data);
        if(scaled == null)
            scaled = new byte[size.width / 2 * size.height / 2 * 3 / 4];

        quarterYUV420(data, scaled, size.width, size.height);
        YuvImage yuvImage = new YuvImage(scaled, ImageFormat.NV21, size.width / 4, size.height / 4, null);

        if (rect == null)
            rect = new Rect(0, 0, size.width / 4, size.height / 4);

        Time currentTime = connectedNode.getCurrentTime();
        String frameId = "camera";

        sensor_msgs.CompressedImage image = imagePublisher.newMessage();
        image.setFormat("jpeg");
        image.getHeader().setStamp(currentTime);
        image.getHeader().setFrameId(frameId);

        synchronized (streamLock) {
            Preconditions.checkState(yuvImage.compressToJpeg(rect, 20, stream));
            image.setData(stream.buffer().copy());
            stream.buffer().clear();
        }

        //LOCK IT
        Thread.currentThread().setPriority(Thread.currentThread().getPriority()-1);
        while (frameQueue.peek() != data);
        Thread.currentThread().setPriority(Thread.currentThread().getPriority()+1);

        imagePublisher.publish(image);
        frameQueue.remove();
    }

    public static void quarterYUV420(byte[] data, byte[] yuv, int imageWidth, int imageHeight) {

        // quarter yuma
        int i = 0;
        for (int y = 0; y < imageHeight; y += 4) {
            for (int x = 0; x < imageWidth; x += 4) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }

        // quarter U and V color components
        for (int y = 0; y < imageHeight / 2; y += 4) {
            for (int x = 0; x < imageWidth; x += 8) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i++;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x + 1)];
                i++;
            }
        }
    }
}
