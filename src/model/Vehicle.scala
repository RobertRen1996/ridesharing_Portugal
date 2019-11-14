package model

import scala.collection.mutable.ListBuffer
import loadData.Config.generateGridID
import loadData.Config
import simulate.simulate.grids
import loadData.Config.VehicleTypes.{TURN_OFF, TURN_ON}
import loadData.Config.VehicleTypes
import loadData.Config.VehicleTypes.VehicleType


class Vehicle(Index : Int, Latitude_i : Double, Longitude_i : Double) {  //根据经纬度信息去创建一个汽车
  val vehicleName = Index  //用index唯一标识车辆
  var currentLatitude = Latitude_i
  var currentLongitude = Longitude_i
  var currentLat_Lon = (currentLatitude, currentLongitude)
  var currentGrid = grids(generateGridID(currentLatitude, currentLongitude))  //得到车辆当前所在的格子对象

  /**
    * 是否空闲的标志位, 默认是空闲的 ，
    *
    * 在每一个车子放下一个乘客之后，需要判断放下乘客之后schedule/routeList是否为空，如果不为空就让他去继续跑，
    * 如果为空，说明现在车子时空闲状态，isCruising设置为true, 标志当前的状态。
    * 这个时候需要给它随机分配一个格子，更新该车的schedule和routeList,让他按照这个routeList去走。
    *
    *
    * 需要在分完车之后 根据该变量的true/false，修改schedule和routeList
    *
    *
    */
  var isCruising:Boolean = true

//  var pickupList : ListBuffer[Grid] = ListBuffer()  //当前车辆要接客的List
//  var pickoffList : ListBuffer[Grid] = ListBuffer()  //当前车上乘客要去的目的地格子的List
  
  var schedule : ListBuffer[Point] = ListBuffer() //以格子对象代替之前的格子编号 ,存储要前往(有乘客)的几个格子的信息
  var route : ListBuffer[Grid] = ListBuffer() //顺序存储将要一一前往的格子对象,movement
  //var src:ListBuffer[Point] = ListBuffer() // 仅仅存储所有的起始point
//  var waitingPassengerList : ListBuffer[Passenger] = ListBuffer()  //存储将要pickup的乘客
  var currentPassengerList : ListBuffer[Passenger] = ListBuffer()  //存储已经上车的乘客信息
  
  
  var allPickedPassenger : ListBuffer[Passenger] = ListBuffer()  //记录该车今天拉过的所有乘客
  var status : VehicleType= null  //分为未启动和已经启动两个层面
  
  var tracedRoutes : ListBuffer[Grid] = ListBuffer()
  
  val maxPassengerNum = loadData.Config.MAX_PASSENGER_NUM  //最大的乘客数量
  var currentPassengerNum = 0   //当前的乘客数量
  
  var travelDistance : Double = 0  //运行距离
  var liveDistance : Double = 0 //载客距离
  var travelTime : Double = 0 //运行时间
  var liveTime : Double = 0 //载客时间
  
  var numOfHunts = 0  //完成的单数
  
  def setCurrentGrid(currentGrid : Grid)={
    this.currentGrid = currentGrid
  }
  
  def getCurrentGrid():Grid={
    currentGrid
  }
  

  def updateSchedule(schedule:ListBuffer[Point])={
    this.schedule = schedule
  }

  def getSchedule():ListBuffer[Point] = {
    schedule
  }
  
  /*def updateSrc(src:ListBuffer[Point])={
    this.src = src
  }

  def getSrc():ListBuffer[Point] = {
    src
  }*/
  
  
  def updateRoute(route:ListBuffer[Grid])={
    this.route = route
  }

  def getRoute():ListBuffer[Grid] = {
    route
  }
  
  def setVehicleStatus(status:VehicleType)={
    this.status = status
  }
  
  
    def cleanPerformance() {

        this.liveDistance = 0;

        this.liveTime = 0;

        this.travelDistance = 0;

        this.travelTime = 0;

        this.numOfHunts = 0;

    }
  
  
  
}