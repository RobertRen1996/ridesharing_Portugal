package processor

import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import au.com.bytecode.opencsv.CSVParser
import org.apache.spark.storage.StorageLevel  
import org.joda.time._
import org.joda.time.format._
import org.joda.time.DateTime
import java.util.Locale  
import java.text.SimpleDateFormat  
import java.util.Date
import scala.collection.immutable.Set
import scala.collection.mutable.ListBuffer
import loadData.Config
import model.GPSData
import gridGenerate.GridGenerate
import scala.collection.immutable.NumericRange
import org.datanucleus.store.rdbms.sql.method
import gridGenerate.GridGenerate
import scala.reflect.runtime.universe

object loadRouteData {
	//定义GPSdata和taxid的case class(临时数据表,参数为字段) 
      case class GpsdataTB(taxid:Int,datetime:Long,lon:Double,lat:Double,status:Int)
      case class tempTB(fromgrid:Int,togrid:Int,times:Double,date:String,time:String,datetime:Long)
      case class emptyTB(fromgrid:Int,togrid:Int,alltimes:Double,allcount:Int,avgtimes:Double,date:String,time:String,datetime:Long)
      case class notimeTB(fromgrid:Int,togrid:Int,times:Double)
	    var temp_list:List[tempTB]=List()
	    var empty_list:List[emptyTB]=List()
	    var notime_list:List[notimeTB]=List()
	    var allroute:DataFrame=null   //补全后的按时间段划分的route数据(学姐需要的route)!!!!!!!!!!!!!!!!!!!!!!!
	    var notimeallroute:DataFrame=null   //不按时间段划分的route数据(边、任)!!!!!!!!!!!!!!!!!!!!!!!
       
	   def InitRouteData()={
       val datapath="F:/software/Eclipse/Scala_workSpace/Taxi_Try/taxiData/*.csv"
          //val output="hdfs://master.sky:9000/output"
    
          val conf = new SparkConf().setAppName("Ridesharing").setMaster("local[*]")
          val sparkContext = new SparkContext(conf)
          val sqlContext = new SQLContext(sparkContext) 
                  
          
          //val sparkConf = new SparkConf().setAppName("Ridesharing").setMaster("spark://master.sky:7077").set("spark.executor.memory", "6g").set("spark.sql.warehouse.dir", ".")
//          val sparkContext = new SparkContext(sparkConf)
//          
//          val sqlContext = new SQLContext(sparkContext)
          
          //var spark=SparkSession.builder().master("local”).appName(“Ridesharing").config("spark.sql.warehouse.dir","file:///D:").getOrCreate() 
          
          //RDD隐式转换成DataFrame
          import sqlContext.implicits._
    
          //读取本地文件，读取path路径下的所有csv文件
          //Convert gps data RDD to a DataFrame and register it as a temp table ,并将date和time合并成时间戳
          val gpsdataDF = sparkContext.textFile(datapath).map(_.split(",")).map{r => 
        	  val datetime=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(r(2)+" "+r(3)).getTime()/1000   
        	  GpsdataTB(r(1).toInt,datetime,r(4).toDouble,r(5).toDouble,r(8).toInt)}.toDF()
          
          gpsdataDF.registerTempTable("gpsdatas")

          //cache the DF in memory with serializer should make the program run much faster  
          gpsdataDF.persist(StorageLevel.MEMORY_ONLY_SER)  
         
          //sqlContext.sql("select * from gpsdatas").show()
          
          //************获取不带时间间隔的route(边、任)************
          processReading(gpsdataDF)//获取不带时间间隔的route
          val notime_route=sqlContext.createDataFrame(notime_list)
          notime_route.registerTempTable("notimeroute_table")
          notimeallroute=sqlContext.sql("select fromgrid,togrid,sum(times) as alltimes,count(*) as allcount,mean(times) as avgtimes from notimeroute_table group by fromgrid,togrid")
        //*******************************************
          
          
          
          //*************带时间间隔的route(曲)****************
         val start_time:Long=1377964800     //2013/9/1 00:00:00 对应的时间戳(s)1377964800
         val end_time:Long=1383235200     //2013/10/31 24:00:00 对应的时间戳(s)1383235200
          
         val time_interval=1800  //时间间隔 30min  统计一次，换算为秒
         
         val times=start_time until end_time by time_interval
         
         times.map { t => 
        	  val fm=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")  //这个是你要转成后的时间的格式
        	   
        	   //时间戳转日期
        	// val tim = fm.format(new Date(t*1000))   //使用毫秒
        	 //val tim2=fm.format(new Date((t+time_interval)*1000))
        	 
        	 val datesegment=sqlContext.sql("select * from gpsdatas where datetime between "+t.toString()+" and "+(t+time_interval).toString())
        	 
            // datesegment.foreach { d => println(d)}
        	  
        	  // println(tim+"-"+tim2)
        	// var time:Array[Array[Double]]=null
        	 //var count:Array[Array[Double]]=null
        	 //datesegment.map { x => ??? }
        	 processReading(datesegment,t)
        	 
        	 }
        	   
        	   val temp_route=sqlContext.createDataFrame(temp_list)
         
        	   temp_route.registerTempTable("route_table")
        	  // temp_route.show()
        	   
        	   var routeDF=sqlContext.sql("select fromgrid,togrid,sum(times) as alltimes,count(*) as allcount,mean(times) as avgtimes,date,time,datetime from route_table group by fromgrid,togrid,date,time order by date,time")
        	   routeDF.registerTempTable("alltable")
        	   
        	   
        	   val allneigrid=sqlContext.sql("select fromgrid,togrid,count(*)  from alltable group by fromgrid,togrid")    //所有统计出的相邻两个grid的记录
        	   val emptygrid=allneigrid.filter { r => r.getInt(2)!=61*48 }.toDF()  //获取那些某些时间段没有值的grid对
        	   
        	  
        	   emptygrid.collect().map { rr => processEmpty(sqlContext,routeDF, rr, times)}
        
        	   var emptyDF:DataFrame=sqlContext.createDataFrame(empty_list)
        	   
        	  allroute= routeDF.unionAll(emptyDF)  //补全后的route数据(学姐需要的route)!!!!!!!!!!!!!!!!!!!!!!!
        	   
        	   //route.foreach { r => println(r) }
         
        	   //route.write.format("csv").save(output)
        	   //all.write.csv(output)  //保存到文件,怎样保存为单个文件
        	   //sparkContext.stop()
            
      }
        
        
     def main(args: Array[String]): Unit = {
        	
         
     }
     
     
     //处理按时间段划分的route数据
     def processReading(gpsreading: DataFrame,t:Long):Unit= {
    	 //val c =new Config()
    	 val number =Config.NUM_OF_LAT_BINS * Config.NUM_OF_LON_BINS
    	 var current:GPSData=null
    	 var last:GPSData=null
    	 val datee=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")  		 
    	 val datetime=datee.format(new Date(t*1000))    //年月日时分秒
    	 val ymd=datetime.split(" ")(0)  //年月日
    	 val hms=datetime.split(" ")(1) //时分秒
     
    	 //println(ymd+"-"+hms)
    	 
    	// val temp:Int=1  //计数
    	 gpsreading.collect().map { row => 
    		 //taxiid,
    		 current=new GPSData(row.getInt(0),row.getDouble(3),row.getDouble(2),row.getLong(1))
    		 //println(current.getTime)
    		
    		 if((last!=null)&&((current.getTime-last.getTime)<Config.MAX_TIME_INTERVAL)&&(current.getId==last.getId)){
    			if(isAdjacent(current, last)){
    				
    				 val fromgrid=GridGenerate.generateGridID(last.getLat,last.getLon)
    			      val togrid=GridGenerate.generateGridID(current.getLat,current.getLon)
    			      val times=current.getTime-last.getTime
    		
    			      temp_list=temp_list.:: (tempTB(fromgrid,togrid,times.toDouble,ymd.toString(),hms.toString(),t)) 
    			   // println(fromgrid+"-"+togrid+"-"+times+"-"+ymd+"-"+hms)
    		 }
    		}
    		last=current
    		//println(last.getTime)
    	 }
     }
     
