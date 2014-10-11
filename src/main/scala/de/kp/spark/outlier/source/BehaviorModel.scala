package de.kp.spark.outlier.source
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

import org.apache.spark.rdd.RDD

import de.kp.spark.outlier.Configuration
import de.kp.spark.outlier.model.Behavior

import de.kp.spark.outlier.markov.StateSpec

import scala.collection.mutable.ArrayBuffer

/**
 * The BeviorModel holds the business logic for the markov chain 
 * based outlier detection approach; it takes 3 different parameters, 
 * 
 * - amount, 
 * - price and 
 * - time elasped 
 * 
 * since last transaction into account and transforms a transaction 
 * into a discrete state 
 */
class BehaviorModel() extends StateSpec with Serializable {
  
  private val model = Configuration.model
  /*
   * The parameters amount high, and norm specify threshold
   * value to determine, whether the amount of a transaction 
   * is classified as Low, Normal or High
   */
  private val AMOUNT_HIGH:Float = model("amount.height").toFloat
  private val AMOUNT_NORM:Float = model("amount.norm").toFloat
  /*
   * The parameter price high specifies, whether the transaction
   * holds a high price item
   */
  private val PRICE_HIGH:Float = model("price.high").toFloat
  /*
   * The parameters date small, and medium specify the days elapsed
   * since the last ecommerce transaction, which are classified as
   * Small, Medium, and Large
   */
  private val DATE_SMALL:Int  = model("date.small").toInt
  private val DATE_MEDIUM:Int = model("date.medium").toInt
        
  private val DAY = 24 * 60 * 60 * 1000 // day in milliseconds
  
  val FD_SCALE = 1
  /*
   * The state model comprises 18 states, which are built from the combination
   * of the individual states from amount, price and elasped time: APT model
   */
  val FD_STATE_DEFS = Array("LNL","LNN","LNS","LHL","LHN","LHS","MNL","MNN","MNS","MHL","MHN","MHS","HNL","HNN","HNS","HHL","HHN","HHS")
  
  override def scaleDef = FD_SCALE
  
  override def stateDefs = FD_STATE_DEFS
  
  /**
   * Represent transactions as a time ordered sequence of Markov States;
   * the result is directly used to build the respective Markov Model
   */
  def buildStates(sequences:RDD[Sequence]):RDD[Behavior] = {
    
    /*
     * We restrict to those sequences that comprise at least 2 orders
     * and convert these sequences into a sequence of states on a per
     * user basis, which is interpreted as behavior
     */
    sequences.filter(s => s.orders.size > 1).map(s => {

      /*
       * A sequence comprises a time-ordered (ascending) list of items
       */
      val (site,user,orders) = (s.site,s.user,s.orders)
      
      /* Extract first order */
      var endtime = orders.head._1
      val states = ArrayBuffer.empty[String]

      for ((starttime,items) <- orders.tail) {
        
        /* Determine state from amount */
        val astate = stateByAmount(items)
        
        /* Determine state from price */
        val pstate = stateByPrice(items)
      
        /* Determine state from time elapsed between
         * subsequent orders or transactions
         */
        val tstate = stateByDate(starttime,endtime)
      
        val state = astate + pstate + tstate
        states += state
        
        endtime = starttime
        
      }
      
      new Behavior(site,user,states.toList)
      
    })
    
  }
  
  /**
   * Determine the amount spent by a transaction and assign a classifier, "H", "N", "L"; 
   * this classifier specifies the (1) part of a transaction state description
   */
  private def stateByAmount(items:List[Item]):String = {
    
    val amount = items.map(item => item.price).sum
    (if (amount > AMOUNT_HIGH) "H" else if (amount > AMOUNT_NORM) "N" else "L")
    
  }
  
  /**
   * Determine whether transaction includes high price ticket item and assign a classifier "H", "N";
   * this classifier specifies the (2) part of a transaction state description. Actually, we do not
   * distinguish between transactions with one or more high price items
   */
  
  private def stateByPrice(items:List[Item]):String = {
    
    val states = items.map(item => {
      if (item.price > PRICE_HIGH) "H" else "N"      
    })
    
    if (states.contains("H")) "H" else "N"
   
  }
 
  /**
   * Time elapsed since last transaction
   * 
   * S : small
   * M : medium
   * L : large
   * 
   */
  private def stateByDate(ndate:Long,pdate:Long):String = {
    
    val d = (ndate -pdate) / DAY
    val dstate = (
        if (d < DATE_SMALL) "S"
        else if (d < DATE_MEDIUM) "M"
        else "L")
    
    dstate
  
  }
}