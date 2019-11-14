package task

import traits.Task
import model.Passenger
import org.slf4j.LoggerFactory
import loadData.Config.status.MISSING
import loadData.Parser.encodeTime
import simulate.simulate
import loadData.Parser.timeStamp2Date
import model.Point
import loadData.Config
import scala.util.control.Breaks._
import simulate.tasks
import model.Vehicle
import scala.collection.mutable.ListBuffer
import loadData.Config.VehicleTypes.{TURN_OFF, TURN_ON}
import model.Grid


class RemovePassengerTask  extends Task{
  val logger = LoggerFactory.getLogger(this.getClass());
  var passenger : Passenger = null
  var vehicle:Vehicle = null
  
  def this(passenger : Passenger){
    this()
    this.passenger = passenger
  }
  
  def exec(currentTime : Long){
    passenger.status = MISSING  //更改用户状态
//    logger.info("在"+ timeStamp2Date(currentTime+"") +", 有乘客在"+ passenger.StartGrid.getid + 
//                " 格子被系统移除,该乘客的状态为"+ passenger.status+ "\n");
    var vehicle = passenger.assignedVehicle
    
//    logger.info("清除该车之后所有的记录")
//    println("清除该车之后所有的记录")
    // 清空 该车的 exec
    for( timekey <- tasks.keys){
       var execList = tasks.get(timekey).get
       for(eachtask <- execList){
          if (eachtask.vehicle == vehicle ){  // 是这个将要
            var index = execList.indexOf(eachtask)
            tasks.get(timekey).get.remove(index)
          }
       }
    }
    
//    println("重新规划路线")
    passenger.assignedVehicle.currentPassengerNum -= 1

    var vehicleSchedule = passenger.assignedVehicle.getSchedule()
//    vehicleSchedule.foreach{x => println(x.getGrid().getid)}
    var currentGrid = passenger.assignedVehicle.currentGrid
//    println(currentGrid.getid)
    var startPoint = passenger.StartPoint
    var endPoint = passenger.EndPoint
    
//    println("--start--" + startPoint.getGrid().getid)
//    println("--end--" + endPoint.getGrid().getid)
    
    try{
      var indexOfStart = vehicleSchedule.indexOf(startPoint)
//      println(indexOfStart)
      vehicleSchedule.remove(indexOfStart)
      var indexOfEnd = vehicleSchedule.indexOf(endPoint)
//      println(indexOfEnd)
      vehicleSchedule.remove(indexOfEnd)
    }catch {
      case t: Throwable => logger.error("Remove里面出错了") // TODO: handle error
      vehicle.updateRoute(null)
      vehicle.updateSchedule(null)
      vehicle.setVehicleStatus(TURN_OFF)
      vehicle.currentPassengerList = null
      vehicle.currentPassengerNum = 0
      return 
    }

      var firstpoint :Grid = null
      // 更新
      vehicle.updateRoute(Config.arrangeRoute(vehicle.currentGrid, vehicle.getSchedule())._1)
//      vehicle.getRoute()
      if(vehicle.getRoute().size == 0 && vehicle.getSchedule().size!=0){ // 得到的path为空，但是schedule不为空，说明还有一个点
        firstpoint = vehicle.getSchedule().head.getGrid()
      }else if(vehicle.getRoute().size != 0){ //得到了path
        firstpoint = vehicle.getRoute().head
      }else if(vehicle.getRoute().size == 0 && vehicle.getSchedule().size == 0){
        vehicle.setVehicleStatus(TURN_OFF)
        return
      }
    
    
    
      // 得到当前位置到第一个 point的路径规划
//      var firstpoint = vehicle.getRoute().head
      var firstPathList = Config.getRouteBetweenTwoPoint(
                new Point(vehicle.currentGrid), 
                new Point(firstpoint))._1
      
//      println("----------ijrejgijevjvihfoiwhoirweweffff----------") 
      if(vehicle.getRoute().size != 0){
         firstPathList.remove(firstPathList.size - 1)
      }

//      firstPathList.foreach{ x => println(x.getid) }
      var tempRoute = vehicle.getRoute()
      tempRoute.insertAll(0, firstPathList)
      vehicle.updateRoute(tempRoute)
//      println("-----------revgrnenevnernvernvnervnevnrenvkenvknevuneuvnrueiv")
//      vehicle.getRoute().foreach { x => println(x.getid) }
      var tempPathlist = vehicle.getRoute()
      var i= 0 
      breakable(
      for(elem <- tempPathlist){
         var index = tempPathlist.indexOf(elem)
//         println("-+-+-+-+-+-+-+-+-+-+-+-+-" + index)
         if(index+1 == tempPathlist.size)
           break
         if(elem.getid == tempPathlist.apply(index+1).getid){
           tempPathlist.remove(index)
         }
      }
      )
      

//      vehicle.getSchedule().foreach { x => print(x.getGrid().getid + "   ") }
//      for( i <- 0 to tempPathlist.size){
//        var elem1 = vehicle.getRoute().apply(i)
//        var elem2 = vehicle.getRoute().apply(i+1)
//        if(elem1.getid == elem2.getid){
//          tempPathlist.remove(i+1)
//        }
//      }
      vehicle.updateRoute(tempPathlist)
//      println("route.......")
//      vehicle.getRoute().foreach { x => println(x.getid) }
//      println("updated route.......")
    
    
    //在全局的乘客列表中移除该用户
    //simulate.getPassengerList().remove(simulate.getPassengerList().indexOf(passenger))
    
      
        if(simulate.tasks.contains(currentTime)){
//         simulate.tasks.get(currentTime).get.append(new MovementTask(vehicle))
           simulate.tasks.get(currentTime).get.append(new MovementTask(vehicle, passenger, 0))
          }else{
           val newListBuffer : ListBuffer[Task] = ListBuffer()
           newListBuffer.+=(new MovementTask(vehicle, passenger, 0)) //新人共享时间0
//         newListBuffer.+=(new MovementTask(vehicle))
           simulate.tasks += (currentTime -> newListBuffer)
          }
           
      
  }
  
}