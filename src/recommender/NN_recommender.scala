package recommender

import loadData.Config
import loadData.Config.PointTypes.PICKUP_POINT
import model.{Point, Vehicle}

import scala.collection.mutable.ListBuffer

object NN_recommender {

  //计算效益最大的profit和schedule
	def FindBestSchedule(v:Vehicle,curlist:ListBuffer[Point],remlist:ListBuffer[Point],
	    oldbestdistance:Double,oldbestSchedule:ListBuffer[Point],startTime:Long, originalScheduleSize:Int):(Double,ListBuffer[Point])={

		var bestDistance:Double=oldbestdistance
		var bestSchedule:ListBuffer[Point]=oldbestSchedule

		for(p ← remlist){
			var f:ListBuffer[Point]=ListBuffer()
			if(curlist != null)
			  curlist.map { g => f.+=(g) }
			f.+=(p)

			var distance:Double = 0
			if(f.size == originalScheduleSize){
				distance=GetDistance(v, f,startTime)  //计算当前schedule的profit
				if(distance == -1)
					return (9999999, new ListBuffer[Point]())
			}
//			println("距离： " + distance)

				var r:ListBuffer[Point]=ListBuffer()
				remlist.map { g => r.+=(g) }
				r.remove(r.indexOf(p))
				if(p.getPointType()==PICKUP_POINT)
					r.+=(p.getTheOtherPoint())  //将当前pickup对应的pickoff放入r
				if(r.size==0){
					return (distance,f)
				}

				var profit_schedule=FindBestSchedule(v, f, r, bestDistance, bestSchedule, startTime, originalScheduleSize)
				if (profit_schedule._2.isEmpty){
					return (9999999, new ListBuffer[Point]())
				}else{
					if(profit_schedule._1 < bestDistance){
						bestDistance=profit_schedule._1
						bestSchedule=profit_schedule._2

					}
				}

		}

		(bestDistance, bestSchedule)
	}
  
  //我要算最短距离
	def GetDistance(v:Vehicle, f:ListBuffer[Point], startTime:Long) : Double = {

	  var distance = 0
	  var currentGrid = v.currentGrid
	  // 这里有一次规划路线 6447->5757
	  // 最短的距离，按照经过格子的个数
		distance = Config.arrangeRoute(currentGrid, f)._1.size
		if(distance == 0)
			return -1

	  return distance
	}
	
}