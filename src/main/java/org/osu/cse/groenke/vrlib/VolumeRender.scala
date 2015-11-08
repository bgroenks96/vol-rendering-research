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

import java.awt.Color
import java.nio.FloatBuffer

import de.lessvoid.coregl.CoreShader
import de.lessvoid.coregl.CoreVAO
import de.lessvoid.coregl.CoreVAO.FloatType
import de.lessvoid.coregl.CoreVBO
import de.lessvoid.coregl.CoreVBO.DataType
import de.lessvoid.coregl.CoreVBO.UsageType
import de.lessvoid.coregl.spi.CoreGL
import de.lessvoid.coregl.spi.CoreSetup.RenderLoopCallback
import de.lessvoid.math.Vec3

class VolumeRender (xdim: Int, ydim: Int, zdim: Int, udim: Int, vdim: Int, volume: FloatBuffer) extends RenderLoopCallback {

  var viewRotation = new Vec3(0,0,0)
  var viewOffs = new Vec3(0,0,0)
  var colorMapFile: String = ""

  private val (vertShaderLoc, fragShaderLoc) = ("vrender.vert", "vrender.frag")

  private var vrshader: CoreShader = _

  private var vao: CoreVAO = _

  private var vbo: CoreVBO[FloatBuffer] = _

  private var dataTexId: Int = _

  private var colorMap: ColorMap = _

  def endLoop = false

  def init(gl: CoreGL) {
    vrshader = CoreShader.createShader(gl)
    vrshader.vertexShader("vrVertLoader", ClassLoader.getSystemClassLoader.getResourceAsStream(vertShaderLoc))
    vrshader.fragmentShader("vrFragLoader", ClassLoader.getSystemClassLoader.getResourceAsStream(fragShaderLoc))
    vrshader.link
    vrshader.activate

    // create static VBO for view quad
    vbo = CoreVBO.createCoreVBO(gl, DataType.FLOAT, UsageType.STATIC_DRAW, 8)
    vbo.bind
    // coordinates for a full screen quad in clip space
    val viewCoords = Array[Float](-1,-1,-1,1,1,-1,1,1)
    val viewCoordBuffer = vbo.getBuffer()
    // write coordinates to buffer and rewind before sending
    viewCoordBuffer.put(viewCoords)
    viewCoordBuffer.rewind
    vbo.send
    updateView(gl)

    vao = CoreVAO.createCoreVAO(gl)
    vao.bind
    vao.enableVertexAttribute(0)
    vao.vertexAttribPointer(0, 2, FloatType.FLOAT, 2, 0)

    // allocate and fill 3D texture with volume data
    val texIdBuff = gl.getUtil.createIntBuffer(1)
    gl.glGenTextures(1, texIdBuff)
    gl.checkGLError("glGenTextures")
    texIdBuff.rewind()
    dataTexId = texIdBuff.get()
    gl.glBindTexture(gl.GL_TEXTURE_3D, dataTexId)
    gl.checkGLError("glBindTexture")
    gl.glTexImage3D(gl.GL_TEXTURE_3D,
                    0,
                    gl.GL_R32F,
                    xdim,
                    ydim,
                    zdim,
                    0,
                    gl.GL_RED,
                    gl.GL_FLOAT,
                    volume)
    gl.checkGLError("glTexImage3D")

    // set sampler value
    vrshader.setUniformi("sVolume", 0);
    // set volume and step size uniforms
    vrshader.setUniformf("fBoundX", xdim)
    vrshader.setUniformf("fBoundY", ydim)
    vrshader.setUniformf("fBoundZ", zdim)
    vrshader.setUniformf("fStepSize", 1.0f)
  }

  def render(gl: CoreGL, timeSinceLastRender: Float): Boolean = {
    updateView(gl)
    gl.glClear(gl.GL_COLOR_BUFFER_BIT)
    gl.glDrawArrays(gl.GL_TRIANGLE_STRIP, 0, 4)
    gl.checkGLError("glDrawArrays")
    return true
  }

  // uploads view location data to shader uniforms
  private def updateView(gl: CoreGL) {
    vrshader.setUniformfv("vClipDims", 2, udim, vdim);
    vrshader.setUniformfv("vViewRotation", 3, viewRotation.getX, viewRotation.getY, viewRotation.getZ)
    vrshader.setUniformfv("vViewOffset", 3, viewOffs.getX, viewOffs.getY, viewOffs.getZ)
  }

  // load color map data from file
  private def loadColorMap(gl: CoreGL, fileName: String) {
    val map = new ColorMap(0, Linear, Color.WHITE, 1, Constant, Color.BLACK)
    map.uploadToShader(gl, vrshader)
  }
}