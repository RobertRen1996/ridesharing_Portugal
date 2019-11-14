package task

import traits.Task
import model.Passenger
import org.slf4j.LoggerFactory
import loadData.Parser.encodeTime
import simulate.simulate
import scala.collection.mutable.{ListBuffer, Set}
import loadData.Config.status.WAITING
import loadData.LoadVehicleData
import model.Vehicle
import scala.collection.mutable.ListMap
import model.Vehicle
import loadData.Config
import loadData.Config.PointTypes.{PICKUP_POINT, PICKOFF_POINT}
import loadData.Config.VehicleTypes.{TURN_OFF, TURN_ON}
import model.Point
import model.Grid
import recommender.NN_recommender
import loadData.Parser.timeStamp2Date
import scala.util.control.Breaks._
import simulate.tasks
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable.Map

class AddPassengerTask extends Task {
  var passenger: Passenger = null
  var vehicle: Vehicle = null

  def this(passenger: Passenger) {
    this()
    this.passenger = passenger
  }

  val logger = LoggerFactory.getLogger(this.getClass());
  var DistanceMap: Map[Vehicle, Double] = Map()


  def exec(currentTime: Long) {
    passenger.status = WAITING //更改用户状态
        logger.info("在  " + passenger.StartTime + " 有一个乘车请求,他要从第 " + passenger.StartGrid.getid +
             " 个格子到第 " + passenger.EndGrid.getid +" 个格子,他现在的状态是" + passenger.status )
    val removeTime = currentTime + passenger.waitingTime
//        logger.info("该用户将在  " + timeStamp2Date(removeTime+"") + " 时刻,在格子"+ passenger.StartGrid.getid +"被移除")

    //如果一定时间后还没来车 ，就在waitingTime时刻加一个remove操作

    //    logger.info("--------passenger的长度 -------  "+simulate.passengerList.size)
    if (simulate.tasks.contains(removeTime)) { //存在该ID，可以直接添加
      simulate.tasks.get(removeTime).get.+=(new RemovePassengerTask(passenger))
    } else { //当前ID尚不存在，需要添加map
      val newListBuffer: ListBuffer[Task] = ListBuffer()
      //          simulate.getPassengerList().append(passenger)
      newListBuffer.+=(new RemovePassengerTask(passenger))
      simulate.tasks += (removeTime -> newListBuffer)
    }

    //    logger.info("--------passenger的长度 -------  "+simulate.passengerList.size)
    val vehicle = assignVehicle(currentTime) //给当前用户分派车辆。返回要给分配的vehicle对象,dispatch没有任何问题
    //    if(vehicle == null)

    passenger.assignedVehicle = vehicle
    this.vehicle = vehicle
    println(vehicle)
    //添加movement事件
    //得到车辆信息,要添加movementTask,模拟移动
    if (vehicle == null) { //没有分配到车
               logger.info("给这个在 " + passenger.StartGrid.getid +" 的乘客分配车辆失败")
      for (eachtask <- tasks.get(removeTime).get) {
        if (eachtask.passenger.userId == passenger.userId) {
          var tempIndex = tasks.get(removeTime).get.indexOf(eachtask)
          tasks.get(removeTime).get.remove(tempIndex)
        }
      }
    } else { //分配到车了
      logger.info("给这个在 " + passenger.StartGrid.getid +" 的乘客分配车辆成功")
      // 需要判断当前车辆是否已经被启动了
      if (vehicle.status == TURN_ON) { //车子已经启动了

      } else if (vehicle.status == TURN_OFF) { //还没有启动
        if (simulate.tasks.contains(currentTime)) {
          //                simulate.tasks.get(currentTime).get.append(new MovementTask(vehicle))
          simulate.tasks.get(currentTime).get.append(new MovementTask(vehicle, passenger, 0))
        } else {
          val newListBuffer: ListBuffer[Task] = ListBuffer()
          newListBuffer.+=(new MovementTask(vehicle, passenger, 0)) //新人共享时间0
          //                newListBuffer.+=(new MovementTask(vehicle))
          simulate.tasks += (currentTime -> newListBuffer)
        }

        vehicle.setVehicleStatus(TURN_ON)
      }
    }


  }

