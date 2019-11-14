package task

import traits.Task
import model.Vehicle
import org.slf4j.LoggerFactory
import loadData.Config.status.WAITING
import loadData.Config.status.MISSING
import loadData.Config.status.PICKUP
import loadData.Config.status.FINISH
import loadData.Config.PointTypes.{PICKOFF_POINT, PICKUP_POINT}
import loadData.Config.VehicleTypes.{TURN_OFF, TURN_ON}

import simulate.simulate
import model.Passenger

import scala.collection.mutable.ListBuffer
import model.Grid
import model.Point

import loadData.Parser.timeStamp2Date
import loadData.Parser.encodeTime
import loadData.Parser.tranTimeToLong
import simulate.travelTime



class MovementTask extends Task {
  val logger = LoggerFactory.getLogger(this.getClass());
  var passenger: Passenger = null
  var vehicle: Vehicle = null
  var movementTime: Int = 0

  def this(vehicle: Vehicle, passenger: Passenger, movementTime: Int) {
    this()
    this.vehicle = vehicle
    this.passenger = passenger
    this.movementTime = movementTime //得到从上一个格子到这个格子所花费的时间
  }

  //为了Cruising的车设置的
  def this(vehicle: Vehicle, movementTime: Int) {
    this()
    this.vehicle = vehicle
    this.movementTime = movementTime //得到从上一个格子到这个格子所花费的时间
  }


  def exec(currentTime: Long) {

         // 更新车的的数据
      vehicle.travelDistance =vehicle.travelDistance + 1


    vehicle.currentGrid = vehicle.route.head //route列表存放的是每一次移动要经过的，更新当前所在的格子对象  //给车上每一个乘客更新tracedGrids
//    simulate.vehicles.filter(line=>line._2.vehicleName == vehicle.vehicleName).foreach(line=>line._2)
    vehicle.tracedRoutes.append(vehicle.currentGrid)
//    logger.info(timeStamp2Date(currentTime+"")+"时刻"+ vehicle.vehicleName +"到了,当前格子ID是" + vehicle.currentGrid.getid)

    NormalCondition(currentTime, vehicle)

  }

