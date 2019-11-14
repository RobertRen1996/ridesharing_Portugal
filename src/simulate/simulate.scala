package simulate

import scala.collection.mutable.ListBuffer
import scala.collection.mutable._
import org.slf4j.LoggerFactory
import traits.Task
import loadData.Parser.parserTime
import loadData.LoadPassengerData
import loadData.Parser.encodeTime
import loadData.Config.status.UNKNOWN
import loadData.Config.status.WAITING
import loadData.Config.status.PICKUP
import loadData.Config.status.MISSING
import loadData.Config.status.FINISH
import model.{Grid, Passenger, Point, Vehicle}
import loadData.LoadVehicleData
import loadData.LoadOriginalGrids
import loadData.Config
import org.apache.spark.sql.Row
import processor.loadRouteData
import loadData.Parser.tranTimeToLong

import Array._
import java.io.FileWriter

import shortestPath.Graph
import shortestPath.Dijkstra
import loadData.LoadReflectionwithScala
import loadData.LoadtravelTime
import loadData.Parser.timeStamp2Date
import java.text.SimpleDateFormat
import java.util.Date

import org.apache.log4j.{Level, Logger}

import scala.collection.mutable


object simulate {

  Logger.getLogger("org").setLevel(Level.ERROR)

  val vehicleFilePath = "portugal_taxi.csv" //出租车数据的路径,第一个区域 200个格子
//  val Area = "First"
  val originalGridsPath = "portugal_grid.csv" //格子数据的路径
  val reflectionPath = "reflection_portugal.csv" //映射
  var tasks: Map[Long, ListBuffer[Task]] = Map() //记录了所有的task任务
  var passengerList: ListBuffer[Passenger] = ListBuffer() //记录所有的乘客信息包括状态
  var graph: Map[Int, Grid] = Map()
  var map = ofDim[Int](Config.NUM_OF_LAT_BINS, Config.NUM_OF_LON_BINS) //地图矩阵
  var vehicles: Map[Int, Vehicle] = Map() //记录所有的汽车的信息
  var grids: Map[Int, Grid] = Map() //初始化所有的格子  10200个格子
  var ObstacleGridList: ListBuffer[Int] = ListBuffer() //记录为障碍物的格子的ID
  var originalGridsWithCentralCoord: Map[Int, (Double, Double)] = Map()
  val logger = LoggerFactory.getLogger(this.getClass());
  var road: Graph = null;
  var reflection: Map[String, String] = Map()
  var decodeReflection: Map[String, String] = Map()
  var travelTime: HashMap[String, HashMap[String, Int]] = HashMap()
  var fromGrid: ListBuffer[Int] = ListBuffer()
  var runtimeCount: Map[String, Long] = Map()

