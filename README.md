# CounterTestNG

This small library helps to count TestNG tests before run them. 

You just need to add Counter.java file into your project, setup root, package path with tests which you want to count. 

This library counts methods which are annotated with @Test and takes into account DataProviders, also you can count enabled/disabled tests, so you should have correct amount of tests in your project without exactly executing tests.

Two features implemented in this class:  
1. Count TestNG tests in your specific package.  
2. Count TestNG tests from testng.xml file.  
