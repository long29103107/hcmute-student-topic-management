package com.hcmute.topicmanagement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.hcmute.topicmanagement.config.EvaluationScoringProperties;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;

/**
 * Keeps score validation and aggregation identical for evaluator and publisher
 * workflows. Invalid rows are ignored for an average and can never make a
 * result publishable.
 */
@Component
public class EvaluationScoreCalculator {

    private static final int AVERAGE_SCALE = 2;

    private final EvaluationScoringProperties scoringProperties;

    public EvaluationScoreCalculator(EvaluationScoringProperties scoringProperties) {
        this.scoringProperties = scoringProperties;
    }

    public List<EvaluationEntity> currentEvaluations(List<EvaluationEntity> evaluations) {
        boolean hasBoardEvaluation = evaluations.stream().anyMatch(evaluation -> evaluation.getBoard() != null);
        if (!hasBoardEvaluation) {
            return List.copyOf(evaluations);
        }
        return evaluations.stream()
                .filter(evaluation -> evaluation.getBoard() != null
                        && evaluation.getBoardMember() != null
                        && evaluation.getBoardMember().isActive())
                .toList();
    }

    public List<EvaluationEntity> validSubmittedEvaluations(List<EvaluationEntity> evaluations) {
        return currentEvaluations(evaluations).stream()
                .filter(this::isSubmittedWithValidScore)
                .toList();
    }

    public boolean isSubmittedWithValidScore(EvaluationEntity evaluation) {
        BigDecimal score = evaluation.getScore();
        return score != null
                && score.scale() <= AVERAGE_SCALE
                && score.compareTo(scoringProperties.getMinimumScore()) >= 0
                && score.compareTo(scoringProperties.getMaximumScore()) <= 0
                && (evaluation.getStatus() == EvaluationStatus.SUBMITTED
                        || evaluation.getStatus() == EvaluationStatus.PUBLISHED);
    }

    public BigDecimal averageScore(List<EvaluationEntity> evaluations) {
        List<EvaluationEntity> valid = validSubmittedEvaluations(evaluations);
        if (valid.isEmpty()) {
            return null;
        }
        BigDecimal total = valid.stream()
                .map(EvaluationEntity::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(valid.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }
}
