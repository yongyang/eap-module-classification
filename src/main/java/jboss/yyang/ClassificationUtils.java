package jboss.yyang;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.*;

/**
 * Created by yyang on 10/30/15.
 */
public class ClassificationUtils {
    private Map<String, Module> previousModules = new HashMap<String, Module>();
    private Map<String, Module> currentModules = new HashMap<String, Module>();

    private List<Module> newModules = new ArrayList<Module>();
    private List<Module> removedModules = new ArrayList<Module>();
    private List<Module> updatedModules = new ArrayList<Module>();
    private List<Module> noUpdateModules = new ArrayList<Module>();

    public ClassificationUtils() {
    }

    public void parseModules(File previousModuleDir, File currentModuleDir){
        previousModules.putAll(parseDistModules(previousModuleDir));
        currentModules.putAll(parseDistModules(currentModuleDir));

        List<Module> allModules = new ArrayList<Module>();
        allModules.addAll(previousModules.values());
        allModules.addAll(currentModules.values());

        // set classification for alias modules
        for(Module module : allModules){
            if(module.isAlias()) {
                String target = module.getAlias();
                if(previousModules.containsKey(target)) {
                    module.setPreviousClassification(previousModules.get(target).getClassification());
                }
                if(currentModules.containsKey(target)) {
                    module.setClassification(currentModules.get(target).getClassification());
                }
            }
        }

        for(Module module : allModules) {
            if(previousModules.containsKey(module.getNameSlot()) && currentModules.containsKey(module.getNameSlot())) {
                if(previousModules.get(module.getNameSlot()).getClassification() != currentModules.get(module.getNameSlot()).getClassification()) {
                    Module newModule = currentModules.get(module.getNameSlot());
                    newModule.setPreviousClassification(previousModules.get(module.getNameSlot()).getClassification());
                    if(!updatedModules.contains(newModule)) {
                        updatedModules.add(newModule);
                    }
                }
                else {
                    Module newModule = currentModules.get(module.getNameSlot());
                    newModule.setPreviousClassification(newModule.getClassification());
                    if(!noUpdateModules.contains(newModule))  {
                        noUpdateModules.add(newModule);
                    }
                }
            }
            else if(previousModules.containsKey(module.getNameSlot()) && !currentModules.containsKey(module.getNameSlot())){
                module.setPreviousClassification(module.getClassification());
                module.setClassification(Module.Classification.NULL);
                if(!removedModules.contains(module)) {
                    removedModules.add(module);
                }
            }
            else if(!previousModules.containsKey(module.getNameSlot()) && currentModules.containsKey(module.getNameSlot())){
                module.setPreviousClassification(Module.Classification.NULL);
                if(!newModules.contains(module)) {
                    newModules.add(module);
                }
            }
        }
    }


    private List<Module> getNewModules() {
        return newModules;
    }

    private List<Module> getRemovedModules() {
        return removedModules;
    }

    private List<Module> getNoUpdateModules(){
        return noUpdateModules;
    }

    private List<Module> getUpdatedModules() {
        return updatedModules;
    }

    private static Map<String, Module> parseDistModules(File distModulesBaseDir) {
        Map<String, Module> modules = new HashMap<String, Module>();

        List<File> dirs = listDirs(distModulesBaseDir);

        for(File dir : dirs) {
            if(dir.isDirectory()&& dir.list(moduleDirFilenameFilter).length > 0 ) {
                System.out.println("INFO: Parse module " + dir);
                Module module = new Module(dir);
                modules.put(module.getNameSlot(), module);
            }
        }
        return modules;
    }

    private static List<File> listDirs(File moduleBaseDir) {
        List<File> files = new ArrayList<File>();
        if (!moduleBaseDir.exists() || moduleBaseDir.isFile())
            return files;
        listDirs(files, moduleBaseDir);
        return files;
    }

    private static void listDirs(List<File> dirList, File baseDir) {
        File[] dirs = baseDir.listFiles(directoryFileFilter());
        List<File> temp = Arrays.asList(dirs);
        dirList.addAll(temp);

        for (File subDir : dirs) {
            listDirs(dirList, subDir);
        }
    }

    /**
     * Returns a filter that checks if the file is a directory.
     *
     * @return directory file filter
     */
    private static FileFilter directoryFileFilter() {
        return new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
    }

