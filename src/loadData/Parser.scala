package loadData

import java.text.SimpleDateFormat
import java.util.Date
import java.lang.Long

object Parser {
  
    //解析时间
    def parserTime(time : String):Long = {
      var hour = time.substring(0, 2).toInt
      var minute = time.substring(3, 5).toInt
      var second = time.substring(6,time.length()).toInt
      
      var binaryTimeSeq = hour*3600 + minute * 60 + second
      
      return binaryTimeSeq
    }
  
    //编码时间
    def encodeTime(time : Long):String = {
      var hour = time/3600%24
      var minute = time%3600/60
      var second = time%3600%60
      return hour + ":" + minute + ":" + second
    }
  
    def tranTimeToLong(tm:String) :scala.Long={
//      println(tm)
      val fm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val dt = fm.parse(tm)
//      println(dt)
      val aa = fm.format(dt)
//      println(aa)
      val tim: Long = dt.getTime()
      var newtim = tim.toString().substring(0, 10).toLong
      newtim
  }
    
/*    def tranTimeToString(tm:String) :String={
      println( "--" +tm)
      
      
      var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      var lt = tm.toLong
      var date = new Date(lt);
      var res = simpleDateFormat.format(date);
      res
    }*/
    
    
    def timeStamp2Date( seconds :String) :String = {  
        var format = "yyyy-MM-dd HH:mm:ss";  
        var sdf = new SimpleDateFormat(format);  
        return sdf.format(new Date(Long.valueOf(seconds+"000")));  
    }  

}