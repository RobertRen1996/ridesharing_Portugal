package gridGenerate

import scala.collection.mutable.ListBuffer;
import java.io.File
import java.io.PrintWriter

object GridGenerate {
  
    val ANGLE_CHUNK = 0.005  //格子长宽大小
//    val MIN_LAT = 30.29      //最小的纬度
//    val MAX_LAT = 30.79      //最大的纬度
//    val MIN_LON = 114.03     //最小的经度
//    val MAX_LON = 114.54     //最大的经度
//
  val MIN_LAT = 37.05      //最小的纬度
  val MAX_LAT = 38      //最大的纬度
  val MIN_LON = -122.8     //最小的经度
  val MAX_LON = -122     //最大的经度

    val NUM_OF_LAT_BINS = math.floor((MAX_LAT - MIN_LAT) * 1.0 / ANGLE_CHUNK).toInt ;  //纬度方向的格子个数
    val NUM_OF_LON_BINS = math.floor((MAX_LON - MIN_LON)*1.0 / ANGLE_CHUNK).toInt+1 ;  //经度方向的格子个数
    
    val filePathString : String =  "C:\\Users\\15877\\Desktop\\Grid.txt"
    var out = new java.io.FileWriter(filePathString, false)

  def main(args: Array[String]): Unit = {
    println(generateGridID(37.79826,-122.26613))
    println(getLat_LonBin(37.79826,-122.26613))
//    generateGridDataSet()
  }
  
  //生成各自数据集，并写入文件
  def generateGridDataSet() = {
    out.append("GridID,Latitude,Longitude\n")
    var LATData = MIN_LAT
    for (LAT_ID <- 1 to NUM_OF_LAT_BINS){
        var LONData = MIN_LON
        for (LON_ID <- 1 to NUM_OF_LON_BINS){
            var currentGridID = getGridID(LAT_ID, LON_ID)      
            var centralData = getCentralLat_LonData(LATData, LONData)
            //println(currentGridID, centralData)  
            val line = currentGridID +" "+ centralData._1 +" " +centralData._2+"\n"
            out.append(line)
            LONData += ANGLE_CHUNK
        }
        LATData += ANGLE_CHUNK
     }
    out.close()
  }

  //返回当前格子的中心坐标
  def getCentralLat_LonData(LAT : Double, LON : Double) : (Double,Double) = {
    return (LAT + ANGLE_CHUNK/2 , LON + ANGLE_CHUNK/2)
  }

  //若传入当前的经纬度，返回当前经纬度所在格子编号
  def generateGridID(RealLat:Double , RealLon:Double):Int ={
    val Lat_LonBin = getLat_LonBin(RealLat, RealLon)
    return getGridID(Lat_LonBin._1, Lat_LonBin._2)
  }

  //若传入当前的经纬度，以元组的形式返回当前点所处的格子的行列值
    def getLat_LonBin(RealLat:Double , RealLon:Double) : (Int,Int)={
      return (math.floor((RealLat - MIN_LAT) / ANGLE_CHUNK ).toInt + 1 ,
          (((RealLon - MIN_LON) / ANGLE_CHUNK) + 1).toInt)
    }
    
  /**************修改******************/
  //若传入格子的编号，以元组的形式返回某一编号格子的行列值
    def decodeLat_LonBin(id : Int) : (Int, Int) = {
      val LonID = if(id % NUM_OF_LON_BINS == 0) 278 else id % NUM_OF_LON_BINS
        return ((math.ceil(id* 1.0 / NUM_OF_LON_BINS)).toInt,
              LonID.toInt)
    }

 //传入格子的行列数可以得到该格子的编号
    def getGridID(latBin : Int, lonBin : Int ):Int = {
        return ((latBin-1) * NUM_OF_LON_BINS + lonBin).toInt;
    }
    
}