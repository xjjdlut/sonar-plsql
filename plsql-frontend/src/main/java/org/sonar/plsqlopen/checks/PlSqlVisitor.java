package org.sonar.plsqlopen.checks;

import java.util.List;
import java.util.Set;

import org.sonar.plsqlopen.PlSqlVisitorContext;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;

public class PlSqlVisitor {

    private PlSqlVisitorContext context;

    private Set<AstNodeType> astNodeTypesToVisit;
    
    public void init() {
     // default implementation does nothing
    }

    public void visitFile(AstNode node) {
        // default implementation does nothing
    }

    public void leaveFile(AstNode node) {
        // default implementation does nothing
    }

    public void visitNode(AstNode node) {
        // default implementation does nothing
    }

    public void visitToken(Token token) {
        // default implementation does nothing
    }

    public void leaveNode(AstNode node) {
        // default implementation does nothing
    }
    
    public void subscribeTo(AstNodeType... astNodeTypes) {
        for (AstNodeType type : astNodeTypes) {
            astNodeTypesToVisit.add(type);
        }
    }

    public PlSqlVisitorContext getContext() {
        return context;
    }

    public void scanFile(PlSqlVisitorContext context) {
        this.context = context;
        AstNode tree = context.rootTree();
        if (tree != null) {
            visitFile(tree);
            scanNode(tree, astNodeTypesToVisit);
            leaveFile(tree);
        }
    }

    public void scanNode(AstNode node) {
        scanNode(node, astNodeTypesToVisit);
    }

    private void scanNode(AstNode node, Set<AstNodeType> astNodeTypesToVisit) {
        boolean isSubscribedType = astNodeTypesToVisit.contains(node.getType());

        if (isSubscribedType) {
            visitNode(node);
        }

        List<AstNode> children = node.getChildren();
        if (children.isEmpty()) {
            for (Token token : node.getTokens()) {
                visitToken(token);
            }
        } else {
            for (AstNode child : children) {
                scanNode(child, astNodeTypesToVisit);
            }
        }

        if (isSubscribedType) {
            leaveNode(node);
        }
    }

}
