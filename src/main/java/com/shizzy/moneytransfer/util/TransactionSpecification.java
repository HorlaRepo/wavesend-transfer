package com.shizzy.moneytransfer.util;

import com.shizzy.moneytransfer.model.Transaction;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {
    public static Specification<Transaction> buildSpecification(String searchQuery, String searchFilter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchQuery != null && !searchQuery.isEmpty()) {
                String likePattern = "%" + searchQuery + "%";
                Predicate inputStringPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("sender").get("firstName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("sender").get("lastName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("receiver").get("firstName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("receiver").get("lastName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("mtcn")), likePattern)
                );
                predicates.add(inputStringPredicate);
            }

            if (searchFilter != null && !searchFilter.isEmpty()) {
                String likePattern = "%" + searchFilter + "%";
                //Predicate searchQueryPredicate = cri
                        //predicates.add(searchQueryPredicate);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}