  def assignVehicle(currentTime: Long): Vehicle = {
    //    logger.info("开始给在"+ passenger.StartGrid.getid +" 的用户分配车辆")
    //    var startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
    //    logger.info("分配车辆的开始时间"+startTime)

    var availVehicle = getNearestAvailableVehicle() //得到最近的可用的车

    if (availVehicle == null) { //没有分到车
      //      logger.info("获取车辆失败")
    } else { //只能在这里添加最为合适，分到车了
//            logger.info("给该用户分派的汽车ID为 " + availVehicle.vehicleName + "  ,他现在所在的格子编号为"+
//                      availVehicle.currentGrid.getid)

      //修改汽车相关属性
      availVehicle.currentPassengerNum += 1
      //      availVehicle.waitingPassengerList.append(passenger)

      var src = getAllStartPoint(availVehicle) //得到当前的所有的startpoint
      if (src == null) { //出错了
        return null
      }
      src.append(passenger.StartPoint) //添加新请求的startPoint

      var singlePickoffPoints = getSinglePick_offPoints(availVehicle) //得到所有落单的endpoint

      if (singlePickoffPoints == null) { //出错了
        return null
      }
      src.appendAll(singlePickoffPoints) //组成最终src

      //已经有分配车辆，需要移除对应的task


      //每一次新加一个passenger都要去更新schedule
//            logger.info("更新schedule之前的内容")
//            availVehicle.schedule.foreach(line => print(line.getGrid().getid + ","))
//            println()
//            logger.info("更新schedule")
//
//            var startTime1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
//            logger.info("findSchedule的开始时间"+startTime1)

      var scheduleList = NN_recommender.FindBestSchedule(availVehicle,
        ListBuffer(), src, 9999999, ListBuffer(), currentTime, availVehicle.schedule.size+2)._2




      if(scheduleList.isEmpty){
        availVehicle.currentPassengerNum -= 1
        return null
      }

//      println("test reachability")
      var firstpoint = scheduleList.head.getGrid()
//      println("--pathlist" + availVehicle.currentGrid.getid, firstpoint.getid )
      var firstPathList = Config.getRouteBetweenTwoPoint(
        new Point(availVehicle.currentGrid),
        new Point(firstpoint))._1

      if(firstPathList.isEmpty){
        availVehicle.currentPassengerNum -= 1
        return null
      }

      availVehicle.updateSchedule(scheduleList)
//      println("车号" + availVehicle.vehicleName + "-----schedule size----" + availVehicle.schedule.size)
//      availVehicle.schedule.foreach(line => print(line.getGrid().getid + "  "))
//      println()

//
//            logger.info("这个车上schedule的长度：  "+availVehicle.schedule.size)
//            var endTime1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
//            logger.info("findSchedule的结束时间"+endTime1)
//
//      logger.info("更新schedule之后的内容")
//      availVehicle.schedule.foreach(line => print(line.getGrid().getid + ","))
//      println()


      /**
        * 在dispatch阶段需要计算预期时间的用户是: 已经在车上的人
        *
        * 1.如果当前车上的用户大于一个人那么肯定共享过了。肯定有预计时间
        * 2.如果当前车上只有一个人，可能有过共享、也有可能还没有共享。因此需要拿出来计算一下。
        * 	2.1 共享过了
        * 	2.2没有共享过, 要考虑新加用户与车上用户的接送位置顺序关系
        */



      var numsOnTheVehicle = availVehicle.currentPassengerList.size //当前车上的乘客数

      if (numsOnTheVehicle > 1) { //车上的人正在共享
        // do nothing 
      } else if (numsOnTheVehicle == 1) { //有可能共享过了、有可能没共享
        // 获取车上乘客
        var passengerOnVehicle = availVehicle.currentPassengerList.head
        if (passengerOnVehicle.expectedTime == 0) { //还没有共享过
          var updatedSchedule = availVehicle.schedule
          if (updatedSchedule.head.getPassenger() == passengerOnVehicle) { // 先送即将到达的用户
            //没有共享
            // do nothing
          } else { //先要去接新来的用户，改变原来的行程 ==> 共享
            var expectedTime = Config.getExpectedTime(
              new Point(passengerOnVehicle.currentGrid), passengerOnVehicle.EndPoint)
            passengerOnVehicle.expectedTime = expectedTime
          }

        } else { //共享过了
          // do nothing
        }
      } else if (numsOnTheVehicle < 1) { //车上没有人
        // do nothing
      }


      // availVehicle.currentGrid 没用到
      availVehicle.updateRoute(Config.arrangeRoute(availVehicle.currentGrid, availVehicle.getSchedule())._1)
//      println("车辆 编号 " + availVehicle.vehicleName + "  第一次 更新后 新的 routelist为")
//      availVehicle.route.foreach(line=>print(line.getid + ","))
//      println()
      // 得到当前位置到第一个 point的路径规划
      firstpoint = availVehicle.getRoute().head


//      println("--pathlist" + availVehicle.currentGrid.getid, firstpoint.getid )
      firstPathList = Config.getRouteBetweenTwoPoint(
        new Point(availVehicle.currentGrid),
        new Point(firstpoint))._1


      /**
        * 有可能车子到schedule第一个位置的route不存在，这个时候，需要把这个乘客移除掉，然后 重新规划路线
       */
//      print("firstPathList : ")
//      firstPathList.foreach(line => print(line.getid + ","))
//      println()

      if(firstPathList.isEmpty){  // 需要把这个乘客的point去掉，然后重新规划路线
        var filteredSchedule = scheduleList.filter(line=> line.getPassenger().userId != this.passenger.userId)


      }else{  // 在可达的情况下，按照旧历执行

      }

      firstPathList.remove(firstPathList.size - 1)
      availVehicle.getRoute().insertAll(0, firstPathList)
      var tempPathlist = availVehicle.getRoute()
      var i = 0
      breakable(
        for (elem <- tempPathlist) {
          var index = tempPathlist.indexOf(elem)
          //         println("-+-+-+-+-+-+-+-+-+-+-+-+-" + index)
          if (index + 1 == tempPathlist.size)
            break
          if (elem.getid == tempPathlist.apply(index + 1).getid) {
            tempPathlist.remove(index)
          }
        }
      )


      availVehicle.updateRoute(tempPathlist)
//      println("车辆 编号 " + availVehicle.vehicleName + "  更新后 新的 routelist为")
//      availVehicle.route.foreach(line => print(line.getid + ","))
//      println()


//      println("route......." + availVehicle.route.size)
//      availVehicle.getRoute().foreach { x => print(x.getid + "  ") }
//      println()
    }
    return availVehicle
  }


