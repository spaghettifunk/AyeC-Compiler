// rtl package containing all intermediate instructions
package rtl; 

import java.util.*;

public class Utility 
{
    public static List<AbstractNode> returnAllNodes(AbstractNode node)
    {
        List<AbstractNode> listOfNodes = new ArrayList<AbstractNode>();
        addAllNodes(node, listOfNodes);
        return listOfNodes;
    }

    private static void addAllNodes(AbstractNode node, List<AbstractNode> listOfNodes) 
    {
        if (node != null) 
        {
            listOfNodes.add(node);
            List<AbstractNode> children = node.getChildren();
            if (children != null) 
            {
                for (AbstractNode child: children) {
                    addAllNodes(child, listOfNodes);
                }
            }
        }
    }
}