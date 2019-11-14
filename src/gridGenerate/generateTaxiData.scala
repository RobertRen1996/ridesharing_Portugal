package gridGenerate

object generateTaxiData {

  def main(args: Array[String]): Unit = {
    //首先初始化一个SparkSession对象
    val spark = org.apache.spark.sql.SparkSession.builder
      .master("local")
      .appName("Spark CSV Reader")
      .config("spark.sql.warehouse.dir", "file:///")
      .getOrCreate;

    //然后使用SparkSessions对象加载CSV成为DataFrame
    val df = spark.read
      .format("com.databricks.spark.csv")
      .option("header", "true") //reading the headers
      .option("mode", "DROPMALFORMED")
      .load("G:\\ridehsharing\\Portugal\\availableGrid.csv")

  }

}
