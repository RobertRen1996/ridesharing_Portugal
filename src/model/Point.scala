package model

import loadData.Config.PointTypes.PointType

class Point {
  private var pointType : PointType  = null
  private var theOtherPoint : Point = null
  private var InGrid : Grid = null
  private var passenger:Passenger=null  //该point对应的乘客的上车点或下车点
  
  def this(pointType : PointType, theOtherPoint : Point, InGrid : Grid){
    this()
    this.pointType = pointType
    this.theOtherPoint = theOtherPoint
    this.InGrid = InGrid
  }
  
    def this(pointType : PointType, InGrid : Grid){
    this()
    this.pointType = pointType
    this.InGrid = InGrid
  }
  
    def this( InGrid : Grid){
    this()
    this.InGrid = InGrid
  }
    
  def getPointType()={
    pointType
  }
  
  def getPassenger()={
	  passenger
  }
  
  def setPassenger(passenger:Passenger)={
	  this.passenger=passenger
  }
  
  def getTheOtherPoint()={
    theOtherPoint
  }
  
  def getGrid()={
    InGrid
  }
  
  def setPointType(pointType : PointType) = {
    this.pointType = pointType
  }
  
  def setTheOtherPoint(theOtherPoint : Point) = {
    this.theOtherPoint = theOtherPoint
  }
  
  def setGrid(InGrid : Grid) = {
    this.InGrid = InGrid
  }
}