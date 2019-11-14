package loadData

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.PrintWriter

import traits.Task
import scala.collection.mutable._
import task.AddPassengerTask
import model.Passenger
import loadData.Parser.parserTime
import loadData.Parser.tranTimeToLong
import simulate.simulate


object LoadPassengerData {
  //首先初始化一个SparkSession对象
  val spark = org.apache.spark.sql.SparkSession.builder
                .master("local")
                .appName("Spark CSV Reader")
                .config("spark.sql.warehouse.dir", "file:///")
                .getOrCreate;
      var dict = "./requests/"
//  var dict = "./testRequest/"

  var reflectiongrid : ListBuffer[Int] = new ListBuffer[Int]()

  def getPassengerData(filename : String) ={

    val reflectionpath = "reflection_portugal.csv"
    val reflection = spark.read
      .format("com.databricks.spark.csv")
      .option("header", "true") //reading the headers
      .option("mode", "DROPMALFORMED")
      .load(reflectionpath); //.csv("csv/file/path") //spark 2.0 api

    reflection.foreach(line=>reflectiongrid.append(line.get(0).toString.toInt))

    //然后使用SparkSessions对象加载CSV成为DataFrame
    val df = spark.read
             .format("com.databricks.spark.csv")
             .option("header", "true") //reading the headers
             .option("mode", "DROPMALFORMED")
             .load(dict + filename);

    //得到数据
    val passengerRDD = df.select("userid", "pickupDate", "pickuptime", "startGrid", "endGrid",
                                  "startLon", "startLat").rdd
    
    //对数据进行处理，添加到Tasks列表
//    passengerRDD.foreach { row => InitPassengerMap(row.get(0).toString(),
//                          row.get(1).toString(),row.get(2).toString(),
//                        row.get(3).toString().toInt, row.get(4).toString().toInt,
//                        row.get(6).toString().toDouble, row.get(5).toString().toDouble) }

    for(row <- passengerRDD){
      if ( reflectiongrid.contains(row.get(3).toString.toInt) && reflectiongrid.contains(row.get(4).toString.toInt)){
        InitPassengerMap(row.get(0).toString,
          row.get(1).toString,row.get(2).toString,
          row.get(3).toString.toInt, row.get(4).toString.toInt,
          row.get(6).toString.toDouble, row.get(5).toString.toDouble)
      }
    }

    
  }
  
  
  /**
   * 每次来先判断一下 ，key是否存在，如果存在直接append List ，如果不存在，则new List<Task>
   */
  def InitPassengerMap(ID : String ,StartDate : String, StartTime : String, 
          StartGridID : Int, EndGridID : Int, StartLat : Double, StartLon : Double)={

      val time = StartDate + " " + StartTime
      val TimeStamp = tranTimeToLong(time)
      if(simulate.getTasks().contains(TimeStamp)){ //存在该ID，可以直接添加
        
//        println(TimeStamp)
//        println(parserTime(StartTime))
          val passenger  = new Passenger(ID, time, StartGridID, EndGridID, 
                                          StartLat, StartLon)
          passenger.StartPoint.setPassenger(passenger)
          passenger.EndPoint.setPassenger(passenger)
          simulate.getPassengerList().append(passenger)
          simulate.getTasks().get(TimeStamp).get.+=(new AddPassengerTask(passenger))
          //println(tasks.size)
      }else{  //当前ID尚不存在，需要添加map
          val newListBuffer : ListBuffer[Task] = ListBuffer()
          val passenger  = new Passenger(ID, time, StartGridID, EndGridID,
                                          StartLat, StartLon)
          passenger.StartPoint.setPassenger(passenger)
          passenger.EndPoint.setPassenger(passenger)
          simulate.getPassengerList().append(passenger)
          newListBuffer.+=(new AddPassengerTask(passenger))
          simulate.getTasks() += (TimeStamp -> newListBuffer)
          //println(tasks.size)
      }
  }

}