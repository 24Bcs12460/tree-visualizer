import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

// ══════════════════════════════════════════════════════════════════════════════
//  DESIGN TOKENS
// ══════════════════════════════════════════════════════════════════════════════
class T {
    // Background layers
    static final Color PAGE    = new Color(246, 248, 252);
    static final Color CARD    = Color.WHITE;
    static final Color SIDEBAR = new Color(250, 251, 254);
    static final Color CANVAS  = new Color(241, 245, 249);

    // Borders
    static final Color BORDER  = new Color(224, 229, 239);
    static final Color BORDER2 = new Color(210, 218, 232);

    // Text
    static final Color T1 = new Color(15,  23,  42);   // primary
    static final Color T2 = new Color(71,  85,  105);  // secondary
    static final Color T3 = new Color(148, 163, 184);  // muted

    // Accent (indigo)
    static final Color ACCENT       = new Color(79,  70,  229);
    static final Color ACCENT_HOVER = new Color(67,  56,  202);
    static final Color ACCENT_LIGHT = new Color(238, 242, 255);
    static final Color ACCENT_MID   = new Color(199, 210, 254);

    // Semantic colours
    static final Color SUCCESS = new Color(16,  185, 129);
    static final Color DANGER  = new Color(239, 68,  68);
    static final Color WARNING = new Color(245, 158, 11);
    static final Color INFO    = new Color(14,  165, 233);
    static final Color PINK    = new Color(236, 72,  153);
    static final Color PURPLE  = new Color(139, 92,  246);
    static final Color ORANGE  = new Color(249, 115, 22);

    // Node colours
    static final Color N_DEFAULT = new Color(79,  70,  229);
    static final Color N_CMP     = new Color(245, 158, 11);
    static final Color N_ROT     = new Color(249, 115, 22);
    static final Color N_NEW     = new Color(16,  185, 129);
    static final Color N_PATH    = new Color(236, 72,  153);
    static final Color N_FOUND   = new Color(16,  185, 129);
    static final Color N_RBRED   = new Color(220, 38,  38);
    static final Color N_RBBLK   = new Color(30,  27,  75);

    static final Color EDGE  = new Color(188, 200, 220);
    static final Color GRID  = new Color(218, 226, 240);

    // Fonts
    static Font  f(int style, int size)  { return new Font("Segoe UI", style, size); }
    static Font  fb(int size)            { return f(Font.BOLD, size); }
    static Font  fr(int size)            { return f(Font.PLAIN, size); }
}

// ══════════════════════════════════════════════════════════════════════════════
//  STEP MODEL  (unchanged)
// ══════════════════════════════════════════════════════════════════════════════
class InsertStep {
    String description;
    Set<Integer> compareNodes = new LinkedHashSet<>();
    Set<Integer> rotateNodes  = new LinkedHashSet<>();
    int  newNodeValue  = -1;
    String rotationLabel = null;
    boolean refreshLayout = false;

    InsertStep(String desc) { this.description = desc; }

    static InsertStep compare(int v, int nv, String dir) {
        InsertStep s = new InsertStep("Compare  " + v + (v < nv ? " < " : " > ") + nv + "  →  go " + dir);
        s.compareNodes.add(nv); return s;
    }
    static InsertStep inserted(int v) {
        InsertStep s = new InsertStep("Empty slot found — inserting  " + v + "  as new node");
        s.newNodeValue = v; s.refreshLayout = true; return s;
    }
    static InsertStep rotation(String label, Set<Integer> nodes) {
        InsertStep s = new InsertStep(label); s.rotateNodes.addAll(nodes);
        s.rotationLabel = label; s.refreshLayout = true; return s;
    }
    static InsertStep colorFlip(String desc, Set<Integer> nodes) {
        InsertStep s = new InsertStep(desc); s.rotateNodes.addAll(nodes);
        s.rotationLabel = "Color Flip"; return s;
    }
    static InsertStep msg(String m) { return new InsertStep(m); }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TREE NODE  (unchanged)
// ══════════════════════════════════════════════════════════════════════════════
class TreeNode {
    int value; TreeNode left, right;
    double animX, animY, targetX, targetY;
    boolean animInit = false;
    int avlHeight = 1;
    boolean rbRed = true;
    boolean stepCompare, stepRotate, stepNew, searchPath, found;

    TreeNode(int v) { value = v; }

