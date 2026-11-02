package se.cloudneutral.orders.aspect;

import java.lang.reflect.Method;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.resilience.retry.MethodRetryPredicate;
import org.springframework.stereotype.Component;

@Component
public class ExceptionClassifier implements MethodRetryPredicate {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SERIALIZATION_FAILURE = "40001";

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean shouldRetry(Method method, Throwable ex) {
        if (!isEnabled() || ex == null) {
            return false;
        }

        Throwable throwable = NestedExceptionUtils.getMostSpecificCause(ex);
        if (throwable instanceof SQLException sqlException
            && SERIALIZATION_FAILURE.equals(sqlException.getSQLState())) {
            logger.warn("Transient SQL exception in {}, retrying: sql state [{}], message [{}]",
                    method.getName(), sqlException.getSQLState(), sqlException.getMessage());
            return true;
        }

        // Business exceptions pass through here too, so this is not worth more than debug
        logger.debug("Non-transient exception in {}, not retrying: {}", method.getName(), ex.getClass());
        return false;
    }
}

