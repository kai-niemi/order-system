package se.cloudneutral.orders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TestDataRunner implements ApplicationRunner {
    @Autowired
    private TestDataService testDataService;

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("drop-test-data")) {
            testDataService.deleteAllData();
        }

        if (testDataService.isEmpty()) {
            testDataService.createProducts(
                    1000,
                    TestDataGenerator::randomProduct
            );

            testDataService.createCustomers(
                    5000,
                    TestDataGenerator::randomCustomer
            );
        }
    }
}
