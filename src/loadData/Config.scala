package loadData

import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.mutable.ListBuffer
import model.Point
import model.Grid
import simulate.simulate.grids
import simulate.simulate.map
import simulate.simulate.road
import simulate.simulate.reflection
import simulate.simulate.decodeReflection
import java.util.List

import gridGenerate.GridGenerate.{MAX_LAT, MAX_LON}
import shortestPath.Dijkstra

import scala.util.control.Breaks._
import simulate.simulate.travelTime
import org.slf4j.LoggerFactory

object Config {
   val WAITING_TIME = 300  //默认等待时间是10分钟
   val Tolerate_TIME = 300 //默认忍耐时间是10分钟
//   val FOUR_HORN: Set[Int] = Set(5655, 5665, 6777, 6787)
   val ANGLE_CHUNK = 0.005  //格子长宽大小


  /***
    * MIN_LAT = 37.05  # 最小的纬度
    * MAX_LAT = 38  # 最大的纬度
    * MIN_LON = -122.8  # 最小的经度
    * MAX_LON = -122  # 最大的经度
    */
  val MIN_LAT = 31.99     //最小的纬度
  val MAX_LAT = 51.04     //最大的纬度
  val MIN_LON = -9.78     //最小的经度
  val MAX_LON = -0.2     //最大的经度


   val logger = LoggerFactory.getLogger(this.getClass());
//   val NUM_OF_LAT_BINS = math.ceil((MAX_LAT - MIN_LAT) * 1.0 / ANGLE_CHUNK).toInt ;  //纬度方向的格子个数 280
//   val NUM_OF_LON_BINS = ((MAX_LON - MIN_LON) / ANGLE_CHUNK).toInt ;  //经度方向的格子个数 278

  val NUM_OF_LAT_BINS = math.ceil((MAX_LAT - MIN_LAT) * 1.0 / ANGLE_CHUNK).toInt ;  //纬度方向的格子个数
  val NUM_OF_LON_BINS = ((MAX_LON - MIN_LON) / ANGLE_CHUNK).toInt ;  //经度方向的格子个数
  
   val  MAX_TIME_INTERVAL:Long = 420  //时间间隔，用于异常点的检测
  
   val PROBABILITY_THRESHOLD:Double = 0.2;
   
  object status extends Enumeration{
    type PassengerStatus = Value//这里仅仅是为了将Enumration.Value的类型暴露出来给外界使用而已
    val UNKNOWN, WAITING, PICKUP, MISSING, FINISH = Value//在这里定义具体的枚举实例
  }
  
  object PointTypes extends Enumeration{
    type PointType = Value
    val PICKUP_POINT, PICKOFF_POINT = Value
  }
  
  object VehicleTypes extends Enumeration{
    type VehicleType = Value
    val TURN_ON , TURN_OFF = Value
  }
  
  val MAX_PASSENGER_NUM = 3
  
  val MAX_CLUSTER=3 //最大可以接的乘客数
   //传入时间戳返回日期字符串
  def getDateTimeString(time:Long):String={
	  val datee=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")  		 
      val datetime=datee.format(new Date(time*1000))    //年月日时分秒
      datetime.toString()
  }
  
  //通过坐标转换计算欧式距离
    def getDistance( start:(Double, Double), end:(Double, Double)):Double= {
        val conv:Double = Math.PI / 180;
        val phiS:Double = end._1 * conv 
        val lambdaS:Double = end._2 * conv
        val phiF:Double = start._1 * conv
        val lambdaF:Double = start._2 * conv
        val t1:Double = Math.pow(Math.sin((phiF - phiS) / 2), 2);
        val t2:Double = Math.pow(Math.sin((lambdaF - lambdaS) / 2), 2);
        var d:Double = t1 + Math.cos(phiS) * Math.cos(phiF) * t2;
        d = Math.sqrt(d);
        d = 2 * Math.asin(d);
        return 6372800 * d    
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
           return (math.ceil((RealLat - MIN_LAT) / ANGLE_CHUNK).toInt ,
           (((RealLon - MIN_LON) / ANGLE_CHUNK) + 1).toInt)
        }
    
        /**************修改******************/
        //若传入格子的编号，以元组的形式返回某一编号格子的行列值
        def decodeLat_LonBin(id : Int) : (Int, Int) = {
            val LonID = if(id % NUM_OF_LON_BINS == 0) NUM_OF_LON_BINS
                        else id % NUM_OF_LON_BINS
            return ((math.ceil(id* 1.0 / NUM_OF_LON_BINS)).toInt,
                  LonID.toInt)
        }

