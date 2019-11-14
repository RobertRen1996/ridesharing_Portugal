package traits

import scala.collection.mutable.ListBuffer
import model.Vehicle
import model.Grid


trait Recommend {
  def recommend(vehicles:ListBuffer[Vehicle],  graph:Map[Int, Grid])
}