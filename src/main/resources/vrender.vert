#version 330

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

uniform float fBoundX, fBoundY, fBoundZ;
uniform vec2 vClipDims;
uniform vec3 vViewRotation;
uniform vec3 vViewOffset;

layout(location = 0) in vec2 viewPlaneCoord;

out vec4 viewCoord, viewNormal;
out float stepOffset, stepLimit;

void main() {
  // pass clip space vertex directly to gl_Position
  vec4 viewPos = vec4(viewPlaneCoord, 0, 1);
  gl_Position = viewPos;

  float x = vViewRotation.x, y = vViewRotation.y, z = vViewRotation.z;
  // create inverted view transform matrix from uniform data
  mat4 transform = inverse(mat4(
                   2/vClipDims.x*cos(y)*cos(z), cos(z)*sin(x)*sin(y)+cos(x)*sin(z), sin(x)*sin(z)-cos(x)*cos(z)*sin(y), 0,
                   -cos(y)*sin(z), 2/vClipDims.y*cos(x)*cos(z)-sin(x)*sin(y)*sin(z), cos(z)*sin(x)+cos(x)*sin(y)*sin(z), 0,
                   sin(y), -cos(y)*sin(x), cos(x)*cos(y), 0,
                   vViewOffset.x - 1, vViewOffset.y - 1, vViewOffset.z - 1, 1 ));

  // transform clip space coords back into world space for frag shader
  viewCoord = viewPos * transform;
  vec4 clipNormal = vec4(0, 0, 1, 1);
  viewNormal = clipNormal * transform;

  // calculate max sample-step distance and initial offset
  const int numBounds = 8; // 8 vertices in bounding box
  vec3 bounds[numBounds] = vec3[] (vec3(0,0,0), vec3(fBoundX,0,0), vec3(0,fBoundY,0), vec3(fBoundX,fBoundY,0),
                                   vec3(0,0,fBoundZ), vec3(0,fBoundY,fBoundZ), vec3(fBoundX,0,fBoundZ), vec3(fBoundX,fBoundY,fBoundZ));
  stepLimit = distance(viewCoord.xyz, bounds[0]);
  for (int i = 1; i < numBounds; i++) {
    stepLimit = max(stepLimit, distance(viewCoord.xyz, bounds[i]));
  }
  stepOffset = length(vViewOffset) - 0.5 * length(vClipDims);
}