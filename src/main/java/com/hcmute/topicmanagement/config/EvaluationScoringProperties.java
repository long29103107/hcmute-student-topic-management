package com.hcmute.topicmanagement.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class EvaluationScoringProperties {

    private final BigDecimal minimumScore;
    private final BigDecimal maximumScore;

    public EvaluationScoringProperties(
            @Value("${evaluation.score.min:0}") BigDecimal minimumScore,
            @Value("${evaluation.score.max:10}") BigDecimal maximumScore) {
        if (minimumScore == null || maximumScore == null || minimumScore.compareTo(maximumScore) >= 0) {
            throw new IllegalArgumentException("evaluation.score.min must be lower than evaluation.score.max.");
        }
        this.minimumScore = minimumScore;
        this.maximumScore = maximumScore;
    }

    public BigDecimal getMinimumScore() {
        return minimumScore;
    }

    public BigDecimal getMaximumScore() {
        return maximumScore;
    }
}