  def main(args: Array[String]) {
    //  更换LoadPassengerData里面的文件夹目录
    var filesArray = Array(
      "2013-07-01_requestsInSelectedGrid.csv",
      "2013-07-02_requestsInSelectedGrid.csv",
      "2013-07-03_requestsInSelectedGrid.csv",
      "2013-07-04_requestsInSelectedGrid.csv",
      "2013-07-05_requestsInSelectedGrid.csv",
      "2013-07-06_requestsInSelectedGrid.csv",
      "2013-07-07_requestsInSelectedGrid.csv",
      "2013-07-08_requestsInSelectedGrid.csv",
      "2013-07-09_requestsInSelectedGrid.csv",
      "2013-07-10_requestsInSelectedGrid.csv",
      "2013-07-11_requestsInSelectedGrid.csv",
      "2013-07-12_requestsInSelectedGrid.csv",
      "2013-07-13_requestsInSelectedGrid.csv",
      "2013-07-14_requestsInSelectedGrid.csv",
      "2013-07-15_requestsInSelectedGrid.csv",
      "2013-07-16_requestsInSelectedGrid.csv",
      "2013-07-17_requestsInSelectedGrid.csv",
      "2013-07-18_requestsInSelectedGrid.csv",
      "2013-07-19_requestsInSelectedGrid.csv",
      "2013-07-20_requestsInSelectedGrid.csv",
      "2013-07-21_requestsInSelectedGrid.csv",
      "2013-07-22_requestsInSelectedGrid.csv",
      "2013-07-23_requestsInSelectedGrid.csv",
      "2013-07-24_requestsInSelectedGrid.csv",
      "2013-07-25_requestsInSelectedGrid.csv",
      "2013-07-26_requestsInSelectedGrid.csv",
      "2013-07-27_requestsInSelectedGrid.csv",
      "2013-07-28_requestsInSelectedGrid.csv",
      "2013-07-29_requestsInSelectedGrid.csv",
      "2013-07-30_requestsInSelectedGrid.csv",
      "2013-07-31_requestsInSelectedGrid.csv",
      "2013-08-01_requestsInSelectedGrid.csv",
      "2013-08-02_requestsInSelectedGrid.csv",
      "2013-08-03_requestsInSelectedGrid.csv",
      "2013-08-04_requestsInSelectedGrid.csv",
      "2013-08-05_requestsInSelectedGrid.csv",
      "2013-08-06_requestsInSelectedGrid.csv",
      "2013-08-07_requestsInSelectedGrid.csv",
      "2013-08-08_requestsInSelectedGrid.csv",
      "2013-08-09_requestsInSelectedGrid.csv",
      "2013-08-10_requestsInSelectedGrid.csv",
      "2013-08-11_requestsInSelectedGrid.csv",
      "2013-08-12_requestsInSelectedGrid.csv",
      "2013-08-13_requestsInSelectedGrid.csv",
      "2013-08-14_requestsInSelectedGrid.csv",
      "2013-08-15_requestsInSelectedGrid.csv",
      "2013-08-16_requestsInSelectedGrid.csv",
      "2013-08-17_requestsInSelectedGrid.csv",
      "2013-08-18_requestsInSelectedGrid.csv",
      "2013-08-19_requestsInSelectedGrid.csv",
      "2013-08-20_requestsInSelectedGrid.csv",
      "2013-08-21_requestsInSelectedGrid.csv",
      "2013-08-22_requestsInSelectedGrid.csv",
      "2013-08-23_requestsInSelectedGrid.csv",
      "2013-08-24_requestsInSelectedGrid.csv",
      "2013-08-25_requestsInSelectedGrid.csv",
      "2013-08-26_requestsInSelectedGrid.csv",
      "2013-08-27_requestsInSelectedGrid.csv",
      "2013-08-28_requestsInSelectedGrid.csv",
      "2013-08-29_requestsInSelectedGrid.csv",
      "2013-08-30_requestsInSelectedGrid.csv",
      "2013-08-31_requestsInSelectedGrid.csv"
    )
//    var testFile = Array("2013-09-01_requestsInSelectedGrid_3000.csv",
//      "2013-09-19_requestsInSelectedGrid_3000.csv")
//        filesArray.filter(line => filesArray.indexOf(line) % 6 == 0 ).foreach(println)

//        filesArray.filter(line => filesArray.indexOf(line) % 6 == 0 ).foreach(line => mainlogical(line))

    filesArray.take(1).foreach(line => mainlogical(line))


//    testFile.foreach(line=>mainlogical(line))


//    mainlogical("2008-05-18_test.csv")

      outputRunTime()


  }

  def mainlogical(PassengerFilename: String) {

    println(PassengerFilename)

    var startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
    println(startTime)
    loadData(PassengerFilename)

    //    println("passenger长度"+passengerList.size)
    println("----------" + PassengerFilename + "---------------" + "新一天")
    StartSimulate(PassengerFilename)
    //

    outputResult(PassengerFilename)


    var endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
    println(endTime)
    var RUNTIME = tranTimeToLong(endTime) - tranTimeToLong(startTime)
    runtimeCount += (PassengerFilename.substring(0, 10) -> RUNTIME)

    tasks.clear()
    passengerList.clear()
    graph.clear()
    vehicles.clear()
    grids.clear()
    originalGridsWithCentralCoord.clear()
    reflection.clear()
    decodeReflection.clear()
    travelTime.clear()
    fromGrid.clear()
  }


  def outputRunTime() = {
    var current = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date).replace(":", "_").replace(" ", "_")
    val outputfilepath = "./RunTimeCount/" + current + "RuntimeResult.csv"
    val writer = new FileWriter(outputfilepath, false)
    writer.append("Date,RunTime\n")
    runtimeCount.foreach { line => writer.append(line._1 + "," + line._2 + "\n") }

