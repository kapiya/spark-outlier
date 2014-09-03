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

import de.kp.spark.outlier.model._
import de.kp.spark.outlier.util.{DetectorCache,PredictorCache}

class OutlierQuestor extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  def receive = {
    
    case req:OutlierRequest => {
      
      val origin = sender
      
      val (uid,task) = (req.uid,req.task)
      task match {
        
        case "outlier" => {

          val method = req.method.getOrElse(null)
          val resp = if (method == null) {
            val message = OutlierMessages.NO_METHOD_PROVIDED(uid)
            new OutlierResponse(uid,Some(message),None,None,OutlierStatus.FAILURE) 
          
          } else {
            if (method == "detect") {

              if (DetectorCache.exists(uid) == false) {           
                val message = OutlierMessages.OUTLIERS_DO_NOT_EXIST(uid)
                new OutlierResponse(uid,Some(message),None,None,OutlierStatus.FAILURE)
            
              } else {            
                val outliers = DetectorCache.outliers(uid)
                new OutlierResponse(uid,None,Some(outliers),None,OutlierStatus.SUCCESS)
            
              }
              
            } else if (method == "predict") {

              if (PredictorCache.exists(uid) == false) {           
                val message = OutlierMessages.OUTLIERS_DO_NOT_EXIST(uid)
                new OutlierResponse(uid,Some(message),None,None,OutlierStatus.FAILURE)
            
              } else {            
                val outliers = PredictorCache.outliers(uid)
                new OutlierResponse(uid,None,None,Some(outliers),OutlierStatus.SUCCESS)
            
              }
            
            } else {
              val message = OutlierMessages.METHOD_NOT_SUPPORTED(uid)
              new OutlierResponse(uid,Some(message),None,None,OutlierStatus.FAILURE) 
      
            }
            
          }
           
          origin ! OutlierModel.serializeResponse(resp)
           
        }
        
      }
      
    }
  
  }
}