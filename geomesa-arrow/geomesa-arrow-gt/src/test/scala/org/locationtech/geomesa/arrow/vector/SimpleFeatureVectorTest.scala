/***********************************************************************
* Copyright (c) 2013-2017 Commonwealth Computer Research, Inc.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License, Version 2.0
* which accompanies this distribution and is available at
* http://www.opensource.org/licenses/apache2.0.php.
*************************************************************************/

package org.locationtech.geomesa.arrow.vector

import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.complex.FixedSizeListVector
import org.junit.runner.RunWith
import org.locationtech.geomesa.features.ScalaSimpleFeature
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
import org.locationtech.geomesa.utils.io.WithClose
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SimpleFeatureVectorTest extends Specification {

  val sft = SimpleFeatureTypes.createType("test", "name:String,dtg:Date,*geom:Point:srid=4326")
  val features = (0 until 10).map { i =>
    ScalaSimpleFeature.create(sft, s"0$i", s"name0${i % 2}", s"2017-03-15T00:0$i:00.000Z", s"POINT (4$i 5$i)")
  }

  implicit val allocator = new RootAllocator(Long.MaxValue)

  "SimpleFeatureVector" should {
    "set and get values" >> {
      WithClose(SimpleFeatureVector.create(sft, Map.empty)) { vector =>
        features.zipWithIndex.foreach { case (f, i) => vector.writer.set(i, f) }
        vector.writer.setValueCount(features.length)
        vector.reader.getValueCount mustEqual features.length
        forall(0 until 10)(i => vector.reader.get(i) mustEqual features(i))
      }
    }
    "wrap values" >> {
      WithClose(SimpleFeatureVector.create(sft, Map.empty)) { vector =>
        features.zipWithIndex.foreach { case (f, i) => vector.writer.set(i, f) }
        vector.writer.setValueCount(features.length)
        WithClose(SimpleFeatureVector.wrap(vector.underlying, Map.empty)) { wrapped =>
          wrapped.reader.getValueCount mustEqual features.length
          forall(0 until 10)(i => wrapped.reader.get(i) mustEqual features(i))
        }
      }
    }
    "set and get dictionary encoded values" >> {
      val dictionary = Map("name" -> new ArrowDictionary(Seq("name00", "name01")))
      WithClose(SimpleFeatureVector.create(sft, dictionary)) { vector =>
        features.zipWithIndex.foreach { case (f, i) => vector.writer.set(i, f) }
        vector.writer.setValueCount(features.length)
        vector.reader.getValueCount mustEqual features.length
        forall(0 until 10)(i => vector.reader.get(i) mustEqual features(i))
      }
    }
    "wrap underlying dictionary encoded vectors" >> {
      val dictionary = Map("name" -> new ArrowDictionary(Seq("name00", "name01")))
      WithClose(SimpleFeatureVector.create(sft, dictionary)) { vector =>
        features.zipWithIndex.foreach { case (f, i) => vector.writer.set(i, f) }
        vector.writer.setValueCount(features.length)
        WithClose(SimpleFeatureVector.wrap(vector.underlying, dictionary)) { wrapped =>
          wrapped.reader.getValueCount mustEqual features.length
          forall(0 until 10)(i => wrapped.reader.get(i) mustEqual features(i))
        }
      }
    }
  }

  step {
    allocator.close()
  }
}