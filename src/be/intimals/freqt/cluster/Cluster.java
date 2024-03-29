package be.intimals.freqt.cluster;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class Cluster {

    private String inputDir;
    private String outputDir;
    private String algorithmName;
    private String numberCluster;
    private String svd;

    private List<String> labels = new LinkedList<>();
    private ArrayList<String> fileNames = new ArrayList<>();
    private ArrayList < ArrayList<Integer> > database = new ArrayList<>();
    private Map<String,Set<String>> whiteLabels = new HashMap<>();
    private Set<String> rootLabels = new LinkedHashSet<>();
    private Set<String> visitedRootLabel = new HashSet<>();
    private String currentRootLabel;
    private String createDataBaseOption;

    ////////////////////////

    public Cluster(String _inputDir, String _outputDir, String _createDataBaseOption, String _algorithmName, String _numCluster, String _svd){
        inputDir = _inputDir;
        outputDir = _outputDir;
        createDataBaseOption = _createDataBaseOption;
        algorithmName = _algorithmName;
        numberCluster = _numCluster;
        svd = _svd;
    }


    //delete directory
    private void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }

    //collect full name of files in the directory
    private void populateFileListNew(File directory, ArrayList<String> list){
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        ArrayList<String> fullNames = new ArrayList<>();
        for(int i=0; i<files.length; ++i)
            fullNames.add(files[i].getAbsolutePath());
        list.addAll(fullNames);
        File[] directories = directory.listFiles(File::isDirectory);
        for (File dir : directories) populateFileListNew(dir,list);
        Collections.sort(list);
    }

    //read AST tree by breadth first traversal
    private void readTreeDepthFirst(Node node , ArrayList <Integer> trans) {
        try {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                //keep the lineNr of root labels which have been visited
                if(rootLabels.contains(node.getNodeName())){
                    String lineNr = node.getAttributes().getNamedItem("LineNr").getNodeValue();
                    visitedRootLabel.add(lineNr);
                }

                if(createDataBaseOption.equals("1")){//keep internal labels
                    if(labels.isEmpty()) {
                        trans.add(0);
                        labels.add(node.getNodeName());
                    }
                    else{
                        if(!labels.contains(node.getNodeName())) {
                            trans.add(labels.size());
                            labels.add(node.getNodeName());
                        }else{
                            trans.add(labels.indexOf(node.getNodeName()));
                        }
                    }
                }
                if (node.hasChildNodes()) {
                    //if node is a leaf
                    if (node.getChildNodes().getLength() == 1) {
                        String leafLabel = node.getTextContent().trim();
                        if(!labels.contains(leafLabel)) {
                            trans.add(labels.size());
                            labels.add(leafLabel);
                        }else {
                            trans.add(labels.indexOf(leafLabel));
                        }
                    } else {//internal node
                        NodeList nodeList = node.getChildNodes();
                        //only allow children labels which are in the white list
                        if(whiteLabels.containsKey(node.getNodeName())){
                            //System.out.println(node.getNodeName());
                            Set<String> temp = whiteLabels.get(node.getNodeName());
                            for(int i=0; i<nodeList.getLength(); ++i)
                                if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                    if(temp.contains(nodeList.item(i).getNodeName())) {
                                        //System.out.println(nodeList.item(i).getNodeName());
                                        readTreeDepthFirst(nodeList.item(i), trans);
                                    }
                                }
                        }else{
                            //recur reading every child node
                            for (int i = 0; i < nodeList.getLength(); ++i) {
                                if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                    readTreeDepthFirst(nodeList.item(i), trans);
                                }
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    //create database from ASTs
    private void readDatabase(File rootDirectory) {
        //collect all input files
        ArrayList<String> files = new ArrayList<>();
        populateFileListNew(rootDirectory, files);
        //read white labels from file
        readWhiteLabel("conf/listWhiteLabel.txt", whiteLabels);
        readRootLabel("conf/listRootLabel.txt", rootLabels);
        //create database
        System.out.print("Reading " + files.size() +" files ... ");
        //XmlFormatter formatter = new XmlFormatter();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            for (String fi : files) {
                //keep the file's name
                fileNames.add(fi);
                //System.out.println(fi);
                //format XML file before create tree
                //String inFileTemp = rootDirectory+sep+"temp.xml";
                //Files.deleteIfExists(Paths.get(inFileTemp));
                //formatter.format(fi,inFileTemp);
                File fXmlFile = new File(fi);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(fXmlFile);
                doc.getDocumentElement().normalize();
                //store lineNr of visited labels which are in the given root labels
                visitedRootLabel = new HashSet<>();
                //for each file create an ArrayList of Integer
                ArrayList<Integer> trans = new ArrayList<>();
                for(String rootLabel : rootLabels){
                    //System.out.println(rootLabel);
                    //get a list of nodes which have rootLabel as label
                    NodeList nList = doc.getElementsByTagName(rootLabel);
                    //for each node expand to collect the set of labels
                    for(int i=0; i<nList.getLength(); ++i){
                        //get lineNr of the current rootLabel
                        String lineNr = nList.item(i).getAttributes().getNamedItem("LineNr").getNodeValue();
                        if (!visitedRootLabel.contains(lineNr)) {
                            readTreeDepthFirst(nList.item(i), trans);
                        }
                    }
                }
                //read from AST's root node
                //readTreeDepthFirst(doc.getDocumentElement(),trans);
                //System.out.println(trans);
                //add found ArrayList into database
                database.add(trans);
            }
            //System.exit(-1);
            //System.out.println(fileNames);
//            System.out.println(labels);
//            for(ArrayList<Integer> arr : database)
//                System.out.println(arr);

            System.out.println("end.");
        } catch (Exception e) {
            System.out.println("read error.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void readRootLabel(String path, Set<String> _rootLabels){

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() && line.charAt(0) != '#' ){
                    String[] str_tmp = line.split(" ");
                    _rootLabels.add(str_tmp[0]);
                }
            }
        }catch (IOException e) {System.out.println("Error: reading listRootLabel "+e);}
    }

    //read white labels from given file
    private static void readWhiteLabel(String path, Map<String,Set<String> > _whiteLabels){
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() && line.charAt(0) != '#' ) {
                    String[] str_tmp = line.split(" ");
                    String ASTNode = str_tmp[0];
                    Set<String> children = new HashSet<>();
                    for(int i=1; i<str_tmp.length; ++i){
                        children.add(str_tmp[i]);
                    }
                    _whiteLabels.put(ASTNode,children);
                }
            }
        }catch (IOException e) {System.out.println("Error: reading white list "+e);}
    }

    //create database for clustering algorithm
    private void createCSV(String outputFileName){
        try{
            System.out.print("Creating database");
            FileWriter fr = new FileWriter(outputFileName);

            //create values for an instance (AST)
            //ArrayList<Integer> allLabels = new ArrayList<>(labelIndex.keySet());

            //Collections.sort(allLabels);
            System.out.print(" (instances: "+database.size()+ ", attributes: "+labels.size()+") ... ");

            ArrayList<Integer> allLabels = new ArrayList<>();
            for(int i=0; i<labels.size(); ++i)
                allLabels.add(i);

            for(int i=0; i<database.size(); ++i) {
                ArrayList<Integer> currentInstance = database.get(i);
                Collections.sort(currentInstance);
                //init tempInstance
                Integer[] tempInstance = new Integer[labels.size()];
                for(int j=0; j<tempInstance.length; ++j) tempInstance[j] = 0;
                //create instance
                for (int j=0; j<currentInstance.size(); ++j) {
                    int index = allLabels.indexOf(currentInstance.get(j));
                    tempInstance[index] = tempInstance[index]+1;
                }
                //write instance to file
                String str="";
                for(int j=0; j<tempInstance.length-1; ++j) {
                    //System.out.print(tempInstance[j] + " ");
                    str += Double.valueOf(tempInstance[j])+",";
                }
                str += Double.valueOf(tempInstance[tempInstance.length-1]);
                fr.write(str+"\n");
            }
            fr.flush();
            fr.close();
            System.out.println(" end.");

        }catch (Exception e){
            System.out.println("error: creating database "+e);
        }
    }

    //create directories to store files of clusters
    private void createClusterDir(String outputDir,String inputFile){
        try{
            //create sub-directories to store ASTs of each clusters
            File c = new File(inputFile);
            if(c.length() == 0){
                System.out.println("No cluster found");
                System.out.println("Finished.");
            }else {
                System.out.println("Creating clusters' directories ...");
                Map<String, Vector<String>> foundClusters = new HashMap<>();
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                String line;
                int count=0;
                while ((line = br.readLine()) != null) {
                    String[] row = line.split(" ");
                    for(int i=0; i< row.length; ++i){
                        if(!row[i].isEmpty()){
                            String clusterID = row[i].trim();
                            String sampleID = String.valueOf(count);
                            if(foundClusters.containsKey(clusterID)){
                                Vector<String> rowTemp = new Vector<>(foundClusters.get(clusterID));
                                rowTemp.add(sampleID);
                                foundClusters.replace(clusterID,rowTemp);

                            }else{
                                Vector<String> rowTemp = new Vector<>();
                                rowTemp.add(sampleID);
                                foundClusters.put(clusterID,rowTemp);
                            }
                            count++;
                        }
                    }
                }
                //put the last cluster to clustersFiles
                System.out.println("\nnumber clusters: "+foundClusters.size());
                //copy all files in a cluster to folder "fold_i", i is the id of cluster
                for (Map.Entry<String, Vector<String>> entry : foundClusters.entrySet()) {
                    String folderName = Paths.get(outputDir, "fold_" + entry.getKey()).toString();
                    System.out.println("fold " + folderName + ", #file: " + entry.getValue().size());
                    File folder = new File(folderName);
                    if (!folder.exists()) folder.mkdir();

                    //copy source to target using Files Class
                    String Lseparator = "/"; //Linux, MacOS separator = "/", Windows 
                    String Wseparator = "\\";
                    for (int j = 0; j < entry.getValue().size(); ++j) {
                        String sourceFilePath = fileNames.get(Integer.valueOf(entry.getValue().get(j)));
                        String[] splittedPath;
                        String sourceFileName = "";
                        String targetFilePath = "";
                        if(sourceFilePath.split(Pattern.quote(Lseparator)).length > 1) {
                        	splittedPath = sourceFilePath.split(Pattern.quote(Lseparator));
                        	sourceFileName = splittedPath[splittedPath.length-1];
                        	targetFilePath = folder.getAbsolutePath()+ Lseparator + sourceFileName;
                        }else if(sourceFilePath.split(Pattern.quote(Wseparator)).length > 1) {
                    		splittedPath = sourceFilePath.split(Pattern.quote(Wseparator));
                    		sourceFileName = splittedPath[splittedPath.length-1];
                    		targetFilePath = folder.getAbsolutePath()+ Wseparator + sourceFileName;
                        }
                        
                        Path sourceDirectory = Paths.get(sourceFilePath);
                        Path targetDirectory = Paths.get(targetFilePath);
                        Files.copy(sourceDirectory, targetDirectory, StandardCopyOption.REPLACE_EXISTING);

                        try{ //I let this here but it should really be refactored
                            String sourceJavaFilePath = fileNames.get(Integer.valueOf(entry.getValue().get(j))).substring(0,
                                    fileNames.get(Integer.valueOf(entry.getValue().get(j))).length()-3)+"java";
                            String sourceJavaFileName = sourceFileName.substring(0,sourceFileName.length()-3)+"java";
                            String targetJavaFilePath = folder.getAbsolutePath()+ Lseparator + sourceJavaFileName;
                            Path sourceJavaDirectory = Paths.get(sourceJavaFilePath);
                            Path targetJavaDirectory = Paths.get(targetJavaFilePath);
                            Files.copy(sourceJavaDirectory, targetJavaDirectory, StandardCopyOption.REPLACE_EXISTING);

                        }catch (Exception e){
                            //System.out.println("not found java file ");
                        }

                    }
                }
                System.out.println("end.");
            }

        }catch (Exception e){
            System.out.println("\n error: createClusterPy "+e);
        }
    }

    //run clustering algorithm
    public void run(){
        try{
            //read ASTs database
            readDatabase(new File(this.inputDir));
            //create csv file for clustering algorithm
            String inputDataCSV = "clusterInputData.csv";
            createCSV(inputDataCSV);
            //create output directory
            File outputDirectory = new File(this.outputDir);
            if(outputDirectory.exists()) {
                deleteDirectoryRecursion(Paths.get(this.outputDir));
                outputDirectory.mkdirs();
            }
            else
                outputDirectory.mkdirs();
            //run cluster algorithm
            System.out.println("Running "+ (this.algorithmName.equals("1") ? "hierarchical " : "kmeans ") + "clustering algorithm ... " );
            String outputCluster = Paths.get(this.outputDir, "outputCluster.txt").toString();
            String clusterScript = Paths.get("clustering", "clustering.py").toString();
            String python3 = "python3 ";
            String python = "python ";
            String commandStr = clusterScript + " " + inputDataCSV + " \"" + outputCluster + "\" " + this.algorithmName+ " " + this.numberCluster + " "+ this.svd;
            try{
                Process proc = Runtime.getRuntime().exec(python3+commandStr); //Run on Mac/Linux
                proc.waitFor();
            }catch(IOException e) {
                try {
                    Process proc = Runtime.getRuntime().exec(python+commandStr); //Run on Windows
                    BufferedReader stdInput = new BufferedReader(new 
                            InputStreamReader(proc.getInputStream()));

                       BufferedReader stdError = new BufferedReader(new 
                            InputStreamReader(proc.getErrorStream()));
                       proc.waitFor();
                       
                       // read the output from the command
                       System.out.println("Here is the standard output of the command:\n");
                       String s = "";
                       while ((s = stdInput.readLine()) != null) {
                           System.out.println(s);
                       }
                       
                       // read any errors from the attempted command
                       System.out.println("Here is the standard error of the command (if any):\n");
                       while ((s = stdError.readLine()) != null) {
                           System.out.println(s);
                       }
                }catch(IOException f) {
                    System.out.println("Couldn't run the python script to create clusters. Are you sure that python is installed ?\nIt should run with \"python\" or \"python3\"");
                    System.out.println("You should also have the python packages numpy, pandas and scipy installed (pip install packageName)");
                    System.exit(-1);
                }
            }
            System.out.println("End of clustering algorithm.");
            //create sub-directories
            createClusterDir(this.outputDir,outputCluster);
            //delete temporary files
            //Files.deleteIfExists(Paths.get(inputDataCSV));
            //Files.deleteIfExists(Paths.get(outputCluster, "outputCluster.txt"));
            //Files.deleteIfExists(Paths.get(outputCluster));
        }catch (Exception e){
            System.out.println("error: running clustering algorithm "+e);
        }
    }

}