     //处理不按时间段划分的route
     def processReading(gpsreading: DataFrame):Unit= {
    	
    	 val number =Config.NUM_OF_LAT_BINS * Config.NUM_OF_LON_BINS
    	 var current:GPSData=null
    	 var last:GPSData=null
  
    	 //println(ymd+"-"+hms)
    	 
    	// val temp:Int=1  //计数
    	 gpsreading.collect().map { row => 
    		 //taxiid,
    		 current=new GPSData(row.getInt(0),row.getDouble(3),row.getDouble(2),row.getLong(1))
    		 //println(current.getTime)
    		
    		 if((last!=null)&&((current.getTime-last.getTime)<Config.MAX_TIME_INTERVAL)&&(current.getId==last.getId)){
    			if(isAdjacent(current, last)){
    				
    				 val fromgrid=GridGenerate.generateGridID(last.getLat,last.getLon)
    			      val togrid=GridGenerate.generateGridID(current.getLat,current.getLon)
    			      val times=current.getTime-last.getTime
    		
    			      notime_list=notime_list.:: (notimeTB(fromgrid,togrid,times.toDouble)) 
    			   // println(fromgrid+"-"+togrid+"-"+times+"-"+ymd+"-"+hms)
    		 }
    		}
    		last=current
    		//println(last.getTime)
    	 }
     }
     
     
     def isAdjacent(current:GPSData, last:GPSData):Boolean= {
        if( last.getLatBin() >= Config.NUM_OF_LAT_BINS || last.getLatBin() < 0)
            return false
        if ( current.getLatBin() >= Config.NUM_OF_LAT_BINS || current.getLatBin() < 0)
            return false
        if ( last.getLonBin() >= Config.NUM_OF_LON_BINS || last.getLonBin() < 0)
            return false
        if ( current.getLonBin() >= Config.NUM_OF_LON_BINS || current.getLonBin() < 0)
            return false
        if (Math.abs(last.getLatBin() - current.getLatBin()) <= 1 && Math.abs(last.getLonBin() - current.getLonBin()) <=1)
                return true
        return false
    }  
     //处理按时间的划分route的空值
     def processEmpty(sqlContext:SQLContext,route:DataFrame,r:Row,times:NumericRange[Long]):Unit={
    	
    	 val empty_grids=route.where("fromgrid="+r.getString(0)+"and togrid="+r.getString(1))
    	 empty_grids.toDF().registerTempTable("emptytable")
         times.map { t => 
        	    		 
        	 val gg=empty_grids.where("datetime="+t.toString())
        	 if(gg==null){
        		 val dt=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")  		 
        		 val dtime=dt.format(new Date(t*1000))    //年月日时分秒
        		 val ymd=dtime.split(" ")(0)  //年月日
        		 val hms=dtime.split(" ")(1) //时分秒
        		 
        		 val route_timedf=sqlContext.sql("select mean(alltimes),mean(allcount),mean(avgtimes) from emptytable where time="+hms.toString())
        			 
        		route_timedf.collect().map { rr => 
        			empty_list=empty_list.:: (emptyTB(r.getInt(0),r.getInt(1),rr.getDouble(0),rr.getInt(1),rr.getDouble(2),ymd.toString(),hms.toString(),t))
        		 }
        	 }
        	    	
    	 }
     }
     
     
}