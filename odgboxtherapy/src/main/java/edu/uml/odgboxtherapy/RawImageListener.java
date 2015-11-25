package edu.uml.odgboxtherapy;

import android.hardware.Camera.Size;

interface RawImageListener {

  void onNewRawImage(byte[] data, Size size);

}