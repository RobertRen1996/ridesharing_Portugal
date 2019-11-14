package model

import loadData.Config
import gridGenerate.GridGenerate
import java.util.Date

class GPSData {
	      private var id:Int=0
        private var lat:Double=0
        private var lon:Double=0
        private var time:Long=0
        private var status:Int=0
        
       
        def this(lat:Double,lon:Double){
                this()
                this.lat=lat
                this.lon=lon
        }
	    
	    def this(taxid:Int,lat:Double,lon:Double,time:Long)
	    {
	    	this(lat,lon)
	    	this.id=taxid
	    	this.time=time	    	
	    }

        

        def this(taxid:Int,lat:Double,lon:Double,time:Long,status:Int){

                this(lat,lon)

                this.time=time

                this.status=status

        }

        

       

        

        def getId=id

        

        def setId(id:Int){

                this.id=id

        }

        

        

        def getLat=lat

        

        def setLat(lat:Double){

                this.lat=lat

        }

        

        def getLon=lon

        

        def setLon(lon:Double){

                this.lon=lon

        }

        

        def getTime=time

        

        def setTime(time:Long){

                this.time=time

        }

        

        def getStatus=status

        

        def setStatus(status:Byte){

                this.status=status

        }

        

        def isOccupide():Boolean={

                return this.status==1

        }

        

       

        

        def getGrid():Int={

                return GridGenerate.generateGridID(this.getLat ,this.getLon)

        }

        

        def getDate():Date={

                return new Date(this.time * 1000)

        }

        

        def getDateString():String={

                return Config.getDateTimeString(this.time)

        }

        

        def getLatBin():Int= {

                return GridGenerate.getLat_LonBin(this.getLat,this.getLon)._1

        }



    

        def getLonBin():Int= {

        

                return GridGenerate.getLat_LonBin(this.getLat,this.getLon)._2

    

        }

}