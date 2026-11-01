package se.cloudneutral.orders;

import java.math.BigDecimal;
import java.math.RoundingMode;

import se.cloudneutral.orders.domain.Address;
import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.util.RandomData;

public abstract class TestDataGenerator {
    private TestDataGenerator() {
    }

    public static final int DEFAULT_INVENTORY_QUANTITY = 500;

    public static Product randomProduct() {
        return Product.builder()
                .withName(RandomData.randomProduct())
                .withPrice(new BigDecimal("150.00").setScale(2, RoundingMode.UNNECESSARY))
                .withSku(RandomData.randomSku(12))
                .withInventory(DEFAULT_INVENTORY_QUANTITY)
                .build();
    }

    public static Customer randomCustomer() {
        String fn = RandomData.randomFirstName();
        String ln = RandomData.randomLastName();
        return Customer.builder()
                .withFirstName(fn)
                .withLastName(ln)
                .withEmail(RandomData.randomEmail(fn, ln))
                .withAddress(randomAddress())
                .build();
    }

    public static Address randomAddress() {
        return Address.builder()
                .withAddress1(RandomData.randomAddress())
                .withAddress2(RandomData.randomAddress())
                .withCity(RandomData.randomCity())
                .withPostcode(RandomData.randomZipCode())
                .withCountry(RandomData.randomCountry())
                .build();
    }
}
