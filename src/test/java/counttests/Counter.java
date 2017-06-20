package counttests;

import org.testng.TestNG;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.xml.Parser;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;


/**
 * Created by Vadym Shevchenko on 6/12/2017.
 */
public class Counter {

    private Set<Method> tests;
    private Set<Method> dataProviders;

    public void countTestsInXmlFile(String rootPackage, String fullPathToSuiteName){
        tests = new HashSet<>();
        List<Class> classes = getClassesFromXmlFile(fullPathToSuiteName);
        // find all methods in specific classes which annotated by Test
        for(Class aClass : classes){
            for (Method method : aClass.getMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    tests.add(method);
                }
            }
        }
        // find all dataProviders
        dataProviders = getAllDataProviders(rootPackage);
    }

    public void countTestsInPackage(String rootPackage, String packageName){
        tests.clear();
        String fullPathToTests = rootPackage;
        if(!packageName.isEmpty())
            fullPathToTests = rootPackage + "." + packageName;

        try {
            // find all methods in specific classes which annotated by Test
            Iterable<Class> allClassesInSpecificPackage = getClasses(fullPathToTests);
            for (Class clazz : allClassesInSpecificPackage) {
                for (Method method : clazz.getMethods()) {
                    if (method.isAnnotationPresent(Test.class)) {
                        tests.add(method);
                    }
                }
            }
            // find all dataProviders
            dataProviders = getAllDataProviders(rootPackage);
        } catch (ClassNotFoundException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InstantiationException {
        String rootPackage = "counttests"; // example: org.site
        String packageName = ""; // example: testpackage.newpack
        String pathToSuiteXml = "src/test/resources/tempto-tests.xml";
        // full path will be org.site.testpackage.newpack as result

        if(args.length == 2){
            rootPackage = args[0];
            packageName = args[1];
        }
        Counter counter = new Counter();
        counter.countTestsInXmlFile(rootPackage, pathToSuiteXml);

        int numberOfAnnotatedTests = counter.getAmountOfAnnotatedTests(true);
        int numberOfAllTests = counter.getAmountOfAllTestsIncludeDataProviders(true);

        System.out.println("Test cases which are annotated by @Test in XML file = " + numberOfAnnotatedTests);
        System.out.println("Amount of tests in XML file = " + numberOfAllTests + " in package " + rootPackage + "." + packageName);

        counter.countTestsInPackage(rootPackage, packageName);

        numberOfAnnotatedTests = counter.getAmountOfAnnotatedTests(true);
        numberOfAllTests = counter.getAmountOfAllTestsIncludeDataProviders(true);

        System.out.println("Test cases which are annotated by @Test in package = " + numberOfAnnotatedTests);
        System.out.println("Amount of tests  = " + numberOfAllTests + " in package " + rootPackage + "." + packageName);

    }

    public int getAmountOfAnnotatedTests(boolean onlyEnabledTests) {
        int amountOfTests = 0;
        for (Method testMethod : tests){
            boolean isEnabled = testMethod.getAnnotation(Test.class).enabled();
            if(onlyEnabledTests){
                if(isEnabled){
                    amountOfTests++;
                }
            } else {
                amountOfTests++;
            }
        }
        return amountOfTests;
    }

    public int getAmountOfAllTestsIncludeDataProviders(boolean onlyEnabledTests) {
        int countOfTest = 0;
        for(Method testMethod : tests){
            boolean isEnabled = testMethod.getAnnotation(Test.class).enabled();
            if(onlyEnabledTests){
                if(!isEnabled){
                    continue; // if test is not enabled just skip it (depending on onlyEnabledTests parameter)
                }
            }

            String nameOfDataProvider = testMethod.getAnnotation(Test.class).dataProvider();
            Class classWhereDataProvider;
            String name = testMethod.getAnnotation(Test.class).dataProviderClass().getName();
            if(!name.contains("java.lang.Object")){
                classWhereDataProvider = testMethod.getAnnotation(Test.class).dataProviderClass();
            } else {
                classWhereDataProvider = testMethod.getDeclaringClass();
            }
            if(nameOfDataProvider.isEmpty()) {
                countOfTest++;
                continue;
            }

            int numberOfAdditionalTests = getSizeOfArrayFromDataProviderByName(nameOfDataProvider, classWhereDataProvider);
            countOfTest += numberOfAdditionalTests;
         }
        return countOfTest;
    }

    private int getSizeOfArrayFromDataProviderByName(String nameOfDataProvider, Class classWhereDataProviderIsLocated) {
        int sizeOfArrayInDataProvider = 0;
        for(Method dataProvider : dataProviders){
            String dataProviderParameterName = dataProvider.getAnnotation(DataProvider.class).name();
            String actualNameOfClassDataProvider = dataProvider.getDeclaringClass().getName();
            String expectedNameOfClassDataProvider = classWhereDataProviderIsLocated.getName();
            if(dataProviderParameterName.equals(nameOfDataProvider) && expectedNameOfClassDataProvider.equals(actualNameOfClassDataProvider)){
                try {
                    Class classOfTest = dataProvider.getDeclaringClass(); // get Class where dataProvider is located
                    String nameOfReturnType = dataProvider.getReturnType().getName();
                    // DataProvider can returns two different types - Object[][] and Iterator<Object[]>
                    if(nameOfReturnType.contains("java.lang.Object")){
                        Object[][] objectReturn = (Object[][]) dataProvider.invoke(classOfTest.newInstance());
                        sizeOfArrayInDataProvider += objectReturn.length;
                    } else if(nameOfReturnType.contains("java.util.Iterator")) {
                        Iterator iteratorReturn = (Iterator) dataProvider.invoke(classOfTest.newInstance());
                        while (iteratorReturn.hasNext()) {
                            iteratorReturn.next();
                            sizeOfArrayInDataProvider++;
                        }
                    }
                    break;
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
        return sizeOfArrayInDataProvider;
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     *
     * @param packageName
     *            The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private Iterable<Class> getClasses(String packageName) throws ClassNotFoundException, IOException, URISyntaxException
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements())
        {
            URL resource = resources.nextElement();
            URI uri = new URI(resource.toString());
            dirs.add(new File(uri.getPath()));
        }
        List<Class> classes = new ArrayList<Class>();
        for (File directory : dirs)
        {
            classes.addAll(findClasses(directory, packageName));
        }

        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * subdirs.
     *
     * @param directory
     *            The base directory
     * @param packageName
     *            The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException
    {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists())
        {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files)
        {
            if (file.isDirectory())
            {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            }
            else if (file.getName().endsWith(".class"))
            {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    private List<Class> getClassesFromXmlFile(String fullPathToSuiteName){
        final Parser parser = new Parser(fullPathToSuiteName);
        final List<XmlSuite> suites;
        final List<Class> classes = new ArrayList<>();
        try {
            suites = parser.parseToList();
            for(XmlSuite xmlSuite : suites){
                List<XmlTest> testsInSuite = xmlSuite.getTests();
                for(XmlTest testInSuite : testsInSuite){
                    List<XmlClass> classesInTest = testInSuite.getClasses();
                    for(XmlClass classInTestSuite : classesInTest){
                        classes.add(Class.forName(classInTestSuite.getName()));
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return classes;
    }

    private Set<Method> getAllDataProviders(String rootPackage){
        Set<Method> dataProvider = new HashSet<>();
        try {
            Iterable<Class> allClassesInRootPackage = getClasses(rootPackage);
            for (Class clazz : allClassesInRootPackage) {
                for (Method method : clazz.getMethods()) {
                    if (method.isAnnotationPresent(DataProvider.class)) {
                        dataProvider.add(method);
                    }
                }
            }
        } catch (ClassNotFoundException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return dataProvider;
    }


}

