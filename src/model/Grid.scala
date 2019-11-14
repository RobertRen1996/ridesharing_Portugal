package model

import scala.collection
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import loadData.Config
import loadData.Config.getGridID

class Grid {
	
	      private var id:Int=0
	      private var latBin:Int=0
        private var lonBin:Int=0
        
        //Stores the neighboring grids of this grid
        private var neighbors= ListBuffer[Grid]()
        
        //Stores the average time to go from this grid to a neighboring grid
        private var time=HashMap[Grid,Double]()
        
        //Stores the distance between the center point of this grid and the center point of a neighboring grid.
        private var distance=HashMap[Grid,Double]()
        
        private var probability:Double=0
        
        private var maxNumberOfTaxis:Int=0;
        
        private var passengers=ListBuffer[Passenger]()
        
        private var central_Lat : Double = 0
        
        private var central_Lon : Double = 0
        
        
        //problem
        def this(id:Int) {
                this()
                latBin = Config.decodeLat_LonBin(id)._1
                lonBin = Config.decodeLat_LonBin(id)._2     
                this.id = id
        }
        
        
        def this(latBin:Int, lonBin:Int) {
                this()
                this.latBin = latBin
                this.lonBin = lonBin       
        }
        
        
        
        private def unHashLonBin(id:Int) :Int={
                id % Config.NUM_OF_LON_BINS
                
        }
        
        private def unHashLatBin(id:Int):Int ={
                id / Config.NUM_OF_LON_BINS
                
        }
        
        def getid=id
        
        def setid(id:Int){
        	this.id=id
        }
        
        
       
        
        
        
        
        /*
          * adds a grid to the neighbors of this grid.
          */
        def addNeighbor(g:Grid,  time:Double) :Unit={
                if (this.hashCode() != g.hashCode()) {
                        this.neighbors+=g
                        this.time+=(g → time)
                        var dis = Config.getDistance(this.getCenterPoint(),g.getCenterPoint())
                        this.distance+=(g → dis)       
                }
        }


        def getNeighborsID(r:Int,flag:Boolean): ListBuffer[Int] = {
		    var i:Int=1
		    var nei: ListBuffer[Int] = ListBuffer()
		    while (i <= r){
			    if (getLatBin >= (56+r)) {
				    nei.+=(getGridID(getLatBin - i, getLonBin))
			    }
			    if (getLatBin <= (67-r)) {
				    nei.+=(getGridID(getLatBin + i, getLonBin))
			    }
			    if (getLonBin >= (45+r)) {
				    nei.+=(getGridID(getLatBin, getLonBin - i))
			    }
			    if (getLonBin <= (55-r)) {
				    nei.+=(getGridID(getLatBin, getLonBin + i))
			    }
			    i += 1
		    }
		    if(flag){
			    if(getid==5655){
				    nei.+=(getGridID(getLatBin+r, getLonBin + r))
			    }else if(getid==5665){
				    nei.+=(getGridID(getLatBin+r, getLonBin - r))
			    }else if(getid==6777){
				    nei.+=(getGridID(getLatBin-r, getLonBin + r))
			    }else if(getid==6787){
				    nei.+=(getGridID(getLatBin-r, getLonBin - r))
			    }
		    }


		    return nei
	    }
        
        
        /*
          * returns the center point of the grid
          */
        def  getCenterPoint():(Double, Double)= {
                var lat:Double = (this.latBin - 0.5) * Config.ANGLE_CHUNK+ Config.MIN_LAT
                var lon:Double = (this.lonBin - 0.5) * Config.ANGLE_CHUNK+ Config.MIN_LON
                return (lat, lon)
        }
       /* 
        @Override
        def  hashCode():Int= {
                latBin * Config.NUM_OF_LON_BINS + lonBin
        }

        @Override
        def  toString():String= {
                this.getName()
        }
        */
        private def getName():String ={
                "(" + latBin + "," + lonBin + ")"
        }
        
        def isHighProbability():Boolean= {
                this.probability > Config.PROBABILITY_THRESHOLD
        }
        
        def getLatBin=latBin
        
        def setLatBin(newValue:Int){
                this.latBin=newValue
        }
        
        def getLonBin=lonBin
        
        def setLonBin(newValue:Int){
                this.lonBin=newValue
        }
        
        def getNeighbors=neighbors
        
        def setNeighbors(newValue:ListBuffer[Grid]){
                this.neighbors=newValue
        }
        
        def getTime=time
        
        def setTime(newValue:HashMap[Grid,Double]){
                this.time=newValue
        }
        
        def getDistance=distance
        
        def setDistance(newValue:HashMap[Grid,Double]){
                this.distance=newValue
        }
        
        def getProbability=probability
        
        def setProbability(newValue:Double){
                this.probability=newValue
        }
        
        def getMaxNumberOfTaxis=maxNumberOfTaxis
        
        def setMaxNumberOfTaxis( maxNumberOfTaxis:Int) {
                this.maxNumberOfTaxis = maxNumberOfTaxis;
    
        }
        
    
        def getgridDistance(g:Grid):Double= {
        
            Config.getDistance(this.getCenterPoint(), g.getCenterPoint())
        }

        
        def getPassengers=passengers

        def getFromToTime(g:Grid) :Double={
        
                this.time.apply(g)
        }
}