    int avlBF() {
        return (left==null?0:left.avlHeight) - (right==null?0:right.avlHeight);
    }
    void updateAVLHeight() {
        avlHeight = 1 + Math.max(left==null?0:left.avlHeight, right==null?0:right.avlHeight);
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  BST LOGIC  (unchanged)
// ══════════════════════════════════════════════════════════════════════════════
class BSTLogic {
    TreeNode root;
    void insert(int val, List<InsertStep> steps) {
        if (steps != null) steps.add(InsertStep.msg("Starting BST insertion of  " + val));
        root = ins(root, val, steps);
        if (steps != null) steps.add(InsertStep.msg("Insertion of " + val + " complete"));
    }
    TreeNode ins(TreeNode n, int val, List<InsertStep> steps) {
        if (n == null) { if (steps!=null) steps.add(InsertStep.inserted(val)); return new TreeNode(val); }
        if (val < n.value) { if (steps!=null) steps.add(InsertStep.compare(val,n.value,"LEFT")); n.left  = ins(n.left, val, steps); }
        else if (val > n.value) { if (steps!=null) steps.add(InsertStep.compare(val,n.value,"RIGHT")); n.right = ins(n.right,val,steps); }
        else { if (steps!=null) steps.add(InsertStep.msg(val + " already exists")); }
        return n;
    }
    void delete(int val) { root = del(root, val); }
    TreeNode del(TreeNode n, int val) {
        if (n==null) return null;
        if (val < n.value) n.left = del(n.left,val);
        else if (val > n.value) n.right = del(n.right,val);
        else { if (n.left==null) return n.right; if (n.right==null) return n.left;
               TreeNode m=minNode(n.right); n.value=m.value; n.right=del(n.right,m.value); }
        return n;
    }
    TreeNode minNode(TreeNode n) { while (n.left!=null) n=n.left; return n; }
    List<TreeNode> searchPath(int val) {
        List<TreeNode> p=new ArrayList<>(); TreeNode c=root;
        while (c!=null) { p.add(c); if (val==c.value) break; c=val<c.value?c.left:c.right; }
        return p;
    }
    List<Integer> inorder()    { List<Integer> r=new ArrayList<>(); inOrd(root,r);   return r; }
    List<Integer> preorder()   { List<Integer> r=new ArrayList<>(); preOrd(root,r);  return r; }
    List<Integer> postorder()  { List<Integer> r=new ArrayList<>(); postOrd(root,r); return r; }
    List<Integer> levelorder() {
        List<Integer> r=new ArrayList<>(); if (root==null) return r;
        Queue<TreeNode> q=new LinkedList<>(); q.add(root);
        while (!q.isEmpty()) { TreeNode n=q.poll(); r.add(n.value);
            if (n.left!=null) q.add(n.left); if (n.right!=null) q.add(n.right); }
        return r;
    }
    void inOrd(TreeNode n, List<Integer> r)   { if(n==null)return; inOrd(n.left,r);   r.add(n.value); inOrd(n.right,r); }
    void preOrd(TreeNode n, List<Integer> r)  { if(n==null)return; r.add(n.value); preOrd(n.left,r); preOrd(n.right,r); }
    void postOrd(TreeNode n, List<Integer> r) { if(n==null)return; postOrd(n.left,r); postOrd(n.right,r); r.add(n.value); }
    int height(TreeNode n) { return n==null?0:1+Math.max(height(n.left),height(n.right)); }
    int size(TreeNode n)   { return n==null?0:1+size(n.left)+size(n.right); }
}

// ══════════════════════════════════════════════════════════════════════════════
//  AVL LOGIC  (unchanged)
// ══════════════════════════════════════════════════════════════════════════════
class AVLLogic extends BSTLogic {
    @Override void insert(int val, List<InsertStep> steps) {
        if (steps!=null) steps.add(InsertStep.msg("Starting AVL insertion of  " + val));
        root = avlIns(root, val, steps);
        if (steps!=null) steps.add(InsertStep.msg("AVL insertion complete — tree is balanced"));
    }
    TreeNode avlIns(TreeNode n, int val, List<InsertStep> steps) {
        if (n==null) { if (steps!=null) steps.add(InsertStep.inserted(val)); return new TreeNode(val); }
        if (val < n.value) { if (steps!=null) steps.add(InsertStep.compare(val,n.value,"LEFT")); n.left  = avlIns(n.left, val,steps); }
        else if (val > n.value) { if (steps!=null) steps.add(InsertStep.compare(val,n.value,"RIGHT")); n.right = avlIns(n.right,val,steps); }
        else { if (steps!=null) steps.add(InsertStep.msg(val + " already exists")); return n; }
        n.updateAVLHeight(); int bf=n.avlBF();
        if (bf> 1 && val<n.left.value)  { if (steps!=null) { Set<Integer> s=new LinkedHashSet<>();s.add(n.value);s.add(n.left.value); steps.add(InsertStep.rotation("LL imbalance at "+n.value+" (BF=+"+bf+")  →  Right Rotation ↺",s)); } return rotR(n); }
        if (bf<-1 && val>n.right.value) { if (steps!=null) { Set<Integer> s=new LinkedHashSet<>();s.add(n.value);s.add(n.right.value);steps.add(InsertStep.rotation("RR imbalance at "+n.value+" (BF="+bf+")  →  Left Rotation ↻",s));  } return rotL(n); }
        if (bf> 1 && val>n.left.value)  { if (steps!=null) { Set<Integer> s=new LinkedHashSet<>();s.add(n.value);s.add(n.left.value); steps.add(InsertStep.rotation("LR imbalance at "+n.value+" (BF=+"+bf+")  →  LR Double Rotation ↻↺",s)); } n.left =rotL(n.left);  return rotR(n); }
        if (bf<-1 && val<n.right.value) { if (steps!=null) { Set<Integer> s=new LinkedHashSet<>();s.add(n.value);s.add(n.right.value);steps.add(InsertStep.rotation("RL imbalance at "+n.value+" (BF="+bf+")  →  RL Double Rotation ↺↻",s)); } n.right=rotR(n.right); return rotL(n); }
        return n;
    }
    TreeNode rotR(TreeNode y) { TreeNode x=y.left,t=x.right; x.right=y; y.left=t; y.updateAVLHeight(); x.updateAVLHeight(); return x; }
    TreeNode rotL(TreeNode x) { TreeNode y=x.right,t=y.left; y.left=x; x.right=t; x.updateAVLHeight(); y.updateAVLHeight(); return y; }
    @Override TreeNode del(TreeNode n, int val) {
        n=super.del(n,val); if (n==null) return null; n.updateAVLHeight(); int bf=n.avlBF();
        if (bf> 1 && n.left.avlBF() >=0) return rotR(n);
        if (bf> 1 && n.left.avlBF() < 0) { n.left =rotL(n.left);  return rotR(n); }
        if (bf<-1 && n.right.avlBF()<=0) return rotL(n);
        if (bf<-1 && n.right.avlBF()> 0) { n.right=rotR(n.right); return rotL(n); }
        return n;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  RED-BLACK LOGIC  — LLRB (unchanged)
// ══════════════════════════════════════════════════════════════════════════════
class RBLogic extends BSTLogic {
    boolean red(TreeNode n) { return n!=null&&n.rbRed; }
    TreeNode rotL(TreeNode h) { TreeNode x=h.right;h.right=x.left;x.left=h;x.rbRed=h.rbRed;h.rbRed=true;return x; }
    TreeNode rotR(TreeNode h) { TreeNode x=h.left;h.left=x.right;x.right=h;x.rbRed=h.rbRed;h.rbRed=true;return x; }
    void flip(TreeNode h) { h.rbRed=!h.rbRed; if(h.left!=null)h.left.rbRed=!h.left.rbRed; if(h.right!=null)h.right.rbRed=!h.right.rbRed; }
    @Override void insert(int val, List<InsertStep> steps) {
        if (steps!=null) steps.add(InsertStep.msg("Starting Red-Black insertion of  " + val));
        root=rbIns(root,val,steps); root.rbRed=false;
        if (steps!=null) steps.add(InsertStep.msg("RB insertion complete — root set BLACK"));
    }
    TreeNode rbIns(TreeNode h, int val, List<InsertStep> steps) {
        if (h==null) { TreeNode n=new TreeNode(val);n.rbRed=true;
            if (steps!=null){InsertStep s=InsertStep.inserted(val);s.description="Insert "+val+" as RED node";steps.add(s);} return n; }
        if (val<h.value){if(steps!=null)steps.add(InsertStep.compare(val,h.value,"LEFT")); h.left =rbIns(h.left, val,steps);}
        else if (val>h.value){if(steps!=null)steps.add(InsertStep.compare(val,h.value,"RIGHT"));h.right=rbIns(h.right,val,steps);}
        else{if(steps!=null)steps.add(InsertStep.msg(val+" already exists"));return h;}
        if (red(h.right)&&!red(h.left)){if(steps!=null){Set<Integer>s=new LinkedHashSet<>();s.add(h.value);if(h.right!=null)s.add(h.right.value);steps.add(InsertStep.rotation("Right-leaning RED at "+h.value+"  →  Left Rotation ↻",s));}h=rotL(h);}
        if (red(h.left)&&red(h.left!=null?h.left.left:null)){if(steps!=null){Set<Integer>s=new LinkedHashSet<>();s.add(h.value);if(h.left!=null)s.add(h.left.value);steps.add(InsertStep.rotation("Two consecutive RED links at "+h.value+"  →  Right Rotation ↺",s));}h=rotR(h);}
        if (red(h.left)&&red(h.right)){if(steps!=null){Set<Integer>s=new LinkedHashSet<>();s.add(h.value);if(h.left!=null)s.add(h.left.value);if(h.right!=null)s.add(h.right.value);steps.add(InsertStep.colorFlip("Both children RED at "+h.value+"  →  Color Flip",s));}flip(h);}
        return h;
    }
    @Override void delete(int val) {
        if (root==null)return; if(!red(root.left)&&!red(root.right))root.rbRed=true;
        root=rbDel(root,val); if(root!=null)root.rbRed=false;
    }
    TreeNode rbDel(TreeNode h,int val){
        if(h==null)return null;
        if(val<h.value){if(h.left==null)return h;if(!red(h.left)&&!red(h.left.left))h=mvL(h);h.left=rbDel(h.left,val);}
        else{if(red(h.left))h=rotR(h);if(val==h.value&&h.right==null)return null;
             if(h.right!=null&&!red(h.right)&&!red(h.right.left))h=mvR(h);
             if(val==h.value){TreeNode m=minNode(h.right);h.value=m.value;h.right=dMin(h.right);}
             else if(h.right!=null)h.right=rbDel(h.right,val);}
        return rbBal(h);
    }
    TreeNode mvL(TreeNode h){flip(h);if(red(h.right!=null?h.right.left:null)){h.right=rotR(h.right);h=rotL(h);flip(h);}return h;}
    TreeNode mvR(TreeNode h){flip(h);if(red(h.left!=null?h.left.left:null)){h=rotR(h);flip(h);}return h;}
    TreeNode dMin(TreeNode h){if(h.left==null)return null;if(!red(h.left)&&!red(h.left.left))h=mvL(h);h.left=dMin(h.left);return rbBal(h);}
    TreeNode rbBal(TreeNode h){if(red(h.right))h=rotL(h);if(red(h.left)&&red(h.left!=null?h.left.left:null))h=rotR(h);if(red(h.left)&&red(h.right))flip(h);return h;}
    int blackHeight(TreeNode n){return n==null?0:(n.rbRed?0:1)+blackHeight(n.left);}
}

// ══════════════════════════════════════════════════════════════════════════════
//  TREE CANVAS  — light theme
// ══════════════════════════════════════════════════════════════════════════════
class TreeCanvas extends JPanel {
    static final int R    = 24;
    static final int VGAP = 80;

    BSTLogic logic;
    String   mode = "BST";

    private Timer  animTimer;
    private float  animProg = 1f;

    Set<Integer> searchVals = new HashSet<>();
    int foundVal = -1;
    Set<Integer> cmpVals = new LinkedHashSet<>();
    Set<Integer> rotVals = new LinkedHashSet<>();
    int    newVal   = -1;
    String rotLabel = null;

    int insertingVal = -1;
    boolean showInsertingVal = true;
    int travHighlightVal = -1;
    Color travHighlightColor = null;

    double offX=0, offY=20, zoom=1.0;
    Point  lastDrag;

    TreeCanvas(BSTLogic l) {
        logic = l;
        setBackground(T.CANVAS);
        setPreferredSize(new Dimension(880, 560));

        addMouseWheelListener(e -> {
            zoom = Math.max(0.25, Math.min(zoom * (e.getWheelRotation()<0?1.1:0.9), 3.0));
            repaint();
        });
        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { lastDrag = e.getPoint(); }
            public void mouseDragged(MouseEvent e)  {
                if (lastDrag!=null) { offX+=e.getX()-lastDrag.x; offY+=e.getY()-lastDrag.y; lastDrag=e.getPoint(); repaint(); }
            }
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    offX = 0; offY = 20; zoom = 1.0;
                    repaint();
                }
            }
        };
        addMouseListener(ma); addMouseMotionListener(ma);

        animTimer = new Timer(14, e -> { if (animProg<1f) { animProg=Math.min(1f,animProg+0.08f); repaint(); } });
        animTimer.start();
    }

    void setTree(BSTLogic l, String m) { logic=l; mode=m; clearAll(); refreshLayout(); }

    void refreshLayout() { animProg=0f; repaint(); }

    void assignPos(TreeNode n, int[] cnt, int depth) {
        if (n==null) return;
        assignPos(n.left, cnt, depth+1);
        if (!n.animInit) { n.animX=getWidth()/2.0; n.animY=80; n.animInit=true; }
        n.targetX=cnt[0]; n.targetY=80+depth*VGAP; cnt[0]++;
        assignPos(n.right, cnt, depth+1);
    }
    void scaleX(TreeNode n, int total) {
        if (n==null) return;
        double m=60, w=Math.max(80,getWidth()-2*m);
        n.targetX = total>1 ? m+(n.targetX/(total-1))*w : getWidth()/2.0;
        scaleX(n.left,total); scaleX(n.right,total);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        drawDotGrid(g2);
        drawHints(g2);

        if (logic.root==null) { drawEmpty(g2); return; }

        int[] cnt={0};
        assignPos(logic.root,cnt,0);
        scaleX(logic.root,cnt[0]);

        g2.translate(offX,offY);
        g2.scale(zoom,zoom);

        lerp(logic.root);
        drawEdges(g2,logic.root);
        drawNodes(g2,logic.root);
        if (rotLabel!=null&&!rotVals.isEmpty()) drawRotLabel(g2);
    }

    void lerp(TreeNode n) {
        if (n==null) return;
        float e=ease(animProg);
        n.animX+=(n.targetX-n.animX)*e; n.animY+=(n.targetY-n.animY)*e;
        lerp(n.left); lerp(n.right);
    }
    float ease(float t) { return t<0.5f?2*t*t:-1+(4-2*t)*t; }

    void drawDotGrid(Graphics2D g2) {
        g2.setColor(T.GRID);
        int step=30;
        for (int x=0;x<getWidth();x+=step)
            for (int y=0;y<getHeight();y+=step)
                g2.fillOval(x-1,y-1,2,2);
    }

    void drawHints(Graphics2D g2) {
        g2.setFont(T.fr(11));
        g2.setColor(T.T3);
        String h="Scroll to zoom  ·  Drag to pan  ·  Double-click to reset";
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(h, getWidth()-fm.stringWidth(h)-12, getHeight()-10);
    }

    void drawEmpty(Graphics2D g2) {
        // Centered placeholder card
        String line1="Your tree will appear here";
        String line2="Enter a value on the left and click Insert";
        g2.setFont(T.fb(17)); g2.setColor(T.T2);
        FontMetrics fm1=g2.getFontMetrics();
        g2.setFont(T.fr(13)); g2.setColor(T.T3);
        FontMetrics fm2=g2.getFontMetrics();
        int cx=getWidth()/2, cy=getHeight()/2;
        g2.setFont(T.fb(17)); g2.setColor(T.T2);
        g2.drawString(line1, cx-fm1.stringWidth(line1)/2, cy-8);
        g2.setFont(T.fr(13)); g2.setColor(T.T3);
        g2.drawString(line2, cx-fm2.stringWidth(line2)/2, cy+16);
    }

    void drawEdges(Graphics2D g2, TreeNode n) {
        if (n==null) return;
        if (n.left !=null){
            if (n.left.value != insertingVal || showInsertingVal) {
                drawEdge(g2,n,n.left);
            }
            drawEdges(g2,n.left);
        }
        if (n.right!=null){
            if (n.right.value != insertingVal || showInsertingVal) {
                drawEdge(g2,n,n.right);
            }
            drawEdges(g2,n.right);
        }
    }
    void drawEdge(Graphics2D g2, TreeNode f, TreeNode t) {
        boolean onPath = searchVals.contains(f.value)&&searchVals.contains(t.value);
        boolean rbRed  = "RB".equals(mode)&&t.rbRed;
        Color c = onPath ? T.N_PATH : rbRed ? new Color(T.N_RBRED.getRed(),T.N_RBRED.getGreen(),T.N_RBRED.getBlue(),160) : T.EDGE;
        g2.setColor(c);
        g2.setStroke(new BasicStroke(onPath?2.5f:rbRed?2f:1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.drawLine((int)f.animX,(int)f.animY,(int)t.animX,(int)t.animY);
    }

    void drawNodes(Graphics2D g2, TreeNode n) {
        if (n==null) return;
        drawNodes(g2,n.left); drawNodes(g2,n.right);
        if (n.value != insertingVal || showInsertingVal) {
            drawNode(g2,n);
        }
    }
    void drawNode(Graphics2D g2, TreeNode n) {
        int cx=(int)n.animX, cy=(int)n.animY;
        Color fill = nodeColor(n);

        // Drop shadow
        g2.setColor(new Color(fill.getRed(),fill.getGreen(),fill.getBlue(),30));
        g2.fillOval(cx-R-2,cy-R+3,(R+2)*2,(R+2)*2);
        g2.setColor(new Color(0,0,0,18));
        g2.fillOval(cx-R+1,cy-R+5,R*2,R*2);

        // Fill
        g2.setColor(fill);
        g2.fillOval(cx-R,cy-R,R*2,R*2);

        // Inner highlight
        GradientPaint shine = new GradientPaint(cx-R/2f,cy-R,
            new Color(255,255,255,55), cx,cy+R, new Color(255,255,255,0));
        g2.setPaint(shine); g2.fillOval(cx-R,cy-R,R*2,R*2);
        g2.setPaint(null);

        // Border
        Color border = rotVals.contains(n.value)||cmpVals.contains(n.value)||n.value==newVal||n.value==foundVal
            ? fill.darker() : new Color(0,0,0,25);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx-R,cy-R,R*2,R*2);

        // RB outer ring
        if ("RB".equals(mode)) {
            g2.setColor(n.rbRed ? T.N_RBRED : new Color(100,100,140));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(cx-R-4,cy-R-4,(R+4)*2,(R+4)*2);
        }

        // Value text
        g2.setColor(Color.WHITE);
        g2.setFont(T.fb(13));
        FontMetrics fm=g2.getFontMetrics();
        String vs=String.valueOf(n.value);
        g2.drawString(vs, cx-fm.stringWidth(vs)/2, cy+fm.getAscent()/2-1);

        // AVL: BF badge
        if ("AVL".equals(mode)) {
            int bf=n.avlBF();
            String bfStr=(bf>0?"+":"")+bf;
            Color bfFg = Math.abs(bf)>1?T.DANGER : Math.abs(bf)==1?T.WARNING : new Color(5,150,105);
            Color bfBg = Math.abs(bf)>1?new Color(254,226,226) : Math.abs(bf)==1?new Color(254,243,199) : new Color(209,250,229);
            g2.setFont(T.fb(9));
            FontMetrics bfm=g2.getFontMetrics();
            int bw=bfm.stringWidth(bfStr)+8, bh=14;
            int bx=cx-bw/2, by=cy+R+3;
            g2.setColor(bfBg); g2.fillRoundRect(bx,by,bw,bh,6,6);
            g2.setColor(bfFg); g2.drawString(bfStr,bx+4,by+bh-3);
        }

        // RB: R/B badge
        if ("RB".equals(mode)) {
            String rbs=n.rbRed?"R":"B";
            Color rbFg=n.rbRed?T.N_RBRED:new Color(60,60,110);
            Color rbBg=n.rbRed?new Color(254,226,226):new Color(224,231,255);
            g2.setFont(T.fb(9));
            FontMetrics rm=g2.getFontMetrics();
            int rw=rm.stringWidth(rbs)+8, rh=14;
            int rx=cx-rw/2, ry=cy+R+4;
            g2.setColor(rbBg); g2.fillRoundRect(rx,ry,rw,rh,6,6);
            g2.setColor(rbFg); g2.drawString(rbs,rx+4,ry+rh-3);
        }
    }

    Color nodeColor(TreeNode n) {
        if (n.value==newVal)             return T.N_NEW;
        if (rotVals.contains(n.value))   return T.N_ROT;
        if (cmpVals.contains(n.value))   return T.N_CMP;
        if (n.value==foundVal)           return T.N_FOUND;
        if (searchVals.contains(n.value))return T.N_PATH;
        if (n.value==travHighlightVal && travHighlightColor != null) return travHighlightColor;
        if ("RB".equals(mode))           return n.rbRed?T.N_RBRED:T.N_RBBLK;
        return T.N_DEFAULT;
    }

    void drawRotLabel(Graphics2D g2) {
        TreeNode piv=findFirst(logic.root,rotVals);
        if (piv==null) return;
        String txt = rotLabel != null ? rotLabel : "Rotation";
        int x=(int)piv.animX-10, y=(int)piv.animY-R-16;
        g2.setFont(T.fb(11));
        FontMetrics fm=g2.getFontMetrics();
        int w=fm.stringWidth(txt)+18, h=22;
        // Badge
        g2.setColor(new Color(T.N_ROT.getRed(),T.N_ROT.getGreen(),T.N_ROT.getBlue(),230));
        g2.fillRoundRect(x,y-h,w,h,8,8);
        g2.setColor(Color.WHITE);
        g2.drawString(txt,x+9,y-5);
    }
    TreeNode findFirst(TreeNode n, Set<Integer> vals) {
        if (n==null||vals.isEmpty()) return null;
        if (vals.contains(n.value)) return n;
        TreeNode l=findFirst(n.left,vals); return l!=null?l:findFirst(n.right,vals);
    }

    void applyStep(InsertStep step) {
        cmpVals.clear(); rotVals.clear(); newVal=-1; rotLabel=null;
        cmpVals.addAll(step.compareNodes);
        rotVals.addAll(step.rotateNodes);
        newVal=step.newNodeValue; rotLabel=step.rotationLabel;
        if (step.refreshLayout) refreshLayout(); else repaint();
    }
    void clearAll() {
        searchVals.clear(); foundVal=-1;
        cmpVals.clear(); rotVals.clear(); newVal=-1; rotLabel=null;
        insertingVal=-1; showInsertingVal=true;
        travHighlightVal=-1; travHighlightColor=null;
        repaint();
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SEGMENTED CONTROL  (mode switcher)
// ══════════════════════════════════════════════════════════════════════════════
class SegmentedControl extends JPanel {
    String[] labels;
    String   selected;
    Consumer<String> onChange;

    SegmentedControl(String[] labels, String def, Consumer<String> cb) {
        this.labels=labels; this.selected=def; this.onChange=cb;
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT,4,0));
        setPreferredSize(new Dimension(280,34));
        rebuild();
    }
    void rebuild() {
        removeAll();
        for (String lbl : labels) {
            boolean sel=lbl.equals(selected);
            JButton b = new JButton(lbl) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2=(Graphics2D)g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    if (sel) {
                        g2.setColor(T.ACCENT_LIGHT);
                        g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                        g2.setColor(T.ACCENT);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                    } else {
                        g2.setColor(new Color(240,242,248));
                        g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                        g2.setColor(T.BORDER);
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                    }
                    super.paintComponent(g);
                }
            };
            b.setFont(T.fb(12));
            b.setForeground(sel?T.ACCENT:T.T2);
            b.setFocusPainted(false); b.setBorderPainted(false); b.setContentAreaFilled(false);
            b.setBorder(BorderFactory.createEmptyBorder(6,16,6,16));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> { selected=lbl; rebuild(); if (onChange!=null) onChange.accept(lbl); revalidate(); repaint(); });
            add(b);
        }
        revalidate(); repaint();
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  CLEAN BUTTON
// ══════════════════════════════════════════════════════════════════════════════
class CBtn extends JButton {
    Color base, hov;
    boolean over=false;
    boolean outline=false;