    private static FilenameFilter moduleDirFilenameFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            if("module.xml".equals(name)) {
                return true;
            }
            return false;
        }
    };

    public static class Module implements Comparable<Module>{

        private File moduleDir;

        private String name;
        private String slot = "";
        private Classification previousClassification = Classification.NULL;
        private Classification classification;

        //TODO: alias
        private String alias = "";

        public static enum Classification {
            NULL,
            PUBLIC,
            PRIVATE,
            UNSUPPORTED,
            DEPRECATED,
            UNKNOWN
        }

        public Module(File moduleDir) {
            this.moduleDir = moduleDir;
            try {
                parse();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Classification getPreviousClassification() {
            return previousClassification;
        }

        public void setPreviousClassification(Classification previousClassification) {
            this.previousClassification = previousClassification;
        }

        public Classification getClassification() {
            return classification;
        }

        public void setClassification(Classification classification) {
            this.classification = classification;
        }

        public String getName() {
            return name;
        }

        public String getNameSlot() {
            if(slot.isEmpty()) {
                return getName();
            }
            else {
                return getName() + ":" + slot;
            }
        }

        public File getModuleDir() {
            return moduleDir;
        }

        public String getAlias() {
            return alias;
        }

        public boolean isAlias(){
            return !alias.isEmpty();
        }

        private void parse() throws Exception {
            Document moduleXML = XMLUtils.loadDocument(new File(moduleDir, "module.xml"));



            name = XMLUtils.getAttributeValue(moduleXML.getDocumentElement(), "name");
            if(moduleXML.getDocumentElement().getTagName().equals("module-alias")) {
                alias = XMLUtils.getAttributeValue(moduleXML.getDocumentElement(), "target-name");
            }
            else {

                String slot = XMLUtils.getAttributeValue(moduleXML.getDocumentElement(), "slot");
                if (slot != null) {
                    this.slot = slot;
                }


                // properties
                List<Element> properties = XMLUtils.getElementsByTagName(moduleXML.getDocumentElement(), "property");
                if (properties != null) {
                    for (Element property : properties) {
                        if ("jboss.api".equals(property.getAttribute("name"))) {
                            String privateString = property.getAttribute("value");
                            if (Classification.PRIVATE.name().toLowerCase().equals(privateString)) {
                                this.classification = Classification.PRIVATE;
                            } else if (Classification.PUBLIC.name().toLowerCase().equals(privateString)) {
                                this.classification = Classification.PUBLIC;
                            } else if (Classification.UNSUPPORTED.name().toLowerCase().equals(privateString)) {
                                this.classification = Classification.UNSUPPORTED;
                            } else if (Classification.DEPRECATED.name().toLowerCase().equals(privateString)) {
                                this.classification = Classification.DEPRECATED;
                            } else {
                                this.classification = Classification.UNKNOWN;
                            }
                        }
                    }
                }
                if (this.classification == null) {
                    this.classification = Classification.PUBLIC;
                }
            }
        }

        @Override
        public String toString() {
            return getNameSlot() + " ["  + (isAlias() ? "alias:" + getAlias() +", " : "") + getPreviousClassification().name().toLowerCase() + "->" + getClassification().name().toLowerCase() + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Module module = (Module) o;

            if (!name.equals(module.name)) return false;
            if (!slot.equals(module.slot)) return false;
            return classification == module.classification;

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + slot.hashCode();
            return result;
        }

        public int compareTo(Module o) {
            return this.getNameSlot().compareTo(o.getNameSlot());
        }
    }


    public static void main(String[] args) {
        ClassificationUtils utils = new ClassificationUtils();
        utils.parseModules(new File(args[0]), new File(args[1]));
//        System.out.println(utils.getNewModules());
        System.out.println("----------------------");
        printModules("NEW MODULES:", utils.getNewModules());
        System.out.println("----------------------");
        printModules("REMOVED MODULES:", utils.getRemovedModules());
        System.out.println("----------------------");
        printModules("UPDATED MODULES:", utils.getUpdatedModules());
        System.out.println("----------------------");
        printModules("NO_UPDATE MODULES:", utils.getNoUpdateModules());
    }

    public static void printModules(String title, List<Module> modules) {
        System.out.println(title);
        Collections.sort(modules);
        for(Module module : modules) {
            System.out.println(module.toString());
        }
    }
}