    writer.close()
  }

  def outputResult(passengerFilePath: String) = {
    //    val outputfilepath = "./resultInSecondArea/" + passengerFilePath.substring(0, 11) + "result.csv"
    var current = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date).replace(":", "_").replace(" ", "_")
    val outputfilepath = "./ResultInPortugal_100v_300s_3person/" + passengerFilePath.substring(0, 11) + "result.csv"
    val writer = new FileWriter(outputfilepath, false)
    writer.append("ID,requestTime,pickupTime,pickoffTime,expectedTime,movementTime,sharingTime,vehicle\n")
    println("passenger长度" + passengerList.size)
    passengerList.foreach { p =>
      var vehicleName: Int = -1
      if (p.assignedVehicle != null) {
        vehicleName = p.assignedVehicle.vehicleName
      }
      writer.append(p.userId + "," + p.StartTime + "," + timeStamp2Date(p.pickUpTime + "") + "," +
        timeStamp2Date(p.pickOffTime + "") + "," + p.expectedTime + "," + p.duration + "," +
        p.sharingTime + "," + vehicleName + "\n")
    }

    writer.close()
  }


  def outputVehiclePostion(passengerFilePath: String, timeslot: Int): Unit = {
    val outputfilepath = "./vehiclePosition/" + passengerFilePath.substring(0, 11)  + "result"+ timeslot +".csv"
    val writer = new FileWriter(outputfilepath, false)
    writer.append("VehicleID,Latitude,Longitude\n")

    vehicles.foreach(line=>{
      var v = line._2
      writer.append(v.vehicleName+","+v.currentGrid.getCenterPoint()._1+","+v.currentGrid.getCenterPoint()._2+"\n")
    })
    writer.close()
  }

  //程序的模拟过程
  def StartSimulate(passengerFilePath: String) = {
    val date = passengerFilePath.substring(0, 10)
    println(date)
    //用时间模拟还是用个数
    val StartTime = tranTimeToLong(date + "  00:00:00") // 一天的开始时间
    val EndTime = tranTimeToLong(date + "  23:59:59") //一天的结束时间  ----需要修改

    var baseTime = StartTime
    var timeslot = 0
    var TimeListInMap = tasks.keys.toList //获取所有的时间阶段,没有更新
    //println(TimeListInMap)
    //很重要，，List  to  BufferList
    var TimeListOrderByKey = TimeListInMap.sorted.to[ListBuffer] //按照时间戳去排序
    //println(TimeListOrderByKey)   

    var currentTime = TimeListOrderByKey.head
    //    println("*************"+currentTime)
    while (currentTime <= EndTime + 1 && !TimeListOrderByKey.isEmpty) { //当前的时间戳下的所有任务
      //println(currentTime)
      var currentTasks = tasks.get(currentTime).get
      //遍历当前时间戳的所有的task

//      logger.error("当前时间：" + timeStamp2Date(currentTime + ""))

        /**
          * 输出当前车子的位置
          */



      for (task <- currentTasks) {
        //           println(currentTime)
        task.exec(currentTime)
        //currentTasks.foreach(println)
      }


      try {


      } catch {
        case e: Exception => {
          println("-----")
          logger.error("当前时间：" + timeStamp2Date(currentTime + ""))
          //           tasks(currentTime).foreach(println)
          //           println(currentTime)
          println(e + "主逻辑中 出错了")
        }
      } finally {
        tasks.remove(currentTime) //移除已经经过的时间节点，应该会有提升

        TimeListInMap = tasks.keys.toList
        TimeListOrderByKey = TimeListInMap.sorted.to[ListBuffer]
        if (TimeListOrderByKey.size != 0) {
          currentTime = TimeListOrderByKey.head
        }
      }

    } //end-while
  } //end-simulate

  //获取当前的tasks
  def getTasks(): Map[Long, ListBuffer[Task]] = {
    return tasks
  }

  //获取全局的乘客列表
  def getPassengerList(): ListBuffer[Passenger] = {
    return passengerList
  }

  //获取全局的汽车列表
  def getVehicles(): Map[Int, Vehicle] = {
    return vehicles
  }


  //导入路网
  def LoadRoad() = {
    LoadReflectionwithScala.getReflectionData(reflectionPath)
    road = Dijkstra.createGraph();
    LoadtravelTime.getTravelTime()

  }

  //加载各种数据
  def loadData(passengerFilePath: String) = {

    //初始化格子，这样以后就不用不断的new，只是每次根据格子ID来这里找就OK
    InitGrids()
    LoadRoad()


    loadPassenger(passengerFilePath)
    loadVehicle(vehicleFilePath)


    /*
    14667,13726
     */
//    var firstPathList = Config.getRouteBetweenTwoPoint(
//      new Point(grids.get(14667).get),
//      new Point(grids.get(13726).get))._1

    println()
  }

  //仅用于可视化阶段，最后可能不要
  def InitOriginalGrids() = {
    LoadOriginalGrids.getOriginalGridsData(originalGridsPath)
  }

  //设置障碍格子
  def InitMaps() = {
    ObstacleGridList.map { x => setObstacleInMap(x) }
  }

  //具体操作
  def setObstacleInMap(gridID: Int) = {
    var gridLatLon = Config.decodeLat_LonBin(gridID)
    var lat = gridLatLon._1
    var lon = gridLatLon._2
    //格子的行列号与地图的i，j之间存在差值1
    map(lon - 1)(lat - 1) = 1 //
  }

  //初始化格子对象
  def InitGrids() = {
//    for (i <- 1 to Config.NUM_OF_LON_BINS)
//      for (j <- 1 to Config.NUM_OF_LAT_BINS) {
//        var GridId = Config.getGridID(j, i)
//        grids += (GridId -> new Grid(GridId))
//      }

    LoadOriginalGrids.getOriginalGridsData(originalGridsPath)

    originalGridsWithCentralCoord.map(line=>line._1).foreach(line=>{
      grids.put(line, new Grid(line))
    })

  }

  //加载乘客数据
  def loadPassenger(passengerFilePath: String) = {
    LoadPassengerData.getPassengerData(passengerFilePath)
    // println(tasks.size)
    // println(passengerList.size)
  }

  def loadVehicle(vehicleFilePath: String) = {
    LoadVehicleData.getVehicleData(vehicleFilePath)
    // println(vehicles.size)
  }


}