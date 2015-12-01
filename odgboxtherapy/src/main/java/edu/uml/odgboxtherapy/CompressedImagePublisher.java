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

/**
 * Publishes preview frames.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
class CompressedImagePublisher implements RawImageListener {

  /* ROS objects */
  private final ConnectedNode connectedNode;
  private final Publisher<sensor_msgs.CompressedImage> imagePublisher;
  private final Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;

  /* image warping parameters */
  public static final double xmag = 2.2, ymag = 2;
  public static final int xtrans = -750, ytrans = -700;
  public static final double SCALE_FACTOR = 0.3;

  private int skipFrame = 0;

  /* image processing variables */
  private byte[] rawImageBuffer;
  private Size rawImageSize;
  private YuvImage yuvImage;
  private Rect rect;
  private ChannelBufferOutputStream stream;

  public CompressedImagePublisher(ConnectedNode connectedNode) {

    //instantiating ros objects
    NameResolver resolver = connectedNode.getResolver().newChild("/ball_mover/camera");
    this.connectedNode = connectedNode;

    imagePublisher =
        connectedNode.newPublisher(resolver.resolve("image/compressed"),
            sensor_msgs.CompressedImage._TYPE);

    cameraInfoPublisher =
        connectedNode.newPublisher(resolver.resolve("camera_info"), sensor_msgs.CameraInfo._TYPE);

    //instantiating stream buffer for image processing
    stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
  }

  @Override
  public void onNewRawImage(byte[] data, Size size) {
//    if(skipFrame++ > 5) {
//      skipFrame = 0;
//      return;
//    }
    Preconditions.checkNotNull(data);
    Preconditions.checkNotNull(size);

    if (data != rawImageBuffer || !size.equals(rawImageSize)) {
      rawImageBuffer = data;
      rawImageSize = size;
      yuvImage = new YuvImage(rawImageBuffer, ImageFormat.NV21, size.width, size.height, null);
      rect = new Rect(0, 0, size.width, size.height);
    }

    Time currentTime = connectedNode.getCurrentTime();
    String frameId = "camera";

    sensor_msgs.CompressedImage image = imagePublisher.newMessage();
    image.setFormat("jpeg");
    image.getHeader().setStamp(currentTime);
    image.getHeader().setFrameId(frameId);

    //********************
    //shrinking for shipment to reduce latency

    ByteArrayOutputStream output_stream=new ByteArrayOutputStream();
    Preconditions.checkState(yuvImage.compressToJpeg(rect, 20, output_stream));

    int shrinked_width = (int)(SCALE_FACTOR * size.width);
    int shrinked_height = (int)(SCALE_FACTOR * size.height);

    Bitmap bitmap = BitmapFactory.decodeByteArray(output_stream.toByteArray(), 0, output_stream.size());
    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, shrinked_width, shrinked_height, true);
    ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, imageStream);

    try {
      stream.write(imageStream.toByteArray());
    } catch (IOException e) {
      e.printStackTrace();
    }

    image.setData(stream.buffer().copy());
    stream.buffer().clear();

    /******************
    //overlaying stuff to gather warping parameters
    final SurfaceHolder holder = Odgboxtherapy.warpedView.getHolder();

    Canvas c = holder.lockCanvas();
    if (holder != null && c != null) {

      BitmapFromCompressedImage bf = new BitmapFromCompressedImage();
      Bitmap bmp = bf.call(image);
      Bitmap scaled = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * xmag), (int) (bmp.getHeight() * ymag), false);

      c.drawBitmap(scaled, xtrans, ytrans, null);

      holder.unlockCanvasAndPost(c);
    }


     /*****************

    BitmapFromCompressedImage bf = new BitmapFromCompressedImage();

    Bitmap bmp = bf.call(image);
    if(bmp != null) {
      Log.d("myODG", MyRosCameraPreviewView.instance.getWidth() + "..." + MyRosCameraPreviewView.instance.getHeight());
      Bitmap scaled = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * 2), (int) (bmp.getHeight() * 1), false);

      Bitmap background = Bitmap.createBitmap(
              MyRosCameraPreviewView.instance.getWidth(),
              MyRosCameraPreviewView.instance.getHeight(),
              scaled.getConfig());

      Canvas c = new Canvas(background);
      c.drawBitmap(bmp, 0, 0, null);

      MyRosCameraPreviewView.instance.draw(c);
    }

    /***********************/

    imagePublisher.publish(image);

    sensor_msgs.CameraInfo cameraInfo = cameraInfoPublisher.newMessage();
    cameraInfo.getHeader().setStamp(currentTime);
    cameraInfo.getHeader().setFrameId(frameId);

    cameraInfo.setWidth(shrinked_width);
    cameraInfo.setHeight(shrinked_height);
    cameraInfoPublisher.publish(cameraInfo);
  }

  byte[] shrink(byte[] data, Size size) {

    if(data == null)
      return null;

    ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
    YuvImage image=new YuvImage(data, ImageFormat.NV21, size.width, size.height,null);
    image.compressToJpeg(new Rect(0, 0, size.width, size.height), 60, output_stream);

    size.width = (int)(SCALE_FACTOR * size.width);
    size.height = (int)(SCALE_FACTOR * size.height);

    Bitmap bitmap = BitmapFactory.decodeByteArray(output_stream.toByteArray(), 0, output_stream.size());
    Bitmap scaledBitmap=Bitmap.createScaledBitmap(bitmap, size.width, size.height, true);
    ByteArrayOutputStream imageStream=new ByteArrayOutputStream();
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, imageStream);

    return imageStream.toByteArray();
  }

}