  /**
    * 新版本的上车下车处理操作
    *
    * @param currentTime
    * @param vehicle
    */
  def NormalCondition(currentTime: Long, vehicle: Vehicle) = {
//    println("车号   " + vehicle.vehicleName)
//    for (elem <- vehicle.currentPassengerList) {
//      println(elem.userId, elem.sharingTime, elem.expectedTime)
//    }
    //每移动到一个点，给当前车上的每一个乘客的tracedGrid添加当前的Grid。
    vehicle.currentPassengerList.foreach { p => p.tracedGrids.append(vehicle.currentGrid) }
    vehicle.currentPassengerList.foreach { p => p.currentGrid = vehicle.currentGrid }
    //更新共享时间,有expectedTime的才算是共享的
    vehicle.currentPassengerList.filter { p => p.expectedTime != 0 }
      .foreach { p => p.sharingTime = (p.sharingTime + this.movementTime) }

    if (vehicle.currentGrid == vehicle.schedule.head.getGrid()) { // 有人要上车或者下车

      /**
        * 去遍历这个schedule，直到某一个point不是currentGrid
        */

      // 得到在这个格子将要执行操作的point
      while (vehicle.schedule.nonEmpty && vehicle.currentGrid == vehicle.schedule.head.getGrid()) {
        // 遍历这个schedule
        var currentPoint = vehicle.schedule.head // 获取这个point
        var currentPassenger = currentPoint.getPassenger() //得到要上车或者下车乘客对象

        if (currentPoint.getPointType() == PICKUP_POINT) { //这个人要上车
          /** */

          //针对每一个用户的操作
          if (currentPassenger.status == MISSING) { //can't wait
            //            logger.info("待拉取乘客等不及已经走了")

            vehicle.currentPassengerNum -= 1
            //需要移除对应的事件，然后更新数据
            /**
              * 移除相应的src, 然后更新schedule和route
              * 这个操作在RemoveTask中已经做过了
              */
          } else if (currentPassenger.status == WAITING) { //乘客还在呢
//            logger.info(vehicle.vehicleName + " 车接客啦，乘客是 " + currentPassenger.userId + " , 当前在格子 " + currentPassenger.StartGrid.getid)
            vehicle.schedule.head.getPassenger().status = PICKUP

            /**
              * 需要把之前添加的removetask移除
              *
              * 得到RemoveTime的TaskList
              */
            var currentTimeTaskList = simulate.tasks(tranTimeToLong(currentPassenger.StartTime) + currentPassenger.waitingTime)

            simulate.tasks(tranTimeToLong(currentPassenger.StartTime) + currentPassenger.waitingTime).
              remove(currentTimeTaskList.indexOf(currentTimeTaskList.filter {
                task => task.passenger.userId == currentPassenger.userId
              }.head)
              )

            currentPassenger.pickUpTime = currentTime
            //刚刚接上来的乘客在这里添加tracedGrid信息，之后的move在前面添加
            //passenger.tracedGrids.append(vehicle.currentGrid)
            //            println(currentPassenger)
            var peopleNumOnVehicle = vehicle.currentPassengerList.size //当前车上的人数

            /**
              * 可以优化
              */
            // expectedTime
            if (peopleNumOnVehicle == 0) { //当前没有人
              /**
                * 有两种情况
                *  1.当前用户是此时schedule中最后一个要接的人，当前schedule中只有一个start point
                *  2.当前用户被pickup之后的schedule中至少还有一个要接的人，
                * 当前schedule中至少有两个start point (其中一个是当前用户的start point)
                **/

              var anotherStartPointBetweenSE = IsAnotherStartPointThere(currentPassenger, vehicle.schedule)

              if (anotherStartPointBetweenSE == true) { //有其他的S
                var expectedTime = loadData.Config.
                  getExpectedTime(currentPassenger.StartPoint, currentPassenger.EndPoint)
                currentPassenger.expectedTime = expectedTime //更新新加乘客的预计时间
              }
            } else if (peopleNumOnVehicle > 0) { //pickup当前用户时,车上已经有人了
              //需要计算当前乘客在当前位置的预计(expected)时间,
              //之前的那一个乘客的与其时间，应该在给当前车辆分配当前乘客时就已经计算过了
              var expectedTime = loadData.Config.
                getExpectedTime(currentPassenger.StartPoint, currentPassenger.EndPoint)
              currentPassenger.expectedTime = expectedTime //更新新加乘客的预计时间
            }

            vehicle.currentPassengerList.append(currentPassenger)
          }

          /**
            * 有人要下车
            */
        } else if (currentPoint.getPointType() == PICKOFF_POINT) { //这个人要下车
          /**
            * 必须要得到这个点在currentPassengerList中的对象，而不是一个单独的Passenger对象
            */
//          logger.info("有用户ID 为  " + currentPassenger.userId + "的人要在格子 " + vehicle.currentGrid.getid + " 下车\n")
          vehicle.currentPassengerList.filter(passenger => passenger == currentPassenger).foreach(passenger => {
//            println("下车  " + passenger.userId, currentPassenger.userId)
            passenger.status = FINISH //用户状态改变
            //把每一个完成travle的用户加入到allPickedPassenger中，便于后期统计
            vehicle.allPickedPassenger.append(passenger)

            //把他从当前的currentList中移除
            vehicle.currentPassengerList.
              remove(vehicle.currentPassengerList.indexOf(passenger))

            passenger.pickOffTime = currentTime

            vehicle.currentPassengerNum -= 1
            vehicle.numOfHunts += 1
            passenger.updateDuration((passenger.pickOffTime - passenger.pickUpTime))

          })

        }

        vehicle.schedule.remove(0)
      } // end while

    } else { //没有人要上/下车，添加下一个movementTask

    } //

    /**
      * 不管有没有人上下车，完了之后都需要进行AddNextTask
      */

    vehicle.route.remove(0) // 移除route的列表
    if (vehicle.route.nonEmpty) { //route 不是空的，说明还有下一个要走的格子
//      AddNextMovement(currentTime, passenger, vehicle.currentGrid, vehicle.route.head)

      AddNextMovement(currentTime, vehicle.currentGrid, vehicle.route.head)
    } else { //route为空，这个时候需要添加闲置状态
      vehicle.setVehicleStatus(TURN_OFF) //改变状态
      //      arrangeRouteForCruising(currentTime, vehicle)
    }

  }

