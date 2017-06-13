# CounterTestNG

This small library helps to count TestNG tests before run them. You just need to add one dependensy in pom.xml or jar library of org.reflections. Then copy Counter.java file into your project, setup root path and package with tests which you want to count. 
This library counts methods which are annotated with @Test and takes into account DataProviders and enabled/disabled tests, so you should have correct amount of tests in your project without exactly executing tests.
