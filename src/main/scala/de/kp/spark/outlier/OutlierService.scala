package de.kp.spark.outlier
/* Copyright (c) 2014 Dr. Krusche & Partner PartG
* 
* This file is part of the Spark-Outlier project
* (https://github.com/skrusche63/spark-outlier).
* 
* Spark-Outlier is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* Spark-Outlier is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* Spark-Outlier. 
* 
* If not, see <http://www.gnu.org/licenses/>.
*/

import akka.actor.{ActorSystem,Props}
import com.typesafe.config.ConfigFactory

import de.kp.spark.core.SparkService
import de.kp.spark.outlier.actor.OutlierMaster

/**
 * The OutlierService supports two different approaches to outlier discovery; one is based 
 * on clustering analysis and determines outlier feature sets due to their distance to the
 * cluster centers. This approach is independent of a certain use case and concentrates on
 * the extraction and evaluation of (equal-size) feature vectors. The other approach to 
 * outlier discovery has a strong focus on the customers purchase behavior and detects those
 * customer that behave different from all other customers.
 */
object OutlierService {

  def main(args: Array[String]) {
    
    val name:String = "outlier-server"
    val conf:String = "server.conf"

    val server = new OutlierService(conf, name)
    while (true) {}
    
    server.shutdown
      
  }

}

class OutlierService(conf:String, name:String) extends SparkService {

  val system = ActorSystem(name, ConfigFactory.load(conf))
  sys.addShutdownHook(system.shutdown)
  
  /* Create Spark context */
  private val sc = createCtxLocal("OutlierContext",Configuration.spark)      
  val master = system.actorOf(Props(new OutlierMaster(sc)), name="outlier-master")

  def shutdown = system.shutdown()
  
}