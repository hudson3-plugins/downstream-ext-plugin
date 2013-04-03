package hudson.model;

import hudson.util.EditDistance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Misc. utility methods extracted from Jenkins, AbstractProject and AbstractItem.
 * 
 * @author Bob Foster
 */
public class HudsonHelper {

      public static Item getItem(String pathName, ItemGroup context) {
        Hudson hudson = Hudson.getInstance();
        if (context==null)  context = hudson;
        if (pathName==null) return null;

        if (pathName.startsWith("/"))   // absolute
            return hudson.getItemByFullName(pathName);

        Object/*Item|ItemGroup*/ ctx = context;

        StringTokenizer tokens = new StringTokenizer(pathName,"/");
        while (tokens.hasMoreTokens()) {
            String s = tokens.nextToken();
            if (s.equals("..")) {
                if (ctx instanceof Item) {
                    ctx = ((Item)ctx).getParent();
                    continue;
                }

                ctx=null;    // can't go up further
                break;
            }
            if (s.equals(".")) {
                continue;
            }

            if (ctx instanceof ItemGroup) {
                ItemGroup g = (ItemGroup) ctx;
                Item i = g.getItem(s);
                if (i==null || !i.hasPermission(Item.READ)) { // XXX consider DISCOVER
                    ctx=null;    // can't go up further
                    break;
                }
                ctx=i;
            } else {
                return null;
            }
        }

        if (ctx instanceof Item)
            return (Item)ctx;

        // fall back to the classic interpretation
        return hudson.getItemByFullName(pathName);
    }

    public static final Item getItem(String pathName, Item context) {
        return getItem(pathName, context!=null?context.getParent():null);
    }

    public static final <T extends Item> T getItem(String pathName, ItemGroup context, Class<T> type) {
        Item r = getItem(pathName, context);
        if (type.isInstance(r))
            return type.cast(r);
        return null;
    }

    public static final <T extends Item> T getItem(String pathName, Item item, Class<T> type) {
        Item r = getItem(pathName, item.getParent());
        if (type.isInstance(r))
            return type.cast(r);
        return null;
    }

    /**
     * Finds a {@link AbstractProject} that has the name closest to the given name.
     */
    public static AbstractProject findNearest(String name) {
        return findNearest(name,Hudson.getInstance());
    }

    /**
     * Finds a {@link AbstractProject} whose name (when referenced from the specified context) is closest to the given name.
     *
     * @since 1.419
     */
    public static AbstractProject findNearest(String name, ItemGroup context) {
        List<AbstractProject> projects = Hudson.getInstance().getAllItems(AbstractProject.class);
        String[] names = new String[projects.size()];
        for( int i=0; i<projects.size(); i++ )
            names[i] = getRelativeNameFrom(projects.get(i), context);

        String nearest = EditDistance.findNearest(name, names);
        return (AbstractProject)getItem(nearest,context);
    }

    public static String getRelativeNameFrom(Item item, ItemGroup p) {
        // first list up all the parents
        Map<ItemGroup,Integer> parents = new HashMap<ItemGroup,Integer>();
        int depth=0;
        while (p!=null) {
            parents.put(p, depth++);
            if (p instanceof Item)
                p = ((Item)p).getParent();
            else
                p = null;
        }

        StringBuilder buf = new StringBuilder();
        Item i=item;
        while (true) {
            if (buf.length()>0) buf.insert(0,'/');
            buf.insert(0,i.getName());
            ItemGroup g = i.getParent();

            Integer d = parents.get(g);
            if (d!=null) {
                String s="";
                for (int j=d; j>0; j--)
                    s+="../";
                return s+buf;
            }

            if (g instanceof Item)
                i = (Item)g;
            else
                return null;
        }
    }
    
    public static <T extends Item> List<T> fromNameList(ItemGroup context, String list, Class<T> type) {
        List<T> r = new ArrayList<T>();
        StringTokenizer tokens = new StringTokenizer(list,",");
        while(tokens.hasMoreTokens()) {
            String fullName = tokens.nextToken().trim();
            T item = getItem(fullName, context, type);
            if(item!=null)
                r.add(item);
        }
        return r;
    }
}