  //所有已经接过的客人，pick_off的theOtherPoint肯定不在当前schedule里面。
  //而还没有被接的乘客，其pickup和pickoff肯定是对称存在于schedule中的
  def getSinglePick_offPoints(availVehicle: Vehicle): ListBuffer[Point] = {
    var pickoff_list: ListBuffer[Point] = ListBuffer()
    try {
      availVehicle.getSchedule().
        map { x =>
          if (!availVehicle.getSchedule().contains(x.getTheOtherPoint()))
            pickoff_list.append(x)
        }
    } catch {
      case t: Throwable => logger.error("AddPassenger里面出错了") // TODO: handle error

        return null
    }


    pickoff_list
  }


  def getNearestAvailableVehicle(): Vehicle = {

    val currentGrid = passenger.StartGrid

    DistanceMap.clear()
    //现在是对所有的车辆进行计算，下一步要去按照临近格子中包含的车辆
    simulate.getVehicles().map(_._2).foreach { vehicle =>
      InitDistanceList(vehicle,
        Config.getDistance((vehicle.currentGrid.getCenterPoint()._1, vehicle.currentGrid.getCenterPoint()._2),
          (passenger.StartLat, passenger.StartLon)))
    }

    return getMinDistanceVehicle()
  }

  def getAllStartPoint(availVehicle: Vehicle): ListBuffer[Point] = {
    var startPoint: ListBuffer[Point] = ListBuffer()
    try {
      availVehicle.getSchedule().
        map { x => if (x.getPointType() == PICKUP_POINT) startPoint.append(x) }
    } catch {
      case t: Throwable => logger.error("error") // TODO: handle error
        return null
    }

    startPoint
  }

  def getMinDistanceVehicle(): Vehicle = {
    var sortedByDistance = DistanceMap.toList.sortBy(_._2) //得到按距离排序由小到大的车辆信息
    //得到已排序的汽车列表中可用的第一辆车
    var sortedAvailVehicle = sortedByDistance.map(_._1).filter { vehicle =>
      vehicle.currentPassengerNum < Config.MAX_PASSENGER_NUM
    }

    var dispatchedVehicle: Vehicle = null

    if (sortedAvailVehicle.size == 0) {
      dispatchedVehicle = null
    } else {
      dispatchedVehicle = sortedAvailVehicle(0)
    }
    return dispatchedVehicle
  }

  def InitDistanceList(vehicle: Vehicle, distance: Double) = {
    DistanceMap += (vehicle -> distance)
  }

}