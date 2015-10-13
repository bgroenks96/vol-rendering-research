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

#define MAX_ENTRY_COUNT 20
#define FUNC_CONSTANT 0
#define FUNC_LINEAR 1

uniform float fBoundX, fBoundY, fBoundZ, fStepSize;

uniform sampler3D sVolume;

// data map uniform values
uniform vec2 mapValues[MAX_ENTRY_COUNT];
uniform vec4  mapColors[MAX_ENTRY_COUNT];
// -----------------------

in vec4 viewCoord, viewNormal;
in float stepOffset, stepLimit;

layout(location = 0) out vec4 fragOut;

bool inDataBounds(in vec4 pos) {
  return all(greaterThanEqual(pos.xyz, vec3(0,0,0))) &&
         all(lessThan(pos.xyz, vec3(fBoundX, fBoundY, fBoundZ)));
}

// assumes coordinate to be within data bounds
float sampleStep(in vec4 pos, in vec4 gradientStep) {
  vec4 spos = (pos + gradientStep) / length(vec3(fBoundX, fBoundY, fBoundZ));
  return texture(sVolume, spos.xyz).r; // sample only red channel
}

void main() {
  int istep = int(stepOffset / fStepSize);
  float dataMax = 0;
  for (int k = istep; k * fStepSize < stepLimit; k++) {
    vec4 stepVec = viewNormal * fStepSize * k;
    int inBounds = int(inDataBounds(viewCoord + stepVec));
    dataMax += sampleStep(viewCoord, stepVec) * inBounds;
    dataMax += 0.1 * inBounds;
  }
  fragOut = vec4(dataMax,0,0,1);
}