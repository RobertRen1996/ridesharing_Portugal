package shortestPath;

import java.io.*;
import java.util.*;


public class Dijkstra{

	static ArrayList<String> resultSet = new ArrayList<String>();
	static ArrayList<String> reflectionSet = new ArrayList<String>();
	static Map<String,String> reflection = new HashMap<String, String>();
	
  public static Graph createGraph(){
	  resultSet = readCsv("./portugal_road.csv");
      System.out.println("\n\n\n\n---------------result----------------");
      resultSet.remove(0);
//      for ( String line: resultSet
//      ) {
//          System.out.println(line);
//      }
      reflectionSet = readCsv("./reflection_portugal.csv");
      System.out.println("\n\n\n\n---------------reflectionSet----------------");
      reflectionSet.remove(0);
      
      
      for ( String line: reflectionSet
      ) {
    	  String [] strings = line.split(",");
    	  reflection.put(strings[0], strings[1]);
      }
     
//      for (String key : reflection.keySet()) { 
//    	  String value = reflection.get(key); 
//    	  System.out.println("Key = " + key + ", Value = " + value);
//      }
      

//      Dijkstra obj = new Dijkstra();

      // Create a new graph.
      Graph g = new Graph(26699);

      // Add the required edges.
      for (String line:resultSet
           ) {
          String []strings = line.split(",");
          int startV = Integer.parseInt(reflection.get(strings[0]));
          int endV = Integer.parseInt(reflection.get(strings[1]));
          int weight = Integer.parseInt(strings[2]);
//          System.out.println(startV+"  "+ endV +"  "+ weight);
          g.addEdge(startV, endV, weight);
      }
      
      return g;
//		g.addEdge(0, 1, 4); g.addEdge(0, 7, 8);
//		g.addEdge(1, 2, 8); g.addEdge(1, 7, 11); g.addEdge(2, 1, 8);
//		g.addEdge(2, 8, 2); g.addEdge(2, 5, 4); g.addEdge(2, 3, 7);
//		g.addEdge(3, 2, 7); g.addEdge(3, 5, 14); g.addEdge(3, 4, 9);
//		g.addEdge(4, 3, 9); g.addEdge(4, 5, 10);
//		g.addEdge(5, 4, 10); g.addEdge(5, 3, 9); g.addEdge(5, 2, 4); g.addEdge(5, 6, 2);
//		g.addEdge(6, 7, 1); g.addEdge(6, 8, 6); g.addEdge(6, 5, 2);
//		g.addEdge(7, 0, 8); g.addEdge(7, 8, 7); g.addEdge(7, 1, 11); g.addEdge(7, 6, 1);
//		g.addEdge(8, 2, 2); g.addEdge(8, 7, 7); g.addEdge(8, 6, 6);
      
      
      	//寻路过程
//      	int StartID = 4657;
//      	int EndID = 4028;
//
//
//		// Calculate Dijkstra.
//		obj.calculate(g.getVertex(StartID));
//		
//		Vertex v = g.getVertex(EndID);
//		System.out.println("Vertex - "+v+" , Dist - "+ v.minDistance + " , Path - ");
//		LinkedList<Vertex> shortestPath = v.path;
//		shortestPath.add(v);
//		
//		for(Vertex pathV : v.path)
//			System.out.println(pathV.getName());
		//寻路结束
		
		// Print the minimum Distance.
//		for(Vertex v:g.getVertices()){
//			System.out.print("Vertex - "+v+" , Dist - "+ v.minDistance+" , Path - ");
//			for(Vertex pathvert:v.path) {
//				System.out.print(pathvert+" ");
//			}
//			System.out.println(""+v);
//		}

	}
  
  	public static HashMap<Double, LinkedList<Vertex>> findshortestPath(Graph g1, int StartID, int EndID ){
  		
  		//寻路过程
        Dijkstra obj = new Dijkstra();
        // Create a new graph.
        Graph g = new Graph(26699);
        // Add the required edges.
        for (String line:resultSet
             ) {
            String []strings = line.split(",");
            int startV = Integer.parseInt(reflection.get(strings[0]));
            int endV = Integer.parseInt(reflection.get(strings[1]));
            int weight = Integer.parseInt(strings[2]);
//            System.out.println(startV+"  "+ endV +"  "+ weight);
            g.addEdge(startV, endV, weight);
        }
        
		// Calculate Dijkstra.
		obj.calculate(g.getVertex(StartID));
		
		Vertex v = g.getVertex(EndID);
//		System.out.println("Vertex - "+v+" , Dist - "+ v.minDistance + " , Path - ");
		LinkedList<Vertex> shortestPath = v.path;
		shortestPath.add(v);

        /**
         * 如果是1 是不合理的，在这里加判断
         */
//		for(Vertex pathV : v.path)
//			System.out.println(pathV.getName());
	//寻路结束
		
		HashMap<Double, LinkedList<Vertex>> result = new HashMap<>();
		result.put(v.minDistance, shortestPath);
		return result;
  	}
  	
	public void calculate(Vertex source){
		// Algo:
		// 1. Take the unvisited node with minimum weight.
		// 2. Visit all its neighbours.
		// 3. Update the distances for all the neighbours (In the Priority Queue).
		// Repeat the process till all the connected nodes are visited.
		
		source.minDistance = 0;
		PriorityQueue<Vertex> queue = new PriorityQueue<Vertex>();
		queue.add(source);
		
		while(!queue.isEmpty()){
			
			Vertex u = queue.poll();
		
			for(Edge neighbour:u.neighbours){
				Double newDist = u.minDistance+neighbour.weight;
				
				if(neighbour.target.minDistance>newDist){
					// Remove the node from the queue to update the distance value.
					queue.remove(neighbour.target);
					neighbour.target.minDistance = newDist;
					
					// Take the path visited till now and add the new node.s
					neighbour.target.path = new LinkedList<Vertex>(u.path);
					neighbour.target.path.add(u);
					
					//Reenter the node with new distance.
					queue.add(neighbour.target);					
				}
			}
		}
	}

    // 读取csv文件的内容
    public static ArrayList<String> readCsv(String filepath) {
        File csv = new File(filepath); // CSV文件路径
        csv.setReadable(true);//设置可读
        csv.setWritable(true);//设置可写
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(csv));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = "";
        String everyLine = "";
        ArrayList<String> allString = new ArrayList<>();
        try {
            while ((line = br.readLine()) != null) // 读取到的内容给line变量
            {
                everyLine = line;
//                System.out.println(everyLine);
                allString.add(everyLine);
            }
            System.out.println("csv表格中所有行数：" + allString.size());
        } catch (IOException e) {
            try {
                br.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return allString;

    }

}

