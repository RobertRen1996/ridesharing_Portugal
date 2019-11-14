package loadData

import loadData.LoadPassengerData.spark //导入之前定义的SparkSession
import model.Vehicle
import simulate.simulate
import loadData.Config.VehicleTypes.{TURN_OFF, TURN_ON}
import simulate.fromGrid

object LoadVehicleData {

  def getVehicleData(filename: String) = {
    //然后使用SparkSessions对象加载CSV成为DataFrame
    val df = spark.read
      .format("com.databricks.spark.csv")
      .option("header", "true") //reading the headers
      .option("mode", "DROPMALFORMED")
      .load(filename);
    //得到数据
    val vehicleRDD = df.select("Index", "Latitude", "Longitude").rdd

    //对数据进行处理，添加到Tasks列表
    vehicleRDD.foreach { row =>
      InitVehicleMap(row.get(0).toString().toInt,
        row.get(1).toString().toDouble, row.get(2).toString().toDouble)
    }

  }

  //初始化汽车信息
  def InitVehicleMap(index: Int, latitude: Double, longitude: Double) = {
    var vehicleID = Config.generateGridID(latitude, longitude)
    if (fromGrid.contains(vehicleID)) {
      val newVehicle = new Vehicle(index, latitude, longitude)
      newVehicle.setVehicleStatus(TURN_OFF)
      simulate.getVehicles() += (index -> newVehicle)
    }

  }

}