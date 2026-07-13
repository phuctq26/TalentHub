package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.AuditLog;
import com.talenthub.recruitment.entity.enums.AuditEventType;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    public static Specification<AuditLog> getFilterSpecification(
            String eventTypeStr,
            String actor,
            LocalDate dateFrom,
            LocalDate dateTo) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (eventTypeStr != null && !eventTypeStr.trim().isEmpty()) {
                try {
                    AuditEventType type = AuditEventType.valueOf(eventTypeStr);
                    predicates.add(criteriaBuilder.equal(root.get("eventType"), type));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid enum strings
                }
            }

            if (actor != null && !actor.trim().isEmpty()) {
                String searchPattern = "%" + actor.trim().toLowerCase() + "%";
                Predicate matchUsername = criteriaBuilder.like(criteriaBuilder.lower(root.get("actorUsernameSnapshot")), searchPattern);
                Predicate matchFullName = criteriaBuilder.like(criteriaBuilder.lower(root.get("actorFullNameSnapshot")), searchPattern);
                predicates.add(criteriaBuilder.or(matchUsername, matchFullName));
            }

            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        dateFrom.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()
                ));
            }

            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("createdAt"),
                        dateTo.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()
                ));
            }

            if (predicates.isEmpty()) {
                return null;
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