  /**
    * 旧版本的上车下车处理操作
    *
    * @param currentTime
    * @param vehicle
    * @return
    */
//  def NormalCondition1(currentTime: Long, vehicle: Vehicle) = {
//    /** ***原来的代码开始的地方 *****/
//
//    //每移动到一个点，给当前车上的每一个乘客的tracedGrid添加当前的Grid。
//    vehicle.currentPassengerList.foreach { p => p.tracedGrids.append(vehicle.currentGrid) }
//    vehicle.currentPassengerList.foreach { p => p.currentGrid = vehicle.currentGrid }
//    //更新共享时间,值得商榷，
//    //movement的值是从上一个格子到这个各自所花费的时间
//    // 什么时候更新？？answer:便利当前车上所有用户，如果他的预期事件是0，不用累加共享时间，如果共享时间不为0，则累加共享时间。
//
//    vehicle.currentPassengerList.filter { p => p.expectedTime != 0 }
//      .foreach { p => p.sharingTime = (p.sharingTime + this.movementTime) }
//
//    /*if(vehicle.currentPassengerNum >= 2){ //有共享的乘客
//      vehicle.currentPassengerList.foreach { p => p.updateSharingTime(movementTime) }
//    }*/
//
//    if (vehicle.currentGrid == vehicle.schedule.head.getGrid()) { //有人要上车或者下车了
//      if (vehicle.schedule.head.getPointType() == PICKUP_POINT) { //有人要上车
//        var passenger = vehicle.schedule.head.getPassenger() //得到当前点发出的乘客是谁？？？
//      //得到要在这个点出发的乘客都有谁
//      var passengerInCurGridList = vehicle.schedule.filter(x =>
//        (x.getPointType() == PICKUP_POINT && x.getGrid() == vehicle.currentGrid))
//        var waitingPassengerList: ListBuffer[Passenger] = ListBuffer()
//        passengerInCurGridList.foreach { point => waitingPassengerList.append(point.getPassenger()) }
//
//        //          println(passenger)
//
//        waitingPassengerList.foreach { passenger =>
//
//          //针对每一个用户的操作
//          if (passenger.status == MISSING) { //can't wait
//            //              logger.info("待拉取乘客等不及已经走了")
//            //                vehicle.waitingPassengerList.drop(0)
//
//            vehicle.currentPassengerNum -= 1
//            //需要移除对应的事件，然后更新数据
//            /**
//              * 移除相应的src, 然后更新schedule和route
//              */
//          } else if (passenger.status == WAITING) { //乘客还在呢
//            //                  logger.info("接客啦，乘客是在    " + passenger.StartGrid.getid+","+passenger.StartTime)
//            //                vehicle.waitingPassengerList.head.status = PICKUP
//            vehicle.schedule.head.getPassenger().status = PICKUP
//            //需要把之前添加的removetask移除
//            ///***********待完成**************///
//            var currentTimeTaskList = simulate.tasks.
//              get((tranTimeToLong(passenger.StartTime) + passenger.waitingTime)).get
//
//            //                  currentTimeTaskList.remove(currentTimeTaskList.indexOf(currentTimeTaskList.
//            //                    filter { task => task.passenger ==  passenger}.head))
//
//            simulate.tasks.
//              get((tranTimeToLong(passenger.StartTime)
//                + passenger.waitingTime)).get.remove(
//              currentTimeTaskList.indexOf(currentTimeTaskList.
//                filter { task => task.passenger.userId == passenger.userId }.head)
//            )
//
//            passenger.pickUpTime = currentTime
//            //刚刚接上来的乘客在这里添加tracedGrid信息，之后的move在前面添加
//            //passenger.tracedGrids.append(vehicle.currentGrid)
//
//            var peopleNumOnVehicle = vehicle.currentPassengerList.size //当前车上的人数
//
//            /**
//              *
//              */
//            // expectedTime
//            if (peopleNumOnVehicle == 0) { //当前没有人
//              /**
//                * 有两种情况
//                *  1.当前用户是此时schedule中最后一个要接的人，当前schedule中只有一个start point
//                *  2.当前用户被pickup之后的schedule中至少还有一个要接的人，
//                * 当前schedule中至少有两个start point (其中一个是当前用户的start point)
//                **/
//
//              var anotherStartPointBetweenSE = IsAnotherStartPointThere(passenger, vehicle.schedule)
//
//              if (anotherStartPointBetweenSE == true) { //有其他的S
//
//                //                      println("----------- true ----------")
//                var expectedTime = loadData.Config.
//                  getExpectedTime(passenger.StartPoint, passenger.EndPoint)
//                passenger.expectedTime = expectedTime //更新新加乘客的预计时间
//              } else { // S 和 E之间没有其他的S
//                // do nothing
//              }
//
//
//            } else if (peopleNumOnVehicle > 0) { //pickup当前用户时,车上已经有人了
//              //需要计算当前乘客在当前位置的预计(expected)时间,
//              //之前的那一个乘客的与其时间，应该在给当前车辆分配当前乘客时就已经计算过了  /** 未完成 */
//              var expectedTime = loadData.Config.
//                getExpectedTime(passenger.StartPoint, passenger.EndPoint)
//              passenger.expectedTime = expectedTime //更新新加乘客的预计时间
//            }
//
//
//            vehicle.currentPassengerList.append(passenger)
//            //                  vehicle.waitingPassengerList.drop(0)
//            //vehicle.currentPassengerNum += 1
//            /**
//              *  logger.error("remove这个Vehicle的head" + vehicle.vehicleName)
//              */
//            vehicle.schedule.remove(0)
//
//          }
//
//        }
//
//
//      } else if (vehicle.schedule.head.getPointType() == PICKOFF_POINT) { //有人要下车
//        val EndPassengerList = vehicle.currentPassengerList.filter { passenger =>
//          passenger.EndGrid == vehicle.currentGrid
//        }
//        //针对每一个用户的移除操作
//        EndPassengerList.foreach { passenger =>
//
//          passenger.status = FINISH //用户状态改变
//          //把每一个完成travle的用户加入到allPickedPassenger中，便于后期统计
//          vehicle.allPickedPassenger.append(passenger)
//
//          //把他从当前的currentList中移除
//          vehicle.currentPassengerList.
//            remove(vehicle.currentPassengerList.indexOf(passenger))
//
//          passenger.pickOffTime = currentTime
//
//          if (vehicle.schedule.size == 0) {
//            println("将要出错了，出错车辆为" + vehicle.vehicleName)
//          }
//          vehicle.schedule.remove(0)
//
//          vehicle.currentPassengerNum -= 1
//          vehicle.numOfHunts += 1
//
//          passenger.updateDuration((passenger.pickOffTime - passenger.pickUpTime))
//
//        }
//
//      } //end-of-下车
//    }
//
//
//    //当车上没有单子时，，怎么办？？？
//    vehicle.route.remove(0) //Move之后需要移除头，但是这具体放在哪里有待之后的任务，觉得是要放在后面，因为要算时间
//    //      println(" 车子当前的ID: " +vehicle.currentGrid.getid)
//    //      println(vehicle.route.size)
//    if (vehicle.route.size != 0) { //还有要走的
//      //         println("即将要前往的道路" + vehicle.route.head.getid)
////      AddNextMovement(currentTime, passenger, vehicle.currentGrid, vehicle.route.head)
//      AddNextMovement(currentTime, vehicle.currentGrid, vehicle.route.head)
//    } else { //没有要走的了
//      vehicle.setVehicleStatus(TURN_OFF)
//      if (vehicle.currentPassengerNum != 0) {
//        vehicle
//      }
//
//      println("MMM车号 " + vehicle.vehicleName + "  MM乘客列表的个数：" + vehicle.currentPassengerList.size)
//      println("MMM车号 " + vehicle.vehicleName + "  MM乘客个数" + vehicle.currentPassengerNum)
//    }
//
//    /** ****正常的代码结束的地方 *******/
//  }


