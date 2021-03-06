package de.kp.spark.outlier.actor
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

import akka.actor.{Actor,ActorLogging,ActorRef,Props}

import de.kp.spark.core.Names
import de.kp.spark.core.model._

import de.kp.spark.core.redis.RedisDB

import de.kp.spark.outlier.model._

class OutlierQuestor extends BaseActor {

  implicit val ec = context.dispatcher
  private val redis = new RedisDB(host,port.toInt)

  def receive = {
    
    case req:ServiceRequest => {
      
      val origin = sender
      val uid = req.data("uid")
      
      val Array(task,topic) = req.task.split(":")
      topic match {

        case "state" => {

          val response = {

            if (redis.outliersExists(req) == false) {   
              failure(req,Messages.OUTLIERS_DO_NOT_EXIST(uid))
            
            } else {       
                
              val outliers = redis.outliers(req)

              val data = Map(Names.REQ_UID -> uid, Names.REQ_RESPONSE -> outliers)            
              new ServiceResponse(req.service,req.task,data,OutlierStatus.SUCCESS)
            
            }
          } 
          
          origin ! response
          context.stop(self)
          
        }
         
        case "vector" => {

          val response = {

            if (redis.pointsExist(req) == false) {    
              failure(req,Messages.OUTLIERS_DO_NOT_EXIST(uid))
            
            } else {         
                
              val points = redis.points(req)

              val data = Map(Names.REQ_UID -> uid, Names.REQ_RESPONSE -> points)            
              new ServiceResponse(req.service,req.task,data,OutlierStatus.SUCCESS)
             
            }
          
          } 
          origin ! response
          context.stop(self)
           
        }
       
        case _ => {
          
          val msg = Messages.TASK_IS_UNKNOWN(uid,req.task)
          
          origin ! failure(req,msg)
          context.stop(self)
          
        }
        
      }
      
    }
  
  }
 
}