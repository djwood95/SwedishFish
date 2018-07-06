import java.lang.reflect.Array;
import java.util.*;
import java.io.*;

public class Main {

    public static void main(String args[]) {

        //config variables:
        String[] animals = {"human", "mouse", "salmon", "zebrafish"};   //only used in multiple read mode
        //String directory = "/home/dan/dev/instances/rnaseq";            //only used in multiple read mode
        //String file = "/home/dan/dev/instances/rnaseq/test/1.graph";   //only used in single read mode
        //String truthFile = "/home/dan/dev/instances/rnaseq/test/1.truth"; //only used in single read mode
        String directory = "/home/dan/dev/instances/rnaseq";
        String file = "/home/peter/Desktop/instances/rnaseq/test/1.graph";         //either single or multiple
        String truthFile = "/home/peter/Desktop/instances/rnaseq/test/1.truth";
        String importMode = "multiple";                                   //either single or multiple


        ArrayList<Network> networks;
        if(importMode.equals("single")) {
            PrintWriter out = null;
            int[] resultBins = new int[100];
            int[] totals = new int[100];
            for(int i = 0; i < 100; i++) resultBins[i] = 0;
            for(int i = 0; i < 100; i++) totals[i] = 0;
            int numSuccess = 0;
            int numTotal = 0;


            try {
                out = new PrintWriter(new File("outputFile.txt"));
                networks = readGraphFile(file);
                //System.out.println(networks.toString());
                System.out.print(".");
                ArrayList<Integer> numTruthPaths = readTruthFile(truthFile);

                for(int num: numTruthPaths) {
                    if(num > 10) continue;
                    totals[num-1]++;
                }

                int count = 0;
                for(Network network: networks) {
                    ArrayList<Integer> valK = network.ValsFromZero();
                    //ArrayList<Integer> valK = network.possibleVals();
                    //ArrayList<Integer> valK = network.ValsToEnd();
                    Collections.sort(valK);
                    Collections.reverse(valK);
                    out.println("Graph # " + count);
                    network.collapseEdges();
                    System.out.println(network.toString());
                    int numPaths = 0;
                    ArrayList<Integer> sortedNodes = network.topoSort();
                    while(network.numEdges() > 0) {
                        ArrayList<Path> paths = new ArrayList<>();
                        for(int k: valK){
                            Path newPath = findMaxPath(network, k, sortedNodes, out);
                            if(newPath == null) break;
                            //out.println("SELECTED PATH: " + newPath.toString());
                            network.reducePath(newPath);
                            //out.println("SELECTED PATH: " + newPath.toString());
                            numPaths++;

                        }

                        for (int k = network.getMinEdge()-1; k < network.getMaxEdge(); k++) {
                            Path newPath = findMaxPath(network, k, sortedNodes, out);
                            if(newPath != null) {
                                paths.add(newPath);
                            }

                        }

                        int maxArea = -1;
                        Path selectedPath = null;
                        for (Path p : paths) {
                            int area = p.getFlow() * p.getEdges().size();
                            if (area > maxArea || maxArea < 0) {
                                maxArea = area;
                                selectedPath = p;
                            }
                        }
                        if(selectedPath == null) break;
                        //out.println("SELECTED PATH: " + selectedPath.toString());
                        numPaths++;

                        network.reducePath(selectedPath);
                    }

                    int truthPaths = numTruthPaths.get(count);
                    out.println("# Truth Paths = " + truthPaths + "\t # Actual Paths = " + numPaths);
                    if(numPaths <= truthPaths) {
                        resultBins[truthPaths-1]++;
                    }
                    //out.println();
                    count++;
                }
            } catch (FileNotFoundException e) {
                System.out.println("Could not open output file.");
                e.printStackTrace();
            } finally {
                out.close();
            }

            System.out.printf("# Paths\tSuccess Rate\n");
            for(int i = 0; i < 10; i++) {
                double successRate = ((double)resultBins[i] / totals[i]) * 100;
                System.out.printf("%d\t\t%.2f\n", i+1, successRate);
            }

            double successRate = ((double) numSuccess / numTotal) * 100;
            System.out.println("Success Rate = " + successRate);


        }

        if(importMode.equals("multiple")) {
            PrintWriter out = null;
            int[] resultBins = new int[100];
            int[] totals = new int[100];
            int numSuccess = 0;
            int numTotal = 0;

            try {
                out = new PrintWriter(new File("outputFile.txt"));

                File dir = new File(directory+"/human");
                File[] files = dir.listFiles();
                for(int i = 0; i < 100; i++) resultBins[i] = 0;
                for(int i = 0; i < 100; i++) totals[i] = 0;

                for (File curFile : files) {

                    int pos = curFile.getName().lastIndexOf(".");
                    String ext = curFile.getName().substring(pos+1);
                    String filenameNoExt = curFile.getName().substring(0, pos);
                    String filename = curFile.getName();
                    if(ext.equals("graph")) {
                        networks = readGraphFile(directory+"/human/"+filename);
                        ArrayList<Integer> numTruthPaths = readTruthFile(directory+"/human/"+filenameNoExt+".truth");

                        for(int num: numTruthPaths) {
                            totals[num-1]++;
                            numTotal++;
                        }

                        //System.out.println(Arrays.toString(totals));
                        System.out.print("*");
                        int count = 0;
                        int numPaths = 0;
                        for(Network network: networks) {
                            network.collapseEdges();
                            ArrayList<Integer> sortedNodes = network.topoSort();
                            //out.println("Graph # " + count);

                            // Max Frequencies
                            if(network.numNodes() < 20) {
                                //System.out.println("***LARGEST FREQUENCY***");
                                //find weight that appears on the most edges
                                HashMap<Integer, Integer> frequencies = new HashMap<>();
                                ArrayList<Edge> edges = network.getEdges();
                                for (Edge e : edges) {
                                    //System.out.println(e.toString());
                                    int weight = e.getWeight();
                                    if (frequencies.get(weight) == null) {
                                        frequencies.put(weight, 1);
                                    } else {
                                        int oldFreq = frequencies.get(weight);
                                        frequencies.put(weight, oldFreq + 1);
                                    }
                                }

                                int maxFreqWeight = -1;
                                int maxFreq = -1;
                                for (Map.Entry entry : frequencies.entrySet()) {
                                    //System.out.println(entry.toString());
                                    if ((int) entry.getValue() > maxFreq) {
                                        maxFreq = (int) entry.getValue();
                                        maxFreqWeight = (int) entry.getKey();
                                    }
                                }

                                //find the path that has the largest concentration of edges with the
                                //max-frequency weight
                                ArrayList<Path> allPaths = getAllPaths(network);
                                int max = -1;
                                Path maxPath = null;
                                //System.out.println(maxFreqWeight);
                                for (Path path : allPaths) {
                                    //System.out.println(path.toString());
                                    //System.out.println(path.toString());
                                    //System.out.println(path.getWeightFreq().toString());
                                    if (path.getWeightFreq().get(maxFreqWeight) != null && path.getWeightFreq().get(maxFreqWeight) > max) {
                                        max = path.getWeightFreq().get(maxFreqWeight);
                                        maxPath = path;
                                    }
                                }

                                network.reducePath(maxPath);
                            }

                            // Remove from beginning
                            ArrayList<Integer> valK = network.ValsToEnd();
                            Collections.sort(valK);
                            Collections.reverse(valK);

                            Network copy = new Network(network);
                            Network copy2 = new Network(network);

                                ArrayList<Path> paths;
                                for (int k : valK) {
                                    Path newPath = findMaxPath(network, k, sortedNodes, out);
                                    if (newPath == null) {
                                        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                        network = copy;
                                        valK = network.ValsFromZero();
                                        Collections.sort(valK);
                                        Collections.reverse(valK);
                                        numPaths = 0;
                                        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                        break;
                                    }
                                    network.reducePath(newPath);
                                    //out.println("SELECTED PATH: " + newPath.toString());
                                    numPaths++;

                                }
                                /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                for(int k: valK){
                                    Path newPath = findMaxPath(network, k, sortedNodes, out);
                                    if(newPath == null){
                                        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                        network = copy2;
                                        /*valK = network.possibleVals();
                                        Collections.sort(valK);
                                        Collections.reverse(valK);*/
                                        numPaths = 0;
                                        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                        break;
                                    }

                                    network.reducePath(newPath);
                                    //out.println("SELECTED PATH: " + newPath.toString());
                                    numPaths++;
                                }
                                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                                /*for(int k: valK){
                                    Path newPath = findMaxPath(network, k, sortedNodes, out);
                                    if(newPath == null) {
                                        numPaths = 0;
                                        break;
                                    }
                                    network.reducePath(newPath);//out.println("SELECTED PATH: " + newPath.toString());
                                        numPaths++;
                                }*/


                                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                //paths = new ArrayList<>();
                                //System.out.println("Using area");
                            while(network.numEdges() > 0){
                                paths = new ArrayList<>();
                                for (int k = network.getMinEdge()-1; k <= network.getMaxEdge(); k++) {
                                    Path newPath = findMaxPath(network, k, sortedNodes, out);
                                    if(newPath != null) {
                                        paths.add(newPath);
                                    }
                                }

                                int maxArea = -1;
                                Path selectedPath = null;
                                for (Path p : paths) {
                                    int area = p.getFlow() * p.getEdges().size();
                                    if (area > maxArea || maxArea < 0) {
                                        maxArea = area;
                                        selectedPath = p;
                                    }
                                }
                                if(selectedPath == null) break;
                                //out.println("SELECTED PATH: " + selectedPath.toString());
                                numPaths++;
                                network.reducePath(selectedPath);


                            }//=============================================================================================================================================

                            int truthPaths = numTruthPaths.get(count);
                            out.println("# Truth Paths = " + truthPaths + "\t # Actual Paths = " + numPaths);
                            if(numPaths <= truthPaths) {
                                resultBins[truthPaths-1]++;
                            }
                            //out.println();
                            count++;
                        }
                    }
                }

            } catch (FileNotFoundException e) {
                System.out.println("Could not open output file.");
                e.printStackTrace();
            } finally {
                out.close();
            }

            System.out.printf("\n# Paths\tSuccess Rate\n");
            for(int i = 0; i < 10; i++) {
                double successRate = ((double)resultBins[i] / totals[i]) * 100;
                System.out.printf("%d\t\t%.2f\n", i+1, successRate);
            }

            /*System.out.println();
            double successRate = ((double) numSuccess / numTotal) * 100;
            System.out.printf("Total Success Rate = %.2f", successRate);*/
        }
    }