    CBtn(String t, Color c) {
        super(t); base=c; hov=c.darker();
        setForeground(Color.WHITE);
        setFont(T.fb(13));
        setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){over=true;repaint();}
            public void mouseExited(MouseEvent e){over=false;repaint();}
        });
    }
    CBtn(String t, Color c, boolean outline) {
        this(t,c); this.outline=outline;
        if (outline) setForeground(c);
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2=(Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = outline?(over?new Color(base.getRed(),base.getGreen(),base.getBlue(),18):Color.WHITE):(over?hov:base);
        g2.setColor(fill); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
        if (outline) { g2.setColor(over?base:new Color(base.getRed(),base.getGreen(),base.getBlue(),160)); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8); }
        super.paintComponent(g);
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  INPUT FIELD
// ══════════════════════════════════════════════════════════════════════════════
class CField extends JTextField {
    private String placeholder;
    CField(String placeholder, int cols) {
        super(cols);
        this.placeholder = placeholder;
        setFont(T.fr(14)); setForeground(T.T1); setBackground(T.CARD);
        setCaretColor(T.ACCENT);
        setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(T.BORDER,1,true),
            BorderFactory.createEmptyBorder(8,12,8,12)
        ));
        addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e) { setBorder(BorderFactory.createCompoundBorder(new LineBorder(T.ACCENT,2,true),BorderFactory.createEmptyBorder(7,11,7,11))); repaint(); }
            public void focusLost(FocusEvent e)   { Border border = getBorder(); setBorder(BorderFactory.createCompoundBorder(new LineBorder(T.BORDER,1,true), BorderFactory.createEmptyBorder(8,12,8,12))); repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (placeholder != null && getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(T.T3);
            g2.setFont(getFont());
            Insets insets = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            int x = insets.left;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, x, y);
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  STATS PANEL
// ══════════════════════════════════════════════════════════════════════════════
class StatsPanel extends JPanel {
    JLabel hLbl, nLbl, infoLbl;

    StatsPanel() {
        setBackground(T.CARD);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,T.BORDER),
            BorderFactory.createEmptyBorder(14,20,14,20)
        ));
        setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
        hLbl    = addStat("Tree Height", "—", T.ACCENT);
        addDiv();
        nLbl    = addStat("Total Nodes",  "—", T.SUCCESS);
        addDiv();
        infoLbl = addStat("Balance",      "—", T.WARNING);
    }

    JLabel addStat(String label, String val, Color accent) {
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(T.fb(9)); lbl.setForeground(T.T3);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel vl = new JLabel(val);
        vl.setFont(T.fb(22)); vl.setForeground(accent);
        vl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lbl); card.add(vl);
        add(card);
        return vl;
    }
    void addDiv() {
        JPanel d=new JPanel(); d.setPreferredSize(new Dimension(1,44)); d.setBackground(T.BORDER); add(d);
    }

    void update(BSTLogic logic, String mode) {
        int h=logic.height(logic.root), n=logic.size(logic.root);
        String info;
        switch (mode) {
            case "AVL": info="Balanced ✓"; break;
            case "RB":  info="BH: "+((logic instanceof RBLogic)?((RBLogic)logic).blackHeight(logic.root):0); break;
            default:    info=isBalanced(logic.root)?"Balanced ✓":"Unbalanced"; break;
        }
        hLbl.setText(String.valueOf(h));
        nLbl.setText(String.valueOf(n));
        infoLbl.setText(info);
    }
    boolean isBalanced(TreeNode n){if(n==null)return true;int l=ht(n.left),r=ht(n.right);return Math.abs(l-r)<=1&&isBalanced(n.left)&&isBalanced(n.right);}
    int ht(TreeNode n){return n==null?0:1+Math.max(ht(n.left),ht(n.right));}
}

