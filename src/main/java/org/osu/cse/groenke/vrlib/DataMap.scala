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
import scala.collection.mutable.ArraySeq
import scala.collection.mutable.MutableList
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import de.lessvoid.coregl.spi.CoreGL
import de.lessvoid.coregl.CoreShader

/**
 * Define controlled map function types
 */
abstract class MapFunction
case object Constant extends MapFunction
case object Linear extends MapFunction

/**
 * ColorMap maps color data to data ranges. <code>uploadToShader</code> can be used to upload map
 * data to the vrlib frag shader.
 */
class ColorMap(val min: Float,
		          val func1: MapFunction,
		          val minColor: Color,
              val max: Float,
		          val func2: MapFunction,
		          val maxColor: Color) {

	private class Entry(val dataVal: Float, var func: MapFunction, var color: Color) {}

	private val entries = new ArrayBuffer[Entry](2)

	// insert initial entries
	entries.insertAll(0, Array[Entry](new Entry(min, func1, minColor), new Entry(max, func2, maxColor)))

  /**
   * Adds a data entry with the given base value, map function, and base color.
   */
	def addEntry(dataVal: Float, func: MapFunction, color: Color) {
    val newEntry = new Entry(dataVal, func, color)
    val insertAt = findIndexFor(newEntry.dataVal, entries)
    entries.insert(insertAt, newEntry)
	}

  /**
   * Updates the entry containing the given data value with a new map function and/or color.
   */
  def changeEntry(dataVal: Float, newFunc: MapFunction, newColor: Color) {
    val valEntry = entries(findIndexFor(dataVal, entries))
    valEntry.func = newFunc; valEntry.color = newColor
  }

  /**
   * Removes the entry containing the given data value and returns a 3-tuple with the base data value,
   * map function, and color.
   */
  def removeEntry(dataVal: Float): (Float, MapFunction, Color) = {
    val entry = entries.remove(findIndexFor(dataVal, entries))
    return (entry.dataVal, entry.func, entry.color)
  }

  /**
   * Uploads the entries from this DataMap to the appropriate uniform values in the vrlib shader.
   */
  def uploadToShader(gl: CoreGL, shader: CoreShader) {
    val mapValBuff = gl.getUtil.createDoubleBuffer(2 * entries.length)
    val colorBuff = gl.getUtil.createFloatBuffer(4 * entries.length)
    val colorComps = new Array[Float](4)
    for (next <- entries) {
      mapValBuff.put(next.dataVal)
      mapValBuff.put(shaderFlagFor(next.func))
      next.color.getComponents(colorComps)
      colorBuff.put(colorComps)
    }
    mapValBuff.rewind()
    colorBuff.rewind()
    shader.setUniformdv("mapValues", 2, mapValBuff)
    shader.setUniformfv("mapColors", 4, colorBuff)
  }

	private def findIndexFor(dataVal: Float, entrySeq: Seq[Entry]): Int = {
    entrySeq.lastIndexWhere { x => x.dataVal < dataVal }
	}

  private def shaderFlagFor(func: MapFunction): Int = func match {
    case Constant => 0
    case Linear => 1
  }
}