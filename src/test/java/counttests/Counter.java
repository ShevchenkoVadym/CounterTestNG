package counttests;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;


/**
 * Created by vadym.shevchenko on 6/12/2017.
 */
public class Counter {

    private Set<Method> tests;
    private Set<Method> dataProviders;

    Counter(String rootPackage, String packageName){
        String fullPathToTests = rootPackage + "." + packageName;
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(fullPathToTests)))
                .setUrls(ClasspathHelper.forPackage(fullPathToTests))
                .addScanners(new MethodAnnotationsScanner()));
        tests = reflections.getMethodsAnnotatedWith(Test.class);

        Reflections reflectionsDataProvidersWholeProject = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(rootPackage))
                .addScanners(new MethodAnnotationsScanner()));
        dataProviders = reflectionsDataProvidersWholeProject.getMethodsAnnotatedWith(DataProvider.class);
    }

    public static void main(String[] args) throws InstantiationException {
        String rootPackage = "counttests"; // example: org.site
        String packageName = ""; // example: testpackage.newpack
        // full path will be org.site.testpackage.newpack as result

        if(args.length == 2){
            rootPackage = args[0];
            packageName = args[1];
        }
        Counter counter = new Counter(rootPackage, packageName);
        int numberOfAnnotatedTests = counter.getAmountOfAnnotatedTests(true);
        int numberOfAllTests = counter.getAmountOfAllTestsIncludeDataProviders(true);

        System.out.println("Test cases which are annotated by @Test = " + numberOfAnnotatedTests);
        System.out.println("Amount of tests = " + numberOfAllTests + " in package " + rootPackage + "." + packageName);
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


}