// ══════════════════════════════════════════════════════════════════════════════
//  STEP CONTROL PANEL
// ══════════════════════════════════════════════════════════════════════════════
class StepPanel extends JPanel {
    JLabel cntLbl, descLbl;
    JButton prevBtn, playBtn, nextBtn;
    JSlider speed;
    Timer   auto;
    List<InsertStep> steps;
    int idx=-1;
    int insertIdx=-1;
    BiConsumer<InsertStep, Boolean> onStep;
    boolean playing=false;

    StepPanel() {
        setBackground(T.CARD);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,T.BORDER),
            BorderFactory.createEmptyBorder(12,20,12,20)
        ));
        setLayout(new BorderLayout(12,8));

        // Left: description
        JPanel left=new JPanel(new BorderLayout(0,4)); left.setOpaque(false);
        JLabel badge=new JLabel("  STEP MODE  ");
        badge.setFont(T.fb(9)); badge.setForeground(T.ACCENT); badge.setOpaque(true); badge.setBackground(T.ACCENT_LIGHT);
        badge.setBorder(BorderFactory.createCompoundBorder(new LineBorder(T.ACCENT_MID,1,true),BorderFactory.createEmptyBorder(2,6,2,6)));
        cntLbl=new JLabel("No active steps"); cntLbl.setFont(T.fr(12)); cntLbl.setForeground(T.T3);
        JPanel topRow=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); topRow.setOpaque(false);
        topRow.add(badge); topRow.add(cntLbl);
        descLbl=new JLabel("Enable step mode and insert a value");
        descLbl.setFont(T.fr(13)); descLbl.setForeground(T.T2);
        left.add(topRow,BorderLayout.NORTH); left.add(descLbl,BorderLayout.CENTER);

        // Right: controls
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); right.setOpaque(false);
        prevBtn=ctlBtn("← Prev");  playBtn=ctlBtn("▶ Play"); nextBtn=ctlBtn("Next →");
        prevBtn.setBackground(new Color(240,242,248)); prevBtn.setForeground(T.T2);
        nextBtn.setBackground(new Color(240,242,248)); nextBtn.setForeground(T.T2);
        playBtn.setBackground(T.ACCENT); playBtn.setForeground(Color.WHITE);
        JLabel sl=new JLabel("Speed"); sl.setFont(T.fr(11)); sl.setForeground(T.T3);
        speed=new JSlider(150,1800,700); speed.setInverted(true);
        speed.setPreferredSize(new Dimension(90,20)); speed.setBackground(T.CARD);
        speed.addChangeListener(e->{if(auto.isRunning())auto.setDelay(speed.getValue());});
        prevBtn.addActionListener(e->back()); playBtn.addActionListener(e->togglePlay()); nextBtn.addActionListener(e->fwd());
        right.add(prevBtn); right.add(playBtn); right.add(nextBtn); right.add(sl); right.add(speed);

        add(left,BorderLayout.CENTER); add(right,BorderLayout.EAST);
        auto=new Timer(700,e->fwd()); updateBtns();
    }

    JButton ctlBtn(String t) {
        JButton b=new JButton(t); b.setFont(T.fb(12));
        b.setBorder(BorderFactory.createCompoundBorder(new LineBorder(T.BORDER,1,true),BorderFactory.createEmptyBorder(6,14,6,14)));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }

    void load(List<InsertStep> s, BiConsumer<InsertStep, Boolean> cb) {
        if (auto.isRunning()){auto.stop();playing=false;playBtn.setText("▶ Play");playBtn.setBackground(T.ACCENT);}
        steps=s; onStep=cb;
        insertIdx = -1;
        if (steps != null) {
            for (int i=0; i<steps.size(); i++) {
                if (steps.get(i).newNodeValue != -1) {
                    insertIdx = i;
                    break;
                }
            }
        }
        idx=0; apply(0); updateBtns();
    }
    void fwd()  {if(steps==null||idx>=steps.size()-1){if(playing){auto.stop();playing=false;playBtn.setText("▶ Play");playBtn.setBackground(T.ACCENT);}return;}apply(++idx);updateBtns();}
    void back() {if(steps==null||idx<=0)return;apply(--idx);updateBtns();}
    void togglePlay() {
        if(steps==null)return; playing=!playing;
        if(playing){playBtn.setText("⏸ Pause");playBtn.setBackground(new Color(100,100,120));auto.setDelay(speed.getValue());auto.start();}
        else{playBtn.setText("▶ Play");playBtn.setBackground(T.ACCENT);auto.stop();}
    }
    void apply(int i) {
        if(steps==null||i<0||i>=steps.size())return;
        cntLbl.setText("Step "+(i+1)+" of "+steps.size());
        descLbl.setText(steps.get(i).description);
        if(onStep!=null) onStep.accept(steps.get(i), insertIdx == -1 || i >= insertIdx);
    }
    void updateBtns(){boolean h=steps!=null&&!steps.isEmpty();prevBtn.setEnabled(h&&idx>0);nextBtn.setEnabled(h&&idx<steps.size()-1);playBtn.setEnabled(h);}
    void clear(){if(auto.isRunning()){auto.stop();playing=false;playBtn.setText("▶ Play");playBtn.setBackground(T.ACCENT);}steps=null;idx=-1;insertIdx=-1;cntLbl.setText("No active steps");descLbl.setText("Enable step mode and insert a value");updateBtns();}
}