  def IsAnotherStartPointThere(passenger: Passenger, schedule: ListBuffer[Point]): Boolean = {
    //    schedule.foreach { p => println(p.getPassenger(), p.getPointType()) }
    //
    //    println("-----" + passenger)
    var startPoint = schedule.filter { p => p.getPassenger() == passenger }
      .filter { p => p.getPointType() == PICKUP_POINT }.head

    var endPoint = schedule.filter { p => p.getPassenger() == passenger }
      .filter { p => p.getPointType() == PICKOFF_POINT }.head

    //      logger.info("--------------------startIndex  " + startPoint +"endIndex" + endPoint )

    var startIndex = schedule.indexOf(startPoint)
    var endIndex = schedule.indexOf(endPoint)
    //      logger.info("--------------------startIndex  " + startIndex +"endIndex" + endIndex )
    if (endIndex == startIndex + 1) { //中间没有
      return false
    } else { //有其他的，肯定是个S
      return true
    }

  }


  def AddNextMovement(currentTime: Long, fromGrid: Grid, toGrid: Grid) = {

    var fromIndex = fromGrid.getid.toString()
    var toIndex = toGrid.getid.toString()

//    println(fromIndex, toIndex)
    var fromToTime = 0
//    println(fromIndex, toIndex)
    if (fromIndex != toIndex) //如果不相等，则需要。
      {
        fromToTime = travelTime.get(fromIndex).get.get(toIndex).get
      }

    // 23599,23759
    var time = currentTime + fromToTime //模拟格子移动后到目的地

//    logger.info("将在"+timeStamp2Date(time.toString())+"执行---------AddNextMovement-----------到达目的地")
//    logger.info("------" + timeStamp2Date(time.toString()))

    if (simulate.tasks.contains(time)) {
      simulate.tasks.get(time).get.append(new MovementTask(vehicle, passenger, fromToTime))
      //        simulate.tasks.get(time).get.append(new MovementTask(vehicle))
    } else {
      val newListBuffer: ListBuffer[Task] = ListBuffer()
      newListBuffer.+=(new MovementTask(vehicle, passenger, fromToTime))
      //        newListBuffer.+=(new MovementTask(vehicle))
      simulate.tasks += (time -> newListBuffer)
//      logger.info("****************  新的nexttaskADDED    ****************")
    }
  }


}