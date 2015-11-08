/*
 * Copyright (c) 2015 Brian Groenke
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.osu.cse.groenke.vrlib

import de.lessvoid.coregl.jogl.CoreSetupJogl
import de.lessvoid.coregl.jogl.JoglCoreGL
import de.lessvoid.math.Vec3
import de.lessvoid.coregl.input.spi.CoreKeyEvent
import de.lessvoid.coregl.input.spi.CoreKeyListener
import de.lessvoid.coregl.spi.CoreGL
import java.util.concurrent.Executors

object Main {

  val WindowSize = (1024, 768);

  def main(args: Array[String]) {
    premain()
    /*
    if (args.length != 8) usage();

    val (udim, vdim) = (args(0).toInt, args(1).toInt)
    val (alpha, beta, gamma) = (args(4).toFloat, args(5).toFloat, args(6).toFloat)

    val dataIn = new DataInputStream(new FileInputStream(args(2)))
    val dims = for (_ <- 1 to 3) yield dataIn.readInt
    val (xdim, ydim, zdim) = (dims(0), dims(1), dims(2))
    val size = xdim*ydim*zdim;
    val volume = FloatBuffer.allocate(size)
    while (volume.remaining() > 0) volume.put(dataIn.readFloat())
    volume.rewind()
    *
    */
    val (xdim, ydim, zdim) = (20, 20, 5)
    val (udim, vdim) = WindowSize;
    val gl = new JoglCoreGL
    val volume = gl.getUtil.createFloatBuffer(Array.fill[Float](xdim*ydim*zdim)(0.5f))

    val glsetup = new CoreSetupJogl(gl)
    glsetup.enableVSync(true)
    glsetup.initialize("Volume Rendering Test", udim, vdim)
    glsetup.initializeLogging
    val input = glsetup.getInput
    val vr = new VolumeRender(xdim, ydim, zdim, udim, vdim, volume)
    input.addListener(new InputListener(vr))
    vr.viewOffs = new Vec3(0, 0, -10)
    vr.viewRotation = new Vec3(0,0,0)
    glsetup.renderLoop(vr)
    glsetup.destroy
  }
  
  private class InputListener(vr: VolumeRender) extends CoreKeyListener {
    
    val DELTA_OFFS = 0.2f
    
    def keyPressed(event: CoreKeyEvent) {
      val code = event.getKeyCode
      if (code == event.VK_UP()) {
        vr.viewRotation = vr.viewRotation.translate(0, DELTA_OFFS, 0)
      } else if (code == event.VK_DOWN()) {
        vr.viewRotation = vr.viewRotation.translate(0, -DELTA_OFFS, 0)
      } else if (code == event.VK_RIGHT()) {
        if (event.isControlDown()) vr.viewRotation = vr.viewRotation.translate(0, 0, DELTA_OFFS)
        else vr.viewRotation = vr.viewRotation.translate(DELTA_OFFS, 0, 0)
      } else if (code == event.VK_LEFT()) {
        if (event.isControlDown()) vr.viewRotation = vr.viewRotation.translate(0, 0, -DELTA_OFFS)
        else vr.viewRotation = vr.viewRotation.translate(-DELTA_OFFS, 0, 0)
      }
    }
    
    def keyReleased(event: CoreKeyEvent) {
      
    }
  }

  private def usage() {
    println("usage: <program> udim vdim volume colormap alpha beta gamma out");
    System.exit(1)
  }

  private def premain() {

  }
}