// ══════════════════════════════════════════════════════════════════════════════
//  TRAVERSAL PANEL
// ══════════════════════════════════════════════════════════════════════════════
class TravPanel extends JPanel {
    JLabel    titleLbl;
    JPanel    row;
    List<JLabel> chips=new ArrayList<>();
    static final Color[] TC={T.ACCENT,T.WARNING,T.INFO,T.PINK};
    static final String[] TN={"Inorder","Preorder","Postorder","Level Order"};
    Timer activeTimer;
    TreeCanvas canvas;

    TravPanel(TreeCanvas canvas) {
        this.canvas = canvas;
        setBackground(T.CARD);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,T.BORDER),
            BorderFactory.createEmptyBorder(12,20,12,20)
        ));
        setLayout(new BorderLayout(0,6));
        titleLbl=new JLabel("Traversal Result");
        titleLbl.setFont(T.fb(12)); titleLbl.setForeground(T.T2);
        row=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); row.setOpaque(false);
        add(titleLbl,BorderLayout.NORTH); add(row,BorderLayout.CENTER);
    }

    void show(String type, List<Integer> order) {
        if(activeTimer!=null){activeTimer.stop();activeTimer=null;}
        if(canvas!=null){canvas.travHighlightVal=-1;canvas.travHighlightColor=null;canvas.repaint();}
        titleLbl.setText(type + " Traversal");
        row.removeAll(); chips.clear();
        Color tc=TC[0]; for(int i=0;i<TN.length;i++) if(TN[i].equals(type)) tc=TC[i];
        final Color fc=tc;
        for (int i=0;i<order.size();i++) {
            JLabel l=new JLabel(String.valueOf(order.get(i)));
            l.setFont(T.fb(12)); l.setOpaque(true);
            l.setBackground(T.PAGE); l.setForeground(T.T2);
            l.setBorder(BorderFactory.createCompoundBorder(new LineBorder(T.BORDER,1,true),BorderFactory.createEmptyBorder(4,10,4,10)));
            chips.add(l); row.add(l);
            if (i<order.size()-1) {
                JLabel ar=new JLabel("›"); ar.setFont(T.fb(12)); ar.setForeground(T.T3); row.add(ar);
            }
        }
        row.revalidate(); row.repaint();
        activeTimer=new Timer(220,null); int[] idx={0};
        activeTimer.addActionListener(e->{
            if(idx[0]<chips.size()){
                int val = order.get(idx[0]);
                if(canvas!=null){
                    canvas.travHighlightVal=val;
                    canvas.travHighlightColor=fc;
                    canvas.repaint();
                }
                chips.get(idx[0]).setBackground(new Color(fc.getRed(),fc.getGreen(),fc.getBlue(),20));
                chips.get(idx[0]).setForeground(fc);
                chips.get(idx[0]).setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(fc.getRed(),fc.getGreen(),fc.getBlue(),80),1,true),BorderFactory.createEmptyBorder(4,10,4,10)));
                idx[0]++;
            } else {
                if(canvas!=null){
                    canvas.travHighlightVal=-1;
                    canvas.travHighlightColor=null;
                    canvas.repaint();
                }
                ((Timer)e.getSource()).stop();
            }
        });
        activeTimer.start();
    }
    void clear(){
        if(activeTimer!=null){activeTimer.stop();activeTimer=null;}
        if(canvas!=null){canvas.travHighlightVal=-1;canvas.travHighlightColor=null;canvas.repaint();}
        titleLbl.setText("Traversal Result");row.removeAll();row.revalidate();row.repaint();
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SIDEBAR SECTION
// ══════════════════════════════════════════════════════════════════════════════
class SideSection extends JPanel {
    SideSection(String title) {
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0,0,4,0));
        if (title!=null) {
            JLabel lbl=new JLabel(title);
            lbl.setFont(T.fb(10)); lbl.setForeground(T.T3);
            lbl.setBorder(BorderFactory.createEmptyBorder(0,0,6,0));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(lbl);
        }
    }
    void addBtn(CBtn b) { b.setAlignmentX(Component.LEFT_ALIGNMENT); b.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); add(b); add(Box.createVerticalStrut(5)); }
}