       //传入格子的行列数可以得到该格子的编号
        def getGridID(latBin : Int, lonBin : Int ):Int = {
            return ((latBin-1) * NUM_OF_LON_BINS + lonBin).toInt;
        }
        
        def getCentralCoordByGridID(id:Int) : (Double, Double)={
          return simulate.simulate.originalGridsWithCentralCoord.get(id).get
        }
        
        //未完成
//        def getBestSchedule(currentGrid : Grid, src:ListBuffer[Point], 
//                  singlePickoffPoints:ListBuffer[Point]):ListBuffer[Point]={
//          /**
//           * 排序操作
//           */
//          //先模拟一下
//          var schedule : ListBuffer[Point] = ListBuffer()
//          //车子到所有的顺序，包括singlePickoffPoints 和 其他点，保存在schedule中，然后返回
//          val tempList = src.toList
//          for (elem <- tempList){
//            schedule.append(elem)
//            schedule.append(elem.getTheOtherPoint())
//          }
//          schedule
//        }
        

        def arrangeRoute(currentGrid : Grid, schedule : ListBuffer[Point]):(ListBuffer[Grid], Double)={
          
          var route:ListBuffer[Grid] = ListBuffer()

          var distance :Double = 0
          for(i <- 0 until schedule.size-1){  //7个元素只需要循环六次，又因为下标是从0开始的
//            println("进入循环。。。。。。。。。")
             var result = getRouteBetweenTwoPoint(schedule(i), schedule(i+1))

            if(result._1.isEmpty) { // 有问题
              return (new ListBuffer[Grid](), Double.MaxValue)
            }

//             routeSeqBetweenTwoPoint.remove(0)
             var routeSeqBetweenTwoPoint = result._1
             route.appendAll(routeSeqBetweenTwoPoint)
             distance = distance + result._2
          }
          
          //如何加上车的位置到第一个接客位置的
          
//          var from = new Point(currentGrid)
//          var to = new Point(schedule(0).getGrid())
//          
//          var routeListFromVehicle = getRouteBetweenTwoPoint(from, to)
//          
//          //可能存在问题的地方
//          routeListFromVehicle.remove(routeListFromVehicle.size - 1)
//          
//          route.insertAll(0, routeListFromVehicle)
          
          (route, distance)
        }
        
        
        def getRouteBetweenTwoPoint(from : Point, to : Point) : (ListBuffer[Grid], Double)={      
          // 起止点的格子编号，
          var fromGrid = from.getGrid().getid
          var toGrid = to.getGrid().getid
          //这块需要重新写
//          var routeList = processor(map, new Node(fromGrid.getLatBin - 1, fromGrid.getLonBin - 1), new Node(toGrid.getLatBin - 1, toGrid.getLonBin - 1))
//          println("起始的Grids编号:  "+fromGrid, toGrid)
//          println(fromGrid)
//          println(toGrid)
          var reflectStartID = reflection.get(fromGrid+"").get
          var reflectEndID = reflection.get(toGrid+"").get
//          println("映射之后的Grids编号:  "+reflectStartID, reflectEndID)
//          println(fromGrid, reflectStartID)
//          println(toGrid, reflectEndID)

          var routeList = processor(reflectStartID, reflectEndID)

          if(routeList._2.isInfinity){
            return (new ListBuffer[Grid](), Double.MaxValue)
          }


//          routeList.remove(routeList.size - 1)
          return routeList
        }
        
