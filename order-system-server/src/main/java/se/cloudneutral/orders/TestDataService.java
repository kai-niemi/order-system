package se.cloudneutral.orders;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.repository.CustomerRepository;
import se.cloudneutral.orders.repository.OrderRepository;
import se.cloudneutral.orders.repository.ProductRepository;
import se.cloudneutral.orders.util.StreamUtils;

@Service
public class TestDataService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    public boolean isEmpty() {
        return productRepository.count() == 0
                && customerRepository.count() == 0;
    }

    @TransactionBoundary
    public void deleteAllData() {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "TX not active");

        orderRepository.deleteAllOrderItems();
        orderRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
    }

    @TransactionBoundary
    public void createProducts(int numProducts, Supplier<Product> supplier) {
        IntStream.rangeClosed(1, numProducts)
                .forEach(value -> productRepository.save(supplier.get()));
    }

    @TransactionBoundary
    public void createCustomers(int numCustomers, Supplier<Customer> supplier) {
        List<Customer> customers = IntStream.rangeClosed(1, numCustomers)
                .mapToObj(value -> supplier.get())
                .toList();

        StreamUtils.chunkedStream(customers.stream(), 128)
                .forEach(chunk -> customerRepository.saveAll(chunk));
    }
}
