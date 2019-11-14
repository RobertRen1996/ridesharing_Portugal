package traits


import scala.collection.mutable.Map
import model.Passenger
import model.Vehicle


trait Task {
  //这是一个接口，每一个Task都会去继承
  //def exec(tasks : Map[String, List[Task]], currentTime:String ){
    
  //}
  var passenger:Passenger
  var vehicle:Vehicle
  def exec(currentTime : Long)
}