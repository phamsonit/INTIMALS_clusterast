## Cluster ASTs ##
This tool is used to cluster ASTs based on the similarity of labels of ASTs.
It provides different options to cluster AST as well as create the input data.
In particular, two algorithms, hierarchical and K-Means, were deployed in this tool; two options to create input data: 
1. Keep all labels of the ASTs
2. Keep labels of ASTs according to the given root labels and white labels

In addition, this tool has `--svd` option to compress the input data before doing clustering algorithm. 

### USAGE ###

type `java -jar clusterAST` without parameter to see the usage of this tool.


Note: clusteringAST uses root labels and white labels to limit the labels of ASTs that considered by clustering algorithms. Thus, please be sure that you put the `listRootLabel.txt` and `listWhiteLabel.txt` in the conf sub-directory

### Input ###
A set of ASTs in XML format

### Output ###
a set of sub-directories. Each sub-directory contains a list of ASTs which belong to its cluster.