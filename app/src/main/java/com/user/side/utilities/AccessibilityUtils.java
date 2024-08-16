package com.user.side.utilities;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityUtils {

    public static List<AccessibilityNodeInfo> getAllNodes(AccessibilityEvent event) {
        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            traverseNode(source, allNodes);
        }
        return allNodes;
    }

    private static void traverseNode(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> nodeList) {
        if (node != null) {
            nodeList.add(node);
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                traverseNode(child, nodeList);
            }
        }
    }
}