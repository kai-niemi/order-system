package se.cloudneutral.orders.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.resilience.annotation.EnableResilientMethods;

import se.cloudneutral.orders.aspect.AdvisorOrder;

@Configuration
@EnableResilientMethods(proxyTargetClass = true, order = AdvisorOrder.TRANSACTION_BEFORE_ADVISOR)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ResilientConfig {
}