       // 这块也是processor需要重写
       def processor(fromID : String , toID : String) :(ListBuffer[Grid], Double) ={
          var fromGridIndex = fromID.toInt
          var toGridIndex = toID.toInt
          
//          var routeList = road.dijkstraTravasal(fromGridIndex, toGridIndex)
          
//          var startTime = System.currentTimeMillis();
//          logger.info("最短路径的开始时间"+startTime)
          var result = Dijkstra.findshortestPath(road, fromGridIndex, toGridIndex)
//          var endTime = System.currentTimeMillis();
//          logger.info("最短路径的结束时间"+endTime)
          var minDistance = result.keySet().toString().stripPrefix("[").stripSuffix("]").toDouble

//          println("minDistance" + minDistance)
          var routeList = result.get(minDistance)
//          var routeList = road.dijkstraTravasal(0, 7);
          if(routeList != null){
//              for(i <- routeList.toArray()){
//            	  System.out.println(i);
//              }
              //转化为格子
              var routeListInGrid : ListBuffer[Grid] = ListBuffer()
              // 需要改变，将映射之后的数据转化到真实路网上
              for(i <- 0 until routeList.size()){
                var curGrid = grids(decodeReflection.get(routeList.get(i).getName).get.toInt)
                routeListInGrid.append(curGrid)
//                if(!routeListInGrid.contains(curGrid)){ //如果没有的话，加进去
//
//                }
              }
              // 这块返回的Grid列表是映射之后的列表，需要转化到真实路网上
              return (routeListInGrid, minDistance)
          }else{
        	  System.out.println("没有得到规划的路线");
        	  return null
          }
         
        }
        
        
//        def processor(maps : Array[Array[Int]], fromNode : Node , toNode : Node):ListBuffer[Grid]={
//          var info : MapInfo = new MapInfo(maps,maps(0).length, maps.length, fromNode, toNode) 
//          var newAStar = new AStar()
//          newAStar.start(info)
//    
//          //println("轨迹：")
//          var routeList = newAStar.getRouteList
//          //println(routeList.size())
//          var routeListInGrid : ListBuffer[Grid] = ListBuffer()
//          for(i <- 0 until routeList.size()){
//            routeListInGrid.append(grids(Config.getGridID(routeList.get(i).coord.x + 1,
//            routeList.get(i).coord.y + 1)))
//          }
//    
////        routeListInGrid.foreach { x => println(x.getid) }
//    
//        return routeListInGrid
//        }
        
        //  两个点之间如何走？？？
//        def getRouteBetweenTwoPoint(from : Point, to : Point) : ListBuffer[Grid]={
//          var routeList : ListBuffer[Grid] = ListBuffer()
//          var fromLatLon = Config.decodeLat_LonBin(from.getGrid().getid)
//          var toLatLon = Config.decodeLat_LonBin(to.getGrid().getid)
//          var fromLat = fromLatLon._1
//          var fromLon = fromLatLon._2
//          var toLat = toLatLon._1
//          var toLon = toLatLon._2   
//          
//          
//          
//          var maxLat = math.max(fromLat,toLat)
//          var minLat = math.min(fromLat,toLat)
//          var maxLon = math.max(fromLon,toLon)
//          var minLon = math.min(fromLon,toLon)
//          
//          var latRange = Range(minLat, maxLat+1).to[ListBuffer]
//          var lonRange = Range(minLon, maxLon+1).to[ListBuffer]
//          
//          if(fromLat > toLat) {
//            latRange = latRange.reverse
//          }
//            
//          if(fromLon > toLon){
//            lonRange = lonRange.reverse
//          }  
//          
//
//          lonRange.remove(lonRange.size-1)
//          
////          println(latRange)
////          println(lonRange)
//          
//          //沿经度走
//          for(i <- lonRange){
//            //println((fromLat-1) * NUM_OF_LON_BINS + i)
//            routeList.append( grids((fromLat-1) * NUM_OF_LON_BINS + i) ) 
//          }
//            
//          for(i <- latRange){
//            //println((i-1) * NUM_OF_LON_BINS + toLon)
//            routeList.append( grids((i-1) * NUM_OF_LON_BINS + toLon) )
//          }
//          
//          //println(routeList.size)
//         
//          routeList
//        }
  
        
       
        def getExpectedTime(from : Point, to : Point):Double={
          var expectedTime:Double = 0
          
          var routeList = getRouteBetweenTwoPoint(from, to)._1
          
//          //从当前节点到终点需要的与其时间
//          for(i <- 0 until routeList.size-1){  //7个元素只需要循环六次，又因为下标是从0开始的
//              expectedTime = expectedTime + 
//                             (routeList(i).getTime.get(routeList(i+1))).toString().toDouble  //从i到(i+1)的时间
//          }
          

          
          
          breakable(
            for(elem <- routeList){
               var index = routeList.indexOf(elem)
               if(index+1 == routeList.size)
                 break
               else{
                 var fromIndex = elem.getid.toString()
                 var toIndex = routeList.apply(index+1).getid.toString()
                 var fromToTime = 0
                 if(fromIndex != toIndex) //如果不相等，则需要。
                      fromToTime = travelTime.get(fromIndex).get.get(toIndex).get
                 expectedTime += fromToTime
               }
            }
      )
          
          return expectedTime
        }
  
}