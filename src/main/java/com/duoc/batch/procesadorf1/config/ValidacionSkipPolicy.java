package com.duoc.batch.procesadorf1.config;

import com.duoc.batch.procesadorf1.exception.DataValidationException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * Politica personalizada de tolerancia a fallos:
 *  - 'DataValidationException' (registros con datos inconsistentes) se
 *    consideran SKIP-es => el Step continua procesando el resto.
 *  - Cualquier otra excepcion no es skipeable => falla el Step.
 */
public class ValidacionSkipPolicy implements SkipPolicy {

    private final int maxSkip;

    public ValidacionSkipPolicy(int maxSkip) {
        this.maxSkip = maxSkip;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
        if (t instanceof DataValidationException) {
            if (this.stopLimitExceeded(skipCount)) {
                throw new SkipLimitExceededException(this.maxSkip, t);
            }
            return true;
        }
        return false;
    }

    private boolean stopLimitExceeded(long skipCount) {
        return skipCount >= maxSkip;
    }
}
