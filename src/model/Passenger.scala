package model

import loadData.Config.status.UNKNOWN
import loadData.Config.PointTypes.{PICKUP_POINT, PICKOFF_POINT}
import simulate.simulate.grids
import scala.collection.mutable.ListBuffer

class Passenger (ID : String, StartTime_p : String, StartGridID_p : Int, EndGridID_p : Int,
                  StartLat_p : Double, StartLon_p : Double) {
  val userId = ID
  val StartTime = StartTime_p  //发起请求的时间
  val StartGrid : Grid = grids(StartGridID_p) //有之前的格子ID变成了格子对象
  val StartPoint : Point = new Point(PICKUP_POINT, StartGrid)
  val EndGrid : Grid = grids(EndGridID_p) 
  val EndPoint : Point = new Point(PICKOFF_POINT, EndGrid)
  StartPoint.setTheOtherPoint(EndPoint)
  EndPoint.setTheOtherPoint(StartPoint)
  
  var currentGrid : Grid = StartGrid //初始化
  
  var assignedVehicle:Vehicle = null  //记录给他分派的车辆对象
  
  val StartLat = StartLat_p
  val StartLon = StartLon_p

  
  var pickUpTime:Long = 0  //上车时间
  var pickOffTime:Long = 0
  
  var duration : Double = (pickOffTime - pickUpTime)
  
  var tracedGrids:ListBuffer[Grid] = ListBuffer()  //记录该用户从上车至到达目的地所经过的格子
  
  //以一种什么样的形式去组织状态呢？，枚举还是五位二进制数
  var status = UNKNOWN
  val waitingTime = loadData.Config.WAITING_TIME  //10min
  val tolerateTime = loadData.Config.Tolerate_TIME  //10min
  
  var sharingTime : Double = 0 //记录一下共享的时间，每到一个格子就需要更新
  var expectedTime : Double = 0 //如果不拼车从当前格子到endGrid所需要的时间
  
  def updateDuration(duration : Double) = {
    this.duration = duration
  }
  
  //更新共享的时间
  def updateSharingTime(time : Double){
    this.sharingTime = (this.sharingTime + time)
  }
  
  
  
  
}