// ══════════════════════════════════════════════════════════════════════════════
//  MAIN WINDOW
// ══════════════════════════════════════════════════════════════════════════════
public class BSTVisualizer extends JFrame {

    final BSTLogic bstL = new BSTLogic();
    final AVLLogic avlL = new AVLLogic();
    final RBLogic  rbL  = new RBLogic();
    BSTLogic cur = bstL;
    String   mode = "BST";

    TreeCanvas canvas;
    StatsPanel stats;
    TravPanel  travPanel;
    StepPanel  stepPanel;
    JLabel     statusLbl;
    CField     inputFld;
    JCheckBox  stepCheck;
    JPanel     bottomP;
    CardLayout bottomCL;
    SegmentedControl modeCtrl;
    Timer      searchTimer;

    public BSTVisualizer() {
        setTitle("Tree Visualizer — BST · AVL · Red-Black");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 820);
        setMinimumSize(new Dimension(980, 660));
        setLocationRelativeTo(null);
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ig) {}
        buildUI(); seed(); setVisible(true);
    }

    void buildUI() {
        JPanel root=new JPanel(new BorderLayout());
        root.setBackground(T.PAGE);
        setContentPane(root);
        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildSidebar(),   BorderLayout.WEST);
        root.add(buildMain(),      BorderLayout.CENTER);
    }

    // ── Header ───────────────────────────────────────────────────────────────
    JPanel buildHeader() {
        JPanel hdr=new JPanel(new BorderLayout());
        hdr.setBackground(T.CARD);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,T.BORDER),
            BorderFactory.createEmptyBorder(0,24,0,24)
        ));
        hdr.setPreferredSize(new Dimension(0,64));

        // Left: logo + title
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));
        left.setOpaque(false);
        // Logo badge
        JLabel logo=new JLabel("BST") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(T.ACCENT); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                super.paintComponent(g);
            }
        };
        logo.setForeground(Color.WHITE); logo.setFont(T.fb(13));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(40,32));
        JPanel titleBox=new JPanel(); titleBox.setOpaque(false); titleBox.setLayout(new BoxLayout(titleBox,BoxLayout.Y_AXIS));
        JLabel title=new JLabel("Tree Visualizer"); title.setFont(T.fb(18)); title.setForeground(T.T1);
        JLabel sub=new JLabel("Binary Search Tree  ·  AVL Tree  ·  Red-Black Tree"); sub.setFont(T.fr(12)); sub.setForeground(T.T3);
        titleBox.add(title); titleBox.add(sub);
        left.add(logo); left.add(titleBox);

        // Right: mode selector
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,16,0)); right.setOpaque(false);
        JLabel ml=new JLabel("Mode"); ml.setFont(T.fr(12)); ml.setForeground(T.T3);
        modeCtrl=new SegmentedControl(new String[]{"BST","AVL","Red-Black"},"BST",this::switchMode);
        right.add(ml); right.add(modeCtrl);

        hdr.add(left,  BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    JPanel buildSidebar() {
        JPanel sb=new JPanel();
        sb.setBackground(T.SIDEBAR);
        sb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,0,1,T.BORDER),
            BorderFactory.createEmptyBorder(24,16,24,16)
        ));
        sb.setLayout(new BoxLayout(sb,BoxLayout.Y_AXIS));
        sb.setPreferredSize(new Dimension(230,0));

        // Value input
        SideSection inputSec=new SideSection("INSERT / DELETE / SEARCH");
        inputFld=new CField("Enter value…",8);
        inputFld.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        inputFld.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputFld.addActionListener(e->doInsert());
        inputSec.add(inputFld); inputSec.add(Box.createVerticalStrut(8));

        CBtn insBtn=new CBtn("Insert",     T.ACCENT);
        CBtn delBtn=new CBtn("Delete",     T.DANGER);
        CBtn schBtn=new CBtn("Search",     T.SUCCESS);
        CBtn clrBtn=new CBtn("Clear Tree", T.T3);
        insBtn.addActionListener(e->doInsert()); delBtn.addActionListener(e->doDelete());
        schBtn.addActionListener(e->doSearch()); clrBtn.addActionListener(e->doClear());
        inputSec.addBtn(insBtn); inputSec.addBtn(delBtn); inputSec.addBtn(schBtn); inputSec.addBtn(clrBtn);

        // Traversals
        SideSection travSec=new SideSection("TRAVERSALS");
        String[][] travs = {{"Inorder","Inorder"},{"Preorder","Preorder"},{"Postorder","Postorder"},{"Level Order","Level Order"}};
        Color[] tc={T.ACCENT,T.WARNING,T.INFO,T.PINK};
        for (int i=0;i<travs.length;i++){
            final String n=travs[i][0]; CBtn b=new CBtn(n,tc[i],true);
            b.addActionListener(e->doTraversal(n)); travSec.addBtn(b);
        }

        // Presets
        SideSection presSec=new SideSection("PRESETS");
        String[][] ps={{"Balanced Tree","50,25,75,12,37,62,87"},{"Right-Skewed","10,20,30,40,50"},{"Random (12 nodes)",null}};
        Color[] pc={new Color(20,184,166),T.PURPLE,T.ORANGE};
        for (int i=0;i<ps.length;i++){
            final String[] p=ps[i]; CBtn b=new CBtn(p[0],pc[i],true);
            b.addActionListener(e->doPreset(p)); presSec.addBtn(b);
        }

        // Step mode
        JSeparator div=new JSeparator();
        div.setForeground(T.BORDER); div.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));
        div.setAlignmentX(Component.LEFT_ALIGNMENT);

        stepCheck=new JCheckBox("Step-by-Step Mode");
        stepCheck.setFont(T.fb(13)); stepCheck.setForeground(T.ACCENT); stepCheck.setBackground(T.SIDEBAR);
        stepCheck.setFocusPainted(false); stepCheck.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        stepCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepCheck.addActionListener(e->{if(!stepCheck.isSelected()){stepPanel.clear();showBottom("trav");}});

        JLabel stepHint=new JLabel("<html><font color='#94a3b8'>Traces each comparison &amp;<br>rotation step by step</font></html>");
        stepHint.setFont(T.fr(11)); stepHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepHint.setBorder(BorderFactory.createEmptyBorder(2,22,0,0));

        sb.add(inputSec);  sb.add(Box.createVerticalStrut(20));
        sb.add(travSec);   sb.add(Box.createVerticalStrut(20));
        sb.add(presSec);   sb.add(Box.createVerticalStrut(20));
        sb.add(div);       sb.add(Box.createVerticalStrut(16));
        sb.add(stepCheck); sb.add(Box.createVerticalStrut(4));
        sb.add(stepHint);
        sb.add(Box.createVerticalGlue());

        // Status label at bottom of sidebar
        statusLbl=new JLabel("Ready");
        statusLbl.setFont(T.fr(11)); statusLbl.setForeground(T.T3);
        statusLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLbl.setBorder(BorderFactory.createEmptyBorder(12,0,0,0));
        sb.add(statusLbl);

        return sb;
    }

    // ── Main content area ─────────────────────────────────────────────────────
    JPanel buildMain() {
        JPanel main=new JPanel(new BorderLayout(0,0));
        main.setBackground(T.PAGE);

        stats=new StatsPanel();
        canvas=new TreeCanvas(cur); canvas.mode=mode;

        bottomP=new JPanel(); bottomCL=new CardLayout();
        bottomP.setLayout(bottomCL); bottomP.setBackground(T.CARD);
        travPanel=new TravPanel(canvas); stepPanel=new StepPanel();
        bottomP.add(travPanel,"trav"); bottomP.add(stepPanel,"step");

        // Canvas with border
        JPanel canvasWrap=new JPanel(new BorderLayout());
        canvasWrap.setBackground(T.CANVAS);
        canvasWrap.setBorder(BorderFactory.createMatteBorder(0,0,1,0,T.BORDER));
        canvasWrap.add(canvas,BorderLayout.CENTER);

        main.add(stats,      BorderLayout.NORTH);
        main.add(canvasWrap, BorderLayout.CENTER);
        main.add(bottomP,    BorderLayout.SOUTH);
        return main;
    }

    void showBottom(String c){bottomCL.show(bottomP,c);}

    // ── Mode switching ────────────────────────────────────────────────────────
    void switchMode(String m) {
        if (searchTimer!=null) searchTimer.stop();
        if ("Red-Black".equals(m)) { mode="RB"; cur=rbL; }
        else if ("AVL".equals(m))  { mode="AVL"; cur=avlL; }
        else                        { mode="BST"; cur=bstL; }
        stepPanel.clear(); canvas.setTree(cur,mode); stats.update(cur,mode);
        setStatus("Switched to " + m + " mode", T.ACCENT);
        showBottom("trav");
    }

    // ── Operations ────────────────────────────────────────────────────────────
    void doInsert() {
        if (searchTimer!=null) searchTimer.stop();
        stepPanel.clear();
        String txt=inputFld.getText().trim();
        if (txt.isEmpty()){setStatus("Enter a value to insert",T.WARNING);return;}
        String[] parts=txt.split("[,\\s]+");
        if (stepCheck.isSelected()&&parts.length==1) {
            try {
                int val=Integer.parseInt(parts[0].trim());
                List<InsertStep> steps=new ArrayList<>();
                cur.insert(val,steps);
                canvas.clearAll();
                canvas.insertingVal = val;
                canvas.showInsertingVal = false;
                canvas.refreshLayout(); stats.update(cur,mode);
                showBottom("step");
                stepPanel.load(steps, (step, visible) -> {
                    canvas.showInsertingVal = visible;
                    canvas.applyStep(step);
                });
                setStatus("Step mode active — "+steps.size()+" steps for inserting "+val, T.ACCENT);
                inputFld.setText("");
            } catch (NumberFormatException ex){setStatus("Enter a valid integer",T.WARNING);}
        } else {
            int count=0;
            for (String p:parts){try{cur.insert(Integer.parseInt(p.trim()),null);count++;}catch(NumberFormatException ig){}}
            if (count>0){
                canvas.clearAll();canvas.refreshLayout();stats.update(cur,mode);
                showBottom("trav");travPanel.clear();
                setStatus("Inserted "+(count==1?parts[0].trim():count+" values"),T.SUCCESS);
                inputFld.setText("");
            } else setStatus("Enter a valid integer",T.WARNING);
        }
    }
    void doDelete() {
        if (searchTimer!=null) searchTimer.stop();
        stepPanel.clear();
        String txt=inputFld.getText().trim(); if(txt.isEmpty()){setStatus("Enter a value",T.WARNING);return;}
        try{int v=Integer.parseInt(txt);cur.delete(v);canvas.clearAll();canvas.refreshLayout();stats.update(cur,mode);travPanel.clear();showBottom("trav");setStatus("Deleted "+v,T.DANGER);inputFld.setText("");}
        catch(NumberFormatException e){setStatus("Enter a valid integer",T.WARNING);}
    }
    void doSearch() {
        if (searchTimer!=null) searchTimer.stop();
        stepPanel.clear();
        String txt=inputFld.getText().trim(); if(txt.isEmpty()){setStatus("Enter a value",T.WARNING);return;}
        try {
            int val=Integer.parseInt(txt);
            canvas.clearAll();travPanel.clear();showBottom("trav");
            List<TreeNode> path=cur.searchPath(val);
            if(path.isEmpty()){setStatus("Tree is empty",T.WARNING);return;}
            boolean found=path.get(path.size()-1).value==val;
            searchTimer=new Timer(360,null);int[]idx={0};
            searchTimer.addActionListener(e->{
                if(idx[0]<path.size()){canvas.searchVals.add(path.get(idx[0]).value);if(idx[0]==path.size()-1&&found)canvas.foundVal=val;canvas.repaint();idx[0]++;}
                else{((Timer)e.getSource()).stop();setStatus(found?"Found "+val+" in "+path.size()+" steps":"Value "+val+" not found",found?T.SUCCESS:T.DANGER);}
            });
            setStatus("Searching for "+val+"…",T.PINK); searchTimer.start();
        } catch(NumberFormatException e){setStatus("Enter a valid integer",T.WARNING);}
    }
    void doTraversal(String type) {
        if(cur.root==null){setStatus("Tree is empty",T.WARNING);return;}
        canvas.clearAll();
        List<Integer> order;
        switch(type){case "Inorder":order=cur.inorder();break;case "Preorder":order=cur.preorder();break;case "Postorder":order=cur.postorder();break;default:order=cur.levelorder();type="Level Order";}
        showBottom("trav");travPanel.show(type,order);setStatus(type+" traversal complete",T.ACCENT);
    }
    void doClear(){if(searchTimer!=null)searchTimer.stop();cur.root=null;canvas.clearAll();canvas.refreshLayout();stats.update(cur,mode);travPanel.clear();stepPanel.clear();showBottom("trav");setStatus("Tree cleared",T.T3);}
    void doPreset(String[] p){
        if(searchTimer!=null)searchTimer.stop();
        cur.root=null;
        if(p[1]!=null){for(String v:p[1].split(","))try{cur.insert(Integer.parseInt(v.trim()),null);}catch(NumberFormatException ig){}}
        else{Random rng=new Random();Set<Integer>seen=new LinkedHashSet<>();while(seen.size()<12)seen.add(rng.nextInt(99)+1);seen.forEach(v->cur.insert(v,null));}
        canvas.clearAll();canvas.refreshLayout();stats.update(cur,mode);travPanel.clear();stepPanel.clear();showBottom("trav");
        setStatus("Loaded preset: "+p[0],T.ACCENT);
    }
    void setStatus(String msg, Color c){statusLbl.setText("<html><font color='"+hex(c)+"'>"+msg+"</font></html>");}
    String hex(Color c){return String.format("#%02x%02x%02x",c.getRed(),c.getGreen(),c.getBlue());}

    void seed(){
        int[]v={50,30,70,20,40,60,80,10,90};
        for(int x:v){bstL.insert(x,null);avlL.insert(x,null);rbL.insert(x,null);}
        canvas.refreshLayout();stats.update(cur,mode);
    }

    public static void main(String[] args){SwingUtilities.invokeLater(BSTVisualizer::new);}
}
