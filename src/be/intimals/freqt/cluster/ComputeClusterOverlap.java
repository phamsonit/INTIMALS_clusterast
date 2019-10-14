package be.intimals.freqt.cluster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class ComputeClusterOverlap {
    private  String file1;
    private  String file2;



    public ComputeClusterOverlap(String _file1, String _file2){
        file1 = _file1;
        file2 = _file2;
    }


/*
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
    */

    private void readFile(String fileName, Map< Integer,ArrayList<Integer> > clusters){
        try{

            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String lineTmp;
            while ((lineTmp = br.readLine()) != null){
                String[] line = lineTmp.split(" ");
                for(int i=0; i<line.length; ++i){
                    int clusterID = Integer.valueOf(line[i]);
                    int fileId = i;
                    if(clusters.containsKey(clusterID)){
                        ArrayList<Integer> tmp = new ArrayList<>(clusters.get(clusterID));
                        tmp.add(fileId);
                        clusters.replace(clusterID,tmp);
                    }else{
                        ArrayList<Integer> tmp = new ArrayList<>();
                        tmp.add(fileId);
                        clusters.put(clusterID,tmp);
                    }
                }
            }

        }catch (Exception e){
            System.out.println("error: read file "+e);
        }
    }

    public void run(){
        try{

        }catch (Exception e){
            System.out.println("error: compute overlapping files "+e);
        }

        Map<Integer, ArrayList<Integer>> cluster1 = new HashMap<>();
        Map<Integer, ArrayList<Integer>> cluster2 = new HashMap<>();

        readFile(file1,cluster1);
        readFile(file2,cluster2);

        for(Map.Entry<Integer, ArrayList<Integer>> entry1 : cluster1.entrySet()){
            System.out.println(file1 + " - cluster:"+entry1.getKey() +" #file:"+entry1.getValue().size());
            for(Map.Entry<Integer, ArrayList<Integer>> entry2 : cluster2.entrySet()) {
                System.out.print(file2 + " - cluster: "+entry2.getKey()+" #file:"+entry2.getValue().size()+ " ... ");
                int overlap = 0;
                for(int i=0; i<entry1.getValue().size(); ++i) {
                    for (int j = 0; j < entry2.getValue().size(); j++) {
                        if (entry1.getValue().get(i).equals(entry2.getValue().get(j))) {
                            //System.out.print(entry1.getValue().get(i)+" ");
                            overlap++;
                        }
                    }
                }
                System.out.println("# files overlapping: "+overlap);
            }
        }
    }
}