    /**
     * Parses the lines of a .graph file into a Network object
     * @param graphs - each row contains a String representation of a graph
     * @return
     */
    public static ArrayList<Network> parseGraph(ArrayList<String> graphs) {
        ArrayList<Network> networks = new ArrayList<>();
        for(String graph: graphs) {
            Network network = new Network();
            String[] lines = graph.split("\n");
            int numNodes = Integer.parseInt(lines[0]);
            //System.out.println("***NEW GRAPH***");

            for(int i = 0; i < numNodes; i++) {
                network.addNode();
            }
            //System.out.println(numNodes + " nodes added!");

            for(int i = 1; i < lines.length; i++) {
                String[] data = lines[i].split(" ");
                Node fromNode = network.getNode(Integer.parseInt(data[0]));
                Node toNode = network.getNode(Integer.parseInt(data[1]));
                int weight = (int) Double.parseDouble(data[2]);
                network.addEdge(fromNode, toNode, weight);
                //System.out.println("Edge: "+fromNode+" -> "+toNode+": "+weight);
            }
            networks.add(network);
        }

        return networks;
    }

    public static ArrayList<Integer> readTruthFile(String file) {
        File inputFile = new File(file);
        ArrayList<Integer> numTruthPaths = new ArrayList<>();
        Scanner scan;

        try {
            scan = new Scanner(inputFile);
            scan.useDelimiter("#[\\s\\S]+?[\\n]"); //splits into graphs by # XXX
            while(scan.hasNext()) {
                String[] lines = scan.next().split("\n");
                numTruthPaths.add(lines.length);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not open file: " + inputFile.toString());
            e.printStackTrace();
        }

        return numTruthPaths;
    }

    /**
     * Reads in all graph files for selected animal subfolders
     */
    public static ArrayList<Network> readGraphFiles(String directory, String[] animals) {
        ArrayList<Network> networks = new ArrayList<>();

        for(String animal: animals) {
            File dir = new File(directory+"/"+animal);
            File[] files = dir.listFiles();

            for (File file : files) {
                int pos = file.getName().lastIndexOf(".");
                String ext = file.getName().substring(pos+1);
                //System.out.println(ext);
                if(!ext.equals("graph")) continue;
                String filePath = file.getPath();
                networks.addAll(readGraphFile(filePath));
            }
        }

        return networks;
    }

    /**
     * Parses a .graph file into individual networks
     * @param file - path to the file
     * @return - list of networks after running on parseGraph()
     */
    public static ArrayList<Network> readGraphFile(String file) {
        File inputFile = new File(file);
        ArrayList<Network> networks;
        ArrayList<String> graphs = new ArrayList<>();
        Scanner scan;

        try {
            scan = new Scanner(inputFile);
            scan.useDelimiter("#[\\s\\S]+?[\\n]"); //splits into graphs by # XXX
            while(scan.hasNext()) {
                graphs.add(scan.next());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not open file: " + inputFile.toString());
            e.printStackTrace();
        }

        //System.out.println(graphs.toString());
        //System.out.print(".");
        networks = parseGraph(graphs);

        return networks;
    }


    /**
     * Finds the path of maximum length with a flow of k
     * @param k
     * @return length of the path (# of nodes, not weight)
     */


    public static Path findMaxPath(Network network, int k, ArrayList<Integer> sortedNodes, PrintWriter out) {

        int[] lengths = new int[network.numNodes()];
        Edge[] selectedEdges = new Edge[network.numNodes()];
        for(int i = 0; i < lengths.length; i++) lengths[i] = -1;
        for(int i = 0; i < selectedEdges.length; i++) selectedEdges[i] = null;
        lengths[0] = 0;

        //System.out.println(sortedNodes);
        for(int nodeId: sortedNodes) {
            Node node = network.getNode(nodeId);

            for(Edge e: node.getOutgoingEdges()) {
                int weight = e.getWeight();
                int newLength = 1 + lengths[nodeId];
                int toNodeId = e.getToNode().getId();

                    if (weight >= k && newLength >= lengths[toNodeId]) {
                        //take smaller weight as tie-breaker
                        if (newLength == lengths[toNodeId] && weight >= selectedEdges[toNodeId].getWeight()) continue;
                        lengths[toNodeId] = newLength;
                        selectedEdges[toNodeId] = e;
                    }

            }
        }

        int count = 0;
        int i = selectedEdges.length-1;
        //System.out.println(Arrays.toString(selectedEdges));
        Stack<Edge> edgesReverse = new Stack<Edge>();
        while(i > 0) {
            Edge e = selectedEdges[i];
            if(e == null) return null;
            Node fromNode = e.getFromNode();
            i = fromNode.getId();
            edgesReverse.push(e);
            count++;
            if(count > selectedEdges.length) return null;
        }

        ArrayList<Edge> selectedEdges2 = new ArrayList<>();
        while(!edgesReverse.empty()) {
            Edge e = edgesReverse.pop();
            selectedEdges2.add(e);
        }

        //out.printf("MAX PATH (Flow %d) = " + Arrays.toString(lengths)+"\n" + Arrays.toString(selectedEdges)+"\n", k);

        Path path = new Path(selectedEdges2);

        return path;
    }

    public static ArrayList<Path> getAllPaths(Network network) {
        Node src = network.getNode(0);
        Node dest = network.getNode(network.numNodes()-1);
        ArrayList<Path> paths = new ArrayList<>();
        ArrayList<Edge> path = new ArrayList<>();
        //System.out.println(network.toString());

        return getAllPathsUtil(network, src, dest, paths, path);
    }

    public static ArrayList<Path> getAllPathsUtil(Network network, Node src, Node dest, ArrayList<Path> paths, ArrayList<Edge> path) {
        src.setVisited(true);

        if(src.getId() == dest.getId()) {
            paths.add(new Path(path));
            //System.out.print(".");
        }

        for(Edge e: src.getOutgoingEdges()) {
            Node n = e.getToNode();
            if(!n.isVisited()) {
                path.add(e);
                getAllPathsUtil(network, n, dest, paths, path);
                path.remove(e);
            }
        }

        src.setVisited(false);

        return paths;
    